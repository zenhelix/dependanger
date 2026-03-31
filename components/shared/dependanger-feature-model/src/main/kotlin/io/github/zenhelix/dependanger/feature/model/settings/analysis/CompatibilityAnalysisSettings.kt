package io.github.zenhelix.dependanger.feature.model.settings.analysis

import io.github.zenhelix.dependanger.core.dsl.DependangerDslMarker
import io.github.zenhelix.dependanger.core.dsl.SettingsDsl
import io.github.zenhelix.dependanger.core.model.Severity
import io.github.zenhelix.dependanger.core.pipeline.ProcessingContextKey
import io.github.zenhelix.dependanger.effective.spi.AbstractFeatureSettingsProvider
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

public val CompatibilityAnalysisSettingsKey: ProcessingContextKey<CompatibilityAnalysisSettings> =
    ProcessingContextKey("compatibilityAnalysis")

@Serializable
public data class CompatibilityAnalysisSettings(
    val enabled: Boolean,
    val targetJdk: Int?,
    val targetKotlin: String?,
    val minSeverity: Severity,
    val failOnErrors: Boolean,
) {
    public companion object {
        public val DEFAULT: CompatibilityAnalysisSettings = CompatibilityAnalysisSettings(
            enabled = false,
            targetJdk = null,
            targetKotlin = null,
            minSeverity = Severity.WARNING,
            failOnErrors = true,
        )
    }
}

public class CompatibilityAnalysisSettingsProvider : AbstractFeatureSettingsProvider<CompatibilityAnalysisSettings>(
    settingsKey = "compatibilityAnalysis",
    contextKey = CompatibilityAnalysisSettingsKey,
    serializer = CompatibilityAnalysisSettings.serializer(),
)

@DependangerDslMarker
public class CompatibilityAnalysisSettingsDsl {
    public var enabled: Boolean = CompatibilityAnalysisSettings.DEFAULT.enabled
    public var targetJdk: Int? = CompatibilityAnalysisSettings.DEFAULT.targetJdk
    public var targetKotlin: String? = CompatibilityAnalysisSettings.DEFAULT.targetKotlin
    public var minSeverity: Severity = CompatibilityAnalysisSettings.DEFAULT.minSeverity
    public var failOnErrors: Boolean = CompatibilityAnalysisSettings.DEFAULT.failOnErrors

    public fun toSettings(): CompatibilityAnalysisSettings = CompatibilityAnalysisSettings(
        enabled = enabled,
        targetJdk = targetJdk,
        targetKotlin = targetKotlin,
        minSeverity = minSeverity,
        failOnErrors = failOnErrors,
    )
}

public fun SettingsDsl.compatibilityAnalysis(block: CompatibilityAnalysisSettingsDsl.() -> Unit) {
    val settings = CompatibilityAnalysisSettingsDsl().apply(block).toSettings()
    putCustomSetting("compatibilityAnalysis", Json.encodeToJsonElement(CompatibilityAnalysisSettings.serializer(), settings))
}
