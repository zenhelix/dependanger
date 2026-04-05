package io.github.zenhelix.dependanger.maven.client

public data class PomParseResult(
    val properties: Map<String, String>,
    val parent: ParentPom?,
    val dependencies: List<RawBomDependency>,
)

public data class ParentPom(
    val group: String,
    val artifact: String,
    val version: String,
)

public data class RawBomDependency(
    val group: String,
    val artifact: String,
    val version: String,
    val scope: String?,
    val type: String?,
)
