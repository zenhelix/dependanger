package io.github.zenhelix.dependanger.feature.model.settings.updates

import io.github.zenhelix.dependanger.core.dsl.DependangerDslMarker
import io.github.zenhelix.dependanger.core.dsl.SettingsDsl
import io.github.zenhelix.dependanger.core.model.Repository
import io.github.zenhelix.dependanger.core.pipeline.ProcessingContextKey
import io.github.zenhelix.dependanger.effective.spi.AbstractFeatureSettingsProvider
import io.github.zenhelix.dependanger.feature.model.settings.common.NETWORK_DEFAULT_CACHE_TTL_HOURS
import io.github.zenhelix.dependanger.feature.model.settings.common.NETWORK_DEFAULT_PARALLELISM
import io.github.zenhelix.dependanger.feature.model.settings.common.NETWORK_DEFAULT_TIMEOUT_MS
import io.github.zenhelix.dependanger.feature.model.settings.common.NetworkCheckSettings
import io.github.zenhelix.dependanger.feature.model.settings.common.NetworkCheckSettingsDsl
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

public val UpdateCheckSettingsKey: ProcessingContextKey<UpdateCheckSettings> =
    ProcessingContextKey("updateCheck")

@Serializable
public data class UpdateCheckSettings(
    override val enabled: Boolean,
    val excludePatterns: List<String>,
    val includePrerelease: Boolean,
    val repositories: List<Repository>,
    override val timeout: Long,
    override val parallelism: Int,
    override val cacheDirectory: String?,
    override val cacheTtlHours: Long,
) : NetworkCheckSettings() {
    public companion object {
        public const val DEFAULT_TIMEOUT_MS: Long = NETWORK_DEFAULT_TIMEOUT_MS
        public const val DEFAULT_PARALLELISM: Int = NETWORK_DEFAULT_PARALLELISM
        public const val DEFAULT_CACHE_TTL_HOURS: Long = NETWORK_DEFAULT_CACHE_TTL_HOURS

        public val DEFAULT: UpdateCheckSettings = UpdateCheckSettings(
            enabled = false,
            excludePatterns = emptyList(),
            includePrerelease = false,
            repositories = emptyList(),
            timeout = DEFAULT_TIMEOUT_MS,
            parallelism = DEFAULT_PARALLELISM,
            cacheDirectory = null,
            cacheTtlHours = DEFAULT_CACHE_TTL_HOURS,
        )
    }
}

public class UpdateCheckSettingsProvider : AbstractFeatureSettingsProvider<UpdateCheckSettings>(
    settingsKey = "updateCheck",
    contextKey = UpdateCheckSettingsKey,
    serializer = UpdateCheckSettings.serializer(),
)

@DependangerDslMarker
public class UpdateCheckSettingsDsl : NetworkCheckSettingsDsl() {
    public var excludePatterns: List<String> = UpdateCheckSettings.DEFAULT.excludePatterns
    public var includePrerelease: Boolean = UpdateCheckSettings.DEFAULT.includePrerelease
    public var repositories: List<Repository> = UpdateCheckSettings.DEFAULT.repositories

    public fun toSettings(): UpdateCheckSettings = UpdateCheckSettings(
        enabled = enabled,
        excludePatterns = excludePatterns,
        includePrerelease = includePrerelease,
        repositories = repositories,
        timeout = timeout,
        parallelism = parallelism,
        cacheDirectory = cacheDirectory,
        cacheTtlHours = cacheTtlHours,
    )
}

public fun SettingsDsl.updateCheck(block: UpdateCheckSettingsDsl.() -> Unit) {
    val settings = UpdateCheckSettingsDsl().apply(block).toSettings()
    putCustomSetting("updateCheck", Json.encodeToJsonElement(UpdateCheckSettings.serializer(), settings))
}
