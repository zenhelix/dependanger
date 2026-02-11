package io.github.zenhelix.dependanger.effective.pipeline

import io.github.zenhelix.dependanger.core.model.Settings
import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata

public class ProcessingContextKey<T>(public val name: String)

public class ProcessingContext(
    public val originalMetadata: DependangerMetadata,
    public val settings: Settings,
    public val environment: ProcessingEnvironment,
    public val activeDistribution: String?,
    public val callback: ProcessingCallback?,
    private val properties: Map<ProcessingContextKey<*>, Any>,
) {
    @Suppress("UNCHECKED_CAST")
    public operator fun <T : Any> get(key: ProcessingContextKey<T>): T? = properties[key] as? T

    public fun <T : Any> with(key: ProcessingContextKey<T>, value: T): ProcessingContext =
        ProcessingContext(originalMetadata, settings, environment, activeDistribution, callback, properties + (key to value))
}
