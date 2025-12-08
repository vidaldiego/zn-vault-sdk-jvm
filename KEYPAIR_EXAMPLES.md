# Keypair Generation and Public Key Publishing

The ZN-Vault JVM SDK now supports cryptographic keypair generation and public key publishing.

## Features

- **Generate RSA, Ed25519, and ECDSA keypairs** server-side
- **Publish public keys** to make them publicly accessible without authentication
- **List published keys** for a tenant (unauthenticated access)
- **Retrieve specific public keys** by alias (unauthenticated access)

## API Methods

### SecretClient

```kotlin
// Generate a keypair
fun generateKeypair(request: GenerateKeypairRequest): GeneratedKeypair
fun generateKeypair(
    algorithm: KeypairAlgorithm,
    alias: String,
    tenant: String,
    rsaBits: Int? = null,
    ecdsaCurve: EcdsaCurve? = null,
    comment: String? = null,
    publishPublicKey: Boolean? = null,
    tags: List<String> = emptyList()
): GeneratedKeypair

// Publish a public key (make it publicly accessible)
fun publish(secretId: String): PublishResult

// Unpublish a public key (make it private again)
fun unpublish(secretId: String)

// Get a published public key (NO AUTH REQUIRED)
fun getPublicKey(tenant: String, alias: String): PublicKeyInfo

// List all published public keys for a tenant (NO AUTH REQUIRED)
fun listPublicKeys(tenant: String): List<PublicKeyInfo>
```

### Data Classes

```kotlin
enum class KeypairAlgorithm {
    RSA,        // RSA keypair (2048 or 4096 bits)
    Ed25519,    // Ed25519 keypair (fixed 256 bits)
    ECDSA       // ECDSA keypair (P-256 or P-384)
}

enum class EcdsaCurve {
    P_256,      // NIST P-256 curve
    P_384       // NIST P-384 curve
}

data class GeneratedKeypair(
    val privateKey: Secret,         // Private key secret metadata
    val publicKey: PublicKeyInfo    // Public key information
)

data class PublicKeyInfo(
    val id: String,
    val alias: String,
    val tenant: String?,
    val subType: SecretSubType?,
    val isPublic: Boolean?,
    val fingerprint: String?,       // SHA-256 fingerprint
    val algorithm: String?,         // "RSA", "Ed25519", "ECDSA"
    val bits: Int?,                 // Key size in bits
    val publicKeyPem: String?,      // PEM format
    val publicKeyOpenSSH: String?   // OpenSSH format
)

data class PublishResult(
    val message: String,
    val publicUrl: String,          // URL to access the public key
    val fingerprint: String?,
    val algorithm: String?
)
```

## Examples

### Kotlin

#### Generate an Ed25519 Keypair

```kotlin
import com.zincware.vault.ZnVaultClient
import com.zincware.vault.models.*

val client = ZnVaultClient.builder()
    .baseUrl("https://vault.example.com:8443")
    .apiKey("znv_xxx")
    .build()

// Generate Ed25519 keypair (most common for SSH)
val keypair = client.secrets.generateKeypair(
    algorithm = KeypairAlgorithm.Ed25519,
    alias = "ssh/deploy-key",
    tenant = "acme",
    comment = "Deployment key for CI/CD",
    publishPublicKey = true,  // Automatically publish the public key
    tags = listOf("ssh", "deployment")
)

println("Private key ID: ${keypair.privateKey.id}")
println("Private key alias: ${keypair.privateKey.alias}")
println("Public key ID: ${keypair.publicKey.id}")
println("Public key fingerprint: ${keypair.publicKey.fingerprint}")
println("OpenSSH format: ${keypair.publicKey.publicKeyOpenSSH}")
```

#### Generate an RSA Keypair

```kotlin
// Generate RSA-4096 keypair
val rsaKeypair = client.secrets.generateKeypair(
    algorithm = KeypairAlgorithm.RSA,
    alias = "ssl/server-key",
    tenant = "acme",
    rsaBits = 4096,  // 2048 or 4096
    comment = "TLS server key",
    tags = listOf("tls", "server")
)

println("Private key: ${rsaKeypair.privateKey.alias}")
println("Public key PEM: ${rsaKeypair.publicKey.publicKeyPem}")
```

#### Generate an ECDSA Keypair

```kotlin
// Generate ECDSA keypair with P-384 curve
val ecdsaKeypair = client.secrets.generateKeypair(
    algorithm = KeypairAlgorithm.ECDSA,
    alias = "ecdsa/signing-key",
    tenant = "acme",
    ecdsaCurve = EcdsaCurve.P_384,  // P_256 or P_384
    comment = "Document signing key",
    tags = listOf("signing", "ecdsa")
)

println("Algorithm: ${ecdsaKeypair.publicKey.algorithm}")
println("Bits: ${ecdsaKeypair.publicKey.bits}")
```

#### Publish a Public Key

```kotlin
// Generate keypair without publishing
val keypair = client.secrets.generateKeypair(
    algorithm = KeypairAlgorithm.Ed25519,
    alias = "ssh/user-key",
    tenant = "acme",
    publishPublicKey = false
)

// Publish the public key later
val result = client.secrets.publish(keypair.publicKey.id)
println("Published: ${result.publicUrl}")
println("Fingerprint: ${result.fingerprint}")
```

#### Unpublish a Public Key

```kotlin
// Make a published key private again
client.secrets.unpublish(keypair.publicKey.id)
println("Public key is now private")
```

#### Get a Published Public Key (No Authentication)

```kotlin
// Create a client WITHOUT authentication
val publicClient = ZnVaultClient.builder()
    .baseUrl("https://vault.example.com:8443")
    .build()

// Retrieve a published public key
val publicKey = publicClient.secrets.getPublicKey(
    tenant = "acme",
    alias = "ssh-deploy-key.pub"  // The public key alias
)

println("Algorithm: ${publicKey.algorithm}")
println("Fingerprint: ${publicKey.fingerprint}")
println("OpenSSH format: ${publicKey.publicKeyOpenSSH}")
```

#### List All Published Public Keys for a Tenant

```kotlin
// Create a client WITHOUT authentication
val publicClient = ZnVaultClient.builder()
    .baseUrl("https://vault.example.com:8443")
    .build()

// List all published keys for a tenant
val publicKeys = publicClient.secrets.listPublicKeys("acme")
publicKeys.forEach { key ->
    println("${key.alias} - ${key.algorithm} - ${key.fingerprint}")
}
```

### Java

#### Generate an Ed25519 Keypair

```java
import com.zincware.vault.ZnVaultClient;
import com.zincware.vault.models.*;

ZnVaultClient client = ZnVaultClient.builder()
    .baseUrl("https://vault.example.com:8443")
    .apiKey("znv_xxx")
    .build();

// Generate Ed25519 keypair
GeneratedKeypair keypair = client.getSecrets().generateKeypair(
    KeypairAlgorithm.Ed25519,
    "ssh/deploy-key",
    "acme",
    null,  // rsaBits (N/A for Ed25519)
    null,  // ecdsaCurve (N/A for Ed25519)
    "Deployment key for CI/CD",
    true,  // publishPublicKey
    List.of("ssh", "deployment")
);

System.out.println("Private key ID: " + keypair.getPrivateKey().getId());
System.out.println("Public key fingerprint: " + keypair.getPublicKey().getFingerprint());
```

#### Generate an RSA Keypair

```java
// Generate RSA-4096 keypair
GeneratedKeypair rsaKeypair = client.getSecrets().generateKeypair(
    KeypairAlgorithm.RSA,
    "ssl/server-key",
    "acme",
    4096,  // RSA bits
    null,  // ecdsaCurve (N/A for RSA)
    "TLS server key",
    false,
    List.of("tls", "server")
);
```

#### Publish and Retrieve

```java
// Publish the public key
PublishResult result = client.getSecrets().publish(keypair.getPublicKey().getId());
System.out.println("Published at: " + result.getPublicUrl());

// Create unauthenticated client
ZnVaultClient publicClient = ZnVaultClient.builder()
    .baseUrl("https://vault.example.com:8443")
    .build();

// Retrieve published public key
PublicKeyInfo publicKey = publicClient.getSecrets().getPublicKey(
    "acme",
    "ssh-deploy-key.pub"
);
System.out.println("Algorithm: " + publicKey.getAlgorithm());
```

### Async Operations (Java CompletableFuture)

```java
import com.zincware.vault.async.SecretClientAsync;

SecretClientAsync async = new SecretClientAsync(client.getSecrets());

// Generate keypair asynchronously
async.generateKeypairAsync(
    KeypairAlgorithm.Ed25519,
    "ssh/async-key",
    "acme",
    null, null, "Async key", true, List.of()
)
.thenCompose(keypair -> {
    System.out.println("Generated: " + keypair.getPublicKey().getFingerprint());
    return async.publishAsync(keypair.getPublicKey().getId());
})
.thenAccept(result -> {
    System.out.println("Published: " + result.getPublicUrl());
})
.exceptionally(e -> {
    e.printStackTrace();
    return null;
});
```

## Use Cases

### SSH Key Management

```kotlin
// Generate SSH deploy key
val sshKey = client.secrets.generateKeypair(
    algorithm = KeypairAlgorithm.Ed25519,
    alias = "ssh/github-deploy",
    tenant = "mycompany",
    comment = "GitHub deployment key",
    publishPublicKey = true,
    tags = listOf("ssh", "github", "deploy")
)

// The public key is now accessible at:
// GET https://vault.example.com:8443/v1/public/mycompany/ssh-github-deploy.pub
println("Add this to GitHub:\n${sshKey.publicKey.publicKeyOpenSSH}")

// Retrieve the private key when needed
val privateKeyData = client.secrets.decrypt(sshKey.privateKey.id)
val privateKeyPem = privateKeyData.data["privateKey"] as String
```

### TLS Certificate Generation

```kotlin
// Generate RSA keypair for TLS certificate
val tlsKey = client.secrets.generateKeypair(
    algorithm = KeypairAlgorithm.RSA,
    alias = "tls/server-2024",
    tenant = "acme",
    rsaBits = 4096,
    comment = "Production TLS key",
    publishPublicKey = false,  // Keep public key private
    tags = listOf("tls", "production")
)

// Use the private key for CSR generation
val privateKeyData = client.secrets.decrypt(tlsKey.privateKey.id)
// ... generate CSR and obtain certificate
```

### Code Signing

```kotlin
// Generate ECDSA keypair for code signing
val signingKey = client.secrets.generateKeypair(
    algorithm = KeypairAlgorithm.ECDSA,
    alias = "signing/release-2024",
    tenant = "acme",
    ecdsaCurve = EcdsaCurve.P_384,
    comment = "Release signing key",
    tags = listOf("signing", "release")
)
```

## Public Key Subtypes

The server automatically assigns appropriate subtypes:
- `ed25519_public_key` for Ed25519 public keys
- `rsa_public_key` for RSA public keys
- `ecdsa_public_key` for ECDSA public keys

Only these subtypes can be published using the `publish()` method.

## API Endpoints

| Method | Endpoint | Auth Required | Description |
|--------|----------|---------------|-------------|
| POST | `/v1/secrets/generate-keypair` | Yes | Generate a keypair |
| POST | `/v1/secrets/:id/publish` | Yes | Publish a public key |
| POST | `/v1/secrets/:id/unpublish` | Yes | Unpublish a public key |
| GET | `/v1/public/:tenant/:alias` | **No** | Get a published public key |
| GET | `/v1/public/:tenant` | **No** | List published public keys |

## Security Notes

- Private keys are encrypted at rest with AES-256-GCM
- Public keys can be made publicly accessible (no authentication required)
- Fingerprints are SHA-256 hashes for verification
- All keypair generation happens server-side (private keys never leave the vault)
- Published public keys are served over HTTPS
