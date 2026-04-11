package io.github.zenhelix.dependanger.maven.client.model

import io.github.zenhelix.dependanger.core.model.MavenCoordinate
import io.github.zenhelix.dependanger.core.model.MavenGAV

public data class PomParseResult(
    val properties: Map<String, String>,
    val parent: ParentPom?,
    val dependencies: List<RawBomDependency>,
)

public data class ParentPom(
    val gav: MavenGAV,
)

public data class RawBomDependency(
    val coordinate: MavenCoordinate,
    val version: String,
    val scope: String?,
    val type: String?,
)
