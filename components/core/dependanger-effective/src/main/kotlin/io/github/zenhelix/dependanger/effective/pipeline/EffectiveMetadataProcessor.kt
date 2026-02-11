package io.github.zenhelix.dependanger.effective.pipeline

import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata

public interface EffectiveMetadataProcessor {
    public val id: String
    public val phase: ProcessingPhase
    public val order: Int
    public val isOptional: Boolean
    public val description: String
    public fun supports(context: ProcessingContext): Boolean
    public suspend fun process(metadata: EffectiveMetadata, context: ProcessingContext): EffectiveMetadata
}
