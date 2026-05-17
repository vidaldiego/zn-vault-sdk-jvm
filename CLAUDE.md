# CLAUDE.md

This file provides guidance to Claude Code when working with code in this repository.

## Project Overview

ZnVault JVM SDK (`io.github.vidaldiego:zn-vault-core`) is the official Kotlin/Java client library for ZnVault secrets management. It provides full API coverage with Kotlin coroutines support and Java CompletableFuture async operations.

### Relationship to ZnVault Server

This SDK is part of the ZnVault ecosystem. The parent directory (`../`) contains the main ZnVault server - see `../CLAUDE.md` for server documentation.

```
zn-vault/                    # Parent - Vault server
├── src/                     # Server source code
├── zn-vault-sdk-jvm/        # THIS REPO - Kotlin/Java SDK
├── zn-vault-sdk-node/       # Node.js SDK
├── zn-vault-sdk-python/     # Python SDK
├── zn-vault-sdk-swift/      # Swift SDK
├── zn-vault-agent/          # Agent for certificate/secret sync
├── znvault-cli/             # Admin CLI
└── vault-secrets-app/       # macOS app
```

## Development Commands

```bash
# Build
./gradlew build

# Run tests
./gradlew test

# Build without tests
./gradlew build -x test

# Publish to local Maven
./gradlew publishToMavenLocal

# Check for dependency updates
./gradlew dependencyUpdates
```

### Integration Test Setup

Integration tests require a running vault instance:

```bash
# Set environment variables
export ZNVAULT_BASE_URL=https://localhost:8443
export ZNVAULT_USERNAME=admin
export ZNVAULT_PASSWORD=your-password

# Run integration tests
./gradlew integrationTest
```

## Architecture

```
src/main/kotlin/com/zincapp/vault/
├── ZnVaultClient.kt          # Main client builder
├── http/
│   ├── HttpClient.kt         # OkHttp-based HTTP client
│   ├── TlsConfig.kt          # TLS/mTLS configuration
│   └── auth/
│       ├── ApiKeyAuth.kt     # API key authentication
│       ├── FileApiKeyAuth.kt # File-based API key with auto-refresh
│       └── JwtAuth.kt        # JWT token management
├── clients/
│   ├── SecretsClient.kt      # Secrets CRUD
│   ├── KmsClient.kt          # Key Management Service
│   ├── AuthClient.kt         # Authentication operations
│   ├── UsersClient.kt        # User management
│   ├── RolesClient.kt        # Role management
│   ├── TenantsClient.kt      # Tenant management
│   ├── PoliciesClient.kt     # ABAC policy management
│   ├── AuditClient.kt        # Audit log operations
│   └── HealthClient.kt       # Health checks
├── models/                   # Data classes
├── exception/                # Exception classes
└── coroutines/               # Kotlin coroutines extensions
```

## Release Process

**Publishing is handled automatically by GitHub Actions CI/CD.**

### Steps to Release

1. Update version in `build.gradle.kts`:
   ```kotlin
   version = "X.Y.Z"
   ```

2. Commit the version bump:
   ```bash
   git add build.gradle.kts
   git commit -m "chore(release): vX.Y.Z"
   ```

3. Create and push tag:
   ```bash
   git tag vX.Y.Z
   git push origin main
   git push origin vX.Y.Z
   ```

4. GitHub Actions automatically:
   - Runs tests
   - Builds the JAR and sources
   - Signs artifacts with GPG
   - Publishes to Maven Central via Sonatype OSSRH

### Maven Central Packages

- **Core:** `io.github.vidaldiego:zn-vault-core`
- **Coroutines:** `io.github.vidaldiego:zn-vault-coroutines`
- **Registry:** https://central.sonatype.com/artifact/io.github.vidaldiego/zn-vault-core

### Verification

```bash
# Check published version on Maven Central
curl -s "https://search.maven.org/solrsearch/select?q=g:io.github.vidaldiego+AND+a:zn-vault-core&rows=1&wt=json" | jq '.response.docs[0].latestVersion'
```

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("io.github.vidaldiego:zn-vault-core:X.Y.Z")
    implementation("io.github.vidaldiego:zn-vault-coroutines:X.Y.Z")  // Optional
}
```

### Maven

```xml
<dependency>
    <groupId>io.github.vidaldiego</groupId>
    <artifactId>zn-vault-core</artifactId>
    <version>X.Y.Z</version>
</dependency>
```

### CI/CD Configuration

The GitHub Actions workflow (`.github/workflows/publish.yml`) handles:
- Running tests on PRs
- Publishing to Maven Central on version tags (`v*`)
- GPG signing of artifacts
- Automatic Sonatype staging and release

### Required Secrets for Publishing

GitHub repository secrets needed:
- `OSSRH_USERNAME` - Sonatype OSSRH username
- `OSSRH_PASSWORD` - Sonatype OSSRH password
- `GPG_SIGNING_KEY` - GPG private key for signing
- `GPG_PASSPHRASE` - GPG key passphrase

## Code Standards

- **Kotlin**: 1.9+ with explicit API mode
- **Java Compatibility**: Java 11+
- **Testing**: JUnit 5 for unit and integration tests
- **Async**: Kotlin coroutines + Java CompletableFuture
