package io.github.zenhelix.dependanger.features.license

import io.github.zenhelix.dependanger.cache.DirBasedCache
import io.github.zenhelix.dependanger.clearlydefined.client.ClearlyDefinedClient
import io.github.zenhelix.dependanger.clearlydefined.client.ClearlyDefinedClientConfig
import io.github.zenhelix.dependanger.core.DependangerPaths
import io.github.zenhelix.dependanger.core.model.CredentialsProvider
import io.github.zenhelix.dependanger.core.model.MavenRepository
import io.github.zenhelix.dependanger.features.license.model.LicenseResult
import io.github.zenhelix.dependanger.features.license.spi.LicenseSourceProvider
import io.github.zenhelix.dependanger.http.client.HttpClientFactory
import io.github.zenhelix.dependanger.maven.client.MavenClientConfig
import io.github.zenhelix.dependanger.maven.client.MavenPomService
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

    val cache: DirBasedCache<List<LicenseResult>> = DirBasedCache(
        cacheDirectory = cacheDirectory ?: DependangerPaths.resolveInUserHome(DependangerPaths.LICENSES_CACHE_DIR),
        ttlHours = cacheTtlHours,
        ttlSnapshotHours = DEFAULT_SNAPSHOT_TTL_HOURS,
        contentSerializer = ListSerializer(LicenseResult.serializer()),
        contentFileName = "license-content.json",
    )

    val pomService: MavenPomService = MavenPomService(
        config = MavenClientConfig(
            repositories = repositories,
            credentialsProvider = credentialsProvider,
            readTimeoutMs = readTimeoutMs,
        ),
        httpClientFactory = httpClientFactory,
    )

    val clearlyDefinedClient: ClearlyDefinedClient = ClearlyDefinedClient(
        config = ClearlyDefinedClientConfig(timeoutMs = readTimeoutMs),
        httpClientFactory = httpClientFactory,
    )

    val resolver: LicenseResolver = LicenseResolver(
        cache = cache,
        pomService = pomService,
        clearlyDefinedClient = clearlyDefinedClient,
        customProviders = customProviders,
    )

    override fun close() {
        pomService.close()
        clearlyDefinedClient.close()
    }
}
