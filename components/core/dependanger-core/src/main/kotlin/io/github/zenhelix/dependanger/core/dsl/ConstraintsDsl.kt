package io.github.zenhelix.dependanger.core.dsl

import io.github.zenhelix.dependanger.core.model.Constraint
import io.github.zenhelix.dependanger.core.model.MavenCoordinate
import io.github.zenhelix.dependanger.core.model.VersionReference

@DependangerDslMarker
public class ConstraintsDsl {
    private val _constraints: MutableList<Constraint> = mutableListOf()
    public val constraints: List<Constraint> get() = _constraints.toList()

    public fun constraint(coordinates: String, version: VersionReference) {
        _constraints.add(Constraint.VersionConstraintDef(coordinate = MavenCoordinate.parse(coordinates), version = version, because = null))
    }

    public fun constraint(coordinates: String, version: String) {
        _constraints.add(
            Constraint.VersionConstraintDef(
                coordinate = MavenCoordinate.parse(coordinates),
                version = VersionReference.Literal(version),
                because = null
            )
        )
    }

    public fun constraint(coordinates: String, block: ConstraintDefDsl.() -> Unit) {
        val dsl = ConstraintDefDsl().apply(block)
        _constraints.add(
            Constraint.VersionConstraintDef(
                coordinate = MavenCoordinate.parse(coordinates),
                version = dsl.version?.let { VersionReference.Literal(it) },
                because = dsl.because,
            )
        )
    }

    public fun exclude(coordinates: String) {
        _constraints.add(Constraint.Exclude(coordinate = MavenCoordinate.parse(coordinates)))
    }

    public fun exclude(group: String, artifact: String) {
        _constraints.add(Constraint.Exclude(coordinate = MavenCoordinate(group, artifact)))
    }

    public fun substitute(from: String, to: String) {
        val toCoordinate = parseSubstituteTo(to)
        _constraints.add(Constraint.Substitute(from = MavenCoordinate.parse(from), to = toCoordinate.first, toVersion = toCoordinate.second, because = null))
    }

    public fun substitute(from: String, to: String, block: SubstituteDsl.() -> Unit) {
        val dsl = SubstituteDsl().apply(block)
        val toCoordinate = parseSubstituteTo(to)
        _constraints.add(
            Constraint.Substitute(
                from = MavenCoordinate.parse(from),
                to = toCoordinate.first,
                toVersion = toCoordinate.second,
                because = dsl.because
            )
        )
    }

    private fun parseSubstituteTo(to: String): Pair<MavenCoordinate, String?> {
        val parts = to.split(":", limit = 3)
        require(parts.size >= 2) { "Substitute 'to' must be group:artifact or group:artifact:version" }
        return MavenCoordinate(parts[0], parts[1]) to parts.getOrNull(2)
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
