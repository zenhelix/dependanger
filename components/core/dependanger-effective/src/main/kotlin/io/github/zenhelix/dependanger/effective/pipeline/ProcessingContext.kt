package io.github.zenhelix.dependanger.effective.pipeline

import io.github.zenhelix.dependanger.core.model.Settings
import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata

public class ProcessingContextKey<T>(public val name: String)

public data class ProcessingContext(
    val originalMetadata: DependangerMetadata,
    val settings: Settings,
    val environment: ProcessingEnvironment,
    val activeDistribution: String? = null,
) {
    private val properties: MutableMap<ProcessingContextKey<*>, Any> = mutableMapOf()

    public operator fun <T : Any> set(key: ProcessingContextKey<T>, value: T) {
        properties[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    public operator fun <T : Any> get(key: ProcessingContextKey<T>): T? = properties[key] as? T
}
