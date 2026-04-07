package io.github.zenhelix.dependanger.effective.serialization

import io.github.zenhelix.dependanger.core.exception.DependangerException

public class EffectiveMetadataReadException(
    message: String,
    cause: Throwable? = null,
) : DependangerException(message, cause)
