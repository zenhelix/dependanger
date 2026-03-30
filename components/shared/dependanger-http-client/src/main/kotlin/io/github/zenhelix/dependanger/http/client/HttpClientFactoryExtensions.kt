package io.github.zenhelix.dependanger.http.client

import io.ktor.client.HttpClient

/**
 * Creates an [HttpClient] with default connect and keep-alive timeouts,
 * using the specified [requestTimeoutMs] for request timeout.
 */
public fun HttpClientFactory.createDefault(requestTimeoutMs: Long): HttpClient = create {
    this.connectTimeoutMs = HttpClientConfig.DEFAULT_CONNECT_TIMEOUT_MS
    this.requestTimeoutMs = requestTimeoutMs
    this.keepAliveMs = HttpClientConfig.DEFAULT_KEEP_ALIVE_MS
}
