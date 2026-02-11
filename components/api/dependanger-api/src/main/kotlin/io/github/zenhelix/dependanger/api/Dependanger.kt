package io.github.zenhelix.dependanger.api

import io.github.zenhelix.dependanger.core.dsl.DependangerDsl
import io.github.zenhelix.dependanger.core.model.DiagnosticMessage
import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.core.model.Severity
import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.PipelineBuilder
import io.github.zenhelix.dependanger.effective.pipeline.PipelineConfigurationException
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingCallback
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingEnvironment
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPipeline
import io.github.zenhelix.dependanger.effective.pipeline.configure
import io.github.zenhelix.dependanger.effective.processor.BundleFilterProcessor
import io.github.zenhelix.dependanger.effective.processor.CompatRulesProcessor
import io.github.zenhelix.dependanger.effective.processor.ExtractedVersionsProcessor
import io.github.zenhelix.dependanger.effective.processor.LibraryFilterProcessor
import io.github.zenhelix.dependanger.effective.processor.MetadataConversionProcessor
import io.github.zenhelix.dependanger.effective.processor.PluginFilterProcessor
import io.github.zenhelix.dependanger.effective.processor.PluginProcessor
import io.github.zenhelix.dependanger.effective.processor.ProfileProcessor
import io.github.zenhelix.dependanger.effective.processor.UsedVersionsProcessor
import io.github.zenhelix.dependanger.effective.processor.ValidationProcessor
import io.github.zenhelix.dependanger.effective.processor.VersionFallbackProcessor
import io.github.zenhelix.dependanger.effective.processor.VersionResolverProcessor
import io.github.zenhelix.dependanger.metadata.JsonSerializationFormat
import java.util.ServiceLoader

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
        val builder = PipelineBuilder()

        builder.addProcessors(coreProcessors())

        val featureProcessors = ServiceLoader.load(EffectiveMetadataProcessor::class.java)
        builder.addProcessors(featureProcessors.toList())

        ProcessingPreset.MINIMAL.configure(builder)
        builder.enableOptional("validation")

        val pipeline = builder.build()
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
        val diagnostics = Diagnostics(
            errors = listOf(
                DiagnosticMessage(
                    code = "VALIDATION_FAILED",
                    message = "Validation failed: ${e.message}",
                    severity = Severity.ERROR,
                    processorId = null,
                    context = emptyMap(),
                )
            ),
            warnings = emptyList(),
            infos = emptyList(),
        )
        DependangerResult(effective = null, diagnostics = diagnostics)
    }

    private fun buildPipeline(): ProcessingPipeline {
        val builder = PipelineBuilder()

        builder.addProcessors(coreProcessors())

        val featureProcessors = ServiceLoader.load(EffectiveMetadataProcessor::class.java)
        builder.addProcessors(featureProcessors.toList())

        builder.addProcessors(additionalProcessors)

        preset.configure(builder)

        disabledProcessorIds.forEach { id -> builder.disable(id) }

        pipelineCustomizer?.invoke(builder)

        return builder.build()
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

        private fun coreProcessors(): List<EffectiveMetadataProcessor> = listOf(
            ProfileProcessor(),
            MetadataConversionProcessor(),
            ExtractedVersionsProcessor(),
            LibraryFilterProcessor(),
            VersionFallbackProcessor(),
            VersionResolverProcessor(),
            BundleFilterProcessor(),
            PluginFilterProcessor(),
            PluginProcessor(),
            UsedVersionsProcessor(),
            ValidationProcessor(),
            CompatRulesProcessor(),
        )
    }
}
