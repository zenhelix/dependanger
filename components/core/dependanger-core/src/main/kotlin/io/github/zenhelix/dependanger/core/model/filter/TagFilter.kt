package io.github.zenhelix.dependanger.core.model.filter

import kotlinx.serialization.Serializable

@Serializable
public data class TagFilter(
    val includes: List<TagInclude> = emptyList(),
    val excludes: List<TagExclude> = emptyList(),
)

@Serializable
public data class TagInclude(
    val anyOf: Set<String> = emptySet(),
    val allOf: Set<String> = emptySet(),
)

@Serializable
public data class TagExclude(
    val anyOf: Set<String> = emptySet(),
)
