// Path: zn-vault-sdk-jvm/zn-vault-core/src/main/kotlin/com/zincapp/vault/models/Audit.kt
package com.zincapp.vault.models

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
 * Page response for audit entries.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class AuditPage(
    val items: List<AuditEntry> = emptyList(),
    val pagination: Pagination = Pagination()
) {
    val hasMore: Boolean get() = pagination.hasMore
    val total: Int get() = pagination.total
}

/**
 * Audit entry status.
 */
enum class AuditStatus {
    @JsonProperty("success") SUCCESS,
    @JsonProperty("failure") FAILURE
}

/**
 * Filter for listing audit logs (tenant-scoped).
 *
 * The tenant is derived from the authenticated principal. Use
 * [SuperadminAuditFilter] (via the superadmin client) for cross-tenant queries.
 */
data class AuditFilter(
    @JsonProperty("start_date") val startDate: Instant? = null,
    @JsonProperty("end_date") val endDate: Instant? = null,
    val client: String? = null,
    val action: String? = null,
    @JsonProperty("resource_type") val resourceType: String? = null,
    @JsonProperty("resource_id") val resourceId: String? = null,
    val status: AuditStatus? = null,
    val limit: Int = 50,
    val offset: Int = 0
)

/**
 * Filter for listing audit logs across tenants (superadmin only).
 *
 * Mirrors [AuditFilter] but adds [tenantId] to scope the query to a specific
 * tenant. Required at the route level by /v1/superadmin/audit.
 */
data class SuperadminAuditFilter(
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
data class AuditVerificationInterval(val firstId: Long? = null, val lastId: Long? = null)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AuditVerifyResult(
    var valid: Boolean = false,
    val status: String? = null,
    val reason: String? = null,
    val reasons: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
    val checkedEntries: Long = 0,
    val readEntries: Long = 0,
    val authenticatedEntries: Long = 0,
    val unverifiedEntries: Long = 0,
    val unsignedDetailsEntries: Long? = null,
    val scope: String? = null,
    val interval: AuditVerificationInterval? = null,
    val completed: Boolean = false,
    val startedAt: String? = null,
    val finishedAt: String? = null,
    val lastVerified: String? = null,
    val consistency: String? = null,
    val historicalCompleteness: String? = null,
    val brokenAt: Long? = null,
    val message: String? = null
) {
    val entriesVerified: Long get() = authenticatedEntries
    val firstEntryId: Long? get() = interval?.firstId
    val lastEntryId: Long? get() = interval?.lastId
    init {
        valid = valid && status == "verified" && scope == "retained" && completed && readEntries > 0
            && authenticatedEntries == readEntries && unverifiedEntries == 0L && unsignedDetailsEntries == 0L
    }
}

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
