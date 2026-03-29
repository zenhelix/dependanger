package io.github.zenhelix.dependanger.features.transitive

import io.github.zenhelix.dependanger.core.model.Constraint
import io.github.zenhelix.dependanger.core.model.VersionReference
import io.github.zenhelix.dependanger.core.util.VersionComparator
import io.github.zenhelix.dependanger.feature.model.transitive.ConflictResolutionStrategy
import io.github.zenhelix.dependanger.feature.model.transitive.TransitiveTree
import io.github.zenhelix.dependanger.feature.model.transitive.VersionConflict

internal object ConflictDetector {

    fun detectConflicts(
        trees: List<TransitiveTree>,
        constraints: List<Constraint>,
        strategy: ConflictResolutionStrategy,
    ): List<VersionConflict> {
        val allVersions = collectAllVersions(trees)

        return allVersions
            .filter { (_, versions) -> versions.distinct().size > 1 }
            .map { (coordinate, versions) ->
                val (group, artifact) = coordinate.split(":", limit = 2)
                val distinctVersions = versions.distinct()
                resolveConflict(group, artifact, distinctVersions, constraints, strategy)
            }
    }

    private fun collectAllVersions(trees: List<TransitiveTree>): Map<String, List<String>> {
        val result = mutableMapOf<String, MutableList<String>>()

        fun traverse(tree: TransitiveTree) {
            val version = tree.version
            if (version != null && !tree.isCycle) {
                val coordinate = "${tree.group}:${tree.artifact}"
                result.getOrPut(coordinate) { mutableListOf() }.add(version)
            }
            tree.children.forEach { traverse(it) }
        }

        trees.forEach { traverse(it) }
        return result
    }

    private fun resolveConflict(
        group: String,
        artifact: String,
        distinctVersions: List<String>,
        constraints: List<Constraint>,
        strategy: ConflictResolutionStrategy,
    ): VersionConflict {
        val coordinate = "$group:$artifact"
        val versionConstraint = constraints
            .filterIsInstance<Constraint.VersionConstraintDef>()
            .firstOrNull { it.coordinates == coordinate }

        val resolvedVersion: String
        val resolution: ConflictResolutionStrategy

        if (versionConstraint != null) {
            resolvedVersion = extractConstraintVersion(versionConstraint)
                ?: selectByStrategy(distinctVersions, strategy)
            resolution = ConflictResolutionStrategy.CONSTRAINT
        } else {
            resolvedVersion = selectByStrategy(distinctVersions, strategy)
            resolution = strategy
        }

        return VersionConflict(
            group = group,
            artifact = artifact,
            requestedVersions = distinctVersions,
            resolvedVersion = resolvedVersion,
            resolution = resolution,
        )
    }

    private fun selectByStrategy(versions: List<String>, strategy: ConflictResolutionStrategy): String =
        when (strategy) {
            ConflictResolutionStrategy.HIGHEST    -> VersionComparator.selectHighest(versions) ?: versions.last()
            ConflictResolutionStrategy.FIRST      -> versions.first()
            ConflictResolutionStrategy.FAIL       -> versions.first()
            ConflictResolutionStrategy.CONSTRAINT -> VersionComparator.selectHighest(versions) ?: versions.last()
        }

    private fun extractConstraintVersion(constraint: Constraint.VersionConstraintDef): String? =
        when (val ref = constraint.version) {
            is VersionReference.Literal -> ref.version
            else                        -> null
        }
}
