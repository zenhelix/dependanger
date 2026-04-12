package io.github.zenhelix.dependanger.effective.processor

import io.github.zenhelix.dependanger.core.model.DiagnosticMessage
import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.core.model.Severity
import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata
import io.github.zenhelix.dependanger.effective.DiagnosticCodes
import io.github.zenhelix.dependanger.effective.ProcessorIds
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.OrderConstraint
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase

internal class CircularDependencyValidator : EffectiveMetadataProcessor {
    override val id: String = ProcessorIds.VALIDATION_CIRCULAR
    override val phase: ProcessingPhase = ProcessingPhase.VALIDATION
    override val constraints: Set<OrderConstraint> = setOf(
        OrderConstraint.runsAfter(ProcessorIds.VERSION_RESOLVER),
        OrderConstraint.runsAfter(ProcessorIds.VALIDATION_DUPLICATES),
    )
    override val isOptional: Boolean = false
    override val description: String = "Detects circular bundle extends dependencies"
    override fun supports(context: ProcessingContext): Boolean = true

    override suspend fun process(
        metadata: EffectiveMetadata,
        context: ProcessingContext,
    ): EffectiveMetadata {
        val diagnostics = Diagnostics.builder(metadata.diagnostics)
        diagnostics.add(validateCircularExtends(context.originalMetadata))
        return metadata.copy(diagnostics = diagnostics.build())
    }

    private fun validateCircularExtends(metadata: DependangerMetadata): Diagnostics {
        val bundleIndex = metadata.bundles.associateBy { it.alias }
        val verified = mutableSetOf<String>()
        val inStack = mutableSetOf<String>()
        val cycleNodes = mutableSetOf<String>()

        fun dfs(alias: String) {
            if (alias in verified) return
            if (alias in inStack) {
                cycleNodes.add(alias)
                return
            }
            inStack.add(alias)
            bundleIndex[alias]?.extends?.forEach { dfs(it) }
            inStack.remove(alias)
            verified.add(alias)
        }

        bundleIndex.keys.forEach { dfs(it) }

        val messages = cycleNodes.map { alias ->
            DiagnosticMessage(
                code = DiagnosticCodes.Validation.CIRCULAR_EXTENDS,
                message = "Bundle '$alias' has circular extends dependency",
                severity = Severity.ERROR,
                processorId = id,
                context = emptyMap(),
            )
        }
        return Diagnostics(errors = messages, warnings = emptyList(), infos = emptyList())
    }
}
