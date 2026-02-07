package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option

public class AddPluginCommand : CliktCommand(name = "add-plugin") {
    override fun help(context: Context): String = "Add a Gradle plugin to metadata.json"

    public val alias: String by argument(help = "Plugin alias")
    public val pluginId: String by argument(help = "Gradle plugin ID")
    public val version: String? by option("-v", "--version", help = "Plugin version or ref:alias")
    public val tags: String? by option("-t", "--tags", help = "Tags (comma-separated)")
    public val input: String by option("-i", "--input", help = "Input metadata file").default("metadata.json")
    public val output: String? by option("-o", "--output", help = "Output file (defaults to input)")

    override fun run(): Unit = TODO()
}
