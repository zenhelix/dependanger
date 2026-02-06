package io.github.zenhelix.dependanger.features.license

import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase

public class LicenseCheckProcessor : EffectiveMetadataProcessor {
    override val id: String = "license-check"
    override val order: Int = ProcessingPhase.LICENSE_CHECK.order

    override suspend fun process(metadata: EffectiveMetadata, context: ProcessingContext): EffectiveMetadata = TODO()
}
