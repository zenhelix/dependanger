package io.github.zenhelix.dependanger.features.security

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.zenhelix.dependanger.cache.CacheKeyResolver
import io.github.zenhelix.dependanger.cache.CacheResult
import io.github.zenhelix.dependanger.cache.DirBasedCache
import io.github.zenhelix.dependanger.core.DependangerPaths
import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.core.model.Severity
import io.github.zenhelix.dependanger.core.pipeline.ProcessingContextKey
import io.github.zenhelix.dependanger.effective.DiagnosticCodes
import io.github.zenhelix.dependanger.effective.ProcessorIds
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.pipeline.ExecutionMode
import io.github.zenhelix.dependanger.effective.pipeline.OrderConstraint
import io.github.zenhelix.dependanger.effective.pipeline.ParallelResult
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase
import io.github.zenhelix.dependanger.feature.model.FeatureProcessorIds
import io.github.zenhelix.dependanger.feature.model.security.VulnerabilitiesExtensionKey
import io.github.zenhelix.dependanger.feature.model.security.VulnerabilityInfo
import io.github.zenhelix.dependanger.feature.model.security.VulnerabilitySeverity
import io.github.zenhelix.dependanger.feature.model.settings.security.SecurityCheckSettings
import io.github.zenhelix.dependanger.feature.model.settings.security.SecurityCheckSettingsKey
import io.github.zenhelix.dependanger.feature.support.AbstractParallelFeatureProcessor
import io.github.zenhelix.dependanger.http.client.HttpClientFactory
import io.github.zenhelix.dependanger.osv.client.OsvClient
import io.github.zenhelix.dependanger.osv.client.OsvClientConfig
import io.github.zenhelix.dependanger.osv.client.model.OsvBatchResult
import io.github.zenhelix.dependanger.osv.client.model.OsvPackageQuery
import io.github.zenhelix.dependanger.osv.client.model.OsvVulnerabilityData
import kotlinx.serialization.builtins.ListSerializer

private val logger = KotlinLogging.logger {}

public class SecurityCheckProcessor : AbstractParallelFeatureProcessor<SecurityCheckSettings>() {
    override val id: String = PROCESSOR_ID
    override val phase: ProcessingPhase = PHASE
    override val constraints: Set<OrderConstraint> = setOf(OrderConstraint.runsAfter(ProcessorIds.VERSION_RESOLVER))
    override val isOptional: Boolean = true
    override val description: String = "Checks for known security vulnerabilities"

    override val settingsKey: ProcessingContextKey<SecurityCheckSettings> = SecurityCheckSettingsKey

    public companion object {
        public const val PROCESSOR_ID: String = FeatureProcessorIds.SECURITY_CHECK
        public val PHASE: ProcessingPhase = ProcessingPhase("SECURITY_CHECK", ExecutionMode.PARALLEL_IO)
    }

    override fun supports(context: ProcessingContext): Boolean =
        context[SecurityCheckSettingsKey]?.enabled == true

    override suspend fun executeWithInfrastructure(
        metadata: EffectiveMetadata,
        context: ProcessingContext,
        settings: SecurityCheckSettings,
        httpClientFactory: HttpClientFactory,
    ): ParallelResult {
        val minSeverity = parseMinSeverity(settings.minSeverity)

        val candidates = metadata.libraries.values.filter { it.version.isResolved }

        if (candidates.isEmpty()) {
            return ParallelResult.emptyResult(DiagnosticCodes.Security.NO_VULNS, "No libraries to scan for vulnerabilities", id, VulnerabilitiesExtensionKey)
        }

        val cacheDir = settings.cacheDirectory
            ?: DependangerPaths.resolveInUserHome(DependangerPaths.SECURITY_CACHE_DIR)
        val cache = DirBasedCache(
            cacheDirectory = cacheDir,
            ttlHours = settings.cacheTtlHours,
            ttlSnapshotHours = settings.cacheTtlHours,
            contentSerializer = ListSerializer(VulnerabilityInfo.serializer()),
            keyResolver = CacheKeyResolver.FlatGroupArtifactVersion,
        )

        val cachedVulns = mutableListOf<VulnerabilityInfo>()
        val uncachedPackages = mutableListOf<OsvPackageQuery>()

        for (lib in candidates) {
            val version = lib.version.valueOrNull!!
            when (val cacheResult = cache.get(lib.group, lib.artifact, version)) {
                is CacheResult.Hit       -> {
                    cachedVulns.addAll(cacheResult.data)
                }

                is CacheResult.Corrupted -> {
                    logger.warn { "Corrupted security cache for ${lib.group}:${lib.artifact}:$version" }
                    uncachedPackages.add(OsvPackageQuery(group = lib.group, artifact = lib.artifact, version = version))
                }

                is CacheResult.Miss      -> {
                    uncachedPackages.add(OsvPackageQuery(group = lib.group, artifact = lib.artifact, version = version))
                }
            }
        }

        val diagnostics = Diagnostics.builder()
        val fetchedVulns = mutableListOf<VulnerabilityInfo>()

        if (uncachedPackages.isNotEmpty()) {
            SecurityCheckContext(httpClientFactory = httpClientFactory, timeoutMs = settings.timeout).use { ctx ->
                when (val result = ctx.osvClient.queryBatch(uncachedPackages)) {
                    is OsvBatchResult.Success -> {
                        fetchedVulns.addAll(processSuccessfulVulnerabilities(result.vulnerabilities, uncachedPackages, cache))
                    }

                    is OsvBatchResult.PartialSuccess -> {
                        fetchedVulns.addAll(processSuccessfulVulnerabilities(result.vulnerabilities, uncachedPackages, cache))
                        val failedPackages = uncachedPackages.drop(result.vulnerabilities.size)
                        val failureDiagCode = if (result.isTimeout) DiagnosticCodes.Security.TIMEOUT else DiagnosticCodes.Security.API_UNREACHABLE
                        val failureResult = handleApiFailure(cache, failedPackages, failureDiagCode, "Partial failure: ${result.error}")
                        diagnostics.add(failureResult.diagnostics)
                        fetchedVulns.addAll(failureResult.staleVulns)
                    }

                    is OsvBatchResult.Timeout        -> {
                        logger.warn { "OSV API request timed out: ${result.error}" }
                        val failureResult = handleApiFailure(
                            cache, uncachedPackages,
                            DiagnosticCodes.Security.TIMEOUT,
                            "OSV API request timed out after retries: ${result.error}",
                        )
                        diagnostics.add(failureResult.diagnostics)
                        fetchedVulns.addAll(failureResult.staleVulns)
                    }

                    is OsvBatchResult.Failed  -> {
                        logger.warn { "OSV API query failed: ${result.error}" }
                        val failureResult = handleApiFailure(
                            cache, uncachedPackages,
                            DiagnosticCodes.Security.API_UNREACHABLE,
                            "OSV API unavailable and no cached data: ${result.error}",
                        )
                        diagnostics.add(failureResult.diagnostics)
                        fetchedVulns.addAll(failureResult.staleVulns)
                    }
                }
            }
        }

        val ignoreSet = settings.ignoreVulnerabilities.toSet()
        val allVulns = (cachedVulns + fetchedVulns).filter { vuln ->
            vuln.id !in ignoreSet && vuln.aliases.none { it in ignoreSet }
        }

        diagnostics.add(buildScanDiagnostics(allVulns, candidates.size, minSeverity, settings.failOnVulnerability))

        return ParallelResult(diagnostics.build(), mapOf(VulnerabilitiesExtensionKey to allVulns))
    }

    private fun processSuccessfulVulnerabilities(
        vulnerabilities: List<List<OsvVulnerabilityData>>,
        packages: List<OsvPackageQuery>,
        cache: DirBasedCache<List<VulnerabilityInfo>>,
    ): List<VulnerabilityInfo> = buildList {
        for ((i, osvVulns) in vulnerabilities.withIndex()) {
            val pkg = packages[i]
            val vulns = osvVulns.map { mapToVulnerabilityInfo(it, pkg) }
            addAll(vulns)
            try {
                cache.put(pkg.group, pkg.artifact, pkg.version, vulns)
            } catch (e: Exception) {
                logger.warn { "Failed to write security cache for ${pkg.group}:${pkg.artifact}:${pkg.version}: ${e.message}" }
            }
        }
    }

    private data class ApiFailureResult(
        val diagnostics: Diagnostics,
        val staleVulns: List<VulnerabilityInfo>,
    )

    private fun handleApiFailure(
        cache: DirBasedCache<List<VulnerabilityInfo>>,
        failedPackages: List<OsvPackageQuery>,
        fallbackDiagnosticCode: String,
        fallbackMessage: String,
    ): ApiFailureResult {
        val staleVulns = failedPackages.flatMap { pkg ->
            cache.getStale(pkg.group, pkg.artifact, pkg.version) ?: emptyList()
        }
        val diagnostics = Diagnostics.builder()
        diagnostics.warning(
            fallbackDiagnosticCode,
            fallbackMessage,
            id, emptyMap(),
        )
        if (staleVulns.isNotEmpty()) {
            diagnostics.warning(
                DiagnosticCodes.Security.STALE_CACHE,
                "Using stale security cache due to OSV API failure",
                id, emptyMap(),
            )
        }
        return ApiFailureResult(diagnostics = diagnostics.build(), staleVulns = staleVulns)
    }

    private fun buildScanDiagnostics(
        vulnerabilities: List<VulnerabilityInfo>,
        libraryCount: Int,
        minSeverity: VulnerabilitySeverity,
        failOnVulnerability: Severity,
    ): Diagnostics {
        if (vulnerabilities.isEmpty()) {
            return Diagnostics.info(
                DiagnosticCodes.Security.NO_VULNS,
                "No vulnerabilities found in $libraryCount libraries",
                id, mapOf("libraryCount" to libraryCount.toString()),
            )
        }

        var result = Diagnostics.info(
            DiagnosticCodes.Security.SCAN_COMPLETE,
            "Scanned $libraryCount libraries, found ${vulnerabilities.size} vulnerabilities",
            id,
            mapOf(
                "libraryCount" to libraryCount.toString(),
                "vulnCount" to vulnerabilities.size.toString(),
            ),
        )

        val hasUnknownCvss = vulnerabilities.any { it.severity == VulnerabilitySeverity.UNKNOWN }
        if (hasUnknownCvss) {
            result += Diagnostics.warning(
                DiagnosticCodes.Security.INVALID_CVSS,
                "Some vulnerabilities have unknown severity (CVSS parsing failed or no score available)",
                id, emptyMap(),
            )
        }

        val hasCvssV2Only = vulnerabilities.any { it.cvssVersion == "CVSS_V2" }
        if (hasCvssV2Only) {
            result += Diagnostics.info(
                DiagnosticCodes.Security.CVSS_V2,
                "Some vulnerabilities only have CVSS v2 scores (v2 formula used for calculation)",
                id, emptyMap(),
            )
        }

        val actionableVulns = vulnerabilities.filter { it.severity.meetsThreshold(minSeverity) }
        if (actionableVulns.isNotEmpty()) {
            val message = "${actionableVulns.size} vulnerability(ies) with severity >= $minSeverity found"
            val details = mapOf("count" to actionableVulns.size.toString(), "minSeverity" to minSeverity.name)
            result += when (failOnVulnerability) {
                Severity.ERROR   -> Diagnostics.error(DiagnosticCodes.Security.VULNERABILITY_FOUND, message, id, details)
                Severity.WARNING -> Diagnostics.warning(DiagnosticCodes.Security.VULNERABILITY_FOUND, message, id, details)
                Severity.INFO    -> Diagnostics.info(DiagnosticCodes.Security.VULNERABILITY_FOUND, message, id, details)
            }
        }

        return result
    }

    private fun parseMinSeverity(value: String): VulnerabilitySeverity =
        try {
            VulnerabilitySeverity.fromString(value)
        } catch (e: IllegalArgumentException) {
            logger.warn { "Invalid minSeverity value '$value', falling back to HIGH: ${e.message}" }
            VulnerabilitySeverity.HIGH
        }
}

private fun mapToVulnerabilityInfo(vuln: OsvVulnerabilityData, pkg: OsvPackageQuery): VulnerabilityInfo =
    VulnerabilityInfo(
        id = vuln.id,
        aliases = vuln.aliases,
        summary = vuln.summary,
        severity = cvssScoreToSeverity(vuln.cvssScore),
        cvssScore = vuln.cvssScore,
        cvssVersion = vuln.cvssVersion,
        fixedVersion = vuln.fixedVersion,
        url = vuln.referenceUrl,
        affectedGroup = pkg.group,
        affectedArtifact = pkg.artifact,
        affectedVersion = pkg.version,
    )

private fun cvssScoreToSeverity(score: Double?): VulnerabilitySeverity = when {
    score == null -> VulnerabilitySeverity.UNKNOWN
    score >= 9.0  -> VulnerabilitySeverity.CRITICAL
    score >= 7.0  -> VulnerabilitySeverity.HIGH
    score >= 4.0  -> VulnerabilitySeverity.MEDIUM
    score >= 0.1  -> VulnerabilitySeverity.LOW
    else          -> VulnerabilitySeverity.NONE
}

private class SecurityCheckContext(
    httpClientFactory: HttpClientFactory,
    timeoutMs: Long,
) : AutoCloseable {

    val osvClient: OsvClient = OsvClient(
        OsvClientConfig(timeoutMs = timeoutMs),
        httpClientFactory,
    )

    override fun close() {
        osvClient.close()
    }
}
