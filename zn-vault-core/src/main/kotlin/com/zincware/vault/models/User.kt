// Path: zn-vault-sdk-jvm/zn-vault-core/src/main/kotlin/com/zincware/vault/models/User.kt
package com.zincware.vault.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

/**
 * Represents a user in ZN-Vault.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class User(
    val id: String,
    val username: String,
    val email: String? = null,
    val role: UserRole? = null,
    @JsonProperty("tenant_id") val tenantId: String? = null,
    val status: UserStatus? = null,
    @JsonProperty("totp_enabled") val totpEnabled: Boolean = false,
    val roles: List<Role> = emptyList(),
    val permissions: List<String> = emptyList(),
    @JsonProperty("created_at") val createdAt: Instant? = null,
    @JsonProperty("last_login") val lastLogin: Instant? = null,
    @JsonProperty("password_must_change") val passwordMustChange: Boolean = false
)

/**
 * User role type (legacy role field).
 */
enum class UserRole {
    @JsonProperty("user") USER,
    @JsonProperty("admin") ADMIN,
    @JsonProperty("superadmin") SUPERADMIN
}

/**
 * User account status.
 */
enum class UserStatus {
    @JsonProperty("active") ACTIVE,
    @JsonProperty("disabled") DISABLED,
    @JsonProperty("locked") LOCKED
}

/**
 * Request to create a new user.
 * Note: API expects camelCase for request fields.
 */
data class CreateUserRequest(
    val username: String,
    val password: String,
    val email: String? = null,
    val role: UserRole = UserRole.USER,
    val tenantId: String? = null,
    val roles: List<String> = emptyList()
)

/**
 * Request to update an existing user.
 * Note: API expects camelCase for request fields, null fields excluded.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class UpdateUserRequest(
    val email: String? = null,
    val role: UserRole? = null,
    val tenantId: String? = null,
    val status: UserStatus? = null,
    val roles: List<String>? = null
)

/**
 * Request to reset user password (admin operation).
 */
data class ResetPasswordRequest(
    val password: String
)

/**
 * Filter for listing users.
 */
data class UserFilter(
    val tenant: String? = null,
    val role: UserRole? = null,
    val status: UserStatus? = null,
    val limit: Int = 50,
    val offset: Int = 0
)

/**
 * TOTP setup result.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class TotpSetupResult(
    val secret: String,
    @JsonProperty("qr_code") val qrCode: String,
    @JsonProperty("backup_codes") val backupCodes: List<String>
)

/**
 * Request to verify TOTP code.
 */
data class TotpVerifyRequest(
    val code: String
)
