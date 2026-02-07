package io.github.zenhelix.dependanger.effective.pipeline

import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.time.TimeSource

public class ProcessingPipeline(
    private val processors: List<EffectiveMetadataProcessor>,
) {
    public suspend fun process(context: ProcessingContext): EffectiveMetadata {
        var result = EffectiveMetadata(
            schemaVersion = context.originalMetadata.schemaVersion,
            distribution = context.activeDistribution,
        )

        val groups = groupByExecutionMode(processors.sortedBy { it.order })

        for (group in groups) {
            result = when (group.executionMode) {
                ExecutionMode.PARALLEL_IO, ExecutionMode.PARALLEL_COMPUTE ->
                    executeParallel(result, group.processors, context)

                else                                                      ->
                    executeSequential(result, group.processors, context)
            }
        }

        return result
    }

    private suspend fun executeSequential(
        metadata: EffectiveMetadata,
        processors: List<EffectiveMetadataProcessor>,
        context: ProcessingContext,
    ): EffectiveMetadata {
        var result = metadata
        for (processor in processors) {
            result = executeProcessor(processor, result, context)
        }
        return result
    }

    private suspend fun executeParallel(
        base: EffectiveMetadata,
        processors: List<EffectiveMetadataProcessor>,
        context: ProcessingContext,
    ): EffectiveMetadata {
        if (processors.size == 1) {
            return executeSequential(base, processors, context)
        }

        val results = coroutineScope {
            processors.map { processor ->
                async { executeProcessor(processor, base, context) }
            }.awaitAll()
        }

        return mergeParallelResults(base, results)
    }

    private suspend fun executeProcessor(
        processor: EffectiveMetadataProcessor,
        metadata: EffectiveMetadata,
        context: ProcessingContext,
    ): EffectiveMetadata {
        val callback = context.callback
        callback?.onEvent(ProcessingEvent.PhaseStarted(processor.phase))
        val mark = TimeSource.Monotonic.markNow()
        return try {
            val result = processor.process(metadata, context)
            callback?.onEvent(ProcessingEvent.PhaseCompleted(processor.phase, mark.elapsedNow()))
            result
        } catch (e: Throwable) {
            callback?.onEvent(ProcessingEvent.PhaseError(processor.phase, e))
            throw e
        }
    }

    private fun mergeParallelResults(
        base: EffectiveMetadata,
        results: List<EffectiveMetadata>,
    ): EffectiveMetadata {
        var merged = base
        for (result in results) {
            merged = merged.copy(
                diagnostics = merged.diagnostics + collectNewDiagnostics(base.diagnostics, result.diagnostics),
                updates = merged.updates + result.updates.drop(base.updates.size),
                vulnerabilities = merged.vulnerabilities + result.vulnerabilities.drop(base.vulnerabilities.size),
                licenseViolations = merged.licenseViolations + result.licenseViolations.drop(base.licenseViolations.size),
                compatibilityIssues = merged.compatibilityIssues + result.compatibilityIssues.drop(base.compatibilityIssues.size),
                transitives = merged.transitives + result.transitives.drop(base.transitives.size),
                flatDependencies = merged.flatDependencies + result.flatDependencies.drop(base.flatDependencies.size),
                versionConflicts = merged.versionConflicts + result.versionConflicts.drop(base.versionConflicts.size),
                extensions = merged.extensions + (result.extensions - base.extensions.keys),
            )
        }
        return merged
    }

    private fun collectNewDiagnostics(base: Diagnostics, result: Diagnostics): Diagnostics = Diagnostics(
        errors = result.errors.drop(base.errors.size),
        warnings = result.warnings.drop(base.warnings.size),
        infos = result.infos.drop(base.infos.size),
    )

    private data class ProcessorGroup(
        val executionMode: ExecutionMode,
        val processors: List<EffectiveMetadataProcessor>,
    )

    private companion object {
        fun groupByExecutionMode(sorted: List<EffectiveMetadataProcessor>): List<ProcessorGroup> {
            if (sorted.isEmpty()) return emptyList()

            val groups = mutableListOf<ProcessorGroup>()
            var currentMode = sorted.first().phase.executionMode
            var currentProcessors = mutableListOf(sorted.first())

            for (processor in sorted.drop(1)) {
                val mode = processor.phase.executionMode
                if (mode == currentMode) {
                    currentProcessors.add(processor)
                } else {
                    groups.add(ProcessorGroup(currentMode, currentProcessors.toList()))
                    currentMode = mode
                    currentProcessors = mutableListOf(processor)
                }
            }
            groups.add(ProcessorGroup(currentMode, currentProcessors.toList()))

            return groups
        }
    }
}
