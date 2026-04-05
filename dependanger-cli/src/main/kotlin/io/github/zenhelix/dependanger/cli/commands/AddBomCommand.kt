package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.cli.options.MetadataOptions
import io.github.zenhelix.dependanger.cli.runner.MetadataRunner
import io.github.zenhelix.dependanger.core.model.BomImport
import io.github.zenhelix.dependanger.core.model.VersionReference

public class AddBomCommand : CliktCommand(name = "bom") {
    override fun help(context: Context): String = "Add a BOM import to metadata.json"

    public val coordinates: String by argument(help = "Maven BOM coordinates (group:artifact[:version])")
    public val alias: String? by option("--alias", help = "BOM alias (defaults to artifactId)")
    public val version: String? by option("-v", "--version", help = "BOM version or ref:alias")
    private val opts by MetadataOptions()

    override fun run(): Unit = MetadataRunner(this, opts).run { metadata ->
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

        updated to "Added BOM import '$resolvedAlias'"
    }
}
