package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import io.github.zenhelix.dependanger.api.Dependanger
import io.github.zenhelix.dependanger.api.compatibilityIssues
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.core.model.Severity
import io.github.zenhelix.dependanger.effective.model.CompatibilityIssue
import kotlinx.serialization.builtins.ListSerializer
import java.nio.file.Path
import kotlin.io.path.writeText

public class AnalyzeCommand : CliktCommand(name = "analyze") {
    override fun help(context: Context): String = "Analyze library compatibility"

    public val input: String by option("-i", "--input", help = "Input metadata file").default(CliDefaults.METADATA_FILE)
    public val output: String? by option("-o", "--output", help = "Output report file")
    public val format: String by option("--format", help = "Output format").default(CliDefaults.OUTPUT_FORMAT_TEXT)
    public val targetJdk: Int? by option("--target-jdk", help = "Target JDK version").int()
    public val failOnError: Boolean by option("--fail-on-error", help = "Fail on violations").flag()
    public val rules: String? by option("--rules", help = "Rule types to check")

    override fun run() {
        val jsonMode = format == CliDefaults.OUTPUT_FORMAT_JSON
        val formatter = OutputFormatter(jsonMode = jsonMode)
        val metadataService = MetadataService()

        withErrorHandling(formatter) {
            val metadata = metadataService.read(Path.of(input))

            val updatedSettings = metadata.settings.copy(
                compatibilityAnalysis = metadata.settings.compatibilityAnalysis.copy(
                    enabled = true,
                    targetJdk = targetJdk ?: metadata.settings.compatibilityAnalysis.targetJdk,
                )
            )
            val updatedMetadata = metadata.copy(settings = updatedSettings)

            val dependanger = Dependanger.fromMetadata(updatedMetadata)
                .preset(ProcessingPreset.STRICT)
                .build()

            val result = CoroutineRunner.run {
                dependanger.process()
            }

            val issues = result.compatibilityIssues

            val filteredIssues = rules?.let { rulesFilter ->
                val allowedRules = rulesFilter.split(",").map { it.trim() }.toSet()
                issues.filter { it.ruleId in allowedRules }
            } ?: issues

            if (jsonMode) {
                formatter.renderJson(filteredIssues, ListSerializer(CompatibilityIssue.serializer()))
            } else {
                if (filteredIssues.isEmpty()) {
                    formatter.success("No compatibility issues found")
                } else {
                    formatter.renderTable(
                        headers = listOf("Rule", "Severity", "Message", "Affected Libraries"),
                        rows = filteredIssues.map { issue ->
                            listOf(
                                issue.ruleId,
                                issue.severity.name,
                                issue.message,
                                issue.affectedLibraries.joinToString(", "),
                            )
                        }
                    )
                    formatter.info("${filteredIssues.size} compatibility issue(s) found")
                }
            }

            output?.let { outputFile ->
                val outputPath = Path.of(outputFile)
                val jsonString = CliDefaults.CLI_JSON.encodeToString(ListSerializer(CompatibilityIssue.serializer()), filteredIssues)
                outputPath.writeText(jsonString)
                formatter.success("Report written to $outputPath")
            }

            if (failOnError && filteredIssues.any { it.severity == Severity.ERROR }) {
                throw ProgramResult(1)
            }
        }
    }
}
