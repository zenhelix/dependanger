package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import java.nio.file.Path

public class RemoveDistributionCommand : CliktCommand(name = "distribution") {
    override fun help(context: Context): String = "Remove a distribution from metadata.json"

    public val name: String by argument(help = "Distribution name to remove")
    public val input: String by option("-i", "--input", help = "Input metadata file").default(CliDefaults.METADATA_FILE)
    public val output: String? by option("-o", "--output", help = "Output file")
    public val force: Boolean by option("-f", "--force", help = "Skip dependency checks").flag()

    override fun run() {
        val formatter = OutputFormatter(terminal = terminal)
        val metadataService = MetadataService()
        withErrorHandling(formatter) {
            val inputPath = Path.of(input)
            val outputPath = Path.of(output ?: input)
            val metadata = metadataService.read(inputPath)

            if (metadata.distributions.none { it.name == name }) {
                throw CliException.AliasNotFound("Distribution", name)
            }

            if (!force) {
                if (metadata.settings.defaultDistribution == name) {
                    throw CliException.ReferenceConflict(name, listOf("settings.defaultDistribution"))
                }
            }

            val updated = metadata.copy(distributions = metadata.distributions.filter { it.name != name })

            metadataService.write(updated, outputPath)
            formatter.success("Removed distribution '$name'")
        }
    }
}
