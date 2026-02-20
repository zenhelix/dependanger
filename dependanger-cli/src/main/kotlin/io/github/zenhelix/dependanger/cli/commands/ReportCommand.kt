package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option

public class ReportCommand : CliktCommand(name = "report") {
    override fun help(context: Context): String = "Generate comprehensive dependency report"

    public val input: String by option("-i", "--input", help = "Input metadata/effective file").default(CliDefaults.METADATA_FILE)
    public val outputDir: String by option("-o", "--output-dir", help = "Output directory").default(".")
    public val format: String by option("--format", help = "Format: json, yaml, markdown, html").default(CliDefaults.OUTPUT_FORMAT_MARKDOWN)
    public val sections: String? by option("--sections", help = "Sections (comma-separated)")
    public val includeTransitives: Boolean by option("--include-transitives", help = "Include transitive data").flag()

    override fun run(): Unit = TODO()
}
