package io.github.zenhelix.dependanger.effective.spi

import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata

public interface ArtifactGenerator<C> {
    public val generatorId: String
    public fun generate(effective: EffectiveMetadata, config: C): String
}
