package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.cli.options.MetadataOptions
import io.github.zenhelix.dependanger.cli.runner.MetadataRunner
import io.github.zenhelix.dependanger.core.model.Plugin

public class AddPluginCommand : CliktCommand(name = "plugin") {
    override fun help(context: Context): String = "Add a Gradle plugin to metadata.json"

    public val alias: String by argument(help = "Plugin alias")
    public val pluginId: String by argument(help = "Gradle plugin ID")
    public val version: String? by option("-v", "--version", help = "Plugin version or ref:alias")
    public val tags: String? by option("-t", "--tags", help = "Tags (comma-separated)")
    private val opts by MetadataOptions()

    override fun run(): Unit = MetadataRunner(this, opts).run { metadata ->
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

        updated to "Added plugin '$alias'"
    }
}
