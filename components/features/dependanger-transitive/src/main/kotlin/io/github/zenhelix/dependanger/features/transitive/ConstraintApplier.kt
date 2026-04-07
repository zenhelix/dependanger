package io.github.zenhelix.dependanger.features.transitive

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.zenhelix.dependanger.core.model.Constraint
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
        val parts = constraint.coordinates.split(":", limit = 2)
        if (parts.size != 2) {
            logger.warn { "Invalid version constraint coordinate: '${constraint.coordinates}', expected 'group:artifact'" }
            return trees
        }
        val (group, artifact) = parts
        val version = extractLiteralVersion(constraint.version) ?: return trees

        return trees.map { tree -> applyVersionToTree(tree, group, artifact, version) }
    }

    private fun applyVersionToTree(
        tree: TransitiveTree,
        group: String,
        artifact: String,
        version: String,
    ): TransitiveTree =
        if (tree.group == group && tree.artifact == artifact) {
            tree.copy(version = version)
        } else {
            tree.copy(children = tree.children.map { applyVersionToTree(it, group, artifact, version) })
        }

    private fun applyExclude(
        trees: List<TransitiveTree>,
        constraint: Constraint.Exclude,
    ): List<TransitiveTree> =
        trees
            .filter { !(it.group == constraint.group && it.artifact == constraint.artifact) }
            .map { tree ->
                tree.copy(children = applyExclude(tree.children, constraint))
            }

    private fun applySubstitute(
        trees: List<TransitiveTree>,
        constraint: Constraint.Substitute,
    ): List<TransitiveTree> {
        val fromParts = constraint.from.split(":", limit = 2)
        if (fromParts.size != 2) {
            logger.warn { "Invalid substitute 'from' coordinate: '${constraint.from}', expected 'group:artifact'" }
            return trees
        }
        val (fromGroup, fromArtifact) = fromParts

        val toParts = constraint.to.split(":", limit = 3)
        if (toParts.size < 2) {
            logger.warn { "Invalid substitute 'to' coordinate: '${constraint.to}', expected 'group:artifact[:version]'" }
            return trees
        }
        val toGroup = toParts[0]
        val toArtifact = toParts[1]
        val toVersion = toParts.getOrNull(2)

        return trees.map { tree -> substituteInTree(tree, fromGroup, fromArtifact, toGroup, toArtifact, toVersion) }
    }

    private fun substituteInTree(
        tree: TransitiveTree,
        fromGroup: String,
        fromArtifact: String,
        toGroup: String,
        toArtifact: String,
        toVersion: String?,
    ): TransitiveTree =
        if (tree.group == fromGroup && tree.artifact == fromArtifact) {
            tree.copy(
                group = toGroup,
                artifact = toArtifact,
                version = toVersion ?: tree.version,
            )
        } else {
            tree.copy(
                children = tree.children.map {
                    substituteInTree(it, fromGroup, fromArtifact, toGroup, toArtifact, toVersion)
                }
            )
        }

    private fun extractLiteralVersion(ref: VersionReference?): String? =
        when (ref) {
            is VersionReference.Literal -> ref.version
            else                        -> null
        }
}
