package io.github.zenhelix.dependanger.features.transitive

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.zenhelix.dependanger.features.resolver.DownloadResult
import io.github.zenhelix.dependanger.features.resolver.MavenPomDownloader
import io.github.zenhelix.dependanger.maven.pom.model.PomDependency
import io.github.zenhelix.dependanger.maven.pom.model.PomDependencyManagement
import io.github.zenhelix.dependanger.maven.pom.model.PomProject
import io.github.zenhelix.dependanger.maven.pom.parser.PomParseException
import io.github.zenhelix.dependanger.maven.pom.parser.PomParser
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

private const val MAX_PARENT_DEPTH = 10

internal class ParentPomResolver(
    private val pomDownloader: MavenPomDownloader,
) {
    private val parentCache = ConcurrentHashMap<String, PomProject?>()

    suspend fun resolveWithParents(pomProject: PomProject): PomProject {
        if (pomProject.parent == null) return pomProject
        return resolveParentChain(pomProject, depth = 0, pathStack = mutableSetOf())
    }

    private suspend fun resolveParentChain(
        pomProject: PomProject,
        depth: Int,
        pathStack: MutableSet<String>,
    ): PomProject {
        val parent = pomProject.parent ?: return pomProject
        val parentCoordinate = "${parent.coordinates.groupId}:${parent.coordinates.artifactId}:${parent.coordinates.version}"

        if (depth >= MAX_PARENT_DEPTH) {
            logger.warn { "Max parent depth $MAX_PARENT_DEPTH reached at $parentCoordinate, stopping parent resolution" }
            return pomProject
        }

        if (!pathStack.add(parentCoordinate)) {
            logger.warn { "Circular parent detected: $parentCoordinate, stopping parent resolution" }
            return pomProject
        }

        val parentProject = downloadAndParseParent(
            parent.coordinates.groupId,
            parent.coordinates.artifactId,
            parent.coordinates.version,
        )

        if (parentProject == null) {
            return pomProject
        }

        val resolvedParent = resolveParentChain(parentProject, depth + 1, pathStack)
        return mergeWithParent(child = pomProject, parent = resolvedParent)
    }

    private fun mergeWithParent(child: PomProject, parent: PomProject): PomProject {
        val mergedProperties = parent.properties + child.properties
        val mergedDependencyManagement = mergeDependencyManagement(parent.dependencyManagement, child.dependencyManagement)
        val mergedDependencies = mergeDependencies(parent.dependencies, child.dependencies)

        return child.copy(
            properties = mergedProperties,
            dependencyManagement = mergedDependencyManagement,
            dependencies = mergedDependencies,
        )
    }

    private fun mergeDependencyManagement(
        parentDm: PomDependencyManagement?,
        childDm: PomDependencyManagement?,
    ): PomDependencyManagement? {
        if (parentDm == null && childDm == null) return null
        val parentDeps = parentDm?.dependencies.orEmpty()
        val childDeps = childDm?.dependencies.orEmpty()
        val merged = mergeDependencies(parentDeps, childDeps)
        return PomDependencyManagement(dependencies = merged)
    }

    private fun mergeDependencies(
        parentDeps: List<PomDependency>,
        childDeps: List<PomDependency>,
    ): List<PomDependency> {
        if (parentDeps.isEmpty()) return childDeps
        if (childDeps.isEmpty()) return parentDeps

        val childKeys = childDeps.associateBy { "${it.groupId}:${it.artifactId}" }
        val merged = mutableListOf<PomDependency>()

        for (parentDep in parentDeps) {
            val key = "${parentDep.groupId}:${parentDep.artifactId}"
            if (key !in childKeys) {
                merged.add(parentDep)
            }
        }
        merged.addAll(childDeps)

        return merged
    }

    private suspend fun downloadAndParseParent(
        group: String,
        artifact: String,
        version: String,
    ): PomProject? {
        val coordinate = "$group:$artifact:$version"

        parentCache[coordinate]?.let { return it }
        if (parentCache.containsKey(coordinate)) return null

        val result = when (val downloadResult = pomDownloader.downloadPom(group, artifact, version)) {
            is DownloadResult.Success      -> {
                try {
                    PomParser.parse(downloadResult.content)
                } catch (e: PomParseException) {
                    logger.warn(e) { "Failed to parse parent POM for $coordinate" }
                    null
                } catch (e: Exception) {
                    logger.warn(e) { "Unexpected error parsing parent POM for $coordinate" }
                    null
                }
            }

            is DownloadResult.NotFound     -> {
                logger.debug { "Parent POM not found for $coordinate" }
                null
            }

            is DownloadResult.AuthRequired -> {
                logger.warn { "Authentication required for parent POM $coordinate (${downloadResult.url})" }
                null
            }

            is DownloadResult.Failed       -> {
                logger.warn { "Failed to download parent POM for $coordinate: ${downloadResult.error}" }
                null
            }
        }

        parentCache[coordinate] = result
        return result
    }
}
