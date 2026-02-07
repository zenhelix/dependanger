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
public sealed class VersionRange {
    @Serializable
    public data class Simple(val notation: String) : VersionRange()

    @Serializable
    public data class Rich(
        val require: String? = null,
        val strictly: String? = null,
        val prefer: String? = null,
        val reject: List<String> = emptyList(),
    ) : VersionRange()
}
