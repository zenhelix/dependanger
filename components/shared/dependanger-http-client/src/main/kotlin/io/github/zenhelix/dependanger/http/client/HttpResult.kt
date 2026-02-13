package io.github.zenhelix.dependanger.http.client

public sealed interface HttpResult<out T> {
    public data class Success<T>(val data: T) : HttpResult<T>
    public data object NotFound : HttpResult<Nothing>
    public data class AuthRequired(val url: String, val statusCode: Int) : HttpResult<Nothing>
    public data class RateLimited(val retryAfterMs: Long?) : HttpResult<Nothing>
    public data class Failed(val error: String, val cause: Throwable? = null) : HttpResult<Nothing>
}
