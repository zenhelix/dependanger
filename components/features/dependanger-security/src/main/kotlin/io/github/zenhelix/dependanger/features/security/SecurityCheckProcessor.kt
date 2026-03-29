package io.github.zenhelix.dependanger.features.security

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.zenhelix.dependanger.cache.CacheResult
import io.github.zenhelix.dependanger.core.DependangerPaths
import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.core.model.Severity
import io.github.zenhelix.dependanger.effective.DiagnosticCodes
import io.github.zenhelix.dependanger.effective.ProcessorIds
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.withExtension
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase
import io.github.zenhelix.dependanger.features.security.model.VulnerabilitiesExtensionKey
import io.github.zenhelix.dependanger.features.security.model.VulnerabilityInfo
import io.github.zenhelix.dependanger.features.security.model.VulnerabilitySeverity
import io.github.zenhelix.dependanger.http.client.HttpClientConfig
import io.github.zenhelix.dependanger.http.client.HttpClientFactory
import io.github.zenhelix.dependanger.http.client.RetryConfig
import io.ktor.client.HttpClient

private val logger = KotlinLogging.logger {}

private const val OSV_API_URL: String = "https://api.osv.dev"
private const val OSV_BATCH_SIZE: Int = 1000

public class SecurityCheckProcessor : EffectiveMetadataProcessor {
    override val id: String = ProcessorIds.SECURITY_CHECK
    override val phase: ProcessingPhase = ProcessingPhase.SECURITY_CHECK
    override val order: Int = phase.order
    override val isOptional: Boolean = true
    override val description: String = "Checks for known security vulnerabilities"

    override fun supports(context: ProcessingContext): Boolean =
        context[SecurityCheckSettingsKey]?.enabled == true

    override suspend fun process(metadata: EffectiveMetadata, context: ProcessingContext): EffectiveMetadata {
        val settings = context.require(SecurityCheckSettingsKey)
        val minSeverity = parseMinSeverity(settings.minSeverity)

        val candidates = metadata.libraries.values.filter { it.version != null }

        if (candidates.isEmpty()) {
            val diag = Diagnostics.info(
                DiagnosticCodes.Security.NO_VULNS,
                "No libraries to scan for vulnerabilities",
                id, emptyMap(),
            )
            return metadata.copy(diagnostics = metadata.diagnostics + diag)
                .withExtension(VulnerabilitiesExtensionKey, emptyList())
        }

        val cacheDir = settings.cacheDirectory
            ?: DependangerPaths.resolveInUserHome(DependangerPaths.SECURITY_CACHE_DIR)
        val cache = SecurityCache(
            cacheDirectory = cacheDir,
            ttlHours = settings.cacheTtlHours,
        )

        val cachedVulns = mutableListOf<VulnerabilityInfo>()
        val uncachedPackages = mutableListOf<PackageQuery>()

        for (lib in candidates) {
            val version = lib.version!!.value
            when (val cacheResult = cache.get(lib.group, lib.artifact, version)) {
                is CacheResult.Hit       -> {
                    cachedVulns.addAll(cacheResult.data)
                }

                is CacheResult.Corrupted -> {
                    logger.warn { "Corrupted security cache for ${lib.group}:${lib.artifact}:$version" }
                    uncachedPackages.add(PackageQuery(group = lib.group, artifact = lib.artifact, version = version))
                }

                is CacheResult.Miss      -> {
                    uncachedPackages.add(PackageQuery(group = lib.group, artifact = lib.artifact, version = version))
                }
            }
        }

        var diagnostics = metadata.diagnostics
        val fetchedVulns = mutableListOf<VulnerabilityInfo>()

        if (uncachedPackages.isNotEmpty()) {
            SecurityCheckContext(timeoutMs = settings.timeout).use { ctx ->
                when (val result = ctx.osvClient.queryBatch(uncachedPackages)) {
                    is OsvBatchResult.Success -> {
                        for ((i, vulns) in result.vulnerabilities.withIndex()) {
                            val pkg = uncachedPackages[i]
                            fetchedVulns.addAll(vulns)
                            try {
                                cache.put(pkg.group, pkg.artifact, pkg.version, vulns)
                            } catch (e: Exception) {
                                logger.warn { "Failed to write security cache for ${pkg.group}:${pkg.artifact}:${pkg.version}: ${e.message}" }
                            }
                        }
                    }

                    is OsvBatchResult.PartialSuccess -> {
                        // Process the successful results
                        for ((i, vulns) in result.vulnerabilities.withIndex()) {
                            val pkg = uncachedPackages[i]
                            fetchedVulns.addAll(vulns)
                            try {
                                cache.put(pkg.group, pkg.artifact, pkg.version, vulns)
                            } catch (e: Exception) {
                                logger.warn { "Failed to write security cache for ${pkg.group}:${pkg.artifact}:${pkg.version}: ${e.message}" }
                            }
                        }
                        // Handle the failed portion
                        val failedPackages = uncachedPackages.drop(result.vulnerabilities.size)
                        val failureDiagCode = if (result.isTimeout) DiagnosticCodes.Security.TIMEOUT else DiagnosticCodes.Security.API_UNREACHABLE
                        val failureResult = handleApiFailure(cache, failedPackages, failureDiagCode, "Partial failure: ${result.error}")
                        diagnostics += failureResult.diagnostics
                        fetchedVulns.addAll(failureResult.staleVulns)
                    }

                    is OsvBatchResult.Timeout        -> {
                        logger.warn { "OSV API request timed out: ${result.error}" }
                        val failureResult = handleApiFailure(
                            cache, uncachedPackages,
                            DiagnosticCodes.Security.TIMEOUT,
                            "OSV API request timed out after retries: ${result.error}",
                        )
                        diagnostics += failureResult.diagnostics
                        fetchedVulns.addAll(failureResult.staleVulns)
                    }

                    is OsvBatchResult.Failed  -> {
                        logger.warn { "OSV API query failed: ${result.error}" }
                        val failureResult = handleApiFailure(
                            cache, uncachedPackages,
                            DiagnosticCodes.Security.API_UNREACHABLE,
                            "OSV API unavailable and no cached data: ${result.error}",
                        )
                        diagnostics += failureResult.diagnostics
                        fetchedVulns.addAll(failureResult.staleVulns)
                    }
                }
            }
        }

        val ignoreSet = settings.ignoreVulnerabilities.toSet()
        val allVulns = (cachedVulns + fetchedVulns).filter { vuln ->
            vuln.id !in ignoreSet && vuln.aliases.none { it in ignoreSet }
        }

        diagnostics += buildScanDiagnostics(allVulns, candidates.size, minSeverity, settings.failOnVulnerability)

        return metadata.copy(diagnostics = diagnostics)
            .withExtension(VulnerabilitiesExtensionKey, allVulns)
    }

    private data class ApiFailureResult(
        val diagnostics: Diagnostics,
        val staleVulns: List<VulnerabilityInfo>,
    )

    private fun handleApiFailure(
        cache: SecurityCache,
        failedPackages: List<PackageQuery>,
        fallbackDiagnosticCode: String,
        fallbackMessage: String,
    ): ApiFailureResult {
        val staleVulns = failedPackages.flatMap { pkg ->
            cache.getStale(pkg.group, pkg.artifact, pkg.version) ?: emptyList()
        }
        var diagnostics = Diagnostics.warning(
            fallbackDiagnosticCode,
            fallbackMessage,
            id, emptyMap(),
        )
        if (staleVulns.isNotEmpty()) {
            diagnostics += Diagnostics.warning(
                DiagnosticCodes.Security.STALE_CACHE,
                "Using stale security cache due to OSV API failure",
                id, emptyMap(),
            )
        }
        return ApiFailureResult(diagnostics = diagnostics, staleVulns = staleVulns)
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

private class SecurityCheckContext(
    timeoutMs: Long,
) : AutoCloseable {

    val httpClient: HttpClient = HttpClientFactory.create {
        connectTimeoutMs = HttpClientConfig.DEFAULT_CONNECT_TIMEOUT_MS
        requestTimeoutMs = timeoutMs
        keepAliveMs = HttpClientConfig.DEFAULT_KEEP_ALIVE_MS
    }

    val osvClient: OsvApiClient = OsvApiClient(
        apiUrl = OSV_API_URL,
        httpClient = httpClient,
        batchSize = OSV_BATCH_SIZE,
        retryConfig = RetryConfig(),
        timeoutMs = timeoutMs,
    )

    override fun close() {
        httpClient.close()
    }
}
