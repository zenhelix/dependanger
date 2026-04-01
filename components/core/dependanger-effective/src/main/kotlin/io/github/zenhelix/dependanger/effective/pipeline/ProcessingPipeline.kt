package io.github.zenhelix.dependanger.effective.pipeline

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.ProcessingInfo
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.Instant
import kotlin.time.TimeSource

private val logger = KotlinLogging.logger {}

public class ProcessingPipeline(
    processors: List<EffectiveMetadataProcessor>,
) {
    private val sortedProcessors: List<EffectiveMetadataProcessor> = ProcessorOrderResolver.resolve(processors)
    private val processorGroups: List<ProcessorGroup> = ProcessorOrderResolver.groupByExecutionMode(sortedProcessors)
    private val registeredIds: List<String> = sortedProcessors.map { it.id }

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

        var currentMetadata = initial
        val allExecutedIds = mutableListOf<String>()

        for (group in processorGroups) {
            val result = when (group.executionMode) {
                ExecutionMode.PARALLEL_IO, ExecutionMode.PARALLEL_COMPUTE ->
                    executeParallel(currentMetadata, group.processors, context)

                else                                                      ->
                    executeSequential(currentMetadata, group.processors, context)
            }
            currentMetadata = result.metadata
            allExecutedIds.addAll(result.executedProcessorIds)
        }

        val skippedIds = registeredIds - allExecutedIds.toSet()

        return currentMetadata.copy(
            processingInfo = ProcessingInfo(
                processedAt = Instant.now().toString(),
                processorIds = allExecutedIds,
                registeredProcessorIds = registeredIds,
                skippedProcessorIds = skippedIds,
                environment = context.environment.toSnapshot(),
            )
        )
    }

    private suspend fun executeSequential(
        metadata: EffectiveMetadata,
        processors: List<EffectiveMetadataProcessor>,
        context: ProcessingContext,
    ): GroupResult {
        val executedIds = mutableListOf<String>()
        val result = processors.foldIndexed(metadata) { index, acc, processor ->
            val output = executeProcessor(processor, acc, context)
            if (output.executedId != null) {
                executedIds.add(output.executedId)
            }
            emitEvent(context, ProcessingEvent.Progress(processor.phase, index + 1, processors.size))
            output.metadata
        }
        return GroupResult(result, executedIds)
    }

    private suspend fun executeParallel(
        base: EffectiveMetadata,
        processors: List<EffectiveMetadataProcessor>,
        context: ProcessingContext,
    ): GroupResult {
        if (processors.size == 1) {
            return executeSequential(base, processors, context)
        }

        val outputs = coroutineScope {
            processors.map { processor ->
                async { executeParallelProcessor(processor, base, context) }
            }.awaitAll()
        }

        val executedIds = outputs.mapNotNull { it.executedId }
        val merged = mergeParallelResults(base, outputs.map { it.result })
        return GroupResult(merged, executedIds)
    }

    private suspend fun executeParallelProcessor(
        processor: EffectiveMetadataProcessor,
        metadata: EffectiveMetadata,
        context: ProcessingContext,
    ): ParallelProcessorOutput {
        if (!processor.supports(context)) {
            return ParallelProcessorOutput(ParallelResult.EMPTY, executedId = null)
        }

        emitEvent(context, ProcessingEvent.PhaseStarted(processor.phase))
        val mark = TimeSource.Monotonic.markNow()
        return try {
            // ParallelMetadataProcessor check is enforced at build time by PipelineBuilder.validateParallelProcessors
            val result = (processor as ParallelMetadataProcessor).processParallel(metadata, context)
            emitNewDiagnostics(context, Diagnostics.EMPTY, result.diagnostics)
            emitEvent(context, ProcessingEvent.PhaseCompleted(processor.phase, mark.elapsedNow()))
            ParallelProcessorOutput(result, executedId = processor.id)
        } catch (e: Throwable) {
            emitEvent(context, ProcessingEvent.PhaseError(processor.phase, e))
            throw e
        }
    }

    private suspend fun executeProcessor(
        processor: EffectiveMetadataProcessor,
        metadata: EffectiveMetadata,
        context: ProcessingContext,
    ): ProcessorOutput {
        if (!processor.supports(context)) {
            return ProcessorOutput(metadata, executedId = null)
        }

        emitEvent(context, ProcessingEvent.PhaseStarted(processor.phase))
        val mark = TimeSource.Monotonic.markNow()
        return try {
            val result = processor.process(metadata, context)
            emitNewDiagnostics(context, metadata.diagnostics, result.diagnostics)
            emitEvent(context, ProcessingEvent.PhaseCompleted(processor.phase, mark.elapsedNow()))
            ProcessorOutput(result, executedId = processor.id)
        } catch (e: Throwable) {
            emitEvent(context, ProcessingEvent.PhaseError(processor.phase, e))
            throw e
        }
    }

    private fun mergeParallelResults(
        base: EffectiveMetadata,
        results: List<ParallelResult>,
    ): EffectiveMetadata =
        results.fold(base) { merged, result ->
            merged.copy(
                diagnostics = merged.diagnostics + result.diagnostics,
                extensions = merged.extensions + result.extensions,
            )
        }

    /**
     * Collects diagnostics added since [base] by computing the diff.
     *
     * Contract: processors append diagnostics via `metadata.diagnostics + newDiags`,
     * so base lists are prefixes of result lists.
     */
    private fun collectNewDiagnostics(base: Diagnostics, result: Diagnostics): Diagnostics = Diagnostics(
        errors = result.errors.drop(base.errors.size),
        warnings = result.warnings.drop(base.warnings.size),
        infos = result.infos.drop(base.infos.size),
    )

    private fun emitNewDiagnostics(context: ProcessingContext, before: Diagnostics, after: Diagnostics) {
        if (context.callback == null) return
        if (before.errors.size == after.errors.size
            && before.warnings.size == after.warnings.size
            && before.infos.size == after.infos.size
        ) return

        val newMessages = collectNewDiagnostics(before, after)
        for (message in newMessages.errors) emitEvent(context, ProcessingEvent.DiagnosticAdded(message))
        for (message in newMessages.warnings) emitEvent(context, ProcessingEvent.DiagnosticAdded(message))
        for (message in newMessages.infos) emitEvent(context, ProcessingEvent.DiagnosticAdded(message))
    }

    private fun emitEvent(context: ProcessingContext, event: ProcessingEvent) {
        try {
            context.callback?.onEvent(event)
        } catch (e: Exception) {
            logger.warn(e) { "Processing callback failed on event $event" }
        }
    }

    private data class ProcessorOutput(
        val metadata: EffectiveMetadata,
        val executedId: String?,
    )

    private data class ParallelProcessorOutput(
        val result: ParallelResult,
        val executedId: String?,
    )

    private data class GroupResult(
        val metadata: EffectiveMetadata,
        val executedProcessorIds: List<String>,
    )
}
