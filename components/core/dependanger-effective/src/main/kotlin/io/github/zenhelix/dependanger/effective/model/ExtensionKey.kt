package io.github.zenhelix.dependanger.effective.model

public class ExtensionKey<T : Any>(public val name: String) {
    override fun equals(other: Any?): Boolean = other is ExtensionKey<*> && name == other.name
    override fun hashCode(): Int = name.hashCode()
    override fun toString(): String = "ExtensionKey($name)"
}

@Suppress("UNCHECKED_CAST")
public fun <T : Any> EffectiveMetadata.getExtension(key: ExtensionKey<T>): T? =
    extensions[key] as? T

public fun <T : Any> EffectiveMetadata.withExtension(key: ExtensionKey<T>, value: T): EffectiveMetadata =
    copy(extensions = extensions + (key to value))
