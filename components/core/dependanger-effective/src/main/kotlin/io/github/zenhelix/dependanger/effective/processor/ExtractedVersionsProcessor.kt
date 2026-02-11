package io.github.zenhelix.dependanger.effective.processor

import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.ResolvedVersion
import io.github.zenhelix.dependanger.effective.model.VersionSource
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase

public class ExtractedVersionsProcessor : EffectiveMetadataProcessor {
    override val id: String = "extracted-versions"
    override val phase: ProcessingPhase = ProcessingPhase.EXTRACTED_VERSIONS

    override suspend fun process(
        metadata: EffectiveMetadata,
        context: ProcessingContext,
    ): EffectiveMetadata {
        val newVersions = metadata.versions.toMutableMap()
        var diagnostics = metadata.diagnostics

        val updatedLibraries = metadata.libraries.mapValues { (alias, lib) ->
            val version = lib.version
            if (version != null && version.alias.isEmpty() && version.value.isNotEmpty()) {
                val versionName = generateVersionName(alias, newVersions)
                val resolved = ResolvedVersion(
                    alias = versionName,
                    value = version.value,
                    source = VersionSource.DECLARED,
                )
                newVersions[versionName] = resolved
                diagnostics = diagnostics + Diagnostics.info(
                    code = "EXTRACTED_VERSION_CREATED",
                    message = "Extracted version '$versionName' = '${version.value}' from library '$alias'",
                    processorId = id,
                )
                lib.copy(version = resolved)
            } else {
                lib
            }
        }

        val updatedPlugins = metadata.plugins.mapValues { (alias, plugin) ->
            val version = plugin.version
            if (version != null && version.alias.isEmpty() && version.value.isNotEmpty()) {
                val versionName = generateVersionName(alias, newVersions)
                val resolved = ResolvedVersion(
                    alias = versionName,
                    value = version.value,
                    source = VersionSource.DECLARED,
                )
                newVersions[versionName] = resolved
                diagnostics = diagnostics + Diagnostics.info(
                    code = "EXTRACTED_VERSION_CREATED",
                    message = "Extracted version '$versionName' = '${version.value}' from plugin '$alias'",
                    processorId = id,
                )
                plugin.copy(version = resolved)
            } else {
                plugin
            }
        }

        return metadata.copy(
            versions = newVersions,
            libraries = updatedLibraries,
            plugins = updatedPlugins,
            diagnostics = diagnostics,
        )
    }

    private fun generateVersionName(
        alias: String,
        existing: Map<String, ResolvedVersion>,
    ): String {
        val base = "$alias-version"
        if (base !in existing) return base
        var counter = 2
        while ("$base-$counter" in existing) counter++
        return "$base-$counter"
    }
}
