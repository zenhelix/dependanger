package io.github.zenhelix.dependanger.maven.client.model

public sealed interface DownloadResult {
    public data class Success(val content: String) : DownloadResult
    public data object NotFound : DownloadResult
    public data class AuthRequired(val url: String, val statusCode: Int) : DownloadResult
    public data class Failed(val error: String) : DownloadResult
}
