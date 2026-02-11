package io.github.zenhelix.dependanger.effective.processor

import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.effective.DiagnosticCodes
import io.github.zenhelix.dependanger.effective.ProcessorIds
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.ResolvedVersion
import io.github.zenhelix.dependanger.effective.model.VersionSource
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase

public class ExtractedVersionsProcessor : EffectiveMetadataProcessor {
    override val id: String = ProcessorIds.EXTRACTED_VERSIONS
    override val phase: ProcessingPhase = ProcessingPhase.EXTRACTED_VERSIONS
    override val order: Int = phase.order
    override val isOptional: Boolean = false
    override val description: String = "Extracts inline versions into named version entries"
    override fun supports(context: ProcessingContext): Boolean = true

    private data class ExtractionAccumulator(
        val usedNames: Set<String>,
        val versions: Map<String, ResolvedVersion>,
        val diagnostics: Diagnostics,
        val resolvedByAlias: Map<String, ResolvedVersion>,
    )

    override suspend fun process(
        metadata: EffectiveMetadata,
        context: ProcessingContext,
    ): EffectiveMetadata {
        val initialAcc = ExtractionAccumulator(
            usedNames = metadata.versions.keys,
            versions = emptyMap(),
            diagnostics = Diagnostics(errors = emptyList(), warnings = emptyList(), infos = emptyList()),
            resolvedByAlias = emptyMap(),
        )

        val afterLibraries = metadata.libraries.entries.fold(initialAcc) { acc, (alias, lib) ->
            extractVersion(acc, alias, lib.version, "library")
        }

        val afterAll = metadata.plugins.entries.fold(afterLibraries) { acc, (alias, plugin) ->
            extractVersion(acc, alias, plugin.version, "plugin")
        }

        val updatedLibraries = metadata.libraries.mapValues { (alias, lib) ->
            val resolved = afterAll.resolvedByAlias[alias]
            if (resolved != null) lib.copy(version = resolved) else lib
        }

        val updatedPlugins = metadata.plugins.mapValues { (alias, plugin) ->
            val resolved = afterAll.resolvedByAlias[alias]
            if (resolved != null) plugin.copy(version = resolved) else plugin
        }

        return metadata.copy(
            versions = metadata.versions + afterAll.versions,
            libraries = updatedLibraries,
            plugins = updatedPlugins,
            diagnostics = metadata.diagnostics + afterAll.diagnostics,
        )
    }

    private fun extractVersion(
        acc: ExtractionAccumulator,
        alias: String,
        version: ResolvedVersion?,
        sourceType: String,
    ): ExtractionAccumulator {
        if (version == null || version.alias.isNotEmpty() || version.value.isEmpty()) return acc

        val versionName = generateVersionName(alias, acc.usedNames)
        val resolved = ResolvedVersion(
            alias = versionName,
            value = version.value,
            source = VersionSource.DECLARED,
            originalRef = null,
        )
        val diagnostic = Diagnostics.info(
            code = DiagnosticCodes.Version.EXTRACTED_CREATED,
            message = "Extracted version '$versionName' = '${version.value}' from $sourceType '$alias'",
            processorId = id,
            context = emptyMap(),
        )

        return ExtractionAccumulator(
            usedNames = acc.usedNames + versionName,
            versions = acc.versions + (versionName to resolved),
            diagnostics = acc.diagnostics + diagnostic,
            resolvedByAlias = acc.resolvedByAlias + (alias to resolved),
        )
    }

    private fun generateVersionName(
        alias: String,
        existingNames: Set<String>,
    ): String {
        val base = "$alias-version"
        if (base !in existingNames) return base
        var counter = 2
        while ("$base-$counter" in existingNames) counter++
        return "$base-$counter"
    }
}
