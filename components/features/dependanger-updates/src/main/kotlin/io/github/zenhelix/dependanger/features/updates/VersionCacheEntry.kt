package io.github.zenhelix.dependanger.features.updates

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
internal data class VersionCacheEntry(
    val versions: List<String>,
    val fetchedAt: Instant,
    val repository: String?,
)
