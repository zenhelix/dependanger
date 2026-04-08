package io.github.zenhelix.dependanger.features.updates

import kotlinx.serialization.Serializable

@Serializable
public data class VersionFetchResult(
    val versions: List<String>,
    val repository: String?,
)
