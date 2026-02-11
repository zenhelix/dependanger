package io.github.zenhelix.dependanger.generators.toml

import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.spi.ArtifactGenerator
import java.nio.file.Path

public class TomlGenerator(private val config: TomlConfig) : ArtifactGenerator<String> {
    override val generatorId: String = "toml-version-catalog"
    override val description: String = "Generates Gradle version catalog in TOML format"
    override val fileExtension: String = ".toml"

    override fun generate(effective: EffectiveMetadata): String = TODO()
    override fun write(artifact: String, output: Path): Unit = TODO()
}
