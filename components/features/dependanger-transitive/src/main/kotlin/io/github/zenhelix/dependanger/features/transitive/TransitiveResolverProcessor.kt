package io.github.zenhelix.dependanger.features.transitive

import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase

public class TransitiveResolverProcessor : EffectiveMetadataProcessor {
    override val id: String = "transitive-resolver"
    override val order: Int = ProcessingPhase.TRANSITIVE_RESOLVER.order

    override fun process(metadata: EffectiveMetadata, context: ProcessingContext): EffectiveMetadata = TODO()
}
