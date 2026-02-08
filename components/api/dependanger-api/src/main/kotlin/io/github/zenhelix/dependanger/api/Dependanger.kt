package io.github.zenhelix.dependanger.api

import io.github.zenhelix.dependanger.core.dsl.DependangerDsl
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.PipelineBuilder
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingCallback
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingEnvironment

public class Dependanger private constructor(
    private val metadata: DependangerMetadata,
    private val preset: ProcessingPreset,
    private val environment: ProcessingEnvironment,
    private val additionalProcessors: List<EffectiveMetadataProcessor> = emptyList(),
    private val disabledProcessorIds: Set<String> = emptySet(),
    private val pipelineCustomizer: (PipelineBuilder.() -> Unit)? = null,
) {
    public suspend fun process(distribution: String? = null, callback: ProcessingCallback? = null): DependangerResult = TODO()
    public suspend fun validate(): DependangerResult = TODO()

    public companion object {
        public fun fromDsl(block: DependangerDsl.() -> Unit): DependangerBuilder = DependangerBuilder(block)
        public fun fromMetadata(metadata: DependangerMetadata): DependangerBuilder = DependangerBuilder(metadata)
        public fun fromJson(json: String): DependangerBuilder = TODO()
    }
}
