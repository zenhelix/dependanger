package io.github.zenhelix.dependanger.cache

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
public data class CacheMetadata(
    val fetchedAt: Instant,
    val isSnapshot: Boolean,
)
