package io.github.zenhelix.dependanger.features.resolver

import io.github.zenhelix.dependanger.maven.pom.model.PomProject
import io.github.zenhelix.dependanger.maven.pom.parser.PomParser
import io.github.zenhelix.dependanger.maven.pom.util.PropertyResolutionException
import io.github.zenhelix.dependanger.maven.pom.util.PropertyResolver

public class PomXmlParser {

    private val pomParser: PomParser = PomParser()

    public fun parseBomContent(pomXml: String): PomParseResult {
        val project = pomParser.parse(pomXml)
        return toPomParseResult(project)
    }

    public fun resolveProperty(
        value: String,
        properties: Map<String, String>,
    ): String {
        val resolver = PropertyResolver(properties)
        try {
            return resolver.resolve(value)
        } catch (e: PropertyResolutionException) {
            throw IllegalStateException(e.message, e)
        }
    }

    private fun toPomParseResult(project: PomProject): PomParseResult {
        val parent = project.parent?.let {
            ParentPom(
                group = it.coordinates.groupId,
                artifact = it.coordinates.artifactId,
                version = it.coordinates.version,
            )
        }

        val syntheticProperties = buildSyntheticProperties(project)
        val explicitProperties = project.properties.entries
        val properties = syntheticProperties + explicitProperties

        val rawDependencies = project.dependencyManagement?.dependencies?.map {
            RawBomDependency(
                group = it.groupId,
                artifact = it.artifactId,
                version = it.version ?: "",
                scope = it.scope,
                type = it.type,
            )
        } ?: emptyList()

        return PomParseResult(properties, parent, rawDependencies)
    }

    private fun buildSyntheticProperties(project: PomProject): Map<String, String> {
        val props = mutableMapOf<String, String>()

        props["project.groupId"] = project.coordinates.groupId
        props["project.artifactId"] = project.coordinates.artifactId
        props["project.version"] = project.coordinates.version

        project.parent?.let { p ->
            props["project.parent.groupId"] = p.coordinates.groupId
            props["project.parent.artifactId"] = p.coordinates.artifactId
            props["project.parent.version"] = p.coordinates.version
        }

        return props
    }
}
