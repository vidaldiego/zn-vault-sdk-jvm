// Path: zn-vault-sdk-jvm/zn-vault-core/src/main/kotlin/com/zincware/vault/ZnVaultClient.kt
package com.zincware.vault

import com.zincware.vault.admin.AdminClient
import com.zincware.vault.audit.AuditClient
import com.zincware.vault.auth.ApiKeyAuth
import com.zincware.vault.auth.AuthClient
import com.zincware.vault.auth.TokenManager
import com.zincware.vault.auth.TokenManagerConfig
import com.zincware.vault.certificates.CertificateClient
import com.zincware.vault.health.HealthClient
import com.zincware.vault.http.HttpClientConfig
import com.zincware.vault.http.RetryPolicy
import com.zincware.vault.http.TlsConfig
import com.zincware.vault.http.ZnVaultHttpClient
import com.zincware.vault.kms.KmsClient
import com.zincware.vault.models.LoginResponse
import com.zincware.vault.secrets.SecretClient
import java.time.Duration

/**
 * Main client for interacting with ZN-Vault.
 *
 * This is the entry point for all ZN-Vault operations. Create an instance
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

    /** Certificate lifecycle management operations */
    val certificates: CertificateClient = CertificateClient(httpClient)

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
         * Default base URL for ZN-Vault.
         */
        const val DEFAULT_BASE_URL = "https://vault.zincapp.com"

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
    }

    /**
     * Builder for ZnVaultClient.
     */
    class Builder {
        private var baseUrl: String = DEFAULT_BASE_URL
        private var apiKey: String? = null
        private var connectTimeout: Duration = Duration.ofSeconds(30)
        private var readTimeout: Duration = Duration.ofSeconds(30)
        private var writeTimeout: Duration = Duration.ofSeconds(30)
        private var retryPolicy: RetryPolicy = RetryPolicy.default()
        private var tlsConfig: TlsConfig? = null
        private var debug: Boolean = false
        private var tokenConfig: TokenManagerConfig = TokenManagerConfig()

        /**
         * Set the base URL for the ZN-Vault server.
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
         *
         * @param key API key (e.g., "znv_xxxx_secretkey")
         */
        fun apiKey(key: String) = apply {
            this.apiKey = key
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

            // Set up authentication
            val key = apiKey // Capture for smart cast
            val tokenManager: TokenManager?
            if (key != null) {
                // Use API key authentication
                tokenManager = null
                httpClient.setAuthProvider(ApiKeyAuthProvider(key))
            } else {
                // Use JWT token authentication
                tokenManager = TokenManager(httpClient, tokenConfig)
                httpClient.setAuthProvider(tokenManager)
            }

            return ZnVaultClient(httpClient, tokenManager, key)
        }
    }
}

/**
 * Auth provider for API key authentication.
 * Uses the X-API-Key header as expected by ZN-Vault.
 */
private class ApiKeyAuthProvider(private val apiKey: String) : com.zincware.vault.http.AuthProvider {
    override fun getAuthHeader(): String? = null  // Not using Authorization header
    override fun getApiKey(): String = apiKey     // Use X-API-Key header
}
