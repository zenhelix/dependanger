package io.github.zenhelix.dependanger.api

import io.github.zenhelix.dependanger.effective.spi.ArtifactGenerator
import java.util.ServiceLoader

/**
 * Registry for discovering [ArtifactGenerator] implementations via ServiceLoader.
 *
 * Generators register themselves in `META-INF/services/io.github.zenhelix.dependanger.effective.spi.ArtifactGenerator`.
 * This registry provides lookup by [generatorId] for programmatic consumers.
 */
public object GeneratorRegistry {

    public val generators: List<ArtifactGenerator<*>> by lazy {
        ServiceLoader.load(ArtifactGenerator::class.java).toList()
    }

    public fun findById(id: String): ArtifactGenerator<*>? =
        generators.firstOrNull { it.generatorId == id }

    public val ids: List<String> by lazy { generators.map { it.generatorId } }
}
