package io.github.zenhelix.dependanger.api

import io.github.zenhelix.dependanger.core.dsl.DependangerDsl
import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata
import io.github.zenhelix.dependanger.effective.coreProcessors
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.PipelineBuilder
import io.github.zenhelix.dependanger.effective.pipeline.PipelineConfigurationException
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingCallback
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingEnvironment
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPipeline
import io.github.zenhelix.dependanger.effective.pipeline.configure
import io.github.zenhelix.dependanger.metadata.JsonSerializationFormat
import java.util.ServiceLoader

public fun Dependanger(
    dslBlock: DependangerDsl.() -> Unit,
    builderAction: DependangerBuilder.() -> Unit,
): Dependanger {
    val builder = DependangerBuilder(dslBlock)
    builder.builderAction()
    return builder.build()
}

public fun Dependanger(
    metadata: DependangerMetadata,
    builderAction: DependangerBuilder.() -> Unit,
): Dependanger {
    val builder = DependangerBuilder(metadata)
    builder.builderAction()
    return builder.build()
}

public class Dependanger internal constructor(
    private val metadata: DependangerMetadata,
    private val preset: ProcessingPreset,
    private val environment: ProcessingEnvironment,
    private val additionalProcessors: List<EffectiveMetadataProcessor>,
    private val disabledProcessorIds: Set<String>,
    private val pipelineCustomizer: (PipelineBuilder.() -> Unit)?,
) {
    public suspend fun process(
        distribution: String? = null,
        callback: ProcessingCallback? = null,
    ): DependangerResult = try {
        val pipeline = buildPipeline()
        val context = ProcessingContext(
            originalMetadata = metadata,
            settings = metadata.settings,
            environment = environment,
            activeDistribution = distribution ?: metadata.settings.defaultDistribution,
            callback = callback,
            properties = emptyMap(),
        )

        val effective = pipeline.process(context)

        DependangerResult(
            effective = effective,
            diagnostics = effective.diagnostics,
        )
    } catch (e: DependangerException) {
        throw e
    } catch (e: PipelineConfigurationException) {
        throw DependangerConfigurationException("Pipeline configuration error: ${e.message}", e)
    } catch (e: Exception) {
        throw DependangerProcessingException("Processing failed: ${e.message}", phase = null, cause = e)
    }

    public suspend fun validate(): DependangerResult = try {
        val pipeline = ProcessingPipeline {
            addProcessors(coreProcessors())
            val featureProcessors = ServiceLoader.load(EffectiveMetadataProcessor::class.java)
            addProcessors(featureProcessors.toList())
            ProcessingPreset.MINIMAL.configure(this)
            enableOptional("validation")
        }
        val context = ProcessingContext(
            originalMetadata = metadata,
            settings = metadata.settings,
            environment = environment,
            activeDistribution = null,
            callback = null,
            properties = emptyMap(),
        )

        val effective = pipeline.process(context)

        DependangerResult(
            effective = effective,
            diagnostics = effective.diagnostics,
        )
    } catch (e: Exception) {
        val diagnostics = Diagnostics.error(
            code = "VALIDATION_FAILED",
            message = "Validation failed: ${e.message}",
            processorId = null,
            context = emptyMap(),
        )
        DependangerResult(effective = null, diagnostics = diagnostics)
    }

    private fun buildPipeline(): ProcessingPipeline = ProcessingPipeline {
        addProcessors(coreProcessors())
        val featureProcessors = ServiceLoader.load(EffectiveMetadataProcessor::class.java)
        addProcessors(featureProcessors.toList())
        addProcessors(additionalProcessors)
        preset.configure(this)
        disabledProcessorIds.forEach { id -> disable(id) }
        pipelineCustomizer?.invoke(this)
    }

    public companion object {
        public fun fromDsl(block: DependangerDsl.() -> Unit): DependangerBuilder = DependangerBuilder(block)
        public fun fromMetadata(metadata: DependangerMetadata): DependangerBuilder = DependangerBuilder(metadata)

        public fun fromJson(json: String): DependangerBuilder {
            val format = JsonSerializationFormat()
            val metadata = try {
                format.deserialize(json)
            } catch (e: Exception) {
                throw DependangerConfigurationException(
                    "Failed to parse metadata from JSON: ${e.message}", e
                )
            }
            return DependangerBuilder(metadata)
        }
    }
}
