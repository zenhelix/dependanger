package io.github.zenhelix.dependanger.effective.pipeline

import io.github.zenhelix.dependanger.effective.coreProcessors

public class PipelineBuilder {
    private val processors: MutableList<EffectiveMetadataProcessor> = mutableListOf()
    private val enabledOptionalIds: MutableSet<String> = mutableSetOf()
    private val disabledIds: MutableSet<String> = mutableSetOf()

    public fun addCoreProcessors() {
        processors.addAll(coreProcessors())
    }

    public fun addProcessor(processor: EffectiveMetadataProcessor) {
        processors.add(processor)
    }

    public fun addProcessors(processors: Collection<EffectiveMetadataProcessor>) {
        this.processors.addAll(processors)
    }

    public fun enableOptional(processorId: String) {
        enabledOptionalIds.add(processorId)
    }

    public fun enableOptional(vararg processorIds: String) {
        enabledOptionalIds.addAll(processorIds)
    }

    public fun disable(processorId: String) {
        disabledIds.add(processorId)
    }

    public fun disable(vararg processorIds: String) {
        disabledIds.addAll(processorIds)
    }

    public fun build(): ProcessingPipeline {
        val activeProcessors = processors.filter { processor ->
            val isDisabled = disabledIds.contains(processor.id)
            val isOptionalAndNotEnabled = processor.isOptional && !enabledOptionalIds.contains(processor.id)
            !isDisabled && !isOptionalAndNotEnabled
        }

        validate(activeProcessors)

        return ProcessingPipeline(activeProcessors)
    }

    private fun validate(processors: List<EffectiveMetadataProcessor>) {
        validateDuplicateIds(processors)
        validateOrderCollisions(processors)
    }

    private fun validateDuplicateIds(processors: List<EffectiveMetadataProcessor>) {
        val duplicates = processors.groupBy { it.id }.filterValues { it.size > 1 }
        if (duplicates.isNotEmpty()) {
            val details = duplicates.entries.joinToString { (id, procs) ->
                "'$id' (${procs.size} instances)"
            }
            throw PipelineConfigurationException("Duplicate processor IDs: $details")
        }
    }

    private fun validateOrderCollisions(processors: List<EffectiveMetadataProcessor>) {
        val collisions = processors
            .groupBy { it.order }
            .filterValues { group -> group.map { it.phase.executionMode }.distinct().size > 1 }

        if (collisions.isNotEmpty()) {
            val details = collisions.entries.joinToString { (order, procs) ->
                "order=$order: ${procs.joinToString { "${it.id}(${it.phase.executionMode})" }}"
            }
            throw PipelineConfigurationException(
                "Processors with same order but different execution modes: $details"
            )
        }
    }
}

public fun ProcessingPipeline(
    builderAction: PipelineBuilder.() -> Unit,
): ProcessingPipeline {
    val builder = PipelineBuilder()
    builder.builderAction()
    return builder.build()
}

public class PipelineConfigurationException(
    message: String,
) : IllegalStateException(message)
