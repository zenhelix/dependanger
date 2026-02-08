package io.github.zenhelix.dependanger.features.transitive.model

import kotlinx.serialization.Serializable

@Serializable
public data class TransitiveTree(
    val group: String,
    val artifact: String,
    val version: String? = null,
    val children: List<TransitiveTree> = emptyList(),
    val isDuplicate: Boolean = false,
    val scope: String? = null,
)
