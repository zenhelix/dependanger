package io.github.zenhelix.dependanger.features.transitive

import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.effective.pipeline.PipelineBuilder
import io.github.zenhelix.dependanger.effective.spi.PresetContributor

public class TransitiveResolverPresetContributor : PresetContributor {
    override fun configure(preset: ProcessingPreset, builder: PipelineBuilder) {
        when (preset) {
            ProcessingPreset.STRICT -> builder.enableOptional(TransitiveResolverProcessor.PROCESSOR_ID)
            else                    -> {}
        }
    }
}
