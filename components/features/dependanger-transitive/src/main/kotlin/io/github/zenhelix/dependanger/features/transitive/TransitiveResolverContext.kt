package io.github.zenhelix.dependanger.features.transitive

import io.github.zenhelix.dependanger.cache.DirBasedCache
import io.github.zenhelix.dependanger.core.DependangerPaths
import io.github.zenhelix.dependanger.core.model.CredentialsProvider
import io.github.zenhelix.dependanger.core.model.MavenRepository
import io.github.zenhelix.dependanger.feature.model.transitive.TransitiveTree
import io.github.zenhelix.dependanger.http.client.HttpClientFactory
import io.github.zenhelix.dependanger.maven.client.MavenClientConfig
import io.github.zenhelix.dependanger.maven.client.MavenPomService

private const val DEFAULT_TTL_HOURS = 24L
private const val DEFAULT_SNAPSHOT_TTL_HOURS = 1L

internal class TransitiveResolverContext(
    repositories: List<MavenRepository>,
    credentialsProvider: CredentialsProvider?,
    httpClientFactory: HttpClientFactory,
    cacheDirectory: String?,
    cacheTtlHours: Long?,
    readTimeoutMs: Long,
) : AutoCloseable {

    val pomDownloader: MavenPomService = MavenPomService(
        MavenClientConfig(
            repositories = repositories,
            credentialsProvider = credentialsProvider,
            readTimeoutMs = readTimeoutMs,
        ),
        httpClientFactory,
    )

    val cache: DirBasedCache<TransitiveTree> = DirBasedCache(
        cacheDirectory = cacheDirectory ?: DependangerPaths.resolveInUserHome(DependangerPaths.TRANSITIVES_CACHE_DIR),
        ttlHours = cacheTtlHours ?: DEFAULT_TTL_HOURS,
        ttlSnapshotHours = DEFAULT_SNAPSHOT_TTL_HOURS,
        contentSerializer = TransitiveTree.serializer(),
        contentFileName = "tree-content.json",
    )

    override fun close() {
        pomDownloader.close()
    }
}
