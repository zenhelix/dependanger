package io.github.zenhelix.dependanger.features.transitive.model

import kotlinx.serialization.Serializable

@Serializable
public data class FlatDependency(
    val group: String,
    val artifact: String,
    val version: String,
    val scope: String?,
    val isDirectDependency: Boolean,
    val isOptional: Boolean,
)
