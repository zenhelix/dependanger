package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.api.Dependanger
import io.github.zenhelix.dependanger.api.licenseViolations
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.feature.model.license.LicenseViolation
import io.github.zenhelix.dependanger.feature.model.license.LicenseViolationType
import io.github.zenhelix.dependanger.feature.model.settings.license.LicenseCheckSettings
import io.github.zenhelix.dependanger.feature.model.settings.license.LicenseCheckSettingsKey
import kotlinx.serialization.builtins.ListSerializer
import java.nio.file.Path
import kotlin.io.path.writeText

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

    override fun run() {
        val jsonMode = format == CliDefaults.OUTPUT_FORMAT_JSON
        val formatter = OutputFormatter(jsonMode = jsonMode, terminal = terminal)
        val metadataService = MetadataService()

        withErrorHandling(formatter) {
            val metadata = metadataService.read(Path.of(input))

            val allowedLicenses = allow?.let { parseCommaSeparated(it) } ?: LicenseCheckSettings.DEFAULT.allowedLicenses
            val deniedLicenses = deny?.let { parseCommaSeparated(it) } ?: LicenseCheckSettings.DEFAULT.deniedLicenses

            val dependanger = Dependanger.fromMetadata(metadata)
                .preset(ProcessingPreset.STRICT)
                .withContextProperty(LicenseCheckSettingsKey, LicenseCheckSettings(
                    enabled = true,
                    allowedLicenses = allowedLicenses,
                    deniedLicenses = deniedLicenses,
                    failOnDenied = failOnDenied,
                    failOnUnknown = failOnUnknown,
                    includeTransitives = includeTransitives,
                    dualLicensePolicy = LicenseCheckSettings.DEFAULT.dualLicensePolicy,
                    failOnCopyleft = LicenseCheckSettings.DEFAULT.failOnCopyleft,
                    warnOnCopyleft = LicenseCheckSettings.DEFAULT.warnOnCopyleft,
                    warnOnUnknown = LicenseCheckSettings.DEFAULT.warnOnUnknown,
                    ignoreLibraries = LicenseCheckSettings.DEFAULT.ignoreLibraries,
                    timeout = LicenseCheckSettings.DEFAULT_TIMEOUT_MS,
                    parallelism = LicenseCheckSettings.DEFAULT_PARALLELISM,
                    cacheDirectory = null,
                    cacheTtlHours = LicenseCheckSettings.DEFAULT_CACHE_TTL_HOURS,
                ))
                .build()

            val result = CoroutineRunner.run {
                dependanger.process()
            }

            val violations = result.licenseViolations

            if (jsonMode) {
                formatter.renderJson(violations, ListSerializer(LicenseViolation.serializer()))
            } else {
                if (violations.isEmpty()) {
                    formatter.success("No license violations found")
                } else {
                    formatter.renderTable(
                        headers = listOf("Library", "License", "Category", "Violation", "Message"),
                        rows = violations.map { violation ->
                            listOf(
                                "${violation.group}:${violation.artifact}",
                                violation.detectedLicense ?: "unknown",
                                violation.category.name,
                                violation.violationType.name,
                                violation.message,
                            )
                        }
                    )
                    formatter.info("${violations.size} license violation(s) found")
                }
            }

            output?.let { outputFile ->
                val outputPath = Path.of(outputFile)
                val jsonString = CliDefaults.CLI_JSON.encodeToString(ListSerializer(LicenseViolation.serializer()), violations)
                outputPath.writeText(jsonString)
                formatter.success("Report written to $outputPath")
            }

            val hasDenied = violations.any { it.violationType == LicenseViolationType.DENIED }
            val hasNotAllowed = violations.any { it.violationType == LicenseViolationType.NOT_ALLOWED }

            if (failOnDenied && hasDenied) {
                throw ProgramResult(1)
            }
            if (failOnUnknown && hasNotAllowed) {
                throw ProgramResult(1)
            }
        }
    }
}
