package io.github.zenhelix.dependanger.features.transitive.model

import kotlinx.serialization.Serializable

@Serializable
public data class TransitiveTree(
    val group: String,
    val artifact: String,
    val version: String?,
    val scope: String?,
    val children: List<TransitiveTree>,
    val isDuplicate: Boolean,
    val isCycle: Boolean,
)
