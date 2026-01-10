// Path: zn-vault-sdk-jvm/zn-vault-core/src/main/kotlin/com/zincapp/vault/admin/PolicyClient.kt
package com.zincapp.vault.admin

import com.fasterxml.jackson.core.type.TypeReference
import com.zincapp.vault.http.ZnVaultHttpClient
import com.zincapp.vault.models.*

/**
 * Client for ABAC policy management operations.
 */
class PolicyClient internal constructor(
    private val httpClient: ZnVaultHttpClient
) {

    /**
     * Create a new ABAC policy.
     *
     * @param request Create policy request
     * @return Created policy
     */
    fun create(request: CreatePolicyRequest): Policy {
        return httpClient.post("/v1/policies", request, Policy::class.java)
    }

    /**
     * Create a simple allow policy.
     *
     * @param name Policy name
     * @param resources List of resource patterns
     * @param actions List of action patterns
     * @param description Optional description
     * @param tenantId Optional tenant ID
     * @return Created policy
     */
    fun createAllowPolicy(
        name: String,
        resources: List<String>,
        actions: List<String>,
        description: String? = null,
        tenantId: String? = null
    ): Policy {
        return create(
            CreatePolicyRequest(
                name = name,
                description = description,
                effect = PolicyEffect.ALLOW,
                resources = resources,
                actions = actions,
                tenantId = tenantId
            )
        )
    }

    /**
     * Create a simple deny policy.
     *
     * @param name Policy name
     * @param resources List of resource patterns
     * @param actions List of action patterns
     * @param description Optional description
     * @param tenantId Optional tenant ID
     * @return Created policy
     */
    fun createDenyPolicy(
        name: String,
        resources: List<String>,
        actions: List<String>,
        description: String? = null,
        tenantId: String? = null
    ): Policy {
        return create(
            CreatePolicyRequest(
                name = name,
                description = description,
                effect = PolicyEffect.DENY,
                resources = resources,
                actions = actions,
                tenantId = tenantId
            )
        )
    }

    /**
     * Get policy by ID.
     *
     * @param id Policy ID
     * @return Policy details
     */
    fun get(id: String): Policy {
        return httpClient.get("/v1/policies/$id", Policy::class.java)
    }

    /**
     * List policies with optional filtering.
     *
     * @param filter Filter criteria
     * @return Page of policies
     */
    fun list(filter: PolicyFilter = PolicyFilter()): Page<Policy> {
        val params = buildFilterParams(filter)
        return httpClient.get("/v1/policies$params",
            object : TypeReference<Page<Policy>>() {})
    }

    /**
     * Update a policy.
     *
     * @param id Policy ID
     * @param request Update request
     * @return Updated policy
     */
    fun update(id: String, request: UpdatePolicyRequest): Policy {
        return httpClient.patch("/v1/policies/$id", request, Policy::class.java)
    }

    /**
     * Delete a policy.
     *
     * @param id Policy ID
     */
    fun delete(id: String) {
        httpClient.delete("/v1/policies/$id")
    }

    /**
     * Enable a policy.
     *
     * @param id Policy ID
     * @return Updated policy
     */
    fun enable(id: String): Policy {
        return httpClient.post("/v1/policies/$id/toggle", mapOf("enabled" to true), Policy::class.java)
    }

    /**
     * Disable a policy.
     *
     * @param id Policy ID
     * @return Updated policy
     */
    fun disable(id: String): Policy {
        return httpClient.post("/v1/policies/$id/toggle", mapOf("enabled" to false), Policy::class.java)
    }

    // ==================== Policy Attachment ====================

    /**
     * Attach a policy to a user.
     *
     * @param policyId Policy ID
     * @param userId User ID
     */
    fun attachToUser(policyId: String, userId: String) {
        val request = AttachPolicyRequest(PrincipalType.USER, userId)
        httpClient.post("/v1/policies/$policyId/attach", request, SuccessResponse::class.java)
    }

    /**
     * Attach a policy to a role.
     *
     * @param policyId Policy ID
     * @param roleId Role ID
     */
    fun attachToRole(policyId: String, roleId: String) {
        val request = AttachPolicyRequest(PrincipalType.ROLE, roleId)
        httpClient.post("/v1/policies/$policyId/attach", request, SuccessResponse::class.java)
    }

    /**
     * Detach a policy from a user.
     *
     * @param policyId Policy ID
     * @param userId User ID
     */
    fun detachFromUser(policyId: String, userId: String) {
        val request = DetachPolicyRequest(PrincipalType.USER, userId)
        httpClient.post("/v1/policies/$policyId/detach", request, SuccessResponse::class.java)
    }

    /**
     * Detach a policy from a role.
     *
     * @param policyId Policy ID
     * @param roleId Role ID
     */
    fun detachFromRole(policyId: String, roleId: String) {
        val request = DetachPolicyRequest(PrincipalType.ROLE, roleId)
        httpClient.post("/v1/policies/$policyId/detach", request, SuccessResponse::class.java)
    }

    private fun buildFilterParams(filter: PolicyFilter): String {
        val params = mutableListOf<String>()

        filter.tenantId?.let { params.add("tenantId=$it") }
        filter.enabled?.let { params.add("enabled=$it") }
        filter.effect?.let { params.add("effect=${it.name.lowercase()}") }
        params.add("limit=${filter.limit}")
        params.add("offset=${filter.offset}")

        return if (params.isNotEmpty()) "?${params.joinToString("&")}" else ""
    }
}
