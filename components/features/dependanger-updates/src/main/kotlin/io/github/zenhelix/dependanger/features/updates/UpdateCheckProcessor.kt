package io.github.zenhelix.dependanger.features.updates

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.zenhelix.dependanger.core.model.CredentialsProvider
import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.core.model.MavenRepository
import io.github.zenhelix.dependanger.core.util.GlobMatcher
import io.github.zenhelix.dependanger.core.util.VersionComparator
import io.github.zenhelix.dependanger.effective.DiagnosticCodes
import io.github.zenhelix.dependanger.effective.model.EffectiveLibrary
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.withExtension
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase
import io.github.zenhelix.dependanger.features.resolver.CredentialsProviderKey
import io.github.zenhelix.dependanger.features.updates.model.UpdateAvailableInfo
import io.github.zenhelix.dependanger.features.updates.model.UpdatesExtensionKey
import io.github.zenhelix.dependanger.http.client.HttpClientFactory
import io.ktor.client.HttpClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

private val logger = KotlinLogging.logger {}

private const val HTTP_CONNECT_TIMEOUT_MS = 30_000L
private const val HTTP_REQUEST_TIMEOUT_MS = 60_000L
private const val HTTP_KEEP_ALIVE_MS = 5_000L

public class UpdateCheckProcessor : EffectiveMetadataProcessor {
    override val id: String = "update-check"
    override val phase: ProcessingPhase = ProcessingPhase.UPDATE_CHECK
    override val order: Int = phase.order
    override val isOptional: Boolean = true
    override val description: String = "Checks for available library updates"

    override fun supports(context: ProcessingContext): Boolean =
        context.settings.updateCheck.enabled

    override suspend fun process(metadata: EffectiveMetadata, context: ProcessingContext): EffectiveMetadata {
        val settings = context.settings.updateCheck
        val repositories = settings.repositories
            .filterIsInstance<MavenRepository>()
            .ifEmpty {
                context.settings.repositories
                    .filterIsInstance<MavenRepository>()
                    .ifEmpty {
                        listOf(MavenRepository(url = "https://repo.maven.apache.org/maven2", name = "Maven Central"))
                    }
            }
        val credentialsProvider = context[CredentialsProviderKey]

        val candidates = metadata.libraries.values.filter { lib ->
            !lib.ignoreUpdates
                    && lib.version != null
                    && settings.excludePatterns.none { pattern -> GlobMatcher.matches(pattern, lib.group, lib.artifact) }
        }

        if (candidates.isEmpty()) {
            val diag = Diagnostics.info(DiagnosticCodes.Update.ALL_UP_TO_DATE, "No libraries to check for updates", id, emptyMap())
            return metadata.copy(diagnostics = metadata.diagnostics + diag)
                .withExtension(UpdatesExtensionKey, emptyList())
        }

        val cacheDir = settings.cacheDirectory
            ?: (System.getProperty("user.home") + "/.dependanger/cache/versions")

        UpdateCheckContext(
            repositories = repositories,
            credentialsProvider = credentialsProvider,
            cacheDirectory = cacheDir,
            cacheTtlHours = settings.cacheTtlHours,
            connectTimeoutMs = HTTP_CONNECT_TIMEOUT_MS,
            readTimeoutMs = settings.timeout,
        ).use { ctx ->
            val semaphore = Semaphore(settings.parallelism)

            val results: List<UpdateCheckResult> = coroutineScope {
                candidates.map { lib ->
                    async {
                        semaphore.withPermit {
                            checkUpdate(lib, ctx, settings.includePrerelease)
                        }
                    }
                }.awaitAll()
            }

            val updates = results.mapNotNull { it.update }
            var diagnostics = metadata.diagnostics
            for (result in results) {
                diagnostics += result.diagnostics
            }

            val summary = if (updates.isNotEmpty()) {
                Diagnostics.info(
                    DiagnosticCodes.Update.UPDATES_AVAILABLE,
                    "${updates.size} update(s) available",
                    id, mapOf("count" to updates.size.toString())
                )
            } else {
                Diagnostics.info(DiagnosticCodes.Update.ALL_UP_TO_DATE, "All libraries are up to date", id, emptyMap())
            }
            diagnostics += summary

            return metadata.copy(diagnostics = diagnostics)
                .withExtension(UpdatesExtensionKey, updates)
        }
    }

    private suspend fun checkUpdate(
        lib: EffectiveLibrary,
        ctx: UpdateCheckContext,
        includePrerelease: Boolean,
    ): UpdateCheckResult {
        val currentVersionStr = lib.version!!.value
        val coordinate = "${lib.group}:${lib.artifact}"

        val currentVersion = VersionComparator.parse(currentVersionStr)
        if (currentVersion == null) {
            return UpdateCheckResult(
                update = null,
                diagnostics = Diagnostics.warning(
                    DiagnosticCodes.Update.INVALID_VERSION,
                    "Cannot parse version '$currentVersionStr' for $coordinate",
                    id, mapOf("library" to coordinate, "version" to currentVersionStr)
                ),
            )
        }

        val fetchResult = fetchVersions(lib.group, lib.artifact, ctx)
        if (fetchResult == null) {
            return UpdateCheckResult(update = null, diagnostics = Diagnostics.EMPTY)
        }

        val allVersions = fetchResult.versions
        if (allVersions.isEmpty()) {
            return UpdateCheckResult(update = null, diagnostics = Diagnostics.EMPTY)
        }

        val stableVersions = allVersions.filterNot { VersionComparator.isPrerelease(it) }
        val latestStable = VersionComparator.selectHighest(stableVersions)
        val latestAny = VersionComparator.selectHighest(allVersions)

        val target = if (includePrerelease) latestAny else latestStable
        if (target == null) {
            return UpdateCheckResult(update = null, diagnostics = Diagnostics.EMPTY)
        }

        val targetVersion = VersionComparator.parse(target)
        if (targetVersion == null || targetVersion <= currentVersion) {
            return UpdateCheckResult(update = null, diagnostics = Diagnostics.EMPTY)
        }

        val updateType = VersionComparator.classifyUpdate(currentVersion, targetVersion)

        return UpdateCheckResult(
            update = UpdateAvailableInfo(
                alias = lib.alias,
                group = lib.group,
                artifact = lib.artifact,
                currentVersion = currentVersionStr,
                latestVersion = target,
                latestStable = latestStable,
                latestAny = latestAny,
                updateType = updateType,
                repository = fetchResult.repository,
            ),
            diagnostics = Diagnostics.EMPTY,
        )
    }

    private suspend fun fetchVersions(
        group: String,
        artifact: String,
        ctx: UpdateCheckContext,
    ): VersionFetchResult? {
        val coordinate = "$group:$artifact"

        when (val cached = ctx.cache.get(group, artifact)) {
            is VersionCacheResult.Hit       -> return cached.result
            is VersionCacheResult.Corrupted -> logger.warn { "Corrupted version cache for $coordinate: ${cached.error}" }
            is VersionCacheResult.Miss      -> { /* proceed to fetch */
            }
        }

        return when (val fetchResult = ctx.fetcher.fetchVersions(group, artifact)) {
            is MetadataFetchResult.Success  -> {
                val result = VersionFetchResult(versions = fetchResult.versions, repository = fetchResult.repository)
                try {
                    ctx.cache.put(group, artifact, result)
                } catch (e: Exception) {
                    logger.warn { "Failed to write version cache for $coordinate: ${e.message}" }
                }
                result
            }

            is MetadataFetchResult.NotFound -> {
                val stale = ctx.cache.getStale(group, artifact)
                if (stale != null) {
                    logger.debug { "Using stale version cache for $coordinate" }
                    stale
                } else {
                    logger.debug { "Library $coordinate not found in any repository" }
                    null
                }
            }

            is MetadataFetchResult.Failed   -> {
                val stale = ctx.cache.getStale(group, artifact)
                if (stale != null) {
                    logger.debug { "Using stale version cache for $coordinate after fetch failure" }
                    stale
                } else {
                    logger.warn { "Failed to fetch versions for $coordinate: ${fetchResult.error}" }
                    null
                }
            }
        }
    }
}

private data class UpdateCheckResult(
    val update: UpdateAvailableInfo?,
    val diagnostics: Diagnostics,
)

private class UpdateCheckContext(
    repositories: List<MavenRepository>,
    credentialsProvider: CredentialsProvider?,
    cacheDirectory: String,
    cacheTtlHours: Long,
    connectTimeoutMs: Long,
    readTimeoutMs: Long,
) : AutoCloseable {

    val httpClient: HttpClient = HttpClientFactory.create {
        this.connectTimeoutMs = connectTimeoutMs
        this.requestTimeoutMs = readTimeoutMs
        this.keepAliveMs = HTTP_KEEP_ALIVE_MS
    }

    val cache: VersionCache = VersionCache(
        cacheDirectory = cacheDirectory,
        ttlHours = cacheTtlHours,
    )

    val fetcher: MavenMetadataFetcher = MavenMetadataFetcher(
        repositories = repositories,
        httpClient = httpClient,
        credentialsProvider = credentialsProvider,
        connectTimeoutMs = connectTimeoutMs,
        readTimeoutMs = readTimeoutMs,
    )

    override fun close() {
        httpClient.close()
    }
}
