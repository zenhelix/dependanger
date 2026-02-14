package io.github.zenhelix.dependanger.features.license

/**
 * Parses SPDX license expressions.
 *
 * Supports basic OR-expressions: "MIT OR Apache-2.0" -> ["MIT", "Apache-2.0"]
 * AND-expressions are NOT split (treated as single license): "MIT AND Apache-2.0" -> ["MIT AND Apache-2.0"]
 */
public object SpdxExpressionParser {

    private val OR_SEPARATOR: Regex = Regex("\\s+OR\\s+", RegexOption.IGNORE_CASE)

    /**
     * Parses an SPDX expression into individual license identifiers.
     * Splits only on " OR " (case-insensitive).
     * Returns empty list for blank input.
     */
    public fun parse(expression: String): List<String> {
        if (expression.isBlank()) return emptyList()

        return expression
            .split(OR_SEPARATOR)
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }
}
