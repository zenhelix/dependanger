package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.core.model.VersionReference
import java.nio.file.Path

public class UpdateVersionCommand : CliktCommand(name = "update-version") {
    override fun help(context: Context): String = "Update a version in metadata.json"

    public val alias: String by argument(help = "Version or library alias")
    public val version: String by argument(help = "New version value")
    public val input: String by option("-i", "--input", help = "Input metadata file").default(CliDefaults.METADATA_FILE)
    public val output: String? by option("-o", "--output", help = "Output file")
    public val library: Boolean by option("-l", "--library", help = "Update library version").flag()

    override fun run() {
        val formatter = OutputFormatter()
        val metadataService = MetadataService()
        withErrorHandling(formatter) {
            val inputPath = Path.of(input)
            val outputPath = Path.of(output ?: input)
            val metadata = metadataService.read(inputPath)

            val updated = if (library) {
                if (metadata.libraries.none { it.alias == alias }) {
                    throw CliException.AliasNotFound("Library", alias)
                }
                metadata.copy(
                    libraries = metadata.libraries.map { lib ->
                        if (lib.alias == alias) lib.copy(version = VersionReference.Literal(version)) else lib
                    }
                )
            } else {
                if (metadata.versions.none { it.name == alias }) {
                    throw CliException.AliasNotFound("Version", alias)
                }
                metadata.copy(
                    versions = metadata.versions.map { ver ->
                        if (ver.name == alias) ver.copy(value = version) else ver
                    }
                )
            }

            metadataService.write(updated, outputPath)
            val entity = if (library) "library" else "version"
            formatter.success("Updated $entity '$alias' to '$version'")
        }
    }
}
