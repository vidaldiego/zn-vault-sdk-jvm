// Path: zn-vault-core/src/test/kotlin/com/zincware/vault/integration/TestConfig.kt
package com.zincware.vault.integration

import com.zincware.vault.ZnVaultClient

/**
 * Test configuration for integration tests.
 */
object TestConfig {
    // Test server
    const val BASE_URL = "https://localhost:8443"

    // Test users
    // Note: Username must be in format "tenant/username" for non-superadmin users.
    // Superadmin can omit tenant prefix. Email can also be used as username.
    object Users {
        // Superadmin - full access (no tenant prefix required)
        const val SUPERADMIN_USERNAME = "admin"
        const val SUPERADMIN_PASSWORD = "Admin123456#"

        // Tenant admin - manages tenant resources (requires tenant/username format)
        const val TENANT_ADMIN_USERNAME = "zincapp/zincadmin"
        const val TENANT_ADMIN_PASSWORD = "Admin123456#"

        // Regular user - limited access (requires tenant/username format)
        const val REGULAR_USER_USERNAME = "zincapp/zincuser"
        const val REGULAR_USER_PASSWORD = "Admin123456#"
    }

    // Default tenant for tests
    const val DEFAULT_TENANT = "zincapp"

    /**
     * Create a client for testing (insecure TLS for localhost).
     */
    fun createTestClient(): ZnVaultClient {
        return ZnVaultClient.builder()
            .baseUrl(BASE_URL)
            .insecureTls()
            .debug(true)
            .build()
    }

    /**
     * Create an authenticated client as superadmin.
     */
    fun createSuperadminClient(): ZnVaultClient {
        val client = createTestClient()
        client.login(Users.SUPERADMIN_USERNAME, Users.SUPERADMIN_PASSWORD)
        return client
    }

    /**
     * Create an authenticated client as tenant admin.
     */
    fun createTenantAdminClient(): ZnVaultClient {
        val client = createTestClient()
        client.login(Users.TENANT_ADMIN_USERNAME, Users.TENANT_ADMIN_PASSWORD)
        return client
    }

    /**
     * Create an authenticated client as regular user.
     */
    fun createRegularUserClient(): ZnVaultClient {
        val client = createTestClient()
        client.login(Users.REGULAR_USER_USERNAME, Users.REGULAR_USER_PASSWORD)
        return client
    }
}
