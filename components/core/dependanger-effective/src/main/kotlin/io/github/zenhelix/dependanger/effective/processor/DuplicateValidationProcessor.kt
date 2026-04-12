package io.github.zenhelix.dependanger.effective.processor

import io.github.zenhelix.dependanger.core.model.DiagnosticMessage
import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.core.model.Severity
import io.github.zenhelix.dependanger.effective.DiagnosticCodes
import io.github.zenhelix.dependanger.effective.ProcessorIds
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.OrderConstraint
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase

internal class DuplicateValidationProcessor : EffectiveMetadataProcessor {
    override val id: String = ProcessorIds.VALIDATION_DUPLICATES
    override val phase: ProcessingPhase = ProcessingPhase.VALIDATION
    override val constraints: Set<OrderConstraint> = setOf(OrderConstraint.runsAfter(ProcessorIds.VERSION_RESOLVER))
    override val isOptional: Boolean = false
    override val description: String = "Validates no duplicate aliases or invalid coordinates"
    override fun supports(context: ProcessingContext): Boolean = true

    override suspend fun process(
        metadata: EffectiveMetadata,
        context: ProcessingContext,
    ): EffectiveMetadata {
        val diagnostics = Diagnostics.builder(metadata.diagnostics)
        diagnostics.add(validateDuplicateAliases(metadata))
        diagnostics.add(validateCoordinates(metadata))
        return metadata.copy(diagnostics = diagnostics.build())
    }

    private fun validateDuplicateAliases(metadata: EffectiveMetadata): Diagnostics {
        val allAliases = buildList {
            metadata.libraries.keys.forEach { add(it to "library") }
            metadata.plugins.keys.forEach { add(it to "plugin") }
            metadata.bundles.keys.forEach { add(it to "bundle") }
        }.groupBy({ it.first }, { it.second })

        val messages = allAliases
            .filter { it.value.size > 1 }
            .map { (alias, types) ->
                DiagnosticMessage(
                    code = DiagnosticCodes.Validation.DUPLICATE_ALIAS,
                    message = "Alias '$alias' used in multiple namespaces: ${types.joinToString()}",
                    severity = Severity.ERROR,
                    processorId = id,
                    context = emptyMap(),
                )
            }
        return Diagnostics(errors = messages, warnings = emptyList(), infos = emptyList())
    }

    private fun validateCoordinates(metadata: EffectiveMetadata): Diagnostics {
        val issues = metadata.libraries.flatMap { (alias, lib) ->
            buildList {
                if (lib.coordinate.group.isBlank() || lib.coordinate.artifact.isBlank()) {
                    add(
                        DiagnosticMessage(
                            code = DiagnosticCodes.Validation.INVALID_COORDINATES,
                            message = "Library '$alias': empty group or artifact (group='${lib.coordinate.group}', artifact='${lib.coordinate.artifact}')",
                            severity = Severity.ERROR,
                            processorId = id,
                            context = emptyMap(),
                        )
                    )
                }
                if (":" in lib.coordinate.group || ":" in lib.coordinate.artifact) {
                    add(
                        DiagnosticMessage(
                            code = DiagnosticCodes.Validation.INVALID_COORDINATES,
                            message = "Library '$alias': group or artifact contains ':' (group='${lib.coordinate.group}', artifact='${lib.coordinate.artifact}')",
                            severity = Severity.ERROR,
                            processorId = id,
                            context = emptyMap(),
                        )
                    )
                }
            }
        }
        return Diagnostics.of(*issues.toTypedArray())
    }
}
