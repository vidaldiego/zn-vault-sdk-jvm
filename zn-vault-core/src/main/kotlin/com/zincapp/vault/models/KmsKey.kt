// Path: zn-vault-sdk-jvm/zn-vault-core/src/main/kotlin/com/zincapp/vault/models/KmsKey.kt
package com.zincapp.vault.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

/**
 * Represents a KMS Customer Master Key (CMK).
 * Note: API uses camelCase for fields.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class KmsKey(
    val keyId: String,
    val alias: String? = null,
    val description: String? = null,
    @JsonProperty("keyUsage") val usage: KeyUsage? = null,
    val keySpec: KeySpec? = null,
    val origin: KeyOrigin = KeyOrigin.ZN_VAULT,
    @JsonProperty("keyState") val state: KeyState? = null,
    val tenant: String? = null,
    val arn: String? = null,
    val createdDate: Instant? = null,
    val deletionDate: Instant? = null,
    val multiRegion: Boolean = false,
    val tags: List<KeyTag> = emptyList(),
    val currentVersion: Int = 1
) {
    // Computed property for enabled status based on state
    val enabled: Boolean get() = state == KeyState.ENABLED
}

/**
 * Wrapper for get key response (has keyMetadata field).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class KeyMetadataResponse(
    val keyMetadata: KmsKey
)

/**
 * Page response for KMS keys (uses 'keys' instead of 'data').
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class KeyListResponse(
    val keys: List<KmsKey> = emptyList(),
    val truncated: Boolean = false,
    val nextMarker: String? = null
) {
    val items: List<KmsKey> get() = keys
    val hasMore: Boolean get() = truncated
}

/**
 * Key usage type.
 */
enum class KeyUsage {
    @JsonProperty("ENCRYPT_DECRYPT") ENCRYPT_DECRYPT,
    @JsonProperty("SIGN_VERIFY") SIGN_VERIFY
}

/**
 * Key specification (algorithm and size).
 */
enum class KeySpec {
    @JsonProperty("AES_256") AES_256,
    @JsonProperty("AES_128") AES_128,
    @JsonProperty("RSA_2048") RSA_2048,
    @JsonProperty("RSA_4096") RSA_4096,
    @JsonProperty("ECC_NIST_P256") ECC_NIST_P256,
    @JsonProperty("ECC_NIST_P384") ECC_NIST_P384
}

/**
 * Key origin.
 */
enum class KeyOrigin {
    @JsonProperty("ZN_VAULT") ZN_VAULT,
    @JsonProperty("EXTERNAL") EXTERNAL,
    @JsonProperty("AWS_KMS") AWS_KMS
}

/**
 * Key state.
 */
enum class KeyState {
    @JsonProperty("ENABLED") ENABLED,
    @JsonProperty("DISABLED") DISABLED,
    @JsonProperty("PENDING_DELETION") PENDING_DELETION,
    @JsonProperty("PENDING_IMPORT") PENDING_IMPORT
}

/**
 * Key tag.
 */
data class KeyTag(
    val key: String,
    val value: String
)

/**
 * Request to create a new KMS key.
 */
data class CreateKeyRequest(
    val alias: String? = null,
    val description: String? = null,
    val usage: KeyUsage = KeyUsage.ENCRYPT_DECRYPT,
    @JsonProperty("key_spec") val keySpec: KeySpec = KeySpec.AES_256,
    val origin: KeyOrigin = KeyOrigin.ZN_VAULT,
    @JsonProperty("multi_region") val multiRegion: Boolean = false,
    val tags: List<KeyTag> = emptyList(),
    val tenant: String
)

/**
 * Filter for listing KMS keys.
 */
data class KeyFilter(
    val tenant: String? = null,
    val alias: String? = null,
    val usage: KeyUsage? = null,
    val state: KeyState? = null,
    val limit: Int = 50,
    val marker: String? = null
)

/**
 * Result of an encryption operation.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class EncryptResult(
    val ciphertext: String,
    val keyId: String,
    val encryptionContext: Map<String, String>? = null,
    val keyVersion: Int? = null
) {
    // Alias for backwards compatibility
    val ciphertextBlob: String get() = ciphertext
}

/**
 * Result of a decrypt operation.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class DecryptResult(
    val plaintext: String,
    val keyId: String,
    val encryptionContext: Map<String, String>? = null
)

/**
 * Request for encrypt operation.
 */
data class EncryptRequest(
    val keyId: String,
    val plaintext: String,
    val context: Map<String, String> = emptyMap()
)

/**
 * Request for decrypt operation.
 */
data class DecryptRequest(
    val keyId: String,
    val ciphertext: String,
    val context: Map<String, String> = emptyMap()
)

/**
 * Request for re-encrypt operation.
 */
data class ReEncryptRequest(
    val sourceKeyId: String,
    val destinationKeyId: String,
    val ciphertext: String,
    val sourceContext: Map<String, String> = emptyMap(),
    val destinationContext: Map<String, String> = emptyMap()
)

/**
 * Result of generate data key operation.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class DataKeyResult(
    val plaintext: String? = null,
    val ciphertext: String,
    val keyId: String
) {
    // Alias for backwards compatibility
    val ciphertextBlob: String get() = ciphertext
}

/**
 * Request for generate data key operation.
 */
data class GenerateDataKeyRequest(
    val keyId: String,
    val keySpec: KeySpec = KeySpec.AES_256,
    val context: Map<String, String> = emptyMap()
)

/**
 * Rotation status for a key.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class RotationStatus(
    val keyId: String,
    val rotationEnabled: Boolean = false,
    val intervalDays: Int = 365,
    val rotationCount: Int = 0,
    val nextRotationDate: Instant? = null,
    val lastRotatedDate: Instant? = null
) {
    // Alias for backwards compatibility
    val rotationPeriodDays: Int get() = intervalDays
}

/**
 * Request to configure key rotation.
 */
data class RotationConfigRequest(
    val enabled: Boolean,
    val intervalDays: Int = 365
)

/**
 * Key policy.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class KeyPolicy(
    val policy: String,
    val keyId: String
)

/**
 * Key grant.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Grant(
    val grantId: String,
    val keyId: String,
    val granteePrincipal: String,
    val operations: List<String>,
    val createdAt: Instant? = null,
    val constraints: Map<String, Any>? = null
)

/**
 * Request to create a grant.
 */
data class CreateGrantRequest(
    val keyId: String,
    val granteePrincipal: String,
    val operations: List<String>,
    val constraints: Map<String, Any>? = null
)
