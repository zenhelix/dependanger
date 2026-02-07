package io.github.zenhelix.dependanger.core.model

import kotlinx.serialization.Serializable

@Serializable
public data class Bundle(
    val alias: String,
    val libraries: List<String> = emptyList(),
    val extends: List<String> = emptyList(),
)
