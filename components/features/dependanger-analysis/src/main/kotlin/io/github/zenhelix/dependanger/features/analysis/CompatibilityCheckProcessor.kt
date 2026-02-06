package io.github.zenhelix.dependanger.features.analysis

import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase

public class CompatibilityCheckProcessor : EffectiveMetadataProcessor {
    override val id: String = "compatibility-analysis"
    override val order: Int = ProcessingPhase.COMPATIBILITY_ANALYSIS.order

    override suspend fun process(metadata: EffectiveMetadata, context: ProcessingContext): EffectiveMetadata = TODO()
}
