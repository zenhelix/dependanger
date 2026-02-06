package io.github.zenhelix.dependanger.features.resolver

import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase

public class BomImportProcessor : EffectiveMetadataProcessor {
    override val id: String = "bom-import"
    override val phase: ProcessingPhase = ProcessingPhase.BOM_IMPORT

    override suspend fun process(metadata: EffectiveMetadata, context: ProcessingContext): EffectiveMetadata = TODO()
}
