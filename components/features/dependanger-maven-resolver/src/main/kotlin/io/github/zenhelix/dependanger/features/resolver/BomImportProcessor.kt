package io.github.zenhelix.dependanger.features.resolver

import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase

public class BomImportProcessor : EffectiveMetadataProcessor {
    override val id: String = "bom-import"
    override val phase: ProcessingPhase = ProcessingPhase.BOM_IMPORT
    override val order: Int = phase.order
    override val isOptional: Boolean = true
    override val description: String = "Imports dependencies from Maven BOM files"
    override fun supports(context: ProcessingContext): Boolean = true

    override suspend fun process(metadata: EffectiveMetadata, context: ProcessingContext): EffectiveMetadata = TODO()
}
