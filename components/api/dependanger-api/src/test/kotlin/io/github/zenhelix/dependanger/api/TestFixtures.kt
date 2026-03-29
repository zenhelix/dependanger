package io.github.zenhelix.dependanger.api

import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.ExtensionKey
import io.github.zenhelix.dependanger.effective.model.withExtension
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase

internal class FakeProcessor<T : Any>(
    override val id: String,
    override val phase: ProcessingPhase,
    override val order: Int,
    private val extensionKey: ExtensionKey<T>,
    private val provider: (EffectiveMetadata) -> T,
) : EffectiveMetadataProcessor {
    override val isOptional: Boolean = false
    override val description: String = "Fake $id for tests"
    override fun supports(context: ProcessingContext): Boolean = true

    override suspend fun process(metadata: EffectiveMetadata, context: ProcessingContext): EffectiveMetadata =
        metadata.withExtension(extensionKey, provider(metadata))
}
