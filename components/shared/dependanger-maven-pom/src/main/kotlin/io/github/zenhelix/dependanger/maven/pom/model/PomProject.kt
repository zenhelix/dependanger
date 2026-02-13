package io.github.zenhelix.dependanger.maven.pom.model

import kotlinx.serialization.Serializable

@Serializable
public data class PomProject(
    val modelVersion: String = "4.0.0",
    val coordinates: PomCoordinates,
    val packaging: String = "jar",
    val parent: PomParent? = null,
    val name: String? = null,
    val description: String? = null,
    val properties: PomProperties = PomProperties(),
    val dependencyManagement: PomDependencyManagement? = null,
)
