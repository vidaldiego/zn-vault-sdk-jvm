# Releasing

This SDK uses automated tag-based releases via GitHub Actions.

## How to Release

1. **Update version** in `build.gradle.kts`:
   ```kotlin
   version = "X.Y.Z"
   ```

2. **Commit and push**:
   ```bash
   git add build.gradle.kts
   git commit -m "chore: bump version to X.Y.Z"
   git push origin main
   ```

3. **Create and push tag**:
   ```bash
   git tag vX.Y.Z
   git push origin vX.Y.Z
   ```

GitHub Actions will automatically:
- Build and test the project
- Sign artifacts with GPG
- Publish to Maven Central

## Artifacts Published

| Artifact | Maven Central |
|----------|---------------|
| `io.github.vidaldiego:zn-vault-core` | Core SDK |
| `io.github.vidaldiego:zn-vault-coroutines` | Kotlin Coroutines support |

## GitHub Secrets Required

These are configured in the repository settings:

| Secret | Description |
|--------|-------------|
| `GPG_SIGNING_KEY` | ASCII-armored GPG private key |
| `GPG_SIGNING_PASSWORD` | GPG key passphrase |
| `MAVEN_CENTRAL_USERNAME` | Maven Central Portal token username |
| `MAVEN_CENTRAL_PASSWORD` | Maven Central Portal token password |

## Verifying Release

After pushing a tag, check:
1. [GitHub Actions](https://github.com/vidaldiego/zn-vault-sdk-jvm/actions) - workflow status
2. [Maven Central](https://central.sonatype.com/artifact/io.github.vidaldiego/zn-vault-core) - published version (may take a few minutes to sync)
