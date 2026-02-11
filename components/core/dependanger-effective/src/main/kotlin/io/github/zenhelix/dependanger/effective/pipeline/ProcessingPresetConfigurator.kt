package io.github.zenhelix.dependanger.effective.pipeline

import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.effective.ProcessorIds

public fun ProcessingPreset.configure(builder: PipelineBuilder) {
    when (this) {
        ProcessingPreset.DEFAULT      -> configureDefault(builder)
        ProcessingPreset.MINIMAL      -> configureMinimal(builder)
        ProcessingPreset.STRICT       -> configureStrict(builder)
        ProcessingPreset.DISTRIBUTION -> configureDistribution(builder)
    }
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
    builder.disable(ProcessorIds.PLUGIN)
    builder.disable(ProcessorIds.USED_VERSIONS)
    builder.disable(ProcessorIds.VALIDATION)
    builder.disable(ProcessorIds.COMPAT_RULES)
}

private fun configureStrict(builder: PipelineBuilder) {
    // STRICT: all mandatory + all checks enabled
    builder.enableOptional(ProcessorIds.BOM_IMPORT)
    builder.enableOptional(ProcessorIds.UPDATE_CHECK)
    builder.enableOptional(ProcessorIds.COMPATIBILITY_ANALYSIS)
    builder.enableOptional(ProcessorIds.SECURITY_CHECK)
    builder.enableOptional(ProcessorIds.LICENSE_CHECK)
    builder.enableOptional(ProcessorIds.TRANSITIVE_RESOLVER)
}

private fun configureDistribution(builder: PipelineBuilder) {
    // DISTRIBUTION: same as DEFAULT + profile filtering (profile processor is mandatory anyway)
    builder.enableOptional(ProcessorIds.BOM_IMPORT)
}
