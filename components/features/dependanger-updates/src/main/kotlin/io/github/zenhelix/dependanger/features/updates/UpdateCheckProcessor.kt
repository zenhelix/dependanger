package io.github.zenhelix.dependanger.features.updates

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.zenhelix.dependanger.cache.CacheResult
import io.github.zenhelix.dependanger.core.DependangerPaths
import io.github.zenhelix.dependanger.core.model.CredentialsProvider
import io.github.zenhelix.dependanger.core.model.CredentialsProviderKey
import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.core.model.MavenRepository
import io.github.zenhelix.dependanger.core.util.GlobMatcher
import io.github.zenhelix.dependanger.core.util.UpdateType
import io.github.zenhelix.dependanger.core.util.VersionComparator
import io.github.zenhelix.dependanger.effective.DiagnosticCodes
import io.github.zenhelix.dependanger.effective.ProcessorIds
import io.github.zenhelix.dependanger.effective.model.EffectiveLibrary
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.pipeline.OrderConstraint
import io.github.zenhelix.dependanger.effective.pipeline.ParallelMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.ParallelResult
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase
import io.github.zenhelix.dependanger.effective.pipeline.resolveMavenRepositories
import io.github.zenhelix.dependanger.feature.model.updates.UpdateAvailableInfo
import io.github.zenhelix.dependanger.feature.model.updates.UpdatesExtensionKey
import io.github.zenhelix.dependanger.http.client.DefaultHttpClientFactory
import io.github.zenhelix.dependanger.http.client.HttpClientFactory
import io.github.zenhelix.dependanger.http.client.HttpClientFactoryKey
import io.github.zenhelix.dependanger.http.client.createDefault
import io.ktor.client.HttpClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

private val logger = KotlinLogging.logger {}

public class UpdateCheckProcessor : ParallelMetadataProcessor {
    override val id: String = ProcessorIds.UPDATE_CHECK
    override val phase: ProcessingPhase = ProcessingPhase.UPDATE_CHECK
    override val constraints: Set<OrderConstraint> = setOf(OrderConstraint.runsAfter(ProcessorIds.VERSION_RESOLVER))
    override val isOptional: Boolean = true
    override val description: String = "Checks for available library updates"

    override fun supports(context: ProcessingContext): Boolean =
        context[UpdateCheckSettingsKey]?.enabled == true

    override suspend fun processParallel(metadata: EffectiveMetadata, context: ProcessingContext): ParallelResult {
        val settings = context.require(UpdateCheckSettingsKey)
        val repositories = context.resolveMavenRepositories(settings.repositories)
        val credentialsProvider = context[CredentialsProviderKey]
        val httpClientFactory = context[HttpClientFactoryKey] ?: DefaultHttpClientFactory

        val candidates = metadata.libraries.values.filter { lib ->
            !lib.ignoreUpdates
                    && lib.version != null
                    && settings.excludePatterns.none { pattern -> GlobMatcher.matches(pattern, lib.group, lib.artifact) }
        }

        if (candidates.isEmpty()) {
            return ParallelResult.emptyResult(DiagnosticCodes.Update.ALL_UP_TO_DATE, "No libraries to check for updates", id, UpdatesExtensionKey)
        }

        val cacheDir = settings.cacheDirectory
            ?: DependangerPaths.resolveInUserHome(DependangerPaths.VERSIONS_CACHE_DIR)

        UpdateCheckContext(
            repositories = repositories,
            credentialsProvider = credentialsProvider,
            httpClientFactory = httpClientFactory,
            cacheDirectory = cacheDir,
            cacheTtlHours = settings.cacheTtlHours,
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
            var diagnostics = Diagnostics.EMPTY
            for (result in results) {
                diagnostics += result.diagnostics
            }

            val summary = if (updates.isNotEmpty()) {
                val typeCounts = updates.groupingBy { it.updateType }.eachCount()
                val major = typeCounts.getOrDefault(UpdateType.MAJOR, 0)
                val minor = typeCounts.getOrDefault(UpdateType.MINOR, 0)
                val patch = typeCounts.getOrDefault(UpdateType.PATCH, 0)
                Diagnostics.info(
                    DiagnosticCodes.Update.UPDATES_AVAILABLE,
                    "Updates available: ${updates.size} ($major major, $minor minor, $patch patch)",
                    id, mapOf(
                        "count" to updates.size.toString(),
                        "major" to major.toString(),
                        "minor" to minor.toString(),
                        "patch" to patch.toString(),
                    )
                )
            } else {
                Diagnostics.info(DiagnosticCodes.Update.ALL_UP_TO_DATE, "All libraries are up to date", id, emptyMap())
            }
            diagnostics += summary

            return ParallelResult(diagnostics, mapOf(UpdatesExtensionKey to updates))
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

        val fetchOutcome = fetchVersions(lib.group, lib.artifact, ctx)
        if (fetchOutcome.result == null) {
            return UpdateCheckResult(update = null, diagnostics = fetchOutcome.diagnostics)
        }

        val fetchResult = fetchOutcome.result
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
    ): FetchOutcome {
        val coordinate = "$group:$artifact"

        when (val cached = ctx.cache.get(group, artifact)) {
            is CacheResult.Hit  -> return FetchOutcome(cached.data, Diagnostics.EMPTY)
            is CacheResult.Corrupted -> logger.warn { "Corrupted version cache for $coordinate: ${cached.error}" }
            is CacheResult.Miss -> { /* proceed to fetch */
            }
        }

        return when (val fetchResult = ctx.fetcher.fetchVersions(group, artifact)) {
            is MetadataFetchResult.Success     -> {
                val result = VersionFetchResult(versions = fetchResult.versions, repository = fetchResult.repository)
                try {
                    ctx.cache.put(group, artifact, result)
                } catch (e: Exception) {
                    logger.warn { "Failed to write version cache for $coordinate: ${e.message}" }
                }
                FetchOutcome(result, Diagnostics.EMPTY)
            }

            is MetadataFetchResult.NotFound    -> {
                val stale = ctx.cache.getStale(group, artifact)
                if (stale != null) {
                    logger.debug { "Using stale version cache for $coordinate" }
                    FetchOutcome(stale, Diagnostics.EMPTY)
                } else {
                    logger.debug { "Library $coordinate not found in any repository" }
                    val diag = Diagnostics.warning(
                        DiagnosticCodes.Update.LIB_NOT_FOUND,
                        "Library $coordinate not found in any configured repository",
                        id, mapOf("library" to coordinate)
                    )
                    FetchOutcome(null, diag)
                }
            }

            is MetadataFetchResult.RateLimited -> {
                logger.warn { "Rate limited when fetching $coordinate: ${fetchResult.error}" }
                val diag = Diagnostics.warning(
                    DiagnosticCodes.Update.RATE_LIMITED,
                    "Rate limited when checking updates for $coordinate",
                    id, mapOf("library" to coordinate)
                )
                FetchOutcome(ctx.cache.getStale(group, artifact), diag)
            }

            is MetadataFetchResult.TimedOut    -> {
                logger.warn { "Timeout when fetching $coordinate: ${fetchResult.error}" }
                val diag = Diagnostics.warning(
                    DiagnosticCodes.Update.TIMEOUT,
                    "Timeout when checking updates for $coordinate",
                    id, mapOf("library" to coordinate)
                )
                FetchOutcome(ctx.cache.getStale(group, artifact), diag)
            }

            is MetadataFetchResult.Failed      -> {
                val stale = ctx.cache.getStale(group, artifact)
                if (stale != null) {
                    logger.debug { "Using stale version cache for $coordinate after fetch failure" }
                    FetchOutcome(stale, Diagnostics.EMPTY)
                } else {
                    logger.warn { "Failed to fetch versions for $coordinate: ${fetchResult.error}" }
                    val diag = Diagnostics.warning(
                        DiagnosticCodes.Update.REPO_UNREACHABLE,
                        "Failed to fetch versions for $coordinate: ${fetchResult.error}",
                        id, mapOf("library" to coordinate, "error" to fetchResult.error)
                    )
                    FetchOutcome(null, diag)
                }
            }
        }
    }
}

private data class UpdateCheckResult(
    val update: UpdateAvailableInfo?,
    val diagnostics: Diagnostics,
)

private data class FetchOutcome(
    val result: VersionFetchResult?,
    val diagnostics: Diagnostics,
)

private class UpdateCheckContext(
    repositories: List<MavenRepository>,
    credentialsProvider: CredentialsProvider?,
    httpClientFactory: HttpClientFactory,
    cacheDirectory: String,
    cacheTtlHours: Long,
    readTimeoutMs: Long,
) : AutoCloseable {

    val httpClient: HttpClient = httpClientFactory.createDefault(readTimeoutMs)

    val cache: VersionCache = VersionCache(
        cacheDirectory = cacheDirectory,
        ttlHours = cacheTtlHours,
    )

    val fetcher: MavenMetadataFetcher = MavenMetadataFetcher(
        repositories = repositories,
        httpClient = httpClient,
        credentialsProvider = credentialsProvider,
        readTimeoutMs = readTimeoutMs,
    )

    override fun close() {
        httpClient.close()
    }
}
