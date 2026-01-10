// Path: zn-vault-sdk-jvm/zn-vault-core/src/main/kotlin/com/zincapp/vault/audit/AuditClient.kt
package com.zincapp.vault.audit

import com.fasterxml.jackson.core.type.TypeReference
import com.zincapp.vault.http.ZnVaultHttpClient
import com.zincapp.vault.models.*
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Client for audit log operations.
 */
class AuditClient internal constructor(
    private val httpClient: ZnVaultHttpClient
) {

    /**
     * List audit log entries with optional filtering.
     *
     * @param filter Filter criteria
     * @return Page of audit entries
     */
    fun list(filter: AuditFilter = AuditFilter()): AuditPage {
        val params = buildFilterParams(filter)
        return httpClient.get("/v1/audit$params", AuditPage::class.java)
    }

    /**
     * Get audit entries for a specific time range.
     *
     * @param startDate Start of range
     * @param endDate End of range
     * @param limit Maximum entries to return
     * @return List of audit entries
     */
    fun getByDateRange(
        startDate: Instant,
        endDate: Instant,
        limit: Int = 100
    ): List<AuditEntry> {
        return list(
            AuditFilter(
                startDate = startDate,
                endDate = endDate,
                limit = limit
            )
        ).items
    }

    /**
     * Get audit entries for a specific client (user).
     *
     * @param client Client/username
     * @param limit Maximum entries to return
     * @return List of audit entries
     */
    fun getByClient(client: String, limit: Int = 100): List<AuditEntry> {
        return list(AuditFilter(client = client, limit = limit)).items
    }

    /**
     * Get audit entries for a specific action.
     *
     * @param action Action name
     * @param limit Maximum entries to return
     * @return List of audit entries
     */
    fun getByAction(action: String, limit: Int = 100): List<AuditEntry> {
        return list(AuditFilter(action = action, limit = limit)).items
    }

    /**
     * Get audit entries for a specific resource.
     *
     * @param resourceType Resource type (e.g., "secret", "kms_key")
     * @param resourceId Resource ID
     * @param limit Maximum entries to return
     * @return List of audit entries
     */
    fun getByResource(
        resourceType: String,
        resourceId: String,
        limit: Int = 100
    ): List<AuditEntry> {
        return list(
            AuditFilter(
                resourceType = resourceType,
                resourceId = resourceId,
                limit = limit
            )
        ).items
    }

    /**
     * Get audit statistics.
     *
     * @param period Statistics period (hour, day, week, month)
     * @return Audit statistics
     */
    fun getStats(period: StatsPeriod = StatsPeriod.DAY): AuditStats {
        return httpClient.get(
            "/v1/audit/stats?period=${period.name.lowercase()}",
            AuditStats::class.java
        )
    }

    /**
     * Export audit logs.
     *
     * @param filter Filter criteria
     * @param format Export format (JSON or CSV)
     * @return Exported data as string
     */
    fun export(
        filter: AuditFilter = AuditFilter(),
        format: ExportFormat = ExportFormat.JSON
    ): String {
        val params = buildFilterParams(filter) +
            (if (filter.toString().contains("?")) "&" else "?") +
            "format=${format.name.lowercase()}"

        // For export, we need raw string response (not JSON deserialized)
        return httpClient.getRaw("/v1/audit/export$params")
    }

    /**
     * Verify audit chain integrity.
     *
     * This verifies that the HMAC chain of audit entries is intact,
     * detecting any tampering, deletion, or reordering of entries.
     *
     * @return Verification result
     */
    fun verify(): AuditVerifyResult {
        return httpClient.get("/v1/audit/verify", AuditVerifyResult::class.java)
    }

    /**
     * Check if audit chain is valid.
     *
     * @return true if chain is valid, false otherwise
     */
    fun isChainValid(): Boolean {
        return verify().valid
    }

    private fun buildFilterParams(filter: AuditFilter): String {
        val params = mutableListOf<String>()

        filter.startDate?.let {
            params.add("start_date=${formatInstant(it)}")
        }
        filter.endDate?.let {
            params.add("end_date=${formatInstant(it)}")
        }
        filter.client?.let { params.add("client=${encode(it)}") }
        filter.action?.let { params.add("action=${encode(it)}") }
        filter.tenantId?.let { params.add("tenant_id=${encode(it)}") }
        filter.resourceType?.let { params.add("resource_type=${encode(it)}") }
        filter.resourceId?.let { params.add("resource_id=${encode(it)}") }
        filter.status?.let { params.add("status=${it.name.lowercase()}") }
        params.add("limit=${filter.limit}")
        params.add("offset=${filter.offset}")

        return if (params.isNotEmpty()) "?${params.joinToString("&")}" else ""
    }

    private fun formatInstant(instant: Instant): String =
        DateTimeFormatter.ISO_INSTANT.format(instant)

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8)
}
