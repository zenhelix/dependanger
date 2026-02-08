package io.github.zenhelix.dependanger.core.model

public data class Credentials(
    val username: String,
    val password: String,
)

public fun interface CredentialsProvider {
    public fun getCredentials(repositoryUrl: String): Credentials?
}
