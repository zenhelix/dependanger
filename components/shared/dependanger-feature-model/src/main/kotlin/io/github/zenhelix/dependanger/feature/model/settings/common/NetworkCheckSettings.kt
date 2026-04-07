package io.github.zenhelix.dependanger.feature.model.settings.common

import io.github.zenhelix.dependanger.core.dsl.DependangerDslMarker

public const val NETWORK_DEFAULT_TIMEOUT_MS: Long = 30_000L
public const val NETWORK_DEFAULT_PARALLELISM: Int = 10
public const val NETWORK_DEFAULT_CACHE_TTL_HOURS: Long = 1L

/**
 * Abstract base for feature settings that perform network calls and use a local cache.
 *
 * NOT annotated with @Serializable — each concrete data class is serialized directly
 * by its own generated serializer. This base provides only a Kotlin-level contract.
 */
public abstract class NetworkCheckSettings {
    public abstract val enabled: Boolean
    public abstract val timeout: Long
    public abstract val parallelism: Int
    public abstract val cacheDirectory: String?
    public abstract val cacheTtlHours: Long
}

/**
 * Abstract DSL base for network check feature settings.
 * Concrete DSL classes extend this and add their feature-specific vars.
 */
@DependangerDslMarker
public abstract class NetworkCheckSettingsDsl {
    public var enabled: Boolean = false
    public var timeout: Long = NETWORK_DEFAULT_TIMEOUT_MS
    public var parallelism: Int = NETWORK_DEFAULT_PARALLELISM
    public var cacheDirectory: String? = null
    public var cacheTtlHours: Long = NETWORK_DEFAULT_CACHE_TTL_HOURS
}
