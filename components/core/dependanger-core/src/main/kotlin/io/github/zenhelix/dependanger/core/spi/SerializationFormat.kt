package io.github.zenhelix.dependanger.core.spi

import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata
import java.nio.file.Path

public interface SerializationFormat<T> {
    public val formatId: String
    public val fileExtension: String
    public fun serialize(metadata: DependangerMetadata): T
    public fun deserialize(input: T): DependangerMetadata
    public fun write(metadata: DependangerMetadata, path: Path)
    public fun read(path: Path): DependangerMetadata
}
