package io.github.zenhelix.dependanger.api

public open class DependangerException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

public class DependangerValidationException(
    message: String,
    public val errors: List<String>,
) : DependangerException(message)

public class DependangerProcessingException(
    message: String,
    public val phase: String? = null,
    cause: Throwable? = null,
) : DependangerException(message, cause)

public class DependangerConfigurationException(
    message: String,
    cause: Throwable? = null,
) : DependangerException(message, cause)
