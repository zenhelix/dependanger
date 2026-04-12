package io.github.zenhelix.dependanger.http.client

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CircuitBreakerStateTest {

    private val config = CircuitBreakerConfig(
        failureThreshold = 3,
        openDurationMs = 100,
        halfOpenMaxProbes = 1,
    )

    @Nested
    inner class ClosedState {

        @Test
        fun `new state starts CLOSED and allows attempts`() {
            val state = CircuitBreakerState(config)

            assertThat(state.canAttempt()).isTrue()
            assertThat(state.status).isEqualTo(CircuitBreakerStatus.CLOSED)
        }

        @Test
        fun `failures below threshold keep state CLOSED`() {
            val state = CircuitBreakerState(config)

            state.recordFailure()
            state.recordFailure()

            assertThat(state.status).isEqualTo(CircuitBreakerStatus.CLOSED)
            assertThat(state.canAttempt()).isTrue()
        }

        @Test
        fun `success resets failure count`() {
            val state = CircuitBreakerState(config)

            state.recordFailure()
            state.recordFailure()
            state.recordSuccess()
            state.recordFailure()
            state.recordFailure()

            assertThat(state.status).isEqualTo(CircuitBreakerStatus.CLOSED)
        }
    }

    @Nested
    inner class OpenState {

        @Test
        fun `transitions to OPEN after reaching failure threshold`() {
            val state = CircuitBreakerState(config)

            repeat(3) { state.recordFailure() }

            assertThat(state.status).isEqualTo(CircuitBreakerStatus.OPEN)
            assertThat(state.canAttempt()).isFalse()
        }

        @Test
        fun `OPEN state rejects attempts`() {
            val state = CircuitBreakerState(config)
            repeat(3) { state.recordFailure() }

            assertThat(state.canAttempt()).isFalse()
        }
    }

    @Nested
    inner class HalfOpenState {

        @Test
        fun `transitions to HALF_OPEN after open duration elapsed`() {
            val state = CircuitBreakerState(config)
            repeat(3) { state.recordFailure() }

            Thread.sleep(150)

            assertThat(state.canAttempt()).isTrue()
            assertThat(state.status).isEqualTo(CircuitBreakerStatus.HALF_OPEN)
        }

        @Test
        fun `success in HALF_OPEN transitions to CLOSED`() {
            val state = CircuitBreakerState(config)
            repeat(3) { state.recordFailure() }
            Thread.sleep(150)
            state.canAttempt()

            state.recordSuccess()

            assertThat(state.status).isEqualTo(CircuitBreakerStatus.CLOSED)
            assertThat(state.canAttempt()).isTrue()
        }

        @Test
        fun `failure in HALF_OPEN transitions back to OPEN`() {
            val state = CircuitBreakerState(config)
            repeat(3) { state.recordFailure() }
            Thread.sleep(150)
            state.canAttempt()

            state.recordFailure()

            assertThat(state.status).isEqualTo(CircuitBreakerStatus.OPEN)
            assertThat(state.canAttempt()).isFalse()
        }

        @Test
        fun `limits probe count in HALF_OPEN`() {
            val state = CircuitBreakerState(config)
            repeat(3) { state.recordFailure() }
            Thread.sleep(150)

            assertThat(state.canAttempt()).isTrue()  // first probe allowed
            assertThat(state.canAttempt()).isFalse() // second probe rejected (maxProbes=1)
        }
    }
}
