package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import io.github.zenhelix.dependanger.api.compatibilityIssues
import io.github.zenhelix.dependanger.cli.options.PipelineOptions
import io.github.zenhelix.dependanger.cli.runner.PipelineRunner
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.core.model.Severity
import io.github.zenhelix.dependanger.effective.model.CompatibilityIssue
import io.github.zenhelix.dependanger.feature.model.settings.analysis.CompatibilityAnalysisSettings
import io.github.zenhelix.dependanger.feature.model.settings.analysis.CompatibilityAnalysisSettingsKey
import kotlinx.serialization.builtins.ListSerializer

public class AnalyzeCommand : CliktCommand(name = "analyze") {
    override fun help(context: Context): String = "Analyze library compatibility"

    private val opts by PipelineOptions()
    public val output: String? by option("-o", "--output", help = "Output report file")
    public val targetJdk: Int? by option("--target-jdk", help = "Target JDK version").int()
    public val failOnError: Boolean by option("--fail-on-error", help = "Fail on violations").flag()
    public val rules: String? by option("--rules", help = "Rule types to check")

    override fun run(): Unit = PipelineRunner(this, opts).run(
        configure = {
            preset(ProcessingPreset.STRICT)
            withContextProperty(
                CompatibilityAnalysisSettingsKey, CompatibilityAnalysisSettings(
                    enabled = true,
                    targetJdk = targetJdk,
                    targetKotlin = CompatibilityAnalysisSettings.DEFAULT.targetKotlin,
                    minSeverity = CompatibilityAnalysisSettings.DEFAULT.minSeverity,
                    failOnErrors = CompatibilityAnalysisSettings.DEFAULT.failOnErrors,
                )
            )
        },
        handle = { result ->
            val issues = result.compatibilityIssues

            val filteredIssues = rules?.let { rulesFilter ->
                val allowedRules = parseCommaSeparated(rulesFilter).toSet()
                issues.filter { it.ruleId in allowedRules }
            } ?: issues

            renderItems(
                items = filteredIssues,
                serializer = CompatibilityIssue.serializer(),
                headers = listOf("Rule", "Severity", "Message", "Affected Libraries"),
                rowMapper = { issue ->
                    listOf(
                        issue.ruleId,
                        issue.severity.name,
                        issue.message,
                        issue.affectedLibraries.joinToString(", "),
                    )
                },
                emptyMessage = "No compatibility issues found",
                itemNoun = "compatibility issue(s) found",
            )

            writeOutputIfRequested(output, filteredIssues, ListSerializer(CompatibilityIssue.serializer()))

            if (failOnError && filteredIssues.any { it.severity == Severity.ERROR }) {
                throw ProgramResult(1)
            }
        }
    )
}
