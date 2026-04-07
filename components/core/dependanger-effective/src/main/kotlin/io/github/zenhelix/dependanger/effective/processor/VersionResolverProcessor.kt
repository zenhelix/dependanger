package io.github.zenhelix.dependanger.effective.processor

import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.core.model.DiagnosticsBuilder
import io.github.zenhelix.dependanger.effective.DiagnosticCodes
import io.github.zenhelix.dependanger.effective.ProcessorIds
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.EffectiveVersion
import io.github.zenhelix.dependanger.effective.model.ResolvedVersion
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.OrderConstraint
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase

internal class VersionResolverProcessor : EffectiveMetadataProcessor {
    override val id: String = ProcessorIds.VERSION_RESOLVER
    override val phase: ProcessingPhase = ProcessingPhase.VERSION_RESOLVER
    override val constraints: Set<OrderConstraint> = setOf(
        OrderConstraint.runsAfter(ProcessorIds.VERSION_FALLBACK),
        OrderConstraint.runsAfter(ProcessorIds.EXTRACTED_VERSIONS),
        OrderConstraint.runsAfter(ProcessorIds.BOM_IMPORT),
    )
    override val isOptional: Boolean = false
    override val description: String = "Resolves version references to actual version values"
    override fun supports(context: ProcessingContext): Boolean = true

    override suspend fun process(
        metadata: EffectiveMetadata,
        context: ProcessingContext,
    ): EffectiveMetadata {
        val diagnostics = Diagnostics.builder(metadata.diagnostics)

        val resolvedLibraries = metadata.libraries.mapValues { (alias, lib) ->
            val resolved = resolveVersion(alias, "Library", lib.version, metadata.versions, diagnostics)
            if (resolved !== lib.version) lib.copy(version = resolved) else lib
        }

        val resolvedPlugins = metadata.plugins.mapValues { (alias, plugin) ->
            val resolved = resolveVersion(alias, "Plugin", plugin.version, metadata.versions, diagnostics)
            if (resolved !== plugin.version) plugin.copy(version = resolved) else plugin
        }

        return metadata.copy(
            libraries = resolvedLibraries,
            plugins = resolvedPlugins,
            diagnostics = diagnostics.build(),
        )
    }

    private fun resolveVersion(
        alias: String,
        entityLabel: String,
        version: EffectiveVersion,
        versions: Map<String, ResolvedVersion>,
        diagnostics: DiagnosticsBuilder,
    ): EffectiveVersion = when (version) {
        is EffectiveVersion.Unresolved -> {
            val resolved = versions[version.refName]
            if (resolved != null) {
                diagnostics.info(
                    code = DiagnosticCodes.Version.RESOLVED,
                    message = "$entityLabel '$alias': version ref '${version.refName}' -> '${resolved.value}'",
                    processorId = id,
                    context = emptyMap(),
                )
                EffectiveVersion.Resolved(
                    ResolvedVersion(
                        alias = version.refName,
                        value = resolved.value,
                        source = resolved.source,
                        originalRef = version.refName,
                    )
                )
            } else {
                diagnostics.error(
                    code = DiagnosticCodes.Version.UNRESOLVED,
                    message = "$entityLabel '$alias': version ref '${version.refName}' not found in versions map",
                    processorId = id,
                    context = mapOf("alias" to alias, "ref" to version.refName),
                )
                version
            }
        }

        is EffectiveVersion.Resolved,
        is EffectiveVersion.Inline,
        is EffectiveVersion.Range,
        is EffectiveVersion.None,
                                       -> version
    }
}
