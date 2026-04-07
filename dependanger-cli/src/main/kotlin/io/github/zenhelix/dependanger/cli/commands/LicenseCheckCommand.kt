package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.api.licenseViolations
import io.github.zenhelix.dependanger.cli.options.PipelineOptions
import io.github.zenhelix.dependanger.cli.runner.PipelineRunner
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.feature.model.license.LicenseViolation
import io.github.zenhelix.dependanger.feature.model.license.LicenseViolationType
import io.github.zenhelix.dependanger.feature.model.settings.common.NETWORK_DEFAULT_PARALLELISM
import io.github.zenhelix.dependanger.feature.model.settings.common.NETWORK_DEFAULT_TIMEOUT_MS
import io.github.zenhelix.dependanger.feature.model.settings.license.LicenseCheckSettings
import io.github.zenhelix.dependanger.feature.model.settings.license.LicenseCheckSettingsKey
import kotlinx.serialization.builtins.ListSerializer

public class LicenseCheckCommand : CliktCommand(name = "license") {
    override fun help(context: Context): String = "Check library licenses for compliance"

    private val opts by PipelineOptions()
    public val output: String? by option("-o", "--output", help = "Output report file")
    public val allow: String? by option("--allow", help = "Allowed licenses (SPDX IDs)")
    public val deny: String? by option("--deny", help = "Denied licenses (SPDX IDs)")
    public val failOnUnknown: Boolean by option("--fail-on-unknown", help = "Fail if license unknown").flag()
    public val failOnDenied: Boolean by option("--fail-on-denied", help = "Fail on denied license").flag(default = true)
    public val includeTransitives: Boolean by option("--include-transitives", help = "Check transitives").flag()

    override fun run(): Unit = PipelineRunner(this, opts).run(
        configure = {
            preset(ProcessingPreset.STRICT)
            withContextProperty(
                LicenseCheckSettingsKey, LicenseCheckSettings(
                    enabled = true,
                    allowedLicenses = allow?.let { parseCommaSeparated(it) } ?: LicenseCheckSettings.DEFAULT.allowedLicenses,
                    deniedLicenses = deny?.let { parseCommaSeparated(it) } ?: LicenseCheckSettings.DEFAULT.deniedLicenses,
                    failOnDenied = failOnDenied,
                    failOnUnknown = failOnUnknown,
                    includeTransitives = includeTransitives,
                    dualLicensePolicy = LicenseCheckSettings.DEFAULT.dualLicensePolicy,
                    failOnCopyleft = LicenseCheckSettings.DEFAULT.failOnCopyleft,
                    warnOnCopyleft = LicenseCheckSettings.DEFAULT.warnOnCopyleft,
                    warnOnUnknown = LicenseCheckSettings.DEFAULT.warnOnUnknown,
                    ignoreLibraries = LicenseCheckSettings.DEFAULT.ignoreLibraries,
                    timeout = NETWORK_DEFAULT_TIMEOUT_MS,
                    parallelism = NETWORK_DEFAULT_PARALLELISM,
                    cacheDirectory = null,
                    cacheTtlHours = LicenseCheckSettings.DEFAULT.cacheTtlHours,
                )
            )
        },
        handle = { result ->
            val violations = result.licenseViolations

            val jsonMode = opts.format == CliDefaults.OUTPUT_FORMAT_JSON

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

            writeOutputIfRequested(output, violations, ListSerializer(LicenseViolation.serializer()))

            val hasDenied = violations.any { it.violationType == LicenseViolationType.DENIED }
            val hasNotAllowed = violations.any { it.violationType == LicenseViolationType.NOT_ALLOWED }

            if (failOnDenied && hasDenied) {
                throw ProgramResult(1)
            }
            if (failOnUnknown && hasNotAllowed) {
                throw ProgramResult(1)
            }
        }
    )
}
