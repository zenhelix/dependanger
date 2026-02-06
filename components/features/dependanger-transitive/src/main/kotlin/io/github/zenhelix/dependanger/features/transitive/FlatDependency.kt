package io.github.zenhelix.dependanger.features.transitive

import kotlinx.serialization.Serializable

@Serializable
public data class FlatDependency(
    val group: String,
    val artifact: String,
    val version: String,
    val isDirectDependency: Boolean = false,
    val isOptional: Boolean = false,
)
