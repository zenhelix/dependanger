package io.github.zenhelix.dependanger.effective.processor

import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.ResolvedVersion
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase

public class UsedVersionsProcessor : EffectiveMetadataProcessor {
    override val id: String = "used-versions"
    override val phase: ProcessingPhase = ProcessingPhase.USED_VERSIONS
    override val order: Int = phase.order
    override val isOptional: Boolean = false
    override val description: String = "Removes unused version entries"
    override fun supports(context: ProcessingContext): Boolean = true

    override suspend fun process(
        metadata: EffectiveMetadata,
        context: ProcessingContext,
    ): EffectiveMetadata {
        val usedVersionNames = buildSet {
            metadata.libraries.values.forEach { lib ->
                lib.version?.let { v ->
                    if (v.alias.isNotEmpty()) add(v.alias)
                    v.originalRef?.let { add(it) }
                }
            }
            metadata.plugins.values.forEach { plugin ->
                plugin.version?.let { v ->
                    if (v.alias.isNotEmpty()) add(v.alias)
                    v.originalRef?.let { add(it) }
                }
            }
        }

        val (usedVersions, removedDiagnostics) = metadata.versions.entries.fold(
            emptyMap<String, ResolvedVersion>() to metadata.diagnostics
        ) { (accVersions, accDiag), (name, version) ->
            if (name in usedVersionNames) {
                (accVersions + (name to version)) to accDiag
            } else {
                accVersions to (accDiag + Diagnostics.info(
                    code = "UNUSED_VERSION_REMOVED",
                    message = "Version '$name' removed: not referenced by any library or plugin",
                    processorId = id,
                    context = emptyMap(),
                ))
            }
        }

        return metadata.copy(versions = usedVersions, diagnostics = removedDiagnostics)
    }
}
