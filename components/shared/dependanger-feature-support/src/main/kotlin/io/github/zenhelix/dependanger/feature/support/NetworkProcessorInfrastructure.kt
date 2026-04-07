package io.github.zenhelix.dependanger.feature.support

import io.github.zenhelix.dependanger.core.model.CredentialsProvider
import io.github.zenhelix.dependanger.core.model.CredentialsProviderKey
import io.github.zenhelix.dependanger.core.model.MavenRepository
import io.github.zenhelix.dependanger.core.model.Repository
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.resolveMavenRepositories
import io.github.zenhelix.dependanger.http.client.DefaultHttpClientFactory
import io.github.zenhelix.dependanger.http.client.HttpClientFactory
import io.github.zenhelix.dependanger.http.client.HttpClientFactoryKey

public data class NetworkProcessorInfrastructure(
    val repositories: List<MavenRepository>,
    val credentialsProvider: CredentialsProvider?,
    val httpClientFactory: HttpClientFactory,
) {
    public companion object {
        internal fun resolve(
            context: ProcessingContext,
            featureRepositories: List<Repository>,
        ): NetworkProcessorInfrastructure = NetworkProcessorInfrastructure(
            repositories = context.resolveMavenRepositories(featureRepositories),
            credentialsProvider = context[CredentialsProviderKey],
            httpClientFactory = context[HttpClientFactoryKey] ?: DefaultHttpClientFactory,
        )
    }
}
