package io.github.zenhelix.dependanger.feature.model.transitive

import io.github.zenhelix.dependanger.core.model.MavenCoordinate
import kotlinx.serialization.Serializable

@Serializable
public data class FlatDependency(
    val coordinate: MavenCoordinate,
    val version: String,
    val scope: String?,
    val isDirectDependency: Boolean,
    val isOptional: Boolean,
)
