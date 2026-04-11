package io.github.zenhelix.dependanger.core.model

import kotlinx.serialization.Serializable

@Serializable
public data class Library(
    val alias: String,
    val coordinate: MavenCoordinate,
    val version: VersionReference?,
    val description: String?,
    val tags: Set<String>,
    val requires: Requirements?,
    val deprecation: DeprecationInfo?,
    val license: LicenseInfo?,
    val constraints: List<Constraint>,
    val isPlatform: Boolean,
    val ignoreUpdates: Boolean = false,
)
