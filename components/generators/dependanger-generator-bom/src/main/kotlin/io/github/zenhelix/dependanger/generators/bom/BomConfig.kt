package io.github.zenhelix.dependanger.generators.bom

public data class BomConfig(
    val groupId: String,
    val artifactId: String,
    val version: String,
    val includeOptionalDependencies: Boolean = false,
)
