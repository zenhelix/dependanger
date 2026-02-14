package io.github.zenhelix.dependanger.features.transitive

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.zenhelix.dependanger.features.resolver.DownloadResult
import io.github.zenhelix.dependanger.features.transitive.model.TransitiveTree
import io.github.zenhelix.dependanger.maven.pom.parser.PomParseException
import io.github.zenhelix.dependanger.maven.pom.parser.PomParser
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger {}

internal class TransitiveTreeBuilder(
    private val ctx: TransitiveResolverContext,
    private val maxDepth: Int,
    private val maxTransitives: Int,
    private val scopes: List<String>,
    private val includeOptional: Boolean,
) {
    private val parentPomResolver: ParentPomResolver = ParentPomResolver(ctx.pomDownloader)
    private val sessionSeen: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val nodeCounter: AtomicInteger = AtomicInteger(0)

    val totalNodes: Int get() = nodeCounter.get()
    val isLimitExceeded: Boolean get() = nodeCounter.get() >= maxTransitives

    suspend fun buildTrees(
        libraries: List<DirectDependencyInput>,
    ): List<TransitiveTree> = coroutineScope {
        libraries.map { lib ->
            async {
                resolveTree(
                    group = lib.group,
                    artifact = lib.artifact,
                    version = lib.version,
                    scope = null,
                    depth = 0,
                    visited = setOf("${lib.group}:${lib.artifact}"),
                )
            }
        }.awaitAll()
    }

    private suspend fun resolveTree(
        group: String,
        artifact: String,
        version: String,
        scope: String?,
        depth: Int,
        visited: Set<String>,
    ): TransitiveTree {
        val coordinate = "$group:$artifact"

        if (nodeCounter.get() >= maxTransitives) {
            return leaf(group = group, artifact = artifact, version = version, scope = scope)
        }

        if (depth >= maxDepth) {
            logger.warn { "Max depth $maxDepth reached for $coordinate:$version, branch truncated" }
            return leaf(group = group, artifact = artifact, version = version, scope = scope)
        }

        val isDuplicate = !sessionSeen.add(coordinate)
        if (isDuplicate) {
            val cached = ctx.cache.getStale(group, artifact, version)
            return cached?.copy(isDuplicate = true, isCycle = false, children = emptyList(), scope = scope)
                ?: leaf(group = group, artifact = artifact, version = version, scope = scope, isDuplicate = true)
        }

        nodeCounter.incrementAndGet()

        when (val cacheResult = ctx.cache.get(group, artifact, version)) {
            is TransitiveCacheResult.Hit       -> return cacheResult.tree.copy(isDuplicate = false, scope = scope)
            is TransitiveCacheResult.Corrupted -> logger.warn { "Corrupted cache for $coordinate:$version" }
            is TransitiveCacheResult.Miss      -> { /* proceed with resolution */
            }
        }

        val rawPomProject = downloadAndParsePom(group, artifact, version)
        if (rawPomProject == null) {
            val tree = leaf(group = group, artifact = artifact, version = version, scope = scope)
            ctx.cache.put(group, artifact, version, tree)
            return tree
        }

        val pomProject = parentPomResolver.resolveWithParents(rawPomProject)
        val pomDependencies = PomDependencyExtractor.extract(pomProject, scopes, includeOptional)

        val children = coroutineScope {
            pomDependencies.map { dep ->
                async {
                    val childCoordinate = "${dep.group}:${dep.artifact}"
                    if (childCoordinate in visited) {
                        leaf(
                            group = dep.group,
                            artifact = dep.artifact,
                            version = dep.version,
                            scope = dep.scope,
                            isCycle = true,
                        )
                    } else {
                        resolveTree(
                            group = dep.group,
                            artifact = dep.artifact,
                            version = dep.version ?: "UNKNOWN",
                            scope = dep.scope,
                            depth = depth + 1,
                            visited = visited + childCoordinate,
                        )
                    }
                }
            }.awaitAll()
        }

        val tree = TransitiveTree(
            group = group,
            artifact = artifact,
            version = version,
            scope = scope,
            children = children,
            isDuplicate = false,
            isCycle = false,
        )

        ctx.cache.put(group, artifact, version, tree)
        return tree
    }

    private suspend fun downloadAndParsePom(
        group: String,
        artifact: String,
        version: String,
    ): io.github.zenhelix.dependanger.maven.pom.model.PomProject? {
        val coordinate = "$group:$artifact:$version"

        return when (val result = ctx.pomDownloader.downloadPom(group, artifact, version)) {
            is DownloadResult.Success      -> {
                try {
                    PomParser.parse(result.content)
                } catch (e: PomParseException) {
                    logger.warn(e) { "Failed to parse POM for $coordinate" }
                    null
                } catch (e: Exception) {
                    logger.warn(e) { "Unexpected error parsing POM for $coordinate" }
                    null
                }
            }

            is DownloadResult.NotFound     -> {
                logger.debug { "POM not found for $coordinate" }
                null
            }

            is DownloadResult.AuthRequired -> {
                logger.warn { "Authentication required to download POM for $coordinate (${result.url})" }
                null
            }

            is DownloadResult.Failed       -> {
                logger.warn { "Failed to download POM for $coordinate: ${result.error}" }
                null
            }
        }
    }

    companion object {
        internal fun collectCycles(trees: List<TransitiveTree>): List<TransitiveTree> {
            val result = mutableListOf<TransitiveTree>()
            fun traverse(tree: TransitiveTree) {
                if (tree.isCycle) result.add(tree)
                tree.children.forEach { traverse(it) }
            }
            trees.forEach { traverse(it) }
            return result
        }

        private fun leaf(
            group: String,
            artifact: String,
            version: String?,
            scope: String?,
            isDuplicate: Boolean = false,
            isCycle: Boolean = false,
        ): TransitiveTree = TransitiveTree(
            group = group,
            artifact = artifact,
            version = version,
            scope = scope,
            children = emptyList(),
            isDuplicate = isDuplicate,
            isCycle = isCycle,
        )
    }
}

internal data class DirectDependencyInput(
    val group: String,
    val artifact: String,
    val version: String,
)
