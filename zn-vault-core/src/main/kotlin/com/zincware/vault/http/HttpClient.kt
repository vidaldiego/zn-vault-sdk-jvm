// Path: zn-vault-sdk-jvm/zn-vault-core/src/main/kotlin/com/zincware/vault/http/HttpClient.kt
package com.zincware.vault.http

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import com.zincware.vault.exception.*
import com.zincware.vault.models.ErrorResponse
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.Duration
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

/**
 * HTTP client for ZN-Vault API.
 */
class ZnVaultHttpClient(
    private val baseUrl: String,
    private val config: HttpClientConfig = HttpClientConfig()
) {
    private val logger = LoggerFactory.getLogger(ZnVaultHttpClient::class.java)
    private val client: OkHttpClient
    private val jsonMediaType = "application/json".toMediaType()

    @Volatile
    private var authProvider: AuthProvider? = null

    val objectMapper: ObjectMapper = jacksonObjectMapper().apply {
        // Create custom JavaTimeModule with lenient date parsing
        val javaTimeModule = JavaTimeModule()

        // Custom deserializer for Instant that handles multiple formats
        javaTimeModule.addDeserializer(Instant::class.java, FlexibleInstantDeserializer())

        registerModule(javaTimeModule)
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false)
        configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        // Note: API uses camelCase, individual fields use @JsonProperty for exceptions
    }

    init {
        val builder = OkHttpClient.Builder()
            .connectTimeout(config.connectTimeout.toMillis(), TimeUnit.MILLISECONDS)
            .readTimeout(config.readTimeout.toMillis(), TimeUnit.MILLISECONDS)
            .writeTimeout(config.writeTimeout.toMillis(), TimeUnit.MILLISECONDS)

        // Add auth interceptor
        builder.addInterceptor(AuthInterceptor { authProvider })

        // Add retry interceptor
        if (config.retryPolicy.maxRetries > 0) {
            builder.addInterceptor(RetryInterceptor(config.retryPolicy))
        }

        // Add logging interceptor in debug mode
        if (config.debug) {
            builder.addInterceptor(LoggingInterceptor(logger))
        }

        // Configure TLS if provided
        config.tlsConfig?.let { tls ->
            tls.configureTls(builder)
        }

        client = builder.build()
    }

    /**
     * Set the authentication provider.
     */
    fun setAuthProvider(provider: AuthProvider?) {
        this.authProvider = provider
    }

    /**
     * Perform a GET request.
     */
    fun <T> get(path: String, responseType: Class<T>): T {
        val request = Request.Builder()
            .url(buildUrl(path))
            .get()
            .build()

        return execute(request, responseType)
    }

    /**
     * Perform a GET request with TypeReference for generics.
     */
    fun <T> get(path: String, typeRef: TypeReference<T>): T {
        val request = Request.Builder()
            .url(buildUrl(path))
            .get()
            .build()

        return execute(request, typeRef)
    }

    /**
     * Perform a GET request and return raw text response.
     */
    fun getRaw(path: String): String {
        val request = Request.Builder()
            .url(buildUrl(path))
            .get()
            .build()

        return executeRaw(request)
    }

    /**
     * Perform a POST request.
     */
    fun <T, R> post(path: String, body: T, responseType: Class<R>): R {
        val jsonBody = objectMapper.writeValueAsString(body)
        val request = Request.Builder()
            .url(buildUrl(path))
            .post(jsonBody.toRequestBody(jsonMediaType))
            .build()

        return execute(request, responseType)
    }

    /**
     * Perform a POST request with TypeReference for generics.
     */
    fun <T, R> post(path: String, body: T, typeRef: TypeReference<R>): R {
        val jsonBody = objectMapper.writeValueAsString(body)
        val request = Request.Builder()
            .url(buildUrl(path))
            .post(jsonBody.toRequestBody(jsonMediaType))
            .build()

        return execute(request, typeRef)
    }

    /**
     * Perform a POST request with no body (sends empty JSON object).
     */
    fun <R> postEmpty(path: String, responseType: Class<R>): R {
        val request = Request.Builder()
            .url(buildUrl(path))
            .post("{}".toRequestBody(jsonMediaType))
            .build()

        return execute(request, responseType)
    }

    /**
     * Perform a POST request with no body and TypeReference for generics.
     */
    fun <R> postEmpty(path: String, typeRef: TypeReference<R>): R {
        val request = Request.Builder()
            .url(buildUrl(path))
            .post("{}".toRequestBody(jsonMediaType))
            .build()

        return execute(request, typeRef)
    }

    /**
     * Perform a PUT request.
     */
    fun <T, R> put(path: String, body: T, responseType: Class<R>): R {
        val jsonBody = objectMapper.writeValueAsString(body)
        val request = Request.Builder()
            .url(buildUrl(path))
            .put(jsonBody.toRequestBody(jsonMediaType))
            .build()

        return execute(request, responseType)
    }

    /**
     * Perform a PUT request with TypeReference for generics.
     */
    fun <T, R> put(path: String, body: T, typeRef: TypeReference<R>): R {
        val jsonBody = objectMapper.writeValueAsString(body)
        val request = Request.Builder()
            .url(buildUrl(path))
            .put(jsonBody.toRequestBody(jsonMediaType))
            .build()

        return execute(request, typeRef)
    }

    /**
     * Perform a PATCH request.
     */
    fun <T, R> patch(path: String, body: T, responseType: Class<R>): R {
        val jsonBody = objectMapper.writeValueAsString(body)
        val request = Request.Builder()
            .url(buildUrl(path))
            .patch(jsonBody.toRequestBody(jsonMediaType))
            .build()

        return execute(request, responseType)
    }

    /**
     * Perform a PATCH request with TypeReference for generics.
     */
    fun <T, R> patch(path: String, body: T, typeRef: TypeReference<R>): R {
        val jsonBody = objectMapper.writeValueAsString(body)
        val request = Request.Builder()
            .url(buildUrl(path))
            .patch(jsonBody.toRequestBody(jsonMediaType))
            .build()

        return execute(request, typeRef)
    }

    /**
     * Perform a DELETE request.
     */
    fun delete(path: String) {
        val request = Request.Builder()
            .url(buildUrl(path))
            .delete()
            .build()

        executeNoContent(request)
    }

    /**
     * Perform a DELETE request and return response.
     */
    fun <R> delete(path: String, responseType: Class<R>): R {
        val request = Request.Builder()
            .url(buildUrl(path))
            .delete()
            .build()

        return execute(request, responseType)
    }

    private fun buildUrl(path: String): String {
        val cleanBase = baseUrl.trimEnd('/')
        val cleanPath = path.trimStart('/')
        return "$cleanBase/$cleanPath"
    }

    private fun <T> execute(request: Request, responseType: Class<T>): T {
        try {
            client.newCall(request).execute().use { response ->
                handleErrorResponse(response)
                val body = response.body?.string() ?: throw ZnVaultException("Empty response body")
                return objectMapper.readValue(body, responseType)
            }
        } catch (e: ZnVaultException) {
            throw e
        } catch (e: SocketTimeoutException) {
            throw TimeoutException("Request timed out", e)
        } catch (e: SSLException) {
            throw TlsException("TLS error: ${e.message}", e)
        } catch (e: IOException) {
            throw ConnectionException("Connection failed: ${e.message}", e)
        }
    }

    private fun <T> execute(request: Request, typeRef: TypeReference<T>): T {
        try {
            client.newCall(request).execute().use { response ->
                handleErrorResponse(response)
                val body = response.body?.string() ?: throw ZnVaultException("Empty response body")
                return objectMapper.readValue(body, typeRef)
            }
        } catch (e: ZnVaultException) {
            throw e
        } catch (e: SocketTimeoutException) {
            throw TimeoutException("Request timed out", e)
        } catch (e: SSLException) {
            throw TlsException("TLS error: ${e.message}", e)
        } catch (e: IOException) {
            throw ConnectionException("Connection failed: ${e.message}", e)
        }
    }

    private fun executeNoContent(request: Request) {
        try {
            client.newCall(request).execute().use { response ->
                handleErrorResponse(response)
            }
        } catch (e: ZnVaultException) {
            throw e
        } catch (e: SocketTimeoutException) {
            throw TimeoutException("Request timed out", e)
        } catch (e: SSLException) {
            throw TlsException("TLS error: ${e.message}", e)
        } catch (e: IOException) {
            throw ConnectionException("Connection failed: ${e.message}", e)
        }
    }

    private fun executeRaw(request: Request): String {
        try {
            client.newCall(request).execute().use { response ->
                handleErrorResponse(response)
                return response.body?.string() ?: ""
            }
        } catch (e: ZnVaultException) {
            throw e
        } catch (e: SocketTimeoutException) {
            throw TimeoutException("Request timed out", e)
        } catch (e: SSLException) {
            throw TlsException("TLS error: ${e.message}", e)
        } catch (e: IOException) {
            throw ConnectionException("Connection failed: ${e.message}", e)
        }
    }

    private fun handleErrorResponse(response: Response) {
        if (response.isSuccessful) return

        val body = response.body?.string()
        val errorResponse = try {
            body?.let { objectMapper.readValue(it, ErrorResponse::class.java) }
        } catch (e: Exception) {
            null
        }

        val message = errorResponse?.message ?: body ?: "Unknown error"
        val errorCode = errorResponse?.error

        throw when (response.code) {
            400 -> ValidationException(
                message = message,
                errors = errorResponse?.errors ?: emptyList()
            )
            401 -> when {
                message.contains("2fa", ignoreCase = true) ||
                message.contains("two-factor", ignoreCase = true) ->
                    TwoFactorRequiredException(message)
                message.contains("totp", ignoreCase = true) ->
                    InvalidTotpException(message)
                else -> AuthenticationException(message, errorCode)
            }
            403 -> AuthorizationException(message, errorCode)
            404 -> NotFoundException(message)
            409 -> ConflictException(message)
            410 -> ExpiredException(message)
            423 -> AccountLockedException(message)
            429 -> {
                val retryAfter = response.header("Retry-After")?.toIntOrNull()
                if (message.contains("quota", ignoreCase = true)) {
                    QuotaExceededException(message)
                } else {
                    RateLimitException(message, retryAfter)
                }
            }
            in 500..599 -> ServerException(message, response.code)
            else -> ZnVaultException(message, response.code, errorCode)
        }
    }
}

/**
 * HTTP client configuration.
 */
data class HttpClientConfig(
    val connectTimeout: Duration = Duration.ofSeconds(30),
    val readTimeout: Duration = Duration.ofSeconds(30),
    val writeTimeout: Duration = Duration.ofSeconds(30),
    val retryPolicy: RetryPolicy = RetryPolicy(),
    val tlsConfig: TlsConfig? = null,
    val debug: Boolean = false
)

/**
 * Authentication provider interface.
 */
interface AuthProvider {
    /**
     * Get the Authorization header value (e.g., "Bearer <token>").
     * Return null if not using Authorization header.
     */
    fun getAuthHeader(): String?

    /**
     * Get the API key for X-API-Key header.
     * Return null if not using API key authentication.
     */
    fun getApiKey(): String? = null
}

/**
 * Extended auth provider that supports credential refresh.
 *
 * Implement this interface when credentials may become stale and need
 * to be reloaded (e.g., from a file that gets updated by an external process).
 */
interface RefreshableAuthProvider : AuthProvider {
    /**
     * Called when an authentication error (401) occurs.
     *
     * The provider should refresh its credentials from the source
     * (e.g., re-read from file). Returns true if credentials were
     * successfully refreshed and the request should be retried.
     *
     * @return true if credentials were refreshed and retry should occur
     */
    fun onAuthenticationError(): Boolean
}

/**
 * Interceptor that adds authentication headers.
 *
 * If the provider implements [RefreshableAuthProvider] and a 401 response
 * is received, the interceptor will call [RefreshableAuthProvider.onAuthenticationError]
 * and retry the request once with refreshed credentials.
 */
private class AuthInterceptor(
    private val authProviderSupplier: () -> AuthProvider?
) : Interceptor {
    private val logger = LoggerFactory.getLogger(AuthInterceptor::class.java)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val provider = authProviderSupplier()

        if (provider == null) {
            return chain.proceed(request)
        }

        // Build request with current credentials
        val authenticatedRequest = addAuthHeaders(request, provider)
        val response = chain.proceed(authenticatedRequest)

        // Check for 401 and retry with refreshed credentials if supported
        if (response.code == 401 && provider is RefreshableAuthProvider) {
            logger.debug("Received 401, attempting credential refresh")

            // Close the original response body before retrying
            response.close()

            // Try to refresh credentials
            if (provider.onAuthenticationError()) {
                logger.debug("Credentials refreshed, retrying request")

                // Retry with refreshed credentials
                val retryRequest = addAuthHeaders(request, provider)
                return chain.proceed(retryRequest)
            } else {
                logger.debug("Credential refresh failed or not available")
                // Re-execute the original request to get a fresh response
                // (since we closed the previous one)
                return chain.proceed(authenticatedRequest)
            }
        }

        return response
    }

    private fun addAuthHeaders(request: Request, provider: AuthProvider): Request {
        val builder = request.newBuilder()

        // Add Authorization header (JWT)
        provider.getAuthHeader()?.let { authHeader ->
            builder.header("Authorization", authHeader)
        }

        // Add X-API-Key header (API key auth)
        provider.getApiKey()?.let { apiKey ->
            builder.header("X-API-Key", apiKey)
        }

        return builder.build()
    }
}

/**
 * Simple logging interceptor.
 */
private class LoggingInterceptor(
    private val logger: org.slf4j.Logger
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        logger.debug("${request.method} ${request.url}")

        val startTime = System.nanoTime()
        val response = chain.proceed(request)
        val duration = (System.nanoTime() - startTime) / 1_000_000

        logger.debug("${response.code} ${request.url} (${duration}ms)")
        return response
    }
}

/**
 * Flexible Instant deserializer that handles multiple date formats:
 * - ISO-8601: 2025-12-02T13:44:26.191Z
 * - Space-separated: 2025-12-02 13:46:40
 * - PostgreSQL with offset: 2026-01-04 22:14:37.688+00
 */
private class FlexibleInstantDeserializer : com.fasterxml.jackson.databind.JsonDeserializer<Instant>() {

    // Space-separated without timezone
    private val spaceFormatter = DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd HH:mm:ss")
        .optionalStart()
        .appendFraction(ChronoField.MICRO_OF_SECOND, 0, 6, true)
        .optionalEnd()
        .toFormatter()

    // PostgreSQL format with timezone offset (e.g., +00)
    private val postgresFormatter = DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd HH:mm:ss")
        .optionalStart()
        .appendFraction(ChronoField.MICRO_OF_SECOND, 0, 6, true)
        .optionalEnd()
        .optionalStart()
        .appendOffset("+HH", "+00")
        .optionalEnd()
        .toFormatter()

    override fun deserialize(
        p: com.fasterxml.jackson.core.JsonParser,
        ctxt: com.fasterxml.jackson.databind.DeserializationContext
    ): Instant? {
        val text = p.text?.trim() ?: return null
        if (text.isEmpty()) return null

        return try {
            // Try ISO-8601 first (most common)
            Instant.parse(text)
        } catch (e: Exception) {
            try {
                // Try PostgreSQL format with offset (e.g., "2026-01-04 22:14:37.688+00")
                val odt = OffsetDateTime.parse(text, postgresFormatter)
                odt.toInstant()
            } catch (e2: Exception) {
                try {
                    // Try space-separated format without timezone (assume UTC)
                    LocalDateTime.parse(text, spaceFormatter).toInstant(ZoneOffset.UTC)
                } catch (e3: Exception) {
                    throw ctxt.weirdStringException(text, Instant::class.java, "Cannot parse date: $text")
                }
            }
        }
    }
}
