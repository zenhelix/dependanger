package io.github.zenhelix.dependanger.features.transitive.model

import io.github.zenhelix.dependanger.features.transitive.ConflictResolutionStrategy
import kotlinx.serialization.Serializable

@Serializable
public data class VersionConflict(
    val group: String,
    val artifact: String,
    val requestedVersions: List<String>,
    val resolvedVersion: String,
    val resolution: ConflictResolutionStrategy,
)
