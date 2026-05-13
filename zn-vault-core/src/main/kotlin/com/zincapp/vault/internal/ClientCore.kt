// Path: zn-vault-sdk-jvm/zn-vault-core/src/main/kotlin/com/zincapp/vault/internal/ClientCore.kt
package com.zincapp.vault.internal

import com.zincapp.vault.auth.ApiKeyAuth
import com.zincapp.vault.auth.FileApiKeyAuth
import com.zincapp.vault.auth.TokenManager
import com.zincapp.vault.auth.TokenManagerConfig
import com.zincapp.vault.http.AuthProvider
import com.zincapp.vault.http.HttpClientConfig
import com.zincapp.vault.http.RetryPolicy
import com.zincapp.vault.http.TlsConfig
import com.zincapp.vault.http.ZnVaultHttpClient
import java.time.Duration

/**
 * Shared HTTP/auth machinery used by both [com.zincapp.vault.ZnVaultClient] and
 * [com.zincapp.vault.ZnVaultSuperadminClient]. Not part of the public API.
 */
internal data class ClientCore(
    val httpClient: ZnVaultHttpClient,
    val tokenManager: TokenManager?,
    val apiKey: String?
)

/**
 * Mutable configuration captured by either client's Builder. Internal helper
 * that holds the union of parameters, with [buildCore] producing the wired-up
 * HTTP client + auth provider.
 */
internal class ClientCoreConfig(
    var baseUrl: String,
    var apiKey: String? = null,
    var apiKeyFilePath: String? = null,
    var apiKeyEnvName: String? = null,
    var connectTimeout: Duration = Duration.ofSeconds(30),
    var readTimeout: Duration = Duration.ofSeconds(30),
    var writeTimeout: Duration = Duration.ofSeconds(30),
    var retryPolicy: RetryPolicy = RetryPolicy.default(),
    var tlsConfig: TlsConfig? = null,
    var debug: Boolean = false,
    var tokenConfig: TokenManagerConfig = TokenManagerConfig()
) {
    fun buildCore(): ClientCore {
        val httpConfig = HttpClientConfig(
            connectTimeout = connectTimeout,
            readTimeout = readTimeout,
            writeTimeout = writeTimeout,
            retryPolicy = retryPolicy,
            tlsConfig = tlsConfig,
            debug = debug
        )

        val httpClient = ZnVaultHttpClient(baseUrl, httpConfig)

        val authProvider: AuthProvider? = when {
            apiKey != null -> ApiKeyAuth(apiKey!!)
            apiKeyFilePath != null -> FileApiKeyAuth(apiKeyFilePath!!)
            apiKeyEnvName != null -> FileApiKeyAuth.fromEnv(apiKeyEnvName!!)
            else -> null
        }

        return if (authProvider != null) {
            httpClient.setAuthProvider(authProvider)
            ClientCore(httpClient, tokenManager = null, apiKey = authProvider.getApiKey())
        } else {
            val tokenManager = TokenManager(httpClient, tokenConfig)
            httpClient.setAuthProvider(tokenManager)
            ClientCore(httpClient, tokenManager, apiKey = null)
        }
    }
}
