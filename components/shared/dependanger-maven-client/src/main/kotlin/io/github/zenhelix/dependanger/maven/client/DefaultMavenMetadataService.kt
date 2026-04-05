package io.github.zenhelix.dependanger.maven.client

import io.github.zenhelix.dependanger.core.model.MavenRepository
import io.github.zenhelix.dependanger.http.client.HttpClientFactory
import io.github.zenhelix.dependanger.http.client.HttpResult
import io.github.zenhelix.dependanger.http.client.createDefault
import io.github.zenhelix.dependanger.http.client.getWithRetry
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.timeout
import io.ktor.client.request.basicAuth

private val versionRegex = Regex("<version>([^<]+)</version>")

internal class DefaultMavenMetadataService(
    private val config: MavenClientConfig,
    httpClientFactory: HttpClientFactory,
) : MavenMetadataService {

    private val httpClient = httpClientFactory.createDefault(config.readTimeoutMs)

    override suspend fun fetchVersions(group: String, artifact: String): MetadataFetchResult {
        val groupPath = group.replace('.', '/')
        val metadataFileName = "maven-metadata.xml"

        var lastFailure: MetadataFetchResult? = null

        for (repo in config.repositories) {
            val url = "${repo.url.trimEnd('/')}/$groupPath/$artifact/$metadataFileName"
            when (val result = fetchFromRepo(url, repo)) {
                is MetadataFetchResult.Success  -> return result
                is MetadataFetchResult.NotFound -> { /* continue to next repo */
                }

                is MetadataFetchResult.RateLimited,
                is MetadataFetchResult.TimedOut,
                is MetadataFetchResult.Failed   -> lastFailure = result
            }
        }

        return lastFailure ?: MetadataFetchResult.NotFound
    }

    private suspend fun fetchFromRepo(url: String, repo: MavenRepository): MetadataFetchResult {
        val httpResult = httpClient.getWithRetry(url, config.retryConfig) {
            config.credentialsProvider?.getCredentials(repo.url)?.let { creds ->
                basicAuth(creds.username, creds.password)
            }
            timeout {
                connectTimeoutMillis = config.connectTimeoutMs
                requestTimeoutMillis = config.readTimeoutMs
            }
        }

        return when (httpResult) {
            is HttpResult.Success      -> {
                val versions = versionRegex.findAll(httpResult.data)
                    .map { it.groupValues[1] }
                    .toList()
                MetadataFetchResult.Success(versions = versions, repository = repo.name)
            }

            is HttpResult.NotFound     -> MetadataFetchResult.NotFound
            is HttpResult.AuthRequired -> MetadataFetchResult.Failed("Authentication required for ${repo.name} ($url)")
            is HttpResult.RateLimited  -> MetadataFetchResult.RateLimited("Rate limited by ${repo.name}, retry after ${httpResult.retryAfterMs}ms")
            is HttpResult.Failed       -> classifyFailure(httpResult, repo)
        }
    }

    private fun classifyFailure(result: HttpResult.Failed, repo: MavenRepository): MetadataFetchResult {
        if (result.cause is HttpRequestTimeoutException) {
            return MetadataFetchResult.TimedOut("Timeout fetching metadata from ${repo.name}: ${result.error}")
        }
        return MetadataFetchResult.Failed(result.error)
    }

    override fun close() {
        httpClient.close()
    }
}
