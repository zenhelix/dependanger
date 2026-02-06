package io.github.zenhelix.dependanger.core.model

import kotlinx.serialization.Serializable

@Serializable
public data class Plugin(
    val alias: String,
    val id: String,
    val version: VersionReference? = null,
    val tags: Set<String> = emptySet(),
)
