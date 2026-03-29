package io.github.zenhelix.dependanger.effective.spi

import io.github.zenhelix.dependanger.core.pipeline.ProcessingContextKey
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

public interface FeatureSettingsProvider {
    public val settingsKey: String
    public fun deserialize(json: JsonElement): Pair<ProcessingContextKey<*>, Any>
}

/** Base class eliminating boilerplate [deserialize] implementations. */
public abstract class AbstractFeatureSettingsProvider<T : Any>(
    override val settingsKey: String,
    private val contextKey: ProcessingContextKey<T>,
    private val serializer: KSerializer<T>,
) : FeatureSettingsProvider {
    override fun deserialize(json: JsonElement): Pair<ProcessingContextKey<*>, Any> {
        val settings = Json.decodeFromJsonElement(serializer, json)
        return contextKey to settings
    }
}
