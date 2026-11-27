package com.example

import com.example.data.LeaveRecord
import com.example.data.LeaveRepository
import com.example.domain.ExpiryWatchdogEngine
import com.example.domain.WatchdogColUrgency
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpiredGhClearLogicTest {

  @Test
  fun watchdogIdentifiesOnlyUnclaimedExpiredGhWorkedRecords() {
    val today = LocalDate.of(2026, 11, 27)
    val records = listOf(
      // 1. Lapsed unclaimed GH Worked (2024-04-11 -> expires 2024-10-11)
      LeaveRecord(
        id = "seed_2024_gh_worked_2024-04-11_1",
        userId = "user1",
        leaveType = "GH_WORKED",
        dateIso = "2024-04-11",
        year = 2024L,
        notes = "GH Hospital Duty"
      ),
      // 2. Lapsed unclaimed GH Worked (2025-04-18 -> expires 2025-10-18)
      LeaveRecord(
        id = "seed_2025_gh_worked_2025-04-18_2",
        userId = "user1",
        leaveType = "GH_WORKED",
        dateIso = "2025-04-18",
        year = 2025L,
        notes = "Good Friday GH"
      ),
      // 3. Old GH Worked that WAS paired with a COL -> must NOT be flagged expired
      LeaveRecord(
        id = "seed_2024_gh_worked_2024-01-26_3",
        userId = "user1",
        leaveType = "GH_WORKED",
        dateIso = "2024-01-26",
        year = 2024L,
        notes = "Republic Day GH"
      ),
      LeaveRecord(
        id = "seed_2024_col_2024-02-15_4",
        userId = "user1",
        leaveType = "COL",
        dateIso = "2024-02-15",
        year = 2024L,
        pairedDateIso = "2024-01-26",
        notes = "Availed COL for Republic Day"
      ),
      // 4. Valid unexpired GH Worked (2026-10-10 -> expires 2027-04-10, 134d left)
      LeaveRecord(
        id = "seed_2026_gh_worked_2026-10-10_5",
        userId = "user1",
        leaveType = "GH_WORKED",
        dateIso = "2026-10-10",
        year = 2026L,
        notes = "Recent GH Duty"
      )
    )

    val summary = ExpiryWatchdogEngine.evaluate(
      records = records,
      workflowsByRecordId = emptyMap(),
      today = today,
      clearedLapsedRecordKeys = emptySet()
    )

    val expired = summary.expiringCols.filter { it.urgency == WatchdogColUrgency.EXPIRED }
    assertEquals(2, expired.size)
    assertEquals(2, summary.expiredColCount)
    assertTrue(summary.totalUrgentAlerts >= 2)
    assertEquals(
      setOf("seed_2024_gh_worked_2024-04-11_1", "seed_2025_gh_worked_2025-04-18_2"),
      expired.map { it.recordId }.toSet()
    )
    // Valid GH worked remains healthy and is not marked EXPIRED
    val validItem = summary.expiringCols.first { it.recordId == "seed_2026_gh_worked_2026-10-10_5" }
    assertEquals(WatchdogColUrgency.HEALTHY, validItem.urgency)
    // Paired GH worked is excluded from unclaimed list altogether
    assertTrue(summary.expiringCols.none { it.recordId == "seed_2024_gh_worked_2024-01-26_3" })
  }

  @Test
  fun firestoreDocIdValidationMatchesSecurityRulesRegex() {
    assertTrue(LeaveRepository.isValidFirestoreDocId("seed_2024_gh_worked_2024-04-11_3"))
    assertTrue(LeaveRepository.isValidFirestoreDocId("AbC123_def-456"))
    assertFalse(LeaveRepository.isValidFirestoreDocId(""))
    assertFalse(LeaveRepository.isValidFirestoreDocId("   "))
    assertFalse(LeaveRepository.isValidFirestoreDocId("bad/path/id"))
    assertFalse(LeaveRepository.isValidFirestoreDocId("has spaces"))
    assertFalse(LeaveRepository.isValidFirestoreDocId("a".repeat(129)))
  }

  @Test
  fun restorableLeaveRecordPayloadMatchesFirestoreSecurityRulesSchema() {
    val record = LeaveRecord(
      id = "seed_2024_gh_worked_2024-04-11_1",
      userId = "stale_uid",
      leaveType = "GH_WORKED",
      dateIso = "2024-04-11",
      year = 2024L,
      pairedDateIso = "",
      notes = "A".repeat(650),
      highlightStatus = "UNKNOWN_STATUS",
      isHalfDay = false
    )

    val payload = LeaveRepository.buildRestorableLeaveRecordPayload(record, uid = "auth_uid_123")
    assertTrue(payload != null)
    val map = payload!!
    assertEquals(
      setOf(
        "userId", "leaveType", "dateIso", "year", "pairedDateIso",
        "notes", "highlightStatus", "isHalfDay", "createdAt", "updatedAt"
      ),
      map.keys
    )
    assertEquals("auth_uid_123", map["userId"])
    assertEquals("GH_WORKED", map["leaveType"])
    assertEquals("2024-04-11", map["dateIso"])
    assertEquals(2024L, map["year"])
    assertEquals(500, (map["notes"] as String).length)
    assertEquals("NORMAL", map["highlightStatus"])

    // Malformed ISO date must be rejected before touching Firestore
    val invalidDateRecord = record.copy(dateIso = "11-04-2024")
    assertEquals(null, LeaveRepository.buildRestorableLeaveRecordPayload(invalidDateRecord, "auth_uid_123"))
  }

  @Test
  fun restorableWorkflowPayloadMatchesFirestoreSecurityRulesSchema() {
    val wf = com.example.data.LeaveWorkflowMeta(
      id = "seed_2024_gh_worked_2024-04-11_1",
      userId = "stale_uid",
      recordId = "seed_2024_gh_worked_2024-04-11_1",
      stage = "INVALID_STAGE",
      sanctionOrderNo = "ORD-" + "9".repeat(120),
      relievingOfficer = "Dr. Sharma",
      relievingContact = "9876543210",
      joiningDateIso = "bad-date",
      joiningSession = "INVALID",
      slipLocalFileName = "slip.jpg",
      workflowNotes = "Restored workflow"
    )

    val map = LeaveRepository.buildRestorableWorkflowPayload(
      meta = wf,
      uid = "auth_uid_123",
      cleanRecordId = "seed_2024_gh_worked_2024-04-11_1"
    )
    assertEquals(
      setOf(
        "userId", "recordId", "stage", "sanctionOrderNo", "relievingOfficer",
        "relievingContact", "joiningDateIso", "joiningSession", "slipLocalFileName",
        "workflowNotes", "createdAt", "updatedAt"
      ),
      map.keys
    )
    assertEquals("auth_uid_123", map["userId"])
    assertEquals("PLANNED", map["stage"])
    assertEquals(80, (map["sanctionOrderNo"] as String).length)
    assertEquals("", map["joiningDateIso"])
    assertEquals("FN", map["joiningSession"])
  }
}
