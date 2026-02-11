package io.github.zenhelix.dependanger.metadata

public open class MetadataSerializationException(
    message: String,
    cause: Throwable?,
) : RuntimeException(message, cause)

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
