// Path: zn-vault-sdk-jvm/zn-vault-core/src/main/kotlin/com/zincware/vault/models/Tenant.kt
package com.zincware.vault.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

/**
 * Represents a tenant in ZN-Vault.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Tenant(
    val id: String,
    val name: String,
    val status: TenantStatus = TenantStatus.ACTIVE,
    @JsonProperty("max_secrets") val maxSecrets: Int? = null,
    @JsonProperty("max_kms_keys") val maxKmsKeys: Int? = null,
    @JsonProperty("max_storage_mb") val maxStorageMb: Int? = null,
    @JsonProperty("plan_tier") val planTier: String? = null,
    @JsonProperty("audit_log_visible") val auditLogVisible: Boolean = true,
    @JsonProperty("audit_log_retention_days") val auditLogRetentionDays: Int? = null,
    @JsonProperty("contact_email") val contactEmail: String? = null,
    @JsonProperty("contact_name") val contactName: String? = null,
    val metadata: String? = null,  // Stored as JSON string in database
    @JsonProperty("created_at") val createdAt: Instant? = null,
    @JsonProperty("created_by") val createdBy: String? = null,
    @JsonProperty("updated_at") val updatedAt: Instant? = null,
    @JsonProperty("last_activity") val lastActivity: Instant? = null
)

/**
 * Tenant status.
 */
enum class TenantStatus {
    @JsonProperty("active") ACTIVE,
    @JsonProperty("suspended") SUSPENDED,
    @JsonProperty("archived") ARCHIVED
}

/**
 * Tenant resource quotas.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class TenantQuotas(
    @JsonProperty("max_secrets") val maxSecrets: Int? = null,
    @JsonProperty("max_kms_keys") val maxKmsKeys: Int? = null,
    @JsonProperty("max_users") val maxUsers: Int? = null,
    @JsonProperty("max_api_keys") val maxApiKeys: Int? = null
)

/**
 * Tenant resource usage.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class TenantUsage(
    @JsonProperty("secrets_count") val secretsCount: Int = 0,
    @JsonProperty("kms_keys_count") val kmsKeysCount: Int = 0,
    @JsonProperty("users_count") val usersCount: Int = 0,
    @JsonProperty("api_keys_count") val apiKeysCount: Int = 0,
    @JsonProperty("storage_bytes") val storageBytes: Long = 0
)

/**
 * Request to create a new tenant.
 */
data class CreateTenantRequest(
    val id: String,
    val name: String,
    @JsonProperty("max_secrets") val maxSecrets: Int? = null,
    @JsonProperty("max_kms_keys") val maxKmsKeys: Int? = null,
    @JsonProperty("max_storage_mb") val maxStorageMb: Int? = null,
    @JsonProperty("plan_tier") val planTier: String? = null,
    @JsonProperty("contact_email") val contactEmail: String? = null,
    @JsonProperty("contact_name") val contactName: String? = null
)

/**
 * Request to update an existing tenant.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class UpdateTenantRequest(
    val name: String? = null,
    @JsonProperty("max_secrets") val maxSecrets: Int? = null,
    @JsonProperty("max_kms_keys") val maxKmsKeys: Int? = null,
    @JsonProperty("max_storage_mb") val maxStorageMb: Int? = null,
    @JsonProperty("contact_email") val contactEmail: String? = null,
    @JsonProperty("contact_name") val contactName: String? = null
)

/**
 * Request to update tenant status.
 */
data class UpdateTenantStatusRequest(
    val status: TenantStatus
)

/**
 * Filter for listing tenants.
 */
data class TenantFilter(
    val status: TenantStatus? = null,
    @JsonProperty("include_usage") val includeUsage: Boolean = false,
    val limit: Int = 50,
    val offset: Int = 0
)

/**
 * Quota check result.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class QuotaCheckResult(
    @JsonProperty("resource_type") val resourceType: String,
    val current: Int,
    val limit: Int?,
    val allowed: Boolean
)
