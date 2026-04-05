package io.github.zenhelix.dependanger.effective.pipeline

import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.effective.ProcessorIds
import io.github.zenhelix.dependanger.effective.spi.PresetContributor
import java.util.ServiceLoader

private val presetContributors: List<PresetContributor> by lazy {
    ServiceLoader.load(PresetContributor::class.java).toList()
}

public fun ProcessingPreset.configure(builder: PipelineBuilder) {
    when (this) {
        ProcessingPreset.DEFAULT      -> configureDefault(builder)
        ProcessingPreset.MINIMAL      -> configureMinimal(builder)
        ProcessingPreset.STRICT       -> configureStrict(builder)
    }
    presetContributors.forEach { it.configure(this, builder) }
}

private fun configureDefault(builder: PipelineBuilder) {
    // DEFAULT: all mandatory processors enabled + bom-import (early feature processor)
    builder.enableOptional(ProcessorIds.BOM_IMPORT)
}

private fun configureMinimal(builder: PipelineBuilder) {
    // MINIMAL: only conversion + version resolution
    builder.disable(ProcessorIds.LIBRARY_FILTER)
    builder.disable(ProcessorIds.BUNDLE_FILTER)
    builder.disable(ProcessorIds.PLUGIN_FILTER)
    builder.disable(ProcessorIds.USED_VERSIONS)
    builder.disable(ProcessorIds.VALIDATION)
    builder.disable(ProcessorIds.COMPAT_RULES)
}

private fun configureStrict(builder: PipelineBuilder) {
    // STRICT: all mandatory + bom-import enabled; feature processors contribute via SPI
    builder.enableOptional(ProcessorIds.BOM_IMPORT)
}

