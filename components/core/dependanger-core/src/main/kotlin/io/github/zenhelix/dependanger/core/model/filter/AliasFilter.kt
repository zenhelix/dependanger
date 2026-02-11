package io.github.zenhelix.dependanger.core.model.filter

import kotlinx.serialization.Serializable

@Serializable
public data class AliasFilter(
    val includes: Set<String>,
    val excludes: Set<String>,
)
