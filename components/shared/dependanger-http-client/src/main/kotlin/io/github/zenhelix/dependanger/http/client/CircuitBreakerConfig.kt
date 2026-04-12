package io.github.zenhelix.dependanger.http.client

public data class CircuitBreakerConfig(
    public val failureThreshold: Int = DEFAULT_FAILURE_THRESHOLD,
    public val openDurationMs: Long = DEFAULT_OPEN_DURATION_MS,
    public val halfOpenMaxProbes: Int = DEFAULT_HALF_OPEN_MAX_PROBES,
) {
    init {
        require(failureThreshold >= 1) { "failureThreshold must be at least 1, got $failureThreshold" }
        require(openDurationMs > 0) { "openDurationMs must be positive, got $openDurationMs" }
        require(halfOpenMaxProbes >= 1) { "halfOpenMaxProbes must be at least 1, got $halfOpenMaxProbes" }
    }

    public companion object {
        public const val DEFAULT_FAILURE_THRESHOLD: Int = 5
        public const val DEFAULT_OPEN_DURATION_MS: Long = 60_000L
        public const val DEFAULT_HALF_OPEN_MAX_PROBES: Int = 1
    }
}
