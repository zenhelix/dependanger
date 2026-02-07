package io.github.zenhelix.dependanger.effective.pipeline

import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata

public interface EffectiveMetadataProcessor {
    public val id: String
    public val phase: ProcessingPhase
    public val order: Int get() = phase.order
    public val isOptional: Boolean get() = false
    public val description: String get() = ""
    public fun supports(context: ProcessingContext): Boolean = true
    public suspend fun process(metadata: EffectiveMetadata, context: ProcessingContext): EffectiveMetadata
}
