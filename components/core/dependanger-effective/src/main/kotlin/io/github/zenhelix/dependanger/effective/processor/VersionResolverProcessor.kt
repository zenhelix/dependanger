package io.github.zenhelix.dependanger.effective.processor

import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.core.model.VersionReference
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.ResolvedVersion
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase

public class VersionResolverProcessor : EffectiveMetadataProcessor {
    override val id: String = "version-resolver"
    override val phase: ProcessingPhase = ProcessingPhase.VERSION_RESOLVER
    override val order: Int = phase.order
    override val isOptional: Boolean = false
    override val description: String = "Resolves version references to actual version values"
    override fun supports(context: ProcessingContext): Boolean = true

    override suspend fun process(
        metadata: EffectiveMetadata,
        context: ProcessingContext,
    ): EffectiveMetadata {
        val originalLibraries = context.originalMetadata.libraries.associateBy { it.alias }
        val originalPlugins = context.originalMetadata.plugins.associateBy { it.alias }
        var diagnostics = metadata.diagnostics

        val resolvedLibraries = metadata.libraries.mapValues { (alias, lib) ->
            if (lib.version != null) return@mapValues lib

            when (val originalRef = originalLibraries[alias]?.version) {
                is VersionReference.Reference -> {
                    val resolved = metadata.versions[originalRef.name]
                    if (resolved != null) {
                        diagnostics = diagnostics + Diagnostics.info(
                            code = "VERSION_RESOLVED",
                            message = "Library '$alias': version ref '${originalRef.name}' -> '${resolved.value}'",
                            processorId = id,
                            context = emptyMap(),
                        )
                        lib.copy(
                            version = ResolvedVersion(
                                alias = originalRef.name,
                                value = resolved.value,
                                source = resolved.source,
                                originalRef = originalRef.name,
                            )
                        )
                    } else {
                        diagnostics = diagnostics + Diagnostics.error(
                            code = "UNRESOLVED_VERSION",
                            message = "Library '$alias': version ref '${originalRef.name}' not found in versions map",
                            processorId = id,
                            context = mapOf("alias" to alias, "ref" to originalRef.name),
                        )
                        lib
                    }
                }

                else                          -> lib
            }
        }

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
                            code = "UNRESOLVED_VERSION",
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

        return metadata.copy(
            libraries = resolvedLibraries,
            plugins = resolvedPlugins,
            diagnostics = diagnostics,
        )
    }
}
