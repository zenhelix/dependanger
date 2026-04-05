package io.github.zenhelix.dependanger.maven.client

import io.github.zenhelix.dependanger.http.client.HttpClientFactory
import io.github.zenhelix.dependanger.http.client.HttpResult
import io.github.zenhelix.dependanger.http.client.createDefault
import io.github.zenhelix.dependanger.http.client.getWithRetry
import io.ktor.client.plugins.timeout
import io.ktor.client.request.basicAuth

internal class DefaultMavenPomService(
    private val config: MavenClientConfig,
    httpClientFactory: HttpClientFactory,
) : MavenPomService {

    private val httpClient = httpClientFactory.createDefault(config.readTimeoutMs)

    override suspend fun downloadPom(
        group: String,
        artifact: String,
        version: String,
    ): DownloadResult {
        val groupPath = group.replace('.', '/')
        val pomFileName = "$artifact-$version.pom"

        var lastAuthRequired: DownloadResult.AuthRequired? = null
        var lastFailure: DownloadResult.Failed? = null
        var allNotFound = true

        for (repo in config.repositories) {
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
        val httpResult = httpClient.getWithRetry(url, config.retryConfig) {
            config.credentialsProvider?.getCredentials(repoUrl)?.let { creds ->
                basicAuth(creds.username, creds.password)
            }
            timeout {
                connectTimeoutMillis = config.connectTimeoutMs
                requestTimeoutMillis = config.readTimeoutMs
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

    override fun close() {
        httpClient.close()
    }
}
