package io.github.zenhelix.dependanger.core.util

/**
 * Glob-matcher для Maven-координат и строковых паттернов.
 *
 * Поддерживаемые wildcard-символы:
 * - `*` — любая подстрока (включая пустую)
 * - `?` — любой одиночный символ
 *
 * Примеры паттернов:
 * - `"org.springframework:*"` — все артефакты группы org.springframework
 * - `"org.springframework*:*"` — org.springframework И org.springframework.boot
 * - `"*:spring-*"` — все группы, артефакты начинающиеся с "spring-"
 * - `"org.jetbrains.kotlin:kotlin-stdlib"` — точное совпадение
 */
public object GlobMatcher {

    /**
     * Проверяет совпадение координат "group:artifact" с glob-паттерном.
     *
     * @param pattern glob-паттерн в формате "groupPattern:artifactPattern"
     * @param group Maven groupId библиотеки
     * @param artifact Maven artifactId библиотеки
     * @return true если координаты соответствуют паттерну
     */
    public fun matches(pattern: String, group: String, artifact: String): Boolean {
        val (patternGroup, patternArtifact) = pattern.split(":", limit = 2)
            .let { if (it.size == 2) it[0] to it[1] else it[0] to "*" }

        return matchesGlob(patternGroup, group) && matchesGlob(patternArtifact, artifact)
    }

    /**
     * Проверяет совпадение строки с glob-паттерном.
     *
     * @param pattern glob-паттерн (может содержать * и ?)
     * @param value проверяемая строка
     * @return true если строка соответствует паттерну
     */
    public fun matchesGlob(pattern: String, value: String): Boolean {
        if (pattern == "*") return true

        val regex = buildString {
            append("^")
            for (ch in pattern) {
                when (ch) {
                    '*'                                                         -> append(".*")
                    '?'                                                         -> append(".")
                    '.', '+', '^', '$', '{', '}', '(', ')', '|', '[', ']', '\\' ->
                        append("\\").append(ch)

                    else                                                        -> append(ch)
                }
            }
            append("$")
        }
        return Regex(regex).matches(value)
    }

    /**
     * Проверяет совпадение строки координат "group:artifact" с glob-паттерном.
     * Удобный метод для случаев, когда координаты уже объединены.
     */
    public fun matchesCoordinate(pattern: String, coordinate: String): Boolean {
        val parts = coordinate.split(":", limit = 2)
        return if (parts.size == 2) {
            matches(pattern, parts[0], parts[1])
        } else {
            matchesGlob(pattern, coordinate)
        }
    }
}
