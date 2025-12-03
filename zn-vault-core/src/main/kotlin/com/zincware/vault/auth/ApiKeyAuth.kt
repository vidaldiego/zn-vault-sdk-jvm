// Path: zn-vault-sdk-jvm/zn-vault-core/src/main/kotlin/com/zincware/vault/auth/ApiKeyAuth.kt
package com.zincware.vault.auth

import com.zincware.vault.http.AuthProvider

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
