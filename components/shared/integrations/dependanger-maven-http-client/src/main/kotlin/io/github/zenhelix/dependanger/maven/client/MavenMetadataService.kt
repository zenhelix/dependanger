package io.github.zenhelix.dependanger.maven.client

import io.github.zenhelix.dependanger.http.client.HttpClientFactory

public interface MavenMetadataService : AutoCloseable {
    public suspend fun fetchVersions(group: String, artifact: String): MetadataFetchResult
}

public fun MavenMetadataService(config: MavenClientConfig, httpClientFactory: HttpClientFactory): MavenMetadataService =
    DefaultMavenMetadataService(config, httpClientFactory)
