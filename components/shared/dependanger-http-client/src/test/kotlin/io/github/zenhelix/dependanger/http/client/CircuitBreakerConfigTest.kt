package io.github.zenhelix.dependanger.http.client

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CircuitBreakerConfigTest {

    @Nested
    inner class Defaults {

        @Test
        fun `default config has expected values`() {
            val config = CircuitBreakerConfig()

            assertThat(config.failureThreshold).isEqualTo(5)
            assertThat(config.openDurationMs).isEqualTo(60_000L)
            assertThat(config.halfOpenMaxProbes).isEqualTo(1)
        }
    }

    @Nested
    inner class Validation {

        @Test
        fun `failureThreshold must be at least 1`() {
            assertThatThrownBy { CircuitBreakerConfig(failureThreshold = 0) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("failureThreshold")
        }

        @Test
        fun `openDurationMs must be positive`() {
            assertThatThrownBy { CircuitBreakerConfig(openDurationMs = 0) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("openDurationMs")
        }

        @Test
        fun `halfOpenMaxProbes must be at least 1`() {
            assertThatThrownBy { CircuitBreakerConfig(halfOpenMaxProbes = 0) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("halfOpenMaxProbes")
        }

        @Test
        fun `valid custom values are accepted`() {
            val config = CircuitBreakerConfig(
                failureThreshold = 10,
                openDurationMs = 120_000L,
                halfOpenMaxProbes = 3,
            )

            assertThat(config.failureThreshold).isEqualTo(10)
            assertThat(config.openDurationMs).isEqualTo(120_000L)
            assertThat(config.halfOpenMaxProbes).isEqualTo(3)
        }
    }
}
