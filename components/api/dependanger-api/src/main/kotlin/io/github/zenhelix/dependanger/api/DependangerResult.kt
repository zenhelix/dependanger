package io.github.zenhelix.dependanger.api

import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.spi.ArtifactGenerator
import java.nio.file.Path

public sealed class DependangerResult {
    public abstract val diagnostics: Diagnostics

    public data class Success(
        val effective: EffectiveMetadata,
        override val diagnostics: Diagnostics,
    ) : DependangerResult()

    public data class Failure(
        override val diagnostics: Diagnostics,
    ) : DependangerResult()

    public val isSuccess: Boolean get() = this is Success && !diagnostics.hasErrors

    public fun effectiveOrNull(): EffectiveMetadata? = when (this) {
        is Success -> effective
        is Failure -> null
    }

    public fun <T> generate(generator: ArtifactGenerator<T>): T {
        val success = this as? Success
        if (success == null || diagnostics.hasErrors) {
            throw DependangerProcessingException(
                "Cannot generate: processing completed with errors. Check diagnostics for details.",
                null, null,
            )
        }
        return wrapNonDependangerException({ e ->
                                               DependangerGenerationException("Generation failed: ${e.message}", generator.generatorId, e)
                                           }) {
            generator.generate(success.effective)
        }
    }

    public fun <T> writeTo(generator: ArtifactGenerator<T>, path: Path): Path {
        val artifact = generate(generator)
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
