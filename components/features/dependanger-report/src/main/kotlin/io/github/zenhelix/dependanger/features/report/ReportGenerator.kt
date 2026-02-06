package io.github.zenhelix.dependanger.features.report

import io.github.zenhelix.dependanger.core.model.ReportFormat
import io.github.zenhelix.dependanger.core.model.ReportSection
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata

public class ReportGenerator {
    public fun generate(
        effective: EffectiveMetadata,
        format: ReportFormat = ReportFormat.MARKDOWN,
        sections: List<ReportSection> = ReportSection.entries,
        outputDir: String = "build/reports/dependanger",
    ): DependangerReport = TODO()
}
