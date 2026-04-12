package io.github.zenhelix.dependanger.http.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.endpoint
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.io.IOException
import kotlin.math.min
import kotlin.math.pow

public object DefaultHttpClientFactory : HttpClientFactory {

    override fun create(block: HttpClientConfig.() -> Unit): HttpClient {
        val config = HttpClientConfig().apply(block)
        return HttpClient(CIO) {
            install(CircuitBreakerPlugin) {
                failureThreshold = config.circuitBreakerConfig.failureThreshold
                openDurationMs = config.circuitBreakerConfig.openDurationMs
                halfOpenMaxProbes = config.circuitBreakerConfig.halfOpenMaxProbes
            }
            install(HttpRequestRetry) {
                maxRetries = config.retryConfig.maxRetries
                retryIf { _, response ->
                    response.status.value in 500..599 || response.status.value == 429
                }
                retryOnExceptionIf { _, cause ->
                    cause is HttpRequestTimeoutException || cause is IOException
                }
                delayMillis { retry ->
                    val base = config.retryConfig.initialDelayMs *
                        config.retryConfig.backoffMultiplier.pow(retry - 1)
                    min(base.toLong(), config.retryConfig.maxDelayMs)
                }
            }
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = config.jsonIgnoreUnknownKeys })
            }
            engine {
                requestTimeout = config.requestTimeoutMs
                endpoint {
                    connectTimeout = config.connectTimeoutMs
                    keepAliveTime = config.keepAliveMs
                }
            }
            install(HttpTimeout) {
                connectTimeoutMillis = config.connectTimeoutMs
                requestTimeoutMillis = config.requestTimeoutMs
            }
        }
    }
}
