package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.api.vulnerabilities
import io.github.zenhelix.dependanger.cli.options.PipelineOptions
import io.github.zenhelix.dependanger.cli.runner.PipelineRunner
import io.github.zenhelix.dependanger.cli.sarif.renderSarif
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.feature.model.security.VulnerabilityInfo
import io.github.zenhelix.dependanger.feature.model.security.VulnerabilitySeverity
import io.github.zenhelix.dependanger.feature.model.settings.common.NETWORK_DEFAULT_PARALLELISM
import io.github.zenhelix.dependanger.feature.model.settings.common.NETWORK_DEFAULT_TIMEOUT_MS
import io.github.zenhelix.dependanger.feature.model.settings.security.SecurityCheckSettings
import io.github.zenhelix.dependanger.feature.model.settings.security.SecurityCheckSettingsKey
import kotlinx.serialization.builtins.ListSerializer

public class SecurityCheckCommand : CliktCommand(name = "security") {
    override fun help(context: Context): String = "Check libraries for known vulnerabilities"

    private val opts by PipelineOptions()
    public val output: String? by option("-o", "--output", help = "Output report file")
    public val sarif: Boolean by option("--sarif", help = "Use SARIF output format").flag()
    public val failOn: String? by option("--fail-on", help = "Fail on severity: CRITICAL,HIGH,MEDIUM,LOW")
    public val ignore: List<String> by option("--ignore", help = "Ignore CVE IDs").multiple()
    public val osvApi: String by option("--osv-api", help = "OSV API URL").default(CliDefaults.OSV_API_URL)
    public val offline: Boolean by option("--offline", help = "Use cache only").flag()
    public val includeTransitives: Boolean by option("--include-transitives", help = "Check transitives").flag()

    override fun run() {
        val jsonMode = opts.format == CliDefaults.OUTPUT_FORMAT_JSON
        val sarifMode = sarif

        PipelineRunner(this, opts, jsonMode = jsonMode || sarifMode).run(
            configure = {
                preset(ProcessingPreset.STRICT)
                withContextProperty(
                    SecurityCheckSettingsKey, SecurityCheckSettings(
                        enabled = true,
                        ignoreVulnerabilities = ignore,
                        cacheTtlHours = if (offline) Long.MAX_VALUE else SecurityCheckSettings.DEFAULT.cacheTtlHours,
                        failOnVulnerability = SecurityCheckSettings.DEFAULT.failOnVulnerability,
                        minSeverity = SecurityCheckSettings.DEFAULT.minSeverity,
                        timeout = NETWORK_DEFAULT_TIMEOUT_MS,
                        parallelism = NETWORK_DEFAULT_PARALLELISM,
                        cacheDirectory = null,
                    )
                )
            },
            handle = { result ->
                val failOnSeverity = failOn?.let { value ->
                    try {
                        VulnerabilitySeverity.fromString(value)
                    } catch (e: IllegalArgumentException) {
                        throw CliException.InvalidArgument(e.message ?: "Invalid severity: $value")
                    }
                }

                val vulnerabilities = result.vulnerabilities

                val sarifOutput = if (sarifMode) renderSarif(vulnerabilities) else null

                if (sarifOutput != null) {
                    formatter.println(sarifOutput)
                } else if (jsonMode) {
                    formatter.renderJson(vulnerabilities, ListSerializer(VulnerabilityInfo.serializer()))
                } else {
                    if (vulnerabilities.isEmpty()) {
                        formatter.success("No vulnerabilities found")
                    } else {
                        formatter.renderTable(
                            headers = listOf("Library", "CVE ID", "Severity", "Summary"),
                            rows = vulnerabilities.map { vuln ->
                                listOf(
                                    "${vuln.affectedGroup}:${vuln.affectedArtifact}",
                                    vuln.id,
                                    vuln.severity.name,
                                    vuln.summary,
                                )
                            }
                        )
                        formatter.info("${vulnerabilities.size} vulnerability(ies) found")
                    }
                }

                if (sarifOutput != null) {
                    writeOutputIfRequested(output, sarifOutput)
                } else {
                    writeOutputIfRequested(output, vulnerabilities, ListSerializer(VulnerabilityInfo.serializer()))
                }

                if (failOnSeverity != null && vulnerabilities.any { it.severity.meetsThreshold(failOnSeverity) }) {
                    throw ProgramResult(1)
                }
            }
        )
    }
}
