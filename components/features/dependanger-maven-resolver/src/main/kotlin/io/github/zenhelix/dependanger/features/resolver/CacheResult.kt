package io.github.zenhelix.dependanger.features.resolver

public sealed interface CacheResult {
    public data class Hit(val content: BomContent) : CacheResult
    public data object Miss : CacheResult
    public data class Corrupted(val key: String, val error: String) : CacheResult
}
