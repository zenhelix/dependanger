package io.github.zenhelix.dependanger.features.analysis

import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase

public class CompatibilityCheckProcessor : EffectiveMetadataProcessor {
    override val id: String = "compatibility-analysis"
    override val phase: ProcessingPhase = ProcessingPhase.COMPATIBILITY_ANALYSIS
    override val order: Int = phase.order
    override val isOptional: Boolean = true
    override val description: String = "Performs advanced compatibility analysis between libraries"
    override fun supports(context: ProcessingContext): Boolean = true

    override suspend fun process(metadata: EffectiveMetadata, context: ProcessingContext): EffectiveMetadata = TODO()
}
