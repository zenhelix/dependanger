package io.github.zenhelix.dependanger.core.model

import kotlinx.serialization.Serializable

@Serializable
public sealed class Constraint {
    @Serializable
    public data class VersionConstraintDef(
        val coordinates: String,
        val version: VersionReference? = null,
        val because: String? = null,
    ) : Constraint()

    @Serializable
    public data class Exclude(
        val group: String,
        val artifact: String,
    ) : Constraint()

    @Serializable
    public data class Substitute(
        val from: String,
        val to: String,
        val because: String? = null,
    ) : Constraint()
}
