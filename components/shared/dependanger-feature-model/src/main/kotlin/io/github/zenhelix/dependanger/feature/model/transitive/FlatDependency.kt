package io.github.zenhelix.dependanger.feature.model.transitive

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
