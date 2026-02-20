package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int

public class ResolveTransitivesCommand : CliktCommand(name = "resolve-transitives") {
    override fun help(context: Context): String = "Resolve transitive dependencies"

    public val input: String by option("-i", "--input", help = "Input metadata file").default(CliDefaults.METADATA_FILE)
    public val output: String? by option("-o", "--output", help = "Output file")
    public val format: String by option("--format", help = "Format: json, yaml, tree, text").default(CliDefaults.OUTPUT_FORMAT_TEXT)
    public val depth: Int? by option("--depth", help = "Max resolution depth").int()
    public val includeOptional: Boolean by option("--include-optional", help = "Include optional deps").flag()
    public val repositories: String? by option("--repositories", help = "Maven repos (comma-separated)")
    public val offline: Boolean by option("--offline", help = "Use cache only").flag()
    public val conflictResolution: String by option(
        "--conflict-resolution",
        help = "Strategy: HIGHEST,FIRST,FAIL"
    ).default(CliDefaults.CONFLICT_RESOLUTION_HIGHEST)

    override fun run(): Unit = TODO()
}
