package io.github.zenhelix.dependanger.effective.model

import kotlinx.serialization.Serializable

@Serializable
public data class EffectiveBundle(
    val alias: String,
    val libraries: List<String> = emptyList(),
)
