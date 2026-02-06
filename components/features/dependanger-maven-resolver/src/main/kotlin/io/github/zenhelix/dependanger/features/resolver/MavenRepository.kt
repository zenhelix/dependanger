package io.github.zenhelix.dependanger.features.resolver

public data class MavenRepository(
    val url: String,
    val name: String = "",
    val username: String? = null,
    val password: String? = null,
)
