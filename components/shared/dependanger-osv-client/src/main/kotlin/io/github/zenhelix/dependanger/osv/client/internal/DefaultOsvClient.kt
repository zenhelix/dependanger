package io.github.zenhelix.dependanger.osv.client.internal

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.zenhelix.dependanger.http.client.HttpClientFactory
import io.github.zenhelix.dependanger.http.client.HttpResult
import io.github.zenhelix.dependanger.http.client.createDefault
import io.github.zenhelix.dependanger.http.client.postWithRetry
import io.github.zenhelix.dependanger.osv.client.OsvBatchResult
import io.github.zenhelix.dependanger.osv.client.OsvClient
import io.github.zenhelix.dependanger.osv.client.OsvClientConfig
import io.github.zenhelix.dependanger.osv.client.OsvPackageQuery
import io.github.zenhelix.dependanger.osv.client.OsvVulnerabilityData
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.timeout
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import kotlinx.serialization.json.Json
import us.springett.cvss.Cvss

private val logger = KotlinLogging.logger {}

private val CVSS_PRIORITY: List<String> = listOf("CVSS_V4", "CVSS_V3", "CVSS_V2")
private const val OSV_MAVEN_ECOSYSTEM = "Maven"
private val lenientJson: Json = Json { ignoreUnknownKeys = true }

internal class DefaultOsvClient(
    private val config: OsvClientConfig,
    httpClientFactory: HttpClientFactory,
) : OsvClient {

    private val httpClient = httpClientFactory.createDefault(config.timeoutMs)

    override suspend fun queryBatch(packages: List<OsvPackageQuery>): OsvBatchResult {
        if (packages.isEmpty()) return OsvBatchResult.Success(vulnerabilities = emptyList())

        val allResults = mutableListOf<List<OsvVulnerabilityData>>()
        val batches = packages.chunked(config.batchSize)

        for ((batchIndex, batch) in batches.withIndex()) {
            fun failureResult(error: String, isTimeout: Boolean): OsvBatchResult {
                val failedPackageCount = batches.drop(batchIndex).sumOf { it.size }
                return if (allResults.isNotEmpty()) {
                    OsvBatchResult.PartialSuccess(
                        vulnerabilities = allResults.toList(),
                        failedPackageCount = failedPackageCount,
                        error = error,
                        isTimeout = isTimeout,
                    )
                } else if (isTimeout) {
                    OsvBatchResult.Timeout(error = error)
                } else {
                    OsvBatchResult.Failed(error = error)
                }
            }

            val request = OsvBatchRequest(
                queries = batch.map { pkg ->
                    OsvQuery(
                        version = pkg.version,
                        pkg = OsvPackage(
                            name = "${pkg.group}:${pkg.artifact}",
                            ecosystem = OSV_MAVEN_ECOSYSTEM,
                        ),
                    )
                },
            )

            val requestBody = lenientJson.encodeToString(OsvBatchRequest.serializer(), request)

            val httpResult = httpClient.postWithRetry(
                url = "${config.apiUrl}/v1/querybatch",
                retryConfig = config.retryConfig,
            ) {
                timeout { requestTimeoutMillis = config.timeoutMs }
                setBody(TextContent(requestBody, ContentType.Application.Json))
            }

            when (httpResult) {
                is HttpResult.Success      -> {
                    val response = try {
                        lenientJson.decodeFromString<OsvBatchResponse>(httpResult.data)
                    } catch (e: Exception) {
                        logger.warn { "Failed to parse OSV API response: ${e.message}" }
                        return failureResult("Failed to parse OSV API response: ${e.message}", isTimeout = false)
                    }

                    if (response.results.size != batch.size) {
                        logger.warn { "OSV API returned ${response.results.size} results for ${batch.size} queries" }
                        return failureResult(
                            "OSV API response size mismatch: expected ${batch.size}, got ${response.results.size}",
                            isTimeout = false,
                        )
                    }

                    val batchVulns = response.results.mapIndexed { index, queryResult ->
                        val pkg = batch[index]
                        queryResult.vulns?.map { vuln ->
                            mapToVulnerabilityData(vuln, pkg)
                        } ?: emptyList()
                    }
                    allResults.addAll(batchVulns)
                }

                is HttpResult.NotFound     -> {
                    allResults.addAll(batch.map { emptyList() })
                }

                is HttpResult.RateLimited  -> {
                    return failureResult("Rate limited by OSV API", isTimeout = false)
                }

                is HttpResult.AuthRequired -> {
                    return failureResult("Authentication required for OSV API: ${httpResult.url}", isTimeout = false)
                }

                is HttpResult.Failed       -> {
                    return if (httpResult.cause is HttpRequestTimeoutException) {
                        failureResult(httpResult.error, isTimeout = true)
                    } else {
                        failureResult(httpResult.error, isTimeout = false)
                    }
                }
            }
        }

        return OsvBatchResult.Success(vulnerabilities = allResults)
    }

    override fun close() {
        httpClient.close()
    }

    private fun mapToVulnerabilityData(vuln: OsvVulnerability, pkg: OsvPackageQuery): OsvVulnerabilityData {
        val cvssEntry = CVSS_PRIORITY.firstNotNullOfOrNull { type ->
            vuln.severity?.firstOrNull { it.type == type }
        }
        val cvssScore = cvssEntry?.let { parseCvssScore(it.score) }
        val cvssVersion = cvssEntry?.type
        val fixedVersion = vuln.affected
            ?.flatMap { it.ranges ?: emptyList() }
            ?.flatMap { it.events ?: emptyList() }
            ?.firstOrNull { it.fixed != null }
            ?.fixed
        val referenceUrl = vuln.references?.firstOrNull()?.url

        return OsvVulnerabilityData(
            id = vuln.id,
            aliases = vuln.aliases ?: emptyList(),
            summary = vuln.summary ?: "",
            cvssScore = cvssScore,
            cvssVersion = cvssVersion,
            fixedVersion = fixedVersion,
            referenceUrl = referenceUrl,
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
