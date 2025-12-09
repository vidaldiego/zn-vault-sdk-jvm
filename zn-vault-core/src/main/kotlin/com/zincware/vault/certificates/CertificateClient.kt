// Path: zn-vault-sdk-jvm/zn-vault-core/src/main/kotlin/com/zincware/vault/certificates/CertificateClient.kt
package com.zincware.vault.certificates

import com.fasterxml.jackson.core.type.TypeReference
import com.zincware.vault.http.ZnVaultHttpClient
import com.zincware.vault.models.*
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.*

/**
 * Client for certificate lifecycle management operations.
 */
class CertificateClient internal constructor(
    private val httpClient: ZnVaultHttpClient
) {

    // ==================== CRUD Operations ====================

    /**
     * Store a new certificate for custody.
     *
     * @param request The certificate storage request.
     * @param tenantId Optional tenant ID (required if not in JWT).
     * @return The certificate metadata.
     */
    fun store(request: StoreCertificateRequest, tenantId: String? = null): Certificate {
        val params = if (tenantId != null) "?tenantId=$tenantId" else ""
        return httpClient.post("/v1/certificates$params", request, Certificate::class.java)
    }

    /**
     * Get certificate metadata by ID.
     *
     * @param id The certificate ID.
     * @param tenantId Optional tenant ID (required if not in JWT).
     * @return The certificate metadata.
     */
    fun get(id: String, tenantId: String? = null): Certificate {
        val params = if (tenantId != null) "?tenantId=$tenantId" else ""
        return httpClient.get("/v1/certificates/$id$params", Certificate::class.java)
    }

    /**
     * Get certificate by business identity (clientId/kind/alias).
     *
     * @param clientId External customer identifier (e.g., NIF/CIF).
     * @param kind Certificate kind (AEAT, FNMT, CUSTOM, etc.).
     * @param alias Human-readable identifier.
     * @param tenantId Optional tenant ID (required if not in JWT).
     * @return The certificate metadata.
     */
    fun getByIdentity(
        clientId: String,
        kind: String,
        alias: String,
        tenantId: String? = null
    ): Certificate {
        val encodedClientId = encode(clientId)
        val encodedKind = encode(kind)
        val encodedAlias = encode(alias)
        val params = if (tenantId != null) "?tenantId=$tenantId" else ""
        return httpClient.get(
            "/v1/certificates/by-identity/$encodedClientId/$encodedKind/$encodedAlias$params",
            Certificate::class.java
        )
    }

    /**
     * List certificates with optional filtering.
     *
     * @param filter Optional filter parameters.
     * @param tenantId Optional tenant ID (required if not in JWT).
     * @return Paginated list of certificates.
     */
    fun list(filter: CertificateFilter = CertificateFilter(), tenantId: String? = null): CertificateListResponse {
        val params = buildQueryParams(filter, tenantId)
        return httpClient.get("/v1/certificates$params", CertificateListResponse::class.java)
    }

    /**
     * Get certificate statistics.
     *
     * @param tenantId Optional tenant ID (required if not in JWT).
     * @return Statistics including counts by status and kind.
     */
    fun getStats(tenantId: String? = null): CertificateStats {
        val params = if (tenantId != null) "?tenantId=$tenantId" else ""
        return httpClient.get("/v1/certificates/stats$params", CertificateStats::class.java)
    }

    /**
     * List certificates expiring within a specified number of days.
     *
     * @param days Number of days to look ahead (default: 30).
     * @param tenantId Optional tenant ID (required if not in JWT).
     * @return List of expiring certificates.
     */
    fun listExpiring(days: Int = 30, tenantId: String? = null): List<Certificate> {
        val params = buildString {
            append("?days=$days")
            if (tenantId != null) append("&tenantId=$tenantId")
        }
        return httpClient.get("/v1/certificates/expiring$params",
            object : TypeReference<List<Certificate>>() {})
    }

    /**
     * Update certificate metadata.
     *
     * @param id The certificate ID.
     * @param request The update request.
     * @param tenantId Optional tenant ID (required if not in JWT).
     * @return The updated certificate metadata.
     */
    fun update(id: String, request: UpdateCertificateRequest, tenantId: String? = null): Certificate {
        val params = if (tenantId != null) "?tenantId=$tenantId" else ""
        return httpClient.patch("/v1/certificates/$id$params", request, Certificate::class.java)
    }

    /**
     * Decrypt certificate (retrieve the actual certificate data).
     * Requires business justification - the purpose is logged for audit.
     *
     * @param id The certificate ID.
     * @param purpose Business justification for accessing the certificate.
     * @param tenantId Optional tenant ID (required if not in JWT).
     * @return Decrypted certificate data (base64 encoded).
     */
    fun decrypt(id: String, purpose: String, tenantId: String? = null): DecryptedCertificate {
        val params = if (tenantId != null) "?tenantId=$tenantId" else ""
        val request = DecryptCertificateRequest(purpose)
        return httpClient.post("/v1/certificates/$id/decrypt$params", request, DecryptedCertificate::class.java)
    }

    /**
     * Rotate certificate (replace with a new certificate).
     * The old certificate is preserved in history.
     *
     * @param id The certificate ID.
     * @param request The rotation request with new certificate data.
     * @param tenantId Optional tenant ID (required if not in JWT).
     * @return The updated certificate metadata.
     */
    fun rotate(id: String, request: RotateCertificateRequest, tenantId: String? = null): Certificate {
        val params = if (tenantId != null) "?tenantId=$tenantId" else ""
        return httpClient.post("/v1/certificates/$id/rotate$params", request, Certificate::class.java)
    }

    /**
     * Delete a certificate.
     * The underlying secret data is preserved for audit purposes.
     *
     * @param id The certificate ID.
     * @param tenantId Optional tenant ID (required if not in JWT).
     */
    fun delete(id: String, tenantId: String? = null) {
        val params = if (tenantId != null) "?tenantId=$tenantId" else ""
        httpClient.delete("/v1/certificates/$id$params")
    }

    /**
     * Get certificate access log.
     *
     * @param id The certificate ID.
     * @param limit Maximum number of entries to return (default: 100).
     * @param tenantId Optional tenant ID (required if not in JWT).
     * @return Access log entries.
     */
    fun getAccessLog(id: String, limit: Int = 100, tenantId: String? = null): List<CertificateAccessLogEntry> {
        val params = buildString {
            append("?limit=$limit")
            if (tenantId != null) append("&tenantId=$tenantId")
        }
        val response = httpClient.get("/v1/certificates/$id/access-log$params", CertificateAccessLogResponse::class.java)
        return response.entries
    }

    // ==================== Convenience Methods ====================

    /**
     * Store a P12 certificate with simplified parameters.
     *
     * @param clientId External customer identifier (e.g., NIF/CIF).
     * @param kind Certificate kind (AEAT, FNMT, CUSTOM, etc.).
     * @param alias Human-readable identifier.
     * @param p12Data P12 certificate data.
     * @param passphrase P12 passphrase.
     * @param purpose Certificate purpose.
     * @param clientName Optional customer display name.
     * @param organizationId Optional organization identifier.
     * @param contactEmail Optional contact email.
     * @param tags Optional tags.
     * @param metadata Optional custom metadata.
     * @param tenantId Optional tenant ID (required if not in JWT).
     * @return The certificate metadata.
     */
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
        metadata: Map<String, Any> = emptyMap(),
        tenantId: String? = null
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
        return store(request, tenantId)
    }

    /**
     * Store a PEM certificate with simplified parameters.
     *
     * @param clientId External customer identifier (e.g., NIF/CIF).
     * @param kind Certificate kind.
     * @param alias Human-readable identifier.
     * @param pemData PEM certificate data.
     * @param purpose Certificate purpose.
     * @param clientName Optional customer display name.
     * @param organizationId Optional organization identifier.
     * @param contactEmail Optional contact email.
     * @param tags Optional tags.
     * @param metadata Optional custom metadata.
     * @param tenantId Optional tenant ID (required if not in JWT).
     * @return The certificate metadata.
     */
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
        metadata: Map<String, Any> = emptyMap(),
        tenantId: String? = null
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
        return store(request, tenantId)
    }

    /**
     * List certificates by client ID.
     *
     * @param clientId The client ID to filter by.
     * @param tenantId Optional tenant ID (required if not in JWT).
     * @param page Page number.
     * @param pageSize Page size.
     * @return Paginated list of certificates.
     */
    fun listByClient(
        clientId: String,
        tenantId: String? = null,
        page: Int = 1,
        pageSize: Int = 20
    ): CertificateListResponse {
        return list(CertificateFilter(clientId = clientId, page = page, pageSize = pageSize), tenantId)
    }

    /**
     * List certificates by kind (AEAT, FNMT, CUSTOM, etc.).
     *
     * @param kind The kind to filter by.
     * @param tenantId Optional tenant ID (required if not in JWT).
     * @param page Page number.
     * @param pageSize Page size.
     * @return Paginated list of certificates.
     */
    fun listByKind(
        kind: String,
        tenantId: String? = null,
        page: Int = 1,
        pageSize: Int = 20
    ): CertificateListResponse {
        return list(CertificateFilter(kind = kind, page = page, pageSize = pageSize), tenantId)
    }

    /**
     * List active certificates only.
     *
     * @param tenantId Optional tenant ID (required if not in JWT).
     * @param page Page number.
     * @param pageSize Page size.
     * @return Paginated list of active certificates.
     */
    fun listActive(
        tenantId: String? = null,
        page: Int = 1,
        pageSize: Int = 20
    ): CertificateListResponse {
        return list(CertificateFilter(status = CertificateStatus.ACTIVE, page = page, pageSize = pageSize), tenantId)
    }

    /**
     * List expired certificates only.
     *
     * @param tenantId Optional tenant ID (required if not in JWT).
     * @param page Page number.
     * @param pageSize Page size.
     * @return Paginated list of expired certificates.
     */
    fun listExpired(
        tenantId: String? = null,
        page: Int = 1,
        pageSize: Int = 20
    ): CertificateListResponse {
        return list(CertificateFilter(status = CertificateStatus.EXPIRED, page = page, pageSize = pageSize), tenantId)
    }

    /**
     * Download certificate as bytes.
     *
     * @param id The certificate ID.
     * @param purpose Business justification.
     * @param tenantId Optional tenant ID.
     * @return Certificate data as bytes.
     */
    fun download(id: String, purpose: String, tenantId: String? = null): ByteArray {
        val decrypted = decrypt(id, purpose, tenantId)
        return Base64.getDecoder().decode(decrypted.certificateData)
    }

    // ==================== Private Helpers ====================

    private fun buildQueryParams(filter: CertificateFilter, tenantId: String?): String {
        val params = mutableListOf<String>()

        filter.clientId?.let { params.add("clientId=${encode(it)}") }
        filter.kind?.let { params.add("kind=${encode(it)}") }
        filter.status?.let { params.add("status=${it.name}") }
        filter.expiringBefore?.let { params.add("expiringBefore=${encode(it.toString())}") }
        filter.tags?.forEach { params.add("tags=${encode(it)}") }
        params.add("page=${filter.page}")
        params.add("pageSize=${filter.pageSize}")
        tenantId?.let { params.add("tenantId=$it") }

        return if (params.isNotEmpty()) "?${params.joinToString("&")}" else ""
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8)
}
