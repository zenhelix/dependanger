package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.cli.options.MetadataOptions
import io.github.zenhelix.dependanger.cli.runner.MetadataRunner

public class RemoveDistributionCommand : CliktCommand(name = "distribution") {
    override fun help(context: Context): String = "Remove a distribution from metadata.json"

    public val name: String by argument(help = "Distribution name to remove")
    public val force: Boolean by option("-f", "--force", help = "Skip dependency checks").flag()
    private val opts by MetadataOptions()

    override fun run(): Unit = MetadataRunner(this, opts).run { metadata ->
        if (metadata.distributions.none { it.name == name }) {
            throw CliException.AliasNotFound("Distribution", name)
        }

        if (!force) {
            if (metadata.settings.defaultDistribution == name) {
                throw CliException.ReferenceConflict(name, listOf("settings.defaultDistribution"))
            }
        }

        val updated = metadata.copy(distributions = metadata.distributions.filter { it.name != name })

        updated to "Removed distribution '$name'"
    }
}
