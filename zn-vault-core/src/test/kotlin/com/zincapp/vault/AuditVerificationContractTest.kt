package com.zincapp.vault
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.zincapp.vault.models.AuditVerifyResult
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
class AuditVerificationContractTest {
    private val json = """{"status": "verified", "valid": true, "reasons": [], "errors": [], "checkedEntries": 3, "readEntries": 3, "authenticatedEntries": 3, "unverifiedEntries": 0, "unsignedDetailsEntries": 0, "scope": "retained", "interval": {"firstId": 5, "lastId": 7}, "completed": true, "startedAt": "2026-11-01T00:00:00Z", "finishedAt": "2026-11-01T00:00:01Z", "consistency": "repeatable_read", "historicalCompleteness": "not_proven"}"""
    @Test fun preservesEvidence() {
        val result = jacksonObjectMapper().readValue(json, AuditVerifyResult::class.java)
        assertTrue(result.valid)
        assertEquals(3L, result.entriesVerified)
        assertEquals(5L, result.interval?.firstId)
        assertEquals("not_proven", result.historicalCompleteness)
    }
    @Test fun neverBlessesUnknownOrPartialEvidence() {
        for (status in listOf("invalid", "unverifiable", "error", "empty", "unknown")) {
            assertFalse(jacksonObjectMapper().readValue(json.replace("verified", status), AuditVerifyResult::class.java).valid)
        }
        assertFalse(jacksonObjectMapper().readValue("""{"valid":true}""", AuditVerifyResult::class.java).valid)
        assertFalse(jacksonObjectMapper().readValue(json.replace("retained", "tail"), AuditVerifyResult::class.java).valid)
    }
}
