// Path: zn-vault-sdk-jvm/zn-vault-core/src/main/kotlin/com/zincapp/vault/audit/SuperadminAuditClient.kt
package com.zincapp.vault.audit

import com.zincapp.vault.http.ZnVaultHttpClient
import com.zincapp.vault.models.AuditEntry
import com.zincapp.vault.models.AuditPage
import com.zincapp.vault.models.AuditStats
import com.zincapp.vault.models.ExportFormat
import com.zincapp.vault.models.StatsPeriod
import com.zincapp.vault.models.SuperadminAuditFilter
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Cross-tenant audit log operations (superadmin only).
 *
 * Mirrors [AuditClient] but targets `/v1/superadmin/audit*` so superadmins can
 * scope a query to a specific tenant via [SuperadminAuditFilter.tenantId].
 *
 * Only available on [com.zincapp.vault.ZnVaultSuperadminClient].
 */
class SuperadminAuditClient internal constructor(
    private val httpClient: ZnVaultHttpClient
) {

    /**
     * List audit log entries across tenants, optionally scoped to a specific tenant.
     */
    fun list(filter: SuperadminAuditFilter = SuperadminAuditFilter()): AuditPage {
        val params = buildFilterParams(filter)
        return httpClient.get("/v1/superadmin/audit$params", AuditPage::class.java)
    }

    /**
     * Get audit entries for a specific time range across tenants.
     */
    fun getByDateRange(
        startDate: Instant,
        endDate: Instant,
        tenantId: String? = null,
        limit: Int = 100
    ): List<AuditEntry> {
        return list(
            SuperadminAuditFilter(
                startDate = startDate,
                endDate = endDate,
                tenantId = tenantId,
                limit = limit
            )
        ).items
    }

    /**
     * Get audit entries for a specific tenant.
     */
    fun getByTenant(tenantId: String, limit: Int = 100): List<AuditEntry> {
        return list(SuperadminAuditFilter(tenantId = tenantId, limit = limit)).items
    }

    /**
     * Get audit statistics across tenants, optionally scoped to one tenant.
     */
    fun getStats(period: StatsPeriod = StatsPeriod.DAY, tenantId: String? = null): AuditStats {
        val tenantParam = tenantId?.let { "&tenantId=${encode(it)}" } ?: ""
        return httpClient.get(
            "/v1/superadmin/audit/stats?period=${period.name.lowercase()}$tenantParam",
            AuditStats::class.java
        )
    }

    /**
     * Export audit logs (cross-tenant).
     */
    fun export(
        filter: SuperadminAuditFilter = SuperadminAuditFilter(),
        format: ExportFormat = ExportFormat.JSON
    ): String {
        val base = buildFilterParams(filter)
        val sep = if (base.contains("?")) "&" else "?"
        return httpClient.getRaw("/v1/superadmin/audit/export$base${sep}format=${format.name.lowercase()}")
    }

    private fun buildFilterParams(filter: SuperadminAuditFilter): String {
        val params = mutableListOf<String>()

        filter.startDate?.let { params.add("start_date=${formatInstant(it)}") }
        filter.endDate?.let { params.add("end_date=${formatInstant(it)}") }
        filter.client?.let { params.add("client=${encode(it)}") }
        filter.action?.let { params.add("action=${encode(it)}") }
        filter.tenantId?.let { params.add("tenantId=${encode(it)}") }
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
