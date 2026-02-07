package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option

public class MigrateDeprecatedCommand : CliktCommand(name = "migrate-deprecated") {
    override fun help(context: Context): String = "Migrate deprecated libraries to their replacements"

    public val dryRun: Boolean by option("--dry-run", help = "Show migration plan without executing").flag()
    public val replace: Boolean by option("--replace", help = "Replace deprecated with replacedBy in bundles").flag(default = true)
    public val remove: Boolean by option("--remove", help = "Remove deprecated libraries from metadata").flag()
    public val removeFromBundles: Boolean by option("--remove-from-bundles", help = "Remove deprecated from bundles instead of replacing").flag()
    public val input: String by option("-i", "--input", help = "Input metadata file").default("metadata.json")
    public val output: String? by option("-o", "--output", help = "Output file (defaults to input)")
    public val backup: Boolean by option("--backup", help = "Create backup before modifying").flag()

    override fun run(): Unit = TODO()
}
