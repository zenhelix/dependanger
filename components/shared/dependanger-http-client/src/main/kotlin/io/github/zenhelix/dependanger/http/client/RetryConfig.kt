package io.github.zenhelix.dependanger.http.client

public data class RetryConfig(
    public val maxRetries: Int = DEFAULT_MAX_RETRIES,
    public val initialDelayMs: Long = DEFAULT_INITIAL_DELAY_MS,
    public val maxDelayMs: Long = DEFAULT_MAX_DELAY_MS,
    public val backoffMultiplier: Double = DEFAULT_BACKOFF_MULTIPLIER,
) {
    init {
        require(maxRetries >= 1) { "maxRetries must be at least 1, got $maxRetries" }
        require(initialDelayMs > 0) { "initialDelayMs must be positive, got $initialDelayMs" }
        require(maxDelayMs >= initialDelayMs) { "maxDelayMs must be >= initialDelayMs, got maxDelayMs=$maxDelayMs, initialDelayMs=$initialDelayMs" }
        require(backoffMultiplier >= 1.0) { "backoffMultiplier must be >= 1.0, got $backoffMultiplier" }
    }

    public companion object {
        public const val DEFAULT_MAX_RETRIES: Int = 3
        public const val DEFAULT_INITIAL_DELAY_MS: Long = 1_000L
        public const val DEFAULT_MAX_DELAY_MS: Long = 30_000L
        public const val DEFAULT_BACKOFF_MULTIPLIER: Double = 2.0
    }
}
