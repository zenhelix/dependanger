package io.github.zenhelix.dependanger.effective.builder

import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingEnvironment

public class EffectiveMetadataBuilder {
    public var preset: ProcessingPreset = ProcessingPreset.DEFAULT
    public var distribution: String? = null
    public var environment: ProcessingEnvironment = ProcessingEnvironment()

    public fun preset(preset: ProcessingPreset): EffectiveMetadataBuilder = apply { this.preset = preset }
    public fun distribution(name: String): EffectiveMetadataBuilder = apply { this.distribution = name }
    public fun environment(env: ProcessingEnvironment): EffectiveMetadataBuilder = apply { this.environment = env }

    public suspend fun build(metadata: DependangerMetadata): EffectiveMetadata = TODO()
}
