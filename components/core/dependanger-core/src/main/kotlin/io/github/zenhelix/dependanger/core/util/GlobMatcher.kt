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
    public fun matches(pattern: String, group: String, artifact: String): Boolean = TODO()

    /**
     * Проверяет совпадение строки с glob-паттерном.
     *
     * @param pattern glob-паттерн (может содержать * и ?)
     * @param value проверяемая строка
     * @return true если строка соответствует паттерну
     */
    public fun matchesGlob(pattern: String, value: String): Boolean = TODO()

    /**
     * Проверяет совпадение строки координат "group:artifact" с glob-паттерном.
     * Удобный метод для случаев, когда координаты уже объединены.
     */
    public fun matchesCoordinate(pattern: String, coordinate: String): Boolean = TODO()
}
