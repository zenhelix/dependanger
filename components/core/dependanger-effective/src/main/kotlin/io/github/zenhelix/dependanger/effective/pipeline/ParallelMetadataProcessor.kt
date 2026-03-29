package io.github.zenhelix.dependanger.effective.pipeline

import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.ExtensionKey

/**
 * Result of a parallel processor execution.
 * Parallel processors can only produce diagnostics and extensions — they cannot modify
 * libraries, versions, plugins, or bundles.
 */
public data class ParallelResult(
    val diagnostics: Diagnostics,
    val extensions: Map<ExtensionKey<*>, Any>,
) {
    public companion object {
        public val EMPTY: ParallelResult = ParallelResult(Diagnostics.EMPTY, emptyMap())
    }
}

/**
 * A metadata processor that runs in parallel with other processors.
 *
 * Unlike [EffectiveMetadataProcessor], parallel processors have a restricted return type
 * ([ParallelResult]) that prevents modification of core metadata fields (libraries, versions,
 * plugins, bundles). This restriction is enforced at compile time.
 *
 * Implementations should override [processParallel] instead of [process].
 * The [process] method delegates to [processParallel] and merges the result into metadata.
 */
public interface ParallelMetadataProcessor : EffectiveMetadataProcessor {

    public suspend fun processParallel(
        metadata: EffectiveMetadata,
        context: ProcessingContext,
    ): ParallelResult

    override suspend fun process(
        metadata: EffectiveMetadata,
        context: ProcessingContext,
    ): EffectiveMetadata {
        val result = processParallel(metadata, context)
        return metadata.copy(
            diagnostics = metadata.diagnostics + result.diagnostics,
            extensions = metadata.extensions + result.extensions,
        )
    }
}
