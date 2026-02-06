package io.github.zenhelix.dependanger.effective.pipeline

import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata

public class ProcessingPipeline(
    private val processors: List<EffectiveMetadataProcessor>,
) {
    public suspend fun process(context: ProcessingContext): EffectiveMetadata {
        var result = EffectiveMetadata(
            schemaVersion = context.originalMetadata.schemaVersion,
            distribution = context.activeDistribution,
        )
        for (processor in processors.sortedBy { it.order }) {
            result = processor.process(result, context)
        }
        return result
    }
}
