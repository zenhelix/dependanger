package io.github.zenhelix.dependanger.core.model

import io.github.zenhelix.dependanger.core.pipeline.ProcessingContextKey

public data class Credentials(
    val username: String,
    val password: String,
)

public fun interface CredentialsProvider {
    public fun getCredentials(repositoryUrl: String): Credentials?
}

public val CredentialsProviderKey: ProcessingContextKey<CredentialsProvider> =
    ProcessingContextKey("credentialsProvider")
