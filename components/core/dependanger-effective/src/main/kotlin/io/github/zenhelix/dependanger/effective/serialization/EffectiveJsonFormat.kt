package io.github.zenhelix.dependanger.effective.serialization

import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import java.nio.file.Path

public class EffectiveJsonFormat {
    public fun serialize(metadata: EffectiveMetadata): String = TODO()
    public fun deserialize(json: String): EffectiveMetadata = TODO()
    public fun write(metadata: EffectiveMetadata, path: Path): Unit = TODO()
    public fun read(path: Path): EffectiveMetadata = TODO()
}
