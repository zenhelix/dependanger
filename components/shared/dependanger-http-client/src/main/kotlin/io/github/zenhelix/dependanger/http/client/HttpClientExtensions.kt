package io.github.zenhelix.dependanger.http.client

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode

private val logger = KotlinLogging.logger {}

public suspend fun HttpClient.getWithRetry(
    url: String,
    retryConfig: RetryConfig = RetryConfig(),
    requestBuilder: HttpRequestBuilder.() -> Unit = {},
): HttpResult<String> = executeWithMapping(url) { get(url, requestBuilder) }

public suspend fun HttpClient.postWithRetry(
    url: String,
    retryConfig: RetryConfig = RetryConfig(),
    requestBuilder: HttpRequestBuilder.() -> Unit = {},
): HttpResult<String> = executeWithMapping(url) { post(url, requestBuilder) }

private suspend fun executeWithMapping(
    url: String,
    httpCall: suspend () -> HttpResponse,
): HttpResult<String> = try {
    val response = httpCall()
    mapResponse(response, url)
} catch (e: CircuitBreakerOpenException) {
    logger.debug { "Circuit breaker open for ${e.host}, skipping request to $url" }
    HttpResult.Failed("Circuit breaker open for ${e.host}", e)
} catch (e: Exception) {
    logger.debug(e) { "Request to $url failed: ${e.message}" }
    HttpResult.Failed("Request to $url failed: ${e.message}", e)
}

private suspend fun mapResponse(response: HttpResponse, url: String): HttpResult<String> = when {
    response.status == HttpStatusCode.OK                                                          ->
        HttpResult.Success(response.bodyAsText())

    response.status == HttpStatusCode.NotFound                                                    ->
        HttpResult.NotFound

    response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden -> {
        logger.debug { "Authentication required for $url (${response.status})" }
        HttpResult.AuthRequired(url, response.status.value)
    }

    response.status == HttpStatusCode.TooManyRequests                                             -> {
        val retryAfterMs = response.headers["Retry-After"]?.toLongOrNull()?.let { it * 1000 }
        HttpResult.RateLimited(retryAfterMs)
    }

    response.status.value >= 500                                                                  -> {
        logger.debug { "Server error ${response.status} for $url" }
        HttpResult.Failed("Server error ${response.status} for $url")
    }

    else                                                                                          -> {
        logger.debug { "Unexpected status ${response.status} for $url" }
        HttpResult.Failed("Unexpected status ${response.status} for $url")
    }
}
