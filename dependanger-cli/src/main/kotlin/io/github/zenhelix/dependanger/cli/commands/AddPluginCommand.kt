package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.core.model.Plugin
import java.nio.file.Path

public class AddPluginCommand : CliktCommand(name = "plugin") {
    override fun help(context: Context): String = "Add a Gradle plugin to metadata.json"

    public val alias: String by argument(help = "Plugin alias")
    public val pluginId: String by argument(help = "Gradle plugin ID")
    public val version: String? by option("-v", "--version", help = "Plugin version or ref:alias")
    public val tags: String? by option("-t", "--tags", help = "Tags (comma-separated)")
    public val input: String by option("-i", "--input", help = "Input metadata file").default(CliDefaults.METADATA_FILE)
    public val output: String? by option("-o", "--output", help = "Output file (defaults to input)")

    override fun run() {
        val formatter = OutputFormatter(terminal = terminal)
        val metadataService = MetadataService()
        withErrorHandling(formatter) {
            val inputPath = Path.of(input)
            val outputPath = Path.of(output ?: input)
            val metadata = metadataService.read(inputPath)

            if (metadata.plugins.any { it.alias == alias }) {
                throw CliException.DuplicateAlias("Plugin", alias)
            }

            val resolvedVersion = parseVersionRef(version)

            val newPlugin = Plugin(
                alias = alias,
                id = pluginId,
                version = resolvedVersion,
                tags = parseCommaSeparated(tags).toSet(),
            )
            val updated = metadata.copy(plugins = metadata.plugins + newPlugin)

            metadataService.write(updated, outputPath)
            formatter.success("Added plugin '$alias'")
        }
    }
}
