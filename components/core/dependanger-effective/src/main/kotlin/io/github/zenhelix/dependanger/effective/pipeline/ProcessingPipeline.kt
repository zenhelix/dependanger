package io.github.zenhelix.dependanger.effective.pipeline

import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata

public class ProcessingPipeline(
    private val processors: List<EffectiveMetadataProcessor>,
) {
    public fun process(context: ProcessingContext): EffectiveMetadata {
        val initial = EffectiveMetadata(
            schemaVersion = context.originalMetadata.schemaVersion,
            distribution = context.activeDistribution,
        )
        return processors.sortedBy { it.order }.fold(initial) { acc, processor ->
            processor.process(acc, context)
        }
    }
}
