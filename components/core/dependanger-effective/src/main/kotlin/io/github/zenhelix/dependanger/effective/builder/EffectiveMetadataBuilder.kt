package io.github.zenhelix.dependanger.effective.builder

import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata
import io.github.zenhelix.dependanger.effective.coreProcessors
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContextKey
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingEnvironment
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPipeline
import io.github.zenhelix.dependanger.effective.pipeline.configure

public class EffectiveMetadataBuilder {
    public var preset: ProcessingPreset = ProcessingPreset.DEFAULT
    public var distribution: String? = null
    public var environment: ProcessingEnvironment = ProcessingEnvironment.DEFAULT
    private val contextProperties: MutableMap<ProcessingContextKey<*>, Any> = mutableMapOf()

    public fun preset(preset: ProcessingPreset) {
        this.preset = preset
    }

    public fun distribution(name: String) {
        this.distribution = name
    }

    public fun environment(env: ProcessingEnvironment) {
        this.environment = env
    }

    public fun <T : Any> withProperty(key: ProcessingContextKey<T>, value: T) {
        contextProperties[key] = value
    }

    public suspend fun build(metadata: DependangerMetadata): EffectiveMetadata {
        val pipeline = ProcessingPipeline {
            addProcessors(coreProcessors())
            this@EffectiveMetadataBuilder.preset.configure(this)
        }

        val activeDistribution = distribution ?: metadata.settings.defaultDistribution

        val context = ProcessingContext(
            originalMetadata = metadata,
            settings = metadata.settings,
            environment = environment,
            activeDistribution = activeDistribution,
            callback = null,
            properties = contextProperties.toMap(),
        )

        return pipeline.process(context)
    }
}

public suspend fun EffectiveMetadata(
    metadata: DependangerMetadata,
    builderAction: EffectiveMetadataBuilder.() -> Unit,
): EffectiveMetadata {
    val builder = EffectiveMetadataBuilder()
    builder.builderAction()
    return builder.build(metadata)
}
