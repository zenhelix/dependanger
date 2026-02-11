package io.github.zenhelix.dependanger.core.dsl

import io.github.zenhelix.dependanger.core.model.Constraint
import io.github.zenhelix.dependanger.core.model.VersionReference

@DependangerDslMarker
public class ConstraintsDsl {
    public val constraints: MutableList<Constraint> = mutableListOf()

    public fun constraint(coordinates: String, version: VersionReference) {
        constraints.add(Constraint.VersionConstraintDef(coordinates = coordinates, version = version, because = null))
    }

    public fun constraint(coordinates: String, version: String) {
        constraints.add(Constraint.VersionConstraintDef(coordinates = coordinates, version = VersionReference.Literal(version), because = null))
    }

    public fun constraint(coordinates: String, block: ConstraintDefDsl.() -> Unit) {
        val dsl = ConstraintDefDsl().apply(block)
        constraints.add(
            Constraint.VersionConstraintDef(
                coordinates = coordinates,
                version = dsl.version?.let { VersionReference.Literal(it) },
                because = dsl.because,
            )
        )
    }

    public fun exclude(coordinates: String) {
        val parts = coordinates.split(":")
        require(parts.size == 2) { "Exclude coordinates must be group:artifact" }
        constraints.add(Constraint.Exclude(group = parts[0], artifact = parts[1]))
    }

    public fun exclude(group: String, artifact: String) {
        constraints.add(Constraint.Exclude(group = group, artifact = artifact))
    }

    public fun substitute(from: String, to: String) {
        constraints.add(Constraint.Substitute(from = from, to = to, because = null))
    }

    public fun substitute(from: String, to: String, block: SubstituteDsl.() -> Unit) {
        val dsl = SubstituteDsl().apply(block)
        constraints.add(Constraint.Substitute(from = from, to = to, because = dsl.because))
    }
}

@DependangerDslMarker
public class ConstraintDefDsl {
    public var version: String? = null
    public var because: String? = null
}

@DependangerDslMarker
public class SubstituteDsl {
    public var because: String? = null
}
