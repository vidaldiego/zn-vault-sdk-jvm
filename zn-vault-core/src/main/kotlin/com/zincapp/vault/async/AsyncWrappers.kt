// Path: zn-vault-sdk-jvm/zn-vault-core/src/main/kotlin/com/zincapp/vault/async/AsyncWrappers.kt
package com.zincapp.vault.async

import com.zincapp.vault.admin.PolicyClient
import com.zincapp.vault.admin.RoleClient
import com.zincapp.vault.admin.TenantClient
import com.zincapp.vault.admin.UserClient
import com.zincapp.vault.audit.AuditClient
import com.zincapp.vault.auth.AuthClient
import com.zincapp.vault.health.HealthClient
import com.zincapp.vault.kms.KmsClient
import com.zincapp.vault.models.*
import com.zincapp.vault.secrets.SecretClient
import java.io.File
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Default executor for async operations.
 * Uses a cached thread pool that creates threads as needed.
 */
private val defaultExecutor: Executor = Executors.newCachedThreadPool { r ->
    Thread(r, "zn-vault-async").apply { isDaemon = true }
}

/**
 * Async wrapper for SecretClient using CompletableFuture.
 *
 * ## Example Usage (Java)
 * ```java
 * ZnVaultClient client = ZnVaultClient.builder()
 *     .baseUrl("https://vault:8443")
 *     .apiKey("znv_xxx")
 *     .build();
 *
 * SecretClientAsync async = new SecretClientAsync(client.secrets());
 *
 * async.decryptAsync("secret-id")
 *     .thenAccept(data -> System.out.println(data.getData()))
 *     .exceptionally(e -> { log.error("Failed", e); return null; });
 * ```
 */
class SecretClientAsync @JvmOverloads constructor(
    private val sync: SecretClient,
    private val executor: Executor = defaultExecutor
) {

    fun createAsync(request: CreateSecretRequest): CompletableFuture<Secret> =
        CompletableFuture.supplyAsync({ sync.create(request) }, executor)

    fun createAsync(
        alias: String,
        type: SecretType,
        data: Map<String, Any>,
        subType: SecretSubType? = null,
        tags: List<String> = emptyList(),
        fileName: String? = null,
        expiresAt: Instant? = null,
        ttlUntil: Instant? = null,
        contentType: String? = null
    ): CompletableFuture<Secret> =
        CompletableFuture.supplyAsync({ sync.create(alias, type, data, subType, tags, fileName, expiresAt, ttlUntil, contentType) }, executor)

    fun getAsync(id: String): CompletableFuture<Secret> =
        CompletableFuture.supplyAsync({ sync.get(id) }, executor)

    fun getByAliasAsync(alias: String): CompletableFuture<Secret> =
        CompletableFuture.supplyAsync({ sync.getByAlias(alias) }, executor)

    fun decryptAsync(id: String): CompletableFuture<SecretData> =
        CompletableFuture.supplyAsync({ sync.decrypt(id) }, executor)

    fun updateAsync(id: String, data: Map<String, Any>): CompletableFuture<Secret> =
        CompletableFuture.supplyAsync({ sync.update(id, data) }, executor)

    fun updateMetadataAsync(id: String, tags: List<String>): CompletableFuture<Secret> =
        CompletableFuture.supplyAsync({ sync.updateMetadata(id, tags) }, executor)

    fun rotateAsync(id: String, newData: Map<String, Any>): CompletableFuture<Secret> =
        CompletableFuture.supplyAsync({ sync.rotate(id, newData) }, executor)

    fun deleteAsync(id: String): CompletableFuture<Void> =
        CompletableFuture.runAsync({ sync.delete(id) }, executor)

    fun listAsync(filter: SecretFilter = SecretFilter()): CompletableFuture<List<Secret>> =
        CompletableFuture.supplyAsync({ sync.list(filter) }, executor)

    fun getHistoryAsync(id: String): CompletableFuture<List<SecretVersion>> =
        CompletableFuture.supplyAsync({ sync.getHistory(id) }, executor)

    fun decryptVersionAsync(id: String, version: Int): CompletableFuture<SecretData> =
        CompletableFuture.supplyAsync({ sync.decryptVersion(id, version) }, executor)

    fun uploadFileAsync(
        alias: String,
        file: File,
        subType: SecretSubType? = null,
        expiresAt: Instant? = null,
        tags: List<String> = emptyList()
    ): CompletableFuture<Secret> =
        CompletableFuture.supplyAsync({ sync.uploadFile(alias, file, subType, expiresAt, tags) }, executor)

    fun uploadFileAsync(
        alias: String,
        filename: String,
        content: ByteArray,
        contentType: String = "application/octet-stream",
        subType: SecretSubType? = null,
        expiresAt: Instant? = null,
        tags: List<String> = emptyList()
    ): CompletableFuture<Secret> =
        CompletableFuture.supplyAsync({ sync.uploadFile(alias, filename, content, contentType, subType, expiresAt, tags) }, executor)

    fun downloadFileAsync(id: String): CompletableFuture<ByteArray> =
        CompletableFuture.supplyAsync({ sync.downloadFile(id) }, executor)

    @Deprecated("Use createPasswordAsync instead", ReplaceWith("createPasswordAsync(alias, username, password, tags = tags)"))
    fun createCredentialAsync(
        alias: String,
        username: String,
        password: String,
        tags: List<String> = emptyList()
    ): CompletableFuture<Secret> =
        CompletableFuture.supplyAsync({ sync.createCredential(alias, username, password, tags) }, executor)

    fun getCredentialsAsync(id: String): CompletableFuture<Pair<String, String>> =
        CompletableFuture.supplyAsync({ sync.getCredentials(id) }, executor)

    // Convenience async methods for typed secret creation

    fun createPasswordAsync(
        alias: String,
        username: String,
        password: String,
        url: String? = null,
        notes: String? = null,
        tags: List<String> = emptyList(),
        ttlUntil: Instant? = null
    ): CompletableFuture<Secret> =
        CompletableFuture.supplyAsync({ sync.createPassword(alias, username, password, url, notes, tags, ttlUntil) }, executor)

    fun createApiKeyAsync(
        alias: String,
        key: String,
        secret: String? = null,
        endpoint: String? = null,
        notes: String? = null,
        tags: List<String> = emptyList(),
        ttlUntil: Instant? = null
    ): CompletableFuture<Secret> =
        CompletableFuture.supplyAsync({ sync.createApiKey(alias, key, secret, endpoint, notes, tags, ttlUntil) }, executor)

    fun createCertificateAsync(
        alias: String,
        content: ByteArray,
        fileName: String? = null,
        chain: List<String>? = null,
        expiresAt: Instant? = null,
        tags: List<String> = emptyList()
    ): CompletableFuture<Secret> =
        CompletableFuture.supplyAsync({ sync.createCertificate(alias, content, fileName, chain, expiresAt, tags) }, executor)

    fun createPrivateKeyAsync(
        alias: String,
        privateKey: ByteArray,
        fileName: String? = null,
        passphrase: String? = null,
        tags: List<String> = emptyList()
    ): CompletableFuture<Secret> =
        CompletableFuture.supplyAsync({ sync.createPrivateKey(alias, privateKey, fileName, passphrase, tags) }, executor)

    fun createKeypairAsync(
        alias: String,
        privateKey: ByteArray,
        publicKey: ByteArray,
        fileName: String? = null,
        passphrase: String? = null,
        tags: List<String> = emptyList()
    ): CompletableFuture<Secret> =
        CompletableFuture.supplyAsync({ sync.createKeypair(alias, privateKey, publicKey, fileName, passphrase, tags) }, executor)

    fun createTokenAsync(
        alias: String,
        token: String,
        tokenType: String? = null,
        refreshToken: String? = null,
        expiresAt: Instant? = null,
        tags: List<String> = emptyList()
    ): CompletableFuture<Secret> =
        CompletableFuture.supplyAsync({ sync.createToken(alias, token, tokenType, refreshToken, expiresAt, tags) }, executor)

    fun createJsonSettingAsync(
        alias: String,
        content: Map<String, Any>,
        tags: List<String> = emptyList()
    ): CompletableFuture<Secret> =
        CompletableFuture.supplyAsync({ sync.createJsonSetting(alias, content, tags) }, executor)

    fun createYamlSettingAsync(
        alias: String,
        content: String,
        tags: List<String> = emptyList()
    ): CompletableFuture<Secret> =
        CompletableFuture.supplyAsync({ sync.createYamlSetting(alias, content, tags) }, executor)

    fun createEnvSettingAsync(
        alias: String,
        content: Map<String, String>,
        tags: List<String> = emptyList()
    ): CompletableFuture<Secret> =
        CompletableFuture.supplyAsync({ sync.createEnvSetting(alias, content, tags) }, executor)

    // Convenience async methods for filtering

    fun listBySubTypeAsync(
        subType: SecretSubType,
        page: Int = 1,
        pageSize: Int = 100
    ): CompletableFuture<List<Secret>> =
        CompletableFuture.supplyAsync({ sync.listBySubType(subType, page, pageSize) }, executor)

    fun listByTypeAsync(
        type: SecretType,
        page: Int = 1,
        pageSize: Int = 100
    ): CompletableFuture<List<Secret>> =
        CompletableFuture.supplyAsync({ sync.listByType(type, page, pageSize) }, executor)

    fun listExpiringCertificatesAsync(
        beforeDate: Instant,
        page: Int = 1,
        pageSize: Int = 100
    ): CompletableFuture<List<Secret>> =
        CompletableFuture.supplyAsync({ sync.listExpiringCertificates(beforeDate, page, pageSize) }, executor)

    fun listExpiringAsync(
        beforeDate: Instant,
        page: Int = 1,
        pageSize: Int = 100
    ): CompletableFuture<List<Secret>> =
        CompletableFuture.supplyAsync({ sync.listExpiring(beforeDate, page, pageSize) }, executor)

    fun listByPathAsync(
        aliasPrefix: String,
        page: Int = 1,
        pageSize: Int = 100
    ): CompletableFuture<List<Secret>> =
        CompletableFuture.supplyAsync({ sync.listByPath(aliasPrefix, page, pageSize) }, executor)

    fun downloadFileWithMetadataAsync(id: String): CompletableFuture<Triple<ByteArray, String, String>> =
        CompletableFuture.supplyAsync({ sync.downloadFileWithMetadata(id) }, executor)

    // Keypair generation and publishing

    fun generateKeypairAsync(request: GenerateKeypairRequest): CompletableFuture<GeneratedKeypair> =
        CompletableFuture.supplyAsync({ sync.generateKeypair(request) }, executor)

    fun generateKeypairAsync(
        algorithm: KeypairAlgorithm,
        alias: String,
        tenant: String,
        rsaBits: Int? = null,
        ecdsaCurve: EcdsaCurve? = null,
        comment: String? = null,
        publishPublicKey: Boolean? = null,
        tags: List<String> = emptyList()
    ): CompletableFuture<GeneratedKeypair> =
        CompletableFuture.supplyAsync({ sync.generateKeypair(algorithm, alias, tenant, rsaBits, ecdsaCurve, comment, publishPublicKey, tags) }, executor)

    fun publishAsync(secretId: String): CompletableFuture<PublishResult> =
        CompletableFuture.supplyAsync({ sync.publish(secretId) }, executor)

    fun unpublishAsync(secretId: String): CompletableFuture<Void> =
        CompletableFuture.runAsync({ sync.unpublish(secretId) }, executor)

    fun getPublicKeyAsync(tenant: String, alias: String): CompletableFuture<PublicKeyInfo> =
        CompletableFuture.supplyAsync({ sync.getPublicKey(tenant, alias) }, executor)

    fun listPublicKeysAsync(tenant: String): CompletableFuture<List<PublicKeyInfo>> =
        CompletableFuture.supplyAsync({ sync.listPublicKeys(tenant) }, executor)
}

/**
 * Async wrapper for KmsClient using CompletableFuture.
 */
class KmsClientAsync @JvmOverloads constructor(
    private val sync: KmsClient,
    private val executor: Executor = defaultExecutor
) {

    fun createKeyAsync(request: CreateKeyRequest): CompletableFuture<KmsKey> =
        CompletableFuture.supplyAsync({ sync.createKey(request) }, executor)

    fun createKeyAsync(
        tenant: String,
        alias: String? = null,
        description: String? = null
    ): CompletableFuture<KmsKey> =
        CompletableFuture.supplyAsync({ sync.createKey(tenant, alias, description) }, executor)

    fun getKeyAsync(keyId: String): CompletableFuture<KmsKey> =
        CompletableFuture.supplyAsync({ sync.getKey(keyId) }, executor)

    fun listKeysAsync(filter: KeyFilter = KeyFilter()): CompletableFuture<KeyListResponse> =
        CompletableFuture.supplyAsync({ sync.listKeys(filter) }, executor)

    fun enableKeyAsync(keyId: String): CompletableFuture<KmsKey> =
        CompletableFuture.supplyAsync({ sync.enableKey(keyId) }, executor)

    fun disableKeyAsync(keyId: String): CompletableFuture<KmsKey> =
        CompletableFuture.supplyAsync({ sync.disableKey(keyId) }, executor)

    fun encryptAsync(
        keyId: String,
        plaintext: ByteArray,
        context: Map<String, String> = emptyMap()
    ): CompletableFuture<EncryptResult> =
        CompletableFuture.supplyAsync({ sync.encrypt(keyId, plaintext, context) }, executor)

    fun encryptAsync(
        keyId: String,
        plaintext: String,
        context: Map<String, String> = emptyMap()
    ): CompletableFuture<EncryptResult> =
        CompletableFuture.supplyAsync({ sync.encrypt(keyId, plaintext, context) }, executor)

    fun decryptAsync(
        keyId: String,
        ciphertextBlob: String,
        context: Map<String, String> = emptyMap()
    ): CompletableFuture<ByteArray> =
        CompletableFuture.supplyAsync({ sync.decrypt(keyId, ciphertextBlob, context) }, executor)

    fun decryptToStringAsync(
        keyId: String,
        ciphertextBlob: String,
        context: Map<String, String> = emptyMap()
    ): CompletableFuture<String> =
        CompletableFuture.supplyAsync({ sync.decryptToString(keyId, ciphertextBlob, context) }, executor)

    fun generateDataKeyAsync(
        keyId: String,
        keySpec: KeySpec = KeySpec.AES_256,
        context: Map<String, String> = emptyMap()
    ): CompletableFuture<DataKeyResult> =
        CompletableFuture.supplyAsync({ sync.generateDataKey(keyId, keySpec, context) }, executor)

    fun rotateKeyAsync(keyId: String): CompletableFuture<KmsKey> =
        CompletableFuture.supplyAsync({ sync.rotateKey(keyId) }, executor)
}

/**
 * Async wrapper for TenantClient using CompletableFuture.
 */
class TenantClientAsync @JvmOverloads constructor(
    private val sync: TenantClient,
    private val executor: Executor = defaultExecutor
) {

    fun createAsync(request: CreateTenantRequest): CompletableFuture<Tenant> =
        CompletableFuture.supplyAsync({ sync.create(request) }, executor)

    fun createAsync(id: String, name: String): CompletableFuture<Tenant> =
        CompletableFuture.supplyAsync({ sync.create(id, name) }, executor)

    fun getAsync(id: String): CompletableFuture<Tenant> =
        CompletableFuture.supplyAsync({ sync.get(id) }, executor)

    fun listAsync(filter: TenantFilter = TenantFilter()): CompletableFuture<Page<Tenant>> =
        CompletableFuture.supplyAsync({ sync.list(filter) }, executor)

    fun updateAsync(id: String, request: UpdateTenantRequest): CompletableFuture<Tenant> =
        CompletableFuture.supplyAsync({ sync.update(id, request) }, executor)

    fun updateStatusAsync(id: String, status: TenantStatus): CompletableFuture<Tenant> =
        CompletableFuture.supplyAsync({ sync.updateStatus(id, status) }, executor)

    fun suspendAsync(id: String): CompletableFuture<Tenant> =
        CompletableFuture.supplyAsync({ sync.suspend(id) }, executor)

    fun activateAsync(id: String): CompletableFuture<Tenant> =
        CompletableFuture.supplyAsync({ sync.activate(id) }, executor)

    fun archiveAsync(id: String): CompletableFuture<Void> =
        CompletableFuture.runAsync({ sync.archive(id) }, executor)

    fun getUsageAsync(id: String): CompletableFuture<TenantUsage> =
        CompletableFuture.supplyAsync({ sync.getUsage(id) }, executor)
}

/**
 * Async wrapper for UserClient using CompletableFuture.
 */
class UserClientAsync @JvmOverloads constructor(
    private val sync: UserClient,
    private val executor: Executor = defaultExecutor
) {

    fun createAsync(request: CreateUserRequest): CompletableFuture<User> =
        CompletableFuture.supplyAsync({ sync.create(request) }, executor)

    fun createAsync(
        username: String,
        password: String,
        email: String? = null,
        tenantId: String? = null
    ): CompletableFuture<User> =
        CompletableFuture.supplyAsync({ sync.create(username, password, email, tenantId) }, executor)

    fun getAsync(id: String): CompletableFuture<User> =
        CompletableFuture.supplyAsync({ sync.get(id) }, executor)

    fun listAsync(filter: UserFilter = UserFilter()): CompletableFuture<Page<User>> =
        CompletableFuture.supplyAsync({ sync.list(filter) }, executor)

    fun updateAsync(id: String, request: UpdateUserRequest): CompletableFuture<User> =
        CompletableFuture.supplyAsync({ sync.update(id, request) }, executor)

    fun resetPasswordAsync(id: String, newPassword: String): CompletableFuture<User> =
        CompletableFuture.supplyAsync({ sync.resetPassword(id, newPassword) }, executor)

    fun deleteAsync(id: String): CompletableFuture<Void> =
        CompletableFuture.runAsync({ sync.delete(id) }, executor)

    fun disableAsync(id: String): CompletableFuture<User> =
        CompletableFuture.supplyAsync({ sync.disable(id) }, executor)

    fun enableAsync(id: String): CompletableFuture<User> =
        CompletableFuture.supplyAsync({ sync.enable(id) }, executor)
}

/**
 * Async wrapper for AuditClient using CompletableFuture.
 */
class AuditClientAsync @JvmOverloads constructor(
    private val sync: AuditClient,
    private val executor: Executor = defaultExecutor
) {

    fun listAsync(filter: AuditFilter = AuditFilter()): CompletableFuture<AuditPage> =
        CompletableFuture.supplyAsync({ sync.list(filter) }, executor)

    fun getStatsAsync(period: StatsPeriod = StatsPeriod.DAY): CompletableFuture<AuditStats> =
        CompletableFuture.supplyAsync({ sync.getStats(period) }, executor)

    fun exportAsync(
        filter: AuditFilter = AuditFilter(),
        format: ExportFormat = ExportFormat.JSON
    ): CompletableFuture<String> =
        CompletableFuture.supplyAsync({ sync.export(filter, format) }, executor)

    fun verifyAsync(): CompletableFuture<AuditVerifyResult> =
        CompletableFuture.supplyAsync({ sync.verify() }, executor)

    fun isChainValidAsync(): CompletableFuture<Boolean> =
        CompletableFuture.supplyAsync({ sync.isChainValid() }, executor)
}

/**
 * Async wrapper for HealthClient using CompletableFuture.
 */
class HealthClientAsync @JvmOverloads constructor(
    private val sync: HealthClient,
    private val executor: Executor = defaultExecutor
) {

    fun getHealthAsync(): CompletableFuture<HealthStatus> =
        CompletableFuture.supplyAsync({ sync.getHealth() }, executor)

    fun isHealthyAsync(): CompletableFuture<Boolean> =
        CompletableFuture.supplyAsync({ sync.isHealthy() }, executor)

    fun isLiveAsync(): CompletableFuture<Boolean> =
        CompletableFuture.supplyAsync({ sync.isLive() }, executor)

    fun isReadyAsync(): CompletableFuture<Boolean> =
        CompletableFuture.supplyAsync({ sync.isReady() }, executor)

    fun pingAsync(): CompletableFuture<Boolean> =
        CompletableFuture.supplyAsync({ sync.ping() }, executor)
}
