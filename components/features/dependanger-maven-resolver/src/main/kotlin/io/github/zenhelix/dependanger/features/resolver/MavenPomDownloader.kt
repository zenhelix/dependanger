package io.github.zenhelix.dependanger.features.resolver

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.zenhelix.dependanger.core.model.CredentialsProvider
import io.github.zenhelix.dependanger.core.model.MavenRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.timeout
import io.ktor.client.request.basicAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay

private val logger = KotlinLogging.logger {}

public class MavenPomDownloader(
    private val repositories: List<MavenRepository>,
    private val httpClient: HttpClient,
    private val credentialsProvider: CredentialsProvider?,
    private val connectTimeoutMs: Long,
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

    private suspend fun downloadWithRetry(url: String, repoUrl: String, maxRetries: Int = 3): DownloadResult {
        var delayMs = 1000L
        repeat(maxRetries) { attempt ->
            try {
                val response = httpClient.get(url) {
                    credentialsProvider?.getCredentials(repoUrl)?.let { creds ->
                        basicAuth(creds.username, creds.password)
                    }
                    timeout {
                        connectTimeoutMillis = connectTimeoutMs
                        requestTimeoutMillis = readTimeoutMs
                    }
                }
                when {
                    response.status == HttpStatusCode.OK                                    -> return DownloadResult.Success(response.bodyAsText())
                    response.status == HttpStatusCode.NotFound                              -> return DownloadResult.NotFound
                    response.status == HttpStatusCode.Unauthorized ||
                            response.status == HttpStatusCode.Forbidden -> {
                        logger.debug { "Authentication required for $url (${response.status})" }
                        return DownloadResult.AuthRequired(url, response.status.value)
                    }

                    response.status == HttpStatusCode.TooManyRequests   -> {
                        val retryAfterMs = response.headers["Retry-After"]?.toLongOrNull()
                            ?.let { it * 1000 }
                            ?: delayMs
                        delay(retryAfterMs)
                        delayMs *= 2
                    }

                    response.status.value >= 500                        -> {
                        if (attempt == maxRetries - 1) {
                            logger.debug { "Server error for $url after $maxRetries attempts: ${response.status}" }
                            return DownloadResult.Failed("Server error for $url after $maxRetries attempts: ${response.status}")
                        }
                        delay(delayMs)
                        delayMs *= 2
                    }

                    else                                                -> {
                        logger.debug { "Unexpected status ${response.status} for $url" }
                        return DownloadResult.Failed("Unexpected status ${response.status} for $url")
                    }
                }
            } catch (e: HttpRequestTimeoutException) {
                if (attempt == maxRetries - 1) {
                    logger.debug(e) { "Timeout downloading $url after $maxRetries attempts" }
                    return DownloadResult.Failed("Timeout downloading $url after $maxRetries attempts: ${e.message}")
                }
                delay(delayMs)
                delayMs *= 2
            } catch (e: Exception) {
                if (attempt == maxRetries - 1) {
                    logger.debug(e) { "Failed to download $url after $maxRetries attempts" }
                    return DownloadResult.Failed("Failed to download $url after $maxRetries attempts: ${e.message}")
                }
                delay(delayMs)
                delayMs *= 2
            }
        }
        return DownloadResult.Failed("Failed to download $url after exhausting retries")
    }
}
