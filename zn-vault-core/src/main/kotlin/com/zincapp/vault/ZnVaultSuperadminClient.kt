// Path: zn-vault-sdk-jvm/zn-vault-core/src/main/kotlin/com/zincapp/vault/ZnVaultSuperadminClient.kt
package com.zincapp.vault

import com.zincapp.vault.admin.TenantClient
import com.zincapp.vault.audit.SuperadminAuditClient
import com.zincapp.vault.auth.SuperadminAuthClient
import com.zincapp.vault.auth.TokenManager
import com.zincapp.vault.auth.TokenManagerConfig
import com.zincapp.vault.health.HealthClient
import com.zincapp.vault.http.RetryPolicy
import com.zincapp.vault.http.TlsConfig
import com.zincapp.vault.http.ZnVaultHttpClient
import com.zincapp.vault.internal.ClientCoreConfig
import com.zincapp.vault.models.LoginResponse
import java.time.Duration

/**
 * Superadmin-scoped client for cross-tenant administrative operations.
 *
 * Use this client only with a superadmin principal. For tenant operations
 * (encrypt/decrypt, secrets, your own tenant's users/roles/policies), use
 * [ZnVaultClient].
 *
 * The split is intentional: a service like zn-api that holds a tenant-scoped
 * API key cannot compile against the superadmin surface, so it can't
 * accidentally call cross-tenant operations even if it wanted to.
 *
 * **Note:** the cross-tenant API key and registration token operations on
 * [auth] (a [SuperadminAuthClient]) target routes under
 * `/v1/superadmin/api-keys/` which are not yet implemented on the vault
 * server (as of 1.38.8). Those calls will 404 until the server adds them.
 *
 * ## Example
 * ```kotlin
 * val admin = ZnVaultSuperadminClient.builder()
 *     .baseUrl("https://vault.example.com:8443")
 *     .apiKey("znv_superadmin_xxx")
 *     .build()
 *
 * val tenant = admin.tenants.create(...)
 * ```
 */
class ZnVaultSuperadminClient private constructor(
    private val httpClient: ZnVaultHttpClient,
    private val tokenManager: TokenManager?,
    private val apiKey: String?
) {
    /** Tenant CRUD (superadmin only). */
    val tenants: TenantClient = TenantClient(httpClient)

    /**
     * Cross-tenant API key / managed-key / registration-token management.
     *
     * Calls routes under `/v1/superadmin/api-keys/` — not yet implemented on
     * the server (will 404 until those routes ship).
     */
    val auth: SuperadminAuthClient = SuperadminAuthClient(httpClient)

    /**
     * Cross-tenant audit log queries via `/v1/superadmin/audit*`. Use
     * [SuperadminAuditFilter.tenantId][com.zincapp.vault.models.SuperadminAuditFilter.tenantId]
     * to scope a query to a specific tenant.
     */
    val audit: SuperadminAuditClient = SuperadminAuditClient(httpClient)

    /** Health check operations. */
    val health: HealthClient = HealthClient(httpClient)

    /** Login with username and password. */
    fun login(username: String, password: String, totpCode: String? = null): LoginResponse {
        requireNotNull(tokenManager) {
            "TokenManager not available. Use builder without apiKey to enable login."
        }
        return tokenManager.login(username, password, totpCode)
    }

    /** Logout and clear tokens. */
    fun logout() {
        tokenManager?.logout()
    }

    /** True if authenticated with JWT or API key. */
    fun isAuthenticated(): Boolean =
        apiKey != null || tokenManager?.isAuthenticated() == true

    /** True if the vault is healthy. */
    fun isHealthy(): Boolean = health.isHealthy()

    companion object {
        const val DEFAULT_BASE_URL = "https://vault.zincapp.com"

        @JvmStatic
        fun builder(): Builder = Builder()

        @JvmStatic
        fun withApiKey(baseUrl: String, apiKey: String): ZnVaultSuperadminClient =
            builder().baseUrl(baseUrl).apiKey(apiKey).build()
    }

    /** Builder for [ZnVaultSuperadminClient]. Same configuration surface as [ZnVaultClient.Builder]. */
    class Builder {
        private val config = ClientCoreConfig(baseUrl = DEFAULT_BASE_URL)

        fun baseUrl(url: String) = apply { config.baseUrl = url.trimEnd('/') }

        fun apiKey(key: String) = apply {
            config.apiKey = key
            config.apiKeyFilePath = null
            config.apiKeyEnvName = null
        }

        fun apiKeyFile(filePath: String) = apply {
            config.apiKeyFilePath = filePath
            config.apiKey = null
            config.apiKeyEnvName = null
        }

        fun apiKeyFromEnv(envName: String) = apply {
            config.apiKeyEnvName = envName
            config.apiKey = null
            config.apiKeyFilePath = null
        }

        fun connectTimeout(timeout: Duration) = apply { config.connectTimeout = timeout }
        fun readTimeout(timeout: Duration) = apply { config.readTimeout = timeout }
        fun writeTimeout(timeout: Duration) = apply { config.writeTimeout = timeout }
        fun timeout(timeout: Duration) = apply {
            config.connectTimeout = timeout
            config.readTimeout = timeout
            config.writeTimeout = timeout
        }
        fun retryPolicy(policy: RetryPolicy) = apply { config.retryPolicy = policy }
        fun noRetries() = apply { config.retryPolicy = RetryPolicy.none() }
        fun tlsConfig(config: TlsConfig) = apply { this.config.tlsConfig = config }
        fun insecureTls() = apply { config.tlsConfig = TlsConfig.insecure() }
        fun caCertificate(path: String) = apply {
            config.tlsConfig = TlsConfig.builder().caCertificate(path).build()
        }
        fun debug(enabled: Boolean = true) = apply { config.debug = enabled }
        fun tokenConfig(config: TokenManagerConfig) = apply { this.config.tokenConfig = config }

        fun build(): ZnVaultSuperadminClient {
            val core = config.buildCore()
            return ZnVaultSuperadminClient(core.httpClient, core.tokenManager, core.apiKey)
        }
    }
}
