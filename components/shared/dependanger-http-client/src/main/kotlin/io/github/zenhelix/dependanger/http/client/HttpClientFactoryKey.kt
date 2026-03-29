package io.github.zenhelix.dependanger.http.client

import io.github.zenhelix.dependanger.core.pipeline.ProcessingContextKey

public val HttpClientFactoryKey: ProcessingContextKey<HttpClientFactory> =
    ProcessingContextKey("httpClientFactory")
