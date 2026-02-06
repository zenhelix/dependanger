package io.github.zenhelix.dependanger.effective.pipeline

import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata

public class ProcessingPipeline(
    private val processors: List<EffectiveMetadataProcessor>,
) {
    public fun process(metadata: DependangerMetadata, context: ProcessingContext): EffectiveMetadata = TODO()
}
