package io.github.zenhelix.dependanger.features.updates

import io.github.zenhelix.dependanger.core.dsl.DependangerDslMarker
import io.github.zenhelix.dependanger.core.dsl.SettingsDsl
import io.github.zenhelix.dependanger.core.model.Repository
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContextKey
import io.github.zenhelix.dependanger.effective.spi.FeatureSettingsProvider
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

public val UpdateCheckSettingsKey: ProcessingContextKey<UpdateCheckSettings> =
    ProcessingContextKey("updateCheck")

@Serializable
public data class UpdateCheckSettings(
    val enabled: Boolean,
    val excludePatterns: List<String>,
    val includePrerelease: Boolean,
    val repositories: List<Repository>,
    val timeout: Long,
    val parallelism: Int,
    val cacheDirectory: String?,
    val cacheTtlHours: Long,
) {
    public companion object {
        public const val DEFAULT_TIMEOUT_MS: Long = 30_000
        public const val DEFAULT_PARALLELISM: Int = 10
        public const val DEFAULT_CACHE_TTL_HOURS: Long = 1

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

public class UpdateCheckSettingsProvider : FeatureSettingsProvider {
    override val settingsKey: String = "updateCheck"

    override fun deserialize(json: JsonElement): Pair<ProcessingContextKey<*>, Any> {
        val settings = Json.decodeFromJsonElement(UpdateCheckSettings.serializer(), json)
        return UpdateCheckSettingsKey to settings
    }
}

@DependangerDslMarker
public class UpdateCheckSettingsDsl {
    public var enabled: Boolean = UpdateCheckSettings.DEFAULT.enabled
    public var excludePatterns: List<String> = UpdateCheckSettings.DEFAULT.excludePatterns
    public var includePrerelease: Boolean = UpdateCheckSettings.DEFAULT.includePrerelease
    public var repositories: List<Repository> = UpdateCheckSettings.DEFAULT.repositories
    public var timeout: Long = UpdateCheckSettings.DEFAULT.timeout
    public var parallelism: Int = UpdateCheckSettings.DEFAULT.parallelism
    public var cacheDirectory: String? = UpdateCheckSettings.DEFAULT.cacheDirectory
    public var cacheTtlHours: Long = UpdateCheckSettings.DEFAULT.cacheTtlHours

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
