package io.github.zenhelix.dependanger.generators.toml

import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.spi.ArtifactGenerator

public class TomlGenerator : ArtifactGenerator<TomlConfig> {
    override val generatorId: String = "toml-version-catalog"

    override fun generate(effective: EffectiveMetadata, config: TomlConfig): String = TODO()
}
