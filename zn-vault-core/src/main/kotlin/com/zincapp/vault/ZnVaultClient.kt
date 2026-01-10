// Path: zn-vault-sdk-jvm/zn-vault-core/src/main/kotlin/com/zincapp/vault/ZnVaultClient.kt
package com.zincapp.vault

import com.zincapp.vault.admin.AdminClient
import com.zincapp.vault.audit.AuditClient
import com.zincapp.vault.auth.ApiKeyAuth
import com.zincapp.vault.auth.AuthClient
import com.zincapp.vault.auth.FileApiKeyAuth
import com.zincapp.vault.auth.TokenManager
import com.zincapp.vault.auth.TokenManagerConfig
import com.zincapp.vault.health.HealthClient
import com.zincapp.vault.http.AuthProvider
import com.zincapp.vault.http.HttpClientConfig
import com.zincapp.vault.http.RetryPolicy
import com.zincapp.vault.http.TlsConfig
import com.zincapp.vault.http.ZnVaultHttpClient
import com.zincapp.vault.kms.KmsClient
import com.zincapp.vault.models.LoginResponse
import com.zincapp.vault.secrets.SecretClient
import java.time.Duration

/**
 * Main client for interacting with ZnVault.
 *
 * This is the entry point for all ZnVault operations. Create an instance
 * using the builder pattern and then access the various client modules.
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
 *     tenant = "acme",
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
 * client.login("admin", "password")
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

    /** Admin operations (tenants, users, roles, policies) */
    val admin: AdminClient = AdminClient(httpClient)

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
        private var baseUrl: String = DEFAULT_BASE_URL
        private var apiKey: String? = null
        private var apiKeyFilePath: String? = null
        private var apiKeyEnvName: String? = null
        private var connectTimeout: Duration = Duration.ofSeconds(30)
        private var readTimeout: Duration = Duration.ofSeconds(30)
        private var writeTimeout: Duration = Duration.ofSeconds(30)
        private var retryPolicy: RetryPolicy = RetryPolicy.default()
        private var tlsConfig: TlsConfig? = null
        private var debug: Boolean = false
        private var tokenConfig: TokenManagerConfig = TokenManagerConfig()

        /**
         * Set the base URL for the ZnVault server.
         *
         * @param url Base URL (e.g., "https://vault.example.com:8443")
         */
        fun baseUrl(url: String) = apply {
            this.baseUrl = url.trimEnd('/')
        }

        /**
         * Set API key for authentication.
         *
         * When set, the client will use API key authentication instead of JWT.
         * For automatic key rotation support, use [apiKeyFile] or [apiKeyFromEnv] instead.
         *
         * @param key API key (e.g., "znv_xxxx_secretkey")
         */
        fun apiKey(key: String) = apply {
            this.apiKey = key
            this.apiKeyFilePath = null
            this.apiKeyEnvName = null
        }

        /**
         * Set API key file path for authentication with automatic refresh.
         *
         * The API key will be read from the specified file. When a 401 error
         * occurs, the file will be re-read and the request retried if the
         * key has changed. This supports automatic key rotation by zn-vault-agent.
         *
         * @param filePath Path to file containing the API key
         */
        fun apiKeyFile(filePath: String) = apply {
            this.apiKeyFilePath = filePath
            this.apiKey = null
            this.apiKeyEnvName = null
        }

        /**
         * Configure API key from environment variables with automatic file detection.
         *
         * Checks for credentials in this order:
         * 1. `{envName}_FILE` - Path to file containing API key (preferred)
         * 2. `{envName}` - Direct API key value (fallback)
         *
         * When using file mode, supports automatic key rotation.
         *
         * @param envName Base environment variable name (e.g., "VAULT_API_KEY")
         */
        fun apiKeyFromEnv(envName: String) = apply {
            this.apiKeyEnvName = envName
            this.apiKey = null
            this.apiKeyFilePath = null
        }

        /**
         * Set connection timeout.
         *
         * @param timeout Connection timeout duration
         */
        fun connectTimeout(timeout: Duration) = apply {
            this.connectTimeout = timeout
        }

        /**
         * Set read timeout.
         *
         * @param timeout Read timeout duration
         */
        fun readTimeout(timeout: Duration) = apply {
            this.readTimeout = timeout
        }

        /**
         * Set write timeout.
         *
         * @param timeout Write timeout duration
         */
        fun writeTimeout(timeout: Duration) = apply {
            this.writeTimeout = timeout
        }

        /**
         * Set all timeouts to the same value.
         *
         * @param timeout Timeout duration for connect, read, and write
         */
        fun timeout(timeout: Duration) = apply {
            this.connectTimeout = timeout
            this.readTimeout = timeout
            this.writeTimeout = timeout
        }

        /**
         * Set retry policy.
         *
         * @param policy Retry policy configuration
         */
        fun retryPolicy(policy: RetryPolicy) = apply {
            this.retryPolicy = policy
        }

        /**
         * Disable retries.
         */
        fun noRetries() = apply {
            this.retryPolicy = RetryPolicy.none()
        }

        /**
         * Set TLS configuration.
         *
         * Use this for custom CA certificates or client certificate (mTLS).
         *
         * @param config TLS configuration
         */
        fun tlsConfig(config: TlsConfig) = apply {
            this.tlsConfig = config
        }

        /**
         * Use insecure TLS (trust all certificates).
         *
         * WARNING: Only use this for development!
         */
        fun insecureTls() = apply {
            this.tlsConfig = TlsConfig.insecure()
        }

        /**
         * Set CA certificate for server verification.
         *
         * @param path Path to CA certificate file (PEM format)
         */
        fun caCertificate(path: String) = apply {
            this.tlsConfig = TlsConfig.builder()
                .caCertificate(path)
                .build()
        }

        /**
         * Enable debug logging.
         */
        fun debug(enabled: Boolean = true) = apply {
            this.debug = enabled
        }

        /**
         * Configure token manager settings.
         *
         * @param config Token manager configuration
         */
        fun tokenConfig(config: TokenManagerConfig) = apply {
            this.tokenConfig = config
        }

        /**
         * Build the ZnVaultClient.
         *
         * @return Configured ZnVaultClient instance
         */
        fun build(): ZnVaultClient {
            val url = baseUrl

            val httpConfig = HttpClientConfig(
                connectTimeout = connectTimeout,
                readTimeout = readTimeout,
                writeTimeout = writeTimeout,
                retryPolicy = retryPolicy,
                tlsConfig = tlsConfig,
                debug = debug
            )

            val httpClient = ZnVaultHttpClient(url, httpConfig)

            // Set up authentication based on configuration
            val authProvider: AuthProvider? = when {
                // Priority 1: Direct API key
                apiKey != null -> ApiKeyAuth(apiKey!!)

                // Priority 2: API key from file (with refresh support)
                apiKeyFilePath != null -> FileApiKeyAuth(apiKeyFilePath!!)

                // Priority 3: API key from environment (auto-detects file mode)
                apiKeyEnvName != null -> FileApiKeyAuth.fromEnv(apiKeyEnvName!!)

                // No API key configured - will use JWT
                else -> null
            }

            val tokenManager: TokenManager?
            val effectiveApiKey: String?

            if (authProvider != null) {
                // Use API key authentication
                tokenManager = null
                effectiveApiKey = authProvider.getApiKey()
                httpClient.setAuthProvider(authProvider)
            } else {
                // Use JWT token authentication
                tokenManager = TokenManager(httpClient, tokenConfig)
                effectiveApiKey = null
                httpClient.setAuthProvider(tokenManager)
            }

            return ZnVaultClient(httpClient, tokenManager, effectiveApiKey)
        }
    }
}
