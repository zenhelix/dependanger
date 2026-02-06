package io.github.zenhelix.dependanger.features.transitive

import kotlinx.serialization.Serializable

@Serializable
public data class TransitiveTree(
    val root: String,
    val children: List<TransitiveTree> = emptyList(),
    val version: String? = null,
    val isDuplicate: Boolean = false,
)
