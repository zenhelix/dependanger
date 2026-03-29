package io.github.zenhelix.dependanger.effective.spi

import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContextKey
import kotlinx.serialization.json.JsonElement

public interface FeatureSettingsProvider {
    public val settingsKey: String
    public fun deserialize(json: JsonElement): Pair<ProcessingContextKey<*>, Any>
}
