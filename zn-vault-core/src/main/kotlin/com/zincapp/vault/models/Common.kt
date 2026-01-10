// Path: zn-vault-sdk-jvm/zn-vault-core/src/main/kotlin/com/zincapp/vault/models/Common.kt
package com.zincapp.vault.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Generic paginated response.
 * Note: API uses "data" field for items array.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Page<T>(
    @JsonProperty("data") val items: List<T> = emptyList(),
    val page: Int = 1,
    val pageSize: Int = 50,
    val total: Int = 0,
    val count: Int = 0,  // Some endpoints use "count" instead of "total"
    @JsonProperty("next_marker") val nextMarker: String? = null,
    val truncated: Boolean = false
) {
    val hasMore: Boolean get() = nextMarker != null || truncated

    /**
     * Total number of items. Uses 'total' field if present, otherwise falls back to 'count'.
     */
    val totalItems: Int get() = if (total > 0) total else count

    val totalPages: Int get() = if (pageSize > 0) (totalItems + pageSize - 1) / pageSize else 0
}

/**
 * Generic API response wrapper.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class ApiResponse<T>(
    val success: Boolean = true,
    val data: T? = null,
    val error: String? = null,
    val message: String? = null
)

/**
 * Error response from the API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class ErrorResponse(
    val error: String,
    val message: String,
    @JsonProperty("status_code") val statusCode: Int,
    val errors: List<String>? = null
)

/**
 * Health check response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class HealthStatus(
    val status: String,
    val version: String? = null,
    val timestamp: String? = null,
    val uptime: Double? = null,
    val environment: String? = null,
    @JsonProperty("totp_required") val totpRequired: Boolean = false,
    val checks: HealthChecks? = null
)

/**
 * Health check components.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class HealthChecks(
    val db: ComponentHealth? = null,
    val tls: ComponentHealth? = null
)

/**
 * Component health status.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class ComponentHealth(
    val status: String,
    val details: Map<String, Any>? = null
)

/**
 * Simple success response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SuccessResponse(
    val success: Boolean = true,
    val message: String? = null
)
