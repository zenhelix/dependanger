package io.github.zenhelix.dependanger.generators.toml

import io.github.zenhelix.dependanger.core.spi.ArtifactGenerator
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata

public class TomlGenerator : ArtifactGenerator<TomlConfig> {
    override val generatorId: String = "toml-version-catalog"

    override fun generate(config: TomlConfig): String = TODO()

    public fun generate(effective: EffectiveMetadata, config: TomlConfig): String = TODO()
}
