// Path: zn-vault-sdk-jvm/zn-vault-core/src/main/kotlin/com/zincware/vault/kms/KmsClient.kt
package com.zincware.vault.kms

import com.fasterxml.jackson.core.type.TypeReference
import com.zincware.vault.http.ZnVaultHttpClient
import com.zincware.vault.models.*
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * Client for Key Management Service (KMS) operations.
 */
class KmsClient internal constructor(
    private val httpClient: ZnVaultHttpClient
) {

    // ==================== Key Lifecycle ====================

    /**
     * Create a new KMS key (Customer Master Key).
     *
     * @param request Create key request
     * @return Created key
     */
    fun createKey(request: CreateKeyRequest): KmsKey {
        return httpClient.post("/v1/kms/keys", request, KmsKey::class.java)
    }

    /**
     * Create a new AES-256 encryption key.
     *
     * @param tenant Tenant ID
     * @param alias Optional alias
     * @param description Optional description
     * @return Created key
     */
    fun createKey(
        tenant: String,
        alias: String? = null,
        description: String? = null
    ): KmsKey {
        return createKey(
            CreateKeyRequest(
                alias = alias,
                description = description,
                tenant = tenant
            )
        )
    }

    /**
     * Get key details by ID.
     *
     * @param keyId Key ID
     * @return Key details
     */
    fun getKey(keyId: String): KmsKey {
        val response = httpClient.get("/v1/kms/keys/$keyId", KeyMetadataResponse::class.java)
        return response.keyMetadata
    }

    /**
     * List KMS keys with optional filtering.
     *
     * @param filter Filter criteria
     * @return Key list response
     */
    fun listKeys(filter: KeyFilter = KeyFilter()): KeyListResponse {
        val params = buildKeyFilterParams(filter)
        return httpClient.get("/v1/kms/keys$params", KeyListResponse::class.java)
    }

    /**
     * Update key description.
     *
     * @param keyId Key ID
     * @param description New description
     * @return Updated key
     */
    fun updateDescription(keyId: String, description: String): KmsKey {
        httpClient.put(
            "/v1/kms/keys/$keyId/description",
            mapOf("description" to description),
            SuccessResponse::class.java
        )
        return getKey(keyId)
    }

    /**
     * Update key alias.
     *
     * @param keyId Key ID
     * @param alias New alias
     * @return Updated key
     */
    fun updateAlias(keyId: String, alias: String): KmsKey {
        httpClient.put(
            "/v1/kms/keys/$keyId/alias",
            mapOf("alias" to alias),
            SuccessResponse::class.java
        )
        return getKey(keyId)
    }

    /**
     * Enable a key.
     *
     * @param keyId Key ID
     * @return Updated key
     */
    fun enableKey(keyId: String): KmsKey {
        httpClient.postEmpty("/v1/kms/keys/$keyId/enable", SuccessResponse::class.java)
        return getKey(keyId)
    }

    /**
     * Disable a key.
     *
     * @param keyId Key ID
     * @return Updated key
     */
    fun disableKey(keyId: String): KmsKey {
        httpClient.postEmpty("/v1/kms/keys/$keyId/disable", SuccessResponse::class.java)
        return getKey(keyId)
    }

    /**
     * Schedule key deletion.
     *
     * @param keyId Key ID
     * @param pendingWindowDays Days until deletion (7-30)
     * @return Updated key
     */
    fun scheduleKeyDeletion(keyId: String, pendingWindowDays: Int = 7): KmsKey {
        httpClient.post(
            "/v1/kms/keys/$keyId/schedule-deletion",
            mapOf("pendingWindowDays" to pendingWindowDays),
            SuccessResponse::class.java
        )
        return getKey(keyId)
    }

    /**
     * Cancel scheduled key deletion.
     *
     * @param keyId Key ID
     * @return Updated key
     */
    fun cancelKeyDeletion(keyId: String): KmsKey {
        httpClient.postEmpty("/v1/kms/keys/$keyId/cancel-deletion", SuccessResponse::class.java)
        return getKey(keyId)
    }

    // ==================== Cryptographic Operations ====================

    /**
     * Encrypt data using a KMS key.
     *
     * @param keyId Key ID
     * @param plaintext Data to encrypt (will be base64 encoded)
     * @param context Optional encryption context (AAD)
     * @return Encryption result with ciphertext
     */
    fun encrypt(
        keyId: String,
        plaintext: ByteArray,
        context: Map<String, String> = emptyMap()
    ): EncryptResult {
        val request = EncryptRequest(
            keyId = keyId,
            plaintext = Base64.getEncoder().encodeToString(plaintext),
            context = context
        )
        return httpClient.post("/v1/kms/encrypt", request, EncryptResult::class.java)
    }

    /**
     * Encrypt a string using a KMS key.
     *
     * @param keyId Key ID
     * @param plaintext String to encrypt
     * @param context Optional encryption context
     * @return Encryption result
     */
    fun encrypt(
        keyId: String,
        plaintext: String,
        context: Map<String, String> = emptyMap()
    ): EncryptResult {
        return encrypt(keyId, plaintext.toByteArray(Charsets.UTF_8), context)
    }

    /**
     * Decrypt data using a KMS key.
     *
     * @param keyId Key ID
     * @param ciphertextBlob Ciphertext to decrypt
     * @param context Encryption context used during encryption
     * @return Decrypted data as bytes
     */
    fun decrypt(
        keyId: String,
        ciphertext: String,
        context: Map<String, String> = emptyMap()
    ): ByteArray {
        val request = DecryptRequest(
            keyId = keyId,
            ciphertext = ciphertext,
            context = context
        )
        val result = httpClient.post("/v1/kms/decrypt", request, DecryptResult::class.java)
        return Base64.getDecoder().decode(result.plaintext)
    }

    /**
     * Decrypt data to a string.
     *
     * @param keyId Key ID
     * @param ciphertext Ciphertext to decrypt
     * @param context Encryption context
     * @return Decrypted string
     */
    fun decryptToString(
        keyId: String,
        ciphertext: String,
        context: Map<String, String> = emptyMap()
    ): String {
        return String(decrypt(keyId, ciphertext, context), Charsets.UTF_8)
    }

    /**
     * Re-encrypt data from one key to another.
     *
     * @param sourceKeyId Source key ID
     * @param destinationKeyId Destination key ID
     * @param ciphertext Ciphertext to re-encrypt
     * @param sourceContext Source encryption context
     * @param destinationContext Destination encryption context
     * @return New ciphertext encrypted with destination key
     */
    fun reEncrypt(
        sourceKeyId: String,
        destinationKeyId: String,
        ciphertext: String,
        sourceContext: Map<String, String> = emptyMap(),
        destinationContext: Map<String, String> = emptyMap()
    ): EncryptResult {
        val request = ReEncryptRequest(
            sourceKeyId = sourceKeyId,
            destinationKeyId = destinationKeyId,
            ciphertext = ciphertext,
            sourceContext = sourceContext,
            destinationContext = destinationContext
        )
        return httpClient.post("/v1/kms/re-encrypt", request, EncryptResult::class.java)
    }

    /**
     * Generate a data encryption key (DEK).
     *
     * Returns both the plaintext DEK (for immediate use) and the encrypted DEK
     * (for storage). The plaintext should be used and then discarded.
     *
     * @param keyId Key ID to wrap the DEK
     * @param keySpec Key specification for the DEK
     * @param context Encryption context
     * @return Data key result with plaintext and ciphertext
     */
    fun generateDataKey(
        keyId: String,
        keySpec: KeySpec = KeySpec.AES_256,
        context: Map<String, String> = emptyMap()
    ): DataKeyResult {
        val request = GenerateDataKeyRequest(keyId, keySpec, context)
        return httpClient.post("/v1/kms/generate-data-key", request, DataKeyResult::class.java)
    }

    /**
     * Generate a data encryption key without plaintext.
     *
     * Use this when you want to generate a DEK that will be decrypted later.
     *
     * @param keyId Key ID to wrap the DEK
     * @param keySpec Key specification for the DEK
     * @param context Encryption context
     * @return Encrypted DEK (ciphertext only)
     */
    fun generateDataKeyWithoutPlaintext(
        keyId: String,
        keySpec: KeySpec = KeySpec.AES_256,
        context: Map<String, String> = emptyMap()
    ): String {
        val request = GenerateDataKeyRequest(keyId, keySpec, context)
        val result = httpClient.post(
            "/v1/kms/generate-data-key-without-plaintext",
            request,
            DataKeyResult::class.java
        )
        return result.ciphertextBlob
    }

    // ==================== Key Rotation ====================

    /**
     * Manually rotate a key.
     *
     * @param keyId Key ID
     * @return Updated key with new version
     */
    fun rotateKey(keyId: String): KmsKey {
        httpClient.postEmpty("/v1/kms/keys/$keyId/rotate", SuccessResponse::class.java)
        return getKey(keyId)
    }

    /**
     * Get key rotation status.
     *
     * @param keyId Key ID
     * @return Rotation status
     */
    fun getRotationStatus(keyId: String): RotationStatus {
        return httpClient.get("/v1/kms/keys/$keyId/rotation-status", RotationStatus::class.java)
    }

    /**
     * Configure automatic key rotation.
     *
     * @param keyId Key ID
     * @param enabled Enable or disable rotation
     * @param intervalDays Rotation interval in days
     * @return Updated rotation status
     */
    fun configureRotation(
        keyId: String,
        enabled: Boolean,
        intervalDays: Int = 365
    ): RotationStatus {
        val request = RotationConfigRequest(enabled, intervalDays)
        return httpClient.put("/v1/kms/keys/$keyId/rotation-status", request, RotationStatus::class.java)
    }

    // ==================== Key Policies & Grants ====================

    /**
     * Get key policy.
     *
     * @param keyId Key ID
     * @return Key policy
     */
    fun getKeyPolicy(keyId: String): KeyPolicy {
        return httpClient.get("/v1/kms/keys/$keyId/policy", KeyPolicy::class.java)
    }

    /**
     * Set key policy.
     *
     * @param keyId Key ID
     * @param policy Policy document
     * @return Updated key policy
     */
    fun setKeyPolicy(keyId: String, policy: String): KeyPolicy {
        return httpClient.put(
            "/v1/kms/keys/$keyId/policy",
            mapOf("policy" to policy),
            KeyPolicy::class.java
        )
    }

    /**
     * Create a grant for a key.
     *
     * @param request Create grant request
     * @return Created grant
     */
    fun createGrant(request: CreateGrantRequest): Grant {
        return httpClient.post("/v1/kms/grants", request, Grant::class.java)
    }

    /**
     * List grants for a key.
     *
     * @param keyId Optional key ID to filter by
     * @return List of grants
     */
    fun listGrants(keyId: String? = null): List<Grant> {
        val path = if (keyId != null) "/v1/kms/grants?key_id=$keyId" else "/v1/kms/grants"
        val response = httpClient.get(path,
            object : TypeReference<ApiResponse<List<Grant>>>() {})
        return response.data ?: emptyList()
    }

    /**
     * Revoke a grant.
     *
     * @param grantId Grant ID
     */
    fun revokeGrant(grantId: String) {
        httpClient.postEmpty("/v1/kms/grants/$grantId/revoke", SuccessResponse::class.java)
    }

    private fun buildKeyFilterParams(filter: KeyFilter): String {
        val params = mutableListOf<String>()

        filter.tenant?.let { params.add("tenant=${encode(it)}") }
        filter.alias?.let { params.add("alias=${encode(it)}") }
        filter.usage?.let { params.add("usage=${it.name}") }
        filter.state?.let { params.add("state=${it.name}") }
        params.add("limit=${filter.limit}")
        filter.marker?.let { params.add("marker=${encode(it)}") }

        return if (params.isNotEmpty()) "?${params.joinToString("&")}" else ""
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8)
}
