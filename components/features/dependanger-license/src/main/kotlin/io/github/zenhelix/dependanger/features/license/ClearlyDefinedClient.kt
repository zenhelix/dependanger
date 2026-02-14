package io.github.zenhelix.dependanger.features.license

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.zenhelix.dependanger.features.license.model.ClearlyDefinedDefinition
import io.github.zenhelix.dependanger.http.client.HttpResult
import io.github.zenhelix.dependanger.http.client.RetryConfig
import io.github.zenhelix.dependanger.http.client.getWithRetry
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json

private val logger = KotlinLogging.logger {}

private const val CLEARLY_DEFINED_BASE_URL = "https://api.clearlydefined.io"

/**
 * Client for the ClearlyDefined API. Fetches license information for Maven artifacts.
 */
public class ClearlyDefinedClient(
    private val httpClient: HttpClient,
) {

    private val json: Json = Json { ignoreUnknownKeys = true }

    /**
     * Fetches license definition from ClearlyDefined API.
     * Returns a list of SPDX license identifiers extracted from the declared field.
     * For SPDX expressions like "MIT OR Apache-2.0", returns ["MIT", "Apache-2.0"].
     */
    public suspend fun fetchLicenses(
        group: String,
        artifact: String,
        version: String,
    ): ClearlyDefinedResult {
        val url = "$CLEARLY_DEFINED_BASE_URL/definitions/maven/mavencentral/$group/$artifact/$version"

        return when (val httpResult = httpClient.getWithRetry(url, RetryConfig())) {
            is HttpResult.Success      -> {
                try {
                    val definition = json.decodeFromString<ClearlyDefinedDefinition>(httpResult.data)
                    val declared = definition.licensed?.declared

                    if (declared.isNullOrBlank() || declared == "NOASSERTION") {
                        ClearlyDefinedResult.NotFound
                    } else {
                        val licenseIds = SpdxExpressionParser.parse(declared)
                        if (licenseIds.isEmpty()) {
                            ClearlyDefinedResult.NotFound
                        } else {
                            ClearlyDefinedResult.Success(licenseIds)
                        }
                    }
                } catch (e: Exception) {
                    logger.warn(e) { "Failed to parse ClearlyDefined response for $group:$artifact:$version" }
                    ClearlyDefinedResult.Failed("Failed to parse response: ${e.message}")
                }
            }

            is HttpResult.NotFound     -> ClearlyDefinedResult.NotFound

            is HttpResult.AuthRequired -> {
                logger.warn { "ClearlyDefined API returned auth required for $group:$artifact:$version" }
                ClearlyDefinedResult.Failed("Authentication required: ${httpResult.statusCode}")
            }

            is HttpResult.RateLimited  -> {
                logger.warn { "ClearlyDefined API rate limited for $group:$artifact:$version" }
                ClearlyDefinedResult.Failed("Rate limited, retry after ${httpResult.retryAfterMs}ms")
            }

            is HttpResult.Failed       -> {
                logger.warn { "ClearlyDefined API failed for $group:$artifact:$version: ${httpResult.error}" }
                ClearlyDefinedResult.Failed(httpResult.error)
            }
        }
    }
}

/**
 * Result of a ClearlyDefined API query.
 */
public sealed interface ClearlyDefinedResult {
    /**
     * License identifiers successfully retrieved.
     * For SPDX expressions, contains multiple entries (e.g., ["MIT", "Apache-2.0"]).
     */
    public data class Success(val licenseIds: List<String>) : ClearlyDefinedResult

    /** No license information found for this artifact. */
    public data object NotFound : ClearlyDefinedResult

    /** Request failed due to network or API error. */
    public data class Failed(val error: String) : ClearlyDefinedResult
}
