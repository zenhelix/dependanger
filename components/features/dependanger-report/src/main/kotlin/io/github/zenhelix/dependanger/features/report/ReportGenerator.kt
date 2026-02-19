package io.github.zenhelix.dependanger.features.report

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.zenhelix.dependanger.core.model.ReportFormat
import io.github.zenhelix.dependanger.core.model.ReportSettings
import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.features.report.mapper.ReportDataMapper
import io.github.zenhelix.dependanger.features.report.renderer.HtmlReportRenderer
import io.github.zenhelix.dependanger.features.report.renderer.JsonReportRenderer
import io.github.zenhelix.dependanger.features.report.renderer.MarkdownReportRenderer
import io.github.zenhelix.dependanger.features.report.renderer.ReportRenderer
import io.github.zenhelix.dependanger.features.report.renderer.YamlReportRenderer
import java.io.File

private val logger = KotlinLogging.logger {}

public class ReportGenerator {

    private val markdownRenderer: MarkdownReportRenderer = MarkdownReportRenderer()

    private val renderers: Map<ReportFormat, ReportRenderer> = mapOf(
        ReportFormat.JSON to JsonReportRenderer(),
        ReportFormat.YAML to YamlReportRenderer(),
        ReportFormat.MARKDOWN to markdownRenderer,
        ReportFormat.HTML to HtmlReportRenderer(markdownRenderer = markdownRenderer),
    )

    public fun generate(
        effective: EffectiveMetadata,
        settings: ReportSettings = ReportSettings.DEFAULT,
        originalMetadata: DependangerMetadata? = null,
    ): DependangerReport {
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

        return DependangerReport(
            format = settings.format,
            content = content,
            outputPath = null,
        )
    }

    public fun generateToFile(
        effective: EffectiveMetadata,
        settings: ReportSettings = ReportSettings.DEFAULT,
        originalMetadata: DependangerMetadata? = null,
    ): DependangerReport {
        val report = generate(
            effective = effective,
            settings = settings,
            originalMetadata = originalMetadata,
        )

        val extension = when (settings.format) {
            ReportFormat.JSON     -> "json"
            ReportFormat.YAML     -> "yaml"
            ReportFormat.MARKDOWN -> "md"
            ReportFormat.HTML     -> "html"
        }

        val outputPath = "${settings.outputDir}/dependanger-report.$extension"
        val dir = File(settings.outputDir)
        if (!dir.exists()) {
            dir.mkdirs()
        }

        val tempFile = File.createTempFile("report-", ".$extension", dir)
        try {
            tempFile.writeText(report.content, Charsets.UTF_8)
            val targetFile = File(outputPath)
            val renamed = tempFile.renameTo(targetFile)
            if (!renamed) {
                try {
                    targetFile.writeText(report.content, Charsets.UTF_8)
                } finally {
                    tempFile.delete()
                }
            }
            logger.info { "Report written to $outputPath" }
        } catch (e: Exception) {
            tempFile.delete()
            throw e
        }

        return report.copy(outputPath = outputPath)
    }
}
