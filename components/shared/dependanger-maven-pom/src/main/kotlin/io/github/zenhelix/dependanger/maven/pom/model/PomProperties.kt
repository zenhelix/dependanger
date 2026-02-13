package io.github.zenhelix.dependanger.maven.pom.model

import kotlinx.serialization.Serializable

@Serializable
public data class PomProperties(
    public val entries: Map<String, String> = emptyMap(),
) {
    public operator fun get(key: String): String? = entries[key]

    public operator fun plus(other: PomProperties): PomProperties =
        PomProperties(entries + other.entries)

    public operator fun plus(map: Map<String, String>): PomProperties =
        PomProperties(entries + map)
}
