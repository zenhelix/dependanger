package io.github.zenhelix.dependanger.clearlydefined.client

import io.github.zenhelix.dependanger.clearlydefined.client.internal.DefaultClearlyDefinedClient
import io.github.zenhelix.dependanger.http.client.HttpClientFactory

/**
 * Client for the ClearlyDefined API. Fetches license information for Maven artifacts.
 */
public interface ClearlyDefinedClient : AutoCloseable {

    /**
     * Fetches the declared license expression from ClearlyDefined for the given Maven artifact.
     * Returns the raw SPDX expression string without parsing.
     */
    public suspend fun fetchLicense(group: String, artifact: String, version: String): ClearlyDefinedResult
}

/**
 * Creates a [ClearlyDefinedClient] with the given configuration and HTTP client factory.
 */
public fun ClearlyDefinedClient(
    config: ClearlyDefinedClientConfig = ClearlyDefinedClientConfig(),
    httpClientFactory: HttpClientFactory,
): ClearlyDefinedClient = DefaultClearlyDefinedClient(config, httpClientFactory)
