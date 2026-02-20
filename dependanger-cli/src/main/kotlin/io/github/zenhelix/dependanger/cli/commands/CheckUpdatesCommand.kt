package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option

public class CheckUpdatesCommand : CliktCommand(name = "check-updates") {
    override fun help(context: Context): String = "Check for available library updates"

    public val input: String by option("-i", "--input", help = "Input metadata file").default(CliDefaults.METADATA_FILE)
    public val output: String? by option("-o", "--output", help = "Output report file")
    public val format: String by option("--format", help = "Output format").default(CliDefaults.OUTPUT_FORMAT_TEXT)
    public val includePrerelease: Boolean by option("--include-prerelease", help = "Include prerelease").flag()
    public val exclude: List<String> by option("--exclude", help = "Exclude patterns").multiple()
    public val type: String? by option("--type", help = "Update types: PATCH,MINOR,MAJOR")
    public val repositories: String? by option("--repositories", help = "Maven repos (comma-separated)")
    public val offline: Boolean by option("--offline", help = "Use cache only").flag()
    public val failOnUpdates: Boolean by option("--fail-on-updates", help = "Fail if updates found").flag()

    override fun run(): Unit = TODO()
}
