package io.github.zenhelix.dependanger.maven.pom.model

import kotlinx.serialization.Serializable

@Serializable
public data class PomDependency(
    val groupId: String,
    val artifactId: String,
    val version: String? = null,
    val scope: String? = null,
    val type: String? = null,
    val optional: Boolean = false,
) {
    public fun isPlatformImport(): Boolean = scope == "import" && type == "pom"
}
