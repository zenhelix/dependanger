package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option

public class UpdateLibraryCommand : CliktCommand(name = "update-library") {
    override fun help(context: Context): String = "Update a library in metadata.json"

    public val alias: String by argument(help = "Library alias to update")
    public val version: String? by option("-v", "--version", help = "New version or ref:alias")
    public val tags: String? by option("-t", "--tags", help = "New tags (comma-separated)")
    public val requiresJdk: String? by option("--requires-jdk", help = "Minimum JDK version")
    public val input: String by option("-i", "--input", help = "Input metadata file").default(CliDefaults.METADATA_FILE)
    public val output: String? by option("-o", "--output", help = "Output file (defaults to input)")

    override fun run(): Unit = TODO()
}
