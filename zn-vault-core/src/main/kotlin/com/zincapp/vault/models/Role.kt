// Path: zn-vault-sdk-jvm/zn-vault-core/src/main/kotlin/com/zincapp/vault/models/Role.kt
package com.zincapp.vault.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

/**
 * Represents an RBAC role in ZnVault.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Role(
    val id: String,
    val name: String,
    val description: String? = null,
    val permissions: List<String> = emptyList(),
    @JsonProperty("is_system") val isSystem: Boolean = false,
    val tenantId: String? = null,
    @JsonProperty("created_at") val createdAt: Instant? = null,
    @JsonProperty("updated_at") val updatedAt: Instant? = null
)

/**
 * Request to create a custom role.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreateRoleRequest(
    val name: String,
    val description: String? = null,
    val permissions: List<String>,
    val tenantId: String? = null
)

/**
 * Request to update a role.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class UpdateRoleRequest(
    val name: String? = null,
    val description: String? = null,
    val permissions: List<String>? = null
)

/**
 * Request to assign a role to a user.
 */
data class AssignRoleRequest(
    val roleId: String
)

/**
 * Filter for listing roles.
 */
data class RoleFilter(
    val tenantId: String? = null,
    val includeSystem: Boolean = true,
    val limit: Int = 100,
    val offset: Int = 0
)

/**
 * Available permission in the system.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Permission(
    val id: String,
    val name: String,
    val description: String? = null,
    val category: String? = null
)
