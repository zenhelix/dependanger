package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.core.model.VersionReference
import java.nio.file.Path

public class RemoveVersionCommand : CliktCommand(name = "remove-version") {
    override fun help(context: Context): String = "Remove a version alias from metadata.json"

    public val alias: String by argument(help = "Version alias to remove")
    public val input: String by option("-i", "--input", help = "Input metadata file").default(CliDefaults.METADATA_FILE)
    public val output: String? by option("-o", "--output", help = "Output file")
    public val force: Boolean by option("-f", "--force", help = "Skip reference checks").flag()

    override fun run() {
        val formatter = OutputFormatter(terminal = terminal)
        val metadataService = MetadataService()
        withErrorHandling(formatter) {
            val inputPath = Path.of(input)
            val outputPath = Path.of(output ?: input)
            val metadata = metadataService.read(inputPath)

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

            metadataService.write(updated, outputPath)
            formatter.success("Removed version '$alias'")
        }
    }
}
