package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option

public class UpdateVersionCommand : CliktCommand(name = "update-version") {
    override fun help(context: Context): String = "Update a version in metadata.json"

    public val alias: String by argument(help = "Version or library alias")
    public val version: String by argument(help = "New version value")
    public val input: String by option("-i", "--input", help = "Input metadata file").default("metadata.json")
    public val output: String? by option("-o", "--output", help = "Output file")
    public val library: Boolean by option("-l", "--library", help = "Update library version").flag()

    override fun run(): Unit = TODO()
}
