package io.github.zenhelix.dependanger.http.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class CircuitBreakerPluginTest {

    private fun mockClient(
        config: CircuitBreakerConfig = CircuitBreakerConfig(failureThreshold = 2, openDurationMs = 100, halfOpenMaxProbes = 1),
        handler: (AtomicInteger) -> HttpStatusCode,
    ): Pair<HttpClient, AtomicInteger> {
        val requestCount = AtomicInteger(0)
        val client = HttpClient(MockEngine { _ ->
            val status = handler(requestCount)
            requestCount.incrementAndGet()
            respond(
                content = if (status == HttpStatusCode.OK) "ok" else "error",
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "text/plain"),
            )
        }) {
            install(CircuitBreakerPlugin) {
                this.config = config
            }
        }
        return client to requestCount
    }

    @Nested
    inner class ClosedCircuit {

        @Test
        fun `requests pass through when circuit is closed`() = runTest {
            val (client, count) = mockClient { HttpStatusCode.OK }

            val response = client.get("https://api.example.com/data")

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(response.bodyAsText()).isEqualTo("ok")
            assertThat(count.get()).isEqualTo(1)
        }
    }

    @Nested
    inner class OpenCircuit {

        @Test
        fun `circuit opens after failure threshold and rejects requests`() = runTest {
            val (client, count) = mockClient { HttpStatusCode.InternalServerError }

            runCatching { client.get("https://api.example.com/data") }
            runCatching { client.get("https://api.example.com/data") }

            val result = runCatching { client.get("https://api.example.com/data") }
            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isInstanceOf(CircuitBreakerOpenException::class.java)
            assertThat(count.get()).isEqualTo(2) // only 2 actual requests made
        }
    }

    @Nested
    inner class HostIsolation {

        @Test
        fun `circuit breaker state is per-host`() = runTest {
            val (client, _) = mockClient { HttpStatusCode.InternalServerError }

            // Open circuit for host-a
            runCatching { client.get("https://host-a.example.com/data") }
            runCatching { client.get("https://host-a.example.com/data") }

            // host-a should be rejected
            val resultA = runCatching { client.get("https://host-a.example.com/data") }
            assertThat(resultA.exceptionOrNull()).isInstanceOf(CircuitBreakerOpenException::class.java)

            // host-b should still work (different host, separate state) — circuit not opened for this host
            val resultB = runCatching { client.get("https://host-b.example.com/data") }
            assertThat(resultB.exceptionOrNull() is CircuitBreakerOpenException).isFalse()
        }
    }

    @Nested
    inner class FailureClassification {

        @Test
        fun `4xx responses do not count as failures`() = runTest {
            val (client, _) = mockClient { HttpStatusCode.NotFound }

            repeat(5) { client.get("https://api.example.com/data") }

            // Circuit should still be closed after many 4xx
            val result = runCatching { client.get("https://api.example.com/data") }
            assertThat(result.isSuccess).isTrue()
        }

        @Test
        fun `429 counts as failure`() = runTest {
            val (client, count) = mockClient { HttpStatusCode.TooManyRequests }

            runCatching { client.get("https://api.example.com/data") }
            runCatching { client.get("https://api.example.com/data") }

            val result = runCatching { client.get("https://api.example.com/data") }
            assertThat(result.exceptionOrNull()).isInstanceOf(CircuitBreakerOpenException::class.java)
            assertThat(count.get()).isEqualTo(2)
        }
    }

    @Nested
    inner class HalfOpenRecovery {

        @Test
        fun `circuit recovers after open duration`() = runTest {
            var shouldFail = true
            val (client, _) = mockClient(
                config = CircuitBreakerConfig(failureThreshold = 2, openDurationMs = 100, halfOpenMaxProbes = 1),
            ) {
                if (shouldFail) HttpStatusCode.InternalServerError else HttpStatusCode.OK
            }

            // Open the circuit
            runCatching { client.get("https://api.example.com/data") }
            runCatching { client.get("https://api.example.com/data") }

            // Wait for open duration to elapse
            Thread.sleep(150)

            // Next request should be a probe (HALF_OPEN)
            shouldFail = false
            val result = client.get("https://api.example.com/data")
            assertThat(result.status).isEqualTo(HttpStatusCode.OK)

            // Circuit should be closed now, normal requests work
            val result2 = client.get("https://api.example.com/data")
            assertThat(result2.status).isEqualTo(HttpStatusCode.OK)
        }
    }
}
