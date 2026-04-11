package io.github.zenhelix.dependanger.features.transitive

import io.github.zenhelix.dependanger.core.model.MavenCoordinate
import io.github.zenhelix.dependanger.effective.model.EffectiveLibrary
import io.github.zenhelix.dependanger.feature.model.transitive.FlatDependency
import io.github.zenhelix.dependanger.feature.model.transitive.TransitiveTree

internal object FlatListBuilder {

    fun build(
        trees: List<TransitiveTree>,
        directLibraries: Collection<EffectiveLibrary>,
    ): List<FlatDependency> {
        val directCoordinates = directLibraries
            .map { it.coordinate }
            .toSet()
        val seen = linkedMapOf<MavenCoordinate, FlatDependency>()

        fun traverse(tree: TransitiveTree) {
            if (tree.isCycle) return

            val coordinate = tree.coordinate
            val version = tree.version
            if (coordinate !in seen && version != null) {
                seen[coordinate] = FlatDependency(
                    coordinate = coordinate,
                    version = version,
                    scope = tree.scope,
                    isDirectDependency = coordinate in directCoordinates,
                    isOptional = false,
                )
            }
            tree.children.forEach { traverse(it) }
        }

        trees.forEach { traverse(it) }
        return seen.values.toList()
    }
}
