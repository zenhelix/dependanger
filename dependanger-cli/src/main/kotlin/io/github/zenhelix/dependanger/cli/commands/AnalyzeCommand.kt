package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int

public class AnalyzeCommand : CliktCommand(name = "analyze") {
    override fun help(context: Context): String = "Analyze library compatibility"

    public val input: String by option("-i", "--input", help = "Input metadata file").default("metadata.json")
    public val output: String? by option("-o", "--output", help = "Output report file")
    public val format: String by option("--format", help = "Output format").default("text")
    public val targetJdk: Int? by option("--target-jdk", help = "Target JDK version").int()
    public val failOnError: Boolean by option("--fail-on-error", help = "Fail on violations").flag()
    public val rules: String? by option("--rules", help = "Rule types to check")

    override fun run(): Unit = TODO()
}
