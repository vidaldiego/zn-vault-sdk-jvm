# Changelog: Keypair Generation and Public Key Publishing APIs

**Date:** 2025-12-08
**SDK Version:** 1.0.2 (unreleased)

## Summary

Added comprehensive support for cryptographic keypair generation and public key publishing to the ZnVault JVM/Kotlin SDK. This update enables server-side keypair generation for RSA, Ed25519, and ECDSA algorithms, with the ability to publish public keys for unauthenticated access.

## New Features

### 1. Keypair Generation

Generate cryptographic keypairs server-side with support for:
- **RSA**: 2048 or 4096 bits
- **Ed25519**: Fixed 256 bits (SSH, signing)
- **ECDSA**: P-256 or P-384 curves

**Methods Added:**
- `SecretClient.generateKeypair(GenerateKeypairRequest): GeneratedKeypair`
- `SecretClient.generateKeypair(algorithm, alias, tenant, ...): GeneratedKeypair` (overload)

**Async Methods:**
- `SecretClientAsync.generateKeypairAsync(GenerateKeypairRequest): CompletableFuture<GeneratedKeypair>`
- `SecretClientAsync.generateKeypairAsync(algorithm, alias, tenant, ...): CompletableFuture<GeneratedKeypair>`

### 2. Public Key Publishing

Publish public keys to make them accessible without authentication.

**Methods Added:**
- `SecretClient.publish(secretId: String): PublishResult` - Publish a public key
- `SecretClient.unpublish(secretId: String)` - Unpublish a public key

**Async Methods:**
- `SecretClientAsync.publishAsync(secretId: String): CompletableFuture<PublishResult>`
- `SecretClientAsync.unpublishAsync(secretId: String): CompletableFuture<Void>`

### 3. Public Key Retrieval (Unauthenticated)

Access published public keys without authentication.

**Methods Added:**
- `SecretClient.getPublicKey(tenant: String, alias: String): PublicKeyInfo` - Get a specific public key
- `SecretClient.listPublicKeys(tenant: String): List<PublicKeyInfo>` - List all published keys for a tenant

**Async Methods:**
- `SecretClientAsync.getPublicKeyAsync(tenant, alias): CompletableFuture<PublicKeyInfo>`
- `SecretClientAsync.listPublicKeysAsync(tenant): CompletableFuture<List<PublicKeyInfo>>`

## New Data Models

### Enums

```kotlin
enum class KeypairAlgorithm {
    RSA,
    Ed25519,
    ECDSA
}

enum class EcdsaCurve {
    P_256,  // NIST P-256
    P_384   // NIST P-384
}

enum class SecretSubType {
    // ... existing values ...
    ED25519_PUBLIC_KEY,
    RSA_PUBLIC_KEY,
    ECDSA_PUBLIC_KEY
}
```

### Request/Response Classes

```kotlin
data class GenerateKeypairRequest(
    val algorithm: KeypairAlgorithm,
    val alias: String,
    val tenant: String,
    val rsaBits: Int? = null,
    val ecdsaCurve: EcdsaCurve? = null,
    val comment: String? = null,
    val publishPublicKey: Boolean? = null,
    val tags: List<String> = emptyList()
)

data class GeneratedKeypair(
    val privateKey: Secret,
    val publicKey: PublicKeyInfo
)

data class PublicKeyInfo(
    val id: String,
    val alias: String,
    val tenant: String?,
    val subType: SecretSubType?,
    val isPublic: Boolean?,
    val fingerprint: String?,
    val algorithm: String?,
    val bits: Int?,
    val publicKeyPem: String?,
    val publicKeyOpenSSH: String?
)

data class PublishResult(
    val message: String,
    val publicUrl: String,
    val fingerprint: String?,
    val algorithm: String?
)

data class PublicKeysListResponse(
    val tenant: String,
    val keys: List<PublicKeyInfo>
)
```

## API Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/v1/secrets/generate-keypair` | Required | Generate a keypair |
| POST | `/v1/secrets/:id/publish` | Required | Publish a public key |
| POST | `/v1/secrets/:id/unpublish` | Required | Unpublish a public key |
| GET | `/v1/public/:tenant/:alias` | **None** | Get a published public key |
| GET | `/v1/public/:tenant` | **None** | List published keys for tenant |

## Files Modified

### Core Module (`zn-vault-core`)

1. **`src/main/kotlin/com/zincapp/vault/models/Secret.kt`**
   - Added `KeypairAlgorithm` enum (RSA, Ed25519, ECDSA)
   - Added `EcdsaCurve` enum (P_256, P_384)
   - Added `GenerateKeypairRequest` data class
   - Added `PublicKeyInfo` data class
   - Added `GeneratedKeypair` data class
   - Added `PublishResult` data class
   - Added `PublicKeysListResponse` data class
   - Updated `SecretSubType` enum with public key types (ED25519_PUBLIC_KEY, RSA_PUBLIC_KEY, ECDSA_PUBLIC_KEY)
   - Updated `subTypeToType` map to include new public key subtypes

2. **`src/main/kotlin/com/zincapp/vault/secrets/SecretClient.kt`**
   - Added `generateKeypair(GenerateKeypairRequest): GeneratedKeypair` method
   - Added `generateKeypair(algorithm, alias, tenant, ...): GeneratedKeypair` overload
   - Added `publish(secretId: String): PublishResult` method
   - Added `unpublish(secretId: String)` method
   - Added `getPublicKey(tenant: String, alias: String): PublicKeyInfo` method
   - Added `listPublicKeys(tenant: String): List<PublicKeyInfo>` method

3. **`src/main/kotlin/com/zincapp/vault/async/AsyncWrappers.kt`**
   - Added `generateKeypairAsync(GenerateKeypairRequest): CompletableFuture<GeneratedKeypair>`
   - Added `generateKeypairAsync(algorithm, alias, tenant, ...): CompletableFuture<GeneratedKeypair>`
   - Added `publishAsync(secretId: String): CompletableFuture<PublishResult>`
   - Added `unpublishAsync(secretId: String): CompletableFuture<Void>`
   - Added `getPublicKeyAsync(tenant, alias): CompletableFuture<PublicKeyInfo>`
   - Added `listPublicKeysAsync(tenant): CompletableFuture<List<PublicKeyInfo>>`

### Documentation

1. **`README.md`**
   - Updated "Available Clients" table to mention keypair generation and public key publishing
   - Added new "Keypair Generation & Public Key Publishing" section with examples

2. **`KEYPAIR_EXAMPLES.md`** (new file)
   - Comprehensive guide with Kotlin and Java examples
   - Use cases: SSH key management, TLS certificates, code signing
   - Detailed API documentation
   - Security notes

3. **`CHANGELOG_KEYPAIR.md`** (new file)
   - This file - detailed changelog for the keypair features

## Usage Examples

### Kotlin

```kotlin
// Generate Ed25519 keypair for SSH
val keypair = client.secrets.generateKeypair(
    algorithm = KeypairAlgorithm.Ed25519,
    alias = "ssh/deploy-key",
    tenant = "acme",
    publishPublicKey = true,
    tags = listOf("ssh", "deployment")
)

println("Fingerprint: ${keypair.publicKey.fingerprint}")
println("OpenSSH: ${keypair.publicKey.publicKeyOpenSSH}")

// Access published key without auth
val publicClient = ZnVaultClient.builder()
    .baseUrl("https://vault.example.com:8443")
    .build()

val publicKey = publicClient.secrets.getPublicKey("acme", "ssh-deploy-key.pub")
```

### Java

```java
// Generate RSA keypair
GeneratedKeypair keypair = client.getSecrets().generateKeypair(
    KeypairAlgorithm.RSA,
    "tls/server-key",
    "acme",
    4096,  // RSA bits
    null,  // ECDSA curve (N/A)
    "TLS server key",
    false,
    List.of("tls", "server")
);

// Publish the public key
PublishResult result = client.getSecrets().publish(keypair.getPublicKey().getId());
System.out.println("Published at: " + result.getPublicUrl());
```

## Breaking Changes

None. This is a backward-compatible addition to the SDK.

## Compatibility

- **ZnVault Server:** Requires server version with keypair generation support (POST /v1/secrets/generate-keypair endpoint)
- **Java Version:** Java 11+
- **Kotlin Version:** 1.9+

## Testing

The SDK successfully compiles with:
```bash
./gradlew :zn-vault-core:build -x test
```

Integration tests should be added to verify:
- Ed25519 keypair generation
- RSA keypair generation (2048 and 4096 bits)
- ECDSA keypair generation (P-256 and P-384)
- Public key publishing and unpublishing
- Unauthenticated public key retrieval
- Listing published keys for a tenant

## Migration Guide

No migration required. Existing code continues to work without changes.

To use the new features, simply add the new method calls:

```kotlin
// Before: Manual keypair creation
val privateKeyBytes = generateKeypairLocally()
val publicKeyBytes = extractPublicKey(privateKeyBytes)
client.secrets.createKeypair("ssh/key", privateKeyBytes, publicKeyBytes)

// After: Server-side generation
val keypair = client.secrets.generateKeypair(
    algorithm = KeypairAlgorithm.Ed25519,
    alias = "ssh/key",
    tenant = "acme"
)
// Private and public keys are automatically created and encrypted
```

## Next Steps

1. Add integration tests for all new methods
2. Update version to 1.0.2 in `gradle.properties`
3. Publish to Maven Central
4. Update official documentation at https://docs.zn-vault.com

## Credits

- **Author:** Claude (Anthropic)
- **Date:** 2025-12-08
- **SDK:** ZnVault JVM/Kotlin SDK
