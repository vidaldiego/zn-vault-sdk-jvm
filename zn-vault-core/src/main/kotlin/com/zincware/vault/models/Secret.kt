// Path: zn-vault-sdk-jvm/zn-vault-core/src/main/kotlin/com/zincware/vault/models/Secret.kt
package com.zincware.vault.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

/**
 * Represents a secret stored in ZN-Vault.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Secret(
    val id: String,
    val alias: String,
    val tenant: String,
    val type: SecretType,
    val version: Int,
    val tags: List<String>? = null,
    @JsonProperty("created_at") val createdAt: Instant? = null,
    @JsonProperty("updated_at") val updatedAt: Instant? = null,
    @JsonProperty("ttl_until") val ttlUntil: Instant? = null,
    @JsonProperty("content_type") val contentType: String? = null,
    @JsonProperty("created_by") val createdBy: String? = null,
    val checksum: String? = null
)

/**
 * Types of secrets supported by ZN-Vault.
 */
enum class SecretType {
    @JsonProperty("opaque") OPAQUE,
    @JsonProperty("credential") CREDENTIAL,
    @JsonProperty("setting") SETTING
}

/**
 * Decrypted secret data.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SecretData(
    val data: Map<String, Any>,
    @JsonProperty("decrypted_at") val decryptedAt: Instant = Instant.now()
)

/**
 * Request to create a new secret.
 */
data class CreateSecretRequest(
    val alias: String,
    val type: SecretType,
    val data: Map<String, Any>,
    val tags: List<String> = emptyList(),
    @JsonProperty("ttl_until") val ttlUntil: Instant? = null
)

/**
 * Request to update an existing secret.
 */
data class UpdateSecretRequest(
    val data: Map<String, Any>
)

/**
 * Request to update secret metadata (tags only).
 */
data class UpdateSecretMetadataRequest(
    val tags: List<String>
)

/**
 * Filter for listing secrets.
 */
data class SecretFilter(
    val type: SecretType? = null,
    val tags: List<String>? = null,
    val limit: Int = 50,
    val offset: Int = 0,
    val marker: String? = null
)

/**
 * Historical version of a secret.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SecretVersion(
    val id: Int,
    val tenant: String? = null,
    val alias: String? = null,
    val type: String? = null,
    val version: Int,
    val tags: List<String>? = null,
    @JsonProperty("created_at") val createdAt: Instant? = null,
    @JsonProperty("created_by") val createdBy: String? = null,
    val checksum: String? = null
)

/**
 * Response from secret history endpoint.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SecretHistoryResponse(
    val history: List<SecretVersion> = emptyList(),
    val count: Int = 0
)

/**
 * File metadata for opaque secrets containing files.
 */
data class FileMetadata(
    val filename: String,
    val contentType: String,
    val checksum: String? = null,
    val size: Long? = null,
    val certificateExpiry: Instant? = null
)
