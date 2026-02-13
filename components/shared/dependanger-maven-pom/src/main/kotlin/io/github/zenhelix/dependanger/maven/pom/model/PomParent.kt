package io.github.zenhelix.dependanger.maven.pom.model

import kotlinx.serialization.Serializable

@Serializable
public data class PomParent(
    val coordinates: PomCoordinates,
    val relativePath: String? = null,
)
