package io.github.zenhelix.dependanger.maven.pom.parser

public class PomParseException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
