package io.github.zenhelix.dependanger.core.dsl

import io.github.zenhelix.dependanger.core.model.Constraint
import io.github.zenhelix.dependanger.core.model.DeprecationInfo
import io.github.zenhelix.dependanger.core.model.Library
import io.github.zenhelix.dependanger.core.model.LicenseInfo
import io.github.zenhelix.dependanger.core.model.Requirements
import io.github.zenhelix.dependanger.core.model.VersionReference

@DependangerDslMarker
public class LibrariesDsl {
    public val libraries: MutableList<Library> = mutableListOf()

    public fun library(alias: String, coordinates: String) {
        val (group, artifact, version) = parseCoordinates(coordinates)
        libraries.add(
            Library(
                alias = alias, group = group, artifact = artifact, version = version,
                description = null, tags = emptySet(), requires = null, deprecation = null,
                license = null, constraints = emptyList(), isPlatform = false,
            )
        )
    }

    public fun library(alias: String, coordinates: String, version: VersionReference) {
        val (group, artifact, _) = parseCoordinates(coordinates)
        libraries.add(
            Library(
                alias = alias, group = group, artifact = artifact, version = version,
                description = null, tags = emptySet(), requires = null, deprecation = null,
                license = null, constraints = emptyList(), isPlatform = false,
            )
        )
    }

    public fun library(alias: String, coordinates: String, block: LibraryDsl.() -> Unit) {
        val (group, artifact, version) = parseCoordinates(coordinates)
        val dsl = LibraryDsl(version).apply(block)
        libraries.add(dsl.toLibrary(alias, group, artifact))
    }

    public fun library(alias: String, coordinates: String, version: VersionReference, block: LibraryDsl.() -> Unit) {
        val (group, artifact, _) = parseCoordinates(coordinates)
        val dsl = LibraryDsl(version).apply(block)
        libraries.add(dsl.toLibrary(alias, group, artifact))
    }

    public fun platformLibrary(alias: String, coordinates: String, version: VersionReference) {
        val (group, artifact, _) = parseCoordinates(coordinates)
        libraries.add(
            Library(
                alias = alias, group = group, artifact = artifact, version = version,
                description = null, tags = emptySet(), requires = null, deprecation = null,
                license = null, constraints = emptyList(), isPlatform = true,
            )
        )
    }

    private fun parseCoordinates(coordinates: String): Triple<String, String, VersionReference?> {
        val parts = coordinates.split(":")
        return when (parts.size) {
            2    -> Triple(parts[0], parts[1], null)
            3    -> Triple(parts[0], parts[1], VersionReference.Literal(parts[2]))
            else -> throw IllegalArgumentException("Invalid coordinates: $coordinates")
        }
    }
}

@DependangerDslMarker
public class LibraryDsl(private var version: VersionReference? = null) {
    public var description: String? = null
    public var tags: MutableSet<String> = mutableSetOf()
    public var requires: Requirements? = null
    public var deprecation: DeprecationInfo? = null
    public var license: LicenseInfo? = null
    public var constraints: MutableList<Constraint> = mutableListOf()

    public fun tags(vararg tags: String) {
        this.tags.addAll(tags)
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
        constraints.addAll(dsl.constraints)
    }

    public fun toLibrary(alias: String, group: String, artifact: String): Library = Library(
        alias = alias,
        group = group,
        artifact = artifact,
        version = version,
        description = description,
        tags = tags.toSet(),
        requires = requires,
        deprecation = deprecation,
        license = license,
        constraints = constraints.toList(),
        isPlatform = false,
    )
}
