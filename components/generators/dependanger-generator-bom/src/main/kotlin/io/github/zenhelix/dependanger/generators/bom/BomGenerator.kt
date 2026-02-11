package io.github.zenhelix.dependanger.generators.bom

import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.spi.ArtifactGenerator
import java.nio.file.Path

public class BomGenerator(private val config: BomConfig) : ArtifactGenerator<String> {
    override val generatorId: String = "maven-bom"
    override val description: String = "Generates Maven BOM (Bill of Materials)"
    override val fileExtension: String = ".xml"

    override fun generate(effective: EffectiveMetadata): String = TODO()
    override fun write(artifact: String, output: Path): Unit = TODO()
}
