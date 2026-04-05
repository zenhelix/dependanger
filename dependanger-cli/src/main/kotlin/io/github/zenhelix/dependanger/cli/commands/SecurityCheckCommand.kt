package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.api.Dependanger
import io.github.zenhelix.dependanger.api.DependangerResult
import io.github.zenhelix.dependanger.api.vulnerabilities
import io.github.zenhelix.dependanger.cli.sarif.renderSarif
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.feature.model.security.VulnerabilityInfo
import io.github.zenhelix.dependanger.feature.model.security.VulnerabilitySeverity
import io.github.zenhelix.dependanger.feature.model.settings.security.SecurityCheckSettings
import io.github.zenhelix.dependanger.feature.model.settings.security.SecurityCheckSettingsKey
import kotlinx.serialization.builtins.ListSerializer
import java.nio.file.Path
import kotlin.io.path.writeText

public class SecurityCheckCommand : CliktCommand(name = "security") {
    override fun help(context: Context): String = "Check libraries for known vulnerabilities"

    public val input: String by option("-i", "--input", help = "Input metadata file").default(CliDefaults.METADATA_FILE)
    public val output: String? by option("-o", "--output", help = "Output report file")
    public val format: String by option("--format", help = "Output format: text, json, sarif").default(CliDefaults.OUTPUT_FORMAT_TEXT)
    public val failOn: String? by option("--fail-on", help = "Fail on severity: CRITICAL,HIGH,MEDIUM,LOW")
    public val ignore: List<String> by option("--ignore", help = "Ignore CVE IDs").multiple()
    public val osvApi: String by option("--osv-api", help = "OSV API URL").default(CliDefaults.OSV_API_URL)
    public val offline: Boolean by option("--offline", help = "Use cache only").flag()
    public val includeTransitives: Boolean by option("--include-transitives", help = "Check transitives").flag()

    override fun run() {
        val jsonMode = format == CliDefaults.OUTPUT_FORMAT_JSON
        val sarifMode = format == CliDefaults.OUTPUT_FORMAT_SARIF
        val formatter = OutputFormatter(jsonMode = jsonMode || sarifMode, terminal = terminal)
        val metadataService = MetadataService()

        withErrorHandling(formatter) {
            val metadata = metadataService.read(Path.of(input))

            val failOnSeverity = if (failOn != null) {
                try {
                    VulnerabilitySeverity.fromString(failOn!!)
                } catch (e: IllegalArgumentException) {
                    throw CliException.InvalidArgument(e.message ?: "Invalid severity: $failOn")
                }
            } else {
                null
            }

            val dependanger = Dependanger.fromMetadata(metadata)
                .preset(ProcessingPreset.STRICT)
                .withContextProperty(SecurityCheckSettingsKey, SecurityCheckSettings(
                    enabled = true,
                    ignoreVulnerabilities = ignore,
                    cacheTtlHours = if (offline) Long.MAX_VALUE else SecurityCheckSettings.DEFAULT_CACHE_TTL_HOURS,
                    failOnVulnerability = SecurityCheckSettings.DEFAULT.failOnVulnerability,
                    minSeverity = SecurityCheckSettings.DEFAULT.minSeverity,
                    timeout = SecurityCheckSettings.DEFAULT_TIMEOUT_MS,
                    parallelism = SecurityCheckSettings.DEFAULT_PARALLELISM,
                    cacheDirectory = null,
                ))
                .build()

            val result = CoroutineRunner.run {
                dependanger.process()
            }

            if (result is DependangerResult.Failure) {
                formatter.renderDiagnostics(result.diagnostics)
                throw CliException.ProcessingFailed("Security check failed")
            }

            val vulnerabilities = result.vulnerabilities

            if (sarifMode) {
                echo(renderSarif(vulnerabilities))
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

            output?.let { outputFile ->
                val outputPath = Path.of(outputFile)
                val outputContent = if (sarifMode) {
                    renderSarif(vulnerabilities)
                } else {
                    CliDefaults.CLI_JSON.encodeToString(ListSerializer(VulnerabilityInfo.serializer()), vulnerabilities)
                }
                outputPath.writeText(outputContent)
                formatter.success("Report written to $outputPath")
            }

            if (failOnSeverity != null && vulnerabilities.any { it.severity.meetsThreshold(failOnSeverity) }) {
                throw ProgramResult(1)
            }
        }
    }
}
