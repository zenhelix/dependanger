package io.github.zenhelix.dependanger.core.model

import kotlinx.serialization.Serializable

@Serializable
public data class Library(
    val alias: String,
    val group: String,
    val artifact: String,
    val version: VersionReference? = null,
    val tags: Set<String> = emptySet(),
    val requires: Requirements? = null,
    val deprecation: DeprecationInfo? = null,
    val license: LicenseInfo? = null,
    val constraints: List<Constraint> = emptyList(),
    val isPlatform: Boolean = false,
)
