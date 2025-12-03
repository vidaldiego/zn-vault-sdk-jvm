// Path: zn-vault-sdk-jvm/zn-vault-core/src/main/kotlin/com/zincware/vault/admin/RoleClient.kt
package com.zincware.vault.admin

import com.fasterxml.jackson.core.type.TypeReference
import com.zincware.vault.http.ZnVaultHttpClient
import com.zincware.vault.models.*

/**
 * Client for RBAC role management operations.
 */
class RoleClient internal constructor(
    private val httpClient: ZnVaultHttpClient
) {

    /**
     * Create a new custom role.
     *
     * @param request Create role request
     * @return Created role
     */
    fun create(request: CreateRoleRequest): Role {
        return httpClient.post("/v1/roles", request, Role::class.java)
    }

    /**
     * Create a new custom role with simplified parameters.
     *
     * @param name Role name
     * @param permissions List of permissions
     * @param description Optional description
     * @param tenantId Optional tenant ID
     * @return Created role
     */
    fun create(
        name: String,
        permissions: List<String>,
        description: String? = null,
        tenantId: String? = null
    ): Role {
        return create(CreateRoleRequest(name, description, permissions, tenantId))
    }

    /**
     * Get role by ID.
     *
     * @param id Role ID
     * @return Role details
     */
    fun get(id: String): Role {
        return httpClient.get("/v1/roles/$id", Role::class.java)
    }

    /**
     * List roles with optional filtering.
     *
     * @param filter Filter criteria
     * @return Page of roles
     */
    fun list(filter: RoleFilter = RoleFilter()): Page<Role> {
        val params = buildFilterParams(filter)
        return httpClient.get("/v1/roles$params",
            object : TypeReference<Page<Role>>() {})
    }

    /**
     * Update a role.
     *
     * @param id Role ID
     * @param request Update request
     * @return Updated role
     */
    fun update(id: String, request: UpdateRoleRequest): Role {
        return httpClient.patch("/v1/roles/$id", request, Role::class.java)
    }

    /**
     * Delete a role.
     *
     * @param id Role ID
     */
    fun delete(id: String) {
        httpClient.delete("/v1/roles/$id")
    }

    // ==================== User Role Assignment ====================

    /**
     * Assign a role to a user.
     *
     * @param userId User ID
     * @param roleId Role ID
     */
    fun assignToUser(userId: String, roleId: String) {
        val request = AssignRoleRequest(roleId)
        httpClient.post("/v1/users/$userId/roles", request, SuccessResponse::class.java)
    }

    /**
     * Remove a role from a user.
     *
     * @param userId User ID
     * @param roleId Role ID
     */
    fun removeFromUser(userId: String, roleId: String) {
        httpClient.delete("/v1/users/$userId/roles/$roleId")
    }

    /**
     * Get roles assigned to a user.
     *
     * @param userId User ID
     * @return List of roles
     */
    fun getUserRoles(userId: String): List<Role> {
        val response = httpClient.get("/v1/users/$userId/roles",
            object : TypeReference<ApiResponse<List<Role>>>() {})
        return response.data ?: emptyList()
    }

    /**
     * Get effective permissions for a user.
     *
     * @param userId User ID
     * @return List of permission names
     */
    fun getUserPermissions(userId: String): List<String> {
        val response = httpClient.get("/v1/users/$userId/permissions",
            object : TypeReference<ApiResponse<List<String>>>() {})
        return response.data ?: emptyList()
    }

    // ==================== Permissions ====================

    /**
     * List all available permissions.
     *
     * @return List of permissions
     */
    fun listPermissions(): List<Permission> {
        val response = httpClient.get("/v1/permissions",
            object : TypeReference<ApiResponse<List<Permission>>>() {})
        return response.data ?: emptyList()
    }

    private fun buildFilterParams(filter: RoleFilter): String {
        val params = mutableListOf<String>()

        filter.tenantId?.let { params.add("tenantId=$it") }
        if (filter.includeSystem) params.add("includeSystem=true")
        params.add("limit=${filter.limit}")
        params.add("offset=${filter.offset}")

        return if (params.isNotEmpty()) "?${params.joinToString("&")}" else ""
    }
}
