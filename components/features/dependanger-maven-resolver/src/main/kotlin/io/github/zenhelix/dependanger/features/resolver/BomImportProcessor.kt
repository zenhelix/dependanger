package io.github.zenhelix.dependanger.features.resolver

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.core.model.MavenRepository
import io.github.zenhelix.dependanger.core.model.VersionReference
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.ResolvedVersion
import io.github.zenhelix.dependanger.effective.model.VersionSource
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.endpoint
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

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
        val cache = BomCache(
            cacheDirectory = cacheDir,
            ttlHours = settings.bomCache.ttlHours,
            ttlSnapshotHours = settings.bomCache.ttlSnapshotHours,
        )
        val repositories = settings.repositories
            .filterIsInstance<MavenRepository>()
            .ifEmpty {
                listOf(MavenRepository("https://repo.maven.apache.org/maven2", "Maven Central"))
            }
        val credentialsProvider = context[CredentialsProviderKey]
        val httpClient = createHttpClient()
        val downloader = MavenPomDownloader(repositories, httpClient, credentialsProvider)
        val parser = PomXmlParser()

        try {
            val resolvedBoms = mutableListOf<BomContent>()
            val resolvedCache = mutableMapOf<String, BomContent>()
            var diagnostics = metadata.diagnostics

            for (bomImport in bomImports) {
                val resolvedVersion = when (val v = bomImport.version) {
                    is VersionReference.Literal   -> v.version
                    is VersionReference.Reference -> {
                        val found = context.originalMetadata.versions.find { it.name == v.name }
                        if (found == null) {
                            diagnostics = diagnostics + Diagnostics.error(
                                "BOM_UNRESOLVED_VERSION",
                                "Version reference '${v.name}' not found for BOM '${bomImport.alias}'",
                                id, emptyMap()
                            )
                            continue
                        }
                        found.value
                    }

                    is VersionReference.Range     -> {
                        diagnostics = diagnostics + Diagnostics.warning(
                            "BOM_VERSION_RANGE",
                            "Version ranges not supported for BOM imports, skipping '${bomImport.alias}'",
                            id, emptyMap()
                        )
                        continue
                    }
                }

                val pathStack = mutableSetOf<String>()
                val result = resolveBomRecursive(
                    bomImport.group, bomImport.artifact, resolvedVersion,
                    cache, downloader, parser, pathStack, resolvedCache, diagnostics, depth = 0
                )
                resolvedBoms.add(result.content)
                diagnostics = result.diagnostics
            }

            // Merge: first BOM has priority
            val versionMap = mutableMapOf<String, Pair<String, String>>()
            for ((index, bom) in resolvedBoms.withIndex()) {
                val bomAlias = bomImports[index].alias
                for (dep in bom.dependencies) {
                    val key = "${dep.group}:${dep.artifact}"
                    versionMap.putIfAbsent(key, dep.version to bomAlias)
                }
            }

            // Enrich libraries
            val updatedLibraries = metadata.libraries.mapValues { (_, lib) ->
                if (lib.version != null) return@mapValues lib
                val key = "${lib.group}:${lib.artifact}"
                val (version, bomAlias) = versionMap[key] ?: return@mapValues lib
                diagnostics = diagnostics + Diagnostics.info(
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

            return metadata.copy(
                libraries = updatedLibraries,
                diagnostics = diagnostics,
            )
        } finally {
            httpClient.close()
        }
    }

    private suspend fun resolveBomRecursive(
        group: String, artifact: String, version: String,
        cache: BomCache, downloader: MavenPomDownloader, parser: PomXmlParser,
        pathStack: MutableSet<String>,
        resolvedCache: MutableMap<String, BomContent>,
        diagnostics: Diagnostics, depth: Int,
    ): BomResolveResult {
        val key = "$group:$artifact:$version"

        if (key in pathStack) {
            return BomResolveResult(
                BomContent(),
                diagnostics + Diagnostics.error(
                    "CIRCULAR_BOM", "Circular BOM dependency detected: $key", id, emptyMap()
                )
            )
        }
        if (depth > MAX_BOM_DEPTH) {
            return BomResolveResult(
                BomContent(),
                diagnostics + Diagnostics.warning(
                    "BOM_DEPTH_EXCEEDED", "BOM parent hierarchy > $MAX_BOM_DEPTH levels for $key", id, emptyMap()
                )
            )
        }

        resolvedCache[key]?.let { return BomResolveResult(it, diagnostics) }

        pathStack.add(key)
        try {
            val cached = cache.get(group, artifact, version)
            if (cached != null) {
                resolvedCache[key] = cached
                return BomResolveResult(cached, diagnostics)
            }

            val pomXml = downloader.downloadPom(group, artifact, version)
            if (pomXml == null) {
                val stale = cache.getStale(group, artifact, version)
                if (stale != null) {
                    resolvedCache[key] = stale
                    return BomResolveResult(
                        stale,
                        diagnostics + Diagnostics.warning(
                            "BOM_STALE_CACHE", "Using stale cache for BOM $key", id, emptyMap()
                        )
                    )
                }
                return BomResolveResult(
                    BomContent(),
                    diagnostics + Diagnostics.error(
                        "BOM_FETCH_FAILED", "Cannot fetch BOM: $key", id, emptyMap()
                    )
                )
            }

            val parseResult = try {
                parser.parseBomContent(pomXml)
            } catch (e: Exception) {
                return BomResolveResult(
                    BomContent(),
                    diagnostics + Diagnostics.error(
                        "INVALID_BOM_XML", "Failed to parse BOM XML for $key: ${e.message}", id, emptyMap()
                    )
                )
            }

            var mergedProperties = parseResult.properties
            val mergedDependencies = mutableListOf<BomDependency>()
            var currentDiagnostics = diagnostics

            if (parseResult.dependencies.isEmpty() && parseResult.parent == null) {
                currentDiagnostics = currentDiagnostics + Diagnostics.warning(
                    "BOM_NO_DEPS", "BOM $key contains no dependencyManagement", id, emptyMap()
                )
            }

            parseResult.parent?.let { parent ->
                val parentResult = resolveBomRecursive(
                    parent.group, parent.artifact, parent.version,
                    cache, downloader, parser, pathStack, resolvedCache, currentDiagnostics, depth + 1
                )
                mergedProperties = parentResult.content.properties + mergedProperties
                mergedDependencies.addAll(parentResult.content.dependencies)
                currentDiagnostics = parentResult.diagnostics
            }

            for (rawDep in parseResult.dependencies) {
                if (rawDep.scope == "import" && rawDep.type == "pom") {
                    val resolved = resolveRawDependency(parser, rawDep, mergedProperties, currentDiagnostics)
                    currentDiagnostics = resolved.second
                    val dep = resolved.first ?: continue
                    val importResult = resolveBomRecursive(
                        dep.group, dep.artifact, dep.version,
                        cache, downloader, parser, pathStack, resolvedCache, currentDiagnostics, depth + 1
                    )
                    mergedDependencies.addAll(importResult.content.dependencies)
                    currentDiagnostics = importResult.diagnostics
                } else {
                    val resolved = resolveRawDependency(parser, rawDep, mergedProperties, currentDiagnostics)
                    currentDiagnostics = resolved.second
                    resolved.first?.let { mergedDependencies.add(it) }
                }
            }

            val content = BomContent(mergedDependencies, mergedProperties)
            try {
                cache.put(group, artifact, version, content)
            } catch (e: Exception) {
                logger.warn { "Failed to write BOM cache for $key: ${e.message}" }
                currentDiagnostics = currentDiagnostics + Diagnostics.warning(
                    "BOM_CACHE_READONLY", "Cannot write cache for $key: ${e.message}", id, emptyMap()
                )
            }
            resolvedCache[key] = content

            return BomResolveResult(content, currentDiagnostics)
        } finally {
            pathStack.remove(key)
        }
    }

    private fun resolveRawDependency(
        parser: PomXmlParser,
        rawDep: RawBomDependency,
        properties: Map<String, String>,
        diagnostics: Diagnostics,
    ): Pair<BomDependency?, Diagnostics> {
        return try {
            BomDependency(
                group = parser.resolveProperty(rawDep.group, properties),
                artifact = parser.resolveProperty(rawDep.artifact, properties),
                version = parser.resolveProperty(rawDep.version, properties),
            ) to diagnostics
        } catch (e: IllegalStateException) {
            null to diagnostics + Diagnostics.error(
                "UNRESOLVED_BOM_PROPERTY",
                "${e.message} in ${rawDep.group}:${rawDep.artifact}:${rawDep.version}",
                id, emptyMap()
            )
        }
    }

    private fun createHttpClient(): HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        engine {
            requestTimeout = HTTP_REQUEST_TIMEOUT_MS
            endpoint {
                connectTimeout = HTTP_CONNECT_TIMEOUT_MS
                keepAliveTime = HTTP_KEEP_ALIVE_MS
            }
        }
        install(HttpTimeout) {
            connectTimeoutMillis = HTTP_CONNECT_TIMEOUT_MS
            requestTimeoutMillis = HTTP_REQUEST_TIMEOUT_MS
        }
    }
}
