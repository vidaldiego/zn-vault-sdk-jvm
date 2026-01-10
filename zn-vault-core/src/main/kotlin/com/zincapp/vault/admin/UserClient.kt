// Path: zn-vault-sdk-jvm/zn-vault-core/src/main/kotlin/com/zincapp/vault/admin/UserClient.kt
package com.zincapp.vault.admin

import com.fasterxml.jackson.core.type.TypeReference
import com.zincapp.vault.http.ZnVaultHttpClient
import com.zincapp.vault.models.*

/**
 * Client for user management operations (admin operations).
 */
class UserClient internal constructor(
    private val httpClient: ZnVaultHttpClient
) {

    /**
     * Create a new user.
     *
     * @param request Create user request
     * @return Created user
     */
    fun create(request: CreateUserRequest): User {
        return httpClient.post("/v1/users", request, User::class.java)
    }

    /**
     * Create a new user with simplified parameters.
     *
     * @param username Username
     * @param password Password
     * @param email Optional email
     * @param tenantId Optional tenant ID
     * @return Created user
     */
    fun create(
        username: String,
        password: String,
        email: String? = null,
        tenantId: String? = null
    ): User {
        return create(
            CreateUserRequest(
                username = username,
                password = password,
                email = email,
                tenantId = tenantId
            )
        )
    }

    /**
     * Get user by ID.
     *
     * @param id User ID
     * @return User details
     */
    fun get(id: String): User {
        return httpClient.get("/v1/users/$id", User::class.java)
    }

    /**
     * List users with optional filtering.
     *
     * @param filter Filter criteria
     * @return Page of users
     */
    fun list(filter: UserFilter = UserFilter()): Page<User> {
        val params = buildFilterParams(filter)
        return httpClient.get("/v1/users$params",
            object : TypeReference<Page<User>>() {})
    }

    /**
     * Update a user.
     *
     * @param id User ID
     * @param request Update request
     * @return Updated user
     */
    fun update(id: String, request: UpdateUserRequest): User {
        return httpClient.put("/v1/users/$id", request, User::class.java)
    }

    /**
     * Reset a user's password (admin operation).
     *
     * @param id User ID
     * @param newPassword New password
     * @return Updated user
     */
    fun resetPassword(id: String, newPassword: String): User {
        val request = ResetPasswordRequest(newPassword)
        return httpClient.post("/v1/users/$id/reset-password", request, User::class.java)
    }

    /**
     * Delete a user.
     *
     * @param id User ID
     */
    fun delete(id: String) {
        httpClient.delete("/v1/users/$id")
    }

    /**
     * Disable a user.
     *
     * @param id User ID
     * @return Updated user
     */
    fun disable(id: String): User {
        return update(id, UpdateUserRequest(status = UserStatus.DISABLED))
    }

    /**
     * Enable a user.
     *
     * @param id User ID
     * @return Updated user
     */
    fun enable(id: String): User {
        return update(id, UpdateUserRequest(status = UserStatus.ACTIVE))
    }

    // ==================== User TOTP Management ====================

    /**
     * Setup TOTP for a user (admin operation).
     *
     * @param id User ID
     * @return TOTP setup result with secret and QR code
     */
    fun setupTotp(id: String): TotpSetupResult {
        return httpClient.postEmpty("/v1/users/$id/totp/setup", TotpSetupResult::class.java)
    }

    /**
     * Enable TOTP for a user after verification.
     *
     * @param id User ID
     * @param code TOTP code to verify
     */
    fun enableTotp(id: String, code: String) {
        val request = TotpVerifyRequest(code)
        httpClient.post("/v1/users/$id/totp/enable", request, SuccessResponse::class.java)
    }

    /**
     * Reset TOTP for a user (generates new secret).
     *
     * @param id User ID
     * @return New TOTP setup result
     */
    fun resetTotp(id: String): TotpSetupResult {
        return httpClient.postEmpty("/v1/users/$id/totp/reset", TotpSetupResult::class.java)
    }

    /**
     * Disable TOTP for a user.
     *
     * @param id User ID
     */
    fun disableTotp(id: String) {
        httpClient.postEmpty("/v1/users/$id/totp/disable", SuccessResponse::class.java)
    }

    private fun buildFilterParams(filter: UserFilter): String {
        val params = mutableListOf<String>()

        filter.tenant?.let { params.add("tenant=$it") }
        filter.role?.let { params.add("role=${it.name.lowercase()}") }
        filter.status?.let { params.add("status=${it.name.lowercase()}") }
        params.add("limit=${filter.limit}")
        params.add("offset=${filter.offset}")

        return if (params.isNotEmpty()) "?${params.joinToString("&")}" else ""
    }
}
