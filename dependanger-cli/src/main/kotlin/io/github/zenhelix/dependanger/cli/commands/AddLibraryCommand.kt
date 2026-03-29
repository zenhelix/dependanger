package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.core.model.Library
import io.github.zenhelix.dependanger.core.model.VersionReference
import java.nio.file.Path

public class AddLibraryCommand : CliktCommand(name = "add-library") {
    override fun help(context: Context): String = "Add a library to metadata.json"

    public val alias: String by argument(help = "Library alias")
    public val coordinates: String by argument(help = "Maven coordinates (group:artifact[:version])")
    public val version: String? by option("-v", "--version", help = "Library version")
    public val versionRef: String? by option("--version-ref", help = "Named version reference")
    public val tags: String? by option("-t", "--tags", help = "Tags (comma-separated)")
    public val input: String by option("-i", "--input", help = "Input metadata file").default(CliDefaults.METADATA_FILE)
    public val output: String? by option("-o", "--output", help = "Output file (defaults to input)")

    override fun run() {
        val formatter = OutputFormatter(terminal = terminal)
        val metadataService = MetadataService()
        withErrorHandling(formatter) {
            val inputPath = Path.of(input)
            val outputPath = Path.of(output ?: input)
            val metadata = metadataService.read(inputPath)

            val coords = parseMavenCoordinates(coordinates)

            val resolvedVersion: VersionReference? = when {
                versionRef != null -> VersionReference.Reference(name = versionRef!!)
                version != null    -> VersionReference.Literal(version = version!!)
                else               -> coords.version?.let { VersionReference.Literal(version = it) }
            }

            if (metadata.libraries.any { it.alias == alias }) {
                throw CliException.DuplicateAlias("Library", alias)
            }

            val newLibrary = Library(
                alias = alias,
                group = coords.group,
                artifact = coords.artifact,
                version = resolvedVersion,
                description = null,
                tags = parseCommaSeparated(tags).toSet(),
                requires = null,
                deprecation = null,
                license = null,
                constraints = emptyList(),
                isPlatform = false,
            )
            val updated = metadata.copy(libraries = metadata.libraries + newLibrary)

            metadataService.write(updated, outputPath)
            formatter.success("Added library '$alias'")
        }
    }
}
