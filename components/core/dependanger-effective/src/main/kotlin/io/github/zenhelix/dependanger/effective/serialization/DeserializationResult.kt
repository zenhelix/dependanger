package io.github.zenhelix.dependanger.effective.serialization

import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata

public data class DeserializationResult(
    val metadata: EffectiveMetadata,
    val warnings: List<DeserializationWarning>,
) {
    public val hasWarnings: Boolean get() = warnings.isNotEmpty()
}

public data class DeserializationWarning(
    val extensionKey: String,
    val message: String,
    val cause: Exception? = null,
)
