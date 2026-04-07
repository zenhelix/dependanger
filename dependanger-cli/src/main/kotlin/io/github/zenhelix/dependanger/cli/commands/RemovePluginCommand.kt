package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import io.github.zenhelix.dependanger.cli.options.MetadataOptions
import io.github.zenhelix.dependanger.cli.runner.MetadataRunner

public class RemovePluginCommand : CliktCommand(name = "plugin") {
    override fun help(context: Context): String = "Remove a plugin from metadata.json"

    public val alias: String by argument(help = "Plugin alias to remove")
    private val opts by MetadataOptions()

    override fun run(): Unit = MetadataRunner(this, opts).run { metadata ->
        if (metadata.plugins.none { it.alias == alias }) {
            throw CliException.AliasNotFound("Plugin", alias)
        }

        val updated = metadata.copy(plugins = metadata.plugins.filter { it.alias != alias })

        updated to "Removed plugin '$alias'"
    }
}
