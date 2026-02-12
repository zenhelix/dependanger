package io.github.zenhelix.dependanger.api

import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.spi.ArtifactGenerator
import java.nio.file.Path

public data class DependangerResult(
    val effective: EffectiveMetadata?,
    val diagnostics: Diagnostics,
) {
    public val isSuccess: Boolean get() = effective != null && !diagnostics.hasErrors

    public fun <T> generate(generator: ArtifactGenerator<T>): T {
        val metadata = effective ?: throw DependangerProcessingException("Cannot generate: processing failed", null, null)
        return generator.generate(metadata)
    }

    public fun <T> writeTo(generator: ArtifactGenerator<T>, path: Path): Path {
        val artifact = generate(generator)
        generator.write(artifact, path)
        return path
    }
}
