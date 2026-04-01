package io.github.zenhelix.dependanger.features.updates

import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.effective.pipeline.PipelineBuilder
import io.github.zenhelix.dependanger.effective.spi.PresetContributor

public class UpdateCheckPresetContributor : PresetContributor {
    override fun configure(preset: ProcessingPreset, builder: PipelineBuilder) {
        when (preset) {
            ProcessingPreset.STRICT -> builder.enableOptional(UpdateCheckProcessor.PROCESSOR_ID)
            else                    -> {}
        }
    }
}
