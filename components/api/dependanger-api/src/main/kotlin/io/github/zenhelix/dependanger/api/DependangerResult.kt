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
        if (!isSuccess) {
            throw DependangerProcessingException(
                "Cannot generate: processing completed with errors. Check diagnostics for details.",
                null,
                null,
            )
        }
        return generateUnsafe(generator)
    }

    public fun <T> generateUnsafe(generator: ArtifactGenerator<T>): T {
        val metadata = effective
            ?: throw DependangerProcessingException("Cannot generate: processing failed (no effective metadata)", null, null)
        return wrapNonDependangerException({ e ->
                                               DependangerGenerationException("Generation failed: ${e.message}", generator.generatorId, e)
                                           }) {
            generator.generate(metadata)
        }
    }

    public fun <T> writeTo(generator: ArtifactGenerator<T>, path: Path): Path {
        val artifact = generate(generator)
        return writeArtifact(generator, artifact, path)
    }

    public fun <T> writeToUnsafe(generator: ArtifactGenerator<T>, path: Path): Path {
        val artifact = generateUnsafe(generator)
        return writeArtifact(generator, artifact, path)
    }

    private fun <T> writeArtifact(generator: ArtifactGenerator<T>, artifact: T, path: Path): Path =
        wrapNonDependangerException({ e ->
                                        DependangerGenerationException("Failed to write artifact: ${e.message}", generator.generatorId, e)
                                    }) {
            generator.write(artifact, path)
            path
        }
}
