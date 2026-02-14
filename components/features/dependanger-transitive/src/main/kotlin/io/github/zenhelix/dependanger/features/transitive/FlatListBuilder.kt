package io.github.zenhelix.dependanger.features.transitive

import io.github.zenhelix.dependanger.effective.model.EffectiveLibrary
import io.github.zenhelix.dependanger.features.transitive.model.FlatDependency
import io.github.zenhelix.dependanger.features.transitive.model.TransitiveTree

internal object FlatListBuilder {

    fun build(
        trees: List<TransitiveTree>,
        directLibraries: Collection<EffectiveLibrary>,
    ): List<FlatDependency> {
        val directCoordinates = directLibraries
            .map { "${it.group}:${it.artifact}" }
            .toSet()
        val seen = linkedMapOf<String, FlatDependency>()

        fun traverse(tree: TransitiveTree) {
            if (tree.isCycle) return

            val coordinate = "${tree.group}:${tree.artifact}"
            if (coordinate !in seen && tree.version != null) {
                seen[coordinate] = FlatDependency(
                    group = tree.group,
                    artifact = tree.artifact,
                    version = tree.version,
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
