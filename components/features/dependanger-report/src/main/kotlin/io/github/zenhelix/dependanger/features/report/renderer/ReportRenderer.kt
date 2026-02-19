package io.github.zenhelix.dependanger.features.report.renderer

import io.github.zenhelix.dependanger.features.report.model.ReportData

internal sealed interface ReportRenderer {

    fun render(data: ReportData): String
}
