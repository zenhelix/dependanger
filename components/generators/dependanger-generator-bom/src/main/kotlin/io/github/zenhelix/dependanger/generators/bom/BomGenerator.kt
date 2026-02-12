package io.github.zenhelix.dependanger.generators.bom

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.zenhelix.dependanger.effective.model.EffectiveLibrary
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.spi.ArtifactGenerator
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
        val sb = StringBuilder()
        val indent = if (config.prettyPrint) INDENT else ""
        val nl = if (config.prettyPrint) "\n" else ""

        // XML declaration
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>$nl")

        // <project>
        val attrSep = if (config.prettyPrint) "\n${indent.repeat(2)}" else " "
        sb.append("<project xmlns=\"$MAVEN_POM_NS\"")
        sb.append("${attrSep}xmlns:xsi=\"$MAVEN_XSI_NS\"")
        sb.append("${attrSep}xsi:schemaLocation=\"$MAVEN_XSD_LOCATION\">$nl")

        // Coordinates
        sb.append("$indent<modelVersion>$MODEL_VERSION</modelVersion>$nl")
        sb.append("$indent<groupId>${escapeXml(config.groupId)}</groupId>$nl")
        sb.append("$indent<artifactId>${escapeXml(config.artifactId)}</artifactId>$nl")
        sb.append("$indent<version>${escapeXml(config.version)}</version>$nl")
        sb.append("$indent<packaging>pom</packaging>$nl")

        // name and description (if provided and not blank)
        if (!config.name.isNullOrBlank()) {
            sb.append("$indent<name>${escapeXml(config.name)}</name>$nl")
        }
        if (!config.description.isNullOrBlank()) {
            sb.append("$indent<description>${escapeXml(config.description)}</description>$nl")
        }

        // dependencyManagement
        if (dependencies.isNotEmpty()) {
            sb.append("${nl}$indent<dependencyManagement>$nl")
            sb.append("$indent$indent<dependencies>$nl")

            for (lib in dependencies) {
                if (lib.isDeprecated && config.includeDeprecationComments) {
                    sb.append(buildDeprecationXmlComment(lib, indent))
                }
                sb.append(buildDependencyElement(lib, indent))
            }

            sb.append("$indent$indent</dependencies>$nl")
            sb.append("$indent</dependencyManagement>$nl")
        }

        // </project>
        sb.append("</project>$nl")

        return sb.toString()
    }

    private fun buildDependencyElement(lib: EffectiveLibrary, baseIndent: String): String {
        val nl = if (config.prettyPrint) "\n" else ""
        val indent3 = if (config.prettyPrint) baseIndent.repeat(3) else ""
        val indent4 = if (config.prettyPrint) baseIndent.repeat(4) else ""

        val sb = StringBuilder()
        sb.append("$indent3<dependency>$nl")
        sb.append("$indent4<groupId>${escapeXml(lib.group)}</groupId>$nl")
        sb.append("$indent4<artifactId>${escapeXml(lib.artifact)}</artifactId>$nl")

        val versionValue = lib.version?.value
        if (versionValue != null) {
            sb.append("$indent4<version>${escapeXml(versionValue)}</version>$nl")
        }

        if (lib.isPlatform) {
            sb.append("$indent4<type>pom</type>$nl")
            sb.append("$indent4<scope>import</scope>$nl")
        }

        if (config.includeOptionalDependencies) {
            sb.append("$indent4<optional>true</optional>$nl")
        }

        sb.append("$indent3</dependency>$nl")
        return sb.toString()
    }

    private fun buildDeprecationXmlComment(lib: EffectiveLibrary, baseIndent: String): String {
        val nl = if (config.prettyPrint) "\n" else ""
        val indent3 = if (config.prettyPrint) baseIndent.repeat(3) else ""

        val deprecation = lib.deprecation
        val parts = mutableListOf<String>()

        parts.add(if (deprecation?.message != null) "DEPRECATED: ${deprecation.message}" else "DEPRECATED")

        deprecation?.replacedBy?.let { parts.add("Use $it instead") }
        deprecation?.removalVersion?.let { parts.add("Removal: $it") }

        val commentText = parts.joinToString(". ")
        val safeComment = commentText.replace("--", "- -")
        return "$indent3<!-- $safeComment -->$nl"
    }

    private fun escapeXml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    public companion object {
        private const val GENERATOR_ID: String = "maven-bom"
        private const val MAVEN_POM_NS: String = "http://maven.apache.org/POM/4.0.0"
        private const val MAVEN_XSI_NS: String = "http://www.w3.org/2001/XMLSchema-instance"
        private const val MAVEN_XSD_LOCATION: String =
            "http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"
        private const val MODEL_VERSION: String = "4.0.0"
        private const val INDENT: String = "    "
    }
}
