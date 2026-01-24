// Path: zn-vault-core/src/test/kotlin/com/zincapp/vault/integration/TenantTest.kt
package com.zincapp.vault.integration

import com.zincapp.vault.exception.NotFoundException
import com.zincapp.vault.models.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

/**
 * Integration tests for tenant management functionality.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class TenantTest : BaseIntegrationTest() {

    private val createdTenantIds = mutableListOf<String>()

    override fun cleanup() {
        // Tenants are typically not deleted in tests to avoid issues
        createdTenantIds.clear()
    }

    // ==================== Create Tenant Tests ====================

    @Test
    @Order(1)
    @DisplayName("Create a new tenant")
    fun testCreateTenant() {
        val tenantId = "test-tenant-${testId}"

        val tenant = client.admin.tenants.create(
            CreateTenantRequest(
                id = tenantId,
                name = "Test Tenant $testId"
            )
        )

        createdTenantIds.add(tenant.id)

        assertEquals(tenantId, tenant.id)
        assertEquals("Test Tenant $testId", tenant.name)

        println("✓ Created tenant: ${tenant.id}")
        println("  Name: ${tenant.name}")
        println("  Plan: ${tenant.planTier}")
    }

    @Test
    @Order(2)
    @DisplayName("Create tenant with simplified method")
    fun testCreateTenantSimplified() {
        val tenantId = "simple-tenant-${testId}"

        val tenant = client.admin.tenants.create(
            id = tenantId,
            name = "Simple Tenant"
        )

        createdTenantIds.add(tenant.id)

        assertNotNull(tenant.id)
        assertEquals("Simple Tenant", tenant.name)

        println("✓ Created tenant via simplified method: ${tenant.id}")
    }

    // ==================== Get Tenant Tests ====================

    @Test
    @Order(10)
    @DisplayName("Get tenant by ID")
    fun testGetTenant() {
        val tenantId = "get-test-${testId}"
        val created = client.admin.tenants.create(tenantId, "Get Test Tenant")
        createdTenantIds.add(created.id)

        val tenant = client.admin.tenants.get(tenantId)

        assertEquals(tenantId, tenant.id)
        assertEquals("Get Test Tenant", tenant.name)

        println("✓ Retrieved tenant: ${tenant.id}")
    }

    @Test
    @Order(11)
    @DisplayName("Get non-existent tenant returns 404")
    fun testGetNonExistentTenant() {
        assertThrows<NotFoundException> {
            client.admin.tenants.get("non-existent-tenant-12345")
        }

        println("✓ Non-existent tenant correctly returns 404")
    }

    // ==================== List Tenants Tests ====================

    @Test
    @Order(20)
    @DisplayName("List tenants with pagination")
    fun testListTenants() {
        val page = client.admin.tenants.list(
            TenantFilter(limit = 10)
        )

        assertTrue(page.items.isNotEmpty())
        println("✓ Listed ${page.items.size} tenants (total: ${page.total})")
    }

    // ==================== Update Tenant Tests ====================

    @Test
    @Order(30)
    @DisplayName("Update tenant name")
    fun testUpdateTenant() {
        val tenantId = "update-test-${testId}"
        val created = client.admin.tenants.create(
            id = tenantId,
            name = "Original Name"
        )
        createdTenantIds.add(created.id)

        val updated = client.admin.tenants.update(
            id = tenantId,
            request = UpdateTenantRequest(
                name = "Updated Name"
            )
        )

        assertEquals("Updated Name", updated.name)

        println("✓ Updated tenant: ${created.name} -> ${updated.name}")
    }

    @Test
    @Order(31)
    @DisplayName("Suspend and activate tenant")
    fun testSuspendActivateTenant() {
        val tenantId = "suspend-test-${testId}"
        val created = client.admin.tenants.create(tenantId, "Suspend Test")
        createdTenantIds.add(created.id)

        // Suspend
        val suspended = client.admin.tenants.suspend(tenantId)
        assertEquals(TenantStatus.SUSPENDED, suspended.status)
        println("✓ Suspended tenant")

        // Activate
        val activated = client.admin.tenants.activate(tenantId)
        assertEquals(TenantStatus.ACTIVE, activated.status)
        println("✓ Re-activated tenant")
    }
}
