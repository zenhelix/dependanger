package io.github.zenhelix.dependanger.core.util

/**
 * Утилита для парсинга и сравнения семантических версий.
 */
public object VersionComparator {

    /**
     * Парсит строку версии в [SemanticVersion].
     * Возвращает null если парсинг невозможен (нечисловой major).
     */
    public fun parse(version: String): SemanticVersion? = TODO()

    /**
     * Определяет, является ли версия предварительным релизом.
     */
    public fun isPrerelease(version: String): Boolean = TODO()

    /**
     * Сравнивает две строковые версии.
     * Если парсинг невозможен — fallback на строковое сравнение.
     */
    public fun compare(a: String, b: String): Int = TODO()

    /**
     * Классифицирует тип обновления между двумя версиями.
     */
    public fun classifyUpdate(current: SemanticVersion, latest: SemanticVersion): UpdateType = TODO()

    /**
     * Извлекает major-компонент версии.
     */
    public fun parseMajor(version: String): String = TODO()

    /**
     * Извлекает major.minor компонент версии.
     */
    public fun parseMajorMinor(version: String): String = TODO()

    /**
     * Выбирает наивысшую версию из списка.
     */
    public fun selectHighest(versions: List<String>): String? = TODO()
}
