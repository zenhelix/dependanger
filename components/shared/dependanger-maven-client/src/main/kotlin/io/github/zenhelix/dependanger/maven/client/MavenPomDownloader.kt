package io.github.zenhelix.dependanger.maven.client

import io.github.zenhelix.dependanger.core.model.CredentialsProvider
import io.github.zenhelix.dependanger.core.model.MavenRepository
import io.github.zenhelix.dependanger.http.client.HttpClientConfig
import io.github.zenhelix.dependanger.http.client.HttpResult
import io.github.zenhelix.dependanger.http.client.RetryConfig
import io.github.zenhelix.dependanger.http.client.getWithRetry
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.basicAuth

public class MavenPomDownloader(
    private val repositories: List<MavenRepository>,
    private val httpClient: HttpClient,
    private val credentialsProvider: CredentialsProvider?,
    private val connectTimeoutMs: Long = HttpClientConfig.DEFAULT_CONNECT_TIMEOUT_MS,
    private val readTimeoutMs: Long,
) {
    public suspend fun downloadPom(
        group: String,
        artifact: String,
        version: String,
    ): DownloadResult {
        val groupPath = group.replace('.', '/')
        val pomFileName = "$artifact-$version.pom"

        var lastAuthRequired: DownloadResult.AuthRequired? = null
        var lastFailure: DownloadResult.Failed? = null
        var allNotFound = true

        for (repo in repositories) {
            val url = "${repo.url.trimEnd('/')}/$groupPath/$artifact/$version/$pomFileName"
            when (val result = downloadWithRetry(url, repo.url)) {
                is DownloadResult.Success      -> return result
                is DownloadResult.AuthRequired -> {
                    allNotFound = false
                    lastAuthRequired = result
                }

                is DownloadResult.NotFound     -> { /* continue to next repo */
                }

                is DownloadResult.Failed       -> {
                    allNotFound = false
                    lastFailure = result
                }
            }
        }

        return when {
            lastAuthRequired != null -> lastAuthRequired
            allNotFound              -> DownloadResult.NotFound
            lastFailure != null      -> lastFailure
            else                     -> DownloadResult.NotFound
        }
    }

    private suspend fun downloadWithRetry(url: String, repoUrl: String): DownloadResult {
        val httpResult = httpClient.getWithRetry(url, RetryConfig()) {
            credentialsProvider?.getCredentials(repoUrl)?.let { creds ->
                basicAuth(creds.username, creds.password)
            }
            timeout {
                connectTimeoutMillis = connectTimeoutMs
                requestTimeoutMillis = readTimeoutMs
            }
        }

        return when (httpResult) {
            is HttpResult.Success      -> DownloadResult.Success(httpResult.data)
            is HttpResult.NotFound     -> DownloadResult.NotFound
            is HttpResult.AuthRequired -> DownloadResult.AuthRequired(httpResult.url, httpResult.statusCode)
            is HttpResult.RateLimited  -> DownloadResult.Failed("Rate limited for $url, retry after ${httpResult.retryAfterMs}ms")
            is HttpResult.Failed       -> DownloadResult.Failed(httpResult.error)
        }
    }
}
