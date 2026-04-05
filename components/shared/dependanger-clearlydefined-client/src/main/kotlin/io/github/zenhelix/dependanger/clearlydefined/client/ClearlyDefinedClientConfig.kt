package io.github.zenhelix.dependanger.clearlydefined.client

import io.github.zenhelix.dependanger.http.client.RetryConfig

public data class ClearlyDefinedClientConfig(
    val apiUrl: String = "https://api.clearlydefined.io",
    val timeoutMs: Long = 30_000L,
    val retryConfig: RetryConfig = RetryConfig(),
)
