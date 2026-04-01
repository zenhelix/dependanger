package io.github.zenhelix.dependanger.effective.pipeline

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.ProcessingInfo
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPipeline.Companion.groupByExecutionMode
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.Instant
import kotlin.time.TimeSource

private val logger = KotlinLogging.logger {}

public class ProcessingPipeline(
    processors: List<EffectiveMetadataProcessor>,
) {
    private val sortedProcessors: List<EffectiveMetadataProcessor> = run {
        val edges = buildDependencyGraph(processors)
        val sorted = topologicalSort(processors, edges)
        consolidateParallelProcessors(sorted, edges)
    }
    private val processorGroups: List<ProcessorGroup> = groupByExecutionMode(sortedProcessors)
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

        val groups = processorGroups

        var currentMetadata = initial
        val allExecutedIds = mutableListOf<String>()

        for (group in groups) {
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
     * Used by [emitNewDiagnostics] to emit events for newly added diagnostics.
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

    private data class ProcessorGroup(
        val executionMode: ExecutionMode,
        val processors: List<EffectiveMetadataProcessor>,
    )

    private companion object {

        /**
         * Builds the dependency graph from processor constraints.
         * Returns adjacency map: edges[A] contains B means A must run before B.
         */
        fun buildDependencyGraph(
            processors: List<EffectiveMetadataProcessor>,
        ): Map<String, Set<String>> {
            val byId = processors.map { it.id }.toSet()
            val edges = mutableMapOf<String, MutableSet<String>>()
            for (p in processors) {
                edges.getOrPut(p.id) { mutableSetOf() }
            }

            for (p in processors) {
                for (c in p.constraints) {
                    when (c) {
                        is OrderConstraint.RunsAfter -> {
                            if (c.processorId in byId) {
                                edges.getOrPut(c.processorId) { mutableSetOf() }.add(p.id)
                            }
                        }
                        is OrderConstraint.RunsBefore -> {
                            if (c.processorId in byId) {
                                edges.getOrPut(p.id) { mutableSetOf() }.add(c.processorId)
                            }
                        }
                    }
                }
            }
            return edges
        }

        fun topologicalSort(
            processors: List<EffectiveMetadataProcessor>,
            edges: Map<String, Set<String>>,
        ): List<EffectiveMetadataProcessor> {
            val byId = processors.associateBy { it.id }
            val inDegree = mutableMapOf<String, Int>()
            for (p in processors) {
                inDegree[p.id] = 0
            }
            for ((_, neighbors) in edges) {
                for (neighbor in neighbors) {
                    if (neighbor in inDegree) {
                        inDegree[neighbor] = (inDegree[neighbor] ?: 0) + 1
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

        /**
         * Consolidates non-adjacent parallel processors into contiguous blocks
         * so that [groupByExecutionMode] can form proper parallel groups.
         *
         * For each parallel [ExecutionMode], all processors with that mode are
         * moved to the position of the last one in topological order — provided
         * no intervening sequential processor transitively depends on any of
         * the parallel processors being moved. When consolidation is unsafe,
         * the original order is preserved and a warning is logged.
         */
        fun consolidateParallelProcessors(
            sorted: List<EffectiveMetadataProcessor>,
            edges: Map<String, Set<String>>,
        ): List<EffectiveMetadataProcessor> {
            val parallelByMode = sorted
                .filter { it.phase.executionMode != ExecutionMode.SEQUENTIAL }
                .groupBy { it.phase.executionMode }

            // Nothing to consolidate if every parallel mode has at most 1 processor
            if (parallelByMode.values.none { it.size > 1 }) return sorted

            // Compute reachability only from parallel processors (the only sources we check)
            val allParallelIds = parallelByMode.values.flatten().map { it.id }
            val reachableFrom = computeReachability(edges, allParallelIds)

            val result = sorted.toMutableList()

            for ((mode, parallelProcessors) in parallelByMode) {
                if (parallelProcessors.size <= 1) continue

                val parallelIds = parallelProcessors.map { it.id }.toSet()
                val idToIndex = result.withIndex().associate { (i, p) -> p.id to i }
                val indices = parallelProcessors.map { idToIndex[it.id]!! }.sorted()
                val firstIdx = indices.first()
                val lastIdx = indices.last()

                // Already adjacent — nothing to do
                if (lastIdx - firstIdx + 1 == parallelProcessors.size) continue

                // Check safety: no sequential processor between first and last parallel
                // is downstream of any parallel processor (i.e., parallel processor can reach it)
                val canMerge = (firstIdx..lastIdx).all { idx ->
                    val proc = result[idx]
                    if (proc.id in parallelIds) {
                        true
                    } else {
                        parallelIds.none { parId -> proc.id in (reachableFrom[parId] ?: emptySet()) }
                    }
                }

                if (canMerge) {
                    val lastProcId = result[lastIdx].id
                    val toMove = parallelProcessors.filter { it.id != lastProcId }
                    result.removeAll(toMove.toSet())
                    val insertPos = result.indexOfFirst { it.id == lastProcId }
                    result.addAll(insertPos, toMove)
                } else {
                    logger.warn {
                        "Parallel processors with mode $mode are split into multiple groups " +
                                "due to ordering constraints: ${parallelIds.sorted().joinToString(", ")}"
                    }
                }
            }

            return result
        }

        /**
         * Computes transitive reachability for each source node via BFS.
         * Returns map: nodeId -> set of all transitively reachable node IDs.
         */
        private fun computeReachability(
            edges: Map<String, Set<String>>,
            sourceIds: List<String>,
        ): Map<String, Set<String>> = buildMap {
            for (startId in sourceIds) {
                val visited = mutableSetOf<String>()
                val queue = ArrayDeque<String>()
                for (neighbor in edges[startId].orEmpty()) {
                    queue.add(neighbor)
                }
                while (queue.isNotEmpty()) {
                    val current = queue.removeFirst()
                    if (visited.add(current)) {
                        for (neighbor in edges[current].orEmpty()) {
                            queue.add(neighbor)
                        }
                    }
                }
                put(startId, visited)
            }
        }

        fun groupByExecutionMode(sorted: List<EffectiveMetadataProcessor>): List<ProcessorGroup> {
            if (sorted.isEmpty()) return emptyList()

            val groups = mutableListOf<ProcessorGroup>()
            var currentMode: ExecutionMode? = null
            var currentProcessors = mutableListOf<EffectiveMetadataProcessor>()

            for (processor in sorted) {
                val mode = processor.phase.executionMode
                if (mode != currentMode) {
                    if (currentProcessors.isNotEmpty()) {
                        groups.add(ProcessorGroup(currentMode!!, currentProcessors))
                    }
                    currentMode = mode
                    currentProcessors = mutableListOf()
                }
                currentProcessors.add(processor)
            }
            if (currentProcessors.isNotEmpty()) {
                groups.add(ProcessorGroup(currentMode!!, currentProcessors))
            }
            return groups
        }
    }
}
