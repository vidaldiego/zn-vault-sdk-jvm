// Path: zn-vault-core/src/test/kotlin/com/zincware/vault/integration/AuthenticationTest.kt
package com.zincware.vault.integration

import com.zincware.vault.ZnVaultClient
import com.zincware.vault.exception.AuthenticationException
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

/**
 * Integration tests for authentication functionality.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class AuthenticationTest : BaseIntegrationTest() {

    override fun createClient(): ZnVaultClient {
        // Start with unauthenticated client
        return TestConfig.createTestClient()
    }

    // ==================== Login Tests ====================

    @Test
    @Order(1)
    @DisplayName("Login with valid superadmin credentials")
    fun testLoginSuperadmin() {
        val response = client.login(
            TestConfig.Users.SUPERADMIN_USERNAME,
            TestConfig.Users.SUPERADMIN_PASSWORD
        )

        assertNotNull(response.accessToken)
        assertNotNull(response.refreshToken)
        assertTrue(response.expiresIn > 0)
        assertTrue(client.isAuthenticated())

        println("✓ Logged in as superadmin, token expires in ${response.expiresIn}s")
    }

    @Test
    @Order(2)
    @DisplayName("Login with valid tenant admin credentials")
    fun testLoginTenantAdmin() {
        val response = client.login(
            TestConfig.Users.TENANT_ADMIN_USERNAME,
            TestConfig.Users.TENANT_ADMIN_PASSWORD
        )

        assertNotNull(response.accessToken)
        assertTrue(client.isAuthenticated())

        println("✓ Logged in as tenant admin")
    }

    @Test
    @Order(3)
    @DisplayName("Login with valid reader user credentials")
    fun testLoginReaderUser() {
        val response = client.login(
            TestConfig.Users.READER_USERNAME,
            TestConfig.Users.READER_PASSWORD
        )

        assertNotNull(response.accessToken)
        assertTrue(client.isAuthenticated())

        println("✓ Logged in as reader user")
    }

    @Test
    @Order(4)
    @DisplayName("Login with valid writer user credentials")
    fun testLoginWriterUser() {
        val response = client.login(
            TestConfig.Users.WRITER_USERNAME,
            TestConfig.Users.WRITER_PASSWORD
        )

        assertNotNull(response.accessToken)
        assertTrue(client.isAuthenticated())

        println("✓ Logged in as writer user")
    }

    @Test
    @Order(5)
    @DisplayName("Login with valid KMS user credentials")
    fun testLoginKmsUser() {
        val response = client.login(
            TestConfig.Users.KMS_USER_USERNAME,
            TestConfig.Users.KMS_USER_PASSWORD
        )

        assertNotNull(response.accessToken)
        assertTrue(client.isAuthenticated())

        println("✓ Logged in as KMS user")
    }

    @Test
    @Order(6)
    @DisplayName("Login with valid certificate user credentials")
    fun testLoginCertUser() {
        val response = client.login(
            TestConfig.Users.CERT_USER_USERNAME,
            TestConfig.Users.CERT_USER_PASSWORD
        )

        assertNotNull(response.accessToken)
        assertTrue(client.isAuthenticated())

        println("✓ Logged in as certificate user")
    }

    @Test
    @Order(10)
    @DisplayName("Login with invalid credentials should fail")
    fun testLoginInvalidCredentials() {
        assertThrows<AuthenticationException> {
            client.login("invalid_user", "wrong_password")
        }

        assertFalse(client.isAuthenticated())
        println("✓ Invalid credentials correctly rejected")
    }

    @Test
    @Order(11)
    @DisplayName("Login with wrong password should fail")
    fun testLoginWrongPassword() {
        assertThrows<AuthenticationException> {
            client.login(TestConfig.Users.SUPERADMIN_USERNAME, "wrong_password")
        }

        println("✓ Wrong password correctly rejected")
    }

    // ==================== Current User Tests ====================

    @Test
    @Order(20)
    @DisplayName("Get current user info after login")
    fun testGetCurrentUser() {
        client.login(TestConfig.Users.SUPERADMIN_USERNAME, TestConfig.Users.SUPERADMIN_PASSWORD)

        val user = client.auth.me()

        assertEquals(TestConfig.Users.SUPERADMIN_USERNAME, user.username)
        assertNotNull(user.id)
        println("✓ Current user: ${user.username} (${user.role})")
    }

    @Test
    @Order(21)
    @DisplayName("Get current user should fail without authentication")
    fun testGetCurrentUserUnauthenticated() {
        assertThrows<AuthenticationException> {
            client.auth.me()
        }

        println("✓ Unauthenticated request correctly rejected")
    }

    // ==================== Logout Tests ====================

    @Test
    @Order(30)
    @DisplayName("Logout clears authentication")
    fun testLogout() {
        client.login(TestConfig.Users.SUPERADMIN_USERNAME, TestConfig.Users.SUPERADMIN_PASSWORD)
        assertTrue(client.isAuthenticated())

        client.logout()

        assertFalse(client.isAuthenticated())
        println("✓ Logout successful")
    }

    // ==================== API Key Tests ====================

    private var createdApiKeyId: String? = null

    @Test
    @Order(40)
    @DisplayName("Create API key")
    fun testCreateApiKey() {
        client.login(TestConfig.Users.SUPERADMIN_USERNAME, TestConfig.Users.SUPERADMIN_PASSWORD)

        val response = client.auth.createApiKey(
            name = "test-key-${testId}",
            permissions = listOf("secret:read:metadata", "secret:read:value"),
            expiresInDays = 30,
            tenantId = TestConfig.DEFAULT_TENANT
        )

        assertNotNull(response.apiKey.id)
        assertNotNull(response.key)
        assertTrue(response.key.startsWith("znv_"))
        assertNotNull(response.apiKey.prefix)

        createdApiKeyId = response.apiKey.id
        println("✓ Created API key: ${response.apiKey.prefix}... (ID: ${response.apiKey.id})")
    }

    @Test
    @Order(41)
    @DisplayName("List API keys")
    fun testListApiKeys() {
        client.login(TestConfig.Users.SUPERADMIN_USERNAME, TestConfig.Users.SUPERADMIN_PASSWORD)

        val keys = client.auth.listApiKeys()

        assertTrue(keys.isNotEmpty())
        println("✓ Found ${keys.size} API key(s)")
        keys.forEach { key ->
            println("  - ${key.name} (${key.prefix}...) expires: ${key.expiresAt}")
        }
    }

    @Test
    @Order(42)
    @DisplayName("Use API key for authentication")
    fun testApiKeyAuthentication() {
        // First create an API key
        client.login(TestConfig.Users.SUPERADMIN_USERNAME, TestConfig.Users.SUPERADMIN_PASSWORD)

        val keyResponse = client.auth.createApiKey(
            name = "auth-test-key-${testId}",
            permissions = listOf("*"),
            expiresInDays = 1,
            tenantId = TestConfig.DEFAULT_TENANT
        )

        // Create new client with API key
        val apiKeyClient = ZnVaultClient.builder()
            .baseUrl(TestConfig.BASE_URL)
            .apiKey(keyResponse.key)
            .insecureTls()
            .build()

        // Should be able to make authenticated requests
        assertTrue(apiKeyClient.isAuthenticated())
        assertTrue(apiKeyClient.health.isHealthy())

        println("✓ API key authentication works")

        // Cleanup - delete the key
        client.auth.deleteApiKey(keyResponse.apiKey.id)
    }

    @Test
    @Order(43)
    @DisplayName("Rotate API key")
    fun testRotateApiKey() {
        client.login(TestConfig.Users.SUPERADMIN_USERNAME, TestConfig.Users.SUPERADMIN_PASSWORD)

        // Create a key to rotate
        val originalKey = client.auth.createApiKey(
            name = "rotate-test-key-${testId}",
            permissions = listOf("secret:read:*"),
            expiresInDays = 1,
            tenantId = TestConfig.DEFAULT_TENANT
        )

        // Rotate the key (note: rotation creates a new key with new ID)
        val rotatedKey = client.auth.rotateApiKey(originalKey.apiKey.id)

        assertNotNull(rotatedKey.key)
        assertNotEquals(originalKey.key, rotatedKey.key)
        // Rotation creates a new key entry, so IDs will be different
        assertNotNull(rotatedKey.apiKey.id)
        assertEquals(originalKey.apiKey.name, rotatedKey.apiKey.name)

        println("✓ Rotated API key: ${originalKey.apiKey.prefix}... -> ${rotatedKey.apiKey.prefix}...")

        // Cleanup - delete the new key
        client.auth.deleteApiKey(rotatedKey.apiKey.id)
    }

    @Test
    @Order(44)
    @DisplayName("Delete API key")
    fun testDeleteApiKey() {
        client.login(TestConfig.Users.SUPERADMIN_USERNAME, TestConfig.Users.SUPERADMIN_PASSWORD)

        // Create a key to delete
        val keyResponse = client.auth.createApiKey(
            name = "delete-test-key-${testId}",
            permissions = listOf("secret:read:metadata"),
            expiresInDays = 1,
            tenantId = TestConfig.DEFAULT_TENANT
        )

        // Delete it
        assertDoesNotThrow {
            client.auth.deleteApiKey(keyResponse.apiKey.id)
        }

        println("✓ Deleted API key: ${keyResponse.apiKey.id}")
    }

    @Test
    @Order(46)
    @DisplayName("Self-rotate current API key")
    fun testSelfRotateApiKey() {
        // First create an API key
        client.login(TestConfig.Users.SUPERADMIN_USERNAME, TestConfig.Users.SUPERADMIN_PASSWORD)

        val originalKey = client.auth.createApiKey(
            name = "self-rotate-test-key-${testId}",
            permissions = listOf("*"),
            expiresInDays = 1,
            tenantId = TestConfig.DEFAULT_TENANT
        )

        // Create new client with API key
        val apiKeyClient = ZnVaultClient.builder()
            .baseUrl(TestConfig.BASE_URL)
            .apiKey(originalKey.key)
            .insecureTls()
            .build()

        // Self-rotate the key
        val rotatedKey = apiKeyClient.auth.rotateCurrentApiKey()

        assertNotNull(rotatedKey.key)
        assertNotEquals(originalKey.key, rotatedKey.key)
        assertEquals(originalKey.apiKey.name, rotatedKey.apiKey.name)

        println("✓ Self-rotated API key: ${originalKey.apiKey.prefix}... -> ${rotatedKey.apiKey.prefix}...")

        // Cleanup - delete the new key using superadmin
        client.auth.deleteApiKey(rotatedKey.apiKey.id)
    }

    // ==================== 2FA Status Tests ====================

    @Test
    @Order(50)
    @DisplayName("Get 2FA status")
    fun testGet2faStatus() {
        client.login(TestConfig.Users.SUPERADMIN_USERNAME, TestConfig.Users.SUPERADMIN_PASSWORD)

        val status = client.auth.get2faStatus()

        assertNotNull(status)
        println("✓ 2FA enabled: ${status.enabled}")
    }

    // ==================== Password Change Tests ====================

    @Test
    @Order(60)
    @DisplayName("Change password requires current password")
    fun testChangePasswordRequiresCurrentPassword() {
        client.login(TestConfig.Users.SUPERADMIN_USERNAME, TestConfig.Users.SUPERADMIN_PASSWORD)

        // Try to change with wrong current password
        assertThrows<Exception> {
            client.auth.changePassword(
                currentPassword = "wrong_password",
                newPassword = "NewPassword123#"
            )
        }

        println("✓ Change password requires correct current password")
    }
}
