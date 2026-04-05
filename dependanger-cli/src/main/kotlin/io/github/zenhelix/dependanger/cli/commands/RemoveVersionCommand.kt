package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.cli.options.MetadataOptions
import io.github.zenhelix.dependanger.cli.runner.MetadataRunner
import io.github.zenhelix.dependanger.core.model.VersionReference

public class RemoveVersionCommand : CliktCommand(name = "version") {
    override fun help(context: Context): String = "Remove a version alias from metadata.json"

    public val alias: String by argument(help = "Version alias to remove")
    public val force: Boolean by option("-f", "--force", help = "Skip reference checks").flag()
    private val opts by MetadataOptions()

    override fun run(): Unit = MetadataRunner(this, opts).run { metadata ->
        if (metadata.versions.none { it.name == alias }) {
            throw CliException.AliasNotFound("Version", alias)
        }

        if (!force) {
            val referencingLibraries = metadata.libraries
                .filter { it.version is VersionReference.Reference && (it.version as VersionReference.Reference).name == alias }
                .map { it.alias }
            val referencingPlugins = metadata.plugins
                .filter { it.version is VersionReference.Reference && (it.version as VersionReference.Reference).name == alias }
                .map { it.alias }
            val refs = referencingLibraries + referencingPlugins
            if (refs.isNotEmpty()) {
                throw CliException.ReferenceConflict(alias, refs)
            }
        }

        val updated = metadata.copy(versions = metadata.versions.filter { it.name != alias })

        updated to "Removed version '$alias'"
    }
}
