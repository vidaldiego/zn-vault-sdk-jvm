// Path: zn-vault-sdk-jvm/zn-vault-core/src/main/kotlin/com/zincware/vault/models/Audit.kt
package com.zincware.vault.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

/**
 * Represents an audit log entry.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class AuditEntry(
    val id: String,
    val timestamp: Instant? = null,
    val action: String,
    val resource: String? = null,
    val actor: String? = null,
    @JsonProperty("client_cert") val clientCert: String? = null,
    val result: String? = null,
    val ip: String? = null,
    val tenantId: String? = null,
    val details: Map<String, Any>? = null
)

/**
 * Page response for audit entries (uses 'entries' instead of 'data').
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class AuditPage(
    val entries: List<AuditEntry> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 50
) {
    val items: List<AuditEntry> get() = entries
    val hasMore: Boolean get() = page * pageSize < total
}

/**
 * Audit entry status.
 */
enum class AuditStatus {
    @JsonProperty("success") SUCCESS,
    @JsonProperty("failure") FAILURE
}

/**
 * Filter for listing audit logs.
 */
data class AuditFilter(
    @JsonProperty("start_date") val startDate: Instant? = null,
    @JsonProperty("end_date") val endDate: Instant? = null,
    val client: String? = null,
    val action: String? = null,
    @JsonProperty("tenant_id") val tenantId: String? = null,
    @JsonProperty("resource_type") val resourceType: String? = null,
    @JsonProperty("resource_id") val resourceId: String? = null,
    val status: AuditStatus? = null,
    val limit: Int = 50,
    val offset: Int = 0
)

/**
 * Audit statistics.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class AuditStats(
    val totalEvents: Int = 0,
    val successRate: Double = 0.0,
    val topActors: List<ActorCount> = emptyList(),
    val topActions: List<ActionCount> = emptyList(),
    val recentFailures: List<RecentFailure> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ActorCount(
    val actor: String,
    val count: Int
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ActionCount(
    val action: String,
    val count: Int
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RecentFailure(
    val timestamp: Instant? = null,
    val action: String,
    val actor: String? = null,
    val reason: String? = null
)

/**
 * Statistics period.
 */
enum class StatsPeriod {
    @JsonProperty("hour") HOUR,
    @JsonProperty("day") DAY,
    @JsonProperty("week") WEEK,
    @JsonProperty("month") MONTH
}

/**
 * Audit chain verification result.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class AuditVerifyResult(
    val valid: Boolean,
    @JsonProperty("entries_verified") val entriesVerified: Long,
    @JsonProperty("first_entry_id") val firstEntryId: Long? = null,
    @JsonProperty("last_entry_id") val lastEntryId: Long? = null,
    @JsonProperty("broken_at") val brokenAt: Long? = null,
    val message: String? = null
)

/**
 * Audit export format.
 */
enum class ExportFormat {
    JSON,
    CSV
}

/**
 * Audit export request parameters.
 */
data class AuditExportParams(
    val filter: AuditFilter = AuditFilter(),
    val format: ExportFormat = ExportFormat.JSON
)
