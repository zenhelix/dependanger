package io.github.zenhelix.dependanger.api

import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.spi.ArtifactGenerator
import java.nio.file.Path

/**
 * Handles artifact generation from a [DependangerResult].
 * Separated from [DependangerResult] to keep it a pure data container.
 */
public class ArtifactGeneration(private val result: DependangerResult) {

    private val effective: EffectiveMetadata
        get() = when (result) {
            is DependangerResult.Success             -> result.effective
            is DependangerResult.CompletedWithErrors -> throw DependangerProcessingException(
                "Cannot generate: processing completed with errors. Check diagnostics for details.",
                null, null,
            )

            is DependangerResult.Failure             -> throw DependangerProcessingException(
                "Cannot generate: processing failed. Check diagnostics for details.",
                null, null,
            )
        }

    public fun <T> generate(generator: ArtifactGenerator<T>): T =
        wrapNonDependangerException({ e ->
                                        DependangerGenerationException("Generation failed: ${e.message}", generator.generatorId, e)
                                    }) {
            generator.generate(effective)
        }

    public fun <T> writeTo(generator: ArtifactGenerator<T>, path: Path): Path {
        val artifact = generate(generator)
        return wrapNonDependangerException({ e ->
                                               DependangerGenerationException("Failed to write artifact: ${e.message}", generator.generatorId, e)
                                           }) {
            generator.write(artifact, path)
            path
        }
    }
}

/** Creates an [ArtifactGeneration] facade for this result. */
public val DependangerResult.generation: ArtifactGeneration get() = ArtifactGeneration(this)
