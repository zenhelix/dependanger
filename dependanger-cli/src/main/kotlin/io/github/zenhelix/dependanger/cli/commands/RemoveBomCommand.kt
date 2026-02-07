package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option

public class RemoveBomCommand : CliktCommand(name = "remove-bom") {
    override fun help(context: Context): String = "Remove a BOM import from metadata.json"

    public val alias: String by argument(help = "BOM alias to remove")
    public val input: String by option("-i", "--input", help = "Input metadata file").default("metadata.json")
    public val output: String? by option("-o", "--output", help = "Output file")
    public val force: Boolean by option("-f", "--force", help = "Skip dependency checks").flag()

    override fun run(): Unit = TODO()
}
