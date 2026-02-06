package io.github.zenhelix.dependanger.effective.model

import kotlinx.serialization.Serializable

@Serializable
public data class ResolvedVersion(
    val alias: String,
    val value: String,
    val source: VersionSource,
    val originalRef: String? = null,
)

@Serializable
public enum class VersionSource {
    DECLARED, BOM_IMPORT
}
