package io.github.zenhelix.dependanger.features.updates

import io.github.zenhelix.dependanger.core.model.CredentialsProvider
import io.github.zenhelix.dependanger.core.model.MavenRepository
import io.github.zenhelix.dependanger.http.client.HttpResult
import io.github.zenhelix.dependanger.http.client.RetryConfig
import io.github.zenhelix.dependanger.http.client.getWithRetry
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.basicAuth

private val versionRegex = Regex("<version>([^<]+)</version>")

public class MavenMetadataFetcher(
    private val repositories: List<MavenRepository>,
    private val httpClient: HttpClient,
    private val credentialsProvider: CredentialsProvider?,
    private val connectTimeoutMs: Long,
    private val readTimeoutMs: Long,
) {
    public suspend fun fetchVersions(group: String, artifact: String): MetadataFetchResult {
        val groupPath = group.replace('.', '/')
        val metadataFileName = "maven-metadata.xml"

        var lastFailure: MetadataFetchResult.Failed? = null

        for (repo in repositories) {
            val url = "${repo.url.trimEnd('/')}/$groupPath/$artifact/$metadataFileName"
            when (val result = fetchFromRepo(url, repo)) {
                is MetadataFetchResult.Success  -> return result
                is MetadataFetchResult.NotFound -> { /* continue to next repo */
                }

                is MetadataFetchResult.Failed   -> lastFailure = result
            }
        }

        return lastFailure ?: MetadataFetchResult.NotFound
    }

    private suspend fun fetchFromRepo(url: String, repo: MavenRepository): MetadataFetchResult {
        val httpResult = httpClient.getWithRetry(url, RetryConfig()) {
            credentialsProvider?.getCredentials(repo.url)?.let { creds ->
                basicAuth(creds.username, creds.password)
            }
            timeout {
                connectTimeoutMillis = connectTimeoutMs
                requestTimeoutMillis = readTimeoutMs
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
            is HttpResult.RateLimited  -> MetadataFetchResult.Failed("Rate limited by ${repo.name}, retry after ${httpResult.retryAfterMs}ms")
            is HttpResult.Failed       -> MetadataFetchResult.Failed(httpResult.error)
        }
    }
}
