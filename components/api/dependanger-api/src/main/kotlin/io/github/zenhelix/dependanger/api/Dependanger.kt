package io.github.zenhelix.dependanger.api

import io.github.zenhelix.dependanger.core.dsl.DependangerDsl
import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata
import io.github.zenhelix.dependanger.core.pipeline.ProcessingContextKey
import io.github.zenhelix.dependanger.effective.ProcessorIds
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.PipelineBuilder
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingCallback
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingEnvironment
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPipeline
import io.github.zenhelix.dependanger.effective.pipeline.configure
import io.github.zenhelix.dependanger.effective.spi.ContextContributor
import io.github.zenhelix.dependanger.effective.spi.FeatureSettingsProvider
import io.github.zenhelix.dependanger.metadata.JsonSerializationFormat
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.util.ServiceLoader

private fun EffectiveMetadata.toResult(): DependangerResult =
    if (diagnostics.hasErrors) {
        DependangerResult.CompletedWithErrors(effective = this, diagnostics = diagnostics)
    } else {
        DependangerResult.Success(effective = this, diagnostics = diagnostics)
    }

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
    private val discoveredProcessors: List<EffectiveMetadataProcessor>,
    private val additionalProcessors: List<EffectiveMetadataProcessor>,
    private val disabledProcessorIds: Set<String>,
    private val pipelineCustomizer: (PipelineBuilder.() -> Unit)?,
    private val contextProperties: Map<ProcessingContextKey<*>, Any>,
) {
    private val featureSettingsProviders: List<FeatureSettingsProvider> by lazy {
        ServiceLoader.load(FeatureSettingsProvider::class.java).toList()
    }

    private val contextContributors: List<ContextContributor> by lazy {
        ServiceLoader.load(ContextContributor::class.java).toList()
    }

    public suspend fun process(
        distribution: String? = null,
        callback: ProcessingCallback? = null,
    ): DependangerResult = runPipeline("Processing") {
        val pipeline = buildPipeline()
        val context = baseContext(
            distribution = distribution ?: metadata.settings.defaultDistribution,
            callback = callback,
        )
        pipeline.process(context).toResult()
    }

    public suspend fun validate(): DependangerResult = runPipeline("Validation") {
        val pipeline = buildValidationPipeline()
        pipeline.process(baseContext()).toResult()
    }

    private suspend fun runPipeline(
        operationName: String,
        block: suspend () -> DependangerResult,
    ): DependangerResult = try {
        block()
    } catch (e: Exception) {
        currentCoroutineContext().ensureActive()
        DependangerResult.Failure(
            diagnostics = Diagnostics.error(
                "PIPELINE_ERROR",
                "$operationName failed: ${e.message}",
                null,
                emptyMap(),
            )
        )
    }

    public suspend fun previewFilter(distribution: String): FilterPreview {
        val pipeline = ProcessingPipeline {
            addCoreProcessors()
            PREVIEW_EXCLUDED_PROCESSORS.forEach { disable(it) }
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
    ): ProcessingContext {
        val settingsProviders = featureSettingsProviders
        val resolvedProperties = buildMap<ProcessingContextKey<*>, Any> {
            for (contributor in contextContributors) {
                putAll(contributor.contribute())
            }
            for (provider in settingsProviders) {
                val json = metadata.settings.customSettings[provider.settingsKey]
                if (json != null) {
                    val (key, value) = provider.deserialize(json)
                    put(key, value)
                }
            }
            putAll(contextProperties)
        }

        return ProcessingContext(
            originalMetadata = metadata,
            settings = metadata.settings,
            environment = environment,
            activeDistribution = distribution ?: metadata.settings.defaultDistribution,
            callback = callback,
            properties = resolvedProperties,
        )
    }

    private fun buildValidationPipeline(): ProcessingPipeline = ProcessingPipeline {
        addCoreProcessors()
        preset.configure(this)
        enableOptional(ProcessorIds.VALIDATION)
    }

    private fun buildPipeline(
        postConfigure: (PipelineBuilder.() -> Unit)? = null,
    ): ProcessingPipeline = ProcessingPipeline {
        addCoreProcessors()
        addProcessors(discoveredProcessors)
        addProcessors(additionalProcessors)
        preset.configure(this)
        disabledProcessorIds.forEach { id -> disable(id) }
        pipelineCustomizer?.invoke(this)
        postConfigure?.invoke(this)
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
