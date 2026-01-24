// Path: zn-vault-sdk-jvm/zn-vault-core/src/main/kotlin/com/zincapp/vault/admin/TenantClient.kt
package com.zincapp.vault.admin

import com.fasterxml.jackson.core.type.TypeReference
import com.zincapp.vault.http.ZnVaultHttpClient
import com.zincapp.vault.models.*
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Client for tenant management operations.
 */
class TenantClient internal constructor(
    private val httpClient: ZnVaultHttpClient
) {

    /**
     * Create a new tenant.
     *
     * @param request Create tenant request
     * @return Created tenant
     */
    fun create(request: CreateTenantRequest): Tenant {
        return httpClient.post("/v1/tenants", request, Tenant::class.java)
    }

    /**
     * Create a new tenant with simplified parameters.
     *
     * @param id Tenant ID
     * @param name Tenant name
     * @return Created tenant
     */
    fun create(id: String, name: String): Tenant {
        return create(CreateTenantRequest(id, name))
    }

    /**
     * Get tenant by ID.
     *
     * @param id Tenant ID
     * @return Tenant details
     */
    fun get(id: String): Tenant {
        return httpClient.get("/v1/tenants/$id", Tenant::class.java)
    }

    /**
     * List tenants with optional filtering.
     *
     * @param filter Filter criteria
     * @return Page of tenants
     */
    fun list(filter: TenantFilter = TenantFilter()): Page<Tenant> {
        val params = buildFilterParams(filter)
        return httpClient.get("/v1/tenants$params",
            object : TypeReference<Page<Tenant>>() {})
    }

    /**
     * Update a tenant.
     *
     * @param id Tenant ID
     * @param request Update request
     * @return Updated tenant
     */
    fun update(id: String, request: UpdateTenantRequest): Tenant {
        return httpClient.patch("/v1/tenants/$id", request, Tenant::class.java)
    }

    /**
     * Update tenant status.
     *
     * @param id Tenant ID
     * @param status New status
     * @return Updated tenant
     */
    fun updateStatus(id: String, status: TenantStatus): Tenant {
        val request = UpdateTenantStatusRequest(status)
        val response = httpClient.put("/v1/tenants/$id/status", request,
            object : TypeReference<ApiResponse<Tenant>>() {})
        return response.data ?: throw IllegalStateException("No tenant data in response")
    }

    /**
     * Suspend a tenant.
     *
     * @param id Tenant ID
     * @return Updated tenant
     */
    fun suspend(id: String): Tenant {
        return updateStatus(id, TenantStatus.SUSPENDED)
    }

    /**
     * Activate a tenant.
     *
     * @param id Tenant ID
     * @return Updated tenant
     */
    fun activate(id: String): Tenant {
        return updateStatus(id, TenantStatus.ACTIVE)
    }

    /**
     * Archive (soft delete) a tenant.
     *
     * @param id Tenant ID
     */
    fun archive(id: String) {
        httpClient.delete("/v1/tenants/$id")
    }

    /**
     * Get tenant usage statistics.
     *
     * @param id Tenant ID
     * @return Tenant usage
     */
    fun getUsage(id: String): TenantUsage {
        val response = httpClient.get("/v1/tenants/$id/usage",
            object : TypeReference<ApiResponse<TenantUsage>>() {})
        return response.data ?: TenantUsage()
    }

    /**
     * Check quota for a resource type.
     *
     * @param id Tenant ID
     * @param resourceType Resource type (secrets, kms_keys, users, api_keys)
     * @return Quota check result
     */
    fun checkQuota(id: String, resourceType: String): QuotaCheckResult {
        return httpClient.get(
            "/v1/tenants/$id/quota/$resourceType",
            QuotaCheckResult::class.java
        )
    }

    private fun buildFilterParams(filter: TenantFilter): String {
        val params = mutableListOf<String>()

        filter.status?.let { params.add("status=${it.name.lowercase()}") }
        if (filter.includeUsage) params.add("include_usage=true")
        params.add("limit=${filter.limit}")
        params.add("offset=${filter.offset}")

        return if (params.isNotEmpty()) "?${params.joinToString("&")}" else ""
    }
}
