package io.github.zenhelix.dependanger.clearlydefined.client.internal

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.zenhelix.dependanger.clearlydefined.client.ClearlyDefinedClient
import io.github.zenhelix.dependanger.clearlydefined.client.ClearlyDefinedClientConfig
import io.github.zenhelix.dependanger.clearlydefined.client.model.ClearlyDefinedResult
import io.github.zenhelix.dependanger.http.client.HttpClientFactory
import io.github.zenhelix.dependanger.http.client.HttpResult
import io.github.zenhelix.dependanger.http.client.createDefault
import io.github.zenhelix.dependanger.http.client.getWithRetry
import kotlinx.serialization.json.Json

private val logger = KotlinLogging.logger {}
private val lenientJson: Json = Json { ignoreUnknownKeys = true }

internal class DefaultClearlyDefinedClient(
    private val config: ClearlyDefinedClientConfig,
    httpClientFactory: HttpClientFactory,
) : ClearlyDefinedClient {

    private val httpClient = httpClientFactory.createDefault(config.timeoutMs)

    override suspend fun fetchLicense(group: String, artifact: String, version: String): ClearlyDefinedResult {
        val url = "${config.apiUrl}/definitions/maven/mavencentral/$group/$artifact/$version"

        return when (val httpResult = httpClient.getWithRetry(url, config.retryConfig)) {
            is HttpResult.Success      -> {
                try {
                    val definition = lenientJson.decodeFromString<ClearlyDefinedDefinition>(httpResult.data)
                    val declared = definition.licensed?.declared

                    if (declared.isNullOrBlank() || declared == "NOASSERTION") {
                        ClearlyDefinedResult.NotFound
                    } else {
                        ClearlyDefinedResult.Found(declaredExpression = declared)
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

    override fun close() {
        httpClient.close()
    }
}
