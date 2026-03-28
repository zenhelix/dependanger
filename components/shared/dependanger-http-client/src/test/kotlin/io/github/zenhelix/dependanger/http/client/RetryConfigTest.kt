package io.github.zenhelix.dependanger.http.client

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class RetryConfigTest {

    @Nested
    inner class DefaultValues {

        @Test
        fun `default values are correct`() {
            val config = RetryConfig()

            assertThat(config.maxRetries).isEqualTo(3)
            assertThat(config.initialDelayMs).isEqualTo(1_000L)
            assertThat(config.maxDelayMs).isEqualTo(30_000L)
            assertThat(config.backoffMultiplier).isEqualTo(2.0)
        }
    }

    @Nested
    inner class MaxRetriesValidation {

        @Test
        fun `maxRetries less than 1 throws`() {
            assertThatThrownBy { RetryConfig(maxRetries = 0) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("maxRetries must be at least 1")
        }

        @Test
        fun `maxRetries negative throws`() {
            assertThatThrownBy { RetryConfig(maxRetries = -1) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("maxRetries must be at least 1")
        }

        @Test
        fun `maxRetries equal to 1 is accepted`() {
            val config = RetryConfig(maxRetries = 1)
            assertThat(config.maxRetries).isEqualTo(1)
        }
    }

    @Nested
    inner class InitialDelayMsValidation {

        @Test
        fun `initialDelayMs zero throws`() {
            assertThatThrownBy { RetryConfig(initialDelayMs = 0) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("initialDelayMs must be positive")
        }

        @Test
        fun `initialDelayMs negative throws`() {
            assertThatThrownBy { RetryConfig(initialDelayMs = -100) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("initialDelayMs must be positive")
        }
    }

    @Nested
    inner class MaxDelayMsValidation {

        @Test
        fun `maxDelayMs less than initialDelayMs throws`() {
            assertThatThrownBy { RetryConfig(initialDelayMs = 1000, maxDelayMs = 500) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("maxDelayMs must be >= initialDelayMs")
        }

        @Test
        fun `maxDelayMs equal to initialDelayMs is accepted`() {
            val config = RetryConfig(initialDelayMs = 1000, maxDelayMs = 1000)
            assertThat(config.maxDelayMs).isEqualTo(1000L)
        }
    }

    @Nested
    inner class BackoffMultiplierValidation {

        @Test
        fun `backoffMultiplier less than 1 throws`() {
            assertThatThrownBy { RetryConfig(backoffMultiplier = 0.5) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("backoffMultiplier must be >= 1.0")
        }

        @Test
        fun `backoffMultiplier equal to 1 is accepted`() {
            val config = RetryConfig(backoffMultiplier = 1.0)
            assertThat(config.backoffMultiplier).isEqualTo(1.0)
        }
    }

    @Nested
    inner class ValidCustomValues {

        @Test
        fun `valid custom values accepted`() {
            val config = RetryConfig(
                maxRetries = 5,
                initialDelayMs = 500,
                maxDelayMs = 60_000,
                backoffMultiplier = 3.0,
            )

            assertThat(config.maxRetries).isEqualTo(5)
            assertThat(config.initialDelayMs).isEqualTo(500L)
            assertThat(config.maxDelayMs).isEqualTo(60_000L)
            assertThat(config.backoffMultiplier).isEqualTo(3.0)
        }
    }
}
