package io.github.zenhelix.dependanger.api

import io.github.zenhelix.dependanger.core.dsl.DependangerDsl
import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata
import io.github.zenhelix.dependanger.effective.ProcessorIds
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.PipelineBuilder
import io.github.zenhelix.dependanger.effective.pipeline.PipelineConfigurationException
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingCallback
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingEnvironment
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPipeline
import io.github.zenhelix.dependanger.effective.pipeline.configure
import io.github.zenhelix.dependanger.metadata.JsonSerializationFormat
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

private val PREVIEW_EXCLUDED_PROCESSORS: Set<String> = setOf(
    ProcessorIds.VALIDATION,
    ProcessorIds.COMPAT_RULES,
    ProcessorIds.USED_VERSIONS,
)

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
    private val coreProcessors: List<EffectiveMetadataProcessor>,
    private val discoveredProcessors: List<EffectiveMetadataProcessor>,
    private val additionalProcessors: List<EffectiveMetadataProcessor>,
    private val disabledProcessorIds: Set<String>,
    private val pipelineCustomizer: (PipelineBuilder.() -> Unit)?,
) {
    public suspend fun process(
        distribution: String? = null,
        callback: ProcessingCallback? = null,
    ): DependangerResult = try {
        val pipeline = buildPipeline()
        val context = baseContext(
            distribution = distribution ?: metadata.settings.defaultDistribution,
            callback = callback,
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
        currentCoroutineContext().ensureActive()
        throw DependangerProcessingException("Processing failed: ${e.message}", phase = null, cause = e)
    }

    public suspend fun validate(): DependangerResult = try {
        val pipeline = ProcessingPipeline {
            addProcessors(coreProcessors)
            addProcessors(discoveredProcessors)
            ProcessingPreset.MINIMAL.configure(this)
            enableOptional(ProcessorIds.VALIDATION)
        }

        val effective = pipeline.process(baseContext())

        DependangerResult(
            effective = effective,
            diagnostics = effective.diagnostics,
        )
    } catch (e: Exception) {
        currentCoroutineContext().ensureActive()
        val diagnostics = Diagnostics.error(
            code = "VALIDATION_FAILED",
            message = "Validation failed: ${e.message}",
            processorId = null,
            context = emptyMap(),
        )
        DependangerResult(effective = null, diagnostics = diagnostics)
    }

    public suspend fun previewFilter(distribution: String): FilterPreview {
        val pipeline = ProcessingPipeline {
            addProcessors(coreProcessors.filter { it.id !in PREVIEW_EXCLUDED_PROCESSORS })
        }

        val (unfiltered, filtered) = coroutineScope {
            val u = async { pipeline.process(baseContext()) }
            val f = async { pipeline.process(baseContext(distribution = distribution)) }
            u.await() to f.await()
        }

        return FilterPreview(
            distribution = distribution,
            included = FilteredItems(
                libraries = filtered.libraries,
                plugins = filtered.plugins,
                bundles = filtered.bundles,
            ),
            excluded = FilteredItems(
                libraries = unfiltered.libraries - filtered.libraries.keys,
                plugins = unfiltered.plugins - filtered.plugins.keys,
                bundles = unfiltered.bundles - filtered.bundles.keys,
            ),
        )
    }

    private fun baseContext(
        distribution: String? = null,
        callback: ProcessingCallback? = null,
    ): ProcessingContext = ProcessingContext(
        originalMetadata = metadata,
        settings = metadata.settings,
        environment = environment,
        activeDistribution = distribution,
        callback = callback,
        properties = emptyMap(),
    )

    private fun buildPipeline(): ProcessingPipeline = ProcessingPipeline {
        addProcessors(coreProcessors)
        addProcessors(discoveredProcessors)
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
