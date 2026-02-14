package io.github.zenhelix.dependanger.features.license

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.core.model.MavenRepository
import io.github.zenhelix.dependanger.core.util.GlobMatcher
import io.github.zenhelix.dependanger.effective.DiagnosticCodes
import io.github.zenhelix.dependanger.effective.model.EffectiveLibrary
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.withExtension
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase
import io.github.zenhelix.dependanger.features.license.model.LicenseCategory
import io.github.zenhelix.dependanger.features.license.model.LicenseResult
import io.github.zenhelix.dependanger.features.license.model.LicenseViolation
import io.github.zenhelix.dependanger.features.license.model.LicenseViolationType
import io.github.zenhelix.dependanger.features.license.model.LicenseViolationsExtensionKey
import io.github.zenhelix.dependanger.features.license.model.isCopyleft
import io.github.zenhelix.dependanger.features.license.spi.LicenseSourceProvider
import io.github.zenhelix.dependanger.features.resolver.CredentialsProviderKey
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.ServiceLoader

private val logger = KotlinLogging.logger {}

public class LicenseCheckProcessor : EffectiveMetadataProcessor {
    override val id: String = "license-check"
    override val phase: ProcessingPhase = ProcessingPhase.LICENSE_CHECK
    override val order: Int = phase.order
    override val isOptional: Boolean = true
    override val description: String = "Checks library license compliance"

    override fun supports(context: ProcessingContext): Boolean =
        context.settings.licenseCheck.enabled

    override suspend fun process(metadata: EffectiveMetadata, context: ProcessingContext): EffectiveMetadata {
        val settings = context.settings.licenseCheck
        val repositories = context.settings.repositories
            .filterIsInstance<MavenRepository>()
            .ifEmpty {
                listOf(MavenRepository(url = "https://repo.maven.apache.org/maven2", name = "Maven Central"))
            }
        val credentialsProvider = context[CredentialsProviderKey]
        val customProviders = ServiceLoader.load(LicenseSourceProvider::class.java).toList()
        if (customProviders.isNotEmpty()) {
            logger.info { "Loaded ${customProviders.size} custom license source provider(s): ${customProviders.map { it.sourceId }}" }
        }

        var diagnostics = metadata.diagnostics

        if (settings.includeTransitives) {
            logger.warn { "includeTransitives is enabled but transitive license checking is not yet implemented (requires F012 Transitive Resolution)" }
            diagnostics = diagnostics + Diagnostics.warning(
                DiagnosticCodes.License.INCLUDE_TRANSITIVES_NOT_SUPPORTED,
                "includeTransitives is enabled but not yet supported; only direct dependencies will be checked",
                id, emptyMap(),
            )
        }

        val candidates = metadata.libraries.values.filter { lib ->
            lib.version != null
                    && settings.ignoreLibraries.none { pattern -> GlobMatcher.matches(pattern, lib.group, lib.artifact) }
        }

        if (candidates.isEmpty()) {
            diagnostics = diagnostics + Diagnostics.info(DiagnosticCodes.License.NO_LIBS, "No libraries to check for licenses", id, emptyMap())
            return metadata.copy(diagnostics = diagnostics)
                .withExtension(LicenseViolationsExtensionKey, emptyList())
        }

        LicenseCheckContext(
            repositories = repositories,
            credentialsProvider = credentialsProvider,
            cacheDirectory = settings.cacheDirectory,
            cacheTtlHours = settings.cacheTtlHours,
            readTimeoutMs = settings.timeout,
            customProviders = customProviders,
        ).use { ctx ->
            val semaphore = Semaphore(settings.parallelism)

            val results: List<Pair<EffectiveLibrary, List<LicenseResult>>> = coroutineScope {
                candidates.map { lib ->
                    async {
                        semaphore.withPermit {
                            val licenses = ctx.resolver.resolve(
                                group = lib.group,
                                artifact = lib.artifact,
                                version = lib.version!!.value,
                                declaredLicenseId = lib.license?.id,
                            )
                            lib to licenses
                        }
                    }
                }.awaitAll()
            }

            val allViolations = mutableListOf<LicenseViolation>()

            for ((lib, licenses) in results) {
                val policyResult = LicensePolicy.checkCompliance(lib, licenses, settings)
                allViolations.addAll(policyResult.violations)
                diagnostics = diagnostics + policyResult.diagnostics
            }

            diagnostics = diagnostics + if (allViolations.isEmpty()) {
                Diagnostics.info(
                    DiagnosticCodes.License.ALL_COMPLIANT,
                    "License check completed: ${results.size} libraries, all compliant",
                    id, mapOf("count" to results.size.toString()),
                )
            } else {
                Diagnostics.info(
                    DiagnosticCodes.License.CHECK_COMPLETE,
                    "License check completed: ${results.size} libraries, ${allViolations.size} violation(s)",
                    id, mapOf("count" to results.size.toString(), "violations" to allViolations.size.toString()),
                )
            }

            if (settings.failOnDenied) {
                val deniedCount = allViolations.count { it.violationType == LicenseViolationType.DENIED }
                if (deniedCount > 0) {
                    diagnostics = diagnostics + Diagnostics.error(
                        DiagnosticCodes.License.DENIED_FOUND,
                        "Denied licenses found in $deniedCount library(ies)",
                        id, mapOf("count" to deniedCount.toString()),
                    )
                }
            }

            if (settings.failOnUnknown) {
                val unknownCount = results.count { (_, licenses) -> licenses.all { it.category == LicenseCategory.UNKNOWN } }
                if (unknownCount > 0) {
                    diagnostics = diagnostics + Diagnostics.error(
                        DiagnosticCodes.License.UNKNOWN_FOUND,
                        "Unknown licenses found in $unknownCount library(ies)",
                        id, mapOf("count" to unknownCount.toString()),
                    )
                }
            }

            if (settings.failOnCopyleft) {
                val copyleftCount = results.count { (_, licenses) -> licenses.any { it.category.isCopyleft } }
                if (copyleftCount > 0) {
                    diagnostics = diagnostics + Diagnostics.error(
                        DiagnosticCodes.License.COPYLEFT_FOUND,
                        "Copyleft licenses found in $copyleftCount library(ies)",
                        id, mapOf("count" to copyleftCount.toString()),
                    )
                }
            }

            return metadata.copy(diagnostics = diagnostics)
                .withExtension(LicenseViolationsExtensionKey, allViolations)
        }
    }
}
