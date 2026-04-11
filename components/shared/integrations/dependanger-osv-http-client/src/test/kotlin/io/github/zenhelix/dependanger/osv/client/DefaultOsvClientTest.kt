package io.github.zenhelix.dependanger.osv.client

import io.github.zenhelix.dependanger.core.model.MavenCoordinate
import io.github.zenhelix.dependanger.http.client.HttpClientConfig
import io.github.zenhelix.dependanger.http.client.HttpClientFactory
import io.github.zenhelix.dependanger.http.client.RetryConfig
import io.github.zenhelix.dependanger.osv.client.model.OsvBatchResult
import io.github.zenhelix.dependanger.osv.client.model.OsvPackageQuery
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class DefaultOsvClientTest {

    private val fastRetryConfig = RetryConfig(
        maxRetries = 1,
        initialDelayMs = 1,
        maxDelayMs = 10,
        backoffMultiplier = 1.0,
    )

    private val defaultConfig = OsvClientConfig(
        apiUrl = "https://api.osv.dev",
        batchSize = 1000,
        timeoutMs = 5_000L,
        retryConfig = fastRetryConfig,
    )

    private fun mockFactory(handler: (MockRequestHandleScope, HttpRequestData) -> HttpResponseData): HttpClientFactory {
        return object : HttpClientFactory {
            override fun create(block: HttpClientConfig.() -> Unit): HttpClient {
                return HttpClient(MockEngine { request -> handler(this, request) })
            }
        }
    }

    private fun respondJson(scope: MockRequestHandleScope, json: String): HttpResponseData {
        return scope.respond(
            content = ByteReadChannel(json.toByteArray()),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
        )
    }

    private fun readRequestBody(request: HttpRequestData): String {
        return when (val body = request.body) {
            is TextContent -> body.text
            else           -> ""
        }
    }

    @Nested
    inner class EmptyInput {

        @Test
        fun `queryBatch with empty list returns Success with empty vulnerabilities`() = runTest {
            val requestCount = AtomicInteger(0)
            val factory = mockFactory { scope, _ ->
                requestCount.incrementAndGet()
                respondJson(scope, """{"results":[]}""")
            }

            OsvClient(defaultConfig, factory).use { client ->
                val result = client.queryBatch(emptyList())

                assertThat(result).isInstanceOf(OsvBatchResult.Success::class.java)
                val success = result as OsvBatchResult.Success
                assertThat(success.vulnerabilities).isEmpty()
                assertThat(requestCount.get()).isEqualTo(0)

            }
        }
    }

    @Nested
    inner class SuccessfulBatch {

        @Test
        fun `queryBatch calls correct URL and maps vulnerabilities`() = runTest {
            val capturedUrls = mutableListOf<String>()
            val capturedBodies = mutableListOf<String>()

            val responseJson = """
                {
                    "results": [
                        {
                            "vulns": [
                                {
                                    "id": "GHSA-1234-abcd",
                                    "aliases": ["CVE-2024-1234"],
                                    "summary": "Test vulnerability",
                                    "severity": [{"type": "CVSS_V3", "score": "7.5"}],
                                    "affected": [{
                                        "ranges": [{
                                            "type": "ECOSYSTEM",
                                            "events": [{"introduced": "0"}, {"fixed": "2.0.0"}]
                                        }]
                                    }],
                                    "references": [{"type": "ADVISORY", "url": "https://example.com/advisory"}]
                                }
                            ]
                        }
                    ]
                }
            """.trimIndent()

            val factory = mockFactory { scope, request ->
                capturedUrls.add(request.url.toString())
                capturedBodies.add(readRequestBody(request))
                respondJson(scope, responseJson)
            }

            OsvClient(defaultConfig, factory).use { client ->
                val result = client.queryBatch(
                    listOf(OsvPackageQuery(coordinate = MavenCoordinate("com.example", "lib"), version = "1.0.0"))
                )

                assertThat(capturedUrls).hasSize(1)
                assertThat(capturedUrls[0]).isEqualTo("https://api.osv.dev/v1/querybatch")

                val body = capturedBodies[0]
                assertThat(body).contains("\"ecosystem\":\"Maven\"")
                assertThat(body).contains("\"name\":\"com.example:lib\"")
                assertThat(body).contains("\"version\":\"1.0.0\"")

                assertThat(result).isInstanceOf(OsvBatchResult.Success::class.java)
                val success = result as OsvBatchResult.Success
                assertThat(success.vulnerabilities).hasSize(1)
                assertThat(success.vulnerabilities[0]).hasSize(1)

                val vuln = success.vulnerabilities[0][0]
                assertThat(vuln.id).isEqualTo("GHSA-1234-abcd")
                assertThat(vuln.aliases).containsExactly("CVE-2024-1234")
                assertThat(vuln.summary).isEqualTo("Test vulnerability")
                assertThat(vuln.cvssScore).isEqualTo(7.5)
                assertThat(vuln.cvssVersion).isEqualTo("CVSS_V3")
                assertThat(vuln.fixedVersion).isEqualTo("2.0.0")
                assertThat(vuln.referenceUrl).isEqualTo("https://example.com/advisory")

            }
        }
    }

    @Nested
    inner class NoVulnerabilitiesFound {

        @Test
        fun `queryBatch returns Success with empty lists when no vulns found`() = runTest {
            val responseJson = """
                {
                    "results": [
                        {"vulns": []},
                        {}
                    ]
                }
            """.trimIndent()

            val factory = mockFactory { scope, _ -> respondJson(scope, responseJson) }

            OsvClient(defaultConfig, factory).use { client ->
                val result = client.queryBatch(
                    listOf(
                        OsvPackageQuery(coordinate = MavenCoordinate("com.safe", "lib1"), version = "1.0.0"),
                        OsvPackageQuery(coordinate = MavenCoordinate("com.safe", "lib2"), version = "2.0.0"),
                    )
                )

                assertThat(result).isInstanceOf(OsvBatchResult.Success::class.java)
                val success = result as OsvBatchResult.Success
                assertThat(success.vulnerabilities).hasSize(2)
                assertThat(success.vulnerabilities[0]).isEmpty()
                assertThat(success.vulnerabilities[1]).isEmpty()

            }
        }
    }

    @Nested
    inner class PartialSuccess {

        @Test
        fun `queryBatch with multiple batches returns PartialSuccess when second batch fails`() = runTest {
            val requestCount = AtomicInteger(0)
            val config = defaultConfig.copy(batchSize = 2)

            val successResponse = """
                {
                    "results": [
                        {"vulns": [{"id": "GHSA-0001", "summary": "vuln1"}]},
                        {"vulns": []}
                    ]
                }
            """.trimIndent()

            val factory = mockFactory { scope, _ ->
                val attempt = requestCount.incrementAndGet()
                if (attempt == 1) {
                    respondJson(scope, successResponse)
                } else {
                    scope.respond(
                        content = ByteReadChannel("Internal Server Error".toByteArray()),
                        status = HttpStatusCode.InternalServerError,
                        headers = headersOf(HttpHeaders.ContentType, "text/plain"),
                    )
                }
            }

            OsvClient(config, factory).use { client ->
                val result = client.queryBatch(
                    listOf(
                        OsvPackageQuery(coordinate = MavenCoordinate("com.example", "a"), version = "1.0"),
                        OsvPackageQuery(coordinate = MavenCoordinate("com.example", "b"), version = "1.0"),
                        OsvPackageQuery(coordinate = MavenCoordinate("com.example", "c"), version = "1.0"),
                    )
                )

                assertThat(result).isInstanceOf(OsvBatchResult.PartialSuccess::class.java)
                val partial = result as OsvBatchResult.PartialSuccess
                assertThat(partial.vulnerabilities).hasSize(2)
                assertThat(partial.failedPackageCount).isEqualTo(1)
                assertThat(partial.isTimeout).isFalse()

            }
        }
    }

    @Nested
    inner class TimeoutHandling {

        @Test
        fun `queryBatch returns Timeout when request times out on first batch`() = runTest {
            val factory = mockFactory { _, _ ->
                throw io.ktor.client.plugins.HttpRequestTimeoutException(
                    "https://api.osv.dev/v1/querybatch",
                    5_000L,
                )
            }

            OsvClient(defaultConfig, factory).use { client ->
                val result = client.queryBatch(
                    listOf(OsvPackageQuery(coordinate = MavenCoordinate("com.example", "lib"), version = "1.0.0"))
                )

                assertThat(result).isInstanceOf(OsvBatchResult.Timeout::class.java)
                val timeout = result as OsvBatchResult.Timeout
                assertThat(timeout.error).isNotEmpty()

            }
        }

        @Test
        fun `queryBatch returns PartialSuccess with isTimeout when second batch times out`() = runTest {
            val requestCount = AtomicInteger(0)
            val config = defaultConfig.copy(batchSize = 1)

            val successResponse = """{"results": [{"vulns": []}]}"""

            val factory = mockFactory { scope, _ ->
                val attempt = requestCount.incrementAndGet()
                if (attempt == 1) {
                    respondJson(scope, successResponse)
                } else {
                    throw io.ktor.client.plugins.HttpRequestTimeoutException(
                        "https://api.osv.dev/v1/querybatch",
                        5_000L,
                    )
                }
            }

            OsvClient(config, factory).use { client ->
                val result = client.queryBatch(
                    listOf(
                        OsvPackageQuery(coordinate = MavenCoordinate("com.example", "a"), version = "1.0"),
                        OsvPackageQuery(coordinate = MavenCoordinate("com.example", "b"), version = "1.0"),
                    )
                )

                assertThat(result).isInstanceOf(OsvBatchResult.PartialSuccess::class.java)
                val partial = result as OsvBatchResult.PartialSuccess
                assertThat(partial.isTimeout).isTrue()
                assertThat(partial.failedPackageCount).isEqualTo(1)

            }
        }
    }

    @Nested
    inner class ApiFailure {

        @Test
        fun `queryBatch returns Failed when API returns 500`() = runTest {
            val factory = mockFactory { scope, _ ->
                scope.respond(
                    content = ByteReadChannel("Internal Server Error".toByteArray()),
                    status = HttpStatusCode.InternalServerError,
                    headers = headersOf(HttpHeaders.ContentType, "text/plain"),
                )
            }

            OsvClient(defaultConfig, factory).use { client ->
                val result = client.queryBatch(
                    listOf(OsvPackageQuery(coordinate = MavenCoordinate("com.example", "lib"), version = "1.0.0"))
                )

                assertThat(result).isInstanceOf(OsvBatchResult.Failed::class.java)
                val failed = result as OsvBatchResult.Failed
                assertThat(failed.error).isNotEmpty()

            }
        }
    }

    @Nested
    inner class RateLimiting {

        @Test
        fun `queryBatch returns Failed when API returns 429 and retries exhaust`() = runTest {
            val factory = mockFactory { scope, _ ->
                scope.respond(
                    content = ByteReadChannel("Too Many Requests".toByteArray()),
                    status = HttpStatusCode.TooManyRequests,
                    headers = headersOf("Retry-After", "1"),
                )
            }

            OsvClient(defaultConfig, factory).use { client ->
                val result = client.queryBatch(
                    listOf(OsvPackageQuery(coordinate = MavenCoordinate("com.example", "lib"), version = "1.0.0"))
                )

                // 429 is retried by the HTTP layer; after exhausting retries, it becomes Failed
                assertThat(result).isInstanceOf(OsvBatchResult.Failed::class.java)

            }
        }
    }

    @Nested
    inner class ResponseSizeMismatch {

        @Test
        fun `queryBatch returns Failed when response has fewer results than queries`() = runTest {
            val responseJson = """
                {
                    "results": [
                        {"vulns": []}
                    ]
                }
            """.trimIndent()

            val factory = mockFactory { scope, _ -> respondJson(scope, responseJson) }

            OsvClient(defaultConfig, factory).use { client ->
                val result = client.queryBatch(
                    listOf(
                        OsvPackageQuery(coordinate = MavenCoordinate("com.example", "a"), version = "1.0"),
                        OsvPackageQuery(coordinate = MavenCoordinate("com.example", "b"), version = "2.0"),
                    )
                )

                assertThat(result).isInstanceOf(OsvBatchResult.Failed::class.java)
                val failed = result as OsvBatchResult.Failed
                assertThat(failed.error).contains("mismatch")
                assertThat(failed.error).contains("expected 2")
                assertThat(failed.error).contains("got 1")

            }
        }
    }

    @Nested
    inner class CvssParsing {

        @Test
        fun `numeric CVSS score string is parsed correctly`() = runTest {
            val responseJson = """
                {
                    "results": [{
                        "vulns": [{
                            "id": "GHSA-num",
                            "severity": [{"type": "CVSS_V3", "score": "7.5"}]
                        }]
                    }]
                }
            """.trimIndent()

            val factory = mockFactory { scope, _ -> respondJson(scope, responseJson) }

            OsvClient(defaultConfig, factory).use { client ->
                val result = client.queryBatch(
                    listOf(OsvPackageQuery(coordinate = MavenCoordinate("com.example", "lib"), version = "1.0"))
                )

                val success = result as OsvBatchResult.Success
                assertThat(success.vulnerabilities[0][0].cvssScore).isEqualTo(7.5)
                assertThat(success.vulnerabilities[0][0].cvssVersion).isEqualTo("CVSS_V3")

            }
        }

        @Test
        fun `CVSS V3 vector string is parsed to base score`() = runTest {
            val responseJson = """
                {
                    "results": [{
                        "vulns": [{
                            "id": "GHSA-vec",
                            "severity": [{"type": "CVSS_V3", "score": "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:N/A:N"}]
                        }]
                    }]
                }
            """.trimIndent()

            val factory = mockFactory { scope, _ -> respondJson(scope, responseJson) }

            OsvClient(defaultConfig, factory).use { client ->
                val result = client.queryBatch(
                    listOf(OsvPackageQuery(coordinate = MavenCoordinate("com.example", "lib"), version = "1.0"))
                )

                val success = result as OsvBatchResult.Success
                assertThat(success.vulnerabilities[0][0].cvssScore).isNotNull()
                assertThat(success.vulnerabilities[0][0].cvssScore).isGreaterThan(0.0)
                assertThat(success.vulnerabilities[0][0].cvssVersion).isEqualTo("CVSS_V3")

            }
        }

        @Test
        fun `invalid CVSS vector results in null cvssScore`() = runTest {
            val responseJson = """
                {
                    "results": [{
                        "vulns": [{
                            "id": "GHSA-inv",
                            "severity": [{"type": "CVSS_V3", "score": "not-a-valid-vector"}]
                        }]
                    }]
                }
            """.trimIndent()

            val factory = mockFactory { scope, _ -> respondJson(scope, responseJson) }

            OsvClient(defaultConfig, factory).use { client ->
                val result = client.queryBatch(
                    listOf(OsvPackageQuery(coordinate = MavenCoordinate("com.example", "lib"), version = "1.0"))
                )

                val success = result as OsvBatchResult.Success
                assertThat(success.vulnerabilities[0][0].cvssScore).isNull()
                assertThat(success.vulnerabilities[0][0].cvssVersion).isEqualTo("CVSS_V3")

            }
        }

        @Test
        fun `CVSS version priority V4 over V3 over V2`() = runTest {
            val responseJson = """
                {
                    "results": [{
                        "vulns": [{
                            "id": "GHSA-prio",
                            "severity": [
                                {"type": "CVSS_V2", "score": "4.0"},
                                {"type": "CVSS_V3", "score": "7.5"},
                                {"type": "CVSS_V4", "score": "9.0"}
                            ]
                        }]
                    }]
                }
            """.trimIndent()

            val factory = mockFactory { scope, _ -> respondJson(scope, responseJson) }

            OsvClient(defaultConfig, factory).use { client ->
                val result = client.queryBatch(
                    listOf(OsvPackageQuery(coordinate = MavenCoordinate("com.example", "lib"), version = "1.0"))
                )

                val success = result as OsvBatchResult.Success
                val vuln = success.vulnerabilities[0][0]
                assertThat(vuln.cvssVersion).isEqualTo("CVSS_V4")
                assertThat(vuln.cvssScore).isEqualTo(9.0)

            }
        }

        @Test
        fun `no severity results in null cvssScore and cvssVersion`() = runTest {
            val responseJson = """
                {
                    "results": [{
                        "vulns": [{
                            "id": "GHSA-none",
                            "summary": "No severity info"
                        }]
                    }]
                }
            """.trimIndent()

            val factory = mockFactory { scope, _ -> respondJson(scope, responseJson) }

            OsvClient(defaultConfig, factory).use { client ->
                val result = client.queryBatch(
                    listOf(OsvPackageQuery(coordinate = MavenCoordinate("com.example", "lib"), version = "1.0"))
                )

                val success = result as OsvBatchResult.Success
                val vuln = success.vulnerabilities[0][0]
                assertThat(vuln.cvssScore).isNull()
                assertThat(vuln.cvssVersion).isNull()

            }
        }
    }

    @Nested
    inner class VulnerabilityDataMapping {

        @Test
        fun `fixedVersion is extracted from affected ranges`() = runTest {
            val responseJson = """
                {
                    "results": [{
                        "vulns": [{
                            "id": "GHSA-fix",
                            "affected": [{
                                "ranges": [{
                                    "type": "ECOSYSTEM",
                                    "events": [
                                        {"introduced": "0"},
                                        {"fixed": "3.2.1"}
                                    ]
                                }]
                            }]
                        }]
                    }]
                }
            """.trimIndent()

            val factory = mockFactory { scope, _ -> respondJson(scope, responseJson) }

            OsvClient(defaultConfig, factory).use { client ->
                val result = client.queryBatch(
                    listOf(OsvPackageQuery(coordinate = MavenCoordinate("com.example", "lib"), version = "1.0"))
                )

                val success = result as OsvBatchResult.Success
                assertThat(success.vulnerabilities[0][0].fixedVersion).isEqualTo("3.2.1")

            }
        }

        @Test
        fun `fixedVersion is null when no fix available`() = runTest {
            val responseJson = """
                {
                    "results": [{
                        "vulns": [{
                            "id": "GHSA-nofix",
                            "affected": [{
                                "ranges": [{
                                    "type": "ECOSYSTEM",
                                    "events": [{"introduced": "0"}]
                                }]
                            }]
                        }]
                    }]
                }
            """.trimIndent()

            val factory = mockFactory { scope, _ -> respondJson(scope, responseJson) }

            OsvClient(defaultConfig, factory).use { client ->
                val result = client.queryBatch(
                    listOf(OsvPackageQuery(coordinate = MavenCoordinate("com.example", "lib"), version = "1.0"))
                )

                val success = result as OsvBatchResult.Success
                assertThat(success.vulnerabilities[0][0].fixedVersion).isNull()

            }
        }

        @Test
        fun `referenceUrl is taken from first reference`() = runTest {
            val responseJson = """
                {
                    "results": [{
                        "vulns": [{
                            "id": "GHSA-ref",
                            "references": [
                                {"type": "ADVISORY", "url": "https://first.example.com"},
                                {"type": "WEB", "url": "https://second.example.com"}
                            ]
                        }]
                    }]
                }
            """.trimIndent()

            val factory = mockFactory { scope, _ -> respondJson(scope, responseJson) }

            OsvClient(defaultConfig, factory).use { client ->
                val result = client.queryBatch(
                    listOf(OsvPackageQuery(coordinate = MavenCoordinate("com.example", "lib"), version = "1.0"))
                )

                val success = result as OsvBatchResult.Success
                assertThat(success.vulnerabilities[0][0].referenceUrl).isEqualTo("https://first.example.com")

            }
        }

        @Test
        fun `aliases are extracted correctly`() = runTest {
            val responseJson = """
                {
                    "results": [{
                        "vulns": [{
                            "id": "GHSA-alias",
                            "aliases": ["CVE-2024-0001", "CVE-2024-0002"]
                        }]
                    }]
                }
            """.trimIndent()

            val factory = mockFactory { scope, _ -> respondJson(scope, responseJson) }

            OsvClient(defaultConfig, factory).use { client ->
                val result = client.queryBatch(
                    listOf(OsvPackageQuery(coordinate = MavenCoordinate("com.example", "lib"), version = "1.0"))
                )

                val success = result as OsvBatchResult.Success
                assertThat(success.vulnerabilities[0][0].aliases)
                    .containsExactly("CVE-2024-0001", "CVE-2024-0002")

            }
        }

        @Test
        fun `missing optional fields default correctly`() = runTest {
            val responseJson = """
                {
                    "results": [{
                        "vulns": [{
                            "id": "GHSA-min"
                        }]
                    }]
                }
            """.trimIndent()

            val factory = mockFactory { scope, _ -> respondJson(scope, responseJson) }

            OsvClient(defaultConfig, factory).use { client ->
                val result = client.queryBatch(
                    listOf(OsvPackageQuery(coordinate = MavenCoordinate("com.example", "lib"), version = "1.0"))
                )

                val success = result as OsvBatchResult.Success
                val vuln = success.vulnerabilities[0][0]
                assertThat(vuln.id).isEqualTo("GHSA-min")
                assertThat(vuln.aliases).isEmpty()
                assertThat(vuln.summary).isEmpty()
                assertThat(vuln.cvssScore).isNull()
                assertThat(vuln.cvssVersion).isNull()
                assertThat(vuln.fixedVersion).isNull()
                assertThat(vuln.referenceUrl).isNull()

            }
        }
    }
}
