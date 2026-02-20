package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option

public class AddDistributionCommand : CliktCommand(name = "add-distribution") {
    override fun help(context: Context): String = "Add a distribution to metadata.json"

    public val name: String by argument(help = "Distribution name")
    public val includeTags: String? by option("--include-tags", help = "Tags to include (comma-separated)")
    public val excludeTags: String? by option("--exclude-tags", help = "Tags to exclude (comma-separated)")
    public val includeBundles: String? by option("--include-bundles", help = "Bundles to include (comma-separated)")
    public val input: String by option("-i", "--input", help = "Input metadata file").default(CliDefaults.METADATA_FILE)
    public val output: String? by option("-o", "--output", help = "Output file (defaults to input)")

    override fun run(): Unit = TODO()
}
