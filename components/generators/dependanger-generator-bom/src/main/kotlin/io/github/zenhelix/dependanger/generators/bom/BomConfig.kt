package io.github.zenhelix.dependanger.generators.bom

public data class BomConfig(
    val groupId: String,
    val artifactId: String,
    val version: String,
    val filename: String = "bom.pom.xml",
    val includeOptionalDependencies: Boolean = false,
    val prettyPrint: Boolean = true,
    val includeDeprecationComments: Boolean = true,
)
