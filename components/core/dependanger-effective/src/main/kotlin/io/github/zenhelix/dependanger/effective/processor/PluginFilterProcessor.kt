package io.github.zenhelix.dependanger.effective.processor

import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.core.model.filter.matchesExact
import io.github.zenhelix.dependanger.effective.DiagnosticCodes
import io.github.zenhelix.dependanger.effective.ProcessorIds
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.OrderConstraint
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase
import io.github.zenhelix.dependanger.effective.pipeline.resolveDistribution
import io.github.zenhelix.dependanger.effective.spi.PluginFiltersKey

internal class PluginFilterProcessor : EffectiveMetadataProcessor {
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
        val distribution = context.resolveDistribution(distName)
        val pluginSpec = distribution?.pluginSpec

        val customFilters = context[PluginFiltersKey] ?: emptyList()

        // Nothing to filter
        if (pluginSpec == null && customFilters.isEmpty()) return metadata

        val diagnostics = Diagnostics.builder(metadata.diagnostics)

        val tagFilter = pluginSpec?.byTags
        val aliasFilter = pluginSpec?.byAliases

        val filtered = metadata.plugins.filter { (alias, plugin) ->
            val passesTagFilter = tagFilter == null || passesTagFilter(plugin.tags, tagFilter)
            val passesAliasFilter = aliasFilter == null || aliasFilter.matchesExact(alias)
            val passesCustom = customFilters.all { filter -> filter.shouldInclude(alias, plugin, context) }
            val passes = passesTagFilter && passesAliasFilter && passesCustom

            if (!passes) {
                diagnostics.info(
                    code = DiagnosticCodes.Plugin.FILTERED,
                    message = "Plugin '$alias' filtered out by distribution '${distName ?: "custom"}'",
                    processorId = id,
                    context = emptyMap(),
                )
            }
            passes
        }

        return metadata.copy(plugins = filtered, diagnostics = diagnostics.build())
    }
}
