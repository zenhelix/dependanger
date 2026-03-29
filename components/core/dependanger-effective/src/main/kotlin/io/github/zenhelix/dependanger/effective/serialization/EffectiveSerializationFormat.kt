package io.github.zenhelix.dependanger.effective.serialization

import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import java.nio.file.Path

public interface EffectiveSerializationFormat<T> {
    public val formatId: String
    public val fileExtension: String
    public fun serialize(metadata: EffectiveMetadata): T
    public fun deserialize(input: T): EffectiveMetadata
    public fun deserializeDetailed(input: T): DeserializationResult
    public fun write(metadata: EffectiveMetadata, path: Path)
    public fun read(path: Path): EffectiveMetadata
    public fun readDetailed(path: Path): DeserializationResult
}
