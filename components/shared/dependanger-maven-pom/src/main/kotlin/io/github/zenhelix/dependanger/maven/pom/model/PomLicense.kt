package io.github.zenhelix.dependanger.maven.pom.model

import kotlinx.serialization.Serializable

@Serializable
public data class PomLicense(
    val name: String?,
    val url: String?,
)
