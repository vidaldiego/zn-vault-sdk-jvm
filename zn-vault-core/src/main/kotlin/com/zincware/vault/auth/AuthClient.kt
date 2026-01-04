// Path: zn-vault-sdk-jvm/zn-vault-core/src/main/kotlin/com/zincware/vault/auth/AuthClient.kt
package com.zincware.vault.auth

import com.fasterxml.jackson.core.type.TypeReference
import com.zincware.vault.http.ZnVaultHttpClient
import com.zincware.vault.models.*

/**
 * Client for authentication and user management operations.
 */
class AuthClient internal constructor(
    private val httpClient: ZnVaultHttpClient
) {

    /**
     * Register a new user.
     *
     * Note: The first registered user becomes a superadmin.
     *
     * @param username Username
     * @param password Password
     * @param email Optional email
     * @return Created user
     */
    fun register(username: String, password: String, email: String? = null): User {
        val request = RegisterRequest(username, password, email)
        val response = httpClient.post("/auth/register", request,
            object : TypeReference<ApiResponse<User>>() {})
        return response.data ?: throw IllegalStateException("No user data in response")
    }

    /**
     * Get current authenticated user info.
     *
     * @return Current user info
     */
    fun me(): User {
        val response = httpClient.get("/auth/me", MeResponse::class.java)
        return response.user
    }

    /**
     * Update current user's profile.
     *
     * @param email New email address
     * @return Updated user info
     */
    fun updateProfile(email: String): User {
        val request = UpdateProfileRequest(email)
        val response = httpClient.put("/auth/me", request, MeResponse::class.java)
        return response.user
    }

    /**
     * Change current user's password.
     *
     * @param currentPassword Current password
     * @param newPassword New password
     */
    fun changePassword(currentPassword: String, newPassword: String) {
        val request = ChangePasswordRequest(currentPassword, newPassword)
        httpClient.post("/auth/change-password", request, SuccessResponse::class.java)
    }

    /**
     * Force change password when password_must_change flag is set.
     *
     * @param username Username
     * @param currentPassword Current password
     * @param newPassword New password
     * @return Login response with new tokens
     */
    fun forceChangePassword(
        username: String,
        currentPassword: String,
        newPassword: String
    ): LoginResponse {
        val request = ForceChangePasswordRequest(username, currentPassword, newPassword)
        return httpClient.post("/auth/force-change-password", request, LoginResponse::class.java)
    }

    // ==================== API Keys ====================

    /**
     * Create a new API key.
     *
     * @param name Name for the API key
     * @param permissions Required list of permissions for the key (e.g., ["secret:read:*"])
     * @param expiresInDays Optional expiration in days (default: 90)
     * @param description Optional description
     * @param ipAllowlist Optional list of allowed IPs/CIDRs
     * @param conditions Optional inline ABAC conditions
     * @param tenantId Required for superadmin creating tenant-scoped keys
     * @return Created API key (key value is only shown once!)
     */
    fun createApiKey(
        name: String,
        permissions: List<String>,
        expiresInDays: Int? = null,
        description: String? = null,
        ipAllowlist: List<String>? = null,
        conditions: ApiKeyConditions? = null,
        tenantId: String? = null
    ): CreateApiKeyResponse {
        val request = CreateApiKeyRequest(
            name = name,
            permissions = permissions,
            expiresInDays = expiresInDays,
            description = description,
            ipAllowlist = ipAllowlist,
            conditions = conditions
        )

        // Tenant ID is passed as query parameter
        val path = if (tenantId != null) {
            "/auth/api-keys?tenantId=${java.net.URLEncoder.encode(tenantId, "UTF-8")}"
        } else {
            "/auth/api-keys"
        }

        return httpClient.post(path, request, CreateApiKeyResponse::class.java)
    }

    /**
     * List API keys.
     *
     * @return List of API keys (without the actual key values)
     */
    fun listApiKeys(): List<ApiKey> {
        val response = httpClient.get("/auth/api-keys", ApiKeyListResponse::class.java)
        return response.keys
    }

    /**
     * Delete an API key.
     *
     * @param id API key ID
     */
    fun deleteApiKey(id: String) {
        httpClient.delete("/auth/api-keys/$id")
    }

    /**
     * Rotate an API key by ID.
     *
     * Note: The rotated key will have a new ID (the old key is deleted).
     *
     * @param id API key ID
     * @return New API key (key value is only shown once!)
     */
    fun rotateApiKey(id: String): RotateApiKeyResponse {
        return httpClient.post("/auth/api-keys/$id/rotate", emptyMap<String, Any>(), RotateApiKeyResponse::class.java)
    }

    /**
     * Get info about the current API key (when authenticated via API key).
     *
     * @return Current API key info
     */
    fun getCurrentApiKey(): ApiKey {
        return httpClient.get("/auth/api-keys/self", ApiKey::class.java)
    }

    /**
     * Rotate the current API key (self-rotation).
     *
     * @return New API key
     */
    fun rotateCurrentApiKey(): RotateApiKeyResponse {
        return httpClient.postEmpty("/auth/api-keys/self/rotate", RotateApiKeyResponse::class.java)
    }

    // ==================== Two-Factor Authentication ====================

    /**
     * Enable two-factor authentication.
     *
     * @return TOTP secret, QR code, and backup codes
     */
    fun enable2fa(): Enable2faResponse {
        return httpClient.postEmpty("/auth/2fa/enable", Enable2faResponse::class.java)
    }

    /**
     * Verify TOTP code to complete 2FA setup.
     *
     * @param code TOTP code from authenticator app
     */
    fun verify2fa(code: String) {
        val request = Verify2faRequest(code)
        httpClient.post("/auth/2fa/verify", request, SuccessResponse::class.java)
    }

    /**
     * Disable two-factor authentication.
     *
     * @param password Current password
     * @param totpCode Optional TOTP code (required if 2FA is currently enabled)
     */
    fun disable2fa(password: String, totpCode: String? = null) {
        val request = Disable2faRequest(password, totpCode)
        httpClient.post("/auth/2fa/disable", request, SuccessResponse::class.java)
    }

    /**
     * Get 2FA status for current user.
     *
     * @return 2FA status
     */
    fun get2faStatus(): TwoFactorStatus {
        return httpClient.get("/auth/2fa/status", TwoFactorStatus::class.java)
    }
}
