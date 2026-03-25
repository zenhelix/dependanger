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

        val groupResult = groups.fold(GroupResult(initial, emptyList())) { acc, group ->
            val result = when (group.executionMode) {
                ExecutionMode.PARALLEL_IO, ExecutionMode.PARALLEL_COMPUTE ->
                    executeParallel(acc.metadata, group.processors, context)

                else                                                      ->
                    executeSequential(acc.metadata, group.processors, context)
            }
            GroupResult(
                metadata = result.metadata,
                executedProcessorIds = acc.executedProcessorIds + result.executedProcessorIds,
            )
        }

        return groupResult.metadata.copy(
            processingInfo = ProcessingInfo(
                processedAt = Instant.now().toString(),
                processorIds = groupResult.executedProcessorIds,
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
        val result = processors.fold(metadata) { acc, processor ->
            val output = executeProcessor(processor, acc, context)
            if (output.executedId != null) {
                executedIds.add(output.executedId)
            }
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
                async { executeProcessor(processor, base, context) }
            }.awaitAll()
        }

        val executedIds = outputs.mapNotNull { it.executedId }
        val merged = mergeParallelResults(base, outputs.map { it.metadata })
        return GroupResult(merged, executedIds)
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
            emitEvent(context, ProcessingEvent.PhaseCompleted(processor.phase, mark.elapsedNow()))
            ProcessorOutput(result, executedId = processor.id)
        } catch (e: Throwable) {
            emitEvent(context, ProcessingEvent.PhaseError(processor.phase, e))
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
        assertUnchanged("versions", base.versions, result.versions)
        assertUnchanged("libraries", base.libraries, result.libraries)
        assertUnchanged("plugins", base.plugins, result.plugins)
        assertUnchanged("bundles", base.bundles, result.bundles)
    }

    private fun assertUnchanged(fieldName: String, base: Any?, result: Any?) {
        if (result != base) {
            throw IllegalStateException(
                "Parallel processor modified '$fieldName' which is not supported. " +
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

    private data class GroupResult(
        val metadata: EffectiveMetadata,
        val executedProcessorIds: List<String>,
    )

    private data class ProcessorGroup(
        val executionMode: ExecutionMode,
        val processors: List<EffectiveMetadataProcessor>,
    )

    private companion object {
        fun groupByExecutionMode(sorted: List<EffectiveMetadataProcessor>): List<ProcessorGroup> {
            if (sorted.isEmpty()) return emptyList()

            val groups = mutableListOf<ProcessorGroup>()
            for (processor in sorted) {
                val mode = processor.phase.executionMode
                val last = groups.lastOrNull()
                if (last != null && last.executionMode == mode) {
                    groups[groups.lastIndex] = last.copy(processors = last.processors + processor)
                } else {
                    groups.add(ProcessorGroup(mode, listOf(processor)))
                }
            }
            return groups
        }
    }
}
