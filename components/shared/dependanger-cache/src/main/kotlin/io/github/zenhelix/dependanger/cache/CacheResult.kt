package io.github.zenhelix.dependanger.cache

public sealed interface CacheResult<out T> {
    public data class Hit<T>(val data: T) : CacheResult<T>
    public data object Miss : CacheResult<Nothing>
    public data class Corrupted(val key: String, val error: String) : CacheResult<Nothing>
}
