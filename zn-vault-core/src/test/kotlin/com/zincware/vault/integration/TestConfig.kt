// Path: zn-vault-core/src/test/kotlin/com/zincware/vault/integration/TestConfig.kt
package com.zincware.vault.integration

import com.zincware.vault.ZnVaultClient

/**
 * Test configuration for integration tests.
 * Uses environment variables if set, otherwise falls back to defaults.
 */
object TestConfig {
    // Test server - can be overridden with ZNVAULT_BASE_URL env var
    val BASE_URL: String = System.getenv("ZNVAULT_BASE_URL") ?: "https://localhost:8443"

    // Test users - can be overridden with ZNVAULT_USERNAME and ZNVAULT_PASSWORD env vars
    // Note: Username must be in format "tenant/username" for non-superadmin users.
    // Superadmin can omit tenant prefix. Email can also be used as username.
    object Users {
        // Superadmin - full access (no tenant prefix required)
        val SUPERADMIN_USERNAME: String = System.getenv("ZNVAULT_USERNAME") ?: "admin"
        val SUPERADMIN_PASSWORD: String = System.getenv("ZNVAULT_PASSWORD") ?: "Admin123456#"

        // Tenant admin - manages tenant resources (requires tenant/username format)
        val TENANT_ADMIN_USERNAME: String = System.getenv("ZNVAULT_TENANT_ADMIN_USERNAME") ?: "zincapp/zincadmin"
        val TENANT_ADMIN_PASSWORD: String = System.getenv("ZNVAULT_TENANT_ADMIN_PASSWORD") ?: "Admin123456#"

        // Regular user - limited access (requires tenant/username format)
        val REGULAR_USER_USERNAME: String = System.getenv("ZNVAULT_REGULAR_USER_USERNAME") ?: "zincapp/zincuser"
        val REGULAR_USER_PASSWORD: String = System.getenv("ZNVAULT_REGULAR_USER_PASSWORD") ?: "Admin123456#"
    }

    // Default tenant for tests
    val DEFAULT_TENANT: String = System.getenv("ZNVAULT_DEFAULT_TENANT") ?: "zincapp"

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
     * Uses caching to reuse the same client across tests (rate limit protection).
     */
    fun createSuperadminClient(): ZnVaultClient {
        return BaseIntegrationTest.getCachedClient("superadmin") {
            val client = createTestClient()
            client.login(Users.SUPERADMIN_USERNAME, Users.SUPERADMIN_PASSWORD)
            client
        }
    }

    /**
     * Create an authenticated client as tenant admin.
     * Uses caching to reuse the same client across tests (rate limit protection).
     */
    fun createTenantAdminClient(): ZnVaultClient {
        return BaseIntegrationTest.getCachedClient("tenant_admin") {
            val client = createTestClient()
            client.login(Users.TENANT_ADMIN_USERNAME, Users.TENANT_ADMIN_PASSWORD)
            client
        }
    }

    /**
     * Create an authenticated client as regular user.
     * Uses caching to reuse the same client across tests (rate limit protection).
     */
    fun createRegularUserClient(): ZnVaultClient {
        return BaseIntegrationTest.getCachedClient("regular_user") {
            val client = createTestClient()
            client.login(Users.REGULAR_USER_USERNAME, Users.REGULAR_USER_PASSWORD)
            client
        }
    }
}
