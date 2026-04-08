package io.github.zenhelix.dependanger.cache

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
internal data class CacheEntryWrapper<T>(
    val content: T,
    val fetchedAt: Instant,
)
