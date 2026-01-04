// Path: zn-vault-core/src/test/kotlin/com/zincware/vault/integration/KmsTest.kt
package com.zincware.vault.integration

import com.zincware.vault.ZnVaultClient
import com.zincware.vault.models.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.util.Base64

/**
 * Integration tests for Key Management Service (KMS) functionality.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class KmsTest : BaseIntegrationTest() {

    private val createdKeyIds = mutableListOf<String>()

    override fun createClient(): ZnVaultClient {
        // Use tenant admin - scoped to sdk-test tenant, has all tenant permissions
        return TestConfig.createTenantAdminClient()
    }

    override fun cleanup() {
        // Schedule deletion for created keys
        createdKeyIds.forEach { keyId ->
            try {
                client.kms.scheduleKeyDeletion(keyId, 7)
                println("  Scheduled deletion for key: $keyId")
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
        createdKeyIds.clear()
    }

    // ==================== Key Creation Tests ====================

    @Test
    @Order(1)
    @DisplayName("Create an AES-256 encryption key")
    fun testCreateAes256Key() {
        val key = client.kms.createKey(
            CreateKeyRequest(
                alias = "alias/test-aes-${testId}",
                description = "Test AES-256 key for SDK tests",
                usage = KeyUsage.ENCRYPT_DECRYPT,
                keySpec = KeySpec.AES_256,
                tenant = TestConfig.DEFAULT_TENANT,
                tags = listOf(KeyTag("purpose", "testing"))
            )
        )

        createdKeyIds.add(key.keyId)

        assertNotNull(key.keyId)
        assertEquals(KeyUsage.ENCRYPT_DECRYPT, key.usage)
        assertEquals(KeySpec.AES_256, key.keySpec)
        assertEquals(KeyState.ENABLED, key.state)
        assertTrue(key.enabled)

        println("✓ Created AES-256 key: ${key.keyId}")
        println("  Alias: ${key.alias}")
        println("  State: ${key.state}")
    }

    @Test
    @Order(2)
    @DisplayName("Create key using simplified method")
    fun testCreateKeySimplified() {
        val key = client.kms.createKey(
            tenant = TestConfig.DEFAULT_TENANT,
            alias = "alias/simple-${testId}",
            description = "Simple test key"
        )

        createdKeyIds.add(key.keyId)

        assertNotNull(key.keyId)
        // Default should be AES-256
        assertEquals(KeySpec.AES_256, key.keySpec)

        println("✓ Created key via simplified method: ${key.keyId}")
    }

    // ==================== Key Retrieval Tests ====================

    @Test
    @Order(10)
    @DisplayName("Get key by ID")
    fun testGetKey() {
        // Create a key first
        val created = client.kms.createKey(
            tenant = TestConfig.DEFAULT_TENANT,
            alias = "alias/get-test-${testId}"
        )
        createdKeyIds.add(created.keyId)

        // Get it
        val key = client.kms.getKey(created.keyId)

        assertEquals(created.keyId, key.keyId)
        assertEquals(created.alias, key.alias)

        println("✓ Retrieved key: ${key.keyId}")
    }

    @Test
    @Order(11)
    @DisplayName("List keys with pagination")
    fun testListKeys() {
        // Create a few keys
        repeat(2) { i ->
            val key = client.kms.createKey(
                tenant = TestConfig.DEFAULT_TENANT,
                alias = "alias/list-test-$i-${testId}"
            )
            createdKeyIds.add(key.keyId)
        }

        val keyList = client.kms.listKeys(
            KeyFilter(
                tenant = TestConfig.DEFAULT_TENANT,
                limit = 10
            )
        )

        assertTrue(keyList.items.isNotEmpty())
        println("✓ Listed ${keyList.items.size} keys")
    }

    // ==================== Encrypt/Decrypt Tests ====================

    @Test
    @Order(20)
    @DisplayName("Encrypt and decrypt data")
    fun testEncryptDecrypt() {
        // Create a key
        val key = client.kms.createKey(
            tenant = TestConfig.DEFAULT_TENANT,
            alias = "alias/encrypt-test-${testId}"
        )
        createdKeyIds.add(key.keyId)

        val plaintext = "This is sensitive data that needs encryption"

        // Encrypt
        val encrypted = client.kms.encrypt(
            keyId = key.keyId,
            plaintext = plaintext
        )

        assertNotNull(encrypted.ciphertextBlob)
        assertNotEquals(plaintext, encrypted.ciphertextBlob)

        println("✓ Encrypted data: ${encrypted.ciphertextBlob.take(50)}...")

        // Decrypt
        val decrypted = client.kms.decryptToString(
            keyId = key.keyId,
            ciphertext = encrypted.ciphertext
        )

        assertEquals(plaintext, decrypted)

        println("✓ Decrypted data matches original")
    }

    @Test
    @Order(21)
    @DisplayName("Encrypt and decrypt binary data")
    fun testEncryptDecryptBinary() {
        val key = client.kms.createKey(
            tenant = TestConfig.DEFAULT_TENANT,
            alias = "alias/binary-test-${testId}"
        )
        createdKeyIds.add(key.keyId)

        val binaryData = byteArrayOf(0x00, 0x01, 0x02, 0xFF.toByte(), 0xFE.toByte())

        // Encrypt
        val encrypted = client.kms.encrypt(
            keyId = key.keyId,
            plaintext = binaryData
        )

        // Decrypt
        val decrypted = client.kms.decrypt(
            keyId = key.keyId,
            ciphertext = encrypted.ciphertext
        )

        assertArrayEquals(binaryData, decrypted)
        println("✓ Binary encrypt/decrypt works correctly")
    }

    @Test
    @Order(22)
    @DisplayName("Encrypt with encryption context")
    fun testEncryptWithContext() {
        val key = client.kms.createKey(
            tenant = TestConfig.DEFAULT_TENANT,
            alias = "alias/context-test-${testId}"
        )
        createdKeyIds.add(key.keyId)

        val plaintext = "Context-bound secret"
        val context = mapOf(
            "app" to "myapp",
            "env" to "test"
        )

        // Encrypt with context
        val encrypted = client.kms.encrypt(
            keyId = key.keyId,
            plaintext = plaintext,
            context = context
        )

        // Decrypt with same context
        val decrypted = client.kms.decryptToString(
            keyId = key.keyId,
            ciphertext = encrypted.ciphertext,
            context = context
        )

        assertEquals(plaintext, decrypted)
        println("✓ Encryption context works correctly")
    }

    // ==================== Data Key Generation Tests ====================

    @Test
    @Order(30)
    @DisplayName("Generate data key")
    fun testGenerateDataKey() {
        val key = client.kms.createKey(
            tenant = TestConfig.DEFAULT_TENANT,
            alias = "alias/dek-test-${testId}"
        )
        createdKeyIds.add(key.keyId)

        val dataKey = client.kms.generateDataKey(
            keyId = key.keyId,
            keySpec = KeySpec.AES_256
        )

        assertNotNull(dataKey.plaintext)
        assertNotNull(dataKey.ciphertextBlob)

        // Plaintext should be base64-encoded 32 bytes for AES-256
        val plaintextBytes = Base64.getDecoder().decode(dataKey.plaintext)
        assertEquals(32, plaintextBytes.size)

        println("✓ Generated data key")
        println("  Plaintext length: ${plaintextBytes.size} bytes")
        println("  Ciphertext: ${dataKey.ciphertextBlob.take(40)}...")
    }

    @Test
    @Order(31)
    @DisplayName("Generate data key without plaintext")
    fun testGenerateDataKeyWithoutPlaintext() {
        val key = client.kms.createKey(
            tenant = TestConfig.DEFAULT_TENANT,
            alias = "alias/dek-noplain-${testId}"
        )
        createdKeyIds.add(key.keyId)

        val ciphertext = client.kms.generateDataKeyWithoutPlaintext(
            keyId = key.keyId,
            keySpec = KeySpec.AES_256
        )

        assertNotNull(ciphertext)
        assertTrue(ciphertext.isNotEmpty())

        println("✓ Generated data key without plaintext")
        println("  Ciphertext: ${ciphertext.take(40)}...")
    }

    // ==================== Key State Management Tests ====================

    @Test
    @Order(40)
    @DisplayName("Disable and enable key")
    fun testDisableEnableKey() {
        val key = client.kms.createKey(
            tenant = TestConfig.DEFAULT_TENANT,
            alias = "alias/state-test-${testId}"
        )
        createdKeyIds.add(key.keyId)

        assertTrue(key.enabled)

        // Disable
        val disabled = client.kms.disableKey(key.keyId)
        assertEquals(KeyState.DISABLED, disabled.state)
        assertFalse(disabled.enabled)
        println("✓ Disabled key")

        // Enable
        val enabled = client.kms.enableKey(key.keyId)
        assertEquals(KeyState.ENABLED, enabled.state)
        assertTrue(enabled.enabled)
        println("✓ Re-enabled key")
    }

    @Test
    @Order(41)
    @DisplayName("Update key description")
    fun testUpdateKeyDescription() {
        val key = client.kms.createKey(
            tenant = TestConfig.DEFAULT_TENANT,
            alias = "alias/desc-test-${testId}",
            description = "Original description"
        )
        createdKeyIds.add(key.keyId)

        val updated = client.kms.updateDescription(
            keyId = key.keyId,
            description = "Updated description"
        )

        assertEquals("Updated description", updated.description)
        println("✓ Updated key description")
    }

    @Test
    @Order(42)
    @DisplayName("Update key alias")
    fun testUpdateKeyAlias() {
        val key = client.kms.createKey(
            tenant = TestConfig.DEFAULT_TENANT,
            alias = "alias/alias-test-${testId}"
        )
        createdKeyIds.add(key.keyId)

        val newAlias = "alias/updated-${testId}"
        val updated = client.kms.updateAlias(
            keyId = key.keyId,
            alias = newAlias
        )

        assertEquals(newAlias, updated.alias)
        println("✓ Updated key alias: ${key.alias} -> ${updated.alias}")
    }

    // ==================== Key Rotation Tests ====================

    @Test
    @Order(50)
    @DisplayName("Rotate key manually")
    fun testRotateKey() {
        val key = client.kms.createKey(
            tenant = TestConfig.DEFAULT_TENANT,
            alias = "alias/rotate-test-${testId}"
        )
        createdKeyIds.add(key.keyId)

        // Rotate the key - just verify it doesn't throw
        val rotated = client.kms.rotateKey(key.keyId)

        // Key should still be enabled after rotation
        assertEquals(KeyState.ENABLED, rotated.state)
        println("✓ Rotated key: ${key.keyId}")
    }

    @Test
    @Order(51)
    @DisplayName("Get rotation status")
    fun testGetRotationStatus() {
        val key = client.kms.createKey(
            tenant = TestConfig.DEFAULT_TENANT,
            alias = "alias/rotation-status-${testId}"
        )
        createdKeyIds.add(key.keyId)

        val status = client.kms.getRotationStatus(key.keyId)

        assertNotNull(status)
        assertEquals(key.keyId, status.keyId)

        println("✓ Rotation status: enabled=${status.rotationEnabled}")
        status.nextRotationDate?.let { println("  Next rotation: $it") }
    }

    @Test
    @Order(52)
    @DisplayName("Configure automatic rotation")
    fun testConfigureRotation() {
        val key = client.kms.createKey(
            tenant = TestConfig.DEFAULT_TENANT,
            alias = "alias/auto-rotate-${testId}"
        )
        createdKeyIds.add(key.keyId)

        val status = client.kms.configureRotation(
            keyId = key.keyId,
            enabled = true,
            intervalDays = 90
        )

        assertTrue(status.rotationEnabled)
        assertEquals(90, status.intervalDays)

        println("✓ Configured automatic rotation: ${status.intervalDays} days")
    }

    // ==================== Key Deletion Tests ====================

    @Test
    @Order(60)
    @DisplayName("Schedule and cancel key deletion")
    fun testScheduleAndCancelDeletion() {
        val key = client.kms.createKey(
            tenant = TestConfig.DEFAULT_TENANT,
            alias = "alias/delete-test-${testId}"
        )
        // Don't add to createdKeyIds - we'll handle cleanup manually

        // Schedule deletion
        val scheduled = client.kms.scheduleKeyDeletion(key.keyId, 7)
        assertEquals(KeyState.PENDING_DELETION, scheduled.state)
        assertNotNull(scheduled.deletionDate)
        println("✓ Scheduled key deletion for: ${scheduled.deletionDate}")

        // Cancel deletion
        val cancelled = client.kms.cancelKeyDeletion(key.keyId)
        assertEquals(KeyState.DISABLED, cancelled.state) // Goes to disabled after cancel
        assertNull(cancelled.deletionDate)
        println("✓ Cancelled key deletion")

        // Enable it again and then schedule for cleanup
        client.kms.enableKey(key.keyId)
        createdKeyIds.add(key.keyId)
    }

    // ==================== Encryption After Rotation Tests ====================

    @Test
    @Order(70)
    @DisplayName("Decrypt old ciphertext after key rotation")
    fun testDecryptAfterRotation() {
        val key = client.kms.createKey(
            tenant = TestConfig.DEFAULT_TENANT,
            alias = "alias/rotation-decrypt-${testId}"
        )
        createdKeyIds.add(key.keyId)

        val plaintext = "Data encrypted before rotation"

        // Encrypt with version 1
        val encrypted = client.kms.encrypt(key.keyId, plaintext)

        // Rotate the key
        client.kms.rotateKey(key.keyId)

        // Should still be able to decrypt old ciphertext
        val decrypted = client.kms.decryptToString(key.keyId, encrypted.ciphertextBlob)

        assertEquals(plaintext, decrypted)
        println("✓ Old ciphertext still decryptable after rotation")
    }
}
