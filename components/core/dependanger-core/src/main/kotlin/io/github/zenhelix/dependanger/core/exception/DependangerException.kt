package io.github.zenhelix.dependanger.core.exception

/**
 * Base exception for all Dependanger domain errors.
 * All modules in the Dependanger project should extend this exception
 * so that consumers can catch a single type for all domain errors.
 */
public open class DependangerException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
