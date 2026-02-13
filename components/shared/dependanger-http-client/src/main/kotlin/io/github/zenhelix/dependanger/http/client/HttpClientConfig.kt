package io.github.zenhelix.dependanger.http.client

public data class HttpClientConfig(
    public var connectTimeoutMs: Long = DEFAULT_CONNECT_TIMEOUT_MS,
    public var requestTimeoutMs: Long = DEFAULT_REQUEST_TIMEOUT_MS,
    public var keepAliveMs: Long = DEFAULT_KEEP_ALIVE_MS,
    public var jsonIgnoreUnknownKeys: Boolean = true,
) {
    public companion object {
        public const val DEFAULT_CONNECT_TIMEOUT_MS: Long = 30_000L
        public const val DEFAULT_REQUEST_TIMEOUT_MS: Long = 60_000L
        public const val DEFAULT_KEEP_ALIVE_MS: Long = 5_000L
    }
}
