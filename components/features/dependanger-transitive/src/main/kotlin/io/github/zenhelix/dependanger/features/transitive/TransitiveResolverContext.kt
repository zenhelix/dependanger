package io.github.zenhelix.dependanger.features.transitive

import io.github.zenhelix.dependanger.core.model.CredentialsProvider
import io.github.zenhelix.dependanger.core.model.MavenRepository
import io.github.zenhelix.dependanger.features.resolver.MavenPomDownloader
import io.github.zenhelix.dependanger.http.client.HttpClientFactory
import io.ktor.client.HttpClient

private const val HTTP_CONNECT_TIMEOUT_MS = 30_000L
private const val HTTP_KEEP_ALIVE_MS = 5_000L

private const val DEFAULT_CACHE_DIR = ".dependanger/cache/transitives"
private const val DEFAULT_TTL_HOURS = 24L
private const val DEFAULT_SNAPSHOT_TTL_HOURS = 1L

internal class TransitiveResolverContext(
    repositories: List<MavenRepository>,
    credentialsProvider: CredentialsProvider?,
    cacheDirectory: String?,
    readTimeoutMs: Long,
) : AutoCloseable {

    val httpClient: HttpClient = HttpClientFactory.create {
        this.connectTimeoutMs = HTTP_CONNECT_TIMEOUT_MS
        this.requestTimeoutMs = readTimeoutMs
        this.keepAliveMs = HTTP_KEEP_ALIVE_MS
    }

    val pomDownloader: MavenPomDownloader = MavenPomDownloader(
        repositories = repositories,
        httpClient = httpClient,
        credentialsProvider = credentialsProvider,
        connectTimeoutMs = HTTP_CONNECT_TIMEOUT_MS,
        readTimeoutMs = readTimeoutMs,
    )

    val cache: TransitiveCache = TransitiveCache(
        cacheDirectory = cacheDirectory ?: (System.getProperty("user.home") + "/$DEFAULT_CACHE_DIR"),
        ttlHours = DEFAULT_TTL_HOURS,
        ttlSnapshotHours = DEFAULT_SNAPSHOT_TTL_HOURS,
    )

    override fun close() {
        httpClient.close()
    }
}
