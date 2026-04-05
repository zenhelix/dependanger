package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.cli.options.MetadataOptions
import io.github.zenhelix.dependanger.cli.runner.MetadataRunner
import io.github.zenhelix.dependanger.core.model.Version

public class AddVersionCommand : CliktCommand(name = "version") {
    override fun help(context: Context): String = "Add a version alias to metadata.json"

    public val alias: String by argument(help = "Version alias name")
    public val value: String by argument(help = "Version value")
    public val fallback: String? by option("--fallback", help = "Fallback condition=value (e.g. jdkBelow(17)=2.7.18)")
    private val opts by MetadataOptions()

    override fun run(): Unit = MetadataRunner(this, opts).run { metadata ->
        if (metadata.versions.any { it.name == alias }) {
            throw CliException.DuplicateAlias("Version", alias)
        }

        val newVersion = Version(name = alias, value = value, fallbacks = emptyList())
        val updated = metadata.copy(versions = metadata.versions + newVersion)

        updated to "Added version '$alias'"
    }
}
