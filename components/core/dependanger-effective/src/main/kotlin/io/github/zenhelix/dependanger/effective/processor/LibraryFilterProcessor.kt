package io.github.zenhelix.dependanger.effective.processor

import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase

public class LibraryFilterProcessor : EffectiveMetadataProcessor {
    override val id: String = "library-filter"
    override val order: Int = ProcessingPhase.LIBRARY_FILTER.order
    override suspend fun process(metadata: EffectiveMetadata, context: ProcessingContext): EffectiveMetadata = TODO()
}
