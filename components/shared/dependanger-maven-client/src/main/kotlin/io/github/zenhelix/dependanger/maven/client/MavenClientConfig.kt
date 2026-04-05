package io.github.zenhelix.dependanger.maven.client

import io.github.zenhelix.dependanger.core.model.CredentialsProvider
import io.github.zenhelix.dependanger.core.model.MavenRepository
import io.github.zenhelix.dependanger.http.client.HttpClientConfig
import io.github.zenhelix.dependanger.http.client.RetryConfig

public data class MavenClientConfig(
    val repositories: List<MavenRepository>,
    val credentialsProvider: CredentialsProvider? = null,
    val connectTimeoutMs: Long = HttpClientConfig.DEFAULT_CONNECT_TIMEOUT_MS,
    val readTimeoutMs: Long = HttpClientConfig.DEFAULT_REQUEST_TIMEOUT_MS,
    val retryConfig: RetryConfig = RetryConfig(),
)
