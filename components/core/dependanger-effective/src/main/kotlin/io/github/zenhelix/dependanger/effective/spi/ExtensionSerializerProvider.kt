package io.github.zenhelix.dependanger.effective.spi

import io.github.zenhelix.dependanger.effective.model.ExtensionKey
import kotlinx.serialization.modules.SerializersModule

public interface ExtensionSerializerProvider {
    public fun serializersModule(): SerializersModule
    public fun knownKeys(): Map<String, ExtensionKey<*>>
}
