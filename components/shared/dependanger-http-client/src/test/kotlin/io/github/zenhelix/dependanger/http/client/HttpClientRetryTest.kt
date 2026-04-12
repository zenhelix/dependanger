package io.github.zenhelix.dependanger.http.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class HttpClientRetryTest {

    private val fastRetryConfig = RetryConfig(
        maxRetries = 2,
        initialDelayMs = 1,
        maxDelayMs = 10,
        backoffMultiplier = 1.0,
    )

    private fun mockClientWithRetry(handler: MockRequestHandler): HttpClient =
        HttpClient(MockEngine(handler)) {
            install(HttpRequestRetry) {
                maxRetries = fastRetryConfig.maxRetries
                retryIf { _, response ->
                    response.status.value in 500..599 || response.status.value == 429
                }
                retryOnExceptionIf { _, cause ->
                    cause is java.io.IOException
                }
                delayMillis { 1L }
            }
        }

    private fun mockClientNoRetry(handler: MockRequestHandler): HttpClient =
        HttpClient(MockEngine(handler))

    @Nested
    inner class SuccessfulRequests {

        @Test
        fun `200 OK returns Success with body text`() = runTest {
            val client = mockClientNoRetry { _ ->
                respond(
                    content = "response body",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "text/plain"),
                )
            }
            val result = client.getWithRetry("https://example.com", fastRetryConfig)

            assertThat(result).isInstanceOf(HttpResult.Success::class.java)
            assertThat((result as HttpResult.Success).data).isEqualTo("response body")
            client.close()
        }

        @Test
        fun `postWithRetry returns Success on 200`() = runTest {
            val client = mockClientNoRetry { _ ->
                respond(
                    content = "post response",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "text/plain"),
                )
            }
            val result = client.postWithRetry("https://example.com/api", fastRetryConfig)

            assertThat(result).isInstanceOf(HttpResult.Success::class.java)
            assertThat((result as HttpResult.Success).data).isEqualTo("post response")
            client.close()
        }
    }

    @Nested
    inner class NotFoundRequests {

        @Test
        fun `404 returns NotFound immediately without retry`() = runTest {
            val requestCount = AtomicInteger(0)
            val client = mockClientWithRetry { _ ->
                requestCount.incrementAndGet()
                respond(content = "", status = HttpStatusCode.NotFound)
            }
            val result = client.getWithRetry("https://example.com/missing", fastRetryConfig)

            assertThat(result).isEqualTo(HttpResult.NotFound)
            assertThat(requestCount.get()).isEqualTo(1)
            client.close()
        }
    }

    @Nested
    inner class AuthenticationErrors {

        @Test
        fun `401 returns AuthRequired`() = runTest {
            val requestCount = AtomicInteger(0)
            val client = mockClientWithRetry { _ ->
                requestCount.incrementAndGet()
                respond(content = "", status = HttpStatusCode.Unauthorized)
            }
            val result = client.getWithRetry("https://example.com/secure", fastRetryConfig)

            assertThat(result).isInstanceOf(HttpResult.AuthRequired::class.java)
            val authResult = result as HttpResult.AuthRequired
            assertThat(authResult.url).isEqualTo("https://example.com/secure")
            assertThat(authResult.statusCode).isEqualTo(401)
            assertThat(requestCount.get()).isEqualTo(1)
            client.close()
        }

        @Test
        fun `403 returns AuthRequired`() = runTest {
            val requestCount = AtomicInteger(0)
            val client = mockClientWithRetry { _ ->
                requestCount.incrementAndGet()
                respond(content = "", status = HttpStatusCode.Forbidden)
            }
            val result = client.getWithRetry("https://example.com/forbidden", fastRetryConfig)

            assertThat(result).isInstanceOf(HttpResult.AuthRequired::class.java)
            val authResult = result as HttpResult.AuthRequired
            assertThat(authResult.statusCode).isEqualTo(403)
            assertThat(requestCount.get()).isEqualTo(1)
            client.close()
        }
    }

    @Nested
    inner class ServerErrors {

        @Test
        fun `500 retries and returns Failed after exhausting retries`() = runTest {
            val requestCount = AtomicInteger(0)
            val client = mockClientWithRetry { _ ->
                requestCount.incrementAndGet()
                respond(content = "Internal Server Error", status = HttpStatusCode.InternalServerError)
            }
            val result = client.getWithRetry("https://example.com/error", fastRetryConfig)

            assertThat(result).isInstanceOf(HttpResult.Failed::class.java)
            assertThat(requestCount.get()).isEqualTo(3) // 1 initial + 2 retries
            client.close()
        }

        @Test
        fun `500 then 200 returns Success on retry`() = runTest {
            val requestCount = AtomicInteger(0)
            val client = mockClientWithRetry { _ ->
                val attempt = requestCount.incrementAndGet()
                if (attempt == 1) {
                    respond(content = "error", status = HttpStatusCode.InternalServerError)
                } else {
                    respond(
                        content = "recovered",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "text/plain"),
                    )
                }
            }
            val result = client.getWithRetry("https://example.com/flaky", fastRetryConfig)

            assertThat(result).isInstanceOf(HttpResult.Success::class.java)
            assertThat((result as HttpResult.Success).data).isEqualTo("recovered")
            assertThat(requestCount.get()).isEqualTo(2)
            client.close()
        }
    }

    @Nested
    inner class RateLimiting {

        @Test
        fun `429 retries and returns Success after recovery`() = runTest {
            val requestCount = AtomicInteger(0)
            val client = mockClientWithRetry { _ ->
                val attempt = requestCount.incrementAndGet()
                if (attempt == 1) {
                    respond(
                        content = "",
                        status = HttpStatusCode.TooManyRequests,
                        headers = headersOf("Retry-After", "1"),
                    )
                } else {
                    respond(
                        content = "ok after rate limit",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "text/plain"),
                    )
                }
            }
            val result = client.getWithRetry("https://example.com/rate-limited", fastRetryConfig)

            assertThat(result).isInstanceOf(HttpResult.Success::class.java)
            assertThat((result as HttpResult.Success).data).isEqualTo("ok after rate limit")
            assertThat(requestCount.get()).isEqualTo(2)
            client.close()
        }
    }

    @Nested
    inner class UnexpectedStatus {

        @Test
        fun `418 returns Failed immediately without retry`() = runTest {
            val requestCount = AtomicInteger(0)
            val client = mockClientWithRetry { _ ->
                requestCount.incrementAndGet()
                respond(content = "I'm a teapot", status = HttpStatusCode(418, "I'm a teapot"))
            }
            val result = client.getWithRetry("https://example.com/teapot", fastRetryConfig)

            assertThat(result).isInstanceOf(HttpResult.Failed::class.java)
            assertThat(requestCount.get()).isEqualTo(1)
            client.close()
        }
    }

    @Nested
    inner class RetryExhaustion {

        @Test
        fun `all attempts fail returns Failed`() = runTest {
            val requestCount = AtomicInteger(0)
            val config = RetryConfig(
                maxRetries = 3,
                initialDelayMs = 1,
                maxDelayMs = 10,
                backoffMultiplier = 1.0,
            )
            val client = HttpClient(MockEngine { _ ->
                requestCount.incrementAndGet()
                respond(content = "error", status = HttpStatusCode.InternalServerError)
            }) {
                install(HttpRequestRetry) {
                    maxRetries = config.maxRetries
                    retryIf { _, response -> response.status.value in 500..599 }
                    delayMillis { 1L }
                }
            }
            val result = client.getWithRetry("https://example.com/always-fail", config)

            assertThat(result).isInstanceOf(HttpResult.Failed::class.java)
            assertThat(requestCount.get()).isEqualTo(4) // 1 initial + 3 retries
            client.close()
        }
    }
}
