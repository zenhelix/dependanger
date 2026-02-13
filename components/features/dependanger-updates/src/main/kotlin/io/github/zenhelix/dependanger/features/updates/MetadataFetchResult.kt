package io.github.zenhelix.dependanger.features.updates

public sealed interface MetadataFetchResult {
    public data class Success(val versions: List<String>, val repository: String) : MetadataFetchResult
    public data object NotFound : MetadataFetchResult
    public data class Failed(val error: String) : MetadataFetchResult
}
