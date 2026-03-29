package io.github.zenhelix.dependanger.api

import io.github.zenhelix.dependanger.core.dsl.DependangerDsl
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata
import io.github.zenhelix.dependanger.effective.coreProcessors
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.PipelineBuilder
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContextKey
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingEnvironment
import java.util.ServiceLoader

public class DependangerBuilder {
    private var metadata: DependangerMetadata? = null
    private var dslBlock: (DependangerDsl.() -> Unit)? = null
    private var preset: ProcessingPreset = ProcessingPreset.DEFAULT
    private var environment: ProcessingEnvironment = ProcessingEnvironment.DEFAULT
    private val additionalProcessors: MutableList<EffectiveMetadataProcessor> = mutableListOf()
    private val disabledProcessorIds: MutableSet<String> = mutableSetOf()
    private var pipelineCustomizer: (PipelineBuilder.() -> Unit)? = null
    private val contextProperties: MutableMap<ProcessingContextKey<*>, Any> = mutableMapOf()

    public constructor(dslBlock: DependangerDsl.() -> Unit) {
        this.dslBlock = dslBlock
    }

    public constructor(metadata: DependangerMetadata) {
        this.metadata = metadata
    }

    public fun preset(preset: ProcessingPreset): DependangerBuilder = apply {
        this.preset = preset
    }

    public fun environment(environment: ProcessingEnvironment): DependangerBuilder = apply {
        this.environment = environment
    }

    public fun jdkVersion(version: Int): DependangerBuilder = apply {
        this.environment = environment.copy(jdkVersion = version)
    }

    public fun kotlinVersion(version: String): DependangerBuilder = apply {
        this.environment = environment.copy(kotlinVersion = version)
    }

    public fun gradleVersion(version: String): DependangerBuilder = apply {
        this.environment = environment.copy(gradleVersion = version)
    }

    public fun addProcessor(processor: EffectiveMetadataProcessor): DependangerBuilder = apply {
        additionalProcessors.add(processor)
    }

    public fun disableProcessor(processorId: String): DependangerBuilder = apply {
        disabledProcessorIds.add(processorId)
    }

    public fun configureProcessing(block: PipelineBuilder.() -> Unit): DependangerBuilder = apply {
        val previous = pipelineCustomizer
        this.pipelineCustomizer = {
            previous?.invoke(this)
            block()
        }
    }

    public fun <T : Any> withContextProperty(key: ProcessingContextKey<T>, value: T): DependangerBuilder = apply {
        contextProperties[key] = value
    }

    public fun build(): Dependanger {
        val resolvedMetadata = resolveMetadata()
        return Dependanger(
            metadata = resolvedMetadata,
            preset = preset,
            environment = environment,
            coreProcessors = coreProcessors(),
            discoveredProcessors = ServiceLoader.load(EffectiveMetadataProcessor::class.java).toList(),
            additionalProcessors = additionalProcessors.toList(),
            disabledProcessorIds = disabledProcessorIds.toSet(),
            pipelineCustomizer = pipelineCustomizer,
            contextProperties = contextProperties.toMap(),
        )
    }

    private fun resolveMetadata(): DependangerMetadata {
        metadata?.let { return it }

        dslBlock?.let { block ->
            return wrapNonDependangerException({ e ->
                                                   DependangerConfigurationException("DSL evaluation failed: ${e.message}", e)
                                               }) {
                val dsl = DependangerDsl()
                dsl.block()
                dsl.toMetadata()
            }
        }

        throw DependangerConfigurationException(
            "Cannot build Dependanger: neither metadata nor DSL block provided. Use Dependanger.fromMetadata() or Dependanger.fromDsl() to create a builder.",
            null
        )
    }
}
