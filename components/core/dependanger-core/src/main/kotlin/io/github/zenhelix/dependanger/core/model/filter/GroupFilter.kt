package io.github.zenhelix.dependanger.core.model.filter

import kotlinx.serialization.Serializable

@Serializable
public data class GroupFilter(
    val includes: List<String> = emptyList(),
    val excludes: List<String> = emptyList(),
)
