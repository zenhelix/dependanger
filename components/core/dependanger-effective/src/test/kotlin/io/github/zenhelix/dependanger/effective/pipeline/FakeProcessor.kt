package io.github.zenhelix.dependanger.effective.pipeline

import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata

class FakeProcessor(
    override val id: String,
    override val phase: ProcessingPhase,
    override val order: Int,
    override val isOptional: Boolean = false,
    override val description: String = "test",
    private val supported: Boolean = true,
) : EffectiveMetadataProcessor {
    override fun supports(context: ProcessingContext): Boolean = supported
    override suspend fun process(metadata: EffectiveMetadata, context: ProcessingContext): EffectiveMetadata = metadata
}
