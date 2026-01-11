// Path: zn-vault-sdk-jvm/zn-vault-core/src/main/kotlin/com/zincapp/vault/models/Common.kt
package com.zincapp.vault.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Pagination metadata included in paginated responses.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Pagination(
    val total: Int = 0,
    val limit: Int = 50,
    val offset: Int = 0,
    val hasMore: Boolean = false
)

/**
 * Generic paginated response.
 * New standardized format with items array and pagination metadata.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Page<T>(
    val items: List<T> = emptyList(),
    val pagination: Pagination = Pagination()
) {
    /**
     * Whether more items exist beyond this page.
     */
    val hasMore: Boolean get() = pagination.hasMore

    /**
     * Total number of items matching the query.
     */
    val total: Int get() = pagination.total

    /**
     * Number of items per page.
     */
    val limit: Int get() = pagination.limit

    /**
     * Current offset position.
     */
    val offset: Int get() = pagination.offset

    /**
     * Calculate total pages based on limit.
     */
    val totalPages: Int get() = if (pagination.limit > 0) (pagination.total + pagination.limit - 1) / pagination.limit else 0
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
