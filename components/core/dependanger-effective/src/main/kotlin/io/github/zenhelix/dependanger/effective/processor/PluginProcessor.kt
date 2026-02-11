package io.github.zenhelix.dependanger.effective.processor

import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.core.model.VersionReference
import io.github.zenhelix.dependanger.effective.DiagnosticCodes
import io.github.zenhelix.dependanger.effective.ProcessorIds
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.ResolvedVersion
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase

public class PluginProcessor : EffectiveMetadataProcessor {
    override val id: String = ProcessorIds.PLUGIN
    override val phase: ProcessingPhase = ProcessingPhase.PLUGIN
    override val order: Int = phase.order
    override val isOptional: Boolean = false
    override val description: String = "Resolves plugin version references"
    override fun supports(context: ProcessingContext): Boolean = true

    override suspend fun process(
        metadata: EffectiveMetadata,
        context: ProcessingContext,
    ): EffectiveMetadata {
        val originalPlugins = context.originalMetadata.plugins.associateBy { it.alias }
        var diagnostics = metadata.diagnostics

        val resolvedPlugins = metadata.plugins.mapValues { (alias, plugin) ->
            if (plugin.version != null) return@mapValues plugin

            when (val originalRef = originalPlugins[alias]?.version) {
                is VersionReference.Reference -> {
                    val resolved = metadata.versions[originalRef.name]
                    if (resolved != null) {
                        plugin.copy(
                            version = ResolvedVersion(
                                alias = originalRef.name,
                                value = resolved.value,
                                source = resolved.source,
                                originalRef = originalRef.name,
                            )
                        )
                    } else {
                        diagnostics = diagnostics + Diagnostics.error(
                            code = DiagnosticCodes.Plugin.VERSION_UNRESOLVED,
                            message = "Plugin '$alias': version ref '${originalRef.name}' not found",
                            processorId = id,
                            context = mapOf("alias" to alias, "ref" to originalRef.name),
                        )
                        plugin
                    }
                }

                else                          -> plugin
            }
        }

        return metadata.copy(plugins = resolvedPlugins, diagnostics = diagnostics)
    }
}
