package io.github.zenhelix.dependanger.features.license

import io.github.zenhelix.dependanger.cache.DirBasedCache
import io.github.zenhelix.dependanger.core.DependangerPaths
import io.github.zenhelix.dependanger.core.model.CredentialsProvider
import io.github.zenhelix.dependanger.core.model.MavenRepository
import io.github.zenhelix.dependanger.features.license.model.LicenseResult
import io.github.zenhelix.dependanger.features.license.spi.LicenseSourceProvider
import io.github.zenhelix.dependanger.features.resolver.MavenPomDownloader
import io.github.zenhelix.dependanger.http.client.HttpClientFactory
import io.github.zenhelix.dependanger.http.client.createDefault
import io.ktor.client.HttpClient
import kotlinx.serialization.builtins.ListSerializer

private const val DEFAULT_SNAPSHOT_TTL_HOURS = 24L

internal class LicenseCheckContext(
    repositories: List<MavenRepository>,
    credentialsProvider: CredentialsProvider?,
    httpClientFactory: HttpClientFactory,
    cacheDirectory: String?,
    cacheTtlHours: Long,
    readTimeoutMs: Long,
    customProviders: List<LicenseSourceProvider> = emptyList(),
) : AutoCloseable {

    val httpClient: HttpClient = httpClientFactory.createDefault(readTimeoutMs)

    val cache: DirBasedCache<List<LicenseResult>> = DirBasedCache(
        cacheDirectory = cacheDirectory ?: DependangerPaths.resolveInUserHome(DependangerPaths.LICENSES_CACHE_DIR),
        ttlHours = cacheTtlHours,
        ttlSnapshotHours = DEFAULT_SNAPSHOT_TTL_HOURS,
        contentSerializer = ListSerializer(LicenseResult.serializer()),
        contentFileName = "license-content.json",
    )

    val pomDownloader: MavenPomDownloader = MavenPomDownloader(
        repositories = repositories,
        httpClient = httpClient,
        credentialsProvider = credentialsProvider,
        readTimeoutMs = readTimeoutMs,
    )

    val clearlyDefinedClient: ClearlyDefinedClient = ClearlyDefinedClient(
        httpClient = httpClient,
    )

    val resolver: LicenseResolver = LicenseResolver(
        cache = cache,
        pomDownloader = pomDownloader,
        clearlyDefinedClient = clearlyDefinedClient,
        customProviders = customProviders,
    )

    override fun close() {
        httpClient.close()
    }
}
