package io.github.zenhelix.dependanger.generators.bom

import io.github.zenhelix.dependanger.core.spi.ArtifactGenerator
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata

public class BomGenerator : ArtifactGenerator<BomConfig> {
    override val generatorId: String = "maven-bom"

    override fun generate(config: BomConfig): String = TODO()

    public fun generate(effective: EffectiveMetadata, config: BomConfig): String = TODO()
}
