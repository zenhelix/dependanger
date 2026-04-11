package io.github.zenhelix.dependanger.feature.model.transitive

import io.github.zenhelix.dependanger.core.model.MavenCoordinate
import kotlinx.serialization.Serializable

@Serializable
public data class TransitiveTree(
    val coordinate: MavenCoordinate,
    val version: String?,
    val scope: String?,
    val children: List<TransitiveTree>,
    val isDuplicate: Boolean,
    val isCycle: Boolean,
)
