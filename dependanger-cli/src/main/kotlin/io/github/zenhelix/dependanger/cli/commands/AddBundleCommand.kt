package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.core.model.Bundle
import java.nio.file.Path

public class AddBundleCommand : CliktCommand(name = "add-bundle") {
    override fun help(context: Context): String = "Add a bundle to metadata.json"

    public val name: String by argument(help = "Bundle name")
    public val libraries: String? by option("--libraries", help = "Library aliases (comma-separated)")
    public val extends: String? by option("--extends", help = "Bundle names to extend (comma-separated)")
    public val input: String by option("-i", "--input", help = "Input metadata file").default(CliDefaults.METADATA_FILE)
    public val output: String? by option("-o", "--output", help = "Output file (defaults to input)")

    override fun run() {
        val formatter = OutputFormatter()
        val metadataService = MetadataService()
        withErrorHandling(formatter) {
            val inputPath = Path.of(input)
            val outputPath = Path.of(output ?: input)
            val metadata = metadataService.read(inputPath)

            if (metadata.bundles.any { it.alias == name }) {
                throw CliException.DuplicateAlias("Bundle", name)
            }

            val newBundle = Bundle(
                alias = name,
                libraries = parseCommaSeparated(libraries),
                extends = parseCommaSeparated(extends),
            )
            val updated = metadata.copy(bundles = metadata.bundles + newBundle)

            metadataService.write(updated, outputPath)
            formatter.success("Added bundle '$name'")
        }
    }
}
