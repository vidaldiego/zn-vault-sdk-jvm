// Path: zn-vault-sdk-jvm/zn-vault-core/src/main/kotlin/com/zincapp/vault/auth/AuthClient.kt
package com.zincapp.vault.auth

import com.fasterxml.jackson.core.type.TypeReference
import com.zincapp.vault.http.ZnVaultHttpClient
import com.zincapp.vault.models.*

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

    // ==================== Managed API Keys ====================

    /**
     * Create a managed API key with auto-rotation configuration.
     *
     * Managed keys automatically rotate based on the configured mode:
     * - SCHEDULED: Rotates at fixed intervals (requires rotationInterval)
     * - ON_USE: Rotates after being used (TTL resets on each use)
     * - ON_BIND: Rotates each time bind is called
     *
     * @param name Unique name for the managed key
     * @param permissions List of permissions for the key
     * @param rotationMode Rotation mode
     * @param rotationInterval Interval for scheduled rotation (e.g., "24h", "7d")
     * @param gracePeriod Grace period for smooth transitions (e.g., "5m")
     * @param description Optional description
     * @param expiresInDays Optional expiration in days
     * @param tenantId Required for superadmin creating tenant-scoped keys
     * @return The created managed key metadata (use bind to get the key value)
     */
    fun createManagedApiKey(
        name: String,
        permissions: List<String>,
        rotationMode: RotationMode,
        rotationInterval: String? = null,
        gracePeriod: String? = null,
        description: String? = null,
        expiresInDays: Int? = null,
        tenantId: String? = null
    ): CreateManagedApiKeyResponse {
        val request = CreateManagedApiKeyRequest(
            name = name,
            permissions = permissions,
            rotationMode = rotationMode,
            rotationInterval = rotationInterval,
            gracePeriod = gracePeriod,
            description = description,
            expiresInDays = expiresInDays
        )

        val path = if (tenantId != null) {
            "/auth/api-keys/managed?tenantId=${java.net.URLEncoder.encode(tenantId, "UTF-8")}"
        } else {
            "/auth/api-keys/managed"
        }

        return httpClient.post(path, request, CreateManagedApiKeyResponse::class.java)
    }

    /**
     * List managed API keys.
     *
     * @param tenantId Optional tenant ID filter (for superadmin)
     * @return List of managed keys
     */
    fun listManagedApiKeys(tenantId: String? = null): List<ManagedApiKey> {
        val path = if (tenantId != null) {
            "/auth/api-keys/managed?tenantId=${java.net.URLEncoder.encode(tenantId, "UTF-8")}"
        } else {
            "/auth/api-keys/managed"
        }

        val response = httpClient.get(path, ManagedApiKeyListResponse::class.java)
        return response.keys
    }

    /**
     * Get a managed API key by name.
     *
     * @param name The managed key name
     * @param tenantId Optional tenant ID (for cross-tenant access)
     * @return The managed key metadata
     */
    fun getManagedApiKey(name: String, tenantId: String? = null): ManagedApiKey {
        val encodedName = java.net.URLEncoder.encode(name, "UTF-8")
        val path = if (tenantId != null) {
            "/auth/api-keys/managed/$encodedName?tenantId=${java.net.URLEncoder.encode(tenantId, "UTF-8")}"
        } else {
            "/auth/api-keys/managed/$encodedName"
        }

        return httpClient.get(path, ManagedApiKey::class.java)
    }

    /**
     * Bind to a managed API key to get the current key value.
     *
     * This is the primary method for agents to obtain their API key.
     * The response includes rotation metadata to help determine when
     * to re-bind for a new key.
     *
     * Security: This endpoint requires the caller to already have a valid
     * API key (the current one, even during grace period). This prevents
     * unauthorized access to managed keys.
     *
     * @param name The managed key name
     * @param tenantId Optional tenant ID (for cross-tenant access)
     * @return The current key value and rotation metadata
     */
    fun bindManagedApiKey(name: String, tenantId: String? = null): ManagedKeyBindResponse {
        val encodedName = java.net.URLEncoder.encode(name, "UTF-8")
        val path = if (tenantId != null) {
            "/auth/api-keys/managed/$encodedName/bind?tenantId=${java.net.URLEncoder.encode(tenantId, "UTF-8")}"
        } else {
            "/auth/api-keys/managed/$encodedName/bind"
        }

        return httpClient.postEmpty(path, ManagedKeyBindResponse::class.java)
    }

    /**
     * Force rotate a managed API key.
     *
     * Creates a new key immediately, regardless of the rotation schedule.
     * The old key remains valid during the grace period.
     *
     * @param name The managed key name
     * @param tenantId Optional tenant ID (for cross-tenant access)
     * @return The new key value and rotation info
     */
    fun rotateManagedApiKey(name: String, tenantId: String? = null): ManagedKeyRotateResponse {
        val encodedName = java.net.URLEncoder.encode(name, "UTF-8")
        val path = if (tenantId != null) {
            "/auth/api-keys/managed/$encodedName/rotate?tenantId=${java.net.URLEncoder.encode(tenantId, "UTF-8")}"
        } else {
            "/auth/api-keys/managed/$encodedName/rotate"
        }

        return httpClient.postEmpty(path, ManagedKeyRotateResponse::class.java)
    }

    /**
     * Update managed API key configuration.
     *
     * @param name The managed key name
     * @param rotationInterval New rotation interval
     * @param gracePeriod New grace period
     * @param enabled Enable/disable the key
     * @param tenantId Optional tenant ID (for cross-tenant access)
     * @return Updated managed key metadata
     */
    fun updateManagedApiKeyConfig(
        name: String,
        rotationInterval: String? = null,
        gracePeriod: String? = null,
        enabled: Boolean? = null,
        tenantId: String? = null
    ): ManagedApiKey {
        val encodedName = java.net.URLEncoder.encode(name, "UTF-8")
        val path = if (tenantId != null) {
            "/auth/api-keys/managed/$encodedName/config?tenantId=${java.net.URLEncoder.encode(tenantId, "UTF-8")}"
        } else {
            "/auth/api-keys/managed/$encodedName/config"
        }

        val request = UpdateManagedApiKeyConfigRequest(
            rotationInterval = rotationInterval,
            gracePeriod = gracePeriod,
            enabled = enabled
        )

        return httpClient.patch(path, request, ManagedApiKey::class.java)
    }

    /**
     * Delete a managed API key.
     *
     * @param name The managed key name
     * @param tenantId Optional tenant ID (for cross-tenant access)
     */
    fun deleteManagedApiKey(name: String, tenantId: String? = null) {
        val encodedName = java.net.URLEncoder.encode(name, "UTF-8")
        val path = if (tenantId != null) {
            "/auth/api-keys/managed/$encodedName?tenantId=${java.net.URLEncoder.encode(tenantId, "UTF-8")}"
        } else {
            "/auth/api-keys/managed/$encodedName"
        }

        httpClient.delete(path)
    }

    // ==================== Registration Tokens (Agent Bootstrap) ====================

    /**
     * Create a registration token for agent bootstrapping.
     *
     * Registration tokens are one-time use tokens that allow agents to
     * obtain their managed API key without prior authentication.
     *
     * @param managedKeyName The managed key to create a token for
     * @param expiresIn Token expiration (e.g., "1h", "24h"). Min 1m, max 24h.
     * @param description Optional description for audit trail
     * @param tenantId Optional tenant ID (for cross-tenant access)
     * @return The created token (shown only once - save it immediately!)
     */
    fun createRegistrationToken(
        managedKeyName: String,
        expiresIn: String? = null,
        description: String? = null,
        tenantId: String? = null
    ): CreateRegistrationTokenResponse {
        val encodedName = java.net.URLEncoder.encode(managedKeyName, "UTF-8")
        val path = if (tenantId != null) {
            "/auth/api-keys/managed/$encodedName/registration-tokens?tenantId=${java.net.URLEncoder.encode(tenantId, "UTF-8")}"
        } else {
            "/auth/api-keys/managed/$encodedName/registration-tokens"
        }

        val request = CreateRegistrationTokenRequest(
            expiresIn = expiresIn,
            description = description
        )

        return httpClient.post(path, request, CreateRegistrationTokenResponse::class.java)
    }

    /**
     * List registration tokens for a managed key.
     *
     * @param managedKeyName The managed key name
     * @param includeUsed Include tokens that have been used
     * @param tenantId Optional tenant ID (for cross-tenant access)
     * @return List of registration tokens
     */
    fun listRegistrationTokens(
        managedKeyName: String,
        includeUsed: Boolean = false,
        tenantId: String? = null
    ): List<RegistrationToken> {
        val encodedName = java.net.URLEncoder.encode(managedKeyName, "UTF-8")
        val queryParams = mutableListOf<String>()
        if (includeUsed) queryParams.add("includeUsed=true")
        if (tenantId != null) queryParams.add("tenantId=${java.net.URLEncoder.encode(tenantId, "UTF-8")}")

        val path = if (queryParams.isNotEmpty()) {
            "/auth/api-keys/managed/$encodedName/registration-tokens?${queryParams.joinToString("&")}"
        } else {
            "/auth/api-keys/managed/$encodedName/registration-tokens"
        }

        val response = httpClient.get(path, RegistrationTokenListResponse::class.java)
        return response.tokens
    }

    /**
     * Revoke a registration token.
     *
     * Prevents the token from being used for bootstrapping.
     *
     * @param managedKeyName The managed key name
     * @param tokenId The token ID to revoke
     * @param tenantId Optional tenant ID (for cross-tenant access)
     */
    fun revokeRegistrationToken(
        managedKeyName: String,
        tokenId: String,
        tenantId: String? = null
    ) {
        val encodedName = java.net.URLEncoder.encode(managedKeyName, "UTF-8")
        val path = if (tenantId != null) {
            "/auth/api-keys/managed/$encodedName/registration-tokens/$tokenId?tenantId=${java.net.URLEncoder.encode(tenantId, "UTF-8")}"
        } else {
            "/auth/api-keys/managed/$encodedName/registration-tokens/$tokenId"
        }

        httpClient.delete(path)
    }

    /**
     * Bootstrap an agent using a registration token.
     *
     * This is the unauthenticated endpoint used by agents to exchange a
     * one-time registration token for a managed API key binding.
     *
     * Note: This method does not require prior authentication.
     *
     * @param token The registration token (format: zrt_...)
     * @return The API key binding response
     */
    fun bootstrap(token: String): BootstrapResponse {
        val request = BootstrapRequest(token)
        return httpClient.post("/agent/bootstrap", request, BootstrapResponse::class.java)
    }
}
