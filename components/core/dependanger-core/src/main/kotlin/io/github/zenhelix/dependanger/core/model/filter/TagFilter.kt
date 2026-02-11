package io.github.zenhelix.dependanger.core.model.filter

import kotlinx.serialization.Serializable

@Serializable
public data class TagFilter(
    val includes: List<TagInclude>,
    val excludes: List<TagExclude>,
)

@Serializable
public data class TagInclude(
    val anyOf: Set<String>,
    val allOf: Set<String>,
)

@Serializable
public data class TagExclude(
    val anyOf: Set<String>,
)
