// Path: zn-vault-sdk-jvm/zn-vault-core/src/main/kotlin/com/zincapp/vault/auth/SuperadminAuthClient.kt
package com.zincapp.vault.auth

import com.zincapp.vault.http.ZnVaultHttpClient
import com.zincapp.vault.models.*
import java.net.URLEncoder

/**
 * Superadmin-only auth operations for cross-tenant API key and registration
 * token management.
 *
 * **Server requirement:** these methods call routes under
 * `/v1/superadmin/api-keys/` (including the `managed/` sub-tree). Those
 * routes are not yet implemented on the server (as of vault 1.38.8); calls
 * will return 404 until they ship. For tenant-scoped operations within a
 * single tenant, use [AuthClient] on a [com.zincapp.vault.ZnVaultClient].
 */
class SuperadminAuthClient internal constructor(
    private val httpClient: ZnVaultHttpClient
) {
    /** Create a regular API key in any tenant. */
    fun createApiKey(
        tenantId: String,
        name: String,
        permissions: List<String>,
        expiresInDays: Int? = null,
        description: String? = null,
        ipAllowlist: List<String>? = null,
        conditions: ApiKeyConditions? = null
    ): CreateApiKeyResponse {
        val request = CreateApiKeyRequest(
            name = name,
            permissions = permissions,
            expiresInDays = expiresInDays,
            description = description,
            ipAllowlist = ipAllowlist,
            conditions = conditions
        )
        return httpClient.post(
            "/v1/superadmin/api-keys?tenantId=${enc(tenantId)}",
            request,
            CreateApiKeyResponse::class.java
        )
    }

    /** List API keys for any tenant. */
    fun listApiKeys(tenantId: String): List<ApiKey> {
        val response = httpClient.get(
            "/v1/superadmin/api-keys?tenantId=${enc(tenantId)}",
            ApiKeyListResponse::class.java
        )
        return response.items
    }

    /** Delete an API key by id (server resolves tenant from the key record). */
    fun deleteApiKey(id: String) {
        httpClient.delete("/v1/superadmin/api-keys/$id")
    }

    // ==================== Managed API Keys ====================

    fun createManagedApiKey(
        tenantId: String,
        name: String,
        permissions: List<String>,
        rotationMode: RotationMode,
        rotationInterval: String? = null,
        gracePeriod: String? = null,
        description: String? = null,
        expiresInDays: Int? = null
    ): CreateManagedApiKeyResponse {
        val request = CreateManagedApiKeyRequest(
            name = name,
            permissions = permissions,
            rotationMode = rotationMode,
            rotationInterval = rotationInterval,
            gracePeriod = gracePeriod,
            description = description,
            expiresInDays = expiresInDays
        )
        return httpClient.post(
            "/v1/superadmin/api-keys/managed?tenantId=${enc(tenantId)}",
            request,
            CreateManagedApiKeyResponse::class.java
        )
    }

    fun listManagedApiKeys(tenantId: String): List<ManagedApiKey> {
        val response = httpClient.get(
            "/v1/superadmin/api-keys/managed?tenantId=${enc(tenantId)}",
            ManagedApiKeyListResponse::class.java
        )
        return response.keys
    }

    fun getManagedApiKey(tenantId: String, name: String): ManagedApiKey {
        return httpClient.get(
            "/v1/superadmin/api-keys/managed/${enc(name)}?tenantId=${enc(tenantId)}",
            ManagedApiKey::class.java
        )
    }

    fun rotateManagedApiKey(tenantId: String, name: String): ManagedKeyRotateResponse {
        return httpClient.postEmpty(
            "/v1/superadmin/api-keys/managed/${enc(name)}/rotate?tenantId=${enc(tenantId)}",
            ManagedKeyRotateResponse::class.java
        )
    }

    fun updateManagedApiKeyConfig(
        tenantId: String,
        name: String,
        rotationInterval: String? = null,
        gracePeriod: String? = null,
        enabled: Boolean? = null
    ): ManagedApiKey {
        val request = UpdateManagedApiKeyConfigRequest(
            rotationInterval = rotationInterval,
            gracePeriod = gracePeriod,
            enabled = enabled
        )
        return httpClient.patch(
            "/v1/superadmin/api-keys/managed/${enc(name)}/config?tenantId=${enc(tenantId)}",
            request,
            ManagedApiKey::class.java
        )
    }

    fun deleteManagedApiKey(tenantId: String, name: String) {
        httpClient.delete("/v1/superadmin/api-keys/managed/${enc(name)}?tenantId=${enc(tenantId)}")
    }

    // ==================== Registration Tokens ====================

    fun createRegistrationToken(
        tenantId: String,
        managedKeyName: String,
        expiresIn: String? = null,
        description: String? = null
    ): CreateRegistrationTokenResponse {
        val request = CreateRegistrationTokenRequest(
            expiresIn = expiresIn,
            description = description
        )
        return httpClient.post(
            "/v1/superadmin/api-keys/managed/${enc(managedKeyName)}/registration-tokens?tenantId=${enc(tenantId)}",
            request,
            CreateRegistrationTokenResponse::class.java
        )
    }

    fun listRegistrationTokens(
        tenantId: String,
        managedKeyName: String,
        includeUsed: Boolean = false
    ): List<RegistrationToken> {
        val q = if (includeUsed) "&includeUsed=true" else ""
        val response = httpClient.get(
            "/v1/superadmin/api-keys/managed/${enc(managedKeyName)}/registration-tokens?tenantId=${enc(tenantId)}$q",
            RegistrationTokenListResponse::class.java
        )
        return response.tokens
    }

    fun revokeRegistrationToken(tenantId: String, managedKeyName: String, tokenId: String) {
        httpClient.delete(
            "/v1/superadmin/api-keys/managed/${enc(managedKeyName)}/registration-tokens/$tokenId?tenantId=${enc(tenantId)}"
        )
    }

    private fun enc(value: String): String =
        URLEncoder.encode(value, "UTF-8")
}
