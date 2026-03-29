package io.github.zenhelix.dependanger.features.resolver

import io.github.zenhelix.dependanger.core.dsl.DependangerDslMarker
import io.github.zenhelix.dependanger.core.dsl.SettingsDsl
import io.github.zenhelix.dependanger.core.pipeline.ProcessingContextKey
import io.github.zenhelix.dependanger.effective.spi.AbstractFeatureSettingsProvider
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

public val BomCacheSettingsKey: ProcessingContextKey<BomCacheSettings> =
    ProcessingContextKey("bomCache")

@Serializable
public data class BomCacheSettings(
    val enabled: Boolean,
    val directory: String?,
    val ttlHours: Long,
    val ttlSnapshotHours: Long,
) {
    public companion object {
        public const val DEFAULT_CACHE_TTL_HOURS: Long = 24
        public const val DEFAULT_SNAPSHOT_CACHE_TTL_HOURS: Long = 1

        public val DEFAULT: BomCacheSettings = BomCacheSettings(
            enabled = true,
            directory = null,
            ttlHours = DEFAULT_CACHE_TTL_HOURS,
            ttlSnapshotHours = DEFAULT_SNAPSHOT_CACHE_TTL_HOURS,
        )
    }
}

public class BomCacheSettingsProvider : AbstractFeatureSettingsProvider<BomCacheSettings>(
    settingsKey = "bomCache",
    contextKey = BomCacheSettingsKey,
    serializer = BomCacheSettings.serializer(),
)

@DependangerDslMarker
public class BomCacheSettingsDsl {
    public var enabled: Boolean = BomCacheSettings.DEFAULT.enabled
    public var directory: String? = BomCacheSettings.DEFAULT.directory
    public var ttlHours: Long = BomCacheSettings.DEFAULT.ttlHours
    public var ttlSnapshotHours: Long = BomCacheSettings.DEFAULT.ttlSnapshotHours

    public fun toSettings(): BomCacheSettings = BomCacheSettings(
        enabled = enabled,
        directory = directory,
        ttlHours = ttlHours,
        ttlSnapshotHours = ttlSnapshotHours,
    )
}

public fun SettingsDsl.bomCache(block: BomCacheSettingsDsl.() -> Unit) {
    val settings = BomCacheSettingsDsl().apply(block).toSettings()
    putCustomSetting("bomCache", Json.encodeToJsonElement(BomCacheSettings.serializer(), settings))
}
