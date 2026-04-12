package io.github.zenhelix.dependanger.features.transitive

import io.github.zenhelix.dependanger.maven.pom.model.PomDependency
import io.github.zenhelix.dependanger.maven.pom.model.PomProject
import io.github.zenhelix.dependanger.maven.pom.util.PropertyResolver

internal data class ResolvedPomDependency(
    val group: String,
    val artifact: String,
    val version: String?,
    val scope: String,
    val optional: Boolean,
)

internal object PomDependencyExtractor {

    fun extract(
        pomProject: PomProject,
        scopes: List<String>,
        includeOptional: Boolean,
    ): List<ResolvedPomDependency> {
        val properties = buildProperties(pomProject)
        val resolver = PropertyResolver(properties)
        val managedVersions = buildManagedVersions(pomProject, resolver)

        return pomProject.dependencies.mapNotNull { dep ->
            toResolvedDependency(dep, resolver, managedVersions, scopes, includeOptional)
        }
    }

    private fun buildProperties(pomProject: PomProject): Map<String, String> {
        val props = mutableMapOf<String, String>()

        props["project.groupId"] = pomProject.coordinates.groupId
        props["project.artifactId"] = pomProject.coordinates.artifactId
        props["project.version"] = pomProject.coordinates.version

        pomProject.parent?.let { parent ->
            props["project.parent.groupId"] = parent.coordinates.groupId
            props["project.parent.artifactId"] = parent.coordinates.artifactId
            props["project.parent.version"] = parent.coordinates.version
        }

        props.putAll(pomProject.properties.entries)

        return props
    }

    private fun buildManagedVersions(pomProject: PomProject, resolver: PropertyResolver): Map<String, String> {
        val dm = pomProject.dependencyManagement ?: return emptyMap()
        return dm.dependencies
            .mapNotNull { dep ->
                val rawVersion = dep.version ?: return@mapNotNull null
                val group = resolver.resolveOrNull(dep.groupId) ?: dep.groupId
                val artifact = resolver.resolveOrNull(dep.artifactId) ?: dep.artifactId
                val version = resolver.resolveOrNull(rawVersion) ?: rawVersion
                "$group:$artifact" to version
            }
            .toMap()
    }

    private fun toResolvedDependency(
        dep: PomDependency,
        resolver: PropertyResolver,
        managedVersions: Map<String, String>,
        scopes: List<String>,
        includeOptional: Boolean,
    ): ResolvedPomDependency? {
        val group = resolver.resolveOrNull(dep.groupId) ?: return null
        val artifact = resolver.resolveOrNull(dep.artifactId) ?: return null
        val scope = dep.scope ?: "compile"
        val optional = dep.optional

        if (scope !in scopes) return null
        if (optional && !includeOptional) return null

        val version = dep.version?.let { resolver.resolveOrNull(it) }
            ?: managedVersions["$group:$artifact"]

        return ResolvedPomDependency(
            group = group,
            artifact = artifact,
            version = version,
            scope = scope,
            optional = optional,
        )
    }
}
