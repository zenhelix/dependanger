package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.cli.options.MetadataOptions
import io.github.zenhelix.dependanger.cli.runner.MetadataRunner

public class RemoveLibraryCommand : CliktCommand(name = "library") {
    override fun help(context: Context): String = "Remove a library from metadata.json"

    public val alias: String by argument(help = "Library alias to remove")
    public val force: Boolean by option("-f", "--force", help = "Skip dependency checks").flag()
    private val opts by MetadataOptions()

    override fun run(): Unit = MetadataRunner(this, opts).run { metadata ->
        if (metadata.libraries.none { it.alias == alias }) {
            throw CliException.AliasNotFound("Library", alias)
        }

        if (!force) {
            val referencingBundles = metadata.bundles
                .filter { alias in it.libraries }
                .map { it.alias }
            if (referencingBundles.isNotEmpty()) {
                throw CliException.ReferenceConflict(alias, referencingBundles)
            }
        }

        val updated = metadata.copy(libraries = metadata.libraries.filter { it.alias != alias })

        updated to "Removed library '$alias'"
    }
}
