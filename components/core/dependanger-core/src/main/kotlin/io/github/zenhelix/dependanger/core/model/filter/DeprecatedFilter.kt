package io.github.zenhelix.dependanger.core.model.filter

import kotlinx.serialization.Serializable

@Serializable
public data class DeprecatedFilter(
    val include: Boolean = true,
)
