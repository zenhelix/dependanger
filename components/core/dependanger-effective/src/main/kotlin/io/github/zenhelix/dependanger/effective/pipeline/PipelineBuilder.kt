package io.github.zenhelix.dependanger.effective.pipeline

public class PipelineBuilder {
    private val processors: MutableList<EffectiveMetadataProcessor> = mutableListOf()
    private val enabledOptionalIds: MutableSet<String> = mutableSetOf()
    private val disabledIds: MutableSet<String> = mutableSetOf()

    public fun addProcessor(processor: EffectiveMetadataProcessor): PipelineBuilder = apply {
        processors.add(processor)
    }

    public fun addProcessors(processors: Collection<EffectiveMetadataProcessor>): PipelineBuilder = apply {
        this.processors.addAll(processors)
    }

    public fun enableOptional(processorId: String): PipelineBuilder = apply {
        enabledOptionalIds.add(processorId)
    }

    public fun enableOptional(vararg processorIds: String): PipelineBuilder = apply {
        enabledOptionalIds.addAll(processorIds)
    }

    public fun disable(processorId: String): PipelineBuilder = apply {
        disabledIds.add(processorId)
    }

    public fun disable(vararg processorIds: String): PipelineBuilder = apply {
        disabledIds.addAll(processorIds)
    }

    public fun build(): ProcessingPipeline {
        val activeProcessors = processors.filter { processor ->
            val isDisabled = disabledIds.contains(processor.id)
            val isOptionalAndNotEnabled = processor.isOptional && !enabledOptionalIds.contains(processor.id)
            !isDisabled && !isOptionalAndNotEnabled
        }
        return ProcessingPipeline(activeProcessors)
    }
}
