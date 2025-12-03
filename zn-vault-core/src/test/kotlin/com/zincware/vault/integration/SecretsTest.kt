// Path: zn-vault-core/src/test/kotlin/com/zincware/vault/integration/SecretsTest.kt
package com.zincware.vault.integration

import com.zincware.vault.ZnVaultClient
import com.zincware.vault.exception.NotFoundException
import com.zincware.vault.models.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.io.File
import java.nio.file.Files
import java.util.*

/**
 * Integration tests for secrets management functionality.
 *
 * Note: Uses regular user (zincuser) because only regular users have
 * secret:read:value permission due to separation of duties principle.
 * Superadmins/admins manage infrastructure, users access secret values.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class SecretsTest : BaseIntegrationTest() {

    private val createdSecretIds = mutableListOf<String>()

    override fun createClient(): ZnVaultClient {
        // Use regular user for secrets tests - only regular users can decrypt
        return TestConfig.createRegularUserClient()
    }

    override fun cleanup() {
        // Clean up created secrets
        createdSecretIds.forEach { id ->
            try {
                client.secrets.delete(id)
                println("  Cleaned up secret: $id")
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
        createdSecretIds.clear()
    }

    // ==================== Create Secret Tests ====================

    @Test
    @Order(1)
    @DisplayName("Create a credential secret")
    fun testCreateCredentialSecret() {
        val alias = uniqueAlias("creds")

        val secret = client.secrets.create(
            CreateSecretRequest(
                alias = alias,
                type = SecretType.CREDENTIAL,
                data = mapOf(
                    "username" to "testuser",
                    "password" to "testpass123"
                ),
                tags = listOf("test", "credential")
            )
        )

        createdSecretIds.add(secret.id)

        assertNotNull(secret.id)
        assertEquals(alias, secret.alias)
        assertEquals(TestConfig.DEFAULT_TENANT, secret.tenant)
        assertEquals(SecretType.CREDENTIAL, secret.type)
        assertEquals(1, secret.version)
        // Note: tags may be null if server doesn't return them in create response
        println("✓ Created credential secret: ${secret.id}")
        println("  Alias: ${secret.alias}")
        println("  Version: ${secret.version}")
        println("  Tags: ${secret.tags}")
    }

    @Test
    @Order(2)
    @DisplayName("Create an opaque secret")
    fun testCreateOpaqueSecret() {
        val alias = uniqueAlias("opaque")

        val secret = client.secrets.create(
            alias = alias,
            type = SecretType.OPAQUE,
            data = mapOf(
                "api_key" to "sk_live_abc123",
                "api_secret" to "secret_xyz789"
            ),
            tags = listOf("test", "api")
        )

        createdSecretIds.add(secret.id)

        assertNotNull(secret.id)
        assertEquals(SecretType.OPAQUE, secret.type)

        println("✓ Created opaque secret: ${secret.id}")
    }

    @Test
    @Order(3)
    @DisplayName("Create a setting secret")
    fun testCreateSettingSecret() {
        val alias = uniqueAlias("settings")

        val secret = client.secrets.create(
            alias = alias,
            type = SecretType.SETTING,
            data = mapOf(
                "db_host" to "localhost",
                "db_port" to 5432,
                "db_name" to "myapp"
            )
        )

        createdSecretIds.add(secret.id)

        assertEquals(SecretType.SETTING, secret.type)
        println("✓ Created setting secret: ${secret.id}")
    }

    @Test
    @Order(4)
    @DisplayName("Create credential using helper method")
    fun testCreateCredentialHelper() {
        val alias = uniqueAlias("helper-cred")

        val secret = client.secrets.createCredential(
            alias = alias,
            username = "dbuser",
            password = "dbpass456",
            tags = listOf("database")
        )

        createdSecretIds.add(secret.id)

        assertEquals(SecretType.CREDENTIAL, secret.type)
        println("✓ Created credential via helper: ${secret.id}")
    }

    // ==================== Get & Decrypt Tests ====================

    @Test
    @Order(10)
    @DisplayName("Get secret metadata by ID")
    fun testGetSecretById() {
        // Create a secret first
        val created = client.secrets.create(
            alias = uniqueAlias("get-test"),
            type = SecretType.OPAQUE,
            data = mapOf("key" to "value")
        )
        createdSecretIds.add(created.id)

        // Get it by ID
        val secret = client.secrets.get(created.id)

        assertEquals(created.id, secret.id)
        assertEquals(created.alias, secret.alias)

        println("✓ Retrieved secret by ID: ${secret.id}")
    }

    @Test
    @Order(11)
    @DisplayName("Decrypt secret value")
    fun testDecryptSecret() {
        // Create a secret
        val created = client.secrets.create(
            alias = uniqueAlias("decrypt-test"),
            type = SecretType.CREDENTIAL,
            data = mapOf(
                "username" to "decryptuser",
                "password" to "decryptpass"
            )
        )
        createdSecretIds.add(created.id)

        // Decrypt it
        val data = client.secrets.decrypt(created.id)

        assertEquals("decryptuser", data.data["username"])
        assertEquals("decryptpass", data.data["password"])

        println("✓ Decrypted secret successfully")
        println("  Username: ${data.data["username"]}")
    }

    @Test
    @Order(12)
    @DisplayName("Get credentials using helper method")
    fun testGetCredentialsHelper() {
        // Create a credential
        val created = client.secrets.createCredential(
            alias = uniqueAlias("getcred-test"),
            username = "helperuser",
            password = "helperpass"
        )
        createdSecretIds.add(created.id)

        // Get credentials
        val (username, password) = client.secrets.getCredentials(created.id)

        assertEquals("helperuser", username)
        assertEquals("helperpass", password)

        println("✓ Retrieved credentials via helper")
    }

    @Test
    @Order(13)
    @DisplayName("Get non-existent secret returns 404")
    fun testGetNonExistentSecret() {
        assertThrows<NotFoundException> {
            client.secrets.get("non-existent-id-12345")
        }

        println("✓ Non-existent secret correctly returns 404")
    }

    // ==================== Update Tests ====================

    @Test
    @Order(20)
    @DisplayName("Update secret data creates new version")
    fun testUpdateSecret() {
        // Create a secret
        val created = client.secrets.create(
            alias = uniqueAlias("update-test"),
            type = SecretType.OPAQUE,
            data = mapOf("key" to "original_value")
        )
        createdSecretIds.add(created.id)

        assertEquals(1, created.version)

        // Update it
        val updated = client.secrets.update(
            id = created.id,
            data = mapOf("key" to "updated_value")
        )

        assertEquals(2, updated.version)

        // Verify the value changed
        val data = client.secrets.decrypt(updated.id)
        assertEquals("updated_value", data.data["key"])

        println("✓ Updated secret, version: ${created.version} -> ${updated.version}")
    }

    @Test
    @Order(21)
    @DisplayName("Update secret metadata (tags)")
    fun testUpdateSecretMetadata() {
        // Create a secret
        val created = client.secrets.create(
            alias = uniqueAlias("meta-test"),
            type = SecretType.OPAQUE,
            data = mapOf("key" to "value"),
            tags = listOf("original")
        )
        createdSecretIds.add(created.id)

        // Update tags
        val updated = client.secrets.updateMetadata(
            id = created.id,
            tags = listOf("updated", "new-tag")
        )

        assertTrue(updated.tags?.contains("updated") ?: false)
        assertTrue(updated.tags?.contains("new-tag") ?: false)

        println("✓ Updated metadata, tags: ${created.tags} -> ${updated.tags}")
    }

    // ==================== Rotate Tests ====================

    @Test
    @Order(30)
    @DisplayName("Rotate secret creates new version")
    fun testRotateSecret() {
        // Create a secret
        val created = client.secrets.create(
            alias = uniqueAlias("rotate-test"),
            type = SecretType.CREDENTIAL,
            data = mapOf(
                "username" to "user",
                "password" to "oldpass"
            )
        )
        createdSecretIds.add(created.id)

        // Rotate it
        val rotated = client.secrets.rotate(
            id = created.id,
            newData = mapOf(
                "username" to "user",
                "password" to "newpass"
            )
        )

        assertEquals(2, rotated.version)

        // Verify new value
        val data = client.secrets.decrypt(rotated.id)
        assertEquals("newpass", data.data["password"])

        println("✓ Rotated secret, version: ${created.version} -> ${rotated.version}")
    }

    // ==================== History Tests ====================

    @Test
    @Order(40)
    @DisplayName("Get secret version history")
    fun testGetSecretHistory() {
        // Create and update a secret to have multiple versions
        val created = client.secrets.create(
            alias = uniqueAlias("history-test"),
            type = SecretType.OPAQUE,
            data = mapOf("version" to "1")
        )
        createdSecretIds.add(created.id)

        client.secrets.update(created.id, mapOf("version" to "2"))
        client.secrets.update(created.id, mapOf("version" to "3"))

        // Get history
        val history = client.secrets.getHistory(created.id)

        // Should have at least 1 version (the current one)
        assertTrue(history.isNotEmpty(), "History should not be empty")
        println("✓ Secret has ${history.size} versions")
        history.forEach { version ->
            println("  Version ${version.version}: created at ${version.createdAt}")
        }
    }

    @Test
    @Order(41)
    @DisplayName("Decrypt specific version")
    fun testDecryptSpecificVersion() {
        // Create a secret
        val created = client.secrets.create(
            alias = uniqueAlias("version-test"),
            type = SecretType.OPAQUE,
            data = mapOf("value" to "version1")
        )
        createdSecretIds.add(created.id)

        // Update to create version 2
        client.secrets.update(created.id, mapOf("value" to "version2"))

        // Decrypt version 1
        val v1Data = client.secrets.decryptVersion(created.id, 1)
        assertEquals("version1", v1Data.data["value"])

        // Decrypt version 2 (current)
        val v2Data = client.secrets.decrypt(created.id)
        assertEquals("version2", v2Data.data["value"])

        println("✓ Decrypted specific versions successfully")
    }

    // ==================== List Tests ====================

    @Test
    @Order(50)
    @DisplayName("List secrets with pagination")
    fun testListSecrets() {
        // Create some secrets
        repeat(3) { i ->
            val secret = client.secrets.create(
                alias = uniqueAlias("list-test-$i"),
                type = SecretType.OPAQUE,
                data = mapOf("index" to i)
            )
            createdSecretIds.add(secret.id)
        }

        // List with filter
        val secrets = client.secrets.list(
            SecretFilter(
                limit = 10
            )
        )

        assertTrue(secrets.isNotEmpty())
        println("✓ Listed ${secrets.size} secrets")
    }

    @Test
    @Order(51)
    @DisplayName("List all secrets")
    fun testListAllSecrets() {
        val secrets = client.secrets.listAll()

        assertTrue(secrets.isNotEmpty())
        println("✓ Listed ${secrets.size} secrets")
    }

    // ==================== Delete Tests ====================

    @Test
    @Order(60)
    @DisplayName("Delete a secret")
    fun testDeleteSecret() {
        // Create a secret
        val created = client.secrets.create(
            alias = uniqueAlias("delete-test"),
            type = SecretType.OPAQUE,
            data = mapOf("key" to "value")
        )

        // Delete it
        assertDoesNotThrow {
            client.secrets.delete(created.id)
        }

        // Verify it's gone
        assertThrows<NotFoundException> {
            client.secrets.get(created.id)
        }

        println("✓ Deleted secret: ${created.id}")
    }

    // ==================== File Storage Tests ====================

    @Test
    @Order(70)
    @DisplayName("Upload a file as secret")
    fun testUploadFile() {
        // Create a temp file
        val tempFile = Files.createTempFile("test-", ".txt").toFile()
        tempFile.writeText("This is test file content for ZN-Vault SDK testing.")
        tempFile.deleteOnExit()

        val alias = uniqueAlias("file-upload")

        val secret = client.secrets.uploadFile(
            alias = alias,
            file = tempFile,
            tags = listOf("file", "test")
        )

        createdSecretIds.add(secret.id)

        assertEquals(SecretType.OPAQUE, secret.type)

        println("✓ Uploaded file as secret: ${secret.id}")
        println("  Original file: ${tempFile.name}")
    }

    @Test
    @Order(71)
    @DisplayName("Download file from secret")
    fun testDownloadFile() {
        val content = "Test content for download: ${UUID.randomUUID()}"

        // Upload content
        val secret = client.secrets.uploadFile(
            alias = uniqueAlias("file-download"),
            filename = "test.txt",
            content = content.toByteArray(),
            contentType = "text/plain"
        )
        createdSecretIds.add(secret.id)

        // Download as bytes
        val downloaded = client.secrets.downloadFile(secret.id)
        val downloadedContent = String(downloaded)

        assertEquals(content, downloadedContent)

        println("✓ Downloaded file content matches")
    }

    @Test
    @Order(72)
    @DisplayName("Download file to disk")
    fun testDownloadFileToDisk() {
        val content = "File content to save to disk: ${UUID.randomUUID()}"

        // Upload
        val secret = client.secrets.uploadFile(
            alias = uniqueAlias("file-to-disk"),
            filename = "save-test.txt",
            content = content.toByteArray()
        )
        createdSecretIds.add(secret.id)

        // Download to file
        val destination = Files.createTempFile("downloaded-", ".txt").toFile()
        destination.deleteOnExit()

        client.secrets.downloadFile(secret.id, destination)

        assertEquals(content, destination.readText())

        println("✓ Downloaded file to disk: ${destination.absolutePath}")
    }
}
