// Path: zn-vault-core/src/test/kotlin/com/zincware/vault/integration/TestConfig.kt
package com.zincware.vault.integration

import com.zincware.vault.ZnVaultClient

/**
 * Test configuration for integration tests.
 * Uses environment variables if set, otherwise falls back to defaults.
 *
 * All test users use the standard password: SdkTest123456#
 *
 * Usage:
 *   # Start the SDK test environment first (from zn-vault root):
 *   npm run test:sdk:start
 *
 *   # Run tests:
 *   ./gradlew test
 *
 *   # Or run against production (not recommended):
 *   ZNVAULT_BASE_URL=https://vault.example.com ./gradlew test
 */
object TestConfig {
    // Test server - defaults to SDK test environment (port 9443)
    val BASE_URL: String = System.getenv("ZNVAULT_BASE_URL") ?: "https://localhost:9443"

    // Default tenant for tests
    val DEFAULT_TENANT: String = System.getenv("ZNVAULT_TENANT") ?: "sdk-test"

    // Secondary tenant for isolation tests
    val TENANT_2: String = "sdk-test-2"

    // Standard password for all test users (matches sdk-test-init.js)
    private const val STANDARD_PASSWORD = "SdkTest123456#"

    // Test users - can be overridden with environment variables
    // Note: Username must be in format "tenant/username" for non-superadmin users.
    // Superadmin can omit tenant prefix. Email can also be used as username.
    object Users {
        // Superadmin - full access (no tenant prefix required)
        val SUPERADMIN_USERNAME: String = System.getenv("ZNVAULT_USERNAME") ?: "admin"
        val SUPERADMIN_PASSWORD: String = System.getenv("ZNVAULT_PASSWORD") ?: "Admin123456#"

        // Tenant admin - manages tenant resources with admin-crypto (requires tenant/username format)
        val TENANT_ADMIN_USERNAME: String = System.getenv("ZNVAULT_TENANT_ADMIN_USERNAME") ?: "$DEFAULT_TENANT/sdk-admin"
        val TENANT_ADMIN_PASSWORD: String = System.getenv("ZNVAULT_TENANT_ADMIN_PASSWORD") ?: STANDARD_PASSWORD

        // Read-only user - can only read secrets (requires tenant/username format)
        val READER_USERNAME: String = System.getenv("ZNVAULT_READER_USERNAME") ?: "$DEFAULT_TENANT/sdk-reader"
        val READER_PASSWORD: String = System.getenv("ZNVAULT_READER_PASSWORD") ?: STANDARD_PASSWORD

        // Read-write user - can read and write secrets (requires tenant/username format)
        val WRITER_USERNAME: String = System.getenv("ZNVAULT_WRITER_USERNAME") ?: "$DEFAULT_TENANT/sdk-writer"
        val WRITER_PASSWORD: String = System.getenv("ZNVAULT_WRITER_PASSWORD") ?: STANDARD_PASSWORD

        // KMS user - can only use KMS operations (requires tenant/username format)
        val KMS_USER_USERNAME: String = System.getenv("ZNVAULT_KMS_USER_USERNAME") ?: "$DEFAULT_TENANT/sdk-kms-user"
        val KMS_USER_PASSWORD: String = System.getenv("ZNVAULT_KMS_USER_PASSWORD") ?: STANDARD_PASSWORD

        // Certificate user - can manage certificates (requires tenant/username format)
        val CERT_USER_USERNAME: String = System.getenv("ZNVAULT_CERT_USER_USERNAME") ?: "$DEFAULT_TENANT/sdk-cert-user"
        val CERT_USER_PASSWORD: String = System.getenv("ZNVAULT_CERT_USER_PASSWORD") ?: STANDARD_PASSWORD

        // Second tenant admin (for isolation testing)
        val TENANT2_ADMIN_USERNAME: String = System.getenv("ZNVAULT_TENANT2_ADMIN_USERNAME") ?: "$TENANT_2/sdk-admin"
        val TENANT2_ADMIN_PASSWORD: String = System.getenv("ZNVAULT_TENANT2_ADMIN_PASSWORD") ?: STANDARD_PASSWORD
    }

    // Pre-created API keys (created by sdk-test-init.js)
    object ApiKeys {
        val FULL_ACCESS: String? = System.getenv("ZNVAULT_API_KEY_FULL")
        val READ_ONLY: String? = System.getenv("ZNVAULT_API_KEY_READONLY")
        val KMS_ONLY: String? = System.getenv("ZNVAULT_API_KEY_KMS")
        val WITH_IP_RESTRICTION: String? = System.getenv("ZNVAULT_API_KEY_WITH_IP")
        val PROD_ONLY: String? = System.getenv("ZNVAULT_API_KEY_PROD_ONLY")
    }

    // Test resources
    object Resources {
        val TEST_SECRET_ALIAS: String = System.getenv("ZNVAULT_TEST_SECRET_ALIAS") ?: "sdk-test/database/credentials"
    }

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
     * Create an authenticated client as read-only user.
     * Uses caching to reuse the same client across tests (rate limit protection).
     */
    fun createReaderClient(): ZnVaultClient {
        return BaseIntegrationTest.getCachedClient("reader") {
            val client = createTestClient()
            client.login(Users.READER_USERNAME, Users.READER_PASSWORD)
            client
        }
    }

    /**
     * Create an authenticated client as read-write user.
     * Uses caching to reuse the same client across tests (rate limit protection).
     */
    fun createWriterClient(): ZnVaultClient {
        return BaseIntegrationTest.getCachedClient("writer") {
            val client = createTestClient()
            client.login(Users.WRITER_USERNAME, Users.WRITER_PASSWORD)
            client
        }
    }

    /**
     * Create an authenticated client as KMS user.
     * Uses caching to reuse the same client across tests (rate limit protection).
     */
    fun createKmsUserClient(): ZnVaultClient {
        return BaseIntegrationTest.getCachedClient("kms_user") {
            val client = createTestClient()
            client.login(Users.KMS_USER_USERNAME, Users.KMS_USER_PASSWORD)
            client
        }
    }

    /**
     * Create an authenticated client as certificate user.
     * Uses caching to reuse the same client across tests (rate limit protection).
     */
    fun createCertUserClient(): ZnVaultClient {
        return BaseIntegrationTest.getCachedClient("cert_user") {
            val client = createTestClient()
            client.login(Users.CERT_USER_USERNAME, Users.CERT_USER_PASSWORD)
            client
        }
    }

    /**
     * Create an authenticated client as second tenant admin.
     * Uses caching to reuse the same client across tests (rate limit protection).
     */
    fun createTenant2AdminClient(): ZnVaultClient {
        return BaseIntegrationTest.getCachedClient("tenant2_admin") {
            val client = createTestClient()
            client.login(Users.TENANT2_ADMIN_USERNAME, Users.TENANT2_ADMIN_PASSWORD)
            client
        }
    }
}
