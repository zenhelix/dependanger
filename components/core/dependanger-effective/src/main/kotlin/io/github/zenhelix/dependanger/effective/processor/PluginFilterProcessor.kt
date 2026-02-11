package io.github.zenhelix.dependanger.effective.processor

import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.core.model.filter.TagFilter
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase

public class PluginFilterProcessor : EffectiveMetadataProcessor {
    override val id: String = "plugin-filter"
    override val phase: ProcessingPhase = ProcessingPhase.PLUGIN_FILTER
    override val order: Int = phase.order
    override val isOptional: Boolean = false
    override val description: String = "Filters plugins based on distribution tag rules"
    override fun supports(context: ProcessingContext): Boolean = true

    override suspend fun process(
        metadata: EffectiveMetadata,
        context: ProcessingContext,
    ): EffectiveMetadata {
        val distName = metadata.distribution ?: return metadata
        val distribution = context.originalMetadata.distributions.find { it.name == distName }
        val tagFilter = distribution?.spec?.byTags ?: return metadata

        val originalPluginsIndex = context.originalMetadata.plugins.associateBy { it.alias }
        var diagnostics = metadata.diagnostics

        val filteredPlugins = metadata.plugins.filter { (alias, _) ->
            val originalPlugin = originalPluginsIndex[alias]
            val tags = originalPlugin?.tags ?: emptySet()

            val passes = passesTagFilter(tags, tagFilter)
            if (!passes) {
                diagnostics = diagnostics + Diagnostics.info(
                    code = "PLUGIN_FILTERED",
                    message = "Plugin '$alias' filtered out by distribution '$distName'",
                    processorId = id,
                    context = emptyMap(),
                )
            }
            passes
        }

        return metadata.copy(plugins = filteredPlugins, diagnostics = diagnostics)
    }

    private fun passesTagFilter(tags: Set<String>, filter: TagFilter): Boolean {
        val passesIncludes = if (filter.includes.isEmpty()) true
        else filter.includes.any { include ->
            val anyOfOk = include.anyOf.isEmpty() || (tags intersect include.anyOf).isNotEmpty()
            val allOfOk = include.allOf.isEmpty() || tags.containsAll(include.allOf)
            anyOfOk && allOfOk
        }

        val passesExcludes = filter.excludes.all { exclude ->
            exclude.anyOf.isEmpty() || (tags intersect exclude.anyOf).isEmpty()
        }

        return passesIncludes && passesExcludes
    }
}
