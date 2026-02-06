package io.github.zenhelix.dependanger.effective.pipeline

import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata

public interface EffectiveMetadataProcessor {
    public val id: String
    public val phase: ProcessingPhase
    public val order: Int get() = phase.order
    public suspend fun process(metadata: EffectiveMetadata, context: ProcessingContext): EffectiveMetadata
}
