package io.github.zenhelix.dependanger.api

import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.spi.ArtifactGenerator
import io.github.zenhelix.dependanger.generators.bom.BomConfig
import io.github.zenhelix.dependanger.generators.toml.TomlConfig
import java.nio.file.Path

public data class DependangerResult(
    val effective: EffectiveMetadata?,
    val diagnostics: Diagnostics,
) {
    public val isSuccess: Boolean get() = effective != null && !diagnostics.hasErrors

    // Generic API for any ArtifactGenerator
    public fun <T> generate(generator: ArtifactGenerator<T>): T {
        val metadata = effective ?: throw DependangerProcessingException("Cannot generate: processing failed", null)
        return generator.generate(metadata)
    }

    public fun <T> writeTo(generator: ArtifactGenerator<T>, path: Path): Path {
        val artifact = generate(generator)
        generator.write(artifact, path)
        return path
    }

    // Convenience methods for built-in generators
    public fun toToml(config: TomlConfig = TomlConfig()): String = TODO()
    public fun toBom(config: BomConfig): String = TODO()
    public fun writeTomlTo(path: Path, config: TomlConfig = TomlConfig()): Path = TODO()
    public fun writeBomTo(path: Path, config: BomConfig): Path = TODO()
}
