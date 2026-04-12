package io.github.zenhelix.dependanger.http.client

import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpStatusCode
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

public class CircuitBreakerOpenException(
    public val host: String,
) : IOException("Circuit breaker is open for host: $host")

public class CircuitBreakerPluginConfig {
    public var failureThreshold: Int = CircuitBreakerConfig.DEFAULT_FAILURE_THRESHOLD
    public var openDurationMs: Long = CircuitBreakerConfig.DEFAULT_OPEN_DURATION_MS
    public var halfOpenMaxProbes: Int = CircuitBreakerConfig.DEFAULT_HALF_OPEN_MAX_PROBES

    internal fun toConfig(): CircuitBreakerConfig = CircuitBreakerConfig(
        failureThreshold = failureThreshold,
        openDurationMs = openDurationMs,
        halfOpenMaxProbes = halfOpenMaxProbes,
    )
}

public val CircuitBreakerPlugin: ClientPlugin<CircuitBreakerPluginConfig> = createClientPlugin("CircuitBreaker", ::CircuitBreakerPluginConfig) {
    val config = pluginConfig.toConfig()
    val states = ConcurrentHashMap<String, CircuitBreakerState>()

    fun stateFor(host: String): CircuitBreakerState =
        states.computeIfAbsent(host) { CircuitBreakerState(config) }

    fun isFailureStatus(status: HttpStatusCode): Boolean =
        status.value >= 500 || status == HttpStatusCode.TooManyRequests

    onRequest { request, _ ->
        val host = request.url.host
        val state = stateFor(host)
        if (!state.canAttempt()) {
            throw CircuitBreakerOpenException(host)
        }
    }

    onResponse { response ->
        val host = response.call.request.url.host
        val state = stateFor(host)
        if (isFailureStatus(response.status)) {
            state.recordFailure()
        } else {
            state.recordSuccess()
        }
    }
}
