package io.github.zenhelix.dependanger.metadata

import io.github.zenhelix.dependanger.core.exception.DependangerException

public open class MetadataSerializationException(
    message: String,
    cause: Throwable?,
) : DependangerException(message, cause)

public class MetadataDeserializationException(
    message: String,
    cause: Throwable?,
) : MetadataSerializationException(message, cause)

public class MetadataWriteException(
    message: String,
    cause: Throwable?,
) : MetadataSerializationException(message, cause)

public class MetadataReadException(
    message: String,
    cause: Throwable?,
) : MetadataSerializationException(message, cause)
