package io.github.zenhelix.dependanger.osv.client.model

import io.github.zenhelix.dependanger.core.model.MavenCoordinate

public data class OsvPackageQuery(
    val coordinate: MavenCoordinate,
    val version: String,
)
