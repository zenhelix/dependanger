package io.github.zenhelix.dependanger.core.model

import kotlinx.serialization.Serializable

@Serializable
public data class Preset(
    val name: String,
    val bundles: List<String>,
    val distributions: List<String>,
    val settings: Settings?,
)
