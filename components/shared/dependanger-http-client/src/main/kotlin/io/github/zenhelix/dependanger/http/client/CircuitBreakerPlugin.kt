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
    public var config: CircuitBreakerConfig = CircuitBreakerConfig()
}

public val CircuitBreakerPlugin: ClientPlugin<CircuitBreakerPluginConfig> = createClientPlugin("CircuitBreaker", ::CircuitBreakerPluginConfig) {
    val config = pluginConfig.config
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
