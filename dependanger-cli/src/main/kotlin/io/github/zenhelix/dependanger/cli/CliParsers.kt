package io.github.zenhelix.dependanger.cli

import io.github.zenhelix.dependanger.core.model.MavenRepository
import io.github.zenhelix.dependanger.core.model.VersionReference

public data class MavenCoordinates(
    val group: String,
    val artifact: String,
    val version: String?,
)

public fun parseMavenCoordinates(raw: String): MavenCoordinates {
    val parts = raw.split(":")
    return when (parts.size) {
        2    -> MavenCoordinates(group = parts[0], artifact = parts[1], version = null)
        3    -> MavenCoordinates(group = parts[0], artifact = parts[1], version = parts[2].ifBlank { null })
        else -> throw CliException.InvalidArgument(
            "Invalid Maven coordinates '$raw': expected format 'group:artifact' or 'group:artifact:version'"
        )
    }
}

public fun parseVersionRef(raw: String?): VersionReference? {
    if (raw == null) return null
    return if (raw.startsWith("ref:")) {
        VersionReference.Reference(name = raw.removePrefix("ref:"))
    } else {
        VersionReference.Literal(version = raw)
    }
}

public fun parseCommaSeparated(raw: String?): List<String> =
    raw?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

public fun parseMavenRepositories(raw: String?): List<MavenRepository>? =
    raw?.split(",")?.mapIndexed { i, url ->
        MavenRepository(url = url.trim(), name = "cli-repo-$i")
    }
