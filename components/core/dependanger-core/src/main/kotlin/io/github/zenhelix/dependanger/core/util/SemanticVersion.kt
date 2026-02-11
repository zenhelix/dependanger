package io.github.zenhelix.dependanger.core.util

import kotlinx.serialization.Serializable

/**
 * Парсированная семантическая версия.
 *
 * Поддерживаемые форматы:
 * - `1.0.0`, `1.0`, `1`
 * - `1.0.0-RC1`, `1.0.0.RELEASE`, `1.0.0-beta.2`
 * - `2.1.20`, `3.4.0.Final`
 *
 * Суффиксы RELEASE, FINAL, GA удаляются при парсинге (считаются стабильными).
 */
@Serializable
public data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val qualifier: String?,
) : Comparable<SemanticVersion> {

    override fun compareTo(other: SemanticVersion): Int {
        if (major != other.major) return major.compareTo(other.major)
        if (minor != other.minor) return minor.compareTo(other.minor)
        if (patch != other.patch) return patch.compareTo(other.patch)
        // stable (qualifier=null) > prerelease (qualifier!=null)
        return when {
            qualifier == null && other.qualifier == null -> 0
            qualifier == null                            -> 1   // this is stable, other is prerelease
            other.qualifier == null                      -> -1
            else                                         -> qualifier.compareTo(other.qualifier)
        }
    }

    override fun toString(): String = buildString {
        append("$major.$minor.$patch")
        qualifier?.let { append("-$it") }
    }
}
