package io.github.zenhelix.dependanger.features.report

import io.github.zenhelix.dependanger.effective.spi.ReportFormat
import kotlinx.serialization.Serializable

@Serializable
public data class DependangerReport(
    val format: ReportFormat,
    val content: String,
    val outputPath: String?,
)
