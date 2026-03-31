package io.github.zenhelix.dependanger.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public sealed class VersionReference {
    @Serializable @SerialName("literal")
    public data class Literal(val version: String) : VersionReference()

    @Serializable @SerialName("reference")
    public data class Reference(val name: String) : VersionReference()

    @Serializable @SerialName("range")
    public data class Range(val range: VersionRange) : VersionReference()

    @Serializable
    public sealed class VersionRange {
        @Serializable @SerialName("simple")
        public data class Simple(val notation: String) : VersionRange()

        @Serializable @SerialName("rich")
        public data class Rich(
            val require: String?,
            val strictly: String?,
            val prefer: String?,
            val reject: List<String>,
        ) : VersionRange()
    }
}
