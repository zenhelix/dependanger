package io.github.zenhelix.dependanger.http.client

import io.ktor.client.HttpClient

public interface HttpClientFactory {
    public fun create(block: HttpClientConfig.() -> Unit = {}): HttpClient
}
