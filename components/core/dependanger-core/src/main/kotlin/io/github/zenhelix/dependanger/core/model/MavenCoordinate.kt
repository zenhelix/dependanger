package io.github.zenhelix.dependanger.core.model

import kotlinx.serialization.Serializable

/**
 * Maven coordinate identifying a library by group and artifact.
 * Format: "group:artifact"
 */
@Serializable
public data class MavenCoordinate(
    val group: String,
    val artifact: String,
) {
    override fun toString(): String = "$group:$artifact"

    public companion object {
        public fun parse(coordinate: String): MavenCoordinate {
            val parts = coordinate.split(":")
            require(parts.size == 2) { "Invalid coordinate format: '$coordinate'. Expected 'group:artifact'" }
            require(parts[0].isNotBlank()) { "Group must not be blank in coordinate '$coordinate'" }
            require(parts[1].isNotBlank()) { "Artifact must not be blank in coordinate '$coordinate'" }
            return MavenCoordinate(parts[0], parts[1])
        }
    }
}

/**
 * Full Maven GAV (Group:Artifact:Version) coordinate.
 * Format: "group:artifact:version"
 */
@Serializable
public data class MavenGAV(
    val coordinate: MavenCoordinate,
    val version: String,
) {
    override fun toString(): String = "$coordinate:$version"

    public companion object {
        public fun parse(gav: String): MavenGAV {
            val parts = gav.split(":", limit = 3)
            require(parts.size == 3) { "Invalid GAV format: '$gav'. Expected 'group:artifact:version'" }
            require(parts[2].isNotBlank()) { "Version must not be blank in GAV '$gav'" }
            return MavenGAV(MavenCoordinate(parts[0], parts[1]), parts[2])
        }
    }
}
