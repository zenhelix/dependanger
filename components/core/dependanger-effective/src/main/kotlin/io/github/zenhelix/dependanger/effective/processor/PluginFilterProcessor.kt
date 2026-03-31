package io.github.zenhelix.dependanger.effective.processor

import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.effective.DiagnosticCodes
import io.github.zenhelix.dependanger.effective.ProcessorIds
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.OrderConstraint
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase
import io.github.zenhelix.dependanger.effective.spi.PluginFilter
import java.util.ServiceLoader

public class PluginFilterProcessor : EffectiveMetadataProcessor {
    override val id: String = ProcessorIds.PLUGIN_FILTER
    override val phase: ProcessingPhase = ProcessingPhase.PLUGIN_FILTER
    override val constraints: Set<OrderConstraint> = setOf(OrderConstraint.runsAfter(ProcessorIds.METADATA_CONVERSION))
    override val isOptional: Boolean = false
    override val description: String = "Filters plugins based on distribution tag rules"
    override fun supports(context: ProcessingContext): Boolean = true

    override suspend fun process(
        metadata: EffectiveMetadata,
        context: ProcessingContext,
    ): EffectiveMetadata {
        val distName = metadata.distribution
        val distribution = distName?.let { name -> context.originalMetadata.distributions.find { it.name == name } }
        val tagFilter = distribution?.spec?.byTags

        val customFilters = ServiceLoader.load(PluginFilter::class.java).toList()

        // Nothing to filter
        if (tagFilter == null && customFilters.isEmpty()) return metadata

        val originalPluginsIndex = context.originalMetadata.plugins.associateBy { it.alias }
        var diagnostics = metadata.diagnostics

        val filtered = metadata.plugins.filter { (alias, plugin) ->
            val tags = originalPluginsIndex[alias]?.tags ?: emptySet()

            val passesTagFilter = tagFilter == null || passesTagFilter(tags, tagFilter)
            val passesCustom = customFilters.all { filter -> filter.shouldInclude(alias, plugin, context) }
            val passes = passesTagFilter && passesCustom

            if (!passes) {
                diagnostics += Diagnostics.info(
                    code = DiagnosticCodes.Plugin.FILTERED,
                    message = "Plugin '$alias' filtered out by distribution '${distName ?: "custom"}'",
                    processorId = id,
                    context = emptyMap(),
                )
            }
            passes
        }

        return metadata.copy(plugins = filtered, diagnostics = diagnostics)
    }
}
