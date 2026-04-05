package io.github.zenhelix.dependanger.generators.toml

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.zenhelix.dependanger.core.model.VersionReference.VersionRange
import io.github.zenhelix.dependanger.effective.model.EffectiveBundle
import io.github.zenhelix.dependanger.effective.model.EffectiveLibrary
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.EffectivePlugin
import io.github.zenhelix.dependanger.effective.model.ResolvedVersion
import io.github.zenhelix.dependanger.effective.spi.ArtifactGenerator
import io.github.zenhelix.dependanger.effective.spi.writeStringArtifact
import java.nio.file.Path

public class TomlGenerator(private val config: TomlConfig) : ArtifactGenerator<String> {
    override val generatorId: String = GENERATOR_ID
    override val description: String = "Generates Gradle Version Catalog TOML file"
    override val fileExtension: String = "toml"

    private val logger = KotlinLogging.logger {}

    override fun generate(effective: EffectiveMetadata): String {
        logger.info { "Generating TOML version catalog (generatorId=$generatorId)" }

        val result = buildString {
            if (config.includeComments) {
                appendLine(HEADER_COMMENT)
                appendLine()
            }

            appendSection("versions", generateVersionsSection(effective.versions))
            appendSection("libraries", generateLibrariesSection(effective.libraries, effective.versions))
            appendSection("bundles", generateBundlesSection(effective.bundles))
            appendSection("plugins", generatePluginsSection(effective.plugins, effective.versions))
        }.trimEnd() + "\n"

        logger.info { "TOML generation complete: ${effective.versions.size} versions, ${effective.libraries.size} libraries, ${effective.bundles.size} bundles, ${effective.plugins.size} plugins" }
        return result
    }

    override fun write(artifact: String, output: Path) {
        val targetFile = output.resolve(config.filename)
        logger.info { "Writing TOML to: $targetFile" }
        writeStringArtifact(artifact, output, config.filename)
        logger.info { "TOML written successfully: $targetFile (${artifact.length} chars)" }
    }

    private fun generateVersionsSection(versions: Map<String, ResolvedVersion>): String {
        if (versions.isEmpty()) return ""

        return sortedByAlias(versions.values) { it.alias }.joinToString("\n") { version ->
            "${version.alias} = \"${escapeTomlValue(version.value)}\""
        } + "\n"
    }

    private fun generateLibrariesSection(
        libraries: Map<String, EffectiveLibrary>,
        versions: Map<String, ResolvedVersion>,
    ): String {
        if (libraries.isEmpty()) return ""

        return buildString {
            for (lib in sortedByAlias(libraries.values) { it.alias }) {
                if (lib.isDeprecated && config.includeDeprecationComments) {
                    appendLine(buildDeprecationComment(lib))
                }
                appendLine(buildLibraryEntry(lib, versions))
            }
        }
    }

    private fun buildLibraryEntry(
        lib: EffectiveLibrary,
        versions: Map<String, ResolvedVersion>,
    ): String {
        val group = escapeTomlValue(lib.group)
        val name = escapeTomlValue(lib.artifact)
        val versionPart = lib.version.rangeOrNull?.let { formatRangeVersion(it) }
            ?: resolveVersionPart(lib.version.resolvedOrNull, versions) { alias ->
                logger.warn { "Library '${lib.alias}' version alias '$alias' not found in [versions], falling back to inline version" }
            }

        return "${lib.alias} = { group = \"$group\", name = \"$name\"$versionPart }"
    }

    private fun buildDeprecationComment(lib: EffectiveLibrary): String =
        "# ${lib.deprecationSummary ?: "DEPRECATED"}."

    private fun generateBundlesSection(bundles: Map<String, EffectiveBundle>): String {
        if (bundles.isEmpty()) return ""

        return sortedByAlias(bundles.values) { it.alias }.joinToString("\n") { bundle ->
            val libs = bundle.libraries.joinToString(", ") { "\"$it\"" }
            "${bundle.alias} = [$libs]"
        } + "\n"
    }

    private fun generatePluginsSection(
        plugins: Map<String, EffectivePlugin>,
        versions: Map<String, ResolvedVersion>,
    ): String {
        if (plugins.isEmpty()) return ""

        return sortedByAlias(plugins.values) { it.alias }.joinToString("\n") { plugin ->
            buildPluginEntry(plugin, versions)
        } + "\n"
    }

    private fun buildPluginEntry(
        plugin: EffectivePlugin,
        versions: Map<String, ResolvedVersion>,
    ): String {
        val id = escapeTomlValue(plugin.id)
        val versionPart = plugin.version.rangeOrNull?.let { formatRangeVersion(it) }
            ?: resolveVersionPart(plugin.version.resolvedOrNull, versions) { alias ->
                logger.warn { "Plugin '${plugin.alias}' version alias '$alias' not found in [versions], falling back to inline version" }
            }

        return "${plugin.alias} = { id = \"$id\"$versionPart }"
    }

    private fun formatRangeVersion(range: VersionRange): String = when (range) {
        is VersionRange.Simple -> ", version = \"${escapeTomlValue(range.notation)}\""
        is VersionRange.Rich   -> ", version = { ${formatRichVersionFields(range)} }"
    }

    private fun formatRichVersionFields(rich: VersionRange.Rich): String = buildList {
        rich.require?.let { add("require = \"${escapeTomlValue(it)}\"") }
        rich.strictly?.let { add("strictly = \"${escapeTomlValue(it)}\"") }
        rich.prefer?.let { add("prefer = \"${escapeTomlValue(it)}\"") }
        if (rich.reject.isNotEmpty()) {
            val rejectList = rich.reject.joinToString(", ") { "\"${escapeTomlValue(it)}\"" }
            add("reject = [$rejectList]")
        }
    }.joinToString(", ")

    private fun resolveVersionPart(
        version: ResolvedVersion?,
        versions: Map<String, ResolvedVersion>,
        onFallback: (alias: String) -> Unit = {},
    ): String {
        if (version == null) return ""

        if (config.useInlineVersions) {
            return ", version = \"${escapeTomlValue(version.value)}\""
        }

        if (version.alias in versions) {
            return ", version.ref = \"${version.alias}\""
        }

        onFallback(version.alias)
        return ", version = \"${escapeTomlValue(version.value)}\""
    }

    private fun <T> sortedByAlias(values: Collection<T>, aliasSelector: (T) -> String): List<T> =
        if (config.sortSections) values.sortedBy(aliasSelector) else values.toList()

    private fun StringBuilder.appendSection(name: String, content: String) {
        if (content.isNotEmpty()) {
            appendLine("[$name]")
            appendLine(content)
        }
    }

    private fun escapeTomlValue(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")

    public companion object {
        private const val GENERATOR_ID: String = "toml-version-catalog"
        private const val HEADER_COMMENT: String = "# Generated by Dependanger ($GENERATOR_ID)"
    }

}
