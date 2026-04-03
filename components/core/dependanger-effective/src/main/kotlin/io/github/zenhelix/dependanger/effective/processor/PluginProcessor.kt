package io.github.zenhelix.dependanger.effective.processor

import io.github.zenhelix.dependanger.effective.ProcessorIds
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.OrderConstraint
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase

/**
 * Plugin enrichment processor. Runs after [VersionResolverProcessor] which already
 * resolves all version references (both libraries and plugins). This processor is
 * a pipeline extension point for future plugin-specific transformations.
 */
public class PluginProcessor : EffectiveMetadataProcessor {
    override val id: String = ProcessorIds.PLUGIN
    override val phase: ProcessingPhase = ProcessingPhase.PLUGIN
    override val constraints: Set<OrderConstraint> = setOf(OrderConstraint.runsAfter(ProcessorIds.PLUGIN_FILTER))
    override val isOptional: Boolean = false
    override val description: String = "Plugin enrichment (version references resolved by VersionResolverProcessor)"
    override fun supports(context: ProcessingContext): Boolean = true

    override suspend fun process(
        metadata: EffectiveMetadata,
        context: ProcessingContext,
    ): EffectiveMetadata = metadata
}
