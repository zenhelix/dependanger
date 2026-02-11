package io.github.zenhelix.dependanger.effective.processor

import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.core.model.filter.AliasFilter
import io.github.zenhelix.dependanger.core.model.filter.BundleFilter
import io.github.zenhelix.dependanger.core.model.filter.DeprecatedFilter
import io.github.zenhelix.dependanger.core.model.filter.GroupFilter
import io.github.zenhelix.dependanger.core.model.filter.TagFilter
import io.github.zenhelix.dependanger.core.util.GlobMatcher
import io.github.zenhelix.dependanger.effective.DiagnosticCodes
import io.github.zenhelix.dependanger.effective.ProcessorIds
import io.github.zenhelix.dependanger.effective.model.EffectiveBundle
import io.github.zenhelix.dependanger.effective.model.EffectiveLibrary
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase

public class LibraryFilterProcessor : EffectiveMetadataProcessor {
    override val id: String = ProcessorIds.LIBRARY_FILTER
    override val phase: ProcessingPhase = ProcessingPhase.LIBRARY_FILTER
    override val order: Int = phase.order
    override val isOptional: Boolean = false
    override val description: String = "Filters libraries based on distribution specification"
    override fun supports(context: ProcessingContext): Boolean = true

    override suspend fun process(
        metadata: EffectiveMetadata,
        context: ProcessingContext,
    ): EffectiveMetadata {
        val distName = metadata.distribution ?: return metadata
        val distribution = context.originalMetadata.distributions.find { it.name == distName }
        val spec = distribution?.spec ?: return metadata

        var diagnostics = metadata.diagnostics
        val bundleIndex = metadata.bundles

        val filtered = metadata.libraries.filter { (alias, lib) ->
            val passes = passesTagFilter(lib, spec.byTags)
                    && passesGroupFilter(lib, spec.byGroups)
                    && passesAliasFilter(alias, spec.byAliases)
                    && passesBundleFilter(alias, bundleIndex, spec.byBundles)
                    && passesDeprecatedFilter(lib, spec.byDeprecated)

            if (!passes) {
                diagnostics += Diagnostics.info(
                    code = DiagnosticCodes.Library.FILTERED,
                    message = "Library '$alias' filtered out by distribution '$distName'",
                    processorId = id,
                    context = emptyMap(),
                )
            }
            passes
        }

        return metadata.copy(libraries = filtered, diagnostics = diagnostics)
    }

    private fun passesTagFilter(lib: EffectiveLibrary, filter: TagFilter?): Boolean {
        if (filter == null) return true
        val tags = lib.tags

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

    private fun passesGroupFilter(lib: EffectiveLibrary, filter: GroupFilter?): Boolean {
        if (filter == null) return true
        val coordinate = "${lib.group}:${lib.artifact}"

        val passesIncludes = filter.includes.isEmpty()
                || filter.includes.any { pattern -> GlobMatcher.matchesCoordinate(pattern, coordinate) }

        val passesExcludes = filter.excludes.isEmpty()
                || filter.excludes.none { pattern -> GlobMatcher.matchesCoordinate(pattern, coordinate) }

        return passesIncludes && passesExcludes
    }

    private fun passesAliasFilter(alias: String, filter: AliasFilter?): Boolean {
        if (filter == null) return true
        val passesIncludes = filter.includes.isEmpty() || alias in filter.includes
        val passesExcludes = filter.excludes.isEmpty() || alias !in filter.excludes
        return passesIncludes && passesExcludes
    }

    private fun passesBundleFilter(
        alias: String,
        bundles: Map<String, EffectiveBundle>,
        filter: BundleFilter?,
    ): Boolean {
        if (filter == null) return true
        val memberOf = bundles.filter { (_, b) -> alias in b.libraries }.keys

        val passesIncludes = filter.includes.isEmpty()
                || (memberOf intersect filter.includes).isNotEmpty()

        val passesExcludes = filter.excludes.isEmpty()
                || (memberOf intersect filter.excludes).isEmpty()

        return passesIncludes && passesExcludes
    }

    private fun passesDeprecatedFilter(lib: EffectiveLibrary, filter: DeprecatedFilter?): Boolean {
        if (filter == null) return true
        return filter.include || !lib.isDeprecated
    }
}
