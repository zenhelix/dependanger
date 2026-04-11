package io.github.zenhelix.dependanger.feature.model.transitive

import io.github.zenhelix.dependanger.core.model.MavenCoordinate
import kotlinx.serialization.Serializable

@Serializable
public data class VersionConflict(
    val coordinate: MavenCoordinate,
    val requestedVersions: List<String>,
    val resolvedVersion: String,
    val resolution: ConflictResolutionStrategy,
)
