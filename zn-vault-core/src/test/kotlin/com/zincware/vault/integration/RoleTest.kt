// Path: zn-vault-core/src/test/kotlin/com/zincware/vault/integration/RoleTest.kt
package com.zincware.vault.integration

import com.zincware.vault.exception.NotFoundException
import com.zincware.vault.models.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

/**
 * Integration tests for role management functionality (RBAC).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class RoleTest : BaseIntegrationTest() {

    private val createdRoleIds = mutableListOf<String>()

    override fun cleanup() {
        // Clean up created roles
        createdRoleIds.forEach { id ->
            try {
                client.admin.roles.delete(id)
                println("  Cleaned up role: $id")
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
        createdRoleIds.clear()
    }

    // ==================== Create Role Tests ====================

    @Test
    @Order(1)
    @DisplayName("Create a custom role")
    fun testCreateRole() {
        val roleName = "custom-role-${testId}"

        val role = client.admin.roles.create(
            CreateRoleRequest(
                name = roleName,
                description = "Custom role for SDK testing",
                tenantId = TestConfig.DEFAULT_TENANT,
                permissions = listOf(
                    "secret:read:metadata",
                    "secret:read:value",
                    "secret:list"
                )
            )
        )

        createdRoleIds.add(role.id)

        assertNotNull(role.id)
        assertEquals(roleName, role.name)
        assertTrue(role.permissions.contains("secret:read:metadata"))
        assertTrue(role.permissions.contains("secret:read:value"))

        println("✓ Created role: ${role.name}")
        println("  ID: ${role.id}")
        println("  Permissions: ${role.permissions}")
    }

    @Test
    @Order(2)
    @DisplayName("Create role with simplified method")
    fun testCreateRoleSimplified() {
        val roleName = "simple-role-${testId}"

        val role = client.admin.roles.create(
            name = roleName,
            description = "Simple role",
            tenantId = TestConfig.DEFAULT_TENANT,
            permissions = listOf("secret:list")
        )

        createdRoleIds.add(role.id)

        assertNotNull(role.id)
        assertEquals(roleName, role.name)

        println("✓ Created role via simplified method: ${role.name}")
    }

    // ==================== Get Role Tests ====================

    @Test
    @Order(10)
    @DisplayName("Get role by ID")
    fun testGetRole() {
        val roleName = "get-role-${testId}"
        val created = client.admin.roles.create(
            name = roleName,
            description = "Get test role",
            tenantId = TestConfig.DEFAULT_TENANT,
            permissions = listOf("secret:list")
        )
        createdRoleIds.add(created.id)

        val role = client.admin.roles.get(created.id)

        assertEquals(created.id, role.id)
        assertEquals(roleName, role.name)

        println("✓ Retrieved role: ${role.name}")
    }

    @Test
    @Order(11)
    @DisplayName("Get non-existent role returns 404")
    fun testGetNonExistentRole() {
        assertThrows<NotFoundException> {
            client.admin.roles.get("non-existent-role-id-12345")
        }

        println("✓ Non-existent role correctly returns 404")
    }

    // ==================== List Roles Tests ====================

    @Test
    @Order(20)
    @DisplayName("List roles")
    fun testListRoles() {
        // Create a few roles
        repeat(2) { i ->
            val role = client.admin.roles.create(
                name = "list-role-$i-${testId}",
                description = "List test role $i",
                tenantId = TestConfig.DEFAULT_TENANT,
                permissions = listOf("secret:list")
            )
            createdRoleIds.add(role.id)
        }

        val page = client.admin.roles.list(
            RoleFilter(
                tenantId = TestConfig.DEFAULT_TENANT,
                limit = 20
            )
        )

        assertTrue(page.items.isNotEmpty())
        println("✓ Listed ${page.items.size} roles (total: ${page.total})")
    }

    // ==================== Update Role Tests ====================

    @Test
    @Order(30)
    @DisplayName("Update role")
    fun testUpdateRole() {
        val roleName = "update-role-${testId}"
        val created = client.admin.roles.create(
            name = roleName,
            description = "Original description",
            tenantId = TestConfig.DEFAULT_TENANT,
            permissions = listOf("secret:list")
        )
        createdRoleIds.add(created.id)

        val updated = client.admin.roles.update(
            id = created.id,
            request = UpdateRoleRequest(description = "Updated description")
        )

        assertEquals("Updated description", updated.description)

        println("✓ Updated role description")
    }

    // ==================== Delete Role Tests ====================

    @Test
    @Order(50)
    @DisplayName("Delete a custom role")
    fun testDeleteRole() {
        val roleName = "delete-role-${testId}"
        val created = client.admin.roles.create(
            name = roleName,
            description = "Delete test role",
            tenantId = TestConfig.DEFAULT_TENANT,
            permissions = listOf("secret:list")
        )

        // Delete it
        assertDoesNotThrow {
            client.admin.roles.delete(created.id)
        }

        // Verify it's gone
        assertThrows<NotFoundException> {
            client.admin.roles.get(created.id)
        }

        println("✓ Deleted role: $roleName")
    }
}
