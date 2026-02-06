package io.github.zenhelix.dependanger.generators.bom

import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.spi.ArtifactGenerator

public class BomGenerator : ArtifactGenerator<BomConfig> {
    override val generatorId: String = "maven-bom"

    override fun generate(effective: EffectiveMetadata, config: BomConfig): String = TODO()
}
