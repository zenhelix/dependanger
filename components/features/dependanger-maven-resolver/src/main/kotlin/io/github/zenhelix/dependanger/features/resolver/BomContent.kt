package io.github.zenhelix.dependanger.features.resolver

import kotlinx.serialization.Serializable

@Serializable
public data class BomContent(
    val dependencies: Map<String, String> = emptyMap(),
    val properties: Map<String, String> = emptyMap(),
)
