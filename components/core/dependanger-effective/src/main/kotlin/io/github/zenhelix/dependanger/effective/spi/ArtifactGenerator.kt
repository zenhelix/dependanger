package io.github.zenhelix.dependanger.effective.spi

import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import java.nio.file.Path

public interface ArtifactGenerator<T> {
    public val generatorId: String
    public val description: String
    public val fileExtension: String
    public fun generate(effective: EffectiveMetadata): T
    public fun write(artifact: T, output: Path)
}
