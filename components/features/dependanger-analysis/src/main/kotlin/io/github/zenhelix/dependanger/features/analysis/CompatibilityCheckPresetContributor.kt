package io.github.zenhelix.dependanger.features.analysis

import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.effective.pipeline.PipelineBuilder
import io.github.zenhelix.dependanger.effective.spi.PresetContributor

public class CompatibilityCheckPresetContributor : PresetContributor {
    override fun configure(preset: ProcessingPreset, builder: PipelineBuilder) {
        when (preset) {
            ProcessingPreset.STRICT -> builder.enableOptional(CompatibilityCheckProcessor.PROCESSOR_ID)
            else                    -> {}
        }
    }
}
