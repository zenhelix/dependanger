package io.github.zenhelix.dependanger.effective.spi

import io.github.zenhelix.dependanger.effective.model.ExtensionKey

/**
 * SPI for discovering extension keys from feature modules.
 * Implementations are loaded via ServiceLoader for EffectiveJsonFormat deserialization.
 *
 * Serialization does not require this SPI — ExtensionKey carries its own KSerializer.
 * This SPI is needed only for deserialization (mapping key name → ExtensionKey with serializer).
 */
public interface ExtensionSerializerProvider {
    /** Returns mapping of extension key name to typed ExtensionKey (with serializer). */
    public fun knownKeys(): Map<String, ExtensionKey<*>>
}
