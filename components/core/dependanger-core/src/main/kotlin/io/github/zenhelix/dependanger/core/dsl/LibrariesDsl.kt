package io.github.zenhelix.dependanger.core.dsl

import io.github.zenhelix.dependanger.core.model.Constraint
import io.github.zenhelix.dependanger.core.model.DeprecationInfo
import io.github.zenhelix.dependanger.core.model.Library
import io.github.zenhelix.dependanger.core.model.LicenseInfo
import io.github.zenhelix.dependanger.core.model.MavenCoordinate
import io.github.zenhelix.dependanger.core.model.Requirements
import io.github.zenhelix.dependanger.core.model.VersionReference

@DependangerDslMarker
public class LibrariesDsl {
    private val _libraries: MutableList<Library> = mutableListOf()
    private val _aliases: MutableSet<String> = mutableSetOf()
    public val libraries: List<Library> get() = _libraries.toList()

    public fun library(alias: String, coordinates: String) {
        requireUniqueAlias(alias, "Library alias", _aliases)
        val (coordinate, version) = parseCoordinates(coordinates)
        _libraries.add(
            Library(
                alias = alias, coordinate = coordinate, version = version,
                description = null, tags = emptySet(), requires = null, deprecation = null,
                license = null, constraints = emptyList(), isPlatform = false, ignoreUpdates = false,
            )
        )
    }

    public fun library(alias: String, coordinates: String, version: VersionReference) {
        requireUniqueAlias(alias, "Library alias", _aliases)
        val (coordinate, _) = parseCoordinates(coordinates)
        _libraries.add(
            Library(
                alias = alias, coordinate = coordinate, version = version,
                description = null, tags = emptySet(), requires = null, deprecation = null,
                license = null, constraints = emptyList(), isPlatform = false, ignoreUpdates = false,
            )
        )
    }

    public fun library(alias: String, coordinates: String, block: LibraryDsl.() -> Unit) {
        requireUniqueAlias(alias, "Library alias", _aliases)
        val (coordinate, version) = parseCoordinates(coordinates)
        val dsl = LibraryDsl(version).apply(block)
        _libraries.add(dsl.toLibrary(alias, coordinate))
    }

    public fun library(alias: String, coordinates: String, version: VersionReference, block: LibraryDsl.() -> Unit) {
        requireUniqueAlias(alias, "Library alias", _aliases)
        val (coordinate, _) = parseCoordinates(coordinates)
        val dsl = LibraryDsl(version).apply(block)
        _libraries.add(dsl.toLibrary(alias, coordinate))
    }

    public fun platformLibrary(alias: String, coordinates: String, version: VersionReference) {
        requireUniqueAlias(alias, "Library alias", _aliases)
        val (coordinate, _) = parseCoordinates(coordinates)
        _libraries.add(
            Library(
                alias = alias, coordinate = coordinate, version = version,
                description = null, tags = emptySet(), requires = null, deprecation = null,
                license = null, constraints = emptyList(), isPlatform = true, ignoreUpdates = false,
            )
        )
    }

    private fun parseCoordinates(raw: String): Pair<MavenCoordinate, VersionReference?> {
        val parts = raw.split(":")
        return when (parts.size) {
            2    -> {
                require(parts[0].isNotBlank()) { "Group must not be blank in coordinates '$raw'" }
                require(parts[1].isNotBlank()) { "Artifact must not be blank in coordinates '$raw'" }
                MavenCoordinate(parts[0], parts[1]) to null
            }

            3    -> {
                require(parts[0].isNotBlank()) { "Group must not be blank in coordinates '$raw'" }
                require(parts[1].isNotBlank()) { "Artifact must not be blank in coordinates '$raw'" }
                require(parts[2].isNotBlank()) { "Version must not be blank in coordinates '$raw'" }
                MavenCoordinate(parts[0], parts[1]) to VersionReference.Literal(parts[2])
            }

            else -> throw IllegalArgumentException("Invalid coordinates: $raw")
        }
    }
}

@DependangerDslMarker
public class LibraryDsl(private var version: VersionReference? = null) {
    public var description: String? = null
    private val _tags: MutableSet<String> = mutableSetOf()
    public val tags: Set<String> get() = _tags.toSet()
    public var requires: Requirements? = null
    public var deprecation: DeprecationInfo? = null
    public var license: LicenseInfo? = null
    public var ignoreUpdates: Boolean = false
    private val _constraints: MutableList<Constraint> = mutableListOf()
    public val constraints: List<Constraint> get() = _constraints.toList()

    public fun tags(vararg tags: String) {
        _tags.addAll(tags)
    }

    public fun requires(block: RequiresDsl.() -> Unit) {
        val dsl = RequiresDsl().apply(block)
        requires = dsl.toRequirements()
    }

    public fun deprecated(block: DeprecationDsl.() -> Unit) {
        val dsl = DeprecationDsl().apply(block)
        deprecation = dsl.toDeprecationInfo()
    }

    public fun deprecated(replacedBy: String? = null, message: String? = null) {
        deprecation = DeprecationInfo(replacedBy = replacedBy, message = message, since = null, removalVersion = null)
    }

    public fun license(block: LicenseDsl.() -> Unit) {
        val dsl = LicenseDsl().apply(block)
        license = dsl.toLicenseInfo()
    }

    public fun constraints(block: ConstraintsDsl.() -> Unit) {
        val dsl = ConstraintsDsl().apply(block)
        _constraints.addAll(dsl.constraints)
    }

    public fun toLibrary(alias: String, coordinate: MavenCoordinate): Library = Library(
        alias = alias,
        coordinate = coordinate,
        version = version,
        description = description,
        tags = tags,
        requires = requires,
        deprecation = deprecation,
        license = license,
        constraints = constraints,
        isPlatform = false,
        ignoreUpdates = ignoreUpdates,
    )
}
