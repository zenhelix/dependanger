package io.github.zenhelix.dependanger.core.model

import kotlinx.serialization.Serializable

@Serializable
public data class BomImport(
    val alias: String,
    val coordinate: MavenCoordinate,
    val version: VersionReference,
)
