// Path: zn-vault-sdk-jvm/zn-vault-core/src/main/kotlin/com/zincapp/vault/ZnVaultClient.kt
package com.zincapp.vault

import com.zincapp.vault.admin.PolicyClient
import com.zincapp.vault.admin.RoleClient
import com.zincapp.vault.admin.UserClient
import com.zincapp.vault.audit.AuditClient
import com.zincapp.vault.auth.AuthClient
import com.zincapp.vault.auth.TokenManager
import com.zincapp.vault.auth.TokenManagerConfig
import com.zincapp.vault.certificates.CertificateClient
import com.zincapp.vault.health.HealthClient
import com.zincapp.vault.http.RetryPolicy
import com.zincapp.vault.http.TlsConfig
import com.zincapp.vault.http.ZnVaultHttpClient
import com.zincapp.vault.internal.ClientCoreConfig
import com.zincapp.vault.kms.KmsClient
import com.zincapp.vault.models.LoginResponse
import com.zincapp.vault.secrets.SecretClient
import java.time.Duration

/**
 * Tenant-scoped client for interacting with ZnVault.
 *
 * This is the entry point for tenant operations. The tenant is always
 * derived from the authenticated principal (the API key or JWT). There is
 * no client-supplied tenant on any request.
 *
 * For cross-tenant administrative operations (managing tenants, users,
 * roles, policies across tenants), use [ZnVaultSuperadminClient].
 *
 * ## Example Usage
 *
 * ### With API Key (recommended for services)
 * ```kotlin
 * val client = ZnVaultClient.builder()
 *     .baseUrl("https://vault.example.com:8443")
 *     .apiKey("znv_xxxx_secretkey")
 *     .build()
 *
 * val secret = client.secrets.create(
 *     alias = "api/production/db-creds",
 *     type = SecretType.CREDENTIAL,
 *     data = mapOf("username" to "admin", "password" to "secret")
 * )
 * ```
 *
 * ### With Username/Password
 * ```kotlin
 * val client = ZnVaultClient.builder()
 *     .baseUrl("https://vault.example.com:8443")
 *     .build()
 *
 * client.login("acme/admin", "password")
 * val secrets = client.secrets.list()
 * ```
 */
class ZnVaultClient private constructor(
    private val httpClient: ZnVaultHttpClient,
    private val tokenManager: TokenManager?,
    private val apiKey: String?
) {
    /** Authentication operations (register, API keys, 2FA) */
    val auth: AuthClient = AuthClient(httpClient)

    /** Secret management operations */
    val secrets: SecretClient = SecretClient(httpClient)

    /** Key Management Service operations */
    val kms: KmsClient = KmsClient(httpClient)

    /**
     * Certificate store operations (store, list, decrypt).
     *
     * [CertificateClient] has always been public, but its constructor is `internal`, so before
     * this accessor existed the class was unreachable from outside the SDK — a caller had to build
     * a second [ZnVaultHttpClient] of its own and risk drifting from this client's base URL, TLS
     * and credentials.
     */
    val certificates: CertificateClient = CertificateClient(httpClient)

    /** Tenant-scoped user management (your tenant only). */
    val users: UserClient = UserClient(httpClient)

    /** Tenant-scoped role management (RBAC, your tenant only). */
    val roles: RoleClient = RoleClient(httpClient)

    /** Tenant-scoped policy management (ABAC, your tenant only). */
    val policies: PolicyClient = PolicyClient(httpClient)

    /** Audit log operations */
    val audit: AuditClient = AuditClient(httpClient)

    /** Health check operations */
    val health: HealthClient = HealthClient(httpClient)

    /**
     * Login with username and password.
     *
     * The username must include the tenant prefix in the format `tenant/username`
     * (e.g., "acme/admin"). This allows multiple tenants to have users with the
     * same username. Email addresses can also be used as username.
     *
     * After successful login, the client will automatically use the JWT token
     * for subsequent requests and refresh it before expiry.
     *
     * @param username Username in format "tenant/username" or email
     * @param password Password
     * @param totpCode Optional TOTP code for 2FA
     * @return Login response with tokens
     */
    fun login(username: String, password: String, totpCode: String? = null): LoginResponse {
        requireNotNull(tokenManager) {
            "TokenManager not available. Use builder without apiKey to enable login."
        }
        return tokenManager.login(username, password, totpCode)
    }

    /**
     * Login with tenant and username as separate parameters.
     *
     * Convenience method that formats the username as "tenant/username".
     *
     * @param tenant Tenant identifier (e.g., "acme")
     * @param username Username within the tenant (e.g., "admin")
     * @param password Password
     * @param totpCode Optional TOTP code for 2FA
     * @return Login response with tokens
     */
    fun login(tenant: String, username: String, password: String, totpCode: String? = null): LoginResponse {
        requireNotNull(tokenManager) {
            "TokenManager not available. Use builder without apiKey to enable login."
        }
        return tokenManager.login(tenant, username, password, totpCode)
    }

    /**
     * Logout and clear tokens.
     */
    fun logout() {
        tokenManager?.logout()
    }

    /**
     * Check if the client is authenticated.
     *
     * @return true if authenticated with JWT or API key
     */
    fun isAuthenticated(): Boolean {
        return apiKey != null || tokenManager?.isAuthenticated() == true
    }

    /**
     * Check if service is healthy.
     */
    fun isHealthy(): Boolean = health.isHealthy()

    companion object {
        /**
         * Default base URL for ZnVault.
         */
        const val DEFAULT_BASE_URL = "https://vault.zincapp.com"

        /**
         * Default environment variable name for the API key.
         */
        const val DEFAULT_API_KEY_ENV = "ZINC_CONFIG_VAULT_API_KEY"

        /**
         * Default environment variable name for the vault URL.
         */
        const val DEFAULT_URL_ENV = "ZINC_CONFIG_VAULT_URL"

        /**
         * Create a new builder for ZnVaultClient.
         */
        @JvmStatic
        fun builder(): Builder = Builder()

        /**
         * Create a client with default base URL.
         *
         * You'll need to call login() before making authenticated requests.
         */
        @JvmStatic
        fun create(): ZnVaultClient = builder().build()

        /**
         * Create a simple client with a custom base URL.
         *
         * You'll need to call login() before making authenticated requests.
         */
        @JvmStatic
        fun create(baseUrl: String): ZnVaultClient = builder()
            .baseUrl(baseUrl)
            .build()

        /**
         * Create a client with API key authentication using default base URL.
         */
        @JvmStatic
        fun withApiKey(apiKey: String): ZnVaultClient = builder()
            .apiKey(apiKey)
            .build()

        /**
         * Create a client with API key authentication.
         */
        @JvmStatic
        fun withApiKey(baseUrl: String, apiKey: String): ZnVaultClient = builder()
            .baseUrl(baseUrl)
            .apiKey(apiKey)
            .build()

        /**
         * Create a client from environment variables with automatic credential refresh.
         *
         * This is the recommended way to create a client in applications managed
         * by zn-vault-agent, as it supports automatic key rotation.
         *
         * ## Environment Variables
         *
         * The method checks for credentials in this order:
         *
         * 1. `ZINC_CONFIG_VAULT_API_KEY_FILE` - Path to file containing API key (preferred)
         * 2. `ZINC_CONFIG_VAULT_API_KEY` - Direct API key value (fallback)
         *
         * For the vault URL:
         * 1. `ZINC_CONFIG_VAULT_URL` - Vault server URL
         * 2. Falls back to default URL if not set
         *
         * ## Key Rotation Support
         *
         * When using `_FILE` mode, the client automatically handles key rotation:
         * - On 401 errors, re-reads the API key from the file
         * - If the key changed (rotated by agent), retries the request
         * - This is transparent to the application
         *
         * ## Example
         *
         * ```kotlin
         * // Agent injects:
         * //   ZINC_CONFIG_VAULT_URL=https://vault.example.com
         * //   ZINC_CONFIG_VAULT_API_KEY_FILE=/run/zn-vault-agent/secrets/ZINC_CONFIG_VAULT_API_KEY
         *
         * val client = ZnVaultClient.fromEnv()
         * val secret = client.secrets.get("my-secret")  // Auto-refreshes on key rotation
         * ```
         *
         * @return ZnVaultClient configured from environment
         * @throws IllegalStateException if required environment variables are not set
         */
        @JvmStatic
        fun fromEnv(): ZnVaultClient {
            // Check env var first, then system property (for Payara -D options)
            val baseUrl = System.getenv(DEFAULT_URL_ENV)
                ?: System.getProperty(DEFAULT_URL_ENV)
                ?: DEFAULT_BASE_URL
            return builder()
                .baseUrl(baseUrl)
                .apiKeyFromEnv(DEFAULT_API_KEY_ENV)
                .build()
        }

        /**
         * Create a client from custom environment variable names.
         *
         * @param urlEnvName Environment variable name for vault URL
         * @param apiKeyEnvName Environment variable name for API key (checks _FILE suffix first)
         * @return ZnVaultClient configured from environment
         * @throws IllegalStateException if required environment variables are not set
         */
        @JvmStatic
        fun fromEnv(urlEnvName: String, apiKeyEnvName: String): ZnVaultClient {
            // Check env var first, then system property (for Payara -D options)
            val baseUrl = System.getenv(urlEnvName)
                ?: System.getProperty(urlEnvName)
                ?: throw IllegalStateException("Environment variable or system property $urlEnvName not set")
            return builder()
                .baseUrl(baseUrl)
                .apiKeyFromEnv(apiKeyEnvName)
                .build()
        }
    }

    /**
     * Builder for ZnVaultClient.
     */
    class Builder {
        private val config = ClientCoreConfig(baseUrl = DEFAULT_BASE_URL)

        /** Set the base URL for the ZnVault server. */
        fun baseUrl(url: String) = apply { config.baseUrl = url.trimEnd('/') }

        /**
         * Set API key for authentication. When set, the client uses API key
         * authentication instead of JWT. For automatic key rotation support,
         * use [apiKeyFile] or [apiKeyFromEnv] instead.
         */
        fun apiKey(key: String) = apply {
            config.apiKey = key
            config.apiKeyFilePath = null
            config.apiKeyEnvName = null
        }

        /** Set API key file path for authentication with automatic refresh. */
        fun apiKeyFile(filePath: String) = apply {
            config.apiKeyFilePath = filePath
            config.apiKey = null
            config.apiKeyEnvName = null
        }

        /** Configure API key from environment variables with file-mode detection. */
        fun apiKeyFromEnv(envName: String) = apply {
            config.apiKeyEnvName = envName
            config.apiKey = null
            config.apiKeyFilePath = null
        }

        fun connectTimeout(timeout: Duration) = apply { config.connectTimeout = timeout }
        fun readTimeout(timeout: Duration) = apply { config.readTimeout = timeout }
        fun writeTimeout(timeout: Duration) = apply { config.writeTimeout = timeout }

        /** Set all timeouts to the same value. */
        fun timeout(timeout: Duration) = apply {
            config.connectTimeout = timeout
            config.readTimeout = timeout
            config.writeTimeout = timeout
        }

        fun retryPolicy(policy: RetryPolicy) = apply { config.retryPolicy = policy }
        fun noRetries() = apply { config.retryPolicy = RetryPolicy.none() }
        fun tlsConfig(config: TlsConfig) = apply { this.config.tlsConfig = config }

        /** Use insecure TLS (trust all certificates). WARNING: development only. */
        fun insecureTls() = apply { config.tlsConfig = TlsConfig.insecure() }

        /** Set CA certificate for server verification. */
        fun caCertificate(path: String) = apply {
            config.tlsConfig = TlsConfig.builder().caCertificate(path).build()
        }

        fun debug(enabled: Boolean = true) = apply { config.debug = enabled }
        fun tokenConfig(config: TokenManagerConfig) = apply { this.config.tokenConfig = config }

        /** Build the ZnVaultClient. */
        fun build(): ZnVaultClient {
            val core = config.buildCore()
            return ZnVaultClient(core.httpClient, core.tokenManager, core.apiKey)
        }
    }
}
