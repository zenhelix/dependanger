package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.api.Dependanger
import io.github.zenhelix.dependanger.api.vulnerabilities
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.features.security.model.VulnerabilityInfo
import io.github.zenhelix.dependanger.features.security.model.VulnerabilitySeverity
import kotlinx.serialization.builtins.ListSerializer
import java.nio.file.Path
import kotlin.io.path.writeText

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

    override fun run() {
        val jsonMode = format == CliDefaults.OUTPUT_FORMAT_JSON
        val formatter = OutputFormatter(jsonMode = jsonMode)
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

            val updatedSettings = metadata.settings.copy(
                securityCheck = metadata.settings.securityCheck.copy(
                    enabled = true,
                    ignoreVulnerabilities = ignore,
                )
            )
            val updatedMetadata = metadata.copy(settings = updatedSettings)

            val dependanger = Dependanger.fromMetadata(updatedMetadata)
                .preset(ProcessingPreset.STRICT)
                .build()

            val result = CoroutineRunner.run {
                dependanger.process()
            }

            val vulnerabilities = result.vulnerabilities

            if (jsonMode) {
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
                val jsonString = CliDefaults.CLI_JSON.encodeToString(ListSerializer(VulnerabilityInfo.serializer()), vulnerabilities)
                outputPath.writeText(jsonString)
                formatter.success("Report written to $outputPath")
            }

            if (failOnSeverity != null && vulnerabilities.any { it.severity.meetsThreshold(failOnSeverity) }) {
                throw ProgramResult(1)
            }
        }
    }
}
