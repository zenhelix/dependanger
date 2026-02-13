package io.github.zenhelix.dependanger.features.resolver

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.zenhelix.dependanger.core.model.CredentialsProvider
import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.core.model.MavenRepository
import io.github.zenhelix.dependanger.core.model.VersionReference
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.ResolvedVersion
import io.github.zenhelix.dependanger.effective.model.VersionSource
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase
import io.github.zenhelix.dependanger.http.client.HttpClientFactory
import io.ktor.client.HttpClient

private val logger = KotlinLogging.logger {}

private const val MAX_BOM_DEPTH = 10
private const val HTTP_REQUEST_TIMEOUT_MS = 60_000L
private const val HTTP_CONNECT_TIMEOUT_MS = 30_000L
private const val HTTP_KEEP_ALIVE_MS = 5_000L

public class BomImportProcessor : EffectiveMetadataProcessor {
    override val id: String = "bom-import"
    override val phase: ProcessingPhase = ProcessingPhase.BOM_IMPORT
    override val order: Int = phase.order
    override val isOptional: Boolean = true
    override val description: String = "Imports dependencies from Maven BOM files"
    override fun supports(context: ProcessingContext): Boolean = true

    override suspend fun process(metadata: EffectiveMetadata, context: ProcessingContext): EffectiveMetadata {
        val bomImports = context.originalMetadata.bomImports
        if (bomImports.isEmpty()) return metadata

        val settings = context.settings
        val cacheDir = settings.bomCache.directory
            ?: (System.getProperty("user.home") + "/.dependanger/cache/bom")
        val repositories = settings.repositories
            .filterIsInstance<MavenRepository>()
            .ifEmpty {
                listOf(MavenRepository(url = "https://repo.maven.apache.org/maven2", name = "Maven Central"))
            }
        val credentialsProvider = context[CredentialsProviderKey]

        BomResolutionContext(
            repositories = repositories,
            credentialsProvider = credentialsProvider,
            cacheDirectory = cacheDir,
            ttlHours = settings.bomCache.ttlHours,
            ttlSnapshotHours = settings.bomCache.ttlSnapshotHours,
        ).use { resolutionCtx ->
            val resolvedBoms = mutableListOf<BomContent>()
            val resolvedCache = mutableMapOf<String, BomContent>()
            var diagnostics = metadata.diagnostics

            for (bomImport in bomImports) {
                val (resolvedVersion, versionDiagnostics) = resolveVersionReference(bomImport, context)
                diagnostics += versionDiagnostics
                if (resolvedVersion == null) continue

                if (resolvedVersion.endsWith("-SNAPSHOT")) {
                    diagnostics += Diagnostics.warning(
                        "BOM_SNAPSHOT_WARNING",
                        "BOM '${bomImport.alias}' uses SNAPSHOT version $resolvedVersion which may be unstable",
                        id, emptyMap()
                    )
                }

                val state = ResolutionState(
                    pathStack = mutableSetOf(),
                    resolvedCache = resolvedCache,
                )
                val result = resolveBomRecursive(
                    group = bomImport.group,
                    artifact = bomImport.artifact,
                    version = resolvedVersion,
                    ctx = resolutionCtx,
                    state = state,
                    diagnostics = diagnostics,
                    depth = 0,
                )
                resolvedBoms.add(result.content)
                diagnostics = result.diagnostics
            }

            val versionMap = buildBomVersionMap(resolvedBoms, bomImports)
            val (updatedLibraries, enrichDiagnostics) = enrichLibraries(metadata.libraries, versionMap, diagnostics)

            return metadata.copy(
                libraries = updatedLibraries,
                diagnostics = enrichDiagnostics,
            )
        }
    }

    private fun resolveVersionReference(
        bomImport: io.github.zenhelix.dependanger.core.model.BomImport,
        context: ProcessingContext,
    ): Pair<String?, Diagnostics> = when (val v = bomImport.version) {
        is VersionReference.Literal   -> v.version to Diagnostics.EMPTY
        is VersionReference.Reference -> {
            val found = context.originalMetadata.versions.find { it.name == v.name }
            if (found == null) {
                null to Diagnostics.error(
                    "BOM_UNRESOLVED_VERSION",
                    "Version reference '${v.name}' not found for BOM '${bomImport.alias}'",
                    id, emptyMap()
                )
            } else {
                found.value to Diagnostics.EMPTY
            }
        }

        is VersionReference.Range     -> {
            null to Diagnostics.warning(
                "BOM_VERSION_RANGE",
                "Version ranges not supported for BOM imports, skipping '${bomImport.alias}'",
                id, emptyMap()
            )
        }
    }

    private fun buildBomVersionMap(
        resolvedBoms: List<BomContent>,
        bomImports: List<io.github.zenhelix.dependanger.core.model.BomImport>,
    ): Map<String, Pair<String, String>> {
        val versionMap = mutableMapOf<String, Pair<String, String>>()
        for ((index, bom) in resolvedBoms.withIndex()) {
            val bomAlias = bomImports[index].alias
            // Within a resolved BOM: last definition wins (child overrides parent, Maven behavior)
            val bomVersions = mutableMapOf<String, Pair<String, String>>()
            for (dep in bom.dependencies) {
                bomVersions["${dep.group}:${dep.artifact}"] = dep.version to bomAlias
            }
            // Across BOMs: first BOM wins
            for ((key, value) in bomVersions) {
                versionMap.putIfAbsent(key, value)
            }
        }
        return versionMap
    }

    private fun enrichLibraries(
        libraries: Map<String, io.github.zenhelix.dependanger.effective.model.EffectiveLibrary>,
        versionMap: Map<String, Pair<String, String>>,
        diagnostics: Diagnostics,
    ): Pair<Map<String, io.github.zenhelix.dependanger.effective.model.EffectiveLibrary>, Diagnostics> {
        var currentDiagnostics = diagnostics
        val updatedLibraries = libraries.mapValues { (_, lib) ->
            if (lib.version != null) return@mapValues lib
            val key = "${lib.group}:${lib.artifact}"
            val (version, bomAlias) = versionMap[key] ?: return@mapValues lib
            currentDiagnostics += Diagnostics.info(
                "BOM_VERSION_IMPORTED",
                "Version $version imported from BOM '$bomAlias' for ${lib.group}:${lib.artifact}",
                id, emptyMap()
            )
            lib.copy(
                version = ResolvedVersion(
                    alias = lib.alias,
                    value = version,
                    source = VersionSource.BOM_IMPORT,
                    originalRef = bomAlias,
                ),
            )
        }
        return updatedLibraries to currentDiagnostics
    }

    private suspend fun resolveBomRecursive(
        group: String,
        artifact: String,
        version: String,
        ctx: BomResolutionContext,
        state: ResolutionState,
        diagnostics: Diagnostics,
        depth: Int,
    ): BomResolveResult {
        val key = "$group:$artifact:$version"

        if (key in state.pathStack) {
            return BomResolveResult(
                content = BomContent.EMPTY,
                diagnostics = diagnostics + Diagnostics.error(
                    "CIRCULAR_BOM", "Circular BOM dependency detected: $key", id, emptyMap()
                ),
            )
        }
        if (depth > MAX_BOM_DEPTH) {
            return BomResolveResult(
                content = BomContent.EMPTY,
                diagnostics = diagnostics + Diagnostics.warning(
                    "BOM_DEPTH_EXCEEDED", "BOM parent hierarchy > $MAX_BOM_DEPTH levels for $key", id, emptyMap()
                ),
            )
        }

        state.resolvedCache[key]?.let { return BomResolveResult(content = it, diagnostics = diagnostics) }

        state.pathStack.add(key)
        try {
            var currentDiagnostics = diagnostics

            when (val cached = ctx.cache.get(group, artifact, version)) {
                is CacheResult.Hit       -> {
                    state.resolvedCache[key] = cached.content
                    return BomResolveResult(content = cached.content, diagnostics = currentDiagnostics)
                }

                is CacheResult.Corrupted -> {
                    currentDiagnostics += Diagnostics.warning(
                        "BOM_CACHE_CORRUPT",
                        "Cache corrupted for $key, will re-fetch: ${cached.error}",
                        id, emptyMap()
                    )
                }

                is CacheResult.Miss      -> { /* proceed to download */
                }
            }

            val pomXml = when (val downloadResult = ctx.downloader.downloadPom(group, artifact, version)) {
                is DownloadResult.Success -> downloadResult.content
                is DownloadResult.NotFound,
                is DownloadResult.AuthRequired,
                is DownloadResult.Failed  -> {
                    val stale = ctx.cache.getStale(group, artifact, version)
                    if (stale != null) {
                        state.resolvedCache[key] = stale
                        return BomResolveResult(
                            content = stale,
                            diagnostics = currentDiagnostics + Diagnostics.warning(
                                "BOM_STALE_CACHE", "Using stale cache for BOM $key", id, emptyMap()
                            ),
                        )
                    }
                    val (errorCode, errorMessage) = downloadErrorDiagnostic(downloadResult, key)
                    return BomResolveResult(
                        content = BomContent.EMPTY,
                        diagnostics = currentDiagnostics + Diagnostics.error(errorCode, errorMessage, id, emptyMap()),
                    )
                }
            }

            val parseResult = try {
                ctx.parser.parseBomContent(pomXml)
            } catch (e: Exception) {
                return BomResolveResult(
                    content = BomContent.EMPTY,
                    diagnostics = currentDiagnostics + Diagnostics.error(
                        "INVALID_BOM_XML", "Failed to parse BOM XML for $key: ${e.message}", id, emptyMap()
                    ),
                )
            }

            var mergedProperties = parseResult.properties
            val mergedDependencies = mutableListOf<BomDependency>()

            if (parseResult.dependencies.isEmpty() && parseResult.parent == null) {
                currentDiagnostics += Diagnostics.warning(
                    "BOM_NO_DEPS", "BOM $key contains no dependencyManagement", id, emptyMap()
                )
            }

            parseResult.parent?.let { parent ->
                val parentResult = resolveBomRecursive(
                    group = parent.group,
                    artifact = parent.artifact,
                    version = parent.version,
                    ctx = ctx,
                    state = state,
                    diagnostics = currentDiagnostics,
                    depth = depth + 1,
                )
                mergedProperties = parentResult.content.properties + mergedProperties
                mergedDependencies.addAll(parentResult.content.dependencies)
                currentDiagnostics = parentResult.diagnostics
            }

            val seen = mutableSetOf<String>()
            for (rawDep in parseResult.dependencies) {
                if (rawDep.scope == "import" && rawDep.type == "pom") {
                    val resolved = resolveRawDependency(ctx.parser, rawDep, mergedProperties, currentDiagnostics)
                    currentDiagnostics = resolved.diagnostics
                    val dep = resolved.dependency ?: continue
                    val importResult = resolveBomRecursive(
                        group = dep.group,
                        artifact = dep.artifact,
                        version = dep.version,
                        ctx = ctx,
                        state = state,
                        diagnostics = currentDiagnostics,
                        depth = depth + 1,
                    )
                    mergedDependencies.addAll(importResult.content.dependencies)
                    currentDiagnostics = importResult.diagnostics
                } else {
                    val resolved = resolveRawDependency(ctx.parser, rawDep, mergedProperties, currentDiagnostics)
                    currentDiagnostics = resolved.diagnostics
                    resolved.dependency?.let { dep ->
                        val depKey = "${dep.group}:${dep.artifact}"
                        if (!seen.add(depKey)) {
                            currentDiagnostics += Diagnostics.warning(
                                "BOM_DUPLICATE_ENTRY",
                                "Duplicate dependency $depKey in BOM $key, last definition wins",
                                id, emptyMap()
                            )
                        }
                        mergedDependencies.add(dep)
                    }
                }
            }

            val content = BomContent(dependencies = mergedDependencies, properties = mergedProperties)
            try {
                ctx.cache.put(group, artifact, version, content)
            } catch (e: Exception) {
                logger.warn { "Failed to write BOM cache for $key: ${e.message}" }
                currentDiagnostics += Diagnostics.warning(
                    "BOM_CACHE_READONLY", "Cannot write cache for $key: ${e.message}", id, emptyMap()
                )
            }
            state.resolvedCache[key] = content

            return BomResolveResult(content = content, diagnostics = currentDiagnostics)
        } finally {
            state.pathStack.remove(key)
        }
    }

    private fun downloadErrorDiagnostic(result: DownloadResult, key: String): Pair<String, String> = when (result) {
        is DownloadResult.AuthRequired -> "BOM_AUTH_REQUIRED" to
                "Authentication required to fetch BOM $key from ${result.url} (HTTP ${result.statusCode})"

        is DownloadResult.NotFound     -> "BOM_FETCH_FAILED" to
                "BOM not found in any repository: $key"

        is DownloadResult.Failed       -> "BOM_FETCH_FAILED" to
                "Cannot fetch BOM $key: ${result.error}"

        is DownloadResult.Success      -> error("Success is not an error")
    }

    private fun resolveRawDependency(
        parser: PomXmlParser,
        rawDep: RawBomDependency,
        properties: Map<String, String>,
        diagnostics: Diagnostics,
    ): DependencyResolutionResult = try {
        DependencyResolutionResult(
            dependency = BomDependency(
                group = parser.resolveProperty(rawDep.group, properties),
                artifact = parser.resolveProperty(rawDep.artifact, properties),
                version = parser.resolveProperty(rawDep.version, properties),
            ),
            diagnostics = diagnostics,
        )
    } catch (e: IllegalStateException) {
        DependencyResolutionResult(
            dependency = null,
            diagnostics = diagnostics + Diagnostics.error(
                "UNRESOLVED_BOM_PROPERTY",
                "${e.message} in ${rawDep.group}:${rawDep.artifact}:${rawDep.version}",
                id, emptyMap()
            ),
        )
    }
}

private data class ResolutionState(
    val pathStack: MutableSet<String>,
    val resolvedCache: MutableMap<String, BomContent>,
)

private class BomResolutionContext(
    repositories: List<MavenRepository>,
    credentialsProvider: CredentialsProvider?,
    cacheDirectory: String,
    ttlHours: Long,
    ttlSnapshotHours: Long,
) : AutoCloseable {

    val httpClient: HttpClient = HttpClientFactory.create {
        connectTimeoutMs = HTTP_CONNECT_TIMEOUT_MS
        requestTimeoutMs = HTTP_REQUEST_TIMEOUT_MS
        keepAliveMs = HTTP_KEEP_ALIVE_MS
    }

    val cache: BomCache = BomCache(
        cacheDirectory = cacheDirectory,
        ttlHours = ttlHours,
        ttlSnapshotHours = ttlSnapshotHours,
    )

    val downloader: MavenPomDownloader = MavenPomDownloader(
        repositories = repositories,
        httpClient = httpClient,
        credentialsProvider = credentialsProvider,
        connectTimeoutMs = HTTP_CONNECT_TIMEOUT_MS,
        readTimeoutMs = HTTP_REQUEST_TIMEOUT_MS,
    )

    val parser: PomXmlParser = PomXmlParser()

    override fun close() {
        httpClient.close()
    }
}
