package io.github.zenhelix.dependanger.core.model.filter

import kotlinx.serialization.Serializable

@Serializable
public data class BundleFilter(
    val includes: Set<String> = emptySet(),
    val excludes: Set<String> = emptySet(),
)
