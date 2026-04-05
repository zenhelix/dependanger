package io.github.zenhelix.dependanger.maven.client

import io.github.zenhelix.dependanger.http.client.HttpClientFactory
import io.github.zenhelix.dependanger.maven.client.internal.DefaultMavenMetadataService
import io.github.zenhelix.dependanger.maven.client.model.MetadataFetchResult

public interface MavenMetadataService : AutoCloseable {
    public suspend fun fetchVersions(group: String, artifact: String): MetadataFetchResult
}

public fun MavenMetadataService(config: MavenClientConfig, httpClientFactory: HttpClientFactory): MavenMetadataService =
    DefaultMavenMetadataService(config, httpClientFactory)
