package io.github.zenhelix.dependanger.core.model

import kotlinx.serialization.Serializable

@Serializable
public sealed class VersionReference {
    @Serializable
    public data class Literal(val version: String) : VersionReference()

    @Serializable
    public data class Reference(val name: String) : VersionReference()

    @Serializable
    public data class Range(val range: VersionRange) : VersionReference()
}

@Serializable
public data class VersionRange(
    val notation: String,
)
