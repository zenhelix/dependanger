package io.github.zenhelix.dependanger.features.license

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.core.model.MavenRepository
import io.github.zenhelix.dependanger.core.util.GlobMatcher
import io.github.zenhelix.dependanger.effective.DiagnosticCodes
import io.github.zenhelix.dependanger.effective.ProcessorIds
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
import io.github.zenhelix.dependanger.features.transitive.model.FlatDependency
import io.github.zenhelix.dependanger.features.transitive.model.flatDependencies
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.ServiceLoader

private val logger = KotlinLogging.logger {}

public class LicenseCheckProcessor : EffectiveMetadataProcessor {
    override val id: String = ProcessorIds.LICENSE_CHECK
    override val phase: ProcessingPhase = ProcessingPhase.LICENSE_CHECK
    override val order: Int = phase.order
    override val isOptional: Boolean = true
    override val description: String = "Checks library license compliance"

    override fun supports(context: ProcessingContext): Boolean =
        context[LicenseCheckSettingsKey]?.enabled == true

    override suspend fun process(metadata: EffectiveMetadata, context: ProcessingContext): EffectiveMetadata {
        val settings = context.require(LicenseCheckSettingsKey)
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

        val candidates = metadata.libraries.values.filter { lib ->
            lib.version != null
                    && settings.ignoreLibraries.none { pattern -> GlobMatcher.matches(pattern, lib.group, lib.artifact) }
        }

        val transitiveCandidates: List<FlatDependency> = if (settings.includeTransitives) {
            val flatDeps = metadata.flatDependencies
            if (flatDeps.isEmpty()) {
                logger.warn { "includeTransitives is enabled but no transitive dependencies found (enable transitive resolution first)" }
                diagnostics = diagnostics + Diagnostics.warning(
                    DiagnosticCodes.License.INCLUDE_TRANSITIVES_NOT_SUPPORTED,
                    "includeTransitives is enabled but no transitive dependencies available; ensure transitive resolution is enabled and runs before license check",
                    id, emptyMap(),
                )
                emptyList()
            } else {
                val directCoordinates = candidates.map { "${it.group}:${it.artifact}" }.toSet()
                flatDeps.filter { dep ->
                    !dep.isDirectDependency
                            && "${dep.group}:${dep.artifact}" !in directCoordinates
                            && settings.ignoreLibraries.none { pattern -> GlobMatcher.matches(pattern, dep.group, dep.artifact) }
                }.also { transitives ->
                    logger.info { "Including ${transitives.size} transitive dependencies in license check" }
                }
            }
        } else {
            emptyList()
        }

        if (candidates.isEmpty() && transitiveCandidates.isEmpty()) {
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

            val directResults: List<Pair<EffectiveLibrary, List<LicenseResult>>> = coroutineScope {
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

            val transitiveResults: List<Pair<FlatDependency, List<LicenseResult>>> = if (transitiveCandidates.isNotEmpty()) {
                coroutineScope {
                    transitiveCandidates.map { dep ->
                        async {
                            semaphore.withPermit {
                                val licenses = ctx.resolver.resolve(
                                    group = dep.group,
                                    artifact = dep.artifact,
                                    version = dep.version,
                                    declaredLicenseId = null,
                                )
                                dep to licenses
                            }
                        }
                    }.awaitAll()
                }
            } else {
                emptyList()
            }

            val allViolations = mutableListOf<LicenseViolation>()

            for ((lib, licenses) in directResults) {
                val policyResult = LicensePolicy.checkCompliance(lib, licenses, settings)
                allViolations.addAll(policyResult.violations)
                diagnostics = diagnostics + policyResult.diagnostics
            }

            for ((dep, licenses) in transitiveResults) {
                val policyResult = LicensePolicy.checkTransitiveCompliance(dep, licenses, settings)
                allViolations.addAll(policyResult.violations)
                diagnostics = diagnostics + policyResult.diagnostics
            }

            val totalChecked = directResults.size + transitiveResults.size
            val allResults = directResults.map { it.second } + transitiveResults.map { it.second }

            diagnostics = diagnostics + if (allViolations.isEmpty()) {
                Diagnostics.info(
                    DiagnosticCodes.License.ALL_COMPLIANT,
                    "License check completed: $totalChecked libraries (${directResults.size} direct, ${transitiveResults.size} transitive), all compliant",
                    id, mapOf("count" to totalChecked.toString(), "direct" to directResults.size.toString(), "transitive" to transitiveResults.size.toString()),
                )
            } else {
                Diagnostics.info(
                    DiagnosticCodes.License.CHECK_COMPLETE,
                    "License check completed: $totalChecked libraries (${directResults.size} direct, ${transitiveResults.size} transitive), ${allViolations.size} violation(s)",
                    id, mapOf("count" to totalChecked.toString(), "violations" to allViolations.size.toString()),
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
                val unknownCount = allResults.count { licenses -> licenses.all { it.category == LicenseCategory.UNKNOWN } }
                if (unknownCount > 0) {
                    diagnostics = diagnostics + Diagnostics.error(
                        DiagnosticCodes.License.UNKNOWN_FOUND,
                        "Unknown licenses found in $unknownCount library(ies)",
                        id, mapOf("count" to unknownCount.toString()),
                    )
                }
            }

            if (settings.failOnCopyleft) {
                val copyleftCount = allResults.count { licenses -> licenses.any { it.category.isCopyleft } }
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
