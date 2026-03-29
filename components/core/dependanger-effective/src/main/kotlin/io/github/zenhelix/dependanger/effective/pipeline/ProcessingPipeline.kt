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
    private val sortedProcessors: List<EffectiveMetadataProcessor> = topologicalSort(processors)

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

        val groups = groupByExecutionMode(sortedProcessors)

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
            val result = when (processor) {
                is ParallelMetadataProcessor -> processor.processParallel(metadata, context)
                else -> {
                    // Fallback for legacy processors that don't implement ParallelMetadataProcessor.
                    // Validate that they didn't modify core fields.
                    val fullResult = processor.process(metadata, context)
                    validateParallelResult(metadata, fullResult)
                    ParallelResult(
                        diagnostics = collectNewDiagnostics(metadata.diagnostics, fullResult.diagnostics),
                        extensions = fullResult.extensions - metadata.extensions.keys,
                    )
                }
            }
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

    private fun emitNewDiagnostics(context: ProcessingContext, before: Diagnostics, after: Diagnostics) {
        if (context.callback == null) return
        if (before.errors.size == after.errors.size
            && before.warnings.size == after.warnings.size
            && before.infos.size == after.infos.size
        ) return

        val newMessages = collectNewDiagnostics(before, after)
        for (message in newMessages.errors + newMessages.warnings + newMessages.infos) {
            emitEvent(context, ProcessingEvent.DiagnosticAdded(message))
        }
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

    private data class ProcessorGroup(
        val executionMode: ExecutionMode,
        val processors: List<EffectiveMetadataProcessor>,
    )

    private companion object {
        fun topologicalSort(processors: List<EffectiveMetadataProcessor>): List<EffectiveMetadataProcessor> {
            val byId = processors.associateBy { it.id }

            // Build adjacency: edges[A] contains B means A must run before B
            val edges = mutableMapOf<String, MutableSet<String>>()
            val inDegree = mutableMapOf<String, Int>()
            for (p in processors) {
                edges.getOrPut(p.id) { mutableSetOf() }
                inDegree.getOrPut(p.id) { 0 }
            }

            for (p in processors) {
                for (c in p.constraints) {
                    when (c) {
                        is OrderConstraint.RunsAfter -> {
                            // p runs after c.processorId -> edge from c.processorId to p.id
                            if (c.processorId in byId) {
                                edges.getOrPut(c.processorId) { mutableSetOf() }.add(p.id)
                                inDegree[p.id] = (inDegree[p.id] ?: 0) + 1
                            }
                        }
                        is OrderConstraint.RunsBefore -> {
                            // p runs before c.processorId -> edge from p.id to c.processorId
                            if (c.processorId in byId) {
                                edges.getOrPut(p.id) { mutableSetOf() }.add(c.processorId)
                                inDegree[c.processorId] = (inDegree[c.processorId] ?: 0) + 1
                            }
                        }
                    }
                }
            }

            // Kahn's algorithm with deterministic tie-breaking (alphabetical by id)
            val queue = java.util.TreeSet<String>() // sorted set for determinism
            for ((id, deg) in inDegree) {
                if (deg == 0) queue.add(id)
            }

            val result = mutableListOf<EffectiveMetadataProcessor>()
            while (queue.isNotEmpty()) {
                val current = queue.pollFirst()!!
                result.add(byId[current]!!)
                for (neighbor in edges[current].orEmpty()) {
                    val newDeg = (inDegree[neighbor] ?: 1) - 1
                    inDegree[neighbor] = newDeg
                    if (newDeg == 0) queue.add(neighbor)
                }
            }

            if (result.size != processors.size) {
                val remaining = processors.map { it.id }.toSet() - result.map { it.id }.toSet()
                throw PipelineConfigurationException(
                    "Circular dependency detected among processors: ${remaining.sorted().joinToString(", ")}"
                )
            }

            return result
        }

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
