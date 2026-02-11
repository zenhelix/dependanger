package io.github.zenhelix.dependanger.effective.processor

import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
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
        val usedVersionNames = mutableSetOf<String>()

        for ((_, lib) in metadata.libraries) {
            lib.version?.let { v ->
                if (v.alias.isNotEmpty()) usedVersionNames.add(v.alias)
                v.originalRef?.let { usedVersionNames.add(it) }
            }
        }

        for ((_, plugin) in metadata.plugins) {
            plugin.version?.let { v ->
                if (v.alias.isNotEmpty()) usedVersionNames.add(v.alias)
                v.originalRef?.let { usedVersionNames.add(it) }
            }
        }

        var diagnostics = metadata.diagnostics

        val usedVersions = metadata.versions.filter { (name, _) ->
            val used = name in usedVersionNames
            if (!used) {
                diagnostics = diagnostics + Diagnostics.info(
                    code = "UNUSED_VERSION_REMOVED",
                    message = "Version '$name' removed: not referenced by any library or plugin",
                    processorId = id,
                    context = emptyMap(),
                )
            }
            used
        }

        return metadata.copy(versions = usedVersions, diagnostics = diagnostics)
    }
}
