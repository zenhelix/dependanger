package io.github.zenhelix.dependanger.effective.model

import kotlinx.serialization.Serializable

@Serializable
public data class EffectivePlugin(
    val alias: String,
    val id: String,
    val version: ResolvedVersion? = null,
)
