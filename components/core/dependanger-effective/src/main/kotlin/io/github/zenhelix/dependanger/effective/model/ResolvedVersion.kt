package io.github.zenhelix.dependanger.effective.model

import kotlinx.serialization.Serializable

@Serializable
public data class ResolvedVersion(
    val value: String,
    val source: VersionSource,
)

@Serializable
public enum class VersionSource {
    DECLARED, BOM_IMPORT
}
