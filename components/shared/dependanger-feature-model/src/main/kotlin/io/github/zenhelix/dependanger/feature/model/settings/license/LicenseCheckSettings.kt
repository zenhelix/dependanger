package io.github.zenhelix.dependanger.feature.model.settings.license

import io.github.zenhelix.dependanger.core.dsl.DependangerDslMarker
import io.github.zenhelix.dependanger.core.dsl.SettingsDsl
import io.github.zenhelix.dependanger.core.pipeline.ProcessingContextKey
import io.github.zenhelix.dependanger.effective.spi.AbstractFeatureSettingsProvider
import io.github.zenhelix.dependanger.feature.model.settings.common.NETWORK_DEFAULT_PARALLELISM
import io.github.zenhelix.dependanger.feature.model.settings.common.NETWORK_DEFAULT_TIMEOUT_MS
import io.github.zenhelix.dependanger.feature.model.settings.common.NetworkCheckSettings
import io.github.zenhelix.dependanger.feature.model.settings.common.NetworkCheckSettingsDsl
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

public val LicenseCheckSettingsKey: ProcessingContextKey<LicenseCheckSettings> =
    ProcessingContextKey("licenseCheck")

@Serializable
public enum class DualLicensePolicy {
    OR, AND
}

@Serializable
public data class LicenseCheckSettings(
    override val enabled: Boolean,
    val allowedLicenses: List<String>,
    val deniedLicenses: List<String>,
    val dualLicensePolicy: DualLicensePolicy,
    val failOnDenied: Boolean,
    val failOnUnknown: Boolean,
    val failOnCopyleft: Boolean,
    val warnOnCopyleft: Boolean,
    val warnOnUnknown: Boolean,
    val ignoreLibraries: List<String>,
    val includeTransitives: Boolean,
    override val timeout: Long,
    override val parallelism: Int,
    override val cacheDirectory: String?,
    override val cacheTtlHours: Long,
) : NetworkCheckSettings() {
    public companion object {
        public const val DEFAULT_TIMEOUT_MS: Long = NETWORK_DEFAULT_TIMEOUT_MS
        public const val DEFAULT_PARALLELISM: Int = NETWORK_DEFAULT_PARALLELISM
        public const val DEFAULT_CACHE_TTL_HOURS: Long = 168

        public val DEFAULT: LicenseCheckSettings = LicenseCheckSettings(
            enabled = false,
            allowedLicenses = emptyList(),
            deniedLicenses = emptyList(),
            dualLicensePolicy = DualLicensePolicy.OR,
            failOnDenied = false,
            failOnUnknown = false,
            failOnCopyleft = false,
            warnOnCopyleft = true,
            warnOnUnknown = true,
            ignoreLibraries = emptyList(),
            includeTransitives = false,
            timeout = DEFAULT_TIMEOUT_MS,
            parallelism = DEFAULT_PARALLELISM,
            cacheDirectory = null,
            cacheTtlHours = DEFAULT_CACHE_TTL_HOURS,
        )
    }
}

public class LicenseCheckSettingsProvider : AbstractFeatureSettingsProvider<LicenseCheckSettings>(
    settingsKey = "licenseCheck",
    contextKey = LicenseCheckSettingsKey,
    serializer = LicenseCheckSettings.serializer(),
)

@DependangerDslMarker
public class LicenseCheckSettingsDsl : NetworkCheckSettingsDsl() {
    init {
        cacheTtlHours = LicenseCheckSettings.DEFAULT_CACHE_TTL_HOURS
    }

    public var allowedLicenses: List<String> = LicenseCheckSettings.DEFAULT.allowedLicenses
    public var deniedLicenses: List<String> = LicenseCheckSettings.DEFAULT.deniedLicenses
    public var dualLicensePolicy: DualLicensePolicy = LicenseCheckSettings.DEFAULT.dualLicensePolicy
    public var failOnDenied: Boolean = LicenseCheckSettings.DEFAULT.failOnDenied
    public var failOnUnknown: Boolean = LicenseCheckSettings.DEFAULT.failOnUnknown
    public var failOnCopyleft: Boolean = LicenseCheckSettings.DEFAULT.failOnCopyleft
    public var warnOnCopyleft: Boolean = LicenseCheckSettings.DEFAULT.warnOnCopyleft
    public var warnOnUnknown: Boolean = LicenseCheckSettings.DEFAULT.warnOnUnknown
    public var ignoreLibraries: List<String> = LicenseCheckSettings.DEFAULT.ignoreLibraries
    public var includeTransitives: Boolean = LicenseCheckSettings.DEFAULT.includeTransitives

    public fun toSettings(): LicenseCheckSettings = LicenseCheckSettings(
        enabled = enabled,
        allowedLicenses = allowedLicenses,
        deniedLicenses = deniedLicenses,
        dualLicensePolicy = dualLicensePolicy,
        failOnDenied = failOnDenied,
        failOnUnknown = failOnUnknown,
        failOnCopyleft = failOnCopyleft,
        warnOnCopyleft = warnOnCopyleft,
        warnOnUnknown = warnOnUnknown,
        ignoreLibraries = ignoreLibraries,
        includeTransitives = includeTransitives,
        timeout = timeout,
        parallelism = parallelism,
        cacheDirectory = cacheDirectory,
        cacheTtlHours = cacheTtlHours,
    )
}

public fun SettingsDsl.licenseCheck(block: LicenseCheckSettingsDsl.() -> Unit) {
    val settings = LicenseCheckSettingsDsl().apply(block).toSettings()
    putCustomSetting("licenseCheck", Json.encodeToJsonElement(LicenseCheckSettings.serializer(), settings))
}
