package io.github.zenhelix.dependanger.effective.processor

import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.core.model.filter.DeprecatedFilter
import io.github.zenhelix.dependanger.core.model.filter.matchesAny
import io.github.zenhelix.dependanger.core.model.filter.matchesExact
import io.github.zenhelix.dependanger.core.model.filter.matchesWithPredicate
import io.github.zenhelix.dependanger.core.util.GlobMatcher
import io.github.zenhelix.dependanger.effective.DiagnosticCodes
import io.github.zenhelix.dependanger.effective.ProcessorIds
import io.github.zenhelix.dependanger.effective.model.EffectiveLibrary
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.OrderConstraint
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase
import io.github.zenhelix.dependanger.effective.pipeline.resolveDistribution
import io.github.zenhelix.dependanger.effective.spi.LibraryFiltersKey

internal class LibraryFilterProcessor : EffectiveMetadataProcessor {
    override val id: String = ProcessorIds.LIBRARY_FILTER
    override val phase: ProcessingPhase = ProcessingPhase.LIBRARY_FILTER
    override val constraints: Set<OrderConstraint> = setOf(
        OrderConstraint.runsAfter(ProcessorIds.METADATA_CONVERSION),
        OrderConstraint.runsAfter(ProcessorIds.BOM_IMPORT),
    )
    override val isOptional: Boolean = false
    override val description: String = "Filters libraries based on distribution specification"
    override fun supports(context: ProcessingContext): Boolean = true

    override suspend fun process(
        metadata: EffectiveMetadata,
        context: ProcessingContext,
    ): EffectiveMetadata {
        val distName = metadata.distribution
        val distribution = context.resolveDistribution(distName)
        val spec = distribution?.librarySpec

        val customFilters = context[LibraryFiltersKey] ?: emptyList()

        // Nothing to filter
        if (spec == null && customFilters.isEmpty()) return metadata

        val diagnostics = Diagnostics.builder(metadata.diagnostics)

        val libraryToBundles: Map<String, Set<String>> = if (spec?.byBundles != null) {
            buildMap<String, MutableSet<String>> {
                metadata.bundles.forEach { (bundleAlias, bundle) ->
                    bundle.libraries.forEach { libAlias ->
                        getOrPut(libAlias) { mutableSetOf() }.add(bundleAlias)
                    }
                }
            }
        } else {
            emptyMap()
        }

        val filtered = metadata.libraries.filter { (alias, lib) ->
            val passesSpec = spec == null || (
                    (spec.byTags?.let { passesTagFilter(lib.tags, it) } != false)
                            && (spec.byGroups?.let {
                        it.matchesWithPredicate(lib.coordinate, GlobMatcher::matches)
                    } != false)
                            && (spec.byAliases?.let { it.matchesExact(alias) } != false)
                            && (spec.byBundles?.let { it.matchesAny(libraryToBundles[alias] ?: emptySet()) } != false)
                            && passesDeprecatedFilter(lib, spec.byDeprecated)
                    )
            val passesCustom = customFilters.all { filter -> filter.shouldInclude(alias, lib, context) }
            val passes = passesSpec && passesCustom

            if (!passes) {
                diagnostics.info(
                    code = DiagnosticCodes.Library.FILTERED,
                    message = "Library '$alias' filtered out by distribution '${distName ?: "custom"}'",
                    processorId = id,
                    context = emptyMap(),
                )
            }
            passes
        }

        return metadata.copy(libraries = filtered, diagnostics = diagnostics.build())
    }

    private fun passesDeprecatedFilter(lib: EffectiveLibrary, filter: DeprecatedFilter?): Boolean {
        if (filter == null) return true
        return filter.include || !lib.isDeprecated
    }
}
