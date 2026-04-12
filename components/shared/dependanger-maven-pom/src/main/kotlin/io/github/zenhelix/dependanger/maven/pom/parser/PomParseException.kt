package io.github.zenhelix.dependanger.maven.pom.parser

import io.github.zenhelix.dependanger.core.exception.DependangerException

public class PomParseException(
    message: String,
    cause: Throwable? = null,
) : DependangerException(message, cause)
