package io.github.zenhelix.dependanger.features.security

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.zenhelix.dependanger.features.security.model.VulnerabilityInfo
import io.github.zenhelix.dependanger.features.security.model.VulnerabilitySeverity
import io.github.zenhelix.dependanger.http.client.HttpResult
import io.github.zenhelix.dependanger.http.client.RetryConfig
import io.github.zenhelix.dependanger.http.client.postWithRetry
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import kotlinx.serialization.json.Json
import us.springett.cvss.Cvss

private val logger = KotlinLogging.logger {}

public data class PackageQuery(
    val group: String,
    val artifact: String,
    val version: String,
)

public sealed interface OsvBatchResult {
    public data class Success(val vulnerabilities: List<List<VulnerabilityInfo>>) : OsvBatchResult
    public data class Failed(val error: String) : OsvBatchResult
}

public class OsvApiClient(
    private val apiUrl: String,
    private val httpClient: HttpClient,
    private val batchSize: Int,
    private val retryConfig: RetryConfig,
    private val timeoutMs: Long,
) {
    private val json: Json = Json {
        ignoreUnknownKeys = true
    }

    public suspend fun queryBatch(packages: List<PackageQuery>): OsvBatchResult {
        if (packages.isEmpty()) return OsvBatchResult.Success(vulnerabilities = emptyList())

        val allResults = mutableListOf<List<VulnerabilityInfo>>()

        for (batch in packages.chunked(batchSize)) {
            val request = OsvBatchRequest(
                queries = batch.map { pkg ->
                    OsvQuery(
                        version = pkg.version,
                        pkg = OsvPackage(
                            name = "${pkg.group}:${pkg.artifact}",
                            ecosystem = "Maven",
                        ),
                    )
                },
            )

            val requestBody = json.encodeToString(OsvBatchRequest.serializer(), request)

            val httpResult = httpClient.postWithRetry(
                url = "$apiUrl/v1/querybatch",
                retryConfig = retryConfig,
            ) {
                timeout { requestTimeoutMillis = timeoutMs }
                setBody(TextContent(requestBody, ContentType.Application.Json))
            }

            when (httpResult) {
                is HttpResult.Success      -> {
                    val response = try {
                        json.decodeFromString<OsvBatchResponse>(httpResult.data)
                    } catch (e: Exception) {
                        logger.warn { "Failed to parse OSV API response: ${e.message}" }
                        return OsvBatchResult.Failed(error = "Failed to parse OSV API response: ${e.message}")
                    }

                    if (response.results.size != batch.size) {
                        logger.warn { "OSV API returned ${response.results.size} results for ${batch.size} queries" }
                        return OsvBatchResult.Failed(
                            error = "OSV API response size mismatch: expected ${batch.size}, got ${response.results.size}"
                        )
                    }

                    val batchVulns = response.results.mapIndexed { index, queryResult ->
                        val pkg = batch[index]
                        queryResult.vulns?.map { vuln ->
                            mapToVulnerabilityInfo(vuln, pkg)
                        } ?: emptyList()
                    }
                    allResults.addAll(batchVulns)
                }

                is HttpResult.NotFound     -> {
                    allResults.addAll(batch.map { emptyList() })
                }

                is HttpResult.RateLimited  -> {
                    return OsvBatchResult.Failed(error = "Rate limited by OSV API")
                }

                is HttpResult.AuthRequired -> {
                    return OsvBatchResult.Failed(error = "Authentication required for OSV API: ${httpResult.url}")
                }

                is HttpResult.Failed       -> {
                    return OsvBatchResult.Failed(error = httpResult.error)
                }
            }
        }

        return OsvBatchResult.Success(vulnerabilities = allResults)
    }

    private fun mapToVulnerabilityInfo(vuln: OsvVulnerability, pkg: PackageQuery): VulnerabilityInfo {
        val cvssEntry = vuln.severity?.firstOrNull { it.type == "CVSS_V4" }
            ?: vuln.severity?.firstOrNull { it.type == "CVSS_V3" }
            ?: vuln.severity?.firstOrNull { it.type == "CVSS_V2" }
        val cvssScore = cvssEntry?.let { parseCvssScore(it.score) }
        val severity = cvssScoreToSeverity(cvssScore)
        val fixedVersion = vuln.affected
            ?.flatMap { it.ranges ?: emptyList() }
            ?.flatMap { it.events ?: emptyList() }
            ?.firstOrNull { it.fixed != null }
            ?.fixed

        return VulnerabilityInfo(
            id = vuln.id,
            aliases = vuln.aliases ?: emptyList(),
            summary = vuln.summary ?: "",
            severity = severity,
            cvssScore = cvssScore,
            fixedVersion = fixedVersion,
            url = vuln.references?.firstOrNull()?.url,
            affectedGroup = pkg.group,
            affectedArtifact = pkg.artifact,
            affectedVersion = pkg.version,
        )
    }
}

private fun parseCvssScore(vector: String): Double? {
    vector.toDoubleOrNull()?.let { score ->
        if (score in 0.0..10.0) return score
        logger.warn { "CVSS score out of valid range (0.0-10.0): $score" }
        return null
    }

    return try {
        val cvss = Cvss.fromVector(vector)
        cvss.calculateScore().baseScore
    } catch (e: Exception) {
        logger.warn { "Failed to parse CVSS vector: $vector (${e.message})" }
        null
    }
}

private fun cvssScoreToSeverity(score: Double?): VulnerabilitySeverity = when {
    score == null -> VulnerabilitySeverity.UNKNOWN
    score >= 9.0  -> VulnerabilitySeverity.CRITICAL
    score >= 7.0  -> VulnerabilitySeverity.HIGH
    score >= 4.0  -> VulnerabilitySeverity.MEDIUM
    score >= 0.1  -> VulnerabilitySeverity.LOW
    else          -> VulnerabilitySeverity.NONE
}
