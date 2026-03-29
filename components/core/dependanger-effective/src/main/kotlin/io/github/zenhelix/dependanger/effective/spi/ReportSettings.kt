package io.github.zenhelix.dependanger.effective.spi

import kotlinx.serialization.Serializable

@Serializable
public data class ReportSettings(
    val format: ReportFormat,
    val outputDir: String,
    val sections: List<ReportSection>,
) {
    public companion object {
        public const val DEFAULT_REPORT_OUTPUT_DIR: String = "build/reports/dependanger"

        public val DEFAULT: ReportSettings = ReportSettings(
            format = ReportFormat.MARKDOWN,
            outputDir = DEFAULT_REPORT_OUTPUT_DIR,
            sections = ReportSection.entries,
        )
    }
}
