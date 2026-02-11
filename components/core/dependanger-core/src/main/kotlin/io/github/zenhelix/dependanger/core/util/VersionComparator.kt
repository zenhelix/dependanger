package io.github.zenhelix.dependanger.core.util

/**
 * Утилита для парсинга и сравнения семантических версий.
 */
public object VersionComparator {

    private val STABLE_SUFFIXES = setOf("release", "final", "ga")

    private val PRERELEASE_PATTERNS = listOf(
        "alpha", "beta", "rc", "cr", "snapshot", "dev", "pre", "ea",
        "m\\d+",  // M1, M2, ...
    )

    private val prereleaseRegex: Regex = Regex(
        PRERELEASE_PATTERNS.joinToString("|") { "(?i)[-.]?$it" }
    )

    /**
     * Парсит строку версии в [SemanticVersion].
     * Возвращает null если парсинг невозможен (нечисловой major).
     */
    public fun parse(version: String): SemanticVersion? {
        // Удаляем стабильные суффиксы: 1.0.0.RELEASE -> 1.0.0
        val cleaned = version.replace(
            Regex("[-.]?(${STABLE_SUFFIXES.joinToString("|")})$", RegexOption.IGNORE_CASE), ""
        )
        val parts = cleaned.split(Regex("[.-]"), limit = 4)
        val major = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
        val qualifier = parts.getOrNull(3)
        return SemanticVersion(major, minor, patch, qualifier)
    }

    /**
     * Определяет, является ли версия предварительным релизом.
     */
    public fun isPrerelease(version: String): Boolean =
        prereleaseRegex.containsMatchIn(version)

    /**
     * Сравнивает две строковые версии.
     * Если парсинг невозможен — fallback на строковое сравнение.
     */
    public fun compare(a: String, b: String): Int {
        val va = parse(a)
        val vb = parse(b)
        return when {
            va != null && vb != null -> va.compareTo(vb)
            else                     -> a.compareTo(b)
        }
    }

    /**
     * Классифицирует тип обновления между двумя версиями.
     */
    public fun classifyUpdate(current: SemanticVersion, latest: SemanticVersion): UpdateType =
        when {
            latest.major > current.major -> UpdateType.MAJOR
            latest.minor > current.minor -> UpdateType.MINOR
            latest.patch > current.patch -> UpdateType.PATCH
            else                         -> UpdateType.PRERELEASE
        }

    /**
     * Извлекает major-компонент версии.
     */
    public fun parseMajor(version: String): String =
        version.split(".").firstOrNull() ?: version

    /**
     * Извлекает major.minor компонент версии.
     */
    public fun parseMajorMinor(version: String): String =
        version.split(".").take(2).joinToString(".")

    /**
     * Выбирает наивысшую версию из списка.
     */
    public fun selectHighest(versions: List<String>): String? =
        versions.maxWithOrNull(Comparator { a, b -> compare(a, b) })
}
