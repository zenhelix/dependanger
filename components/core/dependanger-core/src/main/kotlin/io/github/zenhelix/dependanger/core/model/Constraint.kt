package io.github.zenhelix.dependanger.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public sealed class Constraint {
    @Serializable @SerialName("versionConstraintDef")
    public data class VersionConstraintDef(
        val coordinate: MavenCoordinate,
        val version: VersionReference?,
        val because: String?,
    ) : Constraint()

    @Serializable @SerialName("exclude")
    public data class Exclude(
        val coordinate: MavenCoordinate,
    ) : Constraint()

    @Serializable @SerialName("substitute")
    public data class Substitute(
        val from: MavenCoordinate,
        val to: MavenCoordinate,
        val toVersion: String?,
        val because: String?,
    ) : Constraint()
}
