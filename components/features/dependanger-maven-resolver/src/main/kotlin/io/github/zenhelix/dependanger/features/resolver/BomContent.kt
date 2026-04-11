package io.github.zenhelix.dependanger.features.resolver

import io.github.zenhelix.dependanger.core.model.MavenCoordinate
import kotlinx.serialization.Serializable

@Serializable
public data class BomDependency(
    val coordinate: MavenCoordinate,
    val version: String,
)

@Serializable
public data class BomContent(
    val dependencies: List<BomDependency>,
    val properties: Map<String, String>,
) {
    public companion object {
        public val EMPTY: BomContent = BomContent(
            dependencies = emptyList(),
            properties = emptyMap(),
        )
    }
}
