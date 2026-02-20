package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option

public class AddBomCommand : CliktCommand(name = "add-bom") {
    override fun help(context: Context): String = "Add a BOM import to metadata.json"

    public val coordinates: String by argument(help = "Maven BOM coordinates (group:artifact[:version])")
    public val alias: String? by option("--alias", help = "BOM alias (defaults to artifactId)")
    public val version: String? by option("-v", "--version", help = "BOM version or ref:alias")
    public val input: String by option("-i", "--input", help = "Input metadata file").default(CliDefaults.METADATA_FILE)
    public val output: String? by option("-o", "--output", help = "Output file (defaults to input)")

    override fun run(): Unit = TODO()
}
