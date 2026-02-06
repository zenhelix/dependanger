package io.github.zenhelix.dependanger.core.model

import kotlinx.serialization.Serializable

@Serializable
public data class Version(
    val name: String,
    val value: String,
    val fallbacks: List<VersionFallback> = emptyList(),
)

@Serializable
public data class VersionFallback(
    val value: String,
    val condition: FallbackCondition,
)
