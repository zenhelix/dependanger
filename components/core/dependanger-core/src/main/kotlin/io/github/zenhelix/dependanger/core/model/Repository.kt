package io.github.zenhelix.dependanger.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public sealed interface Repository {
    public val url: String
    public val name: String
}

@Serializable
@SerialName("maven")
public data class MavenRepository(
    override val url: String,
    override val name: String,
) : Repository
