package io.github.zenhelix.dependanger.effective.pipeline

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.zenhelix.dependanger.effective.pipeline.ProcessorOrderResolver.groupByExecutionMode

private val logger = KotlinLogging.logger {}

/**
 * Resolves processor execution order from dependency constraints.
 *
 * Performs topological sort (Kahn's algorithm) with deterministic tie-breaking,
 * then consolidates parallel processors into contiguous groups when safe.
 */
internal object ProcessorOrderResolver {

    /**
     * Resolves the final execution order for processors:
     * 1. Builds dependency graph from [OrderConstraint]s
     * 2. Topologically sorts with alphabetical tie-breaking
     * 3. Consolidates parallel processors into contiguous blocks
     */
    fun resolve(processors: List<EffectiveMetadataProcessor>): List<EffectiveMetadataProcessor> {
        val edges = buildDependencyGraph(processors)
        val sorted = topologicalSort(processors, edges)
        return consolidateParallelProcessors(sorted, edges)
    }

    /**
     * Groups a sorted processor list by [ExecutionMode] into contiguous runs.
     */
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

    /**
     * Builds the dependency graph from processor constraints.
     * Returns adjacency map: edges[A] contains B means A must run before B.
     */
    private fun buildDependencyGraph(
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
                    is OrderConstraint.RunsAfter  -> {
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

    private fun topologicalSort(
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
    private fun consolidateParallelProcessors(
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
}

internal data class ProcessorGroup(
    val executionMode: ExecutionMode,
    val processors: List<EffectiveMetadataProcessor>,
)
