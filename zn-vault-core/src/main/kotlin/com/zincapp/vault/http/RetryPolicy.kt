// Path: zn-vault-sdk-jvm/zn-vault-core/src/main/kotlin/com/zincapp/vault/http/RetryPolicy.kt
package com.zincapp.vault.http

import okhttp3.Interceptor
import okhttp3.Response
import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.Duration
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * Retry policy configuration with exponential backoff.
 */
data class RetryPolicy(
    val maxRetries: Int = 3,
    val initialDelay: Duration = Duration.ofMillis(100),
    val maxDelay: Duration = Duration.ofSeconds(10),
    val multiplier: Double = 2.0,
    val jitterFactor: Double = 0.1,
    val retryableStatusCodes: Set<Int> = setOf(429, 502, 503, 504),
    val retryOnConnectionFailure: Boolean = true
) {
    companion object {
        /**
         * No retries.
         */
        fun none() = RetryPolicy(maxRetries = 0)

        /**
         * Default retry policy with 3 retries.
         */
        fun default() = RetryPolicy()

        /**
         * Aggressive retry policy for critical operations.
         */
        fun aggressive() = RetryPolicy(
            maxRetries = 5,
            initialDelay = Duration.ofMillis(50),
            maxDelay = Duration.ofSeconds(30)
        )
    }

    /**
     * Calculate delay for the given attempt number.
     */
    fun calculateDelay(attempt: Int): Duration {
        val exponentialDelay = initialDelay.toMillis() * multiplier.pow(attempt.toDouble())
        val cappedDelay = min(exponentialDelay, maxDelay.toMillis().toDouble())

        // Add jitter
        val jitter = cappedDelay * jitterFactor * Random.nextDouble(-1.0, 1.0)
        val finalDelay = (cappedDelay + jitter).toLong().coerceAtLeast(0)

        return Duration.ofMillis(finalDelay)
    }

    /**
     * Check if a status code is retryable.
     */
    fun isRetryable(statusCode: Int): Boolean {
        return statusCode in retryableStatusCodes
    }
}

/**
 * OkHttp interceptor that implements retry logic.
 */
class RetryInterceptor(
    private val policy: RetryPolicy
) : Interceptor {
    private val logger = LoggerFactory.getLogger(RetryInterceptor::class.java)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var lastException: IOException? = null
        var lastResponse: Response? = null

        repeat(policy.maxRetries + 1) { attempt ->
            try {
                // Close previous response if any
                lastResponse?.close()

                val response = chain.proceed(request)

                // Check if we should retry based on status code
                if (attempt < policy.maxRetries && policy.isRetryable(response.code)) {
                    val delay = getRetryDelay(response, attempt)
                    logger.debug(
                        "Retrying request to {} after {}ms (attempt {}/{}, status {})",
                        request.url, delay.toMillis(), attempt + 1, policy.maxRetries, response.code
                    )
                    response.close()
                    Thread.sleep(delay.toMillis())
                    return@repeat
                }

                return response
            } catch (e: IOException) {
                lastException = e

                if (!policy.retryOnConnectionFailure || attempt >= policy.maxRetries) {
                    throw e
                }

                val delay = policy.calculateDelay(attempt)
                logger.debug(
                    "Retrying request to {} after {}ms (attempt {}/{}, error: {})",
                    request.url, delay.toMillis(), attempt + 1, policy.maxRetries, e.message
                )
                Thread.sleep(delay.toMillis())
            }
        }

        // Should not reach here, but just in case
        throw lastException ?: IOException("Unknown error after retries")
    }

    private fun getRetryDelay(response: Response, attempt: Int): Duration {
        // Check for Retry-After header
        val retryAfter = response.header("Retry-After")
        if (retryAfter != null) {
            val seconds = retryAfter.toLongOrNull()
            if (seconds != null) {
                return Duration.ofSeconds(seconds)
            }
        }

        // Fall back to exponential backoff
        return policy.calculateDelay(attempt)
    }
}
