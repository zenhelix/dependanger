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
            else -> compareQualifiersNaturally(qualifier, other.qualifier)
        }
    }

    override fun toString(): String = buildString {
        append("$major.$minor.$patch")
        qualifier?.let { append("-$it") }
    }

    private companion object {
        private val SEGMENT_PATTERN: Regex = Regex("""(\d+|\D+)""")

        private fun compareQualifiersNaturally(a: String, b: String): Int {
            val segmentsA = SEGMENT_PATTERN.findAll(a).map { it.value }.toList()
            val segmentsB = SEGMENT_PATTERN.findAll(b).map { it.value }.toList()
            val size = maxOf(segmentsA.size, segmentsB.size)

            for (i in 0 until size) {
                if (i >= segmentsA.size) return -1
                if (i >= segmentsB.size) return 1

                val sa = segmentsA[i]
                val sb = segmentsB[i]
                val na = sa.toLongOrNull()
                val nb = sb.toLongOrNull()

                val cmp = when {
                    na != null && nb != null -> na.compareTo(nb)
                    na != null               -> 1   // number > text
                    nb != null               -> -1  // text < number
                    else                     -> sa.compareTo(sb, ignoreCase = true)
                }
                if (cmp != 0) return cmp
            }
            return 0
        }
    }
}
