package io.github.zenhelix.dependanger.effective.pipeline

import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.ProcessingInfo
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.Instant
import kotlin.time.TimeSource

public class ProcessingPipeline(
    private val processors: List<EffectiveMetadataProcessor>,
) {
    public suspend fun process(context: ProcessingContext): EffectiveMetadata {
        val initial = EffectiveMetadata(
            schemaVersion = context.originalMetadata.schemaVersion,
            distribution = context.activeDistribution,
            versions = emptyMap(),
            libraries = emptyMap(),
            plugins = emptyMap(),
            bundles = emptyMap(),
            diagnostics = Diagnostics.EMPTY,
            processingInfo = null,
        )

        val groups = groupByExecutionMode(processors.sortedBy { it.order })

        val executedProcessorIds = mutableListOf<String>()

        val result = groups.fold(initial) { acc, group ->
            when (group.executionMode) {
                ExecutionMode.PARALLEL_IO, ExecutionMode.PARALLEL_COMPUTE ->
                    executeParallel(acc, group.processors, context, executedProcessorIds)

                else                                                      ->
                    executeSequential(acc, group.processors, context, executedProcessorIds)
            }
        }

        return result.copy(
            processingInfo = ProcessingInfo(
                processedAt = Instant.now().toString(),
                processorIds = executedProcessorIds.toList(),
                environment = context.environment.toSnapshot(),
            )
        )
    }

    private suspend fun executeSequential(
        metadata: EffectiveMetadata,
        processors: List<EffectiveMetadataProcessor>,
        context: ProcessingContext,
        executedProcessorIds: MutableList<String>,
    ): EffectiveMetadata =
        processors.fold(metadata) { acc, processor ->
            executeProcessor(processor, acc, context, executedProcessorIds)
        }

    private suspend fun executeParallel(
        base: EffectiveMetadata,
        processors: List<EffectiveMetadataProcessor>,
        context: ProcessingContext,
        executedProcessorIds: MutableList<String>,
    ): EffectiveMetadata {
        if (processors.size == 1) {
            return executeSequential(base, processors, context, executedProcessorIds)
        }

        val results = coroutineScope {
            processors.map { processor ->
                async { executeProcessor(processor, base, context, executedProcessorIds) }
            }.awaitAll()
        }

        return mergeParallelResults(base, results)
    }

    private suspend fun executeProcessor(
        processor: EffectiveMetadataProcessor,
        metadata: EffectiveMetadata,
        context: ProcessingContext,
        executedProcessorIds: MutableList<String>,
    ): EffectiveMetadata {
        if (!processor.supports(context)) {
            return metadata
        }

        val callback = context.callback
        callback?.onEvent(ProcessingEvent.PhaseStarted(processor.phase))
        val mark = TimeSource.Monotonic.markNow()
        return try {
            val result = processor.process(metadata, context)
            callback?.onEvent(ProcessingEvent.PhaseCompleted(processor.phase, mark.elapsedNow()))
            executedProcessorIds.add(processor.id)
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
        results.forEach { validateParallelResult(base, it) }

        return results.fold(base) { merged, result ->
            merged.copy(
                diagnostics = merged.diagnostics + collectNewDiagnostics(base.diagnostics, result.diagnostics),
                extensions = merged.extensions + (result.extensions - base.extensions.keys),
            )
        }
    }

    private fun validateParallelResult(base: EffectiveMetadata, result: EffectiveMetadata) {
        if (result.versions != base.versions) {
            throw IllegalStateException(
                "Parallel processor modified 'versions' which is not supported. " +
                        "Only diagnostics and extensions can be modified in parallel execution mode."
            )
        }
        if (result.libraries != base.libraries) {
            throw IllegalStateException(
                "Parallel processor modified 'libraries' which is not supported. " +
                        "Only diagnostics and extensions can be modified in parallel execution mode."
            )
        }
        if (result.plugins != base.plugins) {
            throw IllegalStateException(
                "Parallel processor modified 'plugins' which is not supported. " +
                        "Only diagnostics and extensions can be modified in parallel execution mode."
            )
        }
        if (result.bundles != base.bundles) {
            throw IllegalStateException(
                "Parallel processor modified 'bundles' which is not supported. " +
                        "Only diagnostics and extensions can be modified in parallel execution mode."
            )
        }
    }

    /**
     * Collects diagnostics added by a parallel processor.
     *
     * Contract: parallel processors MUST only append diagnostics.
     * The base lists are guaranteed to be prefixes of result lists
     * because [validateParallelResult] prevents modification of core fields.
     */
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

            val first = sorted.first()
            return sorted.drop(1).fold(listOf(ProcessorGroup(first.phase.executionMode, listOf(first)))) { groups, processor ->
                val mode = processor.phase.executionMode
                val lastGroup = groups.last()
                if (mode == lastGroup.executionMode) {
                    groups.dropLast(1) + lastGroup.copy(processors = lastGroup.processors + processor)
                } else {
                    groups + ProcessorGroup(mode, listOf(processor))
                }
            }
        }
    }
}
