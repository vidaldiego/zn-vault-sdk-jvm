// Path: zn-vault-sdk-jvm/zn-vault-core/src/main/kotlin/com/zincware/vault/auth/TokenManager.kt
package com.zincware.vault.auth

import com.zincware.vault.exception.AuthenticationException
import com.zincware.vault.http.AuthProvider
import com.zincware.vault.http.ZnVaultHttpClient
import com.zincware.vault.models.*
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Manages JWT tokens for authentication.
 *
 * Handles login, token refresh, and automatic token renewal.
 */
class TokenManager(
    private val httpClient: ZnVaultHttpClient,
    private val config: TokenManagerConfig = TokenManagerConfig()
) : AuthProvider {

    private val logger = LoggerFactory.getLogger(TokenManager::class.java)
    private val lock = ReentrantLock()

    @Volatile
    private var accessToken: String? = null

    @Volatile
    private var refreshToken: String? = null

    @Volatile
    private var expiresAt: Instant? = null

    @Volatile
    private var currentUser: CurrentUser? = null

    /**
     * Get the current user info.
     */
    fun getCurrentUser(): CurrentUser? = currentUser

    /**
     * Check if the user is authenticated.
     */
    fun isAuthenticated(): Boolean = accessToken != null && !isTokenExpired()

    /**
     * Login with username and password.
     *
     * The username must include the tenant prefix in the format `tenant/username`
     * (e.g., "acme/admin"). This allows multiple tenants to have users with the
     * same username. Email addresses can also be used as username.
     *
     * @param username The username in format "tenant/username" or email
     * @param password The password
     * @param totpCode Optional TOTP code for 2FA
     * @return Login response with tokens
     * @throws AuthenticationException if login fails
     */
    fun login(username: String, password: String, totpCode: String? = null): LoginResponse {
        val request = LoginRequest(username, password, totpCode)
        val response = httpClient.post("/auth/login", request, LoginResponse::class.java)

        lock.withLock {
            accessToken = response.accessToken
            refreshToken = response.refreshToken
            expiresAt = Instant.now().plusSeconds(response.expiresIn)
            currentUser = response.user?.let {
                CurrentUser(
                    id = it.id,
                    username = it.username,
                    email = it.email,
                    role = it.role,
                    tenantId = it.tenantId,
                    totpEnabled = it.totpEnabled,
                    permissions = it.permissions
                )
            }
        }

        logger.debug("Successfully logged in as {}", username)
        return response
    }

    /**
     * Login with tenant and username as separate parameters.
     *
     * Convenience method that formats the username as "tenant/username".
     *
     * @param tenant Tenant identifier (e.g., "acme")
     * @param username Username within the tenant (e.g., "admin")
     * @param password The password
     * @param totpCode Optional TOTP code for 2FA
     * @return Login response with tokens
     * @throws AuthenticationException if login fails
     */
    fun login(tenant: String, username: String, password: String, totpCode: String? = null): LoginResponse {
        val fullUsername = "$tenant/$username"
        return login(fullUsername, password, totpCode)
    }

    /**
     * Refresh the access token using the refresh token.
     *
     * @return New tokens
     * @throws AuthenticationException if refresh fails
     */
    fun refresh(): RefreshTokenResponse {
        val currentRefreshToken = refreshToken
            ?: throw AuthenticationException("No refresh token available")

        val request = RefreshTokenRequest(currentRefreshToken)
        val response = httpClient.post("/auth/refresh", request, RefreshTokenResponse::class.java)

        lock.withLock {
            accessToken = response.accessToken
            refreshToken = response.refreshToken
            expiresAt = Instant.now().plusSeconds(response.expiresIn)
        }

        logger.debug("Successfully refreshed access token")
        return response
    }

    /**
     * Logout and clear tokens.
     */
    fun logout() {
        lock.withLock {
            accessToken = null
            refreshToken = null
            expiresAt = null
            currentUser = null
        }
        logger.debug("Logged out")
    }

    /**
     * Set tokens directly (useful for restoring session).
     */
    fun setTokens(accessToken: String, refreshToken: String, expiresIn: Long) {
        lock.withLock {
            this.accessToken = accessToken
            this.refreshToken = refreshToken
            this.expiresAt = Instant.now().plusSeconds(expiresIn)
        }
    }

    /**
     * Get a valid access token, refreshing if necessary.
     *
     * @return Valid access token
     * @throws AuthenticationException if no valid token is available
     */
    fun getValidToken(): String {
        lock.withLock {
            val token = accessToken
            if (token == null) {
                throw AuthenticationException("Not authenticated. Please login first.")
            }

            // Check if token needs refresh
            if (shouldRefresh()) {
                try {
                    refresh()
                } catch (e: Exception) {
                    logger.warn("Failed to refresh token: {}", e.message)
                    if (isTokenExpired()) {
                        throw AuthenticationException("Token expired and refresh failed", cause = e)
                    }
                }
            }

            return accessToken ?: throw AuthenticationException("No valid token available")
        }
    }

    /**
     * Get the authorization header value.
     */
    override fun getAuthHeader(): String? {
        return try {
            "Bearer ${getValidToken()}"
        } catch (e: AuthenticationException) {
            null
        }
    }

    private fun isTokenExpired(): Boolean {
        val exp = expiresAt ?: return true
        return Instant.now().isAfter(exp)
    }

    private fun shouldRefresh(): Boolean {
        val exp = expiresAt ?: return true
        val refreshThreshold = exp.minusSeconds(config.refreshBeforeExpiry.seconds)
        return Instant.now().isAfter(refreshThreshold)
    }
}

/**
 * Configuration for TokenManager.
 */
data class TokenManagerConfig(
    /**
     * Refresh token this many seconds before expiry.
     */
    val refreshBeforeExpiry: java.time.Duration = java.time.Duration.ofMinutes(5),

    /**
     * Enable automatic token refresh.
     */
    val autoRefresh: Boolean = true
)
