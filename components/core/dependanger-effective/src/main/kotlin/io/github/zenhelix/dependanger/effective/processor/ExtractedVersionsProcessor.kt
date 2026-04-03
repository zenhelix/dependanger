package io.github.zenhelix.dependanger.effective.processor

import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.effective.DiagnosticCodes
import io.github.zenhelix.dependanger.effective.ProcessorIds
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.EffectiveVersion
import io.github.zenhelix.dependanger.effective.model.ResolvedVersion
import io.github.zenhelix.dependanger.effective.model.VersionSource
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.OrderConstraint
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase

public class ExtractedVersionsProcessor : EffectiveMetadataProcessor {
    override val id: String = ProcessorIds.EXTRACTED_VERSIONS
    override val phase: ProcessingPhase = ProcessingPhase.EXTRACTED_VERSIONS
    override val constraints: Set<OrderConstraint> = setOf(OrderConstraint.runsAfter(ProcessorIds.METADATA_CONVERSION))
    override val isOptional: Boolean = false
    override val description: String = "Extracts inline versions into named version entries"
    override fun supports(context: ProcessingContext): Boolean = true

    private data class ExtractionAccumulator(
        val usedNames: Set<String>,
        val versions: Map<String, ResolvedVersion>,
        val diagnostics: Diagnostics,
        val resolvedByAlias: Map<String, EffectiveVersion.Resolved>,
        val allVersionValues: Map<String, String>,
    )

    override suspend fun process(
        metadata: EffectiveMetadata,
        context: ProcessingContext,
    ): EffectiveMetadata {
        val initialAcc = ExtractionAccumulator(
            usedNames = metadata.versions.keys,
            versions = emptyMap(),
            diagnostics = Diagnostics.EMPTY,
            resolvedByAlias = emptyMap(),
            allVersionValues = metadata.versions.mapValues { (_, v) -> v.value },
        )

        val afterLibraries = metadata.libraries.entries.fold(initialAcc) { acc, (alias, lib) ->
            extractVersion(acc, alias, lib.version)
        }

        val afterAll = metadata.plugins.entries.fold(afterLibraries) { acc, (alias, plugin) ->
            extractVersion(acc, alias, plugin.version)
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

    private data class NameResult(val name: String, val baseName: String, val wasCollision: Boolean)

    private fun extractVersion(
        acc: ExtractionAccumulator,
        alias: String,
        version: EffectiveVersion,
    ): ExtractionAccumulator {
        val resolved = version.resolvedOrNull ?: return acc
        if (resolved.alias.isNotEmpty() || resolved.value.isEmpty()) return acc

        val nameResult = generateVersionName(alias, acc.usedNames)
        val newResolved = ResolvedVersion(
            alias = nameResult.name,
            value = resolved.value,
            source = VersionSource.DECLARED,
            originalRef = null,
        )

        val conflictDiagnostic = if (nameResult.wasCollision) {
            val existingValue = acc.allVersionValues[nameResult.baseName]
            if (existingValue != null && existingValue != resolved.value) {
                Diagnostics.warning(
                    code = DiagnosticCodes.Version.EXTRACTED_VERSION_CONFLICT,
                    message = "Extracted version name '${nameResult.baseName}' conflicts with existing version (existing: $existingValue, new: ${resolved.value}), using '${nameResult.name}'",
                    processorId = id,
                    context = emptyMap(),
                )
            } else {
                Diagnostics.EMPTY
            }
        } else {
            Diagnostics.EMPTY
        }

        val infoDiagnostic = Diagnostics.info(
            code = DiagnosticCodes.Version.EXTRACTED_CREATED,
            message = "Extracted version '${nameResult.name}' = '${resolved.value}' from '$alias'",
            processorId = id,
            context = emptyMap(),
        )

        return ExtractionAccumulator(
            usedNames = acc.usedNames + nameResult.name,
            versions = acc.versions + (nameResult.name to newResolved),
            diagnostics = acc.diagnostics + conflictDiagnostic + infoDiagnostic,
            resolvedByAlias = acc.resolvedByAlias + (alias to EffectiveVersion.Resolved(newResolved)),
            allVersionValues = acc.allVersionValues + (nameResult.name to resolved.value),
        )
    }

    private fun generateVersionName(
        alias: String,
        existingNames: Set<String>,
    ): NameResult {
        val base = "$alias-version"
        if (base !in existingNames) return NameResult(name = base, baseName = base, wasCollision = false)
        var counter = 2
        while ("$base-$counter" in existingNames) counter++
        return NameResult(name = "$base-$counter", baseName = base, wasCollision = true)
    }
}
