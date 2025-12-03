// Path: zn-vault-sdk-jvm/zn-vault-core/src/main/kotlin/com/zincware/vault/ZnVaultConfig.kt
package com.zincware.vault

import com.zincware.vault.http.RetryPolicy
import com.zincware.vault.http.TlsConfig
import java.time.Duration

/**
 * Configuration for ZnVaultClient.
 *
 * This is an alternative way to configure the client using a data class
 * instead of the builder pattern.
 *
 * ## Example Usage
 * ```kotlin
 * val config = ZnVaultConfig(
 *     apiKey = "znv_xxxx_secretkey"
 * )
 * val client = config.toClient()
 * ```
 */
data class ZnVaultConfig(
    /**
     * Base URL for the ZN-Vault server.
     * Defaults to https://vault.zincapp.com
     */
    val baseUrl: String = ZnVaultClient.DEFAULT_BASE_URL,

    /**
     * Optional API key for authentication.
     * When set, the client will use API key authentication instead of JWT.
     */
    val apiKey: String? = null,

    /**
     * Connection timeout.
     */
    val connectTimeout: Duration = Duration.ofSeconds(30),

    /**
     * Read timeout.
     */
    val readTimeout: Duration = Duration.ofSeconds(30),

    /**
     * Write timeout.
     */
    val writeTimeout: Duration = Duration.ofSeconds(30),

    /**
     * Retry policy for failed requests.
     */
    val retryPolicy: RetryPolicy = RetryPolicy.default(),

    /**
     * TLS configuration for custom CA certificates or mTLS.
     */
    val tlsConfig: TlsConfig? = null,

    /**
     * Enable debug logging.
     */
    val debug: Boolean = false
) {
    /**
     * Create a ZnVaultClient from this configuration.
     */
    fun toClient(): ZnVaultClient {
        val builder = ZnVaultClient.builder()
            .baseUrl(baseUrl)
            .connectTimeout(connectTimeout)
            .readTimeout(readTimeout)
            .writeTimeout(writeTimeout)
            .retryPolicy(retryPolicy)
            .debug(debug)

        apiKey?.let { builder.apiKey(it) }
        tlsConfig?.let { builder.tlsConfig(it) }

        return builder.build()
    }

    companion object {
        /**
         * Create a default configuration.
         */
        @JvmStatic
        fun default(): ZnVaultConfig = ZnVaultConfig()

        /**
         * Create a configuration with API key authentication using default base URL.
         */
        @JvmStatic
        fun withApiKey(apiKey: String): ZnVaultConfig =
            ZnVaultConfig(apiKey = apiKey)

        /**
         * Create a configuration with API key authentication and custom base URL.
         */
        @JvmStatic
        fun withApiKey(baseUrl: String, apiKey: String): ZnVaultConfig =
            ZnVaultConfig(baseUrl = baseUrl, apiKey = apiKey)

        /**
         * Create a development configuration with insecure TLS.
         *
         * WARNING: Only use for development!
         */
        @JvmStatic
        fun development(baseUrl: String = "https://localhost:8443", apiKey: String? = null): ZnVaultConfig =
            ZnVaultConfig(
                baseUrl = baseUrl,
                apiKey = apiKey,
                tlsConfig = TlsConfig.insecure(),
                debug = true
            )
    }
}
