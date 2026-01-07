// Path: zn-vault-sdk-jvm/zn-vault-core/src/main/kotlin/com/zincware/vault/auth/ApiKeyAuth.kt
package com.zincware.vault.auth

import com.zincware.vault.http.AuthProvider
import com.zincware.vault.http.RefreshableAuthProvider
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException

/**
 * API key authentication provider.
 *
 * Use this for service-to-service authentication with long-lived API keys.
 */
class ApiKeyAuth(
    private val apiKey: String
) : AuthProvider {

    /**
     * Get the authorization header value for API key authentication.
     *
     * Note: ZN-Vault uses X-API-Key header, but this is handled specially
     * in the HTTP client. This returns the key for the standard auth flow.
     */
    override fun getAuthHeader(): String? {
        return null // API key uses X-API-Key header, not Authorization
    }

    /**
     * Get the API key value for the X-API-Key header.
     */
    override fun getApiKey(): String = apiKey

    companion object {
        /**
         * Create an API key auth provider from an API key string.
         */
        @JvmStatic
        fun of(apiKey: String): ApiKeyAuth = ApiKeyAuth(apiKey)
    }
}

/**
 * File-based API key authentication provider with automatic refresh.
 *
 * This provider reads the API key from a file and supports automatic
 * credential refresh when authentication fails. Use this when an external
 * process (like zn-vault-agent) manages and rotates the API key.
 *
 * ## Usage
 *
 * ```kotlin
 * // The agent writes the current key to this file
 * val provider = FileApiKeyAuth("/run/zn-vault-agent/secrets/VAULT_API_KEY")
 *
 * // Or auto-detect from environment
 * val provider = FileApiKeyAuth.fromEnv("VAULT_API_KEY")
 * ```
 *
 * ## How it works
 *
 * 1. On initialization, reads the API key from the file
 * 2. Caches the key in memory for subsequent requests
 * 3. When a 401 error occurs, re-reads the file and retries
 * 4. This handles key rotation by the agent transparently
 *
 * ## File format
 *
 * The file should contain only the API key value (no newlines or whitespace):
 * ```
 * znv_abc123...
 * ```
 *
 * @param filePath Path to the file containing the API key
 * @throws IOException if the file cannot be read on initialization
 */
class FileApiKeyAuth(
    private val filePath: String
) : RefreshableAuthProvider {

    private val logger = LoggerFactory.getLogger(FileApiKeyAuth::class.java)

    @Volatile
    private var cachedKey: String

    @Volatile
    private var lastReadTime: Long = 0

    init {
        // Read initial key from file
        cachedKey = readKeyFromFile()
        logger.debug("Loaded API key from file: {} (prefix: {}...)",
            filePath, cachedKey.take(8))
    }

    override fun getAuthHeader(): String? = null

    override fun getApiKey(): String = cachedKey

    /**
     * Called when authentication fails (401).
     *
     * Re-reads the API key from the file. If the key has changed,
     * returns true to indicate the request should be retried.
     */
    override fun onAuthenticationError(): Boolean {
        val oldKey = cachedKey

        return try {
            val newKey = readKeyFromFile()

            if (newKey != oldKey) {
                cachedKey = newKey
                logger.info("API key refreshed from file: {} (old prefix: {}..., new prefix: {}...)",
                    filePath, oldKey.take(8), newKey.take(8))
                true // Key changed, retry the request
            } else {
                logger.debug("API key unchanged after re-read, not retrying")
                false // Key is the same, don't retry (it's a real auth error)
            }
        } catch (e: IOException) {
            logger.warn("Failed to refresh API key from file: {}", e.message)
            false // Can't refresh, don't retry
        }
    }

    /**
     * Force a refresh of the cached API key from the file.
     *
     * @return The new API key value
     * @throws IOException if the file cannot be read
     */
    fun refresh(): String {
        cachedKey = readKeyFromFile()
        logger.debug("Manually refreshed API key from file: {}", filePath)
        return cachedKey
    }

    private fun readKeyFromFile(): String {
        val file = File(filePath)
        if (!file.exists()) {
            throw IOException("API key file not found: $filePath")
        }
        if (!file.canRead()) {
            throw IOException("Cannot read API key file: $filePath")
        }

        val key = file.readText().trim()
        if (key.isEmpty()) {
            throw IOException("API key file is empty: $filePath")
        }

        lastReadTime = System.currentTimeMillis()
        return key
    }

    companion object {
        private val logger = LoggerFactory.getLogger(FileApiKeyAuth::class.java)

        /**
         * Create a FileApiKeyAuth from environment variable detection.
         *
         * Checks for the file path in `{envName}_FILE` environment variable.
         * For example, if envName is "VAULT_API_KEY", it checks for
         * "VAULT_API_KEY_FILE" to get the file path.
         *
         * Falls back to reading the direct value from `envName` if no file
         * is configured (returns a regular ApiKeyAuth in that case).
         *
         * @param envName Base environment variable name
         * @return AuthProvider configured from environment
         * @throws IllegalStateException if neither file nor value is configured
         */
        @JvmStatic
        fun fromEnv(envName: String): AuthProvider {
            val filePath = System.getenv("${envName}_FILE")
            val directValue = System.getenv(envName)

            return when {
                filePath != null -> {
                    logger.debug("Using file-based API key from {}_FILE: {}", envName, filePath)
                    FileApiKeyAuth(filePath)
                }
                directValue != null -> {
                    logger.debug("Using direct API key from {}", envName)
                    ApiKeyAuth(directValue)
                }
                else -> {
                    throw IllegalStateException(
                        "No API key configured. Set either ${envName}_FILE (recommended) or $envName environment variable."
                    )
                }
            }
        }

        /**
         * Create from a file path.
         */
        @JvmStatic
        fun fromFile(filePath: String): FileApiKeyAuth = FileApiKeyAuth(filePath)
    }
}

/**
 * Auth provider that supports both JWT and API key authentication.
 */
class CompositeAuthProvider(
    private val tokenManager: TokenManager? = null,
    private val apiKeyAuth: ApiKeyAuth? = null
) : AuthProvider {

    override fun getAuthHeader(): String? {
        // Prefer JWT if available and valid
        if (tokenManager?.isAuthenticated() == true) {
            return tokenManager.getAuthHeader()
        }

        // API key doesn't use Authorization header
        return null
    }

    /**
     * Get API key if configured.
     */
    override fun getApiKey(): String? = apiKeyAuth?.getApiKey()

    /**
     * Check if using API key authentication.
     */
    fun isApiKeyAuth(): Boolean = apiKeyAuth != null && tokenManager?.isAuthenticated() != true
}
