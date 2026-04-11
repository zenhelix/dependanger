package io.github.zenhelix.dependanger.features.license

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.core.model.DiagnosticsBuilder
import io.github.zenhelix.dependanger.core.model.MavenCoordinate
import io.github.zenhelix.dependanger.core.pipeline.ProcessingContextKey
import io.github.zenhelix.dependanger.core.util.GlobMatcher
import io.github.zenhelix.dependanger.effective.DiagnosticCodes
import io.github.zenhelix.dependanger.effective.ProcessorIds
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.pipeline.ExecutionMode
import io.github.zenhelix.dependanger.effective.pipeline.OrderConstraint
import io.github.zenhelix.dependanger.effective.pipeline.ParallelResult
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase
import io.github.zenhelix.dependanger.feature.model.FeatureProcessorIds
import io.github.zenhelix.dependanger.feature.model.license.LicenseCategory
import io.github.zenhelix.dependanger.feature.model.license.LicenseViolation
import io.github.zenhelix.dependanger.feature.model.license.LicenseViolationType
import io.github.zenhelix.dependanger.feature.model.license.LicenseViolationsExtensionKey
import io.github.zenhelix.dependanger.feature.model.license.isCopyleft
import io.github.zenhelix.dependanger.feature.model.settings.license.LicenseCheckSettings
import io.github.zenhelix.dependanger.feature.model.settings.license.LicenseCheckSettingsKey
import io.github.zenhelix.dependanger.feature.model.transitive.FlatDependency
import io.github.zenhelix.dependanger.feature.model.transitive.flatDependencies
import io.github.zenhelix.dependanger.feature.support.AbstractParallelMavenProcessor
import io.github.zenhelix.dependanger.feature.support.NetworkProcessorInfrastructure
import io.github.zenhelix.dependanger.features.license.model.LicenseResult
import io.github.zenhelix.dependanger.features.license.spi.LicenseSourceProvidersKey
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

private val logger = KotlinLogging.logger {}

public class LicenseCheckProcessor : AbstractParallelMavenProcessor<LicenseCheckSettings>() {
    override val id: String = PROCESSOR_ID
    override val phase: ProcessingPhase = PHASE
    override val constraints: Set<OrderConstraint> = setOf(OrderConstraint.runsAfter(ProcessorIds.VERSION_RESOLVER))
    override val isOptional: Boolean = true
    override val description: String = "Checks library license compliance"
    override val settingsKey: ProcessingContextKey<LicenseCheckSettings> = LicenseCheckSettingsKey

    public companion object {
        public const val PROCESSOR_ID: String = FeatureProcessorIds.LICENSE_CHECK
        public val PHASE: ProcessingPhase = ProcessingPhase("LICENSE_CHECK", ExecutionMode.PARALLEL_IO)
    }

    override fun supports(context: ProcessingContext): Boolean =
        context[LicenseCheckSettingsKey]?.enabled == true

    override suspend fun executeWithMavenInfrastructure(
        metadata: EffectiveMetadata,
        context: ProcessingContext,
        settings: LicenseCheckSettings,
        infrastructure: NetworkProcessorInfrastructure,
    ): ParallelResult {
        val customProviders = context[LicenseSourceProvidersKey] ?: emptyList()
        if (customProviders.isNotEmpty()) {
            logger.info { "Using ${customProviders.size} custom license source provider(s): ${customProviders.map { it.sourceId }}" }
        }

        val diagnostics = Diagnostics.builder()

        val candidates = metadata.libraries.values.filter { lib ->
            lib.version.isResolved
                    && settings.ignoreLibraries.none { pattern -> GlobMatcher.matches(pattern, lib.coordinate) }
        }

        val transitiveCandidates: List<FlatDependency> = if (settings.includeTransitives) {
            val flatDeps = metadata.flatDependencies
            if (flatDeps.isEmpty()) {
                logger.warn { "includeTransitives is enabled but no transitive dependencies found (enable transitive resolution first)" }
                diagnostics.warning(
                    DiagnosticCodes.License.INCLUDE_TRANSITIVES_NOT_SUPPORTED,
                    "includeTransitives is enabled but no transitive dependencies available; ensure transitive resolution is enabled and runs before license check",
                    id, emptyMap(),
                )
                emptyList()
            } else {
                val directCoordinates = candidates.map { it.coordinate }.toSet()
                flatDeps.filter { dep ->
                    !dep.isDirectDependency
                            && dep.coordinate !in directCoordinates
                            && settings.ignoreLibraries.none { pattern -> GlobMatcher.matches(pattern, dep.coordinate) }
                }.also { transitives ->
                    logger.info { "Including ${transitives.size} transitive dependencies in license check" }
                }
            }
        } else {
            emptyList()
        }

        if (candidates.isEmpty() && transitiveCandidates.isEmpty()) {
            return ParallelResult.emptyResult(
                DiagnosticCodes.License.NO_LIBS,
                "No libraries to check for licenses",
                id,
                LicenseViolationsExtensionKey,
                diagnostics.build()
            )
        }

        LicenseCheckContext(
            repositories = infrastructure.repositories,
            credentialsProvider = infrastructure.credentialsProvider,
            httpClientFactory = infrastructure.httpClientFactory,
            cacheDirectory = settings.cacheDirectory,
            cacheTtlHours = settings.cacheTtlHours,
            readTimeoutMs = settings.timeout,
            customProviders = customProviders,
        ).use { ctx ->
            val semaphore = Semaphore(settings.parallelism)

            val directResults = resolveLicensesInParallel(
                candidates = candidates,
                resolver = ctx.resolver,
                semaphore = semaphore,
                extractCoordinates = { lib -> lib.coordinate to lib.version.requireValue() },
                extractDeclaredLicense = { lib -> lib.license?.id },
            )

            val transitiveResults = resolveLicensesInParallel(
                candidates = transitiveCandidates,
                resolver = ctx.resolver,
                semaphore = semaphore,
                extractCoordinates = { dep -> dep.coordinate to dep.version },
                extractDeclaredLicense = { null },
            )

            val allViolations = mutableListOf<LicenseViolation>()

            for ((lib, licenses) in directResults) {
                val policyResult = LicensePolicy.checkCompliance(lib, licenses, settings)
                allViolations.addAll(policyResult.violations)
                diagnostics.add(policyResult.diagnostics)
            }

            for ((dep, licenses) in transitiveResults) {
                val policyResult = LicensePolicy.checkTransitiveCompliance(dep, licenses, settings)
                allViolations.addAll(policyResult.violations)
                diagnostics.add(policyResult.diagnostics)
            }

            val totalChecked = directResults.size + transitiveResults.size
            val allResults = directResults.map { it.second } + transitiveResults.map { it.second }

            if (allViolations.isEmpty()) {
                diagnostics.info(
                    DiagnosticCodes.License.ALL_COMPLIANT,
                    "License check completed: $totalChecked libraries (${directResults.size} direct, ${transitiveResults.size} transitive), all compliant",
                    id, mapOf("count" to totalChecked.toString(), "direct" to directResults.size.toString(), "transitive" to transitiveResults.size.toString()),
                )
            } else {
                diagnostics.info(
                    DiagnosticCodes.License.CHECK_COMPLETE,
                    "License check completed: $totalChecked libraries (${directResults.size} direct, ${transitiveResults.size} transitive), ${allViolations.size} violation(s)",
                    id, mapOf("count" to totalChecked.toString(), "violations" to allViolations.size.toString()),
                )
            }

            reportPolicyViolations(allViolations, allResults, settings, diagnostics)

            return ParallelResult(diagnostics.build(), mapOf(LicenseViolationsExtensionKey to allViolations))
        }
    }

    private suspend fun <T> resolveLicensesInParallel(
        candidates: List<T>,
        resolver: LicenseResolver,
        semaphore: Semaphore,
        extractCoordinates: (T) -> Pair<MavenCoordinate, String>,
        extractDeclaredLicense: (T) -> String?,
    ): List<Pair<T, List<LicenseResult>>> = if (candidates.isEmpty()) {
        emptyList()
    } else {
        coroutineScope {
            candidates.map { candidate ->
                async {
                    semaphore.withPermit {
                        val (coordinate, version) = extractCoordinates(candidate)
                        val licenses = resolver.resolve(
                            coordinate = coordinate,
                            version = version,
                            declaredLicenseId = extractDeclaredLicense(candidate),
                        )
                        candidate to licenses
                    }
                }
            }.awaitAll()
        }
    }

    private fun reportPolicyViolations(
        allViolations: List<LicenseViolation>,
        allResults: List<List<LicenseResult>>,
        settings: LicenseCheckSettings,
        diagnostics: DiagnosticsBuilder,
    ) {
        if (settings.failOnDenied) {
            val deniedCount = allViolations.count { it.violationType == LicenseViolationType.DENIED }
            if (deniedCount > 0) {
                diagnostics.error(
                    DiagnosticCodes.License.DENIED_FOUND,
                    "Denied licenses found in $deniedCount library(ies)",
                    id, mapOf("count" to deniedCount.toString()),
                )
            }
        }

        if (settings.failOnUnknown || settings.failOnCopyleft) {
            var unknownCount = 0
            var copyleftCount = 0
            for (licenses in allResults) {
                if (licenses.all { it.category == LicenseCategory.UNKNOWN }) unknownCount++
                if (licenses.any { it.category.isCopyleft }) copyleftCount++
            }

            if (settings.failOnUnknown && unknownCount > 0) {
                diagnostics.error(
                    DiagnosticCodes.License.UNKNOWN_FOUND,
                    "Unknown licenses found in $unknownCount library(ies)",
                    id, mapOf("count" to unknownCount.toString()),
                )
            }

            if (settings.failOnCopyleft && copyleftCount > 0) {
                diagnostics.error(
                    DiagnosticCodes.License.COPYLEFT_FOUND,
                    "Copyleft licenses found in $copyleftCount library(ies)",
                    id, mapOf("count" to copyleftCount.toString()),
                )
            }
        }
    }
}
