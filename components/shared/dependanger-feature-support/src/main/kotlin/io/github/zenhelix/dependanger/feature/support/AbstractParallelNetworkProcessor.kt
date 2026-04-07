package io.github.zenhelix.dependanger.feature.support

import io.github.zenhelix.dependanger.core.model.Repository
import io.github.zenhelix.dependanger.core.pipeline.ProcessingContextKey
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.pipeline.ParallelMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.ParallelResult
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext

/**
 * Template Method base for parallel network-aware feature processors.
 *
 * Handles common infrastructure resolution (repositories, credentials, HTTP client factory)
 * and delegates the actual processing to [executeWithInfrastructure].
 */
public abstract class AbstractParallelNetworkProcessor<S : Any> : ParallelMetadataProcessor {

    protected abstract val settingsKey: ProcessingContextKey<S>

    protected open fun featureRepositories(settings: S): List<Repository> = emptyList()

    protected abstract suspend fun executeWithInfrastructure(
        metadata: EffectiveMetadata,
        context: ProcessingContext,
        settings: S,
        infrastructure: NetworkProcessorInfrastructure,
    ): ParallelResult

    final override suspend fun processParallel(
        metadata: EffectiveMetadata,
        context: ProcessingContext,
    ): ParallelResult {
        val settings = context.require(settingsKey)
        val infrastructure = NetworkProcessorInfrastructure.resolve(context, featureRepositories(settings))
        return executeWithInfrastructure(metadata, context, settings, infrastructure)
    }
}
