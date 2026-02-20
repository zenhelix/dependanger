package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option

public class RemoveBundleCommand : CliktCommand(name = "remove-bundle") {
    override fun help(context: Context): String = "Remove a bundle from metadata.json"

    public val name: String by argument(help = "Bundle name to remove")
    public val input: String by option("-i", "--input", help = "Input metadata file").default(CliDefaults.METADATA_FILE)
    public val output: String? by option("-o", "--output", help = "Output file")
    public val force: Boolean by option("-f", "--force", help = "Skip dependency checks").flag()

    override fun run(): Unit = TODO()
}
