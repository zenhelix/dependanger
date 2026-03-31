package io.github.zenhelix.dependanger.feature.model.settings.transitive

import io.github.zenhelix.dependanger.core.dsl.DependangerDslMarker
import io.github.zenhelix.dependanger.core.dsl.SettingsDsl
import io.github.zenhelix.dependanger.core.model.Repository
import io.github.zenhelix.dependanger.core.pipeline.ProcessingContextKey
import io.github.zenhelix.dependanger.effective.spi.AbstractFeatureSettingsProvider
import io.github.zenhelix.dependanger.feature.model.transitive.ConflictResolutionStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

public val TransitiveResolutionSettingsKey: ProcessingContextKey<TransitiveResolutionSettings> =
    ProcessingContextKey("transitiveResolution")

@Serializable
public data class TransitiveResolutionSettings(
    val enabled: Boolean,
    val repositories: List<Repository>,
    val maxDepth: Int?,
    val maxTransitives: Int?,
    val conflictResolution: ConflictResolutionStrategy,
    val includeOptional: Boolean,
    val scopes: List<String>,
    val cacheDirectory: String?,
    val cacheTtlHours: Long,
) {
    public companion object {
        public const val DEFAULT_CACHE_TTL_HOURS: Long = 24

        public val DEFAULT: TransitiveResolutionSettings = TransitiveResolutionSettings(
            enabled = false,
            repositories = emptyList(),
            maxDepth = null,
            maxTransitives = null,
            conflictResolution = ConflictResolutionStrategy.HIGHEST,
            includeOptional = false,
            scopes = listOf("compile", "runtime"),
            cacheDirectory = null,
            cacheTtlHours = DEFAULT_CACHE_TTL_HOURS,
        )
    }
}

public class TransitiveResolutionSettingsProvider : AbstractFeatureSettingsProvider<TransitiveResolutionSettings>(
    settingsKey = "transitiveResolution",
    contextKey = TransitiveResolutionSettingsKey,
    serializer = TransitiveResolutionSettings.serializer(),
)

@DependangerDslMarker
public class TransitiveResolutionSettingsDsl {
    public var enabled: Boolean = TransitiveResolutionSettings.DEFAULT.enabled
    public var repositories: List<Repository> = TransitiveResolutionSettings.DEFAULT.repositories
    public var maxDepth: Int? = TransitiveResolutionSettings.DEFAULT.maxDepth
    public var maxTransitives: Int? = TransitiveResolutionSettings.DEFAULT.maxTransitives
    public var conflictResolution: ConflictResolutionStrategy = TransitiveResolutionSettings.DEFAULT.conflictResolution
    public var includeOptional: Boolean = TransitiveResolutionSettings.DEFAULT.includeOptional
    public var scopes: List<String> = TransitiveResolutionSettings.DEFAULT.scopes
    public var cacheDirectory: String? = TransitiveResolutionSettings.DEFAULT.cacheDirectory
    public var cacheTtlHours: Long = TransitiveResolutionSettings.DEFAULT.cacheTtlHours

    public fun toSettings(): TransitiveResolutionSettings = TransitiveResolutionSettings(
        enabled = enabled,
        repositories = repositories,
        maxDepth = maxDepth,
        maxTransitives = maxTransitives,
        conflictResolution = conflictResolution,
        includeOptional = includeOptional,
        scopes = scopes,
        cacheDirectory = cacheDirectory,
        cacheTtlHours = cacheTtlHours,
    )
}

public fun SettingsDsl.transitiveResolution(block: TransitiveResolutionSettingsDsl.() -> Unit) {
    val settings = TransitiveResolutionSettingsDsl().apply(block).toSettings()
    putCustomSetting("transitiveResolution", Json.encodeToJsonElement(TransitiveResolutionSettings.serializer(), settings))
}
