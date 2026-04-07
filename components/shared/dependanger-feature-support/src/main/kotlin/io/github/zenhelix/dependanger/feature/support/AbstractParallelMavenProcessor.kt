package io.github.zenhelix.dependanger.feature.support

import io.github.zenhelix.dependanger.core.model.Repository
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.pipeline.ParallelResult
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.http.client.HttpClientFactory

/**
 * Template Method base for parallel Maven-aware feature processors.
 *
 * Handles common infrastructure resolution (repositories, credentials, HTTP client factory)
 * and delegates the actual processing to [executeWithMavenInfrastructure].
 */
public abstract class AbstractParallelMavenProcessor<S : Any> : AbstractParallelFeatureProcessor<S>() {

    protected open fun featureRepositories(settings: S): List<Repository> = emptyList()

    protected abstract suspend fun executeWithMavenInfrastructure(
        metadata: EffectiveMetadata,
        context: ProcessingContext,
        settings: S,
        infrastructure: NetworkProcessorInfrastructure,
    ): ParallelResult

    final override suspend fun executeWithInfrastructure(
        metadata: EffectiveMetadata,
        context: ProcessingContext,
        settings: S,
        httpClientFactory: HttpClientFactory,
    ): ParallelResult {
        val infrastructure = NetworkProcessorInfrastructure.resolve(
            context, featureRepositories(settings), httpClientFactory,
        )
        return executeWithMavenInfrastructure(metadata, context, settings, infrastructure)
    }
}
