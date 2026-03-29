package io.github.zenhelix.dependanger.integration.support

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * DSL-based URL-to-response router for Ktor MockEngine.
 *
 * Usage:
 * ```
 * val router = mockHttpRouter {
 *     maven {
 *         metadata("org.jetbrains.kotlin", "kotlin-stdlib", listOf("2.1.20", "2.2.0"))
 *         pom("org.jetbrains.kotlin", "kotlin-stdlib", "2.1.20", "<project>...</project>")
 *     }
 *     osv {
 *         vulnerabilities("com.example:vulnerable-lib", listOf(...))
 *         noVulnerabilities("com.safe:lib")
 *     }
 *     clearlyDefined {
 *         license("org.jetbrains.kotlin", "kotlin-stdlib", "2.1.20", "Apache-2.0")
 *     }
 * }
 * ```
 */
class MockHttpRouter private constructor(
    private val routes: Map<RouteKey, RouteHandler>,
    private val osvHandlers: List<OsvHandler>,
) {
    private val requestedUrls: CopyOnWriteArrayList<String> = CopyOnWriteArrayList()
    private val unmatchedUrls: CopyOnWriteArrayList<String> = CopyOnWriteArrayList()

    /**
     * Base URL for use with MavenRepository settings in DSL.
     * MockEngine intercepts all requests so the actual host doesn't matter.
     */
    val mavenBaseUrl: String = "http://mock-maven.local"

    val requestLog: List<String> get() = requestedUrls.toList()
    val unmatchedLog: List<String> get() = unmatchedUrls.toList()

    fun createMockEngine(): MockEngine = MockEngine { request ->
        val url = request.url.toString()
        requestedUrls.add(url)

        val pathKey = extractPathKey(url)
        val route = routes[pathKey]
        if (route != null) {
            return@MockEngine route.handle(this, request)
        }

        if (request.method == HttpMethod.Post && url.contains("/v1/querybatch")) {
            val bodyText = readRequestBody(request)
            // Extract all package names from the batch request
            val queryNames = "\"name\"\\s*:\\s*\"([^\"]+)\"".toRegex()
                .findAll(bodyText)
                .map { it.groupValues[1] }
                .toList()
            if (queryNames.isNotEmpty()) {
                // Build a batch response with one result per query
                val resultEntries = queryNames.map { name ->
                    val handler = osvHandlers.find { it.packageName == name }
                    if (handler != null) {
                        """{"vulns":${handler.vulnsJson}}"""
                    } else {
                        """{"vulns":[]}"""
                    }
                }
                val batchResponse = """{"results":[${resultEntries.joinToString(",")}]}"""
                return@MockEngine respondJson(batchResponse)
            }
            return@MockEngine respondJson("""{"results":[{"vulns":[]}]}""")
        }

        unmatchedUrls.add(url)
        respondNotFound(url)
    }

    fun verifyAllRoutesUsed(): List<String> {
        val usedPaths = requestedUrls.map { extractPathKey(it) }.toSet()
        return routes.keys
            .filter { it !in usedPaths }
            .map { it.path }
    }

    private fun extractPathKey(url: String): RouteKey {
        val path = url.substringAfter("://").substringAfter("/").substringBefore("?")
        return RouteKey(path)
    }

    private fun readRequestBody(request: HttpRequestData): String {
        return when (val body = request.body) {
            is io.ktor.http.content.TextContent      -> body.text
            is io.ktor.http.content.ByteArrayContent -> body.bytes().decodeToString()
            else                                     -> ""
        }
    }

    data class RouteKey(val path: String)

    fun interface RouteHandler {
        fun handle(scope: MockRequestHandleScope, request: HttpRequestData): HttpResponseData
    }

    data class OsvHandler(
        val packageName: String,
        val vulnsJson: String,
    )

    class Builder {
        private val routes: ConcurrentHashMap<RouteKey, RouteHandler> = ConcurrentHashMap()
        private val osvHandlers: MutableList<OsvHandler> = mutableListOf()

        fun maven(block: MavenRouteBuilder.() -> Unit) {
            MavenRouteBuilder(routes).apply(block)
        }

        fun osv(block: OsvRouteBuilder.() -> Unit) {
            OsvRouteBuilder(osvHandlers).apply(block)
        }

        fun clearlyDefined(block: ClearlyDefinedRouteBuilder.() -> Unit) {
            ClearlyDefinedRouteBuilder(routes).apply(block)
        }

        fun route(pathPattern: String, contentType: ContentType = ContentType.Text.Plain, body: String) {
            routes[RouteKey(pathPattern)] = RouteHandler { scope, _ ->
                scope.respond(
                    content = ByteReadChannel(body.toByteArray()),
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type", contentType.toString()),
                )
            }
        }

        fun build(): MockHttpRouter = MockHttpRouter(
            routes = routes.toMap(),
            osvHandlers = osvHandlers.toList(),
        )
    }

    class MavenRouteBuilder(private val routes: ConcurrentHashMap<RouteKey, RouteHandler>) {

        fun metadata(group: String, artifact: String, versions: List<String>) {
            val groupPath = group.replace('.', '/')
            val path = "$groupPath/$artifact/maven-metadata.xml"
            val xml = MavenResponses.mavenMetadataXml(group, artifact, versions)
            routes[RouteKey(path)] = RouteHandler { scope, _ ->
                scope.respondXml(xml)
            }
        }

        fun pom(group: String, artifact: String, version: String, pomContent: String) {
            val groupPath = group.replace('.', '/')
            val path = "$groupPath/$artifact/$version/$artifact-$version.pom"
            routes[RouteKey(path)] = RouteHandler { scope, _ ->
                scope.respondXml(pomContent)
            }
        }

        fun pom(group: String, artifact: String, version: String, dependencies: List<PomDep> = emptyList(), parent: PomParent? = null) {
            val xml = MavenResponses.pomXml(group, artifact, version, dependencies, parent)
            pom(group, artifact, version, xml)
        }

        fun bomPom(group: String, artifact: String, version: String, managed: List<PomDep>) {
            val xml = MavenResponses.bomPomXml(group, artifact, version, managed)
            pom(group, artifact, version, xml)
        }
    }

    class OsvRouteBuilder(private val handlers: MutableList<OsvHandler>) {

        fun vulnerabilities(
            coordinates: String,
            vulns: List<OsvVulnResponse>,
        ) {
            val vulnsJson = buildVulnsArray(vulns)
            handlers.add(OsvHandler(packageName = coordinates, vulnsJson = vulnsJson))
        }

        fun noVulnerabilities(coordinates: String) {
            handlers.add(OsvHandler(packageName = coordinates, vulnsJson = "[]"))
        }

        private fun buildVulnsArray(vulns: List<OsvVulnResponse>): String {
            val vulnEntries = vulns.joinToString(",") { vuln ->
                val fixedEvent = if (vuln.fixedVersion != null) {
                    """{"introduced":"0"},{"fixed":"${vuln.fixedVersion}"}"""
                } else {
                    """{"introduced":"0"}"""
                }
                """{
                    "id":"${vuln.id}",
                    "summary":"${vuln.summary}",
                    "severity":[{"type":"CVSS_V3","score":"${vuln.cvssScore}"}],
                    "affected":[{"ranges":[{"type":"ECOSYSTEM","events":[$fixedEvent]}]}],
                    "references":[{"type":"ADVISORY","url":"https://github.com/advisories/${vuln.id}"}],
                    "aliases":${vuln.aliases.joinToString(",", "[", "]") { """"$it"""" }}
                }""".trimIndent()
            }
            return "[$vulnEntries]"
        }
    }

    class ClearlyDefinedRouteBuilder(private val routes: ConcurrentHashMap<RouteKey, RouteHandler>) {

        fun license(group: String, artifact: String, version: String, spdxId: String) {
            val path = "definitions/maven/mavencentral/$group/$artifact/$version"
            val json = """{"licensed":{"declared":"$spdxId"}}"""
            routes[RouteKey(path)] = RouteHandler { scope, _ ->
                scope.respondJson(json)
            }
        }

        fun noLicense(group: String, artifact: String, version: String) {
            val path = "definitions/maven/mavencentral/$group/$artifact/$version"
            val json = """{"licensed":{"declared":"NOASSERTION"}}"""
            routes[RouteKey(path)] = RouteHandler { scope, _ ->
                scope.respondJson(json)
            }
        }
    }
}

data class OsvVulnResponse(
    val id: String,
    val summary: String,
    val cvssScore: Double,
    val fixedVersion: String? = null,
    val aliases: List<String> = emptyList(),
)

fun mockHttpRouter(block: MockHttpRouter.Builder.() -> Unit): MockHttpRouter =
    MockHttpRouter.Builder().apply(block).build()

// --- Response helpers ---

private fun MockRequestHandleScope.respondXml(xml: String): HttpResponseData = respond(
    content = ByteReadChannel(xml.toByteArray()),
    status = HttpStatusCode.OK,
    headers = headersOf("Content-Type", ContentType.Application.Xml.toString()),
)

private fun MockRequestHandleScope.respondJson(json: String): HttpResponseData = respond(
    content = ByteReadChannel(json.toByteArray()),
    status = HttpStatusCode.OK,
    headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
)

private fun MockRequestHandleScope.respondNotFound(url: String): HttpResponseData = respond(
    content = ByteReadChannel("Not Found: $url".toByteArray()),
    status = HttpStatusCode.NotFound,
    headers = headersOf("Content-Type", ContentType.Text.Plain.toString()),
)
