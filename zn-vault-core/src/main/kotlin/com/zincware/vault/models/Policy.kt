// Path: zn-vault-sdk-jvm/zn-vault-core/src/main/kotlin/com/zincware/vault/models/Policy.kt
package com.zincware.vault.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

/**
 * Represents an ABAC policy in ZN-Vault.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Policy(
    val id: String,
    val name: String,
    val description: String? = null,
    val effect: PolicyEffect,
    val resources: List<String>,
    val actions: List<String>,
    val conditions: PolicyConditions? = null,
    val enabled: Boolean = true,
    val priority: Int = 0,
    val tenantId: String? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null
)

/**
 * Policy effect (allow or deny).
 */
enum class PolicyEffect {
    @JsonProperty("allow") ALLOW,
    @JsonProperty("deny") DENY
}

/**
 * Policy conditions for attribute-based access control.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class PolicyConditions(
    @JsonProperty("string_equals") val stringEquals: Map<String, String>? = null,
    @JsonProperty("string_not_equals") val stringNotEquals: Map<String, String>? = null,
    @JsonProperty("string_like") val stringLike: Map<String, String>? = null,
    @JsonProperty("ip_address") val ipAddress: Map<String, List<String>>? = null,
    @JsonProperty("date_less_than") val dateLessThan: Map<String, String>? = null,
    @JsonProperty("date_greater_than") val dateGreaterThan: Map<String, String>? = null,
    @JsonProperty("bool") val bool: Map<String, Boolean>? = null
)

/**
 * Request to create an ABAC policy.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreatePolicyRequest(
    val name: String,
    val description: String? = null,
    val effect: PolicyEffect,
    val resources: List<String>,
    val actions: List<String>,
    val conditions: PolicyConditions? = null,
    val priority: Int = 0,
    val tenantId: String? = null
)

/**
 * Request to update an ABAC policy.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class UpdatePolicyRequest(
    val name: String? = null,
    val description: String? = null,
    val effect: PolicyEffect? = null,
    val resources: List<String>? = null,
    val actions: List<String>? = null,
    val conditions: PolicyConditions? = null,
    val priority: Int? = null,
    val enabled: Boolean? = null
)

/**
 * Request to attach a policy to a user or role.
 */
data class AttachPolicyRequest(
    val principalType: PrincipalType,
    val principalId: String
)

/**
 * Request to detach a policy from a user or role.
 */
data class DetachPolicyRequest(
    val principalType: PrincipalType,
    val principalId: String
)

/**
 * Principal type for policy attachment.
 */
enum class PrincipalType {
    @JsonProperty("user") USER,
    @JsonProperty("role") ROLE
}

/**
 * Filter for listing policies.
 */
data class PolicyFilter(
    val tenantId: String? = null,
    val enabled: Boolean? = null,
    val effect: PolicyEffect? = null,
    val limit: Int = 50,
    val offset: Int = 0
)
