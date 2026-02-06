package io.github.zenhelix.dependanger.effective.processor

import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase

public class VersionResolverProcessor : EffectiveMetadataProcessor {
    override val id: String = "version-resolver"
    override val order: Int = ProcessingPhase.VERSION_RESOLVER.order
    override fun process(metadata: EffectiveMetadata, context: ProcessingContext): EffectiveMetadata = TODO()
}
