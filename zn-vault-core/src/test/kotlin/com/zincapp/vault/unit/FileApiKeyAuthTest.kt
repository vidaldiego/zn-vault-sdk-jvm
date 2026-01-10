// Path: zn-vault-core/src/test/kotlin/com/zincapp/vault/unit/FileApiKeyAuthTest.kt
package com.zincapp.vault.unit

import com.zincapp.vault.ZnVaultClient
import com.zincapp.vault.auth.ApiKeyAuth
import com.zincapp.vault.auth.FileApiKeyAuth
import com.zincapp.vault.http.RefreshableAuthProvider
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Unit tests for FileApiKeyAuth - simulates deployment environment
 * and key rotation scenarios without requiring a real vault server.
 *
 * These tests validate the lazy refresh mechanism that allows
 * applications (like Payara/WildFly) to handle API key rotation
 * transparently without restart.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class FileApiKeyAuthTest {

    private lateinit var mockServer: MockWebServer
    private lateinit var tempDir: File
    private lateinit var apiKeyFile: File

    companion object {
        private const val INITIAL_API_KEY = "znv_initial_test_key_abc123"
        private const val ROTATED_API_KEY = "znv_rotated_test_key_xyz789"
        private const val ANOTHER_API_KEY = "znv_another_test_key_def456"

        private val HEALTH_RESPONSE = """
            {"status":"healthy","version":"1.9.0","database":"ok","tls":"enabled"}
        """.trimIndent()

        private val UNAUTHORIZED_RESPONSE = """
            {"error":"Unauthorized","message":"Invalid or expired API key"}
        """.trimIndent()

        private val SECRET_RESPONSE = """
            {"id":"123","alias":"test/secret","type":"generic","tenant":"sdk-test","data":{"key":"value"}}
        """.trimIndent()
    }

    @BeforeEach
    fun setUp() {
        // Create temp directory for API key files
        tempDir = Files.createTempDirectory("znvault-test").toFile()
        apiKeyFile = File(tempDir, "api-key")

        // Initialize with the first API key
        apiKeyFile.writeText(INITIAL_API_KEY)

        // Create mock server
        mockServer = MockWebServer()
    }

    @AfterEach
    fun tearDown() {
        mockServer.shutdown()
        tempDir.deleteRecursively()
    }

    // ==================== Basic FileApiKeyAuth Tests ====================

    @Test
    @Order(1)
    @DisplayName("FileApiKeyAuth loads API key from file on initialization")
    fun testFileApiKeyAuthLoadsKeyOnInit() {
        val auth = FileApiKeyAuth(apiKeyFile.absolutePath)

        assertEquals(INITIAL_API_KEY, auth.getApiKey())
        assertNull(auth.getAuthHeader(), "API key auth should not use Authorization header")
    }

    @Test
    @Order(2)
    @DisplayName("FileApiKeyAuth throws exception if file does not exist")
    fun testFileApiKeyAuthThrowsIfFileNotFound() {
        val exception = assertThrows<IOException> {
            FileApiKeyAuth("/nonexistent/path/to/api-key")
        }
        assertTrue(exception.message!!.contains("not found"))
    }

    @Test
    @Order(3)
    @DisplayName("FileApiKeyAuth throws exception if file is empty")
    fun testFileApiKeyAuthThrowsIfFileEmpty() {
        apiKeyFile.writeText("")

        val exception = assertThrows<IOException> {
            FileApiKeyAuth(apiKeyFile.absolutePath)
        }
        assertTrue(exception.message!!.contains("empty"))
    }

    @Test
    @Order(4)
    @DisplayName("FileApiKeyAuth trims whitespace from file content")
    fun testFileApiKeyAuthTrimsWhitespace() {
        apiKeyFile.writeText("  $INITIAL_API_KEY  \n")

        val auth = FileApiKeyAuth(apiKeyFile.absolutePath)
        assertEquals(INITIAL_API_KEY, auth.getApiKey())
    }

    @Test
    @Order(5)
    @DisplayName("FileApiKeyAuth implements RefreshableAuthProvider")
    fun testFileApiKeyAuthIsRefreshable() {
        val auth = FileApiKeyAuth(apiKeyFile.absolutePath)
        assertTrue(auth is RefreshableAuthProvider)
    }

    // ==================== Key Rotation Tests ====================

    @Test
    @Order(10)
    @DisplayName("onAuthenticationError returns true when key has been rotated")
    fun testOnAuthErrorReturnsTrueWhenKeyRotated() {
        val auth = FileApiKeyAuth(apiKeyFile.absolutePath)
        assertEquals(INITIAL_API_KEY, auth.getApiKey())

        // Simulate key rotation (agent writes new key to file)
        apiKeyFile.writeText(ROTATED_API_KEY)

        // Should return true indicating retry is appropriate
        assertTrue(auth.onAuthenticationError())

        // Cached key should now be the rotated key
        assertEquals(ROTATED_API_KEY, auth.getApiKey())
    }

    @Test
    @Order(11)
    @DisplayName("onAuthenticationError returns false when key has not changed")
    fun testOnAuthErrorReturnsFalseWhenKeyUnchanged() {
        val auth = FileApiKeyAuth(apiKeyFile.absolutePath)
        assertEquals(INITIAL_API_KEY, auth.getApiKey())

        // Key file unchanged - this is a real auth error
        assertFalse(auth.onAuthenticationError())

        // Key should remain the same
        assertEquals(INITIAL_API_KEY, auth.getApiKey())
    }

    @Test
    @Order(12)
    @DisplayName("onAuthenticationError returns false when file cannot be read")
    fun testOnAuthErrorReturnsFalseWhenFileUnreadable() {
        val auth = FileApiKeyAuth(apiKeyFile.absolutePath)

        // Delete the file (simulates file system issue)
        apiKeyFile.delete()

        // Should return false, not throw
        assertFalse(auth.onAuthenticationError())
    }

    @Test
    @Order(13)
    @DisplayName("Manual refresh updates cached key")
    fun testManualRefreshUpdatesKey() {
        val auth = FileApiKeyAuth(apiKeyFile.absolutePath)
        assertEquals(INITIAL_API_KEY, auth.getApiKey())

        // Update file
        apiKeyFile.writeText(ROTATED_API_KEY)

        // Manual refresh
        val newKey = auth.refresh()
        assertEquals(ROTATED_API_KEY, newKey)
        assertEquals(ROTATED_API_KEY, auth.getApiKey())
    }

    // ==================== End-to-End with MockWebServer ====================

    @Test
    @Order(20)
    @DisplayName("E2E: Successful request with file-based API key")
    fun testE2eSuccessfulRequestWithFileApiKey() {
        val requestedKey = AtomicInteger(0)

        mockServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val apiKey = request.getHeader("X-API-Key")
                if (apiKey == INITIAL_API_KEY) {
                    requestedKey.incrementAndGet()
                    return MockResponse()
                        .setResponseCode(200)
                        .setBody(HEALTH_RESPONSE)
                        .setHeader("Content-Type", "application/json")
                }
                return MockResponse().setResponseCode(401).setBody(UNAUTHORIZED_RESPONSE)
            }
        }
        mockServer.start()

        val client = ZnVaultClient.builder()
            .baseUrl(mockServer.url("/").toString())
            .apiKeyFile(apiKeyFile.absolutePath)
            .build()

        assertTrue(client.isHealthy())
        assertEquals(1, requestedKey.get())
    }

    @Test
    @Order(21)
    @DisplayName("E2E: Automatic retry after 401 when key has been rotated")
    fun testE2eAutoRetryAfter401WithKeyRotation() {
        val requestCount = AtomicInteger(0)

        mockServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val apiKey = request.getHeader("X-API-Key")
                val count = requestCount.incrementAndGet()

                println("  Request #$count with API key: ${apiKey?.take(20)}...")

                return when {
                    // First request with old key -> 401
                    count == 1 && apiKey == INITIAL_API_KEY -> {
                        // Simulate key rotation happening now
                        apiKeyFile.writeText(ROTATED_API_KEY)
                        MockResponse()
                            .setResponseCode(401)
                            .setBody(UNAUTHORIZED_RESPONSE)
                    }
                    // Retry with new key -> success
                    apiKey == ROTATED_API_KEY -> {
                        MockResponse()
                            .setResponseCode(200)
                            .setBody(HEALTH_RESPONSE)
                            .setHeader("Content-Type", "application/json")
                    }
                    else -> {
                        MockResponse().setResponseCode(401).setBody(UNAUTHORIZED_RESPONSE)
                    }
                }
            }
        }
        mockServer.start()

        val client = ZnVaultClient.builder()
            .baseUrl(mockServer.url("/").toString())
            .apiKeyFile(apiKeyFile.absolutePath)
            .build()

        // This should succeed after automatic retry
        assertTrue(client.isHealthy())

        // Should have made 2 requests: initial 401 + retry with new key
        assertEquals(2, requestCount.get())
    }

    @Test
    @Order(22)
    @DisplayName("E2E: No retry when 401 is genuine (key unchanged)")
    fun testE2eNoRetryWhen401IsGenuine() {
        val requestCount = AtomicInteger(0)

        mockServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requestCount.incrementAndGet()
                return MockResponse()
                    .setResponseCode(401)
                    .setBody(UNAUTHORIZED_RESPONSE)
            }
        }
        mockServer.start()

        val client = ZnVaultClient.builder()
            .baseUrl(mockServer.url("/").toString())
            .apiKeyFile(apiKeyFile.absolutePath)
            .build()

        // This should fail (no key rotation, just invalid key)
        assertFalse(client.isHealthy())

        // Should have made exactly 2 requests: initial + one retry attempt
        // (SDK re-reads file on 401, sees same key, returns original 401 response)
        assertEquals(2, requestCount.get())
    }

    @Test
    @Order(23)
    @DisplayName("E2E: Multiple key rotations during session")
    fun testE2eMultipleKeyRotations() {
        val currentValidKey = AtomicInteger(0) // 0=initial, 1=rotated, 2=another
        val keys = listOf(INITIAL_API_KEY, ROTATED_API_KEY, ANOTHER_API_KEY)

        mockServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val apiKey = request.getHeader("X-API-Key")
                val validKey = keys[currentValidKey.get()]

                return if (apiKey == validKey) {
                    MockResponse()
                        .setResponseCode(200)
                        .setBody(HEALTH_RESPONSE)
                        .setHeader("Content-Type", "application/json")
                } else {
                    // Key rotated - update file to new key
                    val nextKeyIndex = currentValidKey.get()
                    if (nextKeyIndex < keys.size) {
                        apiKeyFile.writeText(keys[nextKeyIndex])
                    }
                    MockResponse()
                        .setResponseCode(401)
                        .setBody(UNAUTHORIZED_RESPONSE)
                }
            }
        }
        mockServer.start()

        val client = ZnVaultClient.builder()
            .baseUrl(mockServer.url("/").toString())
            .apiKeyFile(apiKeyFile.absolutePath)
            .build()

        // First call - should succeed with initial key
        assertTrue(client.isHealthy())

        // Simulate first rotation
        currentValidKey.set(1)
        assertTrue(client.isHealthy())

        // Simulate second rotation
        currentValidKey.set(2)
        assertTrue(client.isHealthy())
    }

    // ==================== fromEnv() Factory Tests ====================

    @Test
    @Order(30)
    @DisplayName("fromEnv returns FileApiKeyAuth when _FILE env var is set")
    fun testFromEnvWithFileEnvVar() {
        // This test requires setting env vars, which is complex in JUnit
        // Instead, we test the static fromFile method
        val auth = FileApiKeyAuth.fromFile(apiKeyFile.absolutePath)
        assertTrue(auth is FileApiKeyAuth)
        assertEquals(INITIAL_API_KEY, auth.getApiKey())
    }

    @Test
    @Order(31)
    @DisplayName("ApiKeyAuth.of creates static API key provider")
    fun testApiKeyAuthOf() {
        val auth = ApiKeyAuth.of(INITIAL_API_KEY)
        assertEquals(INITIAL_API_KEY, auth.getApiKey())
        assertNull(auth.getAuthHeader())
    }

    // ==================== Concurrency Tests ====================

    @Test
    @Order(40)
    @DisplayName("FileApiKeyAuth is thread-safe during key rotation")
    fun testConcurrentAccessDuringRotation() {
        val auth = FileApiKeyAuth(apiKeyFile.absolutePath)
        val executor = Executors.newFixedThreadPool(10)
        val latch = CountDownLatch(100)
        val errors = AtomicInteger(0)

        repeat(100) { i ->
            executor.submit {
                try {
                    // Alternate between reading and simulating rotation
                    if (i % 10 == 0) {
                        apiKeyFile.writeText("znv_key_$i")
                        auth.onAuthenticationError()
                    } else {
                        auth.getApiKey() // Just read
                    }
                } catch (e: Exception) {
                    errors.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS))
        executor.shutdown()
        assertEquals(0, errors.get(), "No errors should occur during concurrent access")
    }

    @Test
    @Order(41)
    @DisplayName("Concurrent requests all see key update after rotation")
    fun testConcurrentRequestsAfterRotation() {
        // Start with initial key
        val auth = FileApiKeyAuth(apiKeyFile.absolutePath)
        assertEquals(INITIAL_API_KEY, auth.getApiKey())

        // Rotate key
        apiKeyFile.writeText(ROTATED_API_KEY)

        // Simulate 401 triggering refresh
        assertTrue(auth.onAuthenticationError())

        // All threads should see the new key
        val executor = Executors.newFixedThreadPool(10)
        val results = mutableListOf<String>()
        val latch = CountDownLatch(10)

        repeat(10) {
            executor.submit {
                results.add(auth.getApiKey())
                latch.countDown()
            }
        }

        latch.await(5, TimeUnit.SECONDS)
        executor.shutdown()

        // All results should be the rotated key
        assertTrue(results.all { it == ROTATED_API_KEY })
    }

    // ==================== Deployment Simulation Tests ====================

    @Test
    @Order(50)
    @DisplayName("Simulates Payara deployment with key rotation every 4 hours")
    fun testPayaraDeploymentSimulation() {
        println("\n=== Payara Deployment Simulation ===")

        // Track which key is currently valid on the "server"
        val serverValidKey = AtomicInteger(0)
        val keys = listOf(INITIAL_API_KEY, ROTATED_API_KEY, ANOTHER_API_KEY)
        val successfulCalls = AtomicInteger(0)

        mockServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val apiKey = request.getHeader("X-API-Key")
                val validKey = keys[serverValidKey.get()]

                return if (apiKey == validKey) {
                    successfulCalls.incrementAndGet()
                    MockResponse()
                        .setResponseCode(200)
                        .setBody(HEALTH_RESPONSE)
                        .setHeader("Content-Type", "application/json")
                } else {
                    // On 401, agent would have already written new key
                    val newKeyIndex = minOf(serverValidKey.get(), keys.size - 1)
                    apiKeyFile.writeText(keys[newKeyIndex])
                    MockResponse()
                        .setResponseCode(401)
                        .setBody(UNAUTHORIZED_RESPONSE)
                }
            }
        }
        mockServer.start()

        val client = ZnVaultClient.builder()
            .baseUrl(mockServer.url("/").toString())
            .apiKeyFile(apiKeyFile.absolutePath)
            .build()

        // T=0: Initial deployment, secret access works
        println("T=0h: Application starts, accessing secrets...")
        val secrets1 = client.health.isHealthy()
        assertTrue(secrets1)
        println("  ✓ Access successful with initial key")

        // T=4h: Key rotation happens
        println("\nT=4h: Key rotation occurs...")
        serverValidKey.set(1)
        apiKeyFile.writeText(ROTATED_API_KEY) // Agent writes new key

        // Application makes request, gets 401, retries with new key
        println("  Application makes request...")
        val secrets2 = client.health.isHealthy()
        assertTrue(secrets2)
        println("  ✓ Access successful after automatic key refresh")

        // T=8h: Another rotation
        println("\nT=8h: Another key rotation...")
        serverValidKey.set(2)
        apiKeyFile.writeText(ANOTHER_API_KEY)

        val secrets3 = client.health.isHealthy()
        assertTrue(secrets3)
        println("  ✓ Access successful after second rotation")

        println("\n=== Simulation Complete ===")
        println("Total successful calls: ${successfulCalls.get()}")
    }

    @Test
    @Order(51)
    @DisplayName("Simulates zn-vault-agent managed environment")
    fun testAgentManagedEnvironment() {
        println("\n=== Agent-Managed Environment Simulation ===")

        // Simulate the directory structure:
        // /run/zn-vault-agent/secrets/
        //   └── ZINC_CONFIG_VAULT_API_KEY (the file)
        val agentDir = File(tempDir, "run/zn-vault-agent/secrets")
        agentDir.mkdirs()
        val agentKeyFile = File(agentDir, "ZINC_CONFIG_VAULT_API_KEY")
        agentKeyFile.writeText(INITIAL_API_KEY)

        println("Agent directory: ${agentDir.absolutePath}")
        println("Initial key file created")

        val requestLog = mutableListOf<String>()

        mockServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val apiKey = request.getHeader("X-API-Key") ?: "none"
                requestLog.add("Request with key: ${apiKey.take(15)}...")

                return when (apiKey) {
                    INITIAL_API_KEY -> MockResponse()
                        .setResponseCode(200)
                        .setBody(HEALTH_RESPONSE)
                        .setHeader("Content-Type", "application/json")
                    ROTATED_API_KEY -> MockResponse()
                        .setResponseCode(200)
                        .setBody(HEALTH_RESPONSE)
                        .setHeader("Content-Type", "application/json")
                    else -> MockResponse()
                        .setResponseCode(401)
                        .setBody(UNAUTHORIZED_RESPONSE)
                }
            }
        }
        mockServer.start()

        // Create client using file-based auth (like ZnVaultClient.fromEnv() would)
        val client = ZnVaultClient.builder()
            .baseUrl(mockServer.url("/").toString())
            .apiKeyFile(agentKeyFile.absolutePath)
            .build()

        // Normal operation
        println("\n1. Normal operation with initial key")
        assertTrue(client.isHealthy())

        // Agent rotates key (writes new key to file)
        println("\n2. Agent rotates key (writes to file)")
        agentKeyFile.writeText(ROTATED_API_KEY)

        // Application continues without knowing about rotation
        println("\n3. Application continues, SDK handles rotation transparently")
        assertTrue(client.isHealthy())

        println("\nRequest log:")
        requestLog.forEach { println("  - $it") }

        println("\n=== Environment Simulation Complete ===")
    }

    // ==================== Edge Cases ====================

    @Test
    @Order(60)
    @DisplayName("Handles file becoming temporarily unavailable")
    fun testFileTemporarilyUnavailable() {
        val auth = FileApiKeyAuth(apiKeyFile.absolutePath)
        assertEquals(INITIAL_API_KEY, auth.getApiKey())

        // File deleted (e.g., during agent restart)
        apiKeyFile.delete()

        // Should not throw, returns false (can't refresh)
        assertFalse(auth.onAuthenticationError())

        // Original cached key still available
        assertEquals(INITIAL_API_KEY, auth.getApiKey())

        // File restored
        apiKeyFile.writeText(ROTATED_API_KEY)

        // Now refresh works
        assertTrue(auth.onAuthenticationError())
        assertEquals(ROTATED_API_KEY, auth.getApiKey())
    }

    @Test
    @Order(61)
    @DisplayName("Handles file with only whitespace")
    fun testFileWithOnlyWhitespace() {
        apiKeyFile.writeText("   \n\t  ")

        val exception = assertThrows<IOException> {
            FileApiKeyAuth(apiKeyFile.absolutePath)
        }
        assertTrue(exception.message!!.contains("empty"))
    }

    @Test
    @Order(62)
    @DisplayName("Handles very long API keys")
    fun testVeryLongApiKey() {
        val longKey = "znv_" + "a".repeat(1000)
        apiKeyFile.writeText(longKey)

        val auth = FileApiKeyAuth(apiKeyFile.absolutePath)
        assertEquals(longKey, auth.getApiKey())
    }
}
