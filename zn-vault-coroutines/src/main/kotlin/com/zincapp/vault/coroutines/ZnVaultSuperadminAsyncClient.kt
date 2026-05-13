// Path: zn-vault-sdk-jvm/zn-vault-coroutines/src/main/kotlin/com/zincapp/vault/coroutines/ZnVaultSuperadminAsyncClient.kt
package com.zincapp.vault.coroutines

import com.zincapp.vault.ZnVaultSuperadminClient

/**
 * Coroutine-aware wrapper around [ZnVaultSuperadminClient].
 *
 * Mirrors the superadmin surface: tenant CRUD, audit, health.
 *
 * ## Example
 * ```kotlin
 * val admin = ZnVaultSuperadminClient.builder()...build()
 * val asyncAdmin = ZnVaultSuperadminAsyncClient(admin)
 * val tenant = asyncAdmin.tenants.create(...)
 * ```
 */
class ZnVaultSuperadminAsyncClient(private val sync: ZnVaultSuperadminClient) {

    /** Async tenant operations (superadmin only). */
    val tenants = AsyncTenantClient(sync.tenants)

    /** Async audit operations across all tenants. */
    val audit = AsyncAuditClient(sync.audit)

    /** Async health operations. */
    val health = AsyncHealthClient(sync.health)
}
