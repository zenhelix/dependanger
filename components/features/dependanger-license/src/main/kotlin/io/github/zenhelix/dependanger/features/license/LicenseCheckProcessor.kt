package io.github.zenhelix.dependanger.features.license

import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase

public class LicenseCheckProcessor : EffectiveMetadataProcessor {
    override val id: String = "license-check"
    override val phase: ProcessingPhase = ProcessingPhase.LICENSE_CHECK
    override val order: Int = phase.order
    override val isOptional: Boolean = true
    override val description: String = "Checks library license compliance"
    override fun supports(context: ProcessingContext): Boolean = true

    override suspend fun process(metadata: EffectiveMetadata, context: ProcessingContext): EffectiveMetadata = TODO()
}
