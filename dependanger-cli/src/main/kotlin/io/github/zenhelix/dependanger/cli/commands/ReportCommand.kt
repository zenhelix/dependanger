package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.api.Dependanger
import io.github.zenhelix.dependanger.api.writeReportTo
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.effective.spi.ReportFormat
import io.github.zenhelix.dependanger.effective.spi.ReportSection
import io.github.zenhelix.dependanger.effective.spi.ReportSettings
import io.github.zenhelix.dependanger.features.transitive.TransitiveResolutionSettings
import io.github.zenhelix.dependanger.features.transitive.TransitiveResolutionSettingsKey
import java.nio.file.Path

public class ReportCommand : CliktCommand(name = "report") {
    override fun help(context: Context): String = "Generate comprehensive dependency report"

    public val input: String by option("-i", "--input", help = "Input metadata/effective file").default(CliDefaults.METADATA_FILE)
    public val outputDir: String by option("-o", "--output-dir", help = "Output directory").default(".")
    public val format: String by option("--format", help = "Format: json, yaml, markdown, html").default(CliDefaults.OUTPUT_FORMAT_MARKDOWN)
    public val sections: String? by option("--sections", help = "Sections (comma-separated)")
    public val includeTransitives: Boolean by option("--include-transitives", help = "Include transitive data").flag()

    override fun run() {
        val formatter = OutputFormatter(jsonMode = false, terminal = terminal)
        val metadataService = MetadataService()

        withErrorHandling(formatter) {
            val metadata = metadataService.read(Path.of(input))

            val reportFormat = try {
                ReportFormat.valueOf(format.uppercase())
            } catch (_: IllegalArgumentException) {
                throw CliException.InvalidArgument(
                    "Unknown report format '$format'. Available: ${ReportFormat.entries.joinToString { it.name }}"
                )
            }

            val reportSections = if (sections != null) {
                sections!!.split(",").map { sectionName ->
                    val trimmed = sectionName.trim().uppercase()
                    try {
                        ReportSection.valueOf(trimmed)
                    } catch (_: IllegalArgumentException) {
                        throw CliException.InvalidArgument(
                            "Unknown report section '$trimmed'. Available: ${ReportSection.entries.joinToString { it.name }}"
                        )
                    }
                }
            } else {
                ReportSection.entries
            }

            val reportSettings = ReportSettings(
                format = reportFormat,
                outputDir = outputDir,
                sections = reportSections,
            )

            val builder = Dependanger.fromMetadata(metadata)
                .preset(ProcessingPreset.STRICT)
            if (includeTransitives) {
                builder.withContextProperty(
                    TransitiveResolutionSettingsKey,
                    TransitiveResolutionSettings.DEFAULT.copy(enabled = true),
                )
            }
            val dependanger = builder.build()

            val result = CoroutineRunner.run {
                dependanger.process()
            }

            val generatedReport = result.writeReportTo(reportSettings)

            val outputPath = generatedReport.outputPath
            if (outputPath != null) {
                formatter.success("Report generated: $outputPath (${generatedReport.format.name})")
            } else {
                formatter.println(generatedReport.content)
                formatter.success("Report generated (${generatedReport.format.name})")
            }
        }
    }
}
