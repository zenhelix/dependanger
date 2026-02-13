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
    private val credentialsProvider: CredentialsProvider? = null,
    private val connectTimeoutMs: Long = 30_000,
    private val readTimeoutMs: Long = 60_000,
) {
    public suspend fun downloadPom(
        group: String,
        artifact: String,
        version: String,
    ): String? {
        val groupPath = group.replace('.', '/')
        val pomFileName = "$artifact-$version.pom"

        for (repo in repositories) {
            val url = "${repo.url.trimEnd('/')}/$groupPath/$artifact/$version/$pomFileName"
            try {
                val result = downloadWithRetry(url, repo.url)
                if (result != null) return result
            } catch (e: Exception) {
                logger.warn { "Failed to download POM from ${repo.url}: ${e.message}" }
            }
        }
        return null
    }

    private suspend fun downloadWithRetry(url: String, repoUrl: String, maxRetries: Int = 3): String? {
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
                    response.status == HttpStatusCode.OK                -> return response.bodyAsText()
                    response.status == HttpStatusCode.NotFound          -> return null
                    response.status == HttpStatusCode.Unauthorized ||
                            response.status == HttpStatusCode.Forbidden -> {
                        logger.warn { "Authentication required for $url (${response.status})" }
                        return null
                    }

                    response.status == HttpStatusCode.TooManyRequests   -> {
                        val retryAfter = response.headers["Retry-After"]?.toLongOrNull() ?: delayMs / 1000
                        delay(retryAfter * 1000)
                        delayMs *= 2
                    }

                    response.status.value >= 500                        -> {
                        if (attempt == maxRetries - 1) {
                            logger.warn { "Server error for $url after $maxRetries attempts: ${response.status}" }
                            return null
                        }
                        delay(delayMs)
                        delayMs *= 2
                    }

                    else                                                -> {
                        logger.warn { "Unexpected status ${response.status} for $url" }
                        return null
                    }
                }
            } catch (e: HttpRequestTimeoutException) {
                if (attempt == maxRetries - 1) throw e
                delay(delayMs)
                delayMs *= 2
            } catch (e: Exception) {
                if (attempt == maxRetries - 1) throw e
                delay(delayMs)
                delayMs *= 2
            }
        }
        return null
    }
}
