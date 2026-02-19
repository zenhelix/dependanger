package io.github.zenhelix.dependanger.features.report.renderer

import io.github.zenhelix.dependanger.features.report.model.ReportData
import kotlinx.serialization.json.Json

internal class JsonReportRenderer : ReportRenderer {

    private val json: Json = Json {
        prettyPrint = true
        encodeDefaults = false
        explicitNulls = false
    }

    override fun render(data: ReportData): String =
        json.encodeToString(ReportData.serializer(), data)
}
