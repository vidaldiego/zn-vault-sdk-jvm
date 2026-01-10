// Path: zn-vault-core/src/test/kotlin/com/zincapp/vault/integration/HealthTest.kt
package com.zincapp.vault.integration

import com.zincapp.vault.ZnVaultClient
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

/**
 * Integration tests for health check functionality.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class HealthTest : BaseIntegrationTest() {

    override fun createClient(): ZnVaultClient {
        // Health checks don't require authentication
        return TestConfig.createTestClient()
    }

    @Test
    @Order(1)
    @DisplayName("Ping returns true when server is up")
    fun testPing() {
        val result = client.health.ping()

        assertTrue(result)
        println("✓ Ping successful")
    }

    @Test
    @Order(2)
    @DisplayName("Get full health status")
    fun testGetHealth() {
        val health = client.health.getHealth()

        assertNotNull(health)
        assertNotNull(health.status)
        println("✓ Health status: ${health.status}")
        health.checks?.db?.let { println("  - Database: ${it.status}") }
        health.checks?.tls?.let { println("  - TLS: ${it.status}") }
        health.version?.let { println("  - Version: $it") }
        health.environment?.let { println("  - Environment: $it") }
    }

    @Test
    @Order(3)
    @DisplayName("isHealthy returns true when server is healthy")
    fun testIsHealthy() {
        val healthy = client.health.isHealthy()

        assertTrue(healthy)
        println("✓ Server is healthy")
    }

    @Test
    @Order(4)
    @DisplayName("Liveness probe returns true")
    fun testIsLive() {
        val live = client.health.isLive()

        assertTrue(live)
        println("✓ Liveness probe passed")
    }

    @Test
    @Order(5)
    @DisplayName("Readiness probe returns true when ready")
    fun testIsReady() {
        val ready = client.health.isReady()

        assertTrue(ready)
        println("✓ Readiness probe passed")
    }

    @Test
    @Order(6)
    @DisplayName("Client isHealthy convenience method works")
    fun testClientIsHealthy() {
        val healthy = client.isHealthy()

        assertTrue(healthy)
        println("✓ Client.isHealthy() works")
    }
}
