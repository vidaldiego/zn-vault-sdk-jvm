// Path: zn-vault-sdk-jvm/zn-vault-core/src/main/kotlin/com/zincware/vault/models/Auth.kt
package com.zincware.vault.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

/**
 * Login request.
 */
data class LoginRequest(
    val username: String,
    val password: String,
    @JsonProperty("totp_code") val totpCode: String? = null
)

/**
 * Login response with JWT tokens.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    @JsonProperty("requires_2fa") val requires2fa: Boolean = false,
    val user: User? = null
)

/**
 * Token refresh request.
 */
data class RefreshTokenRequest(
    @JsonProperty("refresh_token") val refreshToken: String
)

/**
 * Token refresh response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class RefreshTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val user: User? = null
)

/**
 * User registration request.
 */
data class RegisterRequest(
    val username: String,
    val password: String,
    val email: String? = null
)

/**
 * Change password request.
 */
data class ChangePasswordRequest(
    @JsonProperty("current_password") val currentPassword: String,
    @JsonProperty("new_password") val newPassword: String
)

/**
 * Force change password request (when password_must_change is true).
 */
data class ForceChangePasswordRequest(
    val username: String,
    @JsonProperty("current_password") val currentPassword: String,
    @JsonProperty("new_password") val newPassword: String
)

/**
 * Time range condition for API key access.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class ApiKeyTimeRange(
    val start: String? = null,
    val end: String? = null,
    val timezone: String? = null
)

/**
 * Resource-specific conditions for API keys.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class ApiKeyResourceConditions(
    val certificates: List<String>? = null,
    val secrets: List<String>? = null
)

/**
 * Inline ABAC conditions for API keys.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class ApiKeyConditions(
    val ip: List<String>? = null,
    val timeRange: ApiKeyTimeRange? = null,
    val methods: List<String>? = null,
    val resources: ApiKeyResourceConditions? = null,
    val aliases: List<String>? = null,
    val resourceTags: Map<String, String>? = null
)

/**
 * API key information.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class ApiKey(
    val id: String,
    val name: String,
    val prefix: String? = null,
    @JsonProperty("user_id") val userId: String? = null,
    @JsonProperty("tenant_id") val tenantId: String? = null,
    @JsonProperty("created_at") val createdAt: Instant? = null,
    @JsonProperty("expires_at") val expiresAt: Instant? = null,
    @JsonProperty("last_used") val lastUsed: Instant? = null,
    val scope: String? = null,
    val permissions: List<String>? = null,
    @JsonProperty("ip_allowlist") val ipAllowlist: List<String>? = null,
    val conditions: ApiKeyConditions? = null
)

/**
 * API key scope.
 */
enum class ApiKeyScope {
    @JsonProperty("full") FULL,
    @JsonProperty("read_only") READ_ONLY,
    @JsonProperty("limited") LIMITED
}

/**
 * Request to create an API key.
 *
 * @property name Name for the API key
 * @property permissions Required list of permissions for the key
 * @property expiresInDays Optional expiration in days (default: 90)
 * @property description Optional description
 * @property ipAllowlist Optional list of allowed IPs/CIDRs
 * @property conditions Optional inline ABAC conditions
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreateApiKeyRequest(
    val name: String,
    val permissions: List<String>,
    @JsonProperty("expiresInDays") val expiresInDays: Int? = null,
    val description: String? = null,
    @JsonProperty("ip_allowlist") val ipAllowlist: List<String>? = null,
    val conditions: ApiKeyConditions? = null
)

/**
 * Response when creating an API key (includes the key value, shown only once).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class CreateApiKeyResponse(
    val key: String,
    val apiKey: ApiKey,
    val message: String? = null
)

/**
 * API key rotation response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class RotateApiKeyResponse(
    val key: String,
    val apiKey: ApiKey,
    val message: String? = null
)

/**
 * API key list response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class ApiKeyListResponse(
    val keys: List<ApiKey> = emptyList(),
    val expiringSoon: List<ApiKey> = emptyList()
)

/**
 * 2FA enable response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Enable2faResponse(
    val secret: String,
    @JsonProperty("qr_code") val qrCode: String,
    @JsonProperty("backup_codes") val backupCodes: List<String>
)

/**
 * 2FA verify request.
 */
data class Verify2faRequest(
    val code: String
)

/**
 * 2FA disable request.
 */
data class Disable2faRequest(
    val password: String,
    @JsonProperty("totp_code") val totpCode: String? = null
)

/**
 * 2FA status response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class TwoFactorStatus(
    val enabled: Boolean,
    @JsonProperty("backup_codes_remaining") val backupCodesRemaining: Int? = null
)

/**
 * Current user info response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class CurrentUser(
    val id: String,
    val username: String,
    val email: String? = null,
    val role: UserRole? = null,
    @JsonProperty("tenant_id") val tenantId: String? = null,
    @JsonProperty("totp_enabled") val totpEnabled: Boolean = false,
    val permissions: List<String> = emptyList()
)

/**
 * Update profile request.
 */
data class UpdateProfileRequest(
    val email: String? = null
)

/**
 * Response from /auth/me endpoint.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class MeResponse(
    val user: User,
    val authMethod: String? = null
)
