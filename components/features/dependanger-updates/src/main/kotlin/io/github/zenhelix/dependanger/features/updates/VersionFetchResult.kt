package io.github.zenhelix.dependanger.features.updates

public data class VersionFetchResult(
    val versions: List<String>,
    val repository: String?,
)
