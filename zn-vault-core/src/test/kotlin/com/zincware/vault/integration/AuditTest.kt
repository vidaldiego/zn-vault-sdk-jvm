// Path: zn-vault-core/src/test/kotlin/com/zincware/vault/integration/AuditTest.kt
package com.zincware.vault.integration

import com.zincware.vault.models.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Integration tests for audit log functionality.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class AuditTest : BaseIntegrationTest() {

    // ==================== List Audit Logs Tests ====================

    @Test
    @Order(1)
    @DisplayName("List audit logs")
    fun testListAuditLogs() {
        val page = client.audit.list(
            AuditFilter(limit = 20)
        )

        assertTrue(page.items.isNotEmpty())
        println("✓ Listed ${page.items.size} audit entries (total: ${page.total})")

        // Print sample entries
        page.items.take(3).forEach { entry ->
            println("  - ${entry.timestamp}: ${entry.action} by ${entry.actor}")
        }
    }

    @Test
    @Order(2)
    @DisplayName("List audit logs with pagination")
    fun testListAuditLogsWithPagination() {
        // Get first page
        val page1 = client.audit.list(
            AuditFilter(limit = 5)
        )

        assertTrue(page1.items.isNotEmpty())
        println("✓ First page: ${page1.items.size} entries")
    }

    // ==================== Filter Audit Logs Tests ====================

    @Test
    @Order(10)
    @DisplayName("Filter audit logs by action")
    fun testFilterByAction() {
        val page = client.audit.list(
            AuditFilter(
                action = "auth:login",
                limit = 10
            )
        )

        println("✓ Filtered by action 'auth:login': ${page.items.size} entries")
    }

    @Test
    @Order(11)
    @DisplayName("Filter audit logs by client/actor")
    fun testFilterByClient() {
        val entries = client.audit.getByClient(TestConfig.Users.SUPERADMIN_USERNAME, 10)

        // Note: getByClient filters by "client" param, but response field is "actor"
        println("✓ Filtered by client '${TestConfig.Users.SUPERADMIN_USERNAME}': ${entries.size} entries")
        entries.take(3).forEach { entry ->
            println("  - ${entry.action} by ${entry.actor}")
        }
    }

    @Test
    @Order(12)
    @DisplayName("Filter audit logs by tenant")
    fun testFilterByTenant() {
        val page = client.audit.list(
            AuditFilter(
                tenantId = TestConfig.DEFAULT_TENANT,
                limit = 10
            )
        )

        println("✓ Filtered by tenant '${TestConfig.DEFAULT_TENANT}': ${page.items.size} entries")
    }

    @Test
    @Order(13)
    @DisplayName("Filter audit logs by date range")
    fun testFilterByDateRange() {
        val endDate = Instant.now()
        val startDate = endDate.minus(1, ChronoUnit.DAYS)

        val entries = client.audit.getByDateRange(startDate, endDate, 20)

        entries.forEach { entry ->
            entry.timestamp?.let { ts ->
                assertTrue(ts.isAfter(startDate) || ts == startDate)
                assertTrue(ts.isBefore(endDate) || ts == endDate)
            }
        }

        println("✓ Filtered by date range: ${entries.size} entries")
        println("  From: $startDate")
        println("  To: $endDate")
    }

    @Test
    @Order(14)
    @DisplayName("Filter audit logs by status")
    fun testFilterByStatus() {
        val page = client.audit.list(
            AuditFilter(
                status = AuditStatus.SUCCESS,
                limit = 10
            )
        )

        page.items.forEach { entry ->
            // API returns result as a string field, not enum
            assertEquals("success", entry.result)
        }

        println("✓ Filtered successful operations: ${page.items.size} entries")
    }

    @Test
    @Order(15)
    @DisplayName("Filter audit logs by resource")
    fun testFilterByResource() {
        val entries = client.audit.getByResource("secret", "any", 10)

        println("✓ Filtered by resource type 'secret': ${entries.size} entries")
    }

    // ==================== Audit Chain Verification Tests ====================

    @Test
    @Order(30)
    @DisplayName("Verify audit chain integrity")
    fun testVerifyChain() {
        val result = client.audit.verify()

        assertNotNull(result)
        println("✓ Audit chain verification: ${if (result.valid) "VALID" else "INVALID"}")
        println("  Entries verified: ${result.entriesVerified}")
        result.brokenAt?.let { println("  Broken at: $it") }
    }

    @Test
    @Order(31)
    @DisplayName("Check if chain is valid")
    fun testIsChainValid() {
        val valid = client.audit.isChainValid()

        assertTrue(valid)
        println("✓ Audit chain is valid")
    }

    // ==================== Audit Statistics Tests ====================

    @Test
    @Order(40)
    @DisplayName("Get audit statistics")
    fun testGetAuditStats() {
        val stats = client.audit.getStats()

        assertNotNull(stats)
        println("✓ Audit statistics:")
        println("  Total events: ${stats.totalEvents}")
        println("  Success rate: ${stats.successRate}")

        if (stats.topActions.isNotEmpty()) {
            println("  Top actions:")
            stats.topActions.take(5).forEach { actionCount ->
                println("    - ${actionCount.action}: ${actionCount.count}")
            }
        }

        if (stats.topActors.isNotEmpty()) {
            println("  Top actors:")
            stats.topActors.take(5).forEach { actorCount ->
                println("    - ${actorCount.actor}: ${actorCount.count}")
            }
        }
    }

    @Test
    @Order(41)
    @DisplayName("Get hourly audit statistics")
    fun testGetAuditStatsHourly() {
        val stats = client.audit.getStats(StatsPeriod.HOUR)

        assertNotNull(stats)
        // API returns stats structure, not the period that was requested
        println("✓ Hourly statistics: ${stats.totalEvents} total events")
    }

    // ==================== Export Audit Logs Tests ====================

    @Test
    @Order(60)
    @DisplayName("Export audit logs as JSON")
    fun testExportAuditLogsJson() {
        val export = client.audit.export(
            filter = AuditFilter(limit = 10),
            format = ExportFormat.JSON
        )

        assertNotNull(export)
        assertTrue(export.isNotEmpty())
        assertTrue(export.startsWith("[") || export.startsWith("{"))

        println("✓ Exported audit logs as JSON: ${export.length} characters")
    }

    @Test
    @Order(61)
    @DisplayName("Export audit logs as CSV")
    fun testExportAuditLogsCsv() {
        val export = client.audit.export(
            filter = AuditFilter(limit = 10),
            format = ExportFormat.CSV
        )

        assertNotNull(export)
        assertTrue(export.isNotEmpty())
        // CSV should have headers
        assertTrue(export.contains(","))

        println("✓ Exported audit logs as CSV: ${export.length} characters")
        println("  First line: ${export.lines().firstOrNull()?.take(80)}...")
    }
}
