package io.github.zenhelix.dependanger.http.client

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlin.math.min

private val logger = KotlinLogging.logger {}

public suspend fun HttpClient.getWithRetry(
    url: String,
    retryConfig: RetryConfig = RetryConfig(),
    requestBuilder: HttpRequestBuilder.() -> Unit = {},
): HttpResult<String> {
    var delayMs = retryConfig.initialDelayMs

    repeat(retryConfig.maxRetries) { attempt ->
        try {
            val response = get(url, requestBuilder)
            when (val result = mapResponse(response, url)) {
                is RetryDecision.Return -> return result.httpResult
                is RetryDecision.Retry  -> {
                    if (attempt == retryConfig.maxRetries - 1) {
                        logger.debug { "Server error for $url after ${retryConfig.maxRetries} attempts: ${response.status}" }
                        return HttpResult.Failed("Server error for $url after ${retryConfig.maxRetries} attempts: ${response.status}")
                    }
                    val waitMs = result.overrideDelayMs ?: delayMs
                    delay(waitMs)
                    delayMs = nextDelay(delayMs, null, retryConfig)
                }
            }
        } catch (e: HttpRequestTimeoutException) {
            if (attempt == retryConfig.maxRetries - 1) {
                logger.debug(e) { "Timeout for $url after ${retryConfig.maxRetries} attempts" }
                return HttpResult.Failed("Timeout for $url after ${retryConfig.maxRetries} attempts: ${e.message}", e)
            }
            delay(delayMs)
            delayMs = nextDelay(delayMs, null, retryConfig)
        } catch (e: Exception) {
            if (attempt == retryConfig.maxRetries - 1) {
                logger.debug(e) { "Failed request to $url after ${retryConfig.maxRetries} attempts" }
                return HttpResult.Failed("Failed request to $url after ${retryConfig.maxRetries} attempts: ${e.message}", e)
            }
            delay(delayMs)
            delayMs = nextDelay(delayMs, null, retryConfig)
        }
    }
    return HttpResult.Failed("Failed request to $url after exhausting retries")
}

private sealed interface RetryDecision {
    data class Return(val httpResult: HttpResult<String>) : RetryDecision
    data class Retry(val overrideDelayMs: Long? = null) : RetryDecision
}

private suspend fun mapResponse(response: HttpResponse, url: String): RetryDecision = when {
    response.status == HttpStatusCode.OK                                                          ->
        RetryDecision.Return(HttpResult.Success(response.bodyAsText()))

    response.status == HttpStatusCode.NotFound                                                    ->
        RetryDecision.Return(HttpResult.NotFound)

    response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden -> {
        logger.debug { "Authentication required for $url (${response.status})" }
        RetryDecision.Return(HttpResult.AuthRequired(url, response.status.value))
    }

    response.status == HttpStatusCode.TooManyRequests                                             -> {
        val retryAfterMs = response.headers["Retry-After"]?.toLongOrNull()?.let { it * 1000 }
        RetryDecision.Retry(overrideDelayMs = retryAfterMs)
    }

    response.status.value >= 500 -> RetryDecision.Retry()

    else                                                                                          -> {
        logger.debug { "Unexpected status ${response.status} for $url" }
        RetryDecision.Return(HttpResult.Failed("Unexpected status ${response.status} for $url"))
    }
}

private fun nextDelay(currentDelayMs: Long, overrideMs: Long?, config: RetryConfig): Long {
    val base = overrideMs ?: (currentDelayMs * config.backoffMultiplier).toLong()
    return min(base, config.maxDelayMs)
}
