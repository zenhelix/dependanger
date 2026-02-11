package io.github.zenhelix.dependanger.metadata

public open class MetadataSerializationException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

public class MetadataDeserializationException(
    message: String,
    cause: Throwable? = null,
) : MetadataSerializationException(message, cause)

public class MetadataWriteException(
    message: String,
    cause: Throwable? = null,
) : MetadataSerializationException(message, cause)

public class MetadataReadException(
    message: String,
    cause: Throwable? = null,
) : MetadataSerializationException(message, cause)
