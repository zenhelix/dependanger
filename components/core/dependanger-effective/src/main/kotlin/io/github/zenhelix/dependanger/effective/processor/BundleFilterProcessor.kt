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

internal class BundleFilterProcessor : EffectiveMetadataProcessor {
    override val id: String = ProcessorIds.BUNDLE_FILTER
    override val phase: ProcessingPhase = ProcessingPhase.BUNDLE_FILTER
    override val constraints: Set<OrderConstraint> = setOf(OrderConstraint.runsAfter(ProcessorIds.LIBRARY_FILTER))
    override val isOptional: Boolean = false
    override val description: String = "Filters bundles and removes invalid library references"
    override fun supports(context: ProcessingContext): Boolean = true

    override suspend fun process(
        metadata: EffectiveMetadata,
        context: ProcessingContext,
    ): EffectiveMetadata {
        val existingLibraryAliases = metadata.libraries.keys
        val diagnostics = Diagnostics.builder(metadata.diagnostics)

        // 1. Clean references to non-existing libraries
        var cleanedBundles = metadata.bundles.mapValues { (bundleAlias, bundle) ->
            val validLibraries = bundle.libraries.filter { libAlias ->
                val exists = libAlias in existingLibraryAliases
                if (!exists) {
                    diagnostics.warning(
                        code = DiagnosticCodes.Bundle.LIBRARY_MISSING,
                        message = "Bundle '$bundleAlias': library '$libAlias' not found (filtered out?)",
                        processorId = id,
                        context = emptyMap(),
                    )
                }
                exists
            }
            bundle.copy(libraries = validLibraries)
        }

        // 2. Apply BundleFilter from distribution
        val distName = metadata.distribution
        if (distName != null) {
            val distribution = context.resolveDistribution(distName)
            val bundleFilter = distribution?.librarySpec?.byBundles
            if (bundleFilter != null) {
                cleanedBundles = cleanedBundles.filter { (alias, _) ->
                    bundleFilter.matchesExact(alias)
                }
            }
        }

        // 3. Remove empty bundles
        val finalBundles = cleanedBundles.filter { (alias, bundle) ->
            val notEmpty = bundle.libraries.isNotEmpty()
            if (!notEmpty) {
                diagnostics.warning(
                    code = DiagnosticCodes.Bundle.EMPTIED,
                    message = "Bundle '$alias' removed: no libraries remaining after filtering",
                    processorId = id,
                    context = emptyMap(),
                )
            }
            notEmpty
        }

        return metadata.copy(bundles = finalBundles, diagnostics = diagnostics.build())
    }
}
