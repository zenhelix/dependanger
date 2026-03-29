package io.github.zenhelix.dependanger.feature.model.transitive

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
