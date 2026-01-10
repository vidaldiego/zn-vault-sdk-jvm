// Path: zn-vault-sdk-jvm/zn-vault-core/src/main/kotlin/com/zincapp/vault/secrets/SecretClient.kt
package com.zincapp.vault.secrets

import com.fasterxml.jackson.core.type.TypeReference
import com.zincapp.vault.http.ZnVaultHttpClient
import com.zincapp.vault.models.*
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.Instant
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
     * @param subType Optional semantic sub-type
     * @param tags Optional tags
     * @param fileName Optional filename for file-based secrets
     * @param expiresAt Optional natural expiration date
     * @param ttlUntil Optional user-defined expiration date
     * @param contentType Optional MIME type
     * @return Created secret metadata
     */
    fun create(
        alias: String,
        type: SecretType,
        data: Map<String, Any>,
        subType: SecretSubType? = null,
        tags: List<String> = emptyList(),
        fileName: String? = null,
        expiresAt: Instant? = null,
        ttlUntil: Instant? = null,
        contentType: String? = null
    ): Secret {
        return create(CreateSecretRequest(
            alias = alias,
            type = type,
            data = data,
            subType = subType,
            tags = tags,
            fileName = fileName,
            expiresAt = expiresAt,
            ttlUntil = ttlUntil,
            contentType = contentType
        ))
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
     * @param subType Optional semantic sub-type
     * @param fileName Optional filename
     * @param expiresAt Optional expiration date
     * @param ttlUntil Optional user-defined expiration
     * @param tags Optional tags
     * @param contentType Optional content type
     * @return Updated secret metadata
     */
    fun update(
        id: String,
        data: Map<String, Any>,
        subType: SecretSubType? = null,
        fileName: String? = null,
        expiresAt: Instant? = null,
        ttlUntil: Instant? = null,
        tags: List<String>? = null,
        contentType: String? = null
    ): Secret {
        val request = UpdateSecretRequest(
            data = data,
            subType = subType,
            fileName = fileName,
            expiresAt = expiresAt,
            ttlUntil = ttlUntil,
            tags = tags,
            contentType = contentType
        )
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

    // ==================== Convenience Methods for Typed Secret Creation ====================

    /**
     * Create a password credential secret.
     *
     * @param alias Secret alias
     * @param username Username
     * @param password Password
     * @param url Optional URL
     * @param notes Optional notes
     * @param tags Optional tags
     * @param ttlUntil Optional expiration
     * @return Created secret
     */
    fun createPassword(
        alias: String,
        username: String,
        password: String,
        url: String? = null,
        notes: String? = null,
        tags: List<String> = emptyList(),
        ttlUntil: Instant? = null
    ): Secret {
        val data = mutableMapOf<String, Any>(
            "username" to username,
            "password" to password
        )
        url?.let { data["url"] = it }
        notes?.let { data["notes"] = it }

        return create(
            alias = alias,
            type = SecretType.CREDENTIAL,
            data = data,
            subType = SecretSubType.PASSWORD,
            tags = tags,
            ttlUntil = ttlUntil
        )
    }

    /**
     * Create an API key credential secret.
     *
     * @param alias Secret alias
     * @param key API key
     * @param secret Optional API secret
     * @param endpoint Optional endpoint URL
     * @param notes Optional notes
     * @param tags Optional tags
     * @param ttlUntil Optional expiration
     * @return Created secret
     */
    fun createApiKey(
        alias: String,
        key: String,
        secret: String? = null,
        endpoint: String? = null,
        notes: String? = null,
        tags: List<String> = emptyList(),
        ttlUntil: Instant? = null
    ): Secret {
        val data = mutableMapOf<String, Any>("key" to key)
        secret?.let { data["secret"] = it }
        endpoint?.let { data["endpoint"] = it }
        notes?.let { data["notes"] = it }

        return create(
            alias = alias,
            type = SecretType.CREDENTIAL,
            data = data,
            subType = SecretSubType.API_KEY,
            tags = tags,
            ttlUntil = ttlUntil
        )
    }

    /**
     * Create a certificate secret with automatic expiration tracking.
     *
     * @param alias Secret alias
     * @param content Certificate content (PEM bytes)
     * @param fileName Optional filename
     * @param chain Optional certificate chain
     * @param expiresAt Optional expiration date
     * @param tags Optional tags
     * @return Created secret
     */
    fun createCertificate(
        alias: String,
        content: ByteArray,
        fileName: String? = null,
        chain: List<String>? = null,
        expiresAt: Instant? = null,
        tags: List<String> = emptyList()
    ): Secret {
        val data = mutableMapOf<String, Any>(
            "content" to Base64.getEncoder().encodeToString(content)
        )
        chain?.let { data["chain"] = it }

        return create(
            alias = alias,
            type = SecretType.OPAQUE,
            data = data,
            subType = SecretSubType.CERTIFICATE,
            tags = tags,
            fileName = fileName,
            expiresAt = expiresAt,
            contentType = "application/x-pem-file"
        )
    }

    /**
     * Create a private key secret.
     *
     * @param alias Secret alias
     * @param privateKey Private key content
     * @param fileName Optional filename
     * @param passphrase Optional passphrase
     * @param tags Optional tags
     * @return Created secret
     */
    fun createPrivateKey(
        alias: String,
        privateKey: ByteArray,
        fileName: String? = null,
        passphrase: String? = null,
        tags: List<String> = emptyList()
    ): Secret {
        val data = mutableMapOf<String, Any>(
            "privateKey" to Base64.getEncoder().encodeToString(privateKey)
        )
        passphrase?.let { data["passphrase"] = it }

        return create(
            alias = alias,
            type = SecretType.OPAQUE,
            data = data,
            subType = SecretSubType.PRIVATE_KEY,
            tags = tags,
            fileName = fileName
        )
    }

    /**
     * Create a key pair secret (public + private key).
     *
     * @param alias Secret alias
     * @param privateKey Private key content
     * @param publicKey Public key content
     * @param fileName Optional filename
     * @param passphrase Optional passphrase
     * @param tags Optional tags
     * @return Created secret
     */
    fun createKeypair(
        alias: String,
        privateKey: ByteArray,
        publicKey: ByteArray,
        fileName: String? = null,
        passphrase: String? = null,
        tags: List<String> = emptyList()
    ): Secret {
        val data = mutableMapOf<String, Any>(
            "privateKey" to Base64.getEncoder().encodeToString(privateKey),
            "publicKey" to Base64.getEncoder().encodeToString(publicKey)
        )
        passphrase?.let { data["passphrase"] = it }

        return create(
            alias = alias,
            type = SecretType.OPAQUE,
            data = data,
            subType = SecretSubType.KEYPAIR,
            tags = tags,
            fileName = fileName
        )
    }

    /**
     * Create a token secret (JWT, OAuth, bearer token).
     *
     * @param alias Secret alias
     * @param token Token value
     * @param tokenType Optional token type
     * @param refreshToken Optional refresh token
     * @param expiresAt Optional expiration date
     * @param tags Optional tags
     * @return Created secret
     */
    fun createToken(
        alias: String,
        token: String,
        tokenType: String? = null,
        refreshToken: String? = null,
        expiresAt: Instant? = null,
        tags: List<String> = emptyList()
    ): Secret {
        val data = mutableMapOf<String, Any>("token" to token)
        tokenType?.let { data["tokenType"] = it }
        refreshToken?.let { data["refreshToken"] = it }

        return create(
            alias = alias,
            type = SecretType.OPAQUE,
            data = data,
            subType = SecretSubType.TOKEN,
            tags = tags,
            expiresAt = expiresAt
        )
    }

    /**
     * Create a JSON configuration setting.
     *
     * @param alias Secret alias
     * @param content JSON configuration
     * @param tags Optional tags
     * @return Created secret
     */
    fun createJsonSetting(
        alias: String,
        content: Map<String, Any>,
        tags: List<String> = emptyList()
    ): Secret {
        return create(
            alias = alias,
            type = SecretType.SETTING,
            data = mapOf("content" to content),
            subType = SecretSubType.JSON,
            tags = tags,
            contentType = "application/json"
        )
    }

    /**
     * Create a YAML configuration setting.
     *
     * @param alias Secret alias
     * @param content YAML content as string
     * @param tags Optional tags
     * @return Created secret
     */
    fun createYamlSetting(
        alias: String,
        content: String,
        tags: List<String> = emptyList()
    ): Secret {
        return create(
            alias = alias,
            type = SecretType.SETTING,
            data = mapOf("content" to content),
            subType = SecretSubType.YAML,
            tags = tags,
            contentType = "application/x-yaml"
        )
    }

    /**
     * Create an environment variables setting (.env format).
     *
     * @param alias Secret alias
     * @param content Environment variables as map
     * @param tags Optional tags
     * @return Created secret
     */
    fun createEnvSetting(
        alias: String,
        content: Map<String, String>,
        tags: List<String> = emptyList()
    ): Secret {
        val envContent = content.entries.joinToString("\n") { "${it.key}=${it.value}" }

        return create(
            alias = alias,
            type = SecretType.SETTING,
            data = mapOf("content" to envContent),
            subType = SecretSubType.ENV,
            tags = tags,
            contentType = "text/plain"
        )
    }

    // ==================== Convenience Methods for Filtering ====================

    /**
     * List secrets by sub-type.
     *
     * @param subType Sub-type to filter by
     * @param page Page number
     * @param pageSize Page size
     * @return List of secrets
     */
    fun listBySubType(
        subType: SecretSubType,
        page: Int = 1,
        pageSize: Int = 100
    ): List<Secret> {
        return list(SecretFilter(subType = subType, page = page, pageSize = pageSize))
    }

    /**
     * List secrets by type.
     *
     * @param type Type to filter by
     * @param page Page number
     * @param pageSize Page size
     * @return List of secrets
     */
    fun listByType(
        type: SecretType,
        page: Int = 1,
        pageSize: Int = 100
    ): List<Secret> {
        return list(SecretFilter(type = type, page = page, pageSize = pageSize))
    }

    /**
     * List certificates expiring before a specific date.
     *
     * @param beforeDate Cutoff date
     * @param page Page number
     * @param pageSize Page size
     * @return List of expiring certificates
     */
    fun listExpiringCertificates(
        beforeDate: Instant,
        page: Int = 1,
        pageSize: Int = 100
    ): List<Secret> {
        return list(SecretFilter(
            subType = SecretSubType.CERTIFICATE,
            expiringBefore = beforeDate,
            page = page,
            pageSize = pageSize
        ))
    }

    /**
     * List all expiring secrets (certificates, tokens) before a specific date.
     *
     * @param beforeDate Cutoff date
     * @param page Page number
     * @param pageSize Page size
     * @return List of expiring secrets
     */
    fun listExpiring(
        beforeDate: Instant,
        page: Int = 1,
        pageSize: Int = 100
    ): List<Secret> {
        return list(SecretFilter(
            expiringBefore = beforeDate,
            page = page,
            pageSize = pageSize
        ))
    }

    /**
     * List secrets by alias prefix (hierarchical path).
     *
     * @param aliasPrefix Alias prefix to filter by
     * @param page Page number
     * @param pageSize Page size
     * @return List of secrets
     */
    fun listByPath(
        aliasPrefix: String,
        page: Int = 1,
        pageSize: Int = 100
    ): List<Secret> {
        return list(SecretFilter(
            aliasPrefix = aliasPrefix,
            page = page,
            pageSize = pageSize
        ))
    }

    // ==================== File Upload Helpers ====================

    /**
     * Upload a file as a secret.
     *
     * @param alias Secret alias
     * @param file File to upload
     * @param subType Optional sub-type (defaults to FILE)
     * @param expiresAt Optional expiration date
     * @param tags Optional tags
     * @return Created secret
     */
    fun uploadFile(
        alias: String,
        file: File,
        subType: SecretSubType? = null,
        expiresAt: Instant? = null,
        tags: List<String> = emptyList()
    ): Secret {
        val content = Base64.getEncoder().encodeToString(file.readBytes())
        val mimeType = Files.probeContentType(file.toPath()) ?: "application/octet-stream"

        return create(
            alias = alias,
            type = SecretType.OPAQUE,
            data = mapOf(
                "filename" to file.name,
                "content" to content,
                "contentType" to mimeType
            ),
            subType = subType ?: SecretSubType.FILE,
            tags = tags,
            fileName = file.name,
            expiresAt = expiresAt,
            contentType = mimeType
        )
    }

    /**
     * Upload a file from bytes.
     *
     * @param alias Secret alias
     * @param filename Original filename
     * @param content File content as bytes
     * @param contentType MIME type
     * @param subType Optional sub-type (defaults to FILE)
     * @param expiresAt Optional expiration date
     * @param tags Optional tags
     * @return Created secret
     */
    fun uploadFile(
        alias: String,
        filename: String,
        content: ByteArray,
        contentType: String = "application/octet-stream",
        subType: SecretSubType? = null,
        expiresAt: Instant? = null,
        tags: List<String> = emptyList()
    ): Secret {
        val base64Content = Base64.getEncoder().encodeToString(content)

        return create(
            alias = alias,
            type = SecretType.OPAQUE,
            data = mapOf(
                "filename" to filename,
                "content" to base64Content,
                "contentType" to contentType
            ),
            subType = subType ?: SecretSubType.FILE,
            tags = tags,
            fileName = filename,
            expiresAt = expiresAt,
            contentType = contentType
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

    /**
     * Download a file secret with metadata.
     *
     * @param id Secret ID
     * @return Triple of (content, filename, contentType)
     */
    fun downloadFileWithMetadata(id: String): Triple<ByteArray, String, String> {
        val data = decrypt(id)
        val content = data.data["content"] as? String
            ?: throw IllegalStateException("No content field in secret data")
        val bytes = Base64.getDecoder().decode(content)
        val filename = (data.data["filename"] as? String) ?: data.fileName ?: "file"
        val contentType = (data.data["contentType"] as? String) ?: data.fileMime ?: "application/octet-stream"
        return Triple(bytes, filename, contentType)
    }

    // ==================== Legacy Credential Helpers ====================

    /**
     * Create a credential secret (legacy, use createPassword instead).
     *
     * @param alias Secret alias
     * @param username Username
     * @param password Password
     * @param tags Optional tags
     * @return Created secret
     */
    @Deprecated("Use createPassword instead", ReplaceWith("createPassword(alias, username, password, tags = tags)"))
    fun createCredential(
        alias: String,
        username: String,
        password: String,
        tags: List<String> = emptyList()
    ): Secret {
        return createPassword(alias, username, password, tags = tags)
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

    // ==================== Keypair Generation and Publishing ====================

    /**
     * Generate a cryptographic keypair (RSA, Ed25519, or ECDSA).
     *
     * @param request Keypair generation request
     * @return Generated keypair with private and public key metadata
     */
    fun generateKeypair(request: GenerateKeypairRequest): GeneratedKeypair {
        return httpClient.post("/v1/secrets/generate-keypair", request, GeneratedKeypair::class.java)
    }

    /**
     * Generate a cryptographic keypair with simplified parameters.
     *
     * @param algorithm Keypair algorithm (RSA, Ed25519, ECDSA)
     * @param alias Secret alias for the private key
     * @param tenant Tenant name
     * @param rsaBits RSA key size (2048 or 4096, defaults to 2048)
     * @param ecdsaCurve ECDSA curve (P-256 or P-384, defaults to P-256)
     * @param comment Optional comment for the keypair
     * @param publishPublicKey Whether to immediately publish the public key
     * @param tags Optional tags
     * @return Generated keypair
     */
    fun generateKeypair(
        algorithm: KeypairAlgorithm,
        alias: String,
        tenant: String,
        rsaBits: Int? = null,
        ecdsaCurve: EcdsaCurve? = null,
        comment: String? = null,
        publishPublicKey: Boolean? = null,
        tags: List<String> = emptyList()
    ): GeneratedKeypair {
        return generateKeypair(GenerateKeypairRequest(
            algorithm = algorithm,
            alias = alias,
            tenant = tenant,
            rsaBits = rsaBits,
            ecdsaCurve = ecdsaCurve,
            comment = comment,
            publishPublicKey = publishPublicKey,
            tags = tags
        ))
    }

    /**
     * Publish a public key to make it publicly accessible without authentication.
     * Only works for public key sub-types (ed25519_public_key, rsa_public_key, ecdsa_public_key).
     *
     * @param secretId Secret ID of the public key to publish
     * @return Publish result with public URL and fingerprint
     */
    fun publish(secretId: String): PublishResult {
        return httpClient.postEmpty("/v1/secrets/$secretId/publish", PublishResult::class.java)
    }

    /**
     * Make a published public key private again.
     *
     * @param secretId Secret ID of the public key to unpublish
     */
    fun unpublish(secretId: String) {
        httpClient.postEmpty("/v1/secrets/$secretId/unpublish", Map::class.java)
    }

    /**
     * Get a published public key by tenant and alias (no authentication required).
     *
     * @param tenant Tenant name
     * @param alias Public key alias
     * @return Public key information
     */
    fun getPublicKey(tenant: String, alias: String): PublicKeyInfo {
        val encodedAlias = encode(alias)
        return httpClient.get("/v1/public/$tenant/$encodedAlias", PublicKeyInfo::class.java)
    }

    /**
     * List all published public keys for a tenant (no authentication required).
     *
     * @param tenant Tenant name
     * @return List of published public keys
     */
    fun listPublicKeys(tenant: String): List<PublicKeyInfo> {
        val response = httpClient.get("/v1/public/$tenant", PublicKeysListResponse::class.java)
        return response.keys
    }

    private fun buildQueryParams(filter: SecretFilter): String {
        val params = mutableListOf<String>()

        filter.type?.let { params.add("type=${it.name.lowercase()}") }
        filter.subType?.let { params.add("subType=${encode(it.name.lowercase())}") }
        filter.fileMime?.let { params.add("fileMime=${encode(it)}") }
        filter.expiringBefore?.let { params.add("expiringBefore=${encode(it.toString())}") }
        filter.aliasPrefix?.let { params.add("aliasPrefix=${encode(it)}") }
        filter.tags?.forEach { params.add("tags=${encode(it)}") }
        params.add("page=${filter.page}")
        params.add("pageSize=${filter.pageSize}")

        return if (params.isNotEmpty()) "?${params.joinToString("&")}" else ""
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8)
}
