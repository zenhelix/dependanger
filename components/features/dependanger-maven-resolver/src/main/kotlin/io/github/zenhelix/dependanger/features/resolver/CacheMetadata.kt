package io.github.zenhelix.dependanger.features.resolver

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
internal data class CacheMetadata(
    val fetchedAt: Instant,
    val isSnapshot: Boolean,
)
