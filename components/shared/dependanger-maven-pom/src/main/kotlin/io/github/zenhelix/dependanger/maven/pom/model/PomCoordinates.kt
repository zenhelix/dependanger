package io.github.zenhelix.dependanger.maven.pom.model

import kotlinx.serialization.Serializable

@Serializable
public data class PomCoordinates(
    val groupId: String,
    val artifactId: String,
    val version: String,
)
