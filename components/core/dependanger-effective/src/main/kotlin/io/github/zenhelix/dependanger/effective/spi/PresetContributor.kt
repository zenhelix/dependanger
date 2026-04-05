package io.github.zenhelix.dependanger.effective.spi

import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.effective.pipeline.PipelineBuilder

/**
 * SPI for feature modules to contribute their processor configuration to presets.
 * Implementations are discovered via ServiceLoader and invoked during preset configuration.
 *
 * Each feature module registers its own [PresetContributor] to declare which presets
 * should enable its processor(s), keeping preset knowledge local to the feature.
 */
public interface PresetContributor {
    public fun configure(preset: ProcessingPreset, builder: PipelineBuilder)
}

/**
 * Base [PresetContributor] that enables a single optional processor when the [ProcessingPreset.STRICT] preset is active.
 * Feature modules can extend this with a one-liner instead of duplicating the when-expression.
 */
public abstract class StrictOnlyPresetContributor(
    private val processorId: String,
) : PresetContributor {
    override fun configure(preset: ProcessingPreset, builder: PipelineBuilder) {
        if (preset == ProcessingPreset.STRICT) {
            builder.enableOptional(processorId)
        }
    }
}
