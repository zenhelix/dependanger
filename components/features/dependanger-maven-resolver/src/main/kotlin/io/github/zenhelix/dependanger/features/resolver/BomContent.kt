package io.github.zenhelix.dependanger.features.resolver

import kotlinx.serialization.Serializable

@Serializable
public data class BomDependency(
    val group: String,
    val artifact: String,
    val version: String,
)

@Serializable
public data class BomContent(
    val dependencies: List<BomDependency> = emptyList(),
    val properties: Map<String, String> = emptyMap(),
)
