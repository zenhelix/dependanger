package io.github.zenhelix.dependanger.features.report.renderer

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import io.github.zenhelix.dependanger.features.report.model.ReportData

internal class YamlReportRenderer : ReportRenderer {

    private val yaml: Yaml = Yaml(
        configuration = YamlConfiguration(
            encodeDefaults = false,
        ),
    )

    override fun render(data: ReportData): String =
        yaml.encodeToString(ReportData.serializer(), data)
}
