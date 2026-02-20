package io.github.zenhelix.dependanger.core.model

import kotlinx.serialization.KSerializer

public abstract class TypedKey<T : Any>(
    public val name: String,
    public val serializer: KSerializer<T>,
) {
    override fun equals(other: Any?): Boolean = other is TypedKey<*> && other::class == this::class && name == other.name
    override fun hashCode(): Int = this::class.hashCode() * 31 + name.hashCode()
    override fun toString(): String = "${this::class.simpleName}($name)"
}
