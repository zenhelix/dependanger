package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.core.model.BomImport
import io.github.zenhelix.dependanger.core.model.VersionReference
import java.nio.file.Path

public class AddBomCommand : CliktCommand(name = "add-bom") {
    override fun help(context: Context): String = "Add a BOM import to metadata.json"

    public val coordinates: String by argument(help = "Maven BOM coordinates (group:artifact[:version])")
    public val alias: String? by option("--alias", help = "BOM alias (defaults to artifactId)")
    public val version: String? by option("-v", "--version", help = "BOM version or ref:alias")
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
            val resolvedAlias = alias ?: coords.artifact
            val resolvedVersion = version?.let { parseVersionRef(it) }
                ?: coords.version?.let { VersionReference.Literal(version = it) }
                ?: throw CliException.InvalidArgument("BOM version is required")

            if (metadata.bomImports.any { it.alias == resolvedAlias }) {
                throw CliException.DuplicateAlias("BOM import", resolvedAlias)
            }

            val newBom = BomImport(
                alias = resolvedAlias,
                group = coords.group,
                artifact = coords.artifact,
                version = resolvedVersion,
            )
            val updated = metadata.copy(bomImports = metadata.bomImports + newBom)

            metadataService.write(updated, outputPath)
            formatter.success("Added BOM import '$resolvedAlias'")
        }
    }
}
