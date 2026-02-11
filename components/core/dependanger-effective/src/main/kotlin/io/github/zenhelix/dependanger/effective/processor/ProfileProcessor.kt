package io.github.zenhelix.dependanger.effective.processor

import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.effective.DiagnosticCodes
import io.github.zenhelix.dependanger.effective.ProcessorIds
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase

public class ProfileProcessor : EffectiveMetadataProcessor {
    override val id: String = ProcessorIds.PROFILE
    override val phase: ProcessingPhase = ProcessingPhase.PROFILE
    override val order: Int = phase.order
    override val isOptional: Boolean = false
    override val description: String = "Applies distribution profile to metadata"
    override fun supports(context: ProcessingContext): Boolean = true

    override suspend fun process(
        metadata: EffectiveMetadata,
        context: ProcessingContext,
    ): EffectiveMetadata {
        val distName = context.activeDistribution
            ?: return metadata

        val distributions = context.originalMetadata.distributions
        val found = distributions.find { it.name == distName }

        return if (found != null) {
            metadata.copy(
                distribution = distName,
                diagnostics = metadata.diagnostics + Diagnostics.info(
                    code = DiagnosticCodes.Profile.APPLIED,
                    message = "Distribution '$distName' applied",
                    processorId = id,
                    context = emptyMap(),
                ),
            )
        } else {
            val available = distributions.map { it.name }
            metadata.copy(
                distribution = distName,
                diagnostics = metadata.diagnostics + Diagnostics.error(
                    code = DiagnosticCodes.Profile.NOT_FOUND,
                    message = "Distribution '$distName' not found. Available: $available",
                    processorId = id,
                    context = emptyMap(),
                ),
            )
        }
    }
}
