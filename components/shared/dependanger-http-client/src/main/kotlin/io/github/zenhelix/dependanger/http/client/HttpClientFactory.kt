package io.github.zenhelix.dependanger.http.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.endpoint
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

public object HttpClientFactory {

    public fun create(block: HttpClientConfig.() -> Unit = {}): HttpClient {
        val config = HttpClientConfig().apply(block)
        return HttpClient(CIO) {
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
