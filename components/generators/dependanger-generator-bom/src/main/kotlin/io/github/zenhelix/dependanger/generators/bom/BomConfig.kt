package io.github.zenhelix.dependanger.generators.bom

public data class BomConfig(
    val groupId: String,
    val artifactId: String,
    val version: String,
    val name: String?,
    val description: String?,
    val filename: String,
    val includeOptionalDependencies: Boolean,
    val prettyPrint: Boolean,
    val includeDeprecationComments: Boolean,
) {
    init {
        require(groupId.isNotBlank()) { "BomConfig.groupId must not be blank" }
        require(artifactId.isNotBlank()) { "BomConfig.artifactId must not be blank" }
        require(version.isNotBlank()) { "BomConfig.version must not be blank" }
    }

    public companion object {
        public const val DEFAULT_FILENAME: String = "bom.pom.xml"
    }
}
