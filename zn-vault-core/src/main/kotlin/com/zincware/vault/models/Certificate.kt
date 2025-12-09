// Path: zn-vault-sdk-jvm/zn-vault-core/src/main/kotlin/com/zincware/vault/models/Certificate.kt
package com.zincware.vault.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

/**
 * Certificate format types.
 */
enum class CertificateType {
    @JsonProperty("P12") P12,
    @JsonProperty("PEM") PEM,
    @JsonProperty("DER") DER
}

/**
 * Certificate purpose/usage.
 */
enum class CertificatePurpose {
    @JsonProperty("TLS") TLS,
    @JsonProperty("mTLS") MTLS,
    @JsonProperty("SIGNING") SIGNING,
    @JsonProperty("ENCRYPTION") ENCRYPTION,
    @JsonProperty("AUTHENTICATION") AUTHENTICATION
}

/**
 * Certificate lifecycle status.
 */
enum class CertificateStatus {
    @JsonProperty("ACTIVE") ACTIVE,
    @JsonProperty("EXPIRED") EXPIRED,
    @JsonProperty("REVOKED") REVOKED,
    @JsonProperty("SUSPENDED") SUSPENDED,
    @JsonProperty("PENDING_DELETION") PENDING_DELETION
}

/**
 * Certificate kind/category.
 */
enum class CertificateKind {
    @JsonProperty("AEAT") AEAT,
    @JsonProperty("FNMT") FNMT,
    @JsonProperty("CAMERFIRMA") CAMERFIRMA,
    @JsonProperty("CUSTOM") CUSTOM
}

/**
 * Certificate metadata (without encrypted data).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Certificate(
    val id: String,
    val tenantId: String,
    val clientId: String,
    val kind: String,
    val alias: String,
    val certificateType: CertificateType,
    val purpose: CertificatePurpose,
    val fingerprintSha256: String,
    val subjectCn: String,
    val issuerCn: String,
    val notBefore: Instant,
    val notAfter: Instant,
    val clientName: String,
    val organizationId: String? = null,
    val contactEmail: String? = null,
    val status: CertificateStatus,
    val version: Int,
    val createdAt: Instant,
    val createdBy: String,
    val updatedAt: Instant,
    val lastAccessedAt: Instant? = null,
    val accessCount: Int,
    val tags: List<String> = emptyList(),
    val daysUntilExpiry: Int,
    val isExpired: Boolean
)

/**
 * Decrypted certificate response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class DecryptedCertificate(
    val id: String,
    val certificateData: String,
    val certificateType: CertificateType,
    val fingerprintSha256: String
)

/**
 * Request to store a new certificate.
 */
data class StoreCertificateRequest(
    val clientId: String,
    val kind: String,
    val alias: String,
    val certificateData: String,
    val certificateType: CertificateType,
    val purpose: CertificatePurpose,
    val passphrase: String? = null,
    val clientName: String? = null,
    val organizationId: String? = null,
    val contactEmail: String? = null,
    val tags: List<String> = emptyList(),
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Request to update certificate metadata.
 */
data class UpdateCertificateRequest(
    val alias: String? = null,
    val clientName: String? = null,
    val contactEmail: String? = null,
    val tags: List<String>? = null,
    val metadata: Map<String, Any>? = null
)

/**
 * Request to rotate a certificate.
 */
data class RotateCertificateRequest(
    val certificateData: String,
    val certificateType: CertificateType,
    val passphrase: String? = null,
    val reason: String? = null
)

/**
 * Request to decrypt a certificate.
 */
internal data class DecryptCertificateRequest(
    val purpose: String
)

/**
 * Filter options for listing certificates.
 */
data class CertificateFilter(
    val clientId: String? = null,
    val kind: String? = null,
    val status: CertificateStatus? = null,
    val expiringBefore: Instant? = null,
    val tags: List<String>? = null,
    val page: Int = 1,
    val pageSize: Int = 20
)

/**
 * Certificate statistics.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class CertificateStats(
    val total: Int,
    val byStatus: Map<String, Int> = emptyMap(),
    val byKind: Map<String, Int> = emptyMap(),
    val expiringIn30Days: Int = 0,
    val expiringIn7Days: Int = 0
)

/**
 * Certificate access log entry.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class CertificateAccessLogEntry(
    val id: Int,
    val certificateId: String,
    val tenantId: String,
    val userId: String? = null,
    val apiKeyId: String? = null,
    val purpose: String,
    val operation: String,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val accessedAt: Instant,
    val success: Boolean,
    val errorMessage: String? = null
)

/**
 * Paginated list response for certificates.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class CertificateListResponse(
    val items: List<Certificate> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 20
)

/**
 * Certificate access log response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class CertificateAccessLogResponse(
    val entries: List<CertificateAccessLogEntry> = emptyList()
)
