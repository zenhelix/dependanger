package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.cli.options.MetadataOptions
import io.github.zenhelix.dependanger.cli.runner.MetadataRunner

public class RemoveBundleCommand : CliktCommand(name = "bundle") {
    override fun help(context: Context): String = "Remove a bundle from metadata.json"

    public val name: String by argument(help = "Bundle name to remove")
    public val force: Boolean by option("-f", "--force", help = "Skip dependency checks").flag()
    private val opts by MetadataOptions()

    override fun run(): Unit = MetadataRunner(this, opts).run { metadata ->
        if (metadata.bundles.none { it.alias == name }) {
            throw CliException.AliasNotFound("Bundle", name)
        }

        if (!force) {
            val referencingBundles = metadata.bundles
                .filter { it.alias != name && name in it.extends }
                .map { it.alias }
            if (referencingBundles.isNotEmpty()) {
                throw CliException.ReferenceConflict(name, referencingBundles)
            }
        }

        val updated = metadata.copy(bundles = metadata.bundles.filter { it.alias != name })

        updated to "Removed bundle '$name'"
    }
}
