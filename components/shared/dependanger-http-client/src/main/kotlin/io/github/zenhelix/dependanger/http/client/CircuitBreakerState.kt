package io.github.zenhelix.dependanger.http.client

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

internal enum class CircuitBreakerStatus { CLOSED, OPEN, HALF_OPEN }

internal class CircuitBreakerState(private val config: CircuitBreakerConfig) {

    private val _status = AtomicReference(CircuitBreakerStatus.CLOSED)
    private val consecutiveFailures = AtomicInteger(0)
    private val openedAt = AtomicLong(0)
    private val probeCount = AtomicInteger(0)

    val status: CircuitBreakerStatus get() = _status.get()

    @Synchronized
    fun canAttempt(): Boolean = when (_status.get()) {
        CircuitBreakerStatus.CLOSED    -> true
        CircuitBreakerStatus.OPEN      -> {
            val elapsed = System.currentTimeMillis() - openedAt.get()
            if (elapsed >= config.openDurationMs) {
                _status.set(CircuitBreakerStatus.HALF_OPEN)
                probeCount.set(0)
                probeCount.incrementAndGet() <= config.halfOpenMaxProbes
            } else {
                false
            }
        }
        CircuitBreakerStatus.HALF_OPEN -> {
            probeCount.incrementAndGet() <= config.halfOpenMaxProbes
        }
    }

    @Synchronized
    fun recordSuccess() {
        consecutiveFailures.set(0)
        probeCount.set(0)
        _status.set(CircuitBreakerStatus.CLOSED)
    }

    @Synchronized
    fun recordFailure() {
        val failures = consecutiveFailures.incrementAndGet()
        if (_status.get() == CircuitBreakerStatus.HALF_OPEN) {
            _status.set(CircuitBreakerStatus.OPEN)
            openedAt.set(System.currentTimeMillis())
            probeCount.set(0)
        } else if (failures >= config.failureThreshold) {
            _status.set(CircuitBreakerStatus.OPEN)
            openedAt.set(System.currentTimeMillis())
        }
    }
}
