package io.github.zenhelix.dependanger.features.resolver

import io.github.zenhelix.dependanger.core.model.CredentialsProvider
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContextKey

public val CredentialsProviderKey: ProcessingContextKey<CredentialsProvider> =
    ProcessingContextKey("credentialsProvider")
