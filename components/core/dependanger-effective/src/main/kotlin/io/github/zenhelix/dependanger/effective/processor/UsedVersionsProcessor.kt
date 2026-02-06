package io.github.zenhelix.dependanger.effective.processor

import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase

public class UsedVersionsProcessor : EffectiveMetadataProcessor {
    override val id: String = "used-versions"
    override val order: Int = ProcessingPhase.USED_VERSIONS.order
    override fun process(metadata: EffectiveMetadata, context: ProcessingContext): EffectiveMetadata = TODO()
}
