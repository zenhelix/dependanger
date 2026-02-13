package io.github.zenhelix.dependanger.maven.pom.model

import kotlinx.serialization.Serializable

@Serializable
public data class PomDependencyManagement(
    val dependencies: List<PomDependency> = emptyList(),
)
