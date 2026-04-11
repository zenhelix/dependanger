package io.github.zenhelix.dependanger.features.resolver

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.zenhelix.dependanger.cache.CacheResult
import io.github.zenhelix.dependanger.cache.DirBasedCache
import io.github.zenhelix.dependanger.core.model.CredentialsProvider
import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.core.model.MavenCoordinate
import io.github.zenhelix.dependanger.core.model.MavenRepository
import io.github.zenhelix.dependanger.effective.DiagnosticCodes
import io.github.zenhelix.dependanger.http.client.HttpClientFactory
import io.github.zenhelix.dependanger.maven.client.MavenClientConfig
import io.github.zenhelix.dependanger.maven.client.MavenPomService
import io.github.zenhelix.dependanger.maven.client.PomXmlParser
import io.github.zenhelix.dependanger.maven.client.model.DownloadResult
import io.github.zenhelix.dependanger.maven.client.model.PomParseResult
import io.github.zenhelix.dependanger.maven.client.model.RawBomDependency
import kotlin.coroutines.cancellation.CancellationException

private val logger = KotlinLogging.logger {}

private const val MAX_BOM_DEPTH = 10

/**
 * Recursively resolves BOM (Bill of Materials) dependency trees.
 * Handles cycle detection, disk caching, download with stale fallback,
 * parent POM inheritance, and transitive BOM imports.
 */
internal class BomTreeResolver(
    private val ctx: BomResolutionContext,
    private val processorId: String,
) {
    private val resolvedCache = mutableMapOf<String, BomContent>()

    suspend fun resolve(
        coordinate: MavenCoordinate,
        version: String,
        diagnostics: Diagnostics,
    ): BomResolveResult {
        val pathStack = mutableSetOf<String>()
        return resolveRecursive(coordinate, version, pathStack, diagnostics, depth = 0)
    }

    private suspend fun resolveRecursive(
        coordinate: MavenCoordinate,
        version: String,
        pathStack: MutableSet<String>,
        diagnostics: Diagnostics,
        depth: Int,
    ): BomResolveResult {
        val key = "$coordinate:$version"

        if (key in pathStack) {
            return BomResolveResult(
                content = BomContent.EMPTY,
                diagnostics = diagnostics + Diagnostics.error(
                    DiagnosticCodes.Bom.CIRCULAR, "Circular BOM dependency detected: $key", processorId, emptyMap()
                ),
            )
        }
        if (depth > MAX_BOM_DEPTH) {
            return BomResolveResult(
                content = BomContent.EMPTY,
                diagnostics = diagnostics + Diagnostics.warning(
                    DiagnosticCodes.Bom.DEPTH_EXCEEDED, "BOM parent hierarchy > $MAX_BOM_DEPTH levels for $key", processorId, emptyMap()
                ),
            )
        }

        resolvedCache[key]?.let { return BomResolveResult(content = it, diagnostics = diagnostics) }

        pathStack.add(key)
        try {
            var currentDiagnostics = diagnostics

            when (val cached = ctx.cache.get(coordinate, version)) {
                is CacheResult.Hit       -> {
                    resolvedCache[key] = cached.data
                    return BomResolveResult(content = cached.data, diagnostics = currentDiagnostics)
                }

                is CacheResult.Corrupted -> {
                    currentDiagnostics += Diagnostics.warning(
                        DiagnosticCodes.Bom.CACHE_CORRUPT,
                        "Cache corrupted for $key, will re-fetch: ${cached.error}",
                        processorId, emptyMap()
                    )
                }

                is CacheResult.Miss      -> {}
            }

            val pomXml = when (val fetchOutcome = fetchBomPom(key, coordinate, version, currentDiagnostics)) {
                is FetchOutcome.Resolved   -> {
                    fetchOutcome.result.content.takeIf { it !== BomContent.EMPTY }?.let { resolvedCache[key] = it }
                    return fetchOutcome.result
                }

                is FetchOutcome.Downloaded -> {
                    currentDiagnostics = fetchOutcome.diagnostics
                    fetchOutcome.pomXml
                }
            }

            val parseResult = try {
                ctx.parser.parseBomContent(pomXml)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                return BomResolveResult(
                    content = BomContent.EMPTY,
                    diagnostics = currentDiagnostics + Diagnostics.error(
                        DiagnosticCodes.Bom.INVALID_XML, "Failed to parse BOM XML for $key: ${e.message}", processorId, emptyMap()
                    ),
                )
            }

            val mergeResult = mergeWithParentAndImports(parseResult, key, pathStack, currentDiagnostics, depth)
            val finalDiagnostics = writeToDiskCache(key, coordinate, version, mergeResult.content, mergeResult.diagnostics)

            resolvedCache[key] = mergeResult.content
            return BomResolveResult(content = mergeResult.content, diagnostics = finalDiagnostics)
        } finally {
            pathStack.remove(key)
        }
    }

    private suspend fun fetchBomPom(
        key: String,
        coordinate: MavenCoordinate,
        version: String,
        diagnostics: Diagnostics,
    ): FetchOutcome {
        val pomXml = when (val downloadResult = ctx.downloader.downloadPom(coordinate.group, coordinate.artifact, version)) {
            is DownloadResult.Success -> downloadResult.content
            is DownloadResult.NotFound,
            is DownloadResult.AuthRequired,
            is DownloadResult.Failed  -> {
                val stale = ctx.cache.getStale(coordinate, version)
                if (stale != null) {
                    return FetchOutcome.Resolved(
                        BomResolveResult(
                            content = stale,
                            diagnostics = diagnostics + Diagnostics.warning(
                                DiagnosticCodes.Bom.STALE_CACHE, "Using stale cache for BOM $key", processorId, emptyMap()
                            ),
                        )
                    )
                }
                val (errorCode, errorMessage) = downloadErrorDiagnostic(downloadResult, key)
                return FetchOutcome.Resolved(
                    BomResolveResult(
                        content = BomContent.EMPTY,
                        diagnostics = diagnostics + Diagnostics.error(errorCode, errorMessage, processorId, emptyMap()),
                    )
                )
            }
        }
        return FetchOutcome.Downloaded(pomXml, diagnostics)
    }

    private suspend fun mergeWithParentAndImports(
        parseResult: PomParseResult,
        key: String,
        pathStack: MutableSet<String>,
        diagnostics: Diagnostics,
        depth: Int,
    ): BomResolveResult {
        var currentDiagnostics = diagnostics

        if (parseResult.dependencies.isEmpty() && parseResult.parent == null) {
            currentDiagnostics += Diagnostics.warning(
                DiagnosticCodes.Bom.NO_DEPS, "BOM $key contains no dependencyManagement", processorId, emptyMap()
            )
        }

        var mergedProperties = parseResult.properties
        val mergedDependencies = mutableListOf<BomDependency>()

        parseResult.parent?.let { parent ->
            val parentResult = resolveRecursive(
                coordinate = parent.gav.coordinate,
                version = parent.gav.version,
                pathStack = pathStack,
                diagnostics = currentDiagnostics,
                depth = depth + 1,
            )
            mergedProperties = parentResult.content.properties + mergedProperties
            mergedDependencies.addAll(parentResult.content.dependencies)
            currentDiagnostics = parentResult.diagnostics
        }

        val seen = mutableSetOf<MavenCoordinate>()
        for (rawDep in parseResult.dependencies) {
            if (rawDep.scope == "import" && rawDep.type == "pom") {
                val resolved = resolveRawDependency(rawDep, mergedProperties, currentDiagnostics)
                currentDiagnostics = resolved.diagnostics
                val dep = resolved.dependency ?: continue
                val importResult = resolveRecursive(
                    coordinate = dep.coordinate,
                    version = dep.version,
                    pathStack = pathStack,
                    diagnostics = currentDiagnostics,
                    depth = depth + 1,
                )
                mergedDependencies.addAll(importResult.content.dependencies)
                currentDiagnostics = importResult.diagnostics
            } else {
                val resolved = resolveRawDependency(rawDep, mergedProperties, currentDiagnostics)
                currentDiagnostics = resolved.diagnostics
                resolved.dependency?.let { dep ->
                    if (!seen.add(dep.coordinate)) {
                        currentDiagnostics += Diagnostics.warning(
                            DiagnosticCodes.Bom.DUPLICATE_ENTRY,
                            "Duplicate dependency ${dep.coordinate} in BOM $key, last definition wins",
                            processorId, emptyMap()
                        )
                    }
                    mergedDependencies.add(dep)
                }
            }
        }

        val content = BomContent(dependencies = mergedDependencies, properties = mergedProperties)
        return BomResolveResult(content = content, diagnostics = currentDiagnostics)
    }

    private fun writeToDiskCache(
        key: String,
        coordinate: MavenCoordinate,
        version: String,
        content: BomContent,
        diagnostics: Diagnostics,
    ): Diagnostics = try {
        ctx.cache.put(coordinate, version, content)
        diagnostics
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        logger.warn { "Failed to write BOM cache for $key: ${e.message}" }
        diagnostics + Diagnostics.warning(
            DiagnosticCodes.Bom.CACHE_READONLY, "Cannot write cache for $key: ${e.message}", processorId, emptyMap()
        )
    }

    private fun downloadErrorDiagnostic(result: DownloadResult, key: String): Pair<String, String> = when (result) {
        is DownloadResult.AuthRequired -> DiagnosticCodes.Bom.AUTH_REQUIRED to
                "Authentication required to fetch BOM $key from ${result.url} (HTTP ${result.statusCode})"

        is DownloadResult.NotFound     -> DiagnosticCodes.Bom.FETCH_FAILED to
                "BOM not found in any repository: $key"

        is DownloadResult.Failed       -> DiagnosticCodes.Bom.FETCH_FAILED to
                "Cannot fetch BOM $key: ${result.error}"

        is DownloadResult.Success      -> error("Success is not an error")
    }

    private fun resolveRawDependency(
        rawDep: RawBomDependency,
        properties: Map<String, String>,
        diagnostics: Diagnostics,
    ): DependencyResolutionResult = try {
        DependencyResolutionResult(
            dependency = BomDependency(
                coordinate = MavenCoordinate(
                    group = ctx.parser.resolveProperty(rawDep.coordinate.group, properties),
                    artifact = ctx.parser.resolveProperty(rawDep.coordinate.artifact, properties),
                ),
                version = ctx.parser.resolveProperty(rawDep.version, properties),
            ),
            diagnostics = diagnostics,
        )
    } catch (e: IllegalStateException) {
        DependencyResolutionResult(
            dependency = null,
            diagnostics = diagnostics + Diagnostics.error(
                DiagnosticCodes.Bom.UNRESOLVED_PROPERTY,
                "${e.message} in ${rawDep.coordinate}:${rawDep.version}",
                processorId, emptyMap()
            ),
        )
    }
}

private sealed interface FetchOutcome {
    data class Downloaded(val pomXml: String, val diagnostics: Diagnostics) : FetchOutcome
    data class Resolved(val result: BomResolveResult) : FetchOutcome
}

internal class BomResolutionContext(
    repositories: List<MavenRepository>,
    credentialsProvider: CredentialsProvider?,
    httpClientFactory: HttpClientFactory,
    cacheDirectory: String,
    ttlHours: Long,
    ttlSnapshotHours: Long,
) : AutoCloseable {

    val cache: DirBasedCache<BomContent> = DirBasedCache(
        cacheDirectory = cacheDirectory,
        ttlHours = ttlHours,
        ttlSnapshotHours = ttlSnapshotHours,
        contentSerializer = BomContent.serializer(),
        contentFileName = "bom-content.json",
    )

    val downloader: MavenPomService = MavenPomService(
        MavenClientConfig(
            repositories = repositories,
            credentialsProvider = credentialsProvider,
        ),
        httpClientFactory,
    )

    val parser: PomXmlParser = PomXmlParser()

    override fun close() {
        downloader.close()
    }
}
