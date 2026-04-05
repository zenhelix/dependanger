package io.github.zenhelix.dependanger.osv.client

import io.github.zenhelix.dependanger.http.client.RetryConfig

public data class OsvClientConfig(
    val apiUrl: String = "https://api.osv.dev",
    val batchSize: Int = 1000,
    val timeoutMs: Long = 30_000L,
    val retryConfig: RetryConfig = RetryConfig(),
)
