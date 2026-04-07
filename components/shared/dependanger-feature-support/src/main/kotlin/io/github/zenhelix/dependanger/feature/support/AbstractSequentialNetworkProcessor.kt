package io.github.zenhelix.dependanger.feature.support

import io.github.zenhelix.dependanger.core.model.Repository
import io.github.zenhelix.dependanger.core.pipeline.ProcessingContextKey
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext

/**
 * Template Method base for sequential network-aware feature processors.
 *
 * Handles common infrastructure resolution (repositories, credentials, HTTP client factory)
 * and delegates the actual processing to [executeWithInfrastructure].
 */
public abstract class AbstractSequentialNetworkProcessor<S : Any> : EffectiveMetadataProcessor {

    protected abstract val settingsKey: ProcessingContextKey<S>

    protected open fun featureRepositories(settings: S): List<Repository> = emptyList()

    protected abstract suspend fun executeWithInfrastructure(
        metadata: EffectiveMetadata,
        context: ProcessingContext,
        settings: S,
        infrastructure: NetworkProcessorInfrastructure,
    ): EffectiveMetadata

    final override suspend fun process(
        metadata: EffectiveMetadata,
        context: ProcessingContext,
    ): EffectiveMetadata {
        val settings = context.require(settingsKey)
        val infrastructure = NetworkProcessorInfrastructure.resolve(context, featureRepositories(settings))
        return executeWithInfrastructure(metadata, context, settings, infrastructure)
    }
}
