package io.github.zenhelix.dependanger.cli

import io.github.zenhelix.dependanger.core.model.Diagnostics

public sealed class CliException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {

    public class FileNotFound(
        public val path: String,
    ) : CliException("File not found: $path")

    public class FileAlreadyExists(
        public val path: String,
    ) : CliException("File already exists: $path (use --force to overwrite)")

    public class ParseError(
        detail: String,
        cause: Throwable,
    ) : CliException("Parse error: $detail", cause)

    public class ValidationFailed(
        public val diagnostics: Diagnostics,
    ) : CliException("Validation failed")

    public class DuplicateAlias(
        public val entity: String,
        public val alias: String,
    ) : CliException("$entity '$alias' already exists")

    public class AliasNotFound(
        public val entity: String,
        public val alias: String,
    ) : CliException("$entity '$alias' not found")

    public class ReferenceConflict(
        public val alias: String,
        public val refs: List<String>,
    ) : CliException("Cannot remove '$alias': referenced by ${refs.joinToString()}")

    public class ProcessingFailed(
        detail: String,
        cause: Throwable? = null,
    ) : CliException(detail, cause)

    public class InvalidArgument(
        detail: String,
    ) : CliException(detail)
}
