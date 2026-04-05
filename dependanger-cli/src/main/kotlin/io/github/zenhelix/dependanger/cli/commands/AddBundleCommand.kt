package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.cli.options.MetadataOptions
import io.github.zenhelix.dependanger.cli.runner.MetadataRunner
import io.github.zenhelix.dependanger.core.model.Bundle

public class AddBundleCommand : CliktCommand(name = "bundle") {
    override fun help(context: Context): String = "Add a bundle to metadata.json"

    public val name: String by argument(help = "Bundle name")
    public val libraries: String? by option("--libraries", help = "Library aliases (comma-separated)")
    public val extends: String? by option("--extends", help = "Bundle names to extend (comma-separated)")
    private val opts by MetadataOptions()

    override fun run(): Unit = MetadataRunner(this, opts).run { metadata ->
        if (metadata.bundles.any { it.alias == name }) {
            throw CliException.DuplicateAlias("Bundle", name)
        }

        val newBundle = Bundle(
            alias = name,
            libraries = parseCommaSeparated(libraries),
            extends = parseCommaSeparated(extends),
        )
        val updated = metadata.copy(bundles = metadata.bundles + newBundle)

        updated to "Added bundle '$name'"
    }
}
