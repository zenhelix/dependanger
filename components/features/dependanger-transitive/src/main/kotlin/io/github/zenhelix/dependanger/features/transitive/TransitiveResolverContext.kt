package io.github.zenhelix.dependanger.features.transitive

import io.github.zenhelix.dependanger.cache.DirBasedCache
import io.github.zenhelix.dependanger.core.DependangerPaths
import io.github.zenhelix.dependanger.core.model.CredentialsProvider
import io.github.zenhelix.dependanger.core.model.MavenRepository
import io.github.zenhelix.dependanger.features.resolver.MavenPomDownloader
import io.github.zenhelix.dependanger.features.transitive.model.TransitiveTree
import io.github.zenhelix.dependanger.http.client.HttpClientConfig
import io.github.zenhelix.dependanger.http.client.HttpClientFactory
import io.ktor.client.HttpClient

private const val DEFAULT_TTL_HOURS = 24L
private const val DEFAULT_SNAPSHOT_TTL_HOURS = 1L

internal class TransitiveResolverContext(
    repositories: List<MavenRepository>,
    credentialsProvider: CredentialsProvider?,
    cacheDirectory: String?,
    cacheTtlHours: Long?,
    readTimeoutMs: Long,
) : AutoCloseable {

    val httpClient: HttpClient = HttpClientFactory.create {
        this.connectTimeoutMs = HttpClientConfig.DEFAULT_CONNECT_TIMEOUT_MS
        this.requestTimeoutMs = readTimeoutMs
        this.keepAliveMs = HttpClientConfig.DEFAULT_KEEP_ALIVE_MS
    }

    val pomDownloader: MavenPomDownloader = MavenPomDownloader(
        repositories = repositories,
        httpClient = httpClient,
        credentialsProvider = credentialsProvider,
        connectTimeoutMs = HttpClientConfig.DEFAULT_CONNECT_TIMEOUT_MS,
        readTimeoutMs = readTimeoutMs,
    )

    val cache: DirBasedCache<TransitiveTree> = DirBasedCache(
        cacheDirectory = cacheDirectory ?: DependangerPaths.resolveInUserHome(DependangerPaths.TRANSITIVES_CACHE_DIR),
        ttlHours = cacheTtlHours ?: DEFAULT_TTL_HOURS,
        ttlSnapshotHours = DEFAULT_SNAPSHOT_TTL_HOURS,
        contentSerializer = TransitiveTree.serializer(),
        contentFileName = "tree-content.json",
    )

    override fun close() {
        httpClient.close()
    }
}
