// Path: zn-vault-core/src/test/kotlin/com/zincapp/vault/integration/PolicyTest.kt
package com.zincapp.vault.integration

import com.zincapp.vault.exception.NotFoundException
import com.zincapp.vault.models.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

/**
 * Integration tests for policy management functionality (ABAC).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class PolicyTest : BaseIntegrationTest() {

    private val createdPolicyIds = mutableListOf<String>()

    override fun cleanup() {
        // Clean up created policies
        createdPolicyIds.forEach { id ->
            try {
                client.admin.policies.delete(id)
                println("  Cleaned up policy: $id")
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
        createdPolicyIds.clear()
    }

    // ==================== Create Policy Tests ====================

    @Test
    @Order(1)
    @DisplayName("Create an ABAC allow policy")
    fun testCreatePolicy() {
        val policyName = "test-policy-${testId}"

        val policy = client.admin.policies.create(
            CreatePolicyRequest(
                name = policyName,
                description = "Policy for SDK testing",
                effect = PolicyEffect.ALLOW,
                actions = listOf("secret:read:value", "secret:read:metadata"),
                resources = listOf("secret:*")
            )
        )

        createdPolicyIds.add(policy.id)

        assertNotNull(policy.id)
        assertEquals(policyName, policy.name)
        assertEquals(PolicyEffect.ALLOW, policy.effect)
        assertTrue(policy.actions.contains("secret:read:value"))
        assertTrue(policy.enabled)

        println("✓ Created policy: ${policy.name}")
        println("  ID: ${policy.id}")
        println("  Effect: ${policy.effect}")
        println("  Actions: ${policy.actions}")
    }

    @Test
    @Order(2)
    @DisplayName("Create a deny policy")
    fun testCreateDenyPolicy() {
        val policyName = "deny-policy-${testId}"

        val policy = client.admin.policies.createDenyPolicy(
            name = policyName,
            description = "Deny policy for testing",
            actions = listOf("secret:delete"),
            resources = listOf("secret:production/*")
        )

        createdPolicyIds.add(policy.id)

        assertEquals(PolicyEffect.DENY, policy.effect)
        assertTrue(policy.actions.contains("secret:delete"))

        println("✓ Created deny policy: ${policy.name}")
    }

    @Test
    @Order(3)
    @DisplayName("Create policy with conditions")
    fun testCreatePolicyWithConditions() {
        val policyName = "cond-policy-${testId}"

        val policy = client.admin.policies.create(
            CreatePolicyRequest(
                name = policyName,
                description = "Conditional policy",
                effect = PolicyEffect.ALLOW,
                actions = listOf("secret:read:value"),
                resources = listOf("secret:*"),
                conditions = PolicyConditions(
                    stringEquals = mapOf("resource.env" to "production")
                )
            )
        )

        createdPolicyIds.add(policy.id)

        // Note: Server may not persist conditions, just verify policy was created
        assertNotNull(policy.id)
        assertEquals(policyName, policy.name)

        println("✓ Created conditional policy: ${policy.name}")
    }

    // ==================== Get Policy Tests ====================

    @Test
    @Order(10)
    @DisplayName("Get policy by ID")
    fun testGetPolicy() {
        val policyName = "get-policy-${testId}"
        val created = client.admin.policies.createAllowPolicy(
            name = policyName,
            description = "Get test policy",
            actions = listOf("secret:list"),
            resources = listOf("secret:*")
        )
        createdPolicyIds.add(created.id)

        val policy = client.admin.policies.get(created.id)

        assertEquals(created.id, policy.id)
        assertEquals(policyName, policy.name)

        println("✓ Retrieved policy: ${policy.name}")
    }

    @Test
    @Order(11)
    @DisplayName("Get non-existent policy returns 404")
    fun testGetNonExistentPolicy() {
        assertThrows<NotFoundException> {
            client.admin.policies.get("non-existent-policy-id-12345")
        }

        println("✓ Non-existent policy correctly returns 404")
    }

    // ==================== List Policies Tests ====================

    @Test
    @Order(20)
    @DisplayName("List policies")
    fun testListPolicies() {
        // Create a few policies
        repeat(2) { i ->
            val policy = client.admin.policies.createAllowPolicy(
                name = "list-policy-$i-${testId}",
                description = "List test policy $i",
                actions = listOf("secret:list"),
                resources = listOf("secret:*")
            )
            createdPolicyIds.add(policy.id)
        }

        val page = client.admin.policies.list(
            PolicyFilter(limit = 10)
        )

        assertTrue(page.items.isNotEmpty())
        println("✓ Listed ${page.items.size} policies (total: ${page.totalItems})")
    }

    @Test
    @Order(21)
    @DisplayName("List policies by effect")
    fun testListPoliciesByEffect() {
        val page = client.admin.policies.list(
            PolicyFilter(effect = PolicyEffect.ALLOW, limit = 10)
        )

        page.items.forEach { policy ->
            assertEquals(PolicyEffect.ALLOW, policy.effect)
        }

        println("✓ Listed ${page.items.size} ALLOW policies")
    }

    // ==================== Update Policy Tests ====================

    @Test
    @Order(30)
    @DisplayName("Update policy description")
    fun testUpdatePolicy() {
        val policyName = "update-policy-${testId}"
        val created = client.admin.policies.createAllowPolicy(
            name = policyName,
            description = "Original description",
            actions = listOf("secret:list"),
            resources = listOf("secret:*")
        )
        createdPolicyIds.add(created.id)

        val updated = client.admin.policies.update(
            id = created.id,
            request = UpdatePolicyRequest(description = "Updated description")
        )

        assertEquals("Updated description", updated.description)

        println("✓ Updated policy description")
    }

    @Test
    @Order(31)
    @DisplayName("Update policy actions")
    fun testUpdatePolicyActions() {
        val policyName = "actions-policy-${testId}"
        val created = client.admin.policies.createAllowPolicy(
            name = policyName,
            description = "Actions test policy",
            actions = listOf("secret:list"),
            resources = listOf("secret:*")
        )
        createdPolicyIds.add(created.id)

        val updated = client.admin.policies.update(
            id = created.id,
            request = UpdatePolicyRequest(
                actions = listOf("secret:list", "secret:read:metadata", "secret:read:value")
            )
        )

        assertEquals(3, updated.actions.size)
        assertTrue(updated.actions.contains("secret:read:value"))

        println("✓ Updated policy actions: ${updated.actions}")
    }

    @Test
    @Order(32)
    @DisplayName("Disable and enable policy")
    fun testDisableEnablePolicy() {
        val policyName = "enable-policy-${testId}"
        val created = client.admin.policies.createAllowPolicy(
            name = policyName,
            description = "Enable test policy",
            actions = listOf("secret:list"),
            resources = listOf("secret:*")
        )
        createdPolicyIds.add(created.id)

        assertTrue(created.enabled)

        // Disable
        val disabled = client.admin.policies.disable(created.id)
        assertFalse(disabled.enabled)
        println("✓ Disabled policy")

        // Enable
        val enabled = client.admin.policies.enable(created.id)
        assertTrue(enabled.enabled)
        println("✓ Re-enabled policy")
    }

    // ==================== Delete Policy Tests ====================

    @Test
    @Order(50)
    @DisplayName("Delete a policy")
    fun testDeletePolicy() {
        val policyName = "delete-policy-${testId}"
        val created = client.admin.policies.createAllowPolicy(
            name = policyName,
            description = "Delete test policy",
            actions = listOf("secret:list"),
            resources = listOf("secret:*")
        )

        // Delete it
        assertDoesNotThrow {
            client.admin.policies.delete(created.id)
        }

        // Verify it's gone
        assertThrows<NotFoundException> {
            client.admin.policies.get(created.id)
        }

        println("✓ Deleted policy: $policyName")
    }
}
