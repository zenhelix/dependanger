package io.github.zenhelix.dependanger.features.license

import io.github.zenhelix.dependanger.core.dsl.DependangerDslMarker
import io.github.zenhelix.dependanger.core.dsl.SettingsDsl
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContextKey
import io.github.zenhelix.dependanger.effective.spi.FeatureSettingsProvider
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

public val LicenseCheckSettingsKey: ProcessingContextKey<LicenseCheckSettings> =
    ProcessingContextKey("licenseCheck")

@Serializable
public enum class DualLicensePolicy {
    OR, AND
}

@Serializable
public data class LicenseCheckSettings(
    val enabled: Boolean,
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
    val timeout: Long,
    val parallelism: Int,
    val cacheDirectory: String?,
    val cacheTtlHours: Long,
) {
    public companion object {
        public const val DEFAULT_TIMEOUT_MS: Long = 30_000
        public const val DEFAULT_PARALLELISM: Int = 10
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

public class LicenseCheckSettingsProvider : FeatureSettingsProvider {
    override val settingsKey: String = "licenseCheck"

    override fun deserialize(json: JsonElement): Pair<ProcessingContextKey<*>, Any> {
        val settings = Json.decodeFromJsonElement(LicenseCheckSettings.serializer(), json)
        return LicenseCheckSettingsKey to settings
    }
}

@DependangerDslMarker
public class LicenseCheckSettingsDsl {
    public var enabled: Boolean = LicenseCheckSettings.DEFAULT.enabled
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
    public var timeout: Long = LicenseCheckSettings.DEFAULT.timeout
    public var parallelism: Int = LicenseCheckSettings.DEFAULT.parallelism
    public var cacheDirectory: String? = LicenseCheckSettings.DEFAULT.cacheDirectory
    public var cacheTtlHours: Long = LicenseCheckSettings.DEFAULT.cacheTtlHours

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
