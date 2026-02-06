package io.github.zenhelix.dependanger.api

import io.github.zenhelix.dependanger.core.dsl.DependangerDsl
import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingEnvironment
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPreset

public class Dependanger private constructor(
    private val metadata: DependangerMetadata,
    private val preset: ProcessingPreset,
    private val environment: ProcessingEnvironment,
) {
    public fun process(distribution: String? = null): DependangerResult = TODO()
    public fun process(distribution: String? = null, callback: ProcessingCallback? = null): DependangerResult = TODO()
    public fun generateToml(effective: EffectiveMetadata): String = TODO()
    public fun generateBom(effective: EffectiveMetadata): String = TODO()
    public fun validate(): DependangerResult = TODO()

    public companion object {
        public fun fromDsl(block: DependangerDsl.() -> Unit): DependangerBuilder = DependangerBuilder(block)
        public fun fromMetadata(metadata: DependangerMetadata): DependangerBuilder = DependangerBuilder(metadata)
        public fun fromJson(json: String): DependangerBuilder = TODO()
    }
}
