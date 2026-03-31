package io.github.zenhelix.dependanger.feature.model.settings.security

import io.github.zenhelix.dependanger.core.dsl.DependangerDslMarker
import io.github.zenhelix.dependanger.core.dsl.SettingsDsl
import io.github.zenhelix.dependanger.core.model.Severity
import io.github.zenhelix.dependanger.core.pipeline.ProcessingContextKey
import io.github.zenhelix.dependanger.effective.spi.AbstractFeatureSettingsProvider
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

public val SecurityCheckSettingsKey: ProcessingContextKey<SecurityCheckSettings> =
    ProcessingContextKey("securityCheck")

@Serializable
public data class SecurityCheckSettings(
    val enabled: Boolean,
    val failOnVulnerability: Severity,
    val minSeverity: String,
    val ignoreVulnerabilities: List<String>,
    val timeout: Long,
    val parallelism: Int,
    val cacheDirectory: String?,
    val cacheTtlHours: Long,
) {
    public companion object {
        public const val DEFAULT_TIMEOUT_MS: Long = 30_000
        public const val DEFAULT_PARALLELISM: Int = 10
        public const val DEFAULT_CACHE_TTL_HOURS: Long = 24

        public val DEFAULT: SecurityCheckSettings = SecurityCheckSettings(
            enabled = false,
            failOnVulnerability = Severity.ERROR,
            minSeverity = "HIGH",
            ignoreVulnerabilities = emptyList(),
            timeout = DEFAULT_TIMEOUT_MS,
            parallelism = DEFAULT_PARALLELISM,
            cacheDirectory = null,
            cacheTtlHours = DEFAULT_CACHE_TTL_HOURS,
        )
    }
}

public class SecurityCheckSettingsProvider : AbstractFeatureSettingsProvider<SecurityCheckSettings>(
    settingsKey = "securityCheck",
    contextKey = SecurityCheckSettingsKey,
    serializer = SecurityCheckSettings.serializer(),
)

@DependangerDslMarker
public class SecurityCheckSettingsDsl {
    public var enabled: Boolean = SecurityCheckSettings.DEFAULT.enabled
    public var failOnVulnerability: Severity = SecurityCheckSettings.DEFAULT.failOnVulnerability
    public var minSeverity: String = SecurityCheckSettings.DEFAULT.minSeverity
    public var ignoreVulnerabilities: List<String> = SecurityCheckSettings.DEFAULT.ignoreVulnerabilities
    public var timeout: Long = SecurityCheckSettings.DEFAULT.timeout
    public var parallelism: Int = SecurityCheckSettings.DEFAULT.parallelism
    public var cacheDirectory: String? = SecurityCheckSettings.DEFAULT.cacheDirectory
    public var cacheTtlHours: Long = SecurityCheckSettings.DEFAULT.cacheTtlHours

    public fun toSettings(): SecurityCheckSettings = SecurityCheckSettings(
        enabled = enabled,
        failOnVulnerability = failOnVulnerability,
        minSeverity = minSeverity,
        ignoreVulnerabilities = ignoreVulnerabilities,
        timeout = timeout,
        parallelism = parallelism,
        cacheDirectory = cacheDirectory,
        cacheTtlHours = cacheTtlHours,
    )
}

public fun SettingsDsl.securityCheck(block: SecurityCheckSettingsDsl.() -> Unit) {
    val settings = SecurityCheckSettingsDsl().apply(block).toSettings()
    putCustomSetting("securityCheck", Json.encodeToJsonElement(SecurityCheckSettings.serializer(), settings))
}
