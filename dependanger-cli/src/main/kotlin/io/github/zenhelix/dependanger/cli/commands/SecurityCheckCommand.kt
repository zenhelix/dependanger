package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option

public class SecurityCheckCommand : CliktCommand(name = "security-check") {
    override fun help(context: Context): String = "Check libraries for known vulnerabilities"

    public val input: String by option("-i", "--input", help = "Input metadata file").default(CliDefaults.METADATA_FILE)
    public val output: String? by option("-o", "--output", help = "Output report file")
    public val format: String by option("--format", help = "Output format: text, json, sarif").default(CliDefaults.OUTPUT_FORMAT_TEXT)
    public val failOn: String? by option("--fail-on", help = "Fail on severity: CRITICAL,HIGH,MEDIUM,LOW")
    public val ignore: List<String> by option("--ignore", help = "Ignore CVE IDs").multiple()
    public val osvApi: String by option("--osv-api", help = "OSV API URL").default(CliDefaults.OSV_API_URL)
    public val offline: Boolean by option("--offline", help = "Use cache only").flag()
    public val includeTransitives: Boolean by option("--include-transitives", help = "Check transitives").flag()

    override fun run(): Unit = TODO()
}
