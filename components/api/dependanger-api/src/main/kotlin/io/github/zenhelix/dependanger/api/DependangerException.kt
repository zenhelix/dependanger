package io.github.zenhelix.dependanger.api

public open class DependangerException(
    message: String,
    cause: Throwable?,
) : RuntimeException(message, cause)

public class DependangerValidationException(
    message: String,
    public val errors: List<String>,
    cause: Throwable?,
) : DependangerException(message, cause)

public class DependangerProcessingException(
    message: String,
    public val phase: String?,
    cause: Throwable?,
) : DependangerException(message, cause)

public class DependangerGenerationException(
    message: String,
    public val generatorId: String?,
    cause: Throwable?,
) : DependangerException(message, cause)

public class DependangerConfigurationException(
    message: String,
    cause: Throwable?,
) : DependangerException(message, cause)

internal inline fun <T> wrapNonDependangerException(
    wrap: (Exception) -> DependangerException,
    block: () -> T,
): T = try {
    block()
} catch (e: DependangerException) {
    throw e
} catch (e: Exception) {
    throw wrap(e)
}
