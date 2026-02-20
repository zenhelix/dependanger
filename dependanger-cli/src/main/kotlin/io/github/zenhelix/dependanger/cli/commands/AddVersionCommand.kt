package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option

public class AddVersionCommand : CliktCommand(name = "add-version") {
    override fun help(context: Context): String = "Add a version alias to metadata.json"

    public val alias: String by argument(help = "Version alias name")
    public val value: String by argument(help = "Version value")
    public val fallback: String? by option("--fallback", help = "Fallback condition=value (e.g. jdkBelow(17)=2.7.18)")
    public val input: String by option("-i", "--input", help = "Input metadata file").default(CliDefaults.METADATA_FILE)
    public val output: String? by option("-o", "--output", help = "Output file (defaults to input)")

    override fun run(): Unit = TODO()
}
