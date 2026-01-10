// Path: zn-vault-sdk-jvm/zn-vault-core/src/main/kotlin/com/zincapp/vault/models/Secret.kt
package com.zincapp.vault.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

/**
 * Represents a secret stored in ZnVault.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Secret(
    val id: String,
    val alias: String,
    val tenant: String,
    val type: SecretType,
    val version: Int,
    val subType: SecretSubType? = null,
    val tags: List<String>? = null,
    // File metadata (queryable without decryption)
    val fileName: String? = null,
    val fileSize: Long? = null,
    val fileMime: String? = null,
    val fileChecksum: String? = null,
    // Expiration tracking
    val expiresAt: Instant? = null,
    val ttlUntil: Instant? = null,
    // Content type (for settings)
    val contentType: String? = null,
    // Audit
    val createdBy: String? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null
)

/**
 * Types of secrets supported by ZnVault.
 */
enum class SecretType {
    @JsonProperty("opaque") OPAQUE,
    @JsonProperty("credential") CREDENTIAL,
    @JsonProperty("setting") SETTING
}

/**
 * Semantic sub-types for secrets.
 * These provide more granular classification beyond the base type.
 */
enum class SecretSubType {
    // Credential sub-types
    @JsonProperty("password") PASSWORD,
    @JsonProperty("api_key") API_KEY,

    // Opaque sub-types
    @JsonProperty("file") FILE,
    @JsonProperty("certificate") CERTIFICATE,
    @JsonProperty("private_key") PRIVATE_KEY,
    @JsonProperty("keypair") KEYPAIR,
    @JsonProperty("ssh_key") SSH_KEY,
    @JsonProperty("token") TOKEN,
    @JsonProperty("generic") GENERIC,

    // Public key sub-types
    @JsonProperty("ed25519_public_key") ED25519_PUBLIC_KEY,
    @JsonProperty("rsa_public_key") RSA_PUBLIC_KEY,
    @JsonProperty("ecdsa_public_key") ECDSA_PUBLIC_KEY,

    // Setting sub-types
    @JsonProperty("json") JSON,
    @JsonProperty("yaml") YAML,
    @JsonProperty("env") ENV,
    @JsonProperty("properties") PROPERTIES,
    @JsonProperty("toml") TOML
}

/**
 * Maps sub-types to their parent storage types.
 */
val subTypeToType: Map<SecretSubType, SecretType> = mapOf(
    // Credential sub-types
    SecretSubType.PASSWORD to SecretType.CREDENTIAL,
    SecretSubType.API_KEY to SecretType.CREDENTIAL,
    // Opaque sub-types
    SecretSubType.FILE to SecretType.OPAQUE,
    SecretSubType.CERTIFICATE to SecretType.OPAQUE,
    SecretSubType.PRIVATE_KEY to SecretType.OPAQUE,
    SecretSubType.KEYPAIR to SecretType.OPAQUE,
    SecretSubType.SSH_KEY to SecretType.OPAQUE,
    SecretSubType.TOKEN to SecretType.OPAQUE,
    SecretSubType.GENERIC to SecretType.OPAQUE,
    // Public key sub-types
    SecretSubType.ED25519_PUBLIC_KEY to SecretType.OPAQUE,
    SecretSubType.RSA_PUBLIC_KEY to SecretType.OPAQUE,
    SecretSubType.ECDSA_PUBLIC_KEY to SecretType.OPAQUE,
    // Setting sub-types
    SecretSubType.JSON to SecretType.SETTING,
    SecretSubType.YAML to SecretType.SETTING,
    SecretSubType.ENV to SecretType.SETTING,
    SecretSubType.PROPERTIES to SecretType.SETTING,
    SecretSubType.TOML to SecretType.SETTING
)

/**
 * Decrypted secret data.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SecretData(
    val id: String? = null,
    val alias: String? = null,
    val tenant: String? = null,
    val type: SecretType? = null,
    val subType: SecretSubType? = null,
    val version: Int? = null,
    val data: Map<String, Any>,
    // File metadata
    val fileName: String? = null,
    val fileSize: Long? = null,
    val fileMime: String? = null,
    val fileChecksum: String? = null,
    // Expiration tracking
    val expiresAt: Instant? = null,
    val ttlUntil: Instant? = null,
    // Content type
    val contentType: String? = null,
    // Audit
    val createdBy: String? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
    val decryptedAt: Instant = Instant.now()
)

/**
 * Request to create a new secret.
 */
data class CreateSecretRequest(
    val alias: String,
    val type: SecretType,
    val data: Map<String, Any>,
    val subType: SecretSubType? = null,
    val tags: List<String> = emptyList(),
    val fileName: String? = null,
    val expiresAt: Instant? = null,
    val ttlUntil: Instant? = null,
    val contentType: String? = null
)

/**
 * Request to update an existing secret.
 */
data class UpdateSecretRequest(
    val data: Map<String, Any>,
    val subType: SecretSubType? = null,
    val fileName: String? = null,
    val expiresAt: Instant? = null,
    val ttlUntil: Instant? = null,
    val tags: List<String>? = null,
    val contentType: String? = null
)

/**
 * Request to update secret metadata (tags only).
 */
data class UpdateSecretMetadataRequest(
    val tags: List<String>,
    val expiresAt: Instant? = null,
    val ttlUntil: Instant? = null
)

/**
 * Filter for listing secrets.
 */
data class SecretFilter(
    val type: SecretType? = null,
    val subType: SecretSubType? = null,
    val fileMime: String? = null,
    val expiringBefore: Instant? = null,
    val aliasPrefix: String? = null,
    val tags: List<String>? = null,
    val page: Int = 1,
    val pageSize: Int = 100
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
    val subType: SecretSubType? = null,
    val version: Int,
    val tags: List<String>? = null,
    // File metadata
    val fileName: String? = null,
    val fileSize: Long? = null,
    val fileMime: String? = null,
    // Expiration tracking
    val expiresAt: Instant? = null,
    // Audit
    val createdAt: Instant? = null,
    val createdBy: String? = null,
    val supersededAt: Instant? = null,
    val supersededBy: String? = null
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

// ==================== Keypair Generation Models ====================

/**
 * Algorithm for keypair generation.
 */
enum class KeypairAlgorithm {
    @JsonProperty("RSA") RSA,
    @JsonProperty("Ed25519") Ed25519,
    @JsonProperty("ECDSA") ECDSA
}

/**
 * ECDSA curve options.
 */
enum class EcdsaCurve {
    @JsonProperty("P-256") P_256,
    @JsonProperty("P-384") P_384
}

/**
 * Request to generate a keypair.
 */
data class GenerateKeypairRequest(
    val algorithm: KeypairAlgorithm,
    val alias: String,
    val tenant: String,
    val rsaBits: Int? = null,
    val ecdsaCurve: EcdsaCurve? = null,
    val comment: String? = null,
    val publishPublicKey: Boolean? = null,
    val tags: List<String> = emptyList()
)

/**
 * Public key information from keypair generation.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class PublicKeyInfo(
    val id: String,
    val alias: String,
    val tenant: String? = null,
    val subType: SecretSubType? = null,
    val isPublic: Boolean? = null,
    val fingerprint: String? = null,
    val algorithm: String? = null,
    val bits: Int? = null,
    val publicKeyPem: String? = null,
    val publicKeyOpenSSH: String? = null
)

/**
 * Generated keypair response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GeneratedKeypair(
    val privateKey: Secret,
    val publicKey: PublicKeyInfo
)

/**
 * Result of publishing a public key.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class PublishResult(
    val message: String,
    val publicUrl: String,
    val fingerprint: String? = null,
    val algorithm: String? = null
)

/**
 * Response from listing published public keys.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class PublicKeysListResponse(
    val tenant: String,
    val keys: List<PublicKeyInfo>
)
