package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option

public class ValidateCommand : CliktCommand(name = "validate") {
    override fun help(context: Context): String = "Validate metadata.json or DSL configuration"

    public val input: String by option("-i", "--input", help = "Input file").default("metadata.json")
    public val strict: Boolean by option("--strict", help = "Fail on warnings").flag()
    public val format: String by option("--format", help = "Output format: text, json").default("text")

    override fun run(): Unit = TODO()
}
