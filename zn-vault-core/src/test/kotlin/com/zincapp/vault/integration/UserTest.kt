// Path: zn-vault-core/src/test/kotlin/com/zincapp/vault/integration/UserTest.kt
package com.zincapp.vault.integration

import com.zincapp.vault.exception.NotFoundException
import com.zincapp.vault.models.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

/**
 * Integration tests for user management functionality.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class UserTest : BaseIntegrationTest() {

    private val createdUserIds = mutableListOf<String>()

    override fun cleanup() {
        // Clean up created users
        createdUserIds.forEach { id ->
            try {
                client.admin.users.delete(id)
                println("  Cleaned up user: $id")
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
        createdUserIds.clear()
    }

    // ==================== Create User Tests ====================

    @Test
    @Order(1)
    @DisplayName("Create a new user")
    fun testCreateUser() {
        val username = "testuser-${testId}"

        val user = client.admin.users.create(
            CreateUserRequest(
                username = username,
                email = "$username@example.com",
                password = "TestPassword123#",
                tenantId = TestConfig.DEFAULT_TENANT
            )
        )

        createdUserIds.add(user.id)

        assertNotNull(user.id)
        // Server returns username with tenant prefix (e.g., "zincapp/testuser-xxx")
        assertTrue(user.username.endsWith(username) || user.username == username,
            "Username should end with $username, got ${user.username}")
        assertEquals("$username@example.com", user.email)

        println("✓ Created user: ${user.username}")
        println("  ID: ${user.id}")
        println("  Email: ${user.email}")
        println("  Tenant: ${user.tenantId}")
    }

    @Test
    @Order(2)
    @DisplayName("Create user with simplified method")
    fun testCreateUserSimplified() {
        val username = "simpleuser-${testId}"

        val user = client.admin.users.create(
            username = username,
            email = "$username@example.com",
            password = "SimplePass123#",
            tenantId = TestConfig.DEFAULT_TENANT
        )

        createdUserIds.add(user.id)

        assertNotNull(user.id)
        // Server returns username with tenant prefix (e.g., "zincapp/simpleuser-xxx")
        assertTrue(user.username.endsWith(username) || user.username == username,
            "Username should end with $username, got ${user.username}")

        println("✓ Created user via simplified method: ${user.username}")
    }

    // ==================== Get User Tests ====================

    @Test
    @Order(10)
    @DisplayName("Get user by ID")
    fun testGetUser() {
        val username = "getuser-${testId}"
        val created = client.admin.users.create(
            username = username,
            email = "$username@example.com",
            password = "GetPassword123#",
            tenantId = TestConfig.DEFAULT_TENANT
        )
        createdUserIds.add(created.id)

        val user = client.admin.users.get(created.id)

        assertEquals(created.id, user.id)
        // Server returns username with tenant prefix
        assertTrue(user.username.endsWith(username) || user.username == username,
            "Username should end with $username, got ${user.username}")

        println("✓ Retrieved user: ${user.username}")
    }

    @Test
    @Order(11)
    @DisplayName("Get non-existent user returns 404")
    fun testGetNonExistentUser() {
        assertThrows<NotFoundException> {
            client.admin.users.get("non-existent-user-id-12345")
        }

        println("✓ Non-existent user correctly returns 404")
    }

    // ==================== List Users Tests ====================

    @Test
    @Order(20)
    @DisplayName("List users with pagination")
    fun testListUsers() {
        // Create a few users
        repeat(2) { i ->
            val username = "listuser-$i-${testId}"
            val user = client.admin.users.create(
                username = username,
                email = "$username@example.com",
                password = "ListPassword123#$i",
                tenantId = TestConfig.DEFAULT_TENANT
            )
            createdUserIds.add(user.id)
        }

        val page = client.admin.users.list(
            UserFilter(
                tenant = TestConfig.DEFAULT_TENANT,
                limit = 10
            )
        )

        assertTrue(page.items.isNotEmpty())
        println("✓ Listed ${page.items.size} users (total: ${page.total})")
    }

    // ==================== Update User Tests ====================

    @Test
    @Order(30)
    @DisplayName("Update user")
    fun testUpdateUser() {
        val username = "updateuser-${testId}"
        val created = client.admin.users.create(
            username = username,
            email = "$username@original.com",
            password = "UpdatePassword123#",
            tenantId = TestConfig.DEFAULT_TENANT
        )
        createdUserIds.add(created.id)

        val updated = client.admin.users.update(
            id = created.id,
            request = UpdateUserRequest(
                email = "$username@updated.com"
            )
        )

        assertEquals("$username@updated.com", updated.email)

        println("✓ Updated user email: ${created.email} -> ${updated.email}")
    }

    // ==================== Delete User Tests ====================

    @Test
    @Order(60)
    @DisplayName("Delete a user")
    fun testDeleteUser() {
        val username = "deleteuser-${testId}"
        val created = client.admin.users.create(
            username = username,
            email = "$username@example.com",
            password = "DeletePassword123#",
            tenantId = TestConfig.DEFAULT_TENANT
        )

        // Delete it
        assertDoesNotThrow {
            client.admin.users.delete(created.id)
        }

        // Verify it's gone
        assertThrows<NotFoundException> {
            client.admin.users.get(created.id)
        }

        println("✓ Deleted user: ${created.username}")
    }
}
