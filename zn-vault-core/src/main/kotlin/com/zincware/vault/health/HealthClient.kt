// Path: zn-vault-sdk-jvm/zn-vault-core/src/main/kotlin/com/zincware/vault/health/HealthClient.kt
package com.zincware.vault.health

import com.zincware.vault.http.ZnVaultHttpClient
import com.zincware.vault.models.HealthStatus

/**
 * Client for health check operations.
 */
class HealthClient internal constructor(
    private val httpClient: ZnVaultHttpClient
) {

    /**
     * Get full health status.
     *
     * Includes database and TLS health information.
     *
     * @return Health status
     */
    fun getHealth(): HealthStatus {
        return httpClient.get("/v1/health", HealthStatus::class.java)
    }

    /**
     * Check if the service is healthy.
     *
     * @return true if all components are healthy
     */
    fun isHealthy(): Boolean {
        return try {
            val status = getHealth()
            status.status == "healthy" || status.status == "ok"
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Kubernetes liveness probe.
     *
     * A simple check that the service is running.
     *
     * @return true if service is alive
     */
    fun isLive(): Boolean {
        return try {
            httpClient.get("/v1/health/live", HealthStatus::class.java)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Kubernetes readiness probe.
     *
     * Checks if the service is ready to accept traffic
     * (database connection healthy).
     *
     * @return true if service is ready
     */
    fun isReady(): Boolean {
        return try {
            val status = httpClient.get("/v1/health/ready", HealthStatus::class.java)
            status.status == "ready" || status.status == "ok"
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Simple connectivity test.
     *
     * @return true if ping succeeds
     */
    fun ping(): Boolean {
        return try {
            // Use raw text response (not JSON)
            httpClient.getRaw("/ping") == "pong"
        } catch (e: Exception) {
            false
        }
    }
}
