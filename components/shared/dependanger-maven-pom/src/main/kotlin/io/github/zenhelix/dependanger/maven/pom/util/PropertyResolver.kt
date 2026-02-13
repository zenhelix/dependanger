package io.github.zenhelix.dependanger.maven.pom.util

public class PropertyResolutionException(
    message: String,
) : Exception(message)

public class PropertyResolver(
    private val properties: Map<String, String>,
) {
    public fun resolve(value: String): String {
        return PROPERTY_PATTERN.replace(value) { match ->
            val propName = match.groupValues[1]
            properties[propName]
                ?: throw PropertyResolutionException("Unresolved property '$propName'")
        }
    }

    public fun resolveOrNull(value: String): String? {
        return try {
            resolve(value)
        } catch (_: PropertyResolutionException) {
            null
        }
    }

    private companion object {
        private val PROPERTY_PATTERN: Regex = Regex("""\$\{([^}]+)\}""")
    }
}
