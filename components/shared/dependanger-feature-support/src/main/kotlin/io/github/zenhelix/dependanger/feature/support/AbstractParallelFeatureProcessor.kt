package io.github.zenhelix.dependanger.feature.support

import io.github.zenhelix.dependanger.core.pipeline.ProcessingContextKey
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.pipeline.ParallelMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.ParallelResult
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.http.client.DefaultHttpClientFactory
import io.github.zenhelix.dependanger.http.client.HttpClientFactory
import io.github.zenhelix.dependanger.http.client.HttpClientFactoryKey

public abstract class AbstractParallelFeatureProcessor<S : Any> : ParallelMetadataProcessor {

    protected abstract val settingsKey: ProcessingContextKey<S>

    protected abstract suspend fun executeWithInfrastructure(
        metadata: EffectiveMetadata,
        context: ProcessingContext,
        settings: S,
        httpClientFactory: HttpClientFactory,
    ): ParallelResult

    final override suspend fun processParallel(
        metadata: EffectiveMetadata,
        context: ProcessingContext,
    ): ParallelResult {
        val settings = context.require(settingsKey)
        val httpClientFactory = context[HttpClientFactoryKey] ?: DefaultHttpClientFactory
        return executeWithInfrastructure(metadata, context, settings, httpClientFactory)
    }
}
