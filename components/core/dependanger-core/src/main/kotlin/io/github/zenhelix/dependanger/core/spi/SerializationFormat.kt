package io.github.zenhelix.dependanger.core.spi

import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata

public interface SerializationFormat<T> {
    public val formatId: String
    public fun serialize(metadata: DependangerMetadata): T
    public fun deserialize(input: T): DependangerMetadata
}
