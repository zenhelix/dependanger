package io.github.zenhelix.dependanger.feature.model.settings.security

import io.github.zenhelix.dependanger.core.dsl.DependangerDslMarker
import io.github.zenhelix.dependanger.core.dsl.SettingsDsl
import io.github.zenhelix.dependanger.core.model.Severity
import io.github.zenhelix.dependanger.core.pipeline.ProcessingContextKey
import io.github.zenhelix.dependanger.effective.spi.AbstractFeatureSettingsProvider
import io.github.zenhelix.dependanger.feature.model.settings.common.NETWORK_DEFAULT_PARALLELISM
import io.github.zenhelix.dependanger.feature.model.settings.common.NETWORK_DEFAULT_TIMEOUT_MS
import io.github.zenhelix.dependanger.feature.model.settings.common.NetworkCheckSettings
import io.github.zenhelix.dependanger.feature.model.settings.common.NetworkCheckSettingsDsl
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

public val SecurityCheckSettingsKey: ProcessingContextKey<SecurityCheckSettings> =
    ProcessingContextKey("securityCheck")

@Serializable
public data class SecurityCheckSettings(
    override val enabled: Boolean,
    val failOnVulnerability: Severity,
    val minSeverity: String,
    val ignoreVulnerabilities: List<String>,
    override val timeout: Long,
    override val parallelism: Int,
    override val cacheDirectory: String?,
    override val cacheTtlHours: Long,
) : NetworkCheckSettings() {
    public companion object {
        public const val DEFAULT_CACHE_TTL_HOURS: Long = 24

        public val DEFAULT: SecurityCheckSettings = SecurityCheckSettings(
            enabled = false,
            failOnVulnerability = Severity.ERROR,
            minSeverity = "HIGH",
            ignoreVulnerabilities = emptyList(),
            timeout = NETWORK_DEFAULT_TIMEOUT_MS,
            parallelism = NETWORK_DEFAULT_PARALLELISM,
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
public class SecurityCheckSettingsDsl : NetworkCheckSettingsDsl() {
    init {
        cacheTtlHours = SecurityCheckSettings.DEFAULT_CACHE_TTL_HOURS
    }

    public var failOnVulnerability: Severity = SecurityCheckSettings.DEFAULT.failOnVulnerability
    public var minSeverity: String = SecurityCheckSettings.DEFAULT.minSeverity
    public var ignoreVulnerabilities: List<String> = SecurityCheckSettings.DEFAULT.ignoreVulnerabilities

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
