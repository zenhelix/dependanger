package io.github.zenhelix.dependanger.features.security

import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase

public class SecurityCheckProcessor : EffectiveMetadataProcessor {
    override val id: String = "security-check"
    override val phase: ProcessingPhase = ProcessingPhase.SECURITY_CHECK
    override val isOptional: Boolean = true

    override suspend fun process(metadata: EffectiveMetadata, context: ProcessingContext): EffectiveMetadata = TODO()
}
