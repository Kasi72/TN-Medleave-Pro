package com.example.domain

import com.example.data.ApprovalStage
import com.example.data.DutyShift
import com.example.data.DutyShiftType
import com.example.data.LeaveRecord
import com.example.data.LeaveWorkflowMeta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ClinicalWorkflowEnginesTest {

  @Test
  fun expiryWatchdog_classifiesColUrgencyBucketsAndFifoOrder() {
    val today = LocalDate.of(2026, 11, 20)
    val records = listOf(
      // Worked 2026-05-01 -> expires 2026-11-01 -> EXPIRED (-19d)
      LeaveRecord(id = "gh_expired", dateIso = "2026-05-01", year = 2026L, leaveType = "GH_WORKED", notes = "May Day Duty"),
      // Worked 2026-06-01 -> expires 2026-12-01 -> CRITICAL (11d left)
      LeaveRecord(id = "gh_critical", dateIso = "2026-06-01", year = 2026L, leaveType = "GH_WORKED", notes = "June GH Duty"),
      // Worked 2026-07-05 -> expires 2027-01-05 -> EXPIRING_SOON (46d left)
      LeaveRecord(id = "gh_soon", dateIso = "2026-07-05", year = 2026L, leaveType = "GH_WORKED", notes = "July GH Duty"),
      // Worked 2026-10-10 -> expires 2027-04-10 -> HEALTHY (141d left)
      LeaveRecord(id = "gh_safe", dateIso = "2026-10-10", year = 2026L, leaveType = "GH_WORKED", notes = "Oct GH Duty")
    )

    val summary = ExpiryWatchdogEngine.evaluate(records, emptyMap(), today)

    assertEquals(4, summary.expiringCols.size)
    assertEquals(1, summary.expiredColCount)
    assertEquals(1, summary.criticalColCount)
    assertEquals(1, summary.expiringSoonColCount)
    // FIFO sorted ascending by daysRemaining
    assertEquals("gh_expired", summary.expiringCols[0].recordId)
    assertEquals(WatchdogColUrgency.EXPIRED, summary.expiringCols[0].urgency)
    assertEquals("gh_critical", summary.expiringCols[1].recordId)
    assertEquals(WatchdogColUrgency.CRITICAL, summary.expiringCols[1].urgency)
    assertEquals("gh_soon", summary.expiringCols[2].recordId)
    assertEquals(WatchdogColUrgency.EXPIRING_SOON, summary.expiringCols[2].urgency)
    assertEquals("gh_safe", summary.expiringCols[3].recordId)
    assertEquals(WatchdogColUrgency.HEALTHY, summary.expiringCols[3].urgency)
  }

  @Test
  fun expiryWatchdog_computesClRlBurnDownAndPaperworkAlerts() {
    val today = LocalDate.of(2026, 11, 25) // 36 days left in year
    val records = listOf(
      // Only 4 CL used -> 8 remaining with 36 days left -> HIGH_LAPSE_RISK
      LeaveRecord(id = "cl_1", dateIso = "2026-02-10", year = 2026L, leaveType = "CL"),
      LeaveRecord(id = "cl_2", dateIso = "2026-03-10", year = 2026L, leaveType = "CL"),
      LeaveRecord(id = "cl_3", dateIso = "2026-04-10", year = 2026L, leaveType = "CL"),
      LeaveRecord(id = "cl_4", dateIso = "2026-05-10", year = 2026L, leaveType = "CL"),
      // Past EL without Joining Report -> JOINING_REPORT_DUE
      LeaveRecord(id = "el_past", dateIso = "2026-11-18", year = 2026L, leaveType = "EL"),
      // Upcoming CL in 5 days still PLANNED -> SUBMIT_TO_HOD_SOON
      LeaveRecord(id = "cl_upcoming", dateIso = "2026-11-30", year = 2026L, leaveType = "CL")
    )
    val workflows = mapOf(
      "el_past" to LeaveWorkflowMeta(recordId = "el_past", stage = ApprovalStage.SANCTIONED.code)
    )

    val summary = ExpiryWatchdogEngine.evaluate(records, workflows, today)

    assertEquals(7.0, summary.burnDown.clRemaining, 0.01)
    assertEquals(BurnDownRiskLevel.HIGH_LAPSE_RISK, summary.burnDown.clRiskLevel)
    assertEquals(3.0, summary.burnDown.rlRemaining, 0.01)
    assertEquals(BurnDownRiskLevel.HIGH_LAPSE_RISK, summary.burnDown.rlRiskLevel)

    assertTrue(summary.paperworkAlerts.any { it.recordId == "el_past" && it.alertType == PaperworkAlertType.JOINING_REPORT_DUE })
    assertTrue(summary.paperworkAlerts.any { it.recordId == "cl_upcoming" && it.alertType == PaperworkAlertType.SUBMIT_TO_HOD_SOON })
  }

  @Test
  fun dutyConflictEngine_detectsConflictsAndUnclaimedGhColEligibility() {
    val shifts = listOf(
      // 2026-01-26 is Republic Day (TN Public Holiday) -> eligible for +1 COL
      DutyShift(id = "s_gh", dateIso = "2026-01-26", year = 2026L, shiftType = DutyShiftType.CALL_24H.code, colClaimed = false),
      // 2026-11-29 is a Sunday -> eligible for +1 COL
      DutyShift(id = "s_sun", dateIso = "2026-11-29", year = 2026L, shiftType = DutyShiftType.OPD_DUTY.code, colClaimed = false),
      // 2026-11-24 is a regular Tuesday -> NOT eligible for GH COL
      DutyShift(id = "s_tue", dateIso = "2026-11-24", year = 2026L, shiftType = DutyShiftType.WARD_OT.code, colClaimed = false),
      // Already claimed shift -> skipped
      DutyShift(id = "s_claimed", dateIso = "2026-08-15", year = 2026L, shiftType = DutyShiftType.GH_EMERGENCY.code, colClaimed = true)
    )

    val conflicts = DutyConflictEngine.findDutyConflicts(listOf("2026-11-24", "2026-11-29", "2026-12-01"), shifts)
    assertEquals(2, conflicts.size)
    assertTrue(conflicts.first { it.dateIso == "2026-11-29" }.isHardConflict)
    assertFalse(conflicts.first { it.dateIso == "2026-11-24" }.isHardConflict)

    val protectedDates = DutyConflictEngine.extractProtectedDutyDates(shifts)
    assertTrue("2026-01-26" in protectedDates)
    assertTrue("2026-11-29" in protectedDates)
    assertFalse("2026-11-24" in protectedDates) // WARD_OT is soft conflict

    val candidates = DutyConflictEngine.findUnclaimedGhColCandidates(shifts, emptyList())
    assertEquals(2, candidates.size)
    assertNotNull(DutyConflictEngine.findUnclaimedGhCaptureForDate("2026-01-26", shifts, emptyList()))
    assertNotNull(DutyConflictEngine.findUnclaimedGhCaptureForDate("2026-11-29", shifts, emptyList()))
    assertNull(DutyConflictEngine.findUnclaimedGhCaptureForDate("2026-11-24", shifts, emptyList()))
    assertNull(DutyConflictEngine.findUnclaimedGhCaptureForDate("2026-08-15", shifts, emptyList()))
  }

  @Test
  fun joiningReportAndIcsGenerator_formatsOfficialTextAndExclusiveDtEnd() {
    // 2026-11-28 is Saturday -> next day Sunday (2026-11-29) is skipped -> default joining is Monday 2026-11-30
    val defaultJoin = JoiningReportAndShareGenerator.computeDefaultJoiningDateIso("2026-11-28")
    assertEquals("2026-11-30", defaultJoin)

    val rec = LeaveRecord(
      id = "rec_col_1",
      dateIso = "2026-11-28",
      year = 2026L,
      leaveType = "COL",
      pairedDateIso = "2026-08-15"
    )
    val wf = LeaveWorkflowMeta(
      recordId = "rec_col_1",
      stage = ApprovalStage.SANCTIONED.code,
      sanctionOrderNo = "Ref.No. 142/E1/2026",
      relievingOfficer = "Dr. S. Karthik",
      joiningDateIso = "2026-11-30",
      joiningSession = "FN"
    )

    val report = JoiningReportAndShareGenerator.generateOfficialJoiningReportText(
      officerName = "Dr. K. Anitha",
      designation = "Associate Professor",
      department = "Psychiatry",
      institution = "Govt. Erode Medical College",
      record = rec,
      workflow = wf
    )
    assertTrue(report.contains("JOINING REPORT (RETURN TO DUTY)"))
    assertTrue(report.contains("Forenoon (F.N.)"))
    assertTrue(report.contains("Ref.No. 142/E1/2026"))
    assertTrue(report.contains("Dr. S. Karthik"))
    assertTrue(report.contains("In lieu of GH Duty worked"))

    val handover = JoiningReportAndShareGenerator.generateClinicalHandoverMessage(
      officerName = "Dr. K. Anitha",
      designation = "Associate Professor",
      department = "Psychiatry",
      institution = "Govt. Erode Medical College",
      leaveLabel = "COL",
      startDateIso = "2026-11-28",
      endDateIso = "2026-11-28",
      workflow = wf
    )
    assertTrue(handover.contains("Clinical Charge Handover"))
    assertTrue(handover.contains("Dr. S. Karthik"))

    val ics = JoiningReportAndShareGenerator.buildIcsCalendarContent(
      title = "Leave: COL",
      startDateIso = "2026-11-28",
      endDateIso = "2026-11-28",
      description = "Sanctioned COL",
      location = "GEMCH, Perundurai"
    )
    assertTrue(ics.contains("BEGIN:VCALENDAR"))
    assertTrue(ics.contains("DTSTART;VALUE=DATE:20261128"))
    // RFC-5545 all-day DTEND is exclusive (+1 day)
    assertTrue(ics.contains("DTEND;VALUE=DATE:20261129"))
    assertTrue(ics.contains("END:VCALENDAR"))
  }
}
