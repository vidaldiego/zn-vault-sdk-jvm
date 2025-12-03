// Path: zn-vault-core/src/test/kotlin/com/zincware/vault/integration/BaseIntegrationTest.kt
package com.zincware.vault.integration

import com.zincware.vault.ZnVaultClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import java.util.UUID

/**
 * Base class for integration tests.
 */
abstract class BaseIntegrationTest {

    protected lateinit var client: ZnVaultClient
    protected lateinit var testId: String

    @BeforeEach
    fun setUp(testInfo: TestInfo) {
        testId = UUID.randomUUID().toString().substring(0, 8)
        println("\n=== Starting test: ${testInfo.displayName} (ID: $testId) ===")
        client = createClient()
    }

    @AfterEach
    fun tearDown(testInfo: TestInfo) {
        println("=== Finished test: ${testInfo.displayName} ===\n")
        cleanup()
    }

    /**
     * Create the client for this test.
     * Override to use different authentication.
     */
    protected open fun createClient(): ZnVaultClient {
        return TestConfig.createSuperadminClient()
    }

    /**
     * Cleanup resources created during the test.
     * Override in subclasses to add specific cleanup.
     */
    protected open fun cleanup() {
        // Default: nothing to clean up
    }

    /**
     * Generate a unique alias for testing.
     */
    protected fun uniqueAlias(prefix: String = "test"): String {
        return "$prefix/sdk-test/$testId/${UUID.randomUUID().toString().substring(0, 8)}"
    }

    /**
     * Generate a unique name for testing.
     */
    protected fun uniqueName(prefix: String = "test"): String {
        return "$prefix-$testId-${UUID.randomUUID().toString().substring(0, 8)}"
    }
}
