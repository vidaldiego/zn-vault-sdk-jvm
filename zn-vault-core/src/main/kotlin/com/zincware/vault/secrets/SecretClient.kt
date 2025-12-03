// Path: zn-vault-sdk-jvm/zn-vault-core/src/main/kotlin/com/zincware/vault/secrets/SecretClient.kt
package com.zincware.vault.secrets

import com.fasterxml.jackson.core.type.TypeReference
import com.zincware.vault.http.ZnVaultHttpClient
import com.zincware.vault.models.*
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.*

/**
 * Client for secret management operations.
 */
class SecretClient internal constructor(
    private val httpClient: ZnVaultHttpClient
) {

    /**
     * Create a new secret.
     *
     * @param request Create secret request
     * @return Created secret metadata
     */
    fun create(request: CreateSecretRequest): Secret {
        return httpClient.post("/v1/secrets", request, Secret::class.java)
    }

    /**
     * Create a new secret with simplified parameters.
     *
     * @param alias Path-based alias (e.g., "api/production/db-creds")
     * @param type Secret type
     * @param data Secret data
     * @param tags Optional tags
     * @return Created secret metadata
     */
    fun create(
        alias: String,
        type: SecretType,
        data: Map<String, Any>,
        tags: List<String> = emptyList()
    ): Secret {
        return create(CreateSecretRequest(alias, type, data, tags))
    }

    /**
     * Get secret metadata by ID.
     *
     * @param id Secret ID
     * @return Secret metadata (without the encrypted value)
     */
    fun get(id: String): Secret {
        return httpClient.get("/v1/secrets/$id/meta", Secret::class.java)
    }

    /**
     * Get secret by alias path.
     *
     * @param alias Secret alias path
     * @return Secret metadata
     */
    fun getByAlias(alias: String): Secret {
        val encodedAlias = URLEncoder.encode(alias, StandardCharsets.UTF_8)
        return httpClient.get("/v1/secrets/alias/$encodedAlias", Secret::class.java)
    }

    /**
     * Decrypt a secret's value.
     *
     * @param id Secret ID
     * @return Decrypted secret data
     */
    fun decrypt(id: String): SecretData {
        return httpClient.postEmpty("/v1/secrets/$id/decrypt", SecretData::class.java)
    }

    /**
     * Update a secret's data (creates a new version).
     *
     * @param id Secret ID
     * @param data New secret data
     * @return Updated secret metadata
     */
    fun update(id: String, data: Map<String, Any>): Secret {
        val request = UpdateSecretRequest(data)
        return httpClient.put("/v1/secrets/$id", request, Secret::class.java)
    }

    /**
     * Update secret metadata (tags only).
     *
     * @param id Secret ID
     * @param tags New tags
     * @return Updated secret metadata
     */
    fun updateMetadata(id: String, tags: List<String>): Secret {
        val request = UpdateSecretMetadataRequest(tags)
        return httpClient.patch("/v1/secrets/$id/metadata", request, Secret::class.java)
    }

    /**
     * Rotate a secret to a new value.
     *
     * @param id Secret ID
     * @param newData New secret data
     * @return Rotated secret metadata
     */
    fun rotate(id: String, newData: Map<String, Any>): Secret {
        val request = UpdateSecretRequest(newData)
        return httpClient.post("/v1/secrets/$id/rotate", request, Secret::class.java)
    }

    /**
     * Delete a secret (soft delete).
     *
     * @param id Secret ID
     */
    fun delete(id: String) {
        httpClient.delete("/v1/secrets/$id")
    }

    /**
     * List secrets with optional filtering.
     *
     * @param filter Filter criteria
     * @return List of secrets (metadata only)
     */
    fun list(filter: SecretFilter = SecretFilter()): List<Secret> {
        val params = buildQueryParams(filter)
        return httpClient.get("/v1/secrets$params",
            object : TypeReference<List<Secret>>() {})
    }

    /**
     * List all secrets.
     *
     * @return List of secrets
     */
    fun listAll(): List<Secret> {
        return list(SecretFilter())
    }

    /**
     * Get secret version history.
     *
     * @param id Secret ID
     * @return List of secret versions
     */
    fun getHistory(id: String): List<SecretVersion> {
        val response = httpClient.get("/v1/secrets/$id/history", SecretHistoryResponse::class.java)
        return response.history
    }

    /**
     * Decrypt a specific version of a secret.
     *
     * @param id Secret ID
     * @param version Version number
     * @return Decrypted secret data
     */
    fun decryptVersion(id: String, version: Int): SecretData {
        return httpClient.postEmpty("/v1/secrets/$id/history/$version/decrypt", SecretData::class.java)
    }

    // ==================== File Upload Helpers ====================

    /**
     * Upload a file as a secret.
     *
     * @param alias Secret alias
     * @param file File to upload
     * @param tags Optional tags
     * @return Created secret
     */
    fun uploadFile(
        alias: String,
        file: File,
        tags: List<String> = emptyList()
    ): Secret {
        val content = Base64.getEncoder().encodeToString(file.readBytes())
        val mimeType = Files.probeContentType(file.toPath()) ?: "application/octet-stream"

        return create(
            CreateSecretRequest(
                alias = alias,
                type = SecretType.OPAQUE,
                data = mapOf(
                    "filename" to file.name,
                    "content" to content,
                    "contentType" to mimeType
                ),
                tags = tags
            )
        )
    }

    /**
     * Upload a file from bytes.
     *
     * @param alias Secret alias
     * @param filename Original filename
     * @param content File content as bytes
     * @param contentType MIME type
     * @param tags Optional tags
     * @return Created secret
     */
    fun uploadFile(
        alias: String,
        filename: String,
        content: ByteArray,
        contentType: String = "application/octet-stream",
        tags: List<String> = emptyList()
    ): Secret {
        val base64Content = Base64.getEncoder().encodeToString(content)

        return create(
            CreateSecretRequest(
                alias = alias,
                type = SecretType.OPAQUE,
                data = mapOf(
                    "filename" to filename,
                    "content" to base64Content,
                    "contentType" to contentType
                ),
                tags = tags
            )
        )
    }

    /**
     * Download a file secret as bytes.
     *
     * @param id Secret ID
     * @return File content as bytes
     */
    fun downloadFile(id: String): ByteArray {
        val data = decrypt(id)
        val content = data.data["content"] as? String
            ?: throw IllegalStateException("No content field in secret data")
        return Base64.getDecoder().decode(content)
    }

    /**
     * Download a file secret to a file.
     *
     * @param id Secret ID
     * @param destination Destination file
     */
    fun downloadFile(id: String, destination: File) {
        val bytes = downloadFile(id)
        destination.writeBytes(bytes)
    }

    // ==================== Credential Helpers ====================

    /**
     * Create a credential secret.
     *
     * @param alias Secret alias
     * @param username Username
     * @param password Password
     * @param tags Optional tags
     * @return Created secret
     */
    fun createCredential(
        alias: String,
        username: String,
        password: String,
        tags: List<String> = emptyList()
    ): Secret {
        return create(
            CreateSecretRequest(
                alias = alias,
                type = SecretType.CREDENTIAL,
                data = mapOf(
                    "username" to username,
                    "password" to password
                ),
                tags = tags
            )
        )
    }

    /**
     * Get credentials from a secret.
     *
     * @param id Secret ID
     * @return Pair of (username, password)
     */
    fun getCredentials(id: String): Pair<String, String> {
        val data = decrypt(id)
        val username = data.data["username"] as? String
            ?: throw IllegalStateException("No username in credential")
        val password = data.data["password"] as? String
            ?: throw IllegalStateException("No password in credential")
        return username to password
    }

    private fun buildQueryParams(filter: SecretFilter): String {
        val params = mutableListOf<String>()

        filter.type?.let { params.add("type=${it.name.lowercase()}") }
        filter.tags?.forEach { params.add("tags=${encode(it)}") }
        params.add("limit=${filter.limit}")
        params.add("offset=${filter.offset}")
        filter.marker?.let { params.add("marker=${encode(it)}") }

        return if (params.isNotEmpty()) "?${params.joinToString("&")}" else ""
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8)
}
