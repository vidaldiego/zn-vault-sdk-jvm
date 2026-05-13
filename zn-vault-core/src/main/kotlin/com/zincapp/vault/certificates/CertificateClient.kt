// Path: zn-vault-sdk-jvm/zn-vault-core/src/main/kotlin/com/zincapp/vault/certificates/CertificateClient.kt
package com.zincapp.vault.certificates

import com.fasterxml.jackson.core.type.TypeReference
import com.zincapp.vault.http.ZnVaultHttpClient
import com.zincapp.vault.models.*
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * Client for certificate lifecycle management operations.
 *
 * Tenant is always derived from the authenticated principal by the server;
 * there are no client-supplied tenant parameters on this client. For
 * cross-tenant certificate operations, use ZnVaultSuperadminClient.
 */
class CertificateClient internal constructor(
    private val httpClient: ZnVaultHttpClient
) {

    // ==================== CRUD Operations ====================

    /** Store a new certificate for custody. */
    fun store(request: StoreCertificateRequest): Certificate {
        return httpClient.post("/v1/certificates", request, Certificate::class.java)
    }

    /** Get certificate metadata by ID. */
    fun get(id: String): Certificate {
        return httpClient.get("/v1/certificates/$id", Certificate::class.java)
    }

    /** Get certificate by business identity (clientId/kind/alias). */
    fun getByIdentity(
        clientId: String,
        kind: String,
        alias: String
    ): Certificate {
        val encodedClientId = encode(clientId)
        val encodedKind = encode(kind)
        val encodedAlias = encode(alias)
        return httpClient.get(
            "/v1/certificates/by-identity/$encodedClientId/$encodedKind/$encodedAlias",
            Certificate::class.java
        )
    }

    /** List certificates with optional filtering. */
    fun list(filter: CertificateFilter = CertificateFilter()): CertificateListResponse {
        val params = buildQueryParams(filter)
        return httpClient.get("/v1/certificates$params", CertificateListResponse::class.java)
    }

    /** Get certificate statistics. */
    fun getStats(): CertificateStats {
        return httpClient.get("/v1/certificates/stats", CertificateStats::class.java)
    }

    /** List certificates expiring within a specified number of days. */
    fun listExpiring(days: Int = 30): List<Certificate> {
        return httpClient.get(
            "/v1/certificates/expiring?days=$days",
            object : TypeReference<List<Certificate>>() {}
        )
    }

    /** Update certificate metadata. */
    fun update(id: String, request: UpdateCertificateRequest): Certificate {
        return httpClient.patch("/v1/certificates/$id", request, Certificate::class.java)
    }

    /**
     * Decrypt certificate (retrieve the actual certificate data).
     * Requires business justification - the purpose is logged for audit.
     */
    fun decrypt(id: String, purpose: String): DecryptedCertificate {
        val request = DecryptCertificateRequest(purpose)
        return httpClient.post("/v1/certificates/$id/decrypt", request, DecryptedCertificate::class.java)
    }

    /**
     * Rotate certificate (replace with a new certificate).
     * The old certificate is preserved in history.
     */
    fun rotate(id: String, request: RotateCertificateRequest): Certificate {
        return httpClient.post("/v1/certificates/$id/rotate", request, Certificate::class.java)
    }

    /**
     * Delete a certificate.
     * The underlying secret data is preserved for audit purposes.
     */
    fun delete(id: String) {
        httpClient.delete("/v1/certificates/$id")
    }

    /** Get certificate access log. */
    fun getAccessLog(id: String, limit: Int = 100): List<CertificateAccessLogEntry> {
        val response = httpClient.get(
            "/v1/certificates/$id/access-log?limit=$limit",
            CertificateAccessLogResponse::class.java
        )
        return response.entries
    }

    // ==================== Convenience Methods ====================

    /** Store a P12 certificate with simplified parameters. */
    fun storeP12(
        clientId: String,
        kind: String,
        alias: String,
        p12Data: ByteArray,
        passphrase: String,
        purpose: CertificatePurpose,
        clientName: String? = null,
        organizationId: String? = null,
        contactEmail: String? = null,
        tags: List<String> = emptyList(),
        metadata: Map<String, Any> = emptyMap()
    ): Certificate {
        val certificateData = Base64.getEncoder().encodeToString(p12Data)
        val request = StoreCertificateRequest(
            clientId = clientId,
            kind = kind,
            alias = alias,
            certificateData = certificateData,
            certificateType = CertificateType.P12,
            purpose = purpose,
            passphrase = passphrase,
            clientName = clientName,
            organizationId = organizationId,
            contactEmail = contactEmail,
            tags = tags,
            metadata = metadata
        )
        return store(request)
    }

    /** Store a PEM certificate with simplified parameters. */
    fun storePEM(
        clientId: String,
        kind: String,
        alias: String,
        pemData: ByteArray,
        purpose: CertificatePurpose,
        clientName: String? = null,
        organizationId: String? = null,
        contactEmail: String? = null,
        tags: List<String> = emptyList(),
        metadata: Map<String, Any> = emptyMap()
    ): Certificate {
        val certificateData = Base64.getEncoder().encodeToString(pemData)
        val request = StoreCertificateRequest(
            clientId = clientId,
            kind = kind,
            alias = alias,
            certificateData = certificateData,
            certificateType = CertificateType.PEM,
            purpose = purpose,
            clientName = clientName,
            organizationId = organizationId,
            contactEmail = contactEmail,
            tags = tags,
            metadata = metadata
        )
        return store(request)
    }

    /** List certificates by client ID. */
    fun listByClient(
        clientId: String,
        limit: Int = 50,
        offset: Int = 0
    ): CertificateListResponse {
        return list(CertificateFilter(clientId = clientId, limit = limit, offset = offset))
    }

    /** List certificates by kind (AEAT, FNMT, CUSTOM, etc.). */
    fun listByKind(
        kind: String,
        limit: Int = 50,
        offset: Int = 0
    ): CertificateListResponse {
        return list(CertificateFilter(kind = kind, limit = limit, offset = offset))
    }

    /** List active certificates only. */
    fun listActive(
        limit: Int = 50,
        offset: Int = 0
    ): CertificateListResponse {
        return list(CertificateFilter(status = CertificateStatus.ACTIVE, limit = limit, offset = offset))
    }

    /** List expired certificates only. */
    fun listExpired(
        limit: Int = 50,
        offset: Int = 0
    ): CertificateListResponse {
        return list(CertificateFilter(status = CertificateStatus.EXPIRED, limit = limit, offset = offset))
    }

    /** Download certificate as bytes. */
    fun download(id: String, purpose: String): ByteArray {
        val decrypted = decrypt(id, purpose)
        return Base64.getDecoder().decode(decrypted.certificateData)
    }

    // ==================== Private Helpers ====================

    private fun buildQueryParams(filter: CertificateFilter): String {
        val params = mutableListOf<String>()

        filter.clientId?.let { params.add("clientId=${encode(it)}") }
        filter.kind?.let { params.add("kind=${encode(it)}") }
        filter.status?.let { params.add("status=${it.name}") }
        filter.expiringBefore?.let { params.add("expiringBefore=${encode(it.toString())}") }
        filter.tags?.forEach { params.add("tags=${encode(it)}") }
        params.add("limit=${filter.limit}")
        params.add("offset=${filter.offset}")

        return if (params.isNotEmpty()) "?${params.joinToString("&")}" else ""
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8)
}
