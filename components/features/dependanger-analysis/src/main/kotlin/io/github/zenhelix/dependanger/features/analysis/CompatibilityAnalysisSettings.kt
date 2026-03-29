package io.github.zenhelix.dependanger.features.analysis

import io.github.zenhelix.dependanger.core.dsl.DependangerDslMarker
import io.github.zenhelix.dependanger.core.dsl.SettingsDsl
import io.github.zenhelix.dependanger.core.model.Severity
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContextKey
import io.github.zenhelix.dependanger.effective.spi.FeatureSettingsProvider
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

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

public class CompatibilityAnalysisSettingsProvider : FeatureSettingsProvider {
    override val settingsKey: String = "compatibilityAnalysis"

    override fun deserialize(json: JsonElement): Pair<ProcessingContextKey<*>, Any> {
        val settings = Json.decodeFromJsonElement(CompatibilityAnalysisSettings.serializer(), json)
        return CompatibilityAnalysisSettingsKey to settings
    }
}

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
