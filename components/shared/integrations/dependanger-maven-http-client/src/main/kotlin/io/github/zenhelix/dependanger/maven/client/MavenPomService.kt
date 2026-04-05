package io.github.zenhelix.dependanger.maven.client

import io.github.zenhelix.dependanger.http.client.HttpClientFactory
import io.github.zenhelix.dependanger.maven.client.internal.DefaultMavenPomService
import io.github.zenhelix.dependanger.maven.client.model.DownloadResult

public interface MavenPomService : AutoCloseable {
    public suspend fun downloadPom(group: String, artifact: String, version: String): DownloadResult
}

public fun MavenPomService(config: MavenClientConfig, httpClientFactory: HttpClientFactory): MavenPomService =
    DefaultMavenPomService(config, httpClientFactory)
