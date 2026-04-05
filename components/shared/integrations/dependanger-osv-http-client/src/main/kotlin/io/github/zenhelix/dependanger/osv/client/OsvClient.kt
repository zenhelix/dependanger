package io.github.zenhelix.dependanger.osv.client

import io.github.zenhelix.dependanger.http.client.HttpClientFactory
import io.github.zenhelix.dependanger.osv.client.internal.DefaultOsvClient
import io.github.zenhelix.dependanger.osv.client.model.OsvBatchResult
import io.github.zenhelix.dependanger.osv.client.model.OsvPackageQuery

public interface OsvClient : AutoCloseable {
    public suspend fun queryBatch(packages: List<OsvPackageQuery>): OsvBatchResult
}

public fun OsvClient(config: OsvClientConfig, httpClientFactory: HttpClientFactory): OsvClient =
    DefaultOsvClient(config, httpClientFactory)
