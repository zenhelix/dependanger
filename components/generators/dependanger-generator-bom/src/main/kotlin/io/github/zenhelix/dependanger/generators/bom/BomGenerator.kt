package io.github.zenhelix.dependanger.generators.bom

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.zenhelix.dependanger.effective.model.EffectiveLibrary
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.spi.ArtifactGenerator
import io.github.zenhelix.dependanger.maven.pom.model.PomCoordinates
import io.github.zenhelix.dependanger.maven.pom.model.PomDependency
import io.github.zenhelix.dependanger.maven.pom.model.PomDependencyManagement
import io.github.zenhelix.dependanger.maven.pom.model.PomProject
import io.github.zenhelix.dependanger.maven.pom.writer.PomWriter
import io.github.zenhelix.dependanger.maven.pom.writer.PomWriterConfig
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

public class BomGenerator(private val config: BomConfig) : ArtifactGenerator<String> {
    override val generatorId: String = GENERATOR_ID
    override val description: String = "Generates Maven BOM (Bill of Materials) POM file"
    override val fileExtension: String = "xml"

    private val logger = KotlinLogging.logger {}

    override fun generate(effective: EffectiveMetadata): String {
        validateConfig()

        logger.info { "Generating Maven BOM (generatorId=$generatorId) for ${config.groupId}:${config.artifactId}:${config.version}" }

        val dependencies = prepareDependencies(effective)
        val xml = buildXml(dependencies)

        logger.info { "BOM generation complete: ${dependencies.size} dependencies" }
        return xml
    }

    override fun write(artifact: String, output: Path) {
        val targetFile = output.resolve(config.filename)

        logger.info { "Writing BOM to: $targetFile" }

        Files.createDirectories(targetFile.parent)
        Files.writeString(targetFile, artifact, StandardCharsets.UTF_8)

        logger.info { "BOM written successfully: $targetFile (${artifact.length} chars)" }
    }

    private fun validateConfig() {
        require(config.groupId.isNotBlank()) { "BomConfig.groupId must not be blank" }
        require(config.artifactId.isNotBlank()) { "BomConfig.artifactId must not be blank" }
        require(config.version.isNotBlank()) { "BomConfig.version must not be blank" }
    }

    private fun prepareDependencies(effective: EffectiveMetadata): List<EffectiveLibrary> {
        val noVersion = effective.libraries.values.filter { it.version == null }
        if (noVersion.isNotEmpty()) {
            logger.warn { "Skipping ${noVersion.size} libraries without version: ${noVersion.map { it.alias }}" }
        }

        return effective.libraries.values
            .filter { it.version != null }
            .sortedWith(compareBy({ it.group }, { it.artifact }))
    }

    private fun buildXml(dependencies: List<EffectiveLibrary>): String {
        val pomDependencies = dependencies.map { lib ->
            PomDependency(
                groupId = lib.group,
                artifactId = lib.artifact,
                version = lib.version?.value,
                type = if (lib.isPlatform) "pom" else null,
                scope = if (lib.isPlatform) "import" else null,
                optional = config.includeOptionalDependencies,
            )
        }

        val project = PomProject(
            coordinates = PomCoordinates(config.groupId, config.artifactId, config.version),
            packaging = "pom",
            name = config.name,
            description = config.description,
            dependencyManagement = if (pomDependencies.isNotEmpty()) PomDependencyManagement(pomDependencies) else null,
        )

        val writerConfig = PomWriterConfig(
            prettyPrint = config.prettyPrint,
            indent = INDENT,
        )

        val writer = PomWriter(writerConfig)

        val dependencyComments = if (config.includeDeprecationComments) {
            dependencies.withIndex()
                .filter { (_, lib) -> lib.isDeprecated }
                .associate { (index, lib) -> index to buildDeprecationComment(lib) }
        } else {
            emptyMap()
        }

        return writer.write(project, dependencyComments)
    }

    private fun buildDeprecationComment(lib: EffectiveLibrary): String {
        val parts = lib.deprecation?.toCommentParts() ?: listOf("DEPRECATED")
        return parts.joinToString(". ")
    }

    public companion object {
        private const val GENERATOR_ID: String = "maven-bom"
        private const val INDENT: String = "    "
    }
}
