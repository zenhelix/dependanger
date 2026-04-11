package io.github.zenhelix.dependanger.features.transitive

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.zenhelix.dependanger.core.model.Constraint
import io.github.zenhelix.dependanger.core.model.MavenCoordinate
import io.github.zenhelix.dependanger.core.model.VersionReference
import io.github.zenhelix.dependanger.feature.model.transitive.TransitiveTree

private val logger = KotlinLogging.logger {}

internal object ConstraintApplier {

    fun apply(trees: List<TransitiveTree>, constraints: List<Constraint>): List<TransitiveTree> =
        constraints.fold(trees) { acc, constraint ->
            when (constraint) {
                is Constraint.VersionConstraintDef -> applyVersionConstraint(acc, constraint)
                is Constraint.Exclude              -> applyExclude(acc, constraint)
                is Constraint.Substitute           -> applySubstitute(acc, constraint)
            }
        }

    /** Applies [constraints] to [tree]'s children only, leaving the root untouched. */
    fun applyToChildren(tree: TransitiveTree, constraints: List<Constraint>): TransitiveTree {
        if (constraints.isEmpty()) return tree
        return tree.copy(children = apply(tree.children, constraints))
    }

    private fun applyVersionConstraint(
        trees: List<TransitiveTree>,
        constraint: Constraint.VersionConstraintDef,
    ): List<TransitiveTree> {
        val coordinate = constraint.coordinate
        val version = extractLiteralVersion(constraint.version) ?: return trees

        return trees.map { tree -> applyVersionToTree(tree, coordinate, version) }
    }

    private fun applyVersionToTree(
        tree: TransitiveTree,
        coordinate: MavenCoordinate,
        version: String,
    ): TransitiveTree =
        if (tree.coordinate == coordinate) {
            tree.copy(version = version)
        } else {
            tree.copy(children = tree.children.map { applyVersionToTree(it, coordinate, version) })
        }

    private fun applyExclude(
        trees: List<TransitiveTree>,
        constraint: Constraint.Exclude,
    ): List<TransitiveTree> =
        trees
            .filter { it.coordinate != constraint.coordinate }
            .map { tree ->
                tree.copy(children = applyExclude(tree.children, constraint))
            }

    private fun applySubstitute(
        trees: List<TransitiveTree>,
        constraint: Constraint.Substitute,
    ): List<TransitiveTree> =
        trees.map { tree -> substituteInTree(tree, constraint.from, constraint.to, constraint.toVersion) }

    private fun substituteInTree(
        tree: TransitiveTree,
        from: MavenCoordinate,
        to: MavenCoordinate,
        toVersion: String?,
    ): TransitiveTree =
        if (tree.coordinate == from) {
            tree.copy(
                coordinate = to,
                version = toVersion ?: tree.version,
            )
        } else {
            tree.copy(
                children = tree.children.map { substituteInTree(it, from, to, toVersion) }
            )
        }

    private fun extractLiteralVersion(ref: VersionReference?): String? =
        when (ref) {
            is VersionReference.Literal -> ref.version
            else                        -> null
        }
}
