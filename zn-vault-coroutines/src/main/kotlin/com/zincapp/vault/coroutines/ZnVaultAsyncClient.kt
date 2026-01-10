// Path: zn-vault-sdk-jvm/zn-vault-coroutines/src/main/kotlin/com/zincapp/vault/coroutines/ZnVaultAsyncClient.kt
package com.zincapp.vault.coroutines

import com.zincapp.vault.ZnVaultClient
import com.zincapp.vault.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * Async wrapper for ZnVaultClient using Kotlin Coroutines.
 *
 * All operations are executed on Dispatchers.IO and are suspending functions.
 *
 * ## Example Usage
 * ```kotlin
 * val client = ZnVaultClient.builder()
 *     .baseUrl("https://vault.example.com:8443")
 *     .apiKey("znv_xxxx_secretkey")
 *     .build()
 *
 * val asyncClient = ZnVaultAsyncClient(client)
 *
 * runBlocking {
 *     val secret = asyncClient.secrets.create(
 *         CreateSecretRequest(
 *             alias = "api/production/db-creds",
 *             tenant = "acme",
 *             type = SecretType.CREDENTIAL,
 *             data = mapOf("username" to "admin", "password" to "secret")
 *         )
 *     )
 *     println("Created secret: ${secret.id}")
 *
 *     // Stream all secrets
 *     asyncClient.secrets.listAsFlow()
 *         .filter { it.tenant == "acme" }
 *         .collect { println(it.alias) }
 * }
 * ```
 */
class ZnVaultAsyncClient(private val sync: ZnVaultClient) {

    /** Async secret operations */
    val secrets = AsyncSecretClient(sync.secrets)

    /** Async KMS operations */
    val kms = AsyncKmsClient(sync.kms)

    /** Async tenant operations */
    val tenants = AsyncTenantClient(sync.admin.tenants)

    /** Async user operations */
    val users = AsyncUserClient(sync.admin.users)

    /** Async role operations */
    val roles = AsyncRoleClient(sync.admin.roles)

    /** Async policy operations */
    val policies = AsyncPolicyClient(sync.admin.policies)

    /** Async audit operations */
    val audit = AsyncAuditClient(sync.audit)

    /** Async health operations */
    val health = AsyncHealthClient(sync.health)

    /** Async auth operations */
    val auth = AsyncAuthClient(sync.auth)

    /**
     * Login with username and password.
     */
    suspend fun login(
        username: String,
        password: String,
        totpCode: String? = null
    ): LoginResponse = withContext(Dispatchers.IO) {
        sync.login(username, password, totpCode)
    }

    /**
     * Logout and clear tokens.
     */
    suspend fun logout() = withContext(Dispatchers.IO) {
        sync.logout()
    }

    /**
     * Check if authenticated.
     */
    suspend fun isAuthenticated(): Boolean = withContext(Dispatchers.IO) {
        sync.isAuthenticated()
    }

    /**
     * Check if healthy.
     */
    suspend fun isHealthy(): Boolean = withContext(Dispatchers.IO) {
        sync.isHealthy()
    }
}

/**
 * Extension function to create an async client from a sync client.
 */
fun ZnVaultClient.async(): ZnVaultAsyncClient = ZnVaultAsyncClient(this)
