package io.github.zenhelix.dependanger.features.report

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.zenhelix.dependanger.effective.spi.ReportFormat
import io.github.zenhelix.dependanger.effective.spi.ReportSettings
import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.spi.GeneratedReport
import io.github.zenhelix.dependanger.effective.spi.ReportProvider
import io.github.zenhelix.dependanger.features.report.mapper.ReportDataMapper
import io.github.zenhelix.dependanger.features.report.renderer.HtmlReportRenderer
import io.github.zenhelix.dependanger.features.report.renderer.JsonReportRenderer
import io.github.zenhelix.dependanger.features.report.renderer.MarkdownReportRenderer
import io.github.zenhelix.dependanger.features.report.renderer.ReportRenderer
import io.github.zenhelix.dependanger.features.report.renderer.YamlReportRenderer
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

public class ReportGenerator : ReportProvider {

    override val providerId: String = "report"

    private val markdownRenderer: MarkdownReportRenderer = MarkdownReportRenderer()

    private val renderers: Map<ReportFormat, ReportRenderer> = mapOf(
        ReportFormat.JSON to JsonReportRenderer(),
        ReportFormat.YAML to YamlReportRenderer(),
        ReportFormat.MARKDOWN to markdownRenderer,
        ReportFormat.HTML to HtmlReportRenderer(markdownRenderer = markdownRenderer),
    )

    override fun generate(
        effective: EffectiveMetadata,
        settings: ReportSettings,
        originalMetadata: DependangerMetadata?,
    ): GeneratedReport {
        logger.info { "Generating report in ${settings.format} format" }

        val reportData = ReportDataMapper.buildReportData(
            effective = effective,
            sections = settings.sections,
            originalMetadata = originalMetadata,
            summaryOnly = false,
        )

        val renderer = renderers.getValue(settings.format)
        val content = renderer.render(reportData)

        logger.info { "Report generated: ${content.length} characters" }

        return GeneratedReport(
            format = settings.format,
            content = content,
            outputPath = null,
        )
    }

    override fun generateToFile(
        effective: EffectiveMetadata,
        settings: ReportSettings,
        originalMetadata: DependangerMetadata?,
    ): GeneratedReport {
        val report = generate(
            effective = effective,
            settings = settings,
            originalMetadata = originalMetadata,
        )

        val extension = formatExtension(settings.format)
        val outputDir = Path.of(settings.outputDir)
        Files.createDirectories(outputDir)

        val targetPath = outputDir.resolve("dependanger-report.$extension")
        val dir = outputDir.toFile()
        val tempFile = File.createTempFile("report-", ".$extension", dir)
        try {
            tempFile.writeText(report.content, Charsets.UTF_8)
            val targetFile = targetPath.toFile()
            val renamed = tempFile.renameTo(targetFile)
            if (!renamed) {
                try {
                    targetFile.writeText(report.content, Charsets.UTF_8)
                } finally {
                    tempFile.delete()
                }
            }
            logger.info { "Report written to $targetPath" }
        } catch (e: Exception) {
            tempFile.delete()
            throw e
        }

        return report.copy(outputPath = targetPath)
    }

    private fun formatExtension(format: ReportFormat): String = when (format) {
        ReportFormat.JSON     -> "json"
        ReportFormat.YAML     -> "yaml"
        ReportFormat.MARKDOWN -> "md"
        ReportFormat.HTML     -> "html"
    }
}
