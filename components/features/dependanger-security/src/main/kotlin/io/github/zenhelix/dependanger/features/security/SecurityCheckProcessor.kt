package io.github.zenhelix.dependanger.features.security

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.core.model.Severity
import io.github.zenhelix.dependanger.effective.DiagnosticCodes
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.withExtension
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase
import io.github.zenhelix.dependanger.features.security.model.VulnerabilitiesExtensionKey
import io.github.zenhelix.dependanger.features.security.model.VulnerabilityInfo
import io.github.zenhelix.dependanger.features.security.model.VulnerabilitySeverity
import io.github.zenhelix.dependanger.http.client.HttpClientFactory
import io.github.zenhelix.dependanger.http.client.RetryConfig
import io.ktor.client.HttpClient

private val logger = KotlinLogging.logger {}

private const val OSV_API_URL: String = "https://api.osv.dev"
private const val OSV_BATCH_SIZE: Int = 1000
private const val HTTP_CONNECT_TIMEOUT_MS: Long = 30_000L
private const val HTTP_KEEP_ALIVE_MS: Long = 5_000L

public class SecurityCheckProcessor : EffectiveMetadataProcessor {
    override val id: String = "security-check"
    override val phase: ProcessingPhase = ProcessingPhase.SECURITY_CHECK
    override val order: Int = phase.order
    override val isOptional: Boolean = true
    override val description: String = "Checks for known security vulnerabilities"

    override fun supports(context: ProcessingContext): Boolean =
        context.settings.securityCheck.enabled

    override suspend fun process(metadata: EffectiveMetadata, context: ProcessingContext): EffectiveMetadata {
        val settings = context.settings.securityCheck

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
            ?: (System.getProperty("user.home") + "/.dependanger/cache/security")
        val cache = SecurityCache(
            cacheDirectory = cacheDir,
            ttlHours = settings.cacheTtlHours,
        )

        val cachedVulns = mutableListOf<VulnerabilityInfo>()
        val uncachedPackages = mutableListOf<PackageQuery>()

        for (lib in candidates) {
            val version = lib.version!!.value
            when (val cacheResult = cache.get(lib.group, lib.artifact, version)) {
                is SecurityCacheResult.Hit       -> {
                    cachedVulns.addAll(cacheResult.vulnerabilities)
                }

                is SecurityCacheResult.Corrupted -> {
                    logger.warn { "Corrupted security cache for ${lib.group}:${lib.artifact}:$version" }
                    uncachedPackages.add(PackageQuery(group = lib.group, artifact = lib.artifact, version = version))
                }

                is SecurityCacheResult.Miss      -> {
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

                    is OsvBatchResult.Failed  -> {
                        logger.warn { "OSV API query failed: ${result.error}" }
                        var usedStaleCache = false
                        for (pkg in uncachedPackages) {
                            val stale = cache.getStale(pkg.group, pkg.artifact, pkg.version)
                            if (stale != null) {
                                fetchedVulns.addAll(stale)
                                usedStaleCache = true
                            }
                        }
                        diagnostics += if (usedStaleCache) {
                            Diagnostics.warning(
                                DiagnosticCodes.Security.STALE_CACHE,
                                "Using stale security cache due to OSV API failure",
                                id, emptyMap(),
                            )
                        } else {
                            Diagnostics.warning(
                                DiagnosticCodes.Security.API_UNREACHABLE,
                                "OSV API unavailable and no cached data: ${result.error}",
                                id, emptyMap(),
                            )
                        }
                    }
                }
            }
        }

        val ignoreSet = settings.ignoreVulnerabilities.toSet()
        val allVulns = (cachedVulns + fetchedVulns).filter { vuln ->
            vuln.id !in ignoreSet && vuln.aliases.none { it in ignoreSet }
        }

        diagnostics += buildScanDiagnostics(allVulns, candidates.size, settings.failOnVulnerability)

        return metadata.copy(diagnostics = diagnostics)
            .withExtension(VulnerabilitiesExtensionKey, allVulns)
    }

    private fun buildScanDiagnostics(
        vulnerabilities: List<VulnerabilityInfo>,
        libraryCount: Int,
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

        result += when (failOnVulnerability) {
            Severity.ERROR   -> Diagnostics.error(
                DiagnosticCodes.Security.VULNERABILITY_FOUND,
                "${vulnerabilities.size} vulnerability(ies) found",
                id, mapOf("count" to vulnerabilities.size.toString()),
            )

            Severity.WARNING -> Diagnostics.warning(
                DiagnosticCodes.Security.VULNERABILITY_FOUND,
                "${vulnerabilities.size} vulnerability(ies) found",
                id, mapOf("count" to vulnerabilities.size.toString()),
            )

            Severity.INFO    -> Diagnostics.info(
                DiagnosticCodes.Security.VULNERABILITY_FOUND,
                "${vulnerabilities.size} vulnerability(ies) found",
                id, mapOf("count" to vulnerabilities.size.toString()),
            )
        }

        return result
    }
}

private class SecurityCheckContext(
    timeoutMs: Long,
) : AutoCloseable {

    val httpClient: HttpClient = HttpClientFactory.create {
        connectTimeoutMs = HTTP_CONNECT_TIMEOUT_MS
        requestTimeoutMs = timeoutMs
        keepAliveMs = HTTP_KEEP_ALIVE_MS
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
