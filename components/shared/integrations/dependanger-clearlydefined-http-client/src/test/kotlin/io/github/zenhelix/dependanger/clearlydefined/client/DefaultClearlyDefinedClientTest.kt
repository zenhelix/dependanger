package io.github.zenhelix.dependanger.clearlydefined.client

import io.github.zenhelix.dependanger.clearlydefined.client.model.ClearlyDefinedResult
import io.github.zenhelix.dependanger.http.client.HttpClientConfig
import io.github.zenhelix.dependanger.http.client.HttpClientFactory
import io.github.zenhelix.dependanger.http.client.RetryConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class DefaultClearlyDefinedClientTest {

    private val fastRetryConfig = RetryConfig(
        maxRetries = 1,
        initialDelayMs = 1,
        maxDelayMs = 10,
        backoffMultiplier = 1.0,
    )

    private val config = ClearlyDefinedClientConfig(
        apiUrl = "https://api.clearlydefined.io",
        timeoutMs = 5_000L,
        retryConfig = fastRetryConfig,
    )

    private fun mockFactory(handler: MockRequestHandler): HttpClientFactory = object : HttpClientFactory {
        override fun create(block: HttpClientConfig.() -> Unit): HttpClient = HttpClient(MockEngine(handler))
    }

    private fun createClient(handler: MockRequestHandler): ClearlyDefinedClient =
        ClearlyDefinedClient(config = config, httpClientFactory = mockFactory(handler))

    @Nested
    inner class SuccessfulLicenseLookup {

        @Test
        fun `valid license found returns Found with declared expression`() = runTest {
            val client = createClient { _ ->
                respond(
                    content = """{"licensed":{"declared":"Apache-2.0"}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }

            client.use {
                val result = it.fetchLicense("org.apache.commons", "commons-lang3", "3.12.0")

                assertThat(result).isInstanceOf(ClearlyDefinedResult.Found::class.java)
                assertThat((result as ClearlyDefinedResult.Found).declaredExpression).isEqualTo("Apache-2.0")
            }
        }

        @Test
        fun `complex SPDX expression returns Found with full expression`() = runTest {
            val client = createClient { _ ->
                respond(
                    content = """{"licensed":{"declared":"MIT OR Apache-2.0"}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }

            client.use {
                val result = it.fetchLicense("com.example", "dual-licensed", "1.0.0")

                assertThat(result).isInstanceOf(ClearlyDefinedResult.Found::class.java)
                assertThat((result as ClearlyDefinedResult.Found).declaredExpression).isEqualTo("MIT OR Apache-2.0")
            }
        }
    }

    @Nested
    inner class NotFoundLicenseCases {

        @Test
        fun `NOASSERTION declared returns NotFound`() = runTest {
            val client = createClient { _ ->
                respond(
                    content = """{"licensed":{"declared":"NOASSERTION"}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }

            client.use {
                val result = it.fetchLicense("com.example", "no-assertion", "1.0.0")
                assertThat(result).isEqualTo(ClearlyDefinedResult.NotFound)
            }
        }

        @Test
        fun `blank declared returns NotFound`() = runTest {
            val client = createClient { _ ->
                respond(
                    content = """{"licensed":{"declared":""}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }

            client.use {
                val result = it.fetchLicense("com.example", "blank-license", "1.0.0")
                assertThat(result).isEqualTo(ClearlyDefinedResult.NotFound)
            }
        }

        @Test
        fun `null declared returns NotFound`() = runTest {
            val client = createClient { _ ->
                respond(
                    content = """{"licensed":{"declared":null}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }

            client.use {
                val result = it.fetchLicense("com.example", "null-declared", "1.0.0")
                assertThat(result).isEqualTo(ClearlyDefinedResult.NotFound)
            }
        }

        @Test
        fun `null licensed returns NotFound`() = runTest {
            val client = createClient { _ ->
                respond(
                    content = """{"licensed":null}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }

            client.use {
                val result = it.fetchLicense("com.example", "null-licensed", "1.0.0")
                assertThat(result).isEqualTo(ClearlyDefinedResult.NotFound)
            }
        }

        @Test
        fun `missing licensed field returns Failed with parse error`() = runTest {
            val client = createClient { _ ->
                respond(
                    content = """{}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }

            client.use {
                val result = it.fetchLicense("com.example", "missing-licensed", "1.0.0")

                assertThat(result).isInstanceOf(ClearlyDefinedResult.Failed::class.java)
                assertThat((result as ClearlyDefinedResult.Failed).error).contains("parse")
            }
        }
    }

    @Nested
    inner class UrlConstruction {

        @Test
        fun `request URL is constructed correctly`() = runTest {
            var capturedUrl: String? = null

            val client = createClient { request ->
                capturedUrl = request.url.toString()
                respond(
                    content = """{"licensed":{"declared":"MIT"}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }

            client.use {
                it.fetchLicense("org.apache.commons", "commons-lang3", "3.12.0")
            }

            assertThat(capturedUrl).isEqualTo(
                "https://api.clearlydefined.io/definitions/maven/mavencentral/org.apache.commons/commons-lang3/3.12.0"
            )
        }
    }

    @Nested
    inner class HttpErrorResponses {

        @Test
        fun `HTTP 404 returns NotFound`() = runTest {
            val client = createClient { _ ->
                respond(content = "", status = HttpStatusCode.NotFound)
            }

            client.use {
                val result = it.fetchLicense("com.example", "nonexistent", "1.0.0")
                assertThat(result).isEqualTo(ClearlyDefinedResult.NotFound)
            }
        }

        @Test
        fun `HTTP 429 rate limited returns Failed`() = runTest {
            val client = createClient { _ ->
                respond(
                    content = "",
                    status = HttpStatusCode.TooManyRequests,
                    headers = headersOf("Retry-After", "60"),
                )
            }

            client.use {
                val result = it.fetchLicense("com.example", "rate-limited", "1.0.0")

                assertThat(result).isInstanceOf(ClearlyDefinedResult.Failed::class.java)
            }
        }

        @Test
        fun `HTTP 500 returns Failed`() = runTest {
            val client = createClient { _ ->
                respond(content = "Internal Server Error", status = HttpStatusCode.InternalServerError)
            }

            client.use {
                val result = it.fetchLicense("com.example", "server-error", "1.0.0")

                assertThat(result).isInstanceOf(ClearlyDefinedResult.Failed::class.java)
            }
        }
    }

    @Nested
    inner class MalformedResponses {

        @Test
        fun `malformed JSON returns Failed with parse error`() = runTest {
            val client = createClient { _ ->
                respond(
                    content = "this is not json",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }

            client.use {
                val result = it.fetchLicense("com.example", "bad-json", "1.0.0")

                assertThat(result).isInstanceOf(ClearlyDefinedResult.Failed::class.java)
                assertThat((result as ClearlyDefinedResult.Failed).error).contains("parse")
            }
        }

        @Test
        fun `extra fields in response are ignored and license is parsed correctly`() = runTest {
            val client = createClient { _ ->
                respond(
                    content = """{"licensed":{"declared":"MIT","score":{"total":85}},"coordinates":{"type":"maven"},"extra":"field"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }

            client.use {
                val result = it.fetchLicense("com.example", "extra-fields", "1.0.0")

                assertThat(result).isInstanceOf(ClearlyDefinedResult.Found::class.java)
                assertThat((result as ClearlyDefinedResult.Found).declaredExpression).isEqualTo("MIT")
            }
        }
    }
}
