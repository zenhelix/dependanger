package io.github.zenhelix.dependanger.features.updates

public sealed interface VersionCacheResult {
    public data class Hit(val result: VersionFetchResult) : VersionCacheResult
    public data object Miss : VersionCacheResult
    public data class Corrupted(val key: String, val error: String) : VersionCacheResult
}
