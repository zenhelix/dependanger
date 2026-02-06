package io.github.zenhelix.dependanger.effective.pipeline

import io.github.zenhelix.dependanger.core.model.Settings
import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata

public class ProcessingContextKey<T>(public val name: String)

public class ProcessingContext(
    public val originalMetadata: DependangerMetadata,
    public val settings: Settings,
    public val environment: ProcessingEnvironment,
    public val activeDistribution: String? = null,
    public val callback: ProcessingCallback? = null,
) {
    private val properties: MutableMap<ProcessingContextKey<*>, Any> = mutableMapOf()

    public operator fun <T : Any> set(key: ProcessingContextKey<T>, value: T) {
        properties[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    public operator fun <T : Any> get(key: ProcessingContextKey<T>): T? = properties[key] as? T
}
