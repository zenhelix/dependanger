package io.github.zenhelix.dependanger.effective.pipeline

import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata

public interface EffectiveMetadataProcessor {
    public val id: String
    public val order: Int
    public fun process(metadata: EffectiveMetadata, context: ProcessingContext): EffectiveMetadata
}
