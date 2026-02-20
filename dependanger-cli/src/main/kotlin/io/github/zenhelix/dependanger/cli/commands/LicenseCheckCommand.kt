package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option

public class LicenseCheckCommand : CliktCommand(name = "license-check") {
    override fun help(context: Context): String = "Check library licenses for compliance"

    public val input: String by option("-i", "--input", help = "Input metadata file").default(CliDefaults.METADATA_FILE)
    public val output: String? by option("-o", "--output", help = "Output report file")
    public val format: String by option("--format", help = "Output format").default(CliDefaults.OUTPUT_FORMAT_TEXT)
    public val allow: String? by option("--allow", help = "Allowed licenses (SPDX IDs)")
    public val deny: String? by option("--deny", help = "Denied licenses (SPDX IDs)")
    public val failOnUnknown: Boolean by option("--fail-on-unknown", help = "Fail if license unknown").flag()
    public val failOnDenied: Boolean by option("--fail-on-denied", help = "Fail on denied license").flag(default = true)
    public val includeTransitives: Boolean by option("--include-transitives", help = "Check transitives").flag()

    override fun run(): Unit = TODO()
}
