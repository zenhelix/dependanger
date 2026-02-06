package io.github.zenhelix.dependanger.features.transitive

import io.github.zenhelix.dependanger.core.model.ConflictResolutionStrategy
import kotlinx.serialization.Serializable

@Serializable
public data class VersionConflict(
    val group: String,
    val artifact: String,
    val requestedVersions: List<String>,
    val resolvedVersion: String,
    val resolution: ConflictResolutionStrategy,
)
