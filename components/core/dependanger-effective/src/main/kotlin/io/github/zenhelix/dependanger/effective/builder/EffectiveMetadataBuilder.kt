package io.github.zenhelix.dependanger.effective.builder

import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.pipeline.PipelineBuilder
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingEnvironment
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

public class EffectiveMetadataBuilder {
    public var preset: ProcessingPreset = ProcessingPreset.DEFAULT
    public var distribution: String? = null
    public var environment: ProcessingEnvironment = ProcessingEnvironment(
        jdkVersion = null,
        kotlinVersion = null,
        gradleVersion = null,
        environmentVariables = emptyMap(),
    )

    public fun preset(preset: ProcessingPreset): EffectiveMetadataBuilder = apply { this.preset = preset }
    public fun distribution(name: String): EffectiveMetadataBuilder = apply { this.distribution = name }
    public fun environment(env: ProcessingEnvironment): EffectiveMetadataBuilder = apply { this.environment = env }

    public suspend fun build(metadata: DependangerMetadata): EffectiveMetadata {
        val builder = PipelineBuilder()

        builder.addProcessors(
            listOf(
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
        )

        preset.configure(builder)

        val pipeline = builder.build()

        val activeDistribution = distribution ?: metadata.settings.defaultDistribution

        val context = ProcessingContext(
            originalMetadata = metadata,
            settings = metadata.settings,
            environment = environment,
            activeDistribution = activeDistribution,
            callback = null,
            properties = emptyMap(),
        )

        return pipeline.process(context)
    }
}
