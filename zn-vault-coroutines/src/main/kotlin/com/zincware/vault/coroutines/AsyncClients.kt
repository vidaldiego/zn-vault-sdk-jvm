// Path: zn-vault-sdk-jvm/zn-vault-coroutines/src/main/kotlin/com/zincware/vault/coroutines/AsyncClients.kt
package com.zincware.vault.coroutines

import com.zincware.vault.admin.PolicyClient
import com.zincware.vault.admin.RoleClient
import com.zincware.vault.admin.TenantClient
import com.zincware.vault.admin.UserClient
import com.zincware.vault.audit.AuditClient
import com.zincware.vault.auth.AuthClient
import com.zincware.vault.health.HealthClient
import com.zincware.vault.kms.KmsClient
import com.zincware.vault.models.*
import com.zincware.vault.secrets.SecretClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Async wrapper for SecretClient.
 */
class AsyncSecretClient(private val sync: SecretClient) {

    suspend fun create(request: CreateSecretRequest): Secret = withContext(Dispatchers.IO) {
        sync.create(request)
    }

    suspend fun create(
        alias: String,
        tenant: String,
        type: SecretType,
        data: Map<String, Any>,
        tags: List<String> = emptyList()
    ): Secret = withContext(Dispatchers.IO) {
        sync.create(alias, tenant, type, data, tags)
    }

    suspend fun get(id: String): Secret = withContext(Dispatchers.IO) {
        sync.get(id)
    }

    suspend fun getByAlias(tenant: String, alias: String): Secret = withContext(Dispatchers.IO) {
        sync.getByAlias(tenant, alias)
    }

    suspend fun decrypt(id: String): SecretData = withContext(Dispatchers.IO) {
        sync.decrypt(id)
    }

    suspend fun update(id: String, data: Map<String, Any>): Secret = withContext(Dispatchers.IO) {
        sync.update(id, data)
    }

    suspend fun updateMetadata(id: String, tags: List<String>): Secret = withContext(Dispatchers.IO) {
        sync.updateMetadata(id, tags)
    }

    suspend fun rotate(id: String, newData: Map<String, Any>): Secret = withContext(Dispatchers.IO) {
        sync.rotate(id, newData)
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        sync.delete(id)
    }

    suspend fun list(filter: SecretFilter = SecretFilter()): List<Secret> = withContext(Dispatchers.IO) {
        sync.list(filter)
    }

    /**
     * List all secrets as a Flow.
     *
     * Note: Currently returns all secrets from single call as list API doesn't paginate.
     */
    fun listAsFlow(filter: SecretFilter = SecretFilter()): Flow<Secret> = flow {
        val secrets = withContext(Dispatchers.IO) {
            sync.list(filter)
        }
        secrets.forEach { emit(it) }
    }

    suspend fun getHistory(id: String): List<SecretVersion> = withContext(Dispatchers.IO) {
        sync.getHistory(id)
    }

    suspend fun decryptVersion(id: String, version: Int): SecretData = withContext(Dispatchers.IO) {
        sync.decryptVersion(id, version)
    }

    suspend fun uploadFile(
        alias: String,
        tenant: String,
        file: File,
        tags: List<String> = emptyList()
    ): Secret = withContext(Dispatchers.IO) {
        sync.uploadFile(alias, tenant, file, tags)
    }

    suspend fun uploadFile(
        alias: String,
        tenant: String,
        filename: String,
        content: ByteArray,
        contentType: String = "application/octet-stream",
        tags: List<String> = emptyList()
    ): Secret = withContext(Dispatchers.IO) {
        sync.uploadFile(alias, tenant, filename, content, contentType, tags)
    }

    suspend fun downloadFile(id: String): ByteArray = withContext(Dispatchers.IO) {
        sync.downloadFile(id)
    }

    suspend fun downloadFile(id: String, destination: File) = withContext(Dispatchers.IO) {
        sync.downloadFile(id, destination)
    }

    suspend fun createCredential(
        alias: String,
        tenant: String,
        username: String,
        password: String,
        tags: List<String> = emptyList()
    ): Secret = withContext(Dispatchers.IO) {
        sync.createCredential(alias, tenant, username, password, tags)
    }

    suspend fun getCredentials(id: String): Pair<String, String> = withContext(Dispatchers.IO) {
        sync.getCredentials(id)
    }
}

/**
 * Async wrapper for KmsClient.
 */
class AsyncKmsClient(private val sync: KmsClient) {

    suspend fun createKey(request: CreateKeyRequest): KmsKey = withContext(Dispatchers.IO) {
        sync.createKey(request)
    }

    suspend fun createKey(
        tenant: String,
        alias: String? = null,
        description: String? = null
    ): KmsKey = withContext(Dispatchers.IO) {
        sync.createKey(tenant, alias, description)
    }

    suspend fun getKey(keyId: String): KmsKey = withContext(Dispatchers.IO) {
        sync.getKey(keyId)
    }

    suspend fun listKeys(filter: KeyFilter = KeyFilter()): KeyListResponse = withContext(Dispatchers.IO) {
        sync.listKeys(filter)
    }

    fun listKeysAsFlow(filter: KeyFilter = KeyFilter()): Flow<KmsKey> = flow {
        var currentFilter = filter
        do {
            val response = withContext(Dispatchers.IO) {
                sync.listKeys(currentFilter)
            }
            response.items.forEach { emit(it) }
            currentFilter = currentFilter.copy(marker = response.nextMarker)
        } while (response.hasMore)
    }

    suspend fun enableKey(keyId: String): KmsKey = withContext(Dispatchers.IO) {
        sync.enableKey(keyId)
    }

    suspend fun disableKey(keyId: String): KmsKey = withContext(Dispatchers.IO) {
        sync.disableKey(keyId)
    }

    suspend fun encrypt(
        keyId: String,
        plaintext: ByteArray,
        context: Map<String, String> = emptyMap()
    ): EncryptResult = withContext(Dispatchers.IO) {
        sync.encrypt(keyId, plaintext, context)
    }

    suspend fun encrypt(
        keyId: String,
        plaintext: String,
        context: Map<String, String> = emptyMap()
    ): EncryptResult = withContext(Dispatchers.IO) {
        sync.encrypt(keyId, plaintext, context)
    }

    suspend fun decrypt(
        keyId: String,
        ciphertextBlob: String,
        context: Map<String, String> = emptyMap()
    ): ByteArray = withContext(Dispatchers.IO) {
        sync.decrypt(keyId, ciphertextBlob, context)
    }

    suspend fun decryptToString(
        keyId: String,
        ciphertextBlob: String,
        context: Map<String, String> = emptyMap()
    ): String = withContext(Dispatchers.IO) {
        sync.decryptToString(keyId, ciphertextBlob, context)
    }

    suspend fun generateDataKey(
        keyId: String,
        keySpec: KeySpec = KeySpec.AES_256,
        context: Map<String, String> = emptyMap()
    ): DataKeyResult = withContext(Dispatchers.IO) {
        sync.generateDataKey(keyId, keySpec, context)
    }

    suspend fun rotateKey(keyId: String): KmsKey = withContext(Dispatchers.IO) {
        sync.rotateKey(keyId)
    }
}

/**
 * Async wrapper for TenantClient.
 */
class AsyncTenantClient(private val sync: TenantClient) {

    suspend fun create(request: CreateTenantRequest): Tenant = withContext(Dispatchers.IO) {
        sync.create(request)
    }

    suspend fun create(id: String, name: String): Tenant = withContext(Dispatchers.IO) {
        sync.create(id, name)
    }

    suspend fun get(id: String): Tenant = withContext(Dispatchers.IO) {
        sync.get(id)
    }

    suspend fun list(filter: TenantFilter = TenantFilter()): Page<Tenant> = withContext(Dispatchers.IO) {
        sync.list(filter)
    }

    fun listAsFlow(filter: TenantFilter = TenantFilter()): Flow<Tenant> = flow {
        var currentFilter = filter
        do {
            val page = withContext(Dispatchers.IO) {
                sync.list(currentFilter)
            }
            page.items.forEach { emit(it) }
            currentFilter = currentFilter.copy(offset = currentFilter.offset + page.items.size)
        } while (page.hasMore)
    }

    suspend fun update(id: String, request: UpdateTenantRequest): Tenant = withContext(Dispatchers.IO) {
        sync.update(id, request)
    }

    suspend fun updateStatus(id: String, status: TenantStatus): Tenant = withContext(Dispatchers.IO) {
        sync.updateStatus(id, status)
    }

    suspend fun suspend(id: String): Tenant = withContext(Dispatchers.IO) {
        sync.suspend(id)
    }

    suspend fun activate(id: String): Tenant = withContext(Dispatchers.IO) {
        sync.activate(id)
    }

    suspend fun archive(id: String) = withContext(Dispatchers.IO) {
        sync.archive(id)
    }

    suspend fun getUsage(id: String): TenantUsage = withContext(Dispatchers.IO) {
        sync.getUsage(id)
    }
}

/**
 * Async wrapper for UserClient.
 */
class AsyncUserClient(private val sync: UserClient) {

    suspend fun create(request: CreateUserRequest): User = withContext(Dispatchers.IO) {
        sync.create(request)
    }

    suspend fun create(
        username: String,
        password: String,
        email: String? = null,
        tenantId: String? = null
    ): User = withContext(Dispatchers.IO) {
        sync.create(username, password, email, tenantId)
    }

    suspend fun get(id: String): User = withContext(Dispatchers.IO) {
        sync.get(id)
    }

    suspend fun list(filter: UserFilter = UserFilter()): Page<User> = withContext(Dispatchers.IO) {
        sync.list(filter)
    }

    fun listAsFlow(filter: UserFilter = UserFilter()): Flow<User> = flow {
        var currentFilter = filter
        do {
            val page = withContext(Dispatchers.IO) {
                sync.list(currentFilter)
            }
            page.items.forEach { emit(it) }
            currentFilter = currentFilter.copy(offset = currentFilter.offset + page.items.size)
        } while (page.hasMore)
    }

    suspend fun update(id: String, request: UpdateUserRequest): User = withContext(Dispatchers.IO) {
        sync.update(id, request)
    }

    suspend fun resetPassword(id: String, newPassword: String): User = withContext(Dispatchers.IO) {
        sync.resetPassword(id, newPassword)
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        sync.delete(id)
    }

    suspend fun disable(id: String): User = withContext(Dispatchers.IO) {
        sync.disable(id)
    }

    suspend fun enable(id: String): User = withContext(Dispatchers.IO) {
        sync.enable(id)
    }
}

/**
 * Async wrapper for RoleClient.
 */
class AsyncRoleClient(private val sync: RoleClient) {

    suspend fun create(request: CreateRoleRequest): Role = withContext(Dispatchers.IO) {
        sync.create(request)
    }

    suspend fun create(
        name: String,
        permissions: List<String>,
        description: String? = null,
        tenantId: String? = null
    ): Role = withContext(Dispatchers.IO) {
        sync.create(name, permissions, description, tenantId)
    }

    suspend fun get(id: String): Role = withContext(Dispatchers.IO) {
        sync.get(id)
    }

    suspend fun list(filter: RoleFilter = RoleFilter()): Page<Role> = withContext(Dispatchers.IO) {
        sync.list(filter)
    }

    suspend fun update(id: String, request: UpdateRoleRequest): Role = withContext(Dispatchers.IO) {
        sync.update(id, request)
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        sync.delete(id)
    }

    suspend fun assignToUser(userId: String, roleId: String) = withContext(Dispatchers.IO) {
        sync.assignToUser(userId, roleId)
    }

    suspend fun removeFromUser(userId: String, roleId: String) = withContext(Dispatchers.IO) {
        sync.removeFromUser(userId, roleId)
    }

    suspend fun getUserRoles(userId: String): List<Role> = withContext(Dispatchers.IO) {
        sync.getUserRoles(userId)
    }

    suspend fun getUserPermissions(userId: String): List<String> = withContext(Dispatchers.IO) {
        sync.getUserPermissions(userId)
    }

    suspend fun listPermissions(): List<Permission> = withContext(Dispatchers.IO) {
        sync.listPermissions()
    }
}

/**
 * Async wrapper for PolicyClient.
 */
class AsyncPolicyClient(private val sync: PolicyClient) {

    suspend fun create(request: CreatePolicyRequest): Policy = withContext(Dispatchers.IO) {
        sync.create(request)
    }

    suspend fun get(id: String): Policy = withContext(Dispatchers.IO) {
        sync.get(id)
    }

    suspend fun list(filter: PolicyFilter = PolicyFilter()): Page<Policy> = withContext(Dispatchers.IO) {
        sync.list(filter)
    }

    suspend fun update(id: String, request: UpdatePolicyRequest): Policy = withContext(Dispatchers.IO) {
        sync.update(id, request)
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        sync.delete(id)
    }

    suspend fun enable(id: String): Policy = withContext(Dispatchers.IO) {
        sync.enable(id)
    }

    suspend fun disable(id: String): Policy = withContext(Dispatchers.IO) {
        sync.disable(id)
    }

    suspend fun attachToUser(policyId: String, userId: String) = withContext(Dispatchers.IO) {
        sync.attachToUser(policyId, userId)
    }

    suspend fun attachToRole(policyId: String, roleId: String) = withContext(Dispatchers.IO) {
        sync.attachToRole(policyId, roleId)
    }

    suspend fun detachFromUser(policyId: String, userId: String) = withContext(Dispatchers.IO) {
        sync.detachFromUser(policyId, userId)
    }

    suspend fun detachFromRole(policyId: String, roleId: String) = withContext(Dispatchers.IO) {
        sync.detachFromRole(policyId, roleId)
    }
}

/**
 * Async wrapper for AuditClient.
 */
class AsyncAuditClient(private val sync: AuditClient) {

    suspend fun list(filter: AuditFilter = AuditFilter()): AuditPage = withContext(Dispatchers.IO) {
        sync.list(filter)
    }

    fun listAsFlow(filter: AuditFilter = AuditFilter()): Flow<AuditEntry> = flow {
        var currentFilter = filter
        do {
            val response = withContext(Dispatchers.IO) {
                sync.list(currentFilter)
            }
            response.entries.forEach { emit(it) }
            currentFilter = currentFilter.copy(offset = currentFilter.offset + response.entries.size)
        } while (response.hasMore)
    }

    suspend fun getStats(period: StatsPeriod = StatsPeriod.DAY): AuditStats = withContext(Dispatchers.IO) {
        sync.getStats(period)
    }

    suspend fun export(
        filter: AuditFilter = AuditFilter(),
        format: ExportFormat = ExportFormat.JSON
    ): String = withContext(Dispatchers.IO) {
        sync.export(filter, format)
    }

    suspend fun verify(): AuditVerifyResult = withContext(Dispatchers.IO) {
        sync.verify()
    }

    suspend fun isChainValid(): Boolean = withContext(Dispatchers.IO) {
        sync.isChainValid()
    }
}

/**
 * Async wrapper for HealthClient.
 */
class AsyncHealthClient(private val sync: HealthClient) {

    suspend fun getHealth(): HealthStatus = withContext(Dispatchers.IO) {
        sync.getHealth()
    }

    suspend fun isHealthy(): Boolean = withContext(Dispatchers.IO) {
        sync.isHealthy()
    }

    suspend fun isLive(): Boolean = withContext(Dispatchers.IO) {
        sync.isLive()
    }

    suspend fun isReady(): Boolean = withContext(Dispatchers.IO) {
        sync.isReady()
    }

    suspend fun ping(): Boolean = withContext(Dispatchers.IO) {
        sync.ping()
    }
}

/**
 * Async wrapper for AuthClient.
 */
class AsyncAuthClient(private val sync: AuthClient) {

    suspend fun register(
        username: String,
        password: String,
        email: String? = null
    ): User = withContext(Dispatchers.IO) {
        sync.register(username, password, email)
    }

    suspend fun me(): User = withContext(Dispatchers.IO) {
        sync.me()
    }

    suspend fun updateProfile(email: String): User = withContext(Dispatchers.IO) {
        sync.updateProfile(email)
    }

    suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ) = withContext(Dispatchers.IO) {
        sync.changePassword(currentPassword, newPassword)
    }

    suspend fun createApiKey(
        name: String,
        expiresIn: String = "90d"
    ): CreateApiKeyResponse = withContext(Dispatchers.IO) {
        sync.createApiKey(name, expiresIn)
    }

    suspend fun listApiKeys(): List<ApiKey> = withContext(Dispatchers.IO) {
        sync.listApiKeys()
    }

    suspend fun deleteApiKey(id: String) = withContext(Dispatchers.IO) {
        sync.deleteApiKey(id)
    }

    suspend fun rotateApiKey(id: String): RotateApiKeyResponse = withContext(Dispatchers.IO) {
        sync.rotateApiKey(id)
    }

    suspend fun enable2fa(): Enable2faResponse = withContext(Dispatchers.IO) {
        sync.enable2fa()
    }

    suspend fun verify2fa(code: String) = withContext(Dispatchers.IO) {
        sync.verify2fa(code)
    }

    suspend fun disable2fa(password: String, totpCode: String? = null) = withContext(Dispatchers.IO) {
        sync.disable2fa(password, totpCode)
    }

    suspend fun get2faStatus(): TwoFactorStatus = withContext(Dispatchers.IO) {
        sync.get2faStatus()
    }
}
