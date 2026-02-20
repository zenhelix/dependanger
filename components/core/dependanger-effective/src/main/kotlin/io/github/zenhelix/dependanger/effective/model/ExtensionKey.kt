package io.github.zenhelix.dependanger.effective.model

import io.github.zenhelix.dependanger.core.model.TypedKey
import kotlinx.serialization.KSerializer

public class ExtensionKey<T : Any>(name: String, serializer: KSerializer<T>) : TypedKey<T>(name, serializer)

@Suppress("UNCHECKED_CAST")
public fun <T : Any> EffectiveMetadata.getExtension(key: ExtensionKey<T>): T? =
    extensions[key] as? T

public fun <T : Any> EffectiveMetadata.withExtension(key: ExtensionKey<T>, value: T): EffectiveMetadata =
    copy(extensions = extensions + (key to value))
