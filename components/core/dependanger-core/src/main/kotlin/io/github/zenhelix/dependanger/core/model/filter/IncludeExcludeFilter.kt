package io.github.zenhelix.dependanger.core.model.filter

import io.github.zenhelix.dependanger.core.model.MavenCoordinate

/**
 * Common contract for filters with include/exclude sets.
 *
 * Matching semantics:
 * - Empty includes = everything passes (no whitelist restriction)
 * - Empty excludes = nothing excluded
 * - Non-empty includes = only matching items pass (whitelist)
 * - Non-empty excludes = matching items blocked (blacklist)
 */
public interface IncludeExcludeFilter {
    public val includes: Set<String>
    public val excludes: Set<String>
}

/**
 * Exact match: value must be in includes (if non-empty) and not in excludes (if non-empty).
 */
public fun IncludeExcludeFilter.matchesExact(value: String): Boolean {
    val passesIncludes = includes.isEmpty() || value in includes
    val passesExcludes = excludes.isEmpty() || value !in excludes
    return passesIncludes && passesExcludes
}

/**
 * Set membership match: at least one of [values] must be in includes (if non-empty),
 * and none of [values] must be in excludes (if non-empty).
 *
 * Useful when an item belongs to multiple groups (e.g., a library in multiple bundles).
 * Short-circuits without allocating intermediate sets.
 */
public fun IncludeExcludeFilter.matchesAny(values: Set<String>): Boolean {
    val passesIncludes = includes.isEmpty() || values.any { it in includes }
    val passesExcludes = excludes.isEmpty() || values.none { it in excludes }
    return passesIncludes && passesExcludes
}

/**
 * Predicate-based match using [MavenCoordinate]: uses [predicate] to test each pattern against the coordinate.
 */
public fun IncludeExcludeFilter.matchesWithPredicate(
    coordinate: MavenCoordinate,
    predicate: (pattern: String, coordinate: MavenCoordinate) -> Boolean,
): Boolean {
    val passesIncludes = includes.isEmpty() || includes.any { predicate(it, coordinate) }
    val passesExcludes = excludes.isEmpty() || excludes.none { predicate(it, coordinate) }
    return passesIncludes && passesExcludes
}
