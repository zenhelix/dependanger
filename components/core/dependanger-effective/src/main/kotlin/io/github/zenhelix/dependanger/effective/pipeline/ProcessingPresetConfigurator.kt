package io.github.zenhelix.dependanger.effective.pipeline

import io.github.zenhelix.dependanger.core.model.ProcessingPreset

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
    builder.enableOptional("bom-import")
}

private fun configureMinimal(builder: PipelineBuilder) {
    // MINIMAL: only conversion + version resolution
    builder.disable("library-filter")
    builder.disable("bundle-filter")
    builder.disable("plugin-filter")
    builder.disable("plugin")
    builder.disable("used-versions")
    builder.disable("validation")
    builder.disable("compat-rules")
}

private fun configureStrict(builder: PipelineBuilder) {
    // STRICT: all mandatory + all checks enabled
    builder.enableOptional("bom-import")
    builder.enableOptional("update-check")
    builder.enableOptional("compatibility-analysis")
    builder.enableOptional("security-check")
    builder.enableOptional("license-check")
    builder.enableOptional("transitive-resolver")
}

private fun configureDistribution(builder: PipelineBuilder) {
    // DISTRIBUTION: same as DEFAULT + profile filtering (profile processor is mandatory anyway)
    builder.enableOptional("bom-import")
}
