package io.github.zenhelix.dependanger.features.updates

import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase

public class UpdateCheckProcessor : EffectiveMetadataProcessor {
    override val id: String = "update-check"
    override val phase: ProcessingPhase = ProcessingPhase.UPDATE_CHECK
    override val order: Int = phase.order
    override val isOptional: Boolean = true
    override val description: String = "Checks for available library updates"
    override fun supports(context: ProcessingContext): Boolean = true

    override suspend fun process(metadata: EffectiveMetadata, context: ProcessingContext): EffectiveMetadata = TODO()
}
