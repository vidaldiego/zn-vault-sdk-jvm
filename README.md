# ZN-Vault SDK for Java/Kotlin

[![Maven Central](https://img.shields.io/maven-central/v/io.github.vidaldiego/zn-vault-core.svg)](https://central.sonatype.com/artifact/io.github.vidaldiego/zn-vault-core)
[![Java 11+](https://img.shields.io/badge/Java-11+-blue.svg)](https://openjdk.java.net/)
[![Kotlin 1.9+](https://img.shields.io/badge/Kotlin-1.9+-purple.svg)](https://kotlinlang.org/)

Official Java/Kotlin client library for ZN-Vault secrets management.

**Maven Central:** https://central.sonatype.com/artifact/io.github.vidaldiego/zn-vault-core
**GitHub:** https://github.com/vidaldiego/zn-vault-sdk-jvm

## Features

- **Full API Coverage**: Secrets, KMS, tenants, users, roles, policies, audit logs
- **Multiple Auth Methods**: JWT tokens and API keys
- **Async Support**: Kotlin Coroutines and Java CompletableFuture
- **TLS/mTLS**: Custom CA certificates and client certificates
- **Retry Logic**: Configurable exponential backoff with jitter
- **Type-Safe**: Full Kotlin/Java interoperability

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    // Core SDK
    implementation("io.github.vidaldiego:zn-vault-core:1.0.0")

    // Optional: Kotlin Coroutines support
    implementation("io.github.vidaldiego:zn-vault-coroutines:1.0.0")
}
```

### Gradle (Groovy)

```groovy
dependencies {
    implementation 'io.github.vidaldiego:zn-vault-core:1.0.0'
    implementation 'io.github.vidaldiego:zn-vault-coroutines:1.0.0'
}
```

### Maven

```xml
<dependency>
    <groupId>io.github.vidaldiego</groupId>
    <artifactId>zn-vault-core</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- Optional: Kotlin Coroutines support -->
<dependency>
    <groupId>io.github.vidaldiego</groupId>
    <artifactId>zn-vault-coroutines</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Quick Start

### Kotlin with API Key

```kotlin
import com.zincware.vault.ZnVaultClient
import com.zincware.vault.models.SecretType

// Create client with API key (uses default URL: https://vault.zincapp.com)
val client = ZnVaultClient.withApiKey("znv_xxxx_your_api_key")

// Or with custom URL
val client = ZnVaultClient.builder()
    .baseUrl("https://vault.example.com:8443")
    .apiKey("znv_xxxx_your_api_key")
    .build()

// Create a secret
val secret = client.secrets.create(
    alias = "api/production/db-credentials",
    tenant = "acme",
    type = SecretType.CREDENTIAL,
    data = mapOf(
        "username" to "dbuser",
        "password" to "supersecret123"
    )
)
println("Created secret: ${secret.id}")

// Decrypt a secret
val data = client.secrets.decrypt(secret.id)
println("Username: ${data.data["username"]}")
```

### Java with API Key

```java
import com.zincware.vault.ZnVaultClient;
import com.zincware.vault.models.*;

// Create client (uses default URL: https://vault.zincapp.com)
ZnVaultClient client = ZnVaultClient.withApiKey("znv_xxxx_your_api_key");

// Or with custom URL
ZnVaultClient client = ZnVaultClient.builder()
    .baseUrl("https://vault.example.com:8443")
    .apiKey("znv_xxxx_your_api_key")
    .build();

// Create a secret
Secret secret = client.getSecrets().create(
    new CreateSecretRequest(
        "api/production/db-credentials",
        "acme",
        SecretType.CREDENTIAL,
        Map.of("username", "dbuser", "password", "secret123"),
        List.of(),
        null,
        null,
        null
    )
);

// Decrypt
SecretData data = client.getSecrets().decrypt(secret.getId());
System.out.println("Password: " + data.getData().get("password"));
```

### Username/Password Authentication

```kotlin
// Create client (uses default URL: https://vault.zincapp.com)
val client = ZnVaultClient.create()

// Login (tokens are managed automatically)
client.login("admin", "password")

// Or with TOTP for 2FA
client.login("admin", "password", totpCode = "123456")

// Use the client
val secrets = client.secrets.list()

// Logout when done
client.logout()
```

## Async Operations

### Kotlin Coroutines

```kotlin
import com.zincware.vault.coroutines.async
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.collect

val asyncClient = client.async()

runBlocking {
    // Create secret asynchronously
    val secret = asyncClient.secrets.create(
        CreateSecretRequest(
            alias = "api/prod/api-key",
            tenant = "acme",
            type = SecretType.OPAQUE,
            data = mapOf("key" to "sk_live_xxx")
        )
    )

    // Stream all secrets with Flow
    asyncClient.secrets.listAsFlow()
        .filter { it.tenant == "acme" }
        .collect { println(it.alias) }
}
```

### Java CompletableFuture

```java
import com.zincware.vault.async.SecretClientAsync;

SecretClientAsync async = new SecretClientAsync(client.getSecrets());

// Async operations
async.decryptAsync("secret-id")
    .thenAccept(data -> System.out.println(data.getData()))
    .exceptionally(e -> {
        logger.error("Failed to decrypt", e);
        return null;
    });

// Chain operations
async.getAsync("secret-id")
    .thenCompose(secret -> async.decryptAsync(secret.getId()))
    .thenAccept(data -> process(data));
```

## KMS Operations

```kotlin
// Create an encryption key
val key = client.kms.createKey(
    tenant = "acme",
    alias = "alias/my-app-key",
    description = "Application encryption key"
)

// Encrypt data
val encrypted = client.kms.encrypt(
    keyId = key.keyId,
    plaintext = "sensitive data",
    context = mapOf("app" to "myapp")
)

// Decrypt data
val decrypted = client.kms.decryptToString(
    keyId = key.keyId,
    ciphertextBlob = encrypted.ciphertextBlob,
    context = mapOf("app" to "myapp")
)

// Generate a data encryption key
val dataKey = client.kms.generateDataKey(key.keyId)
// Use dataKey.plaintext for encryption, store dataKey.ciphertextBlob
```

## File Storage

```kotlin
import java.io.File

// Upload a file
val secret = client.secrets.uploadFile(
    alias = "ssl/production/server-cert",
    tenant = "acme",
    file = File("/path/to/cert.pem"),
    tags = listOf("ssl", "production")
)

// Download a file
val certBytes = client.secrets.downloadFile(secret.id)

// Or download directly to a file
client.secrets.downloadFile(secret.id, File("/path/to/output.pem"))
```

## TLS Configuration

### Custom CA Certificate

```kotlin
val client = ZnVaultClient.builder()
    .baseUrl("https://vault.example.com:8443")
    .apiKey("znv_xxx")
    .caCertificate("/path/to/ca.pem")
    .build()
```

### Mutual TLS (mTLS)

```kotlin
import com.zincware.vault.http.TlsConfig

val tlsConfig = TlsConfig.builder()
    .caCertificate("/path/to/ca.pem")
    .clientCertificate("/path/to/client.pem")
    .clientKey("/path/to/client.key")
    .build()

val client = ZnVaultClient.builder()
    .baseUrl("https://vault.example.com:8443")
    .tlsConfig(tlsConfig)
    .build()
```

### Development (Insecure)

```kotlin
// WARNING: Only use for development!
val client = ZnVaultClient.builder()
    .baseUrl("https://localhost:8443")
    .apiKey("znv_xxx")
    .insecureTls()
    .debug(true)
    .build()
```

## Error Handling

```kotlin
import com.zincware.vault.exception.*

try {
    val secret = client.secrets.get("non-existent-id")
} catch (e: NotFoundException) {
    println("Secret not found: ${e.message}")
} catch (e: AuthenticationException) {
    println("Auth failed: ${e.message}")
} catch (e: AuthorizationException) {
    println("Permission denied: ${e.message}")
} catch (e: RateLimitException) {
    println("Rate limited, retry after: ${e.retryAfterSeconds}s")
} catch (e: ValidationException) {
    println("Validation errors: ${e.errors}")
} catch (e: ZnVaultException) {
    println("API error: ${e.message} (status: ${e.statusCode})")
}
```

## Configuration Options

```kotlin
import com.zincware.vault.http.RetryPolicy
import java.time.Duration

val client = ZnVaultClient.builder()
    .baseUrl("https://vault.example.com:8443")
    .apiKey("znv_xxx")
    .connectTimeout(Duration.ofSeconds(10))
    .readTimeout(Duration.ofSeconds(30))
    .writeTimeout(Duration.ofSeconds(30))
    .retryPolicy(RetryPolicy(
        maxRetries = 3,
        initialDelay = Duration.ofMillis(100),
        maxDelay = Duration.ofSeconds(10),
        multiplier = 2.0
    ))
    .debug(true)  // Enable request/response logging
    .build()
```

## Available Clients

| Client | Description |
|--------|-------------|
| `secrets` | Secret CRUD, encryption/decryption, file storage |
| `kms` | Key Management Service (CMKs, DEKs, encryption) |
| `auth` | Registration, API keys, 2FA management |
| `tenants` | Multi-tenant management |
| `users` | User CRUD and TOTP management |
| `roles` | RBAC role management |
| `policies` | ABAC policy management |
| `audit` | Audit logs, statistics, chain verification |
| `health` | Health checks and readiness probes |

## Requirements

- Java 11+
- Kotlin 1.9+ (for Kotlin features)

## Building from Source

```bash
git clone https://github.com/vidaldiego/zn-vault-sdk-jvm.git
cd zn-vault-sdk-jvm
./gradlew build
```

## License

Apache License 2.0
