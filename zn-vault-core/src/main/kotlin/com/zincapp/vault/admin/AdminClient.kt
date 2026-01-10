// Path: zn-vault-core/src/main/kotlin/com/zincapp/vault/admin/AdminClient.kt
package com.zincapp.vault.admin

import com.zincapp.vault.http.ZnVaultHttpClient

/**
 * Admin operations facade providing access to tenant, user, role, and policy management.
 */
class AdminClient(httpClient: ZnVaultHttpClient) {

    /** Tenant management operations. */
    val tenants: TenantClient = TenantClient(httpClient)

    /** User management operations. */
    val users: UserClient = UserClient(httpClient)

    /** Role management operations (RBAC). */
    val roles: RoleClient = RoleClient(httpClient)

    /** Policy management operations (ABAC). */
    val policies: PolicyClient = PolicyClient(httpClient)
}
