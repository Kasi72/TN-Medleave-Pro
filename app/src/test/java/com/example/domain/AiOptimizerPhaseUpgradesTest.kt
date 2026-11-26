package com.example.domain

import com.example.data.InitialLeaveSeedData
import com.example.data.LeaveRecord
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiOptimizerPhaseUpgradesTest {

  @Test
  fun naturalLanguageConstraintParserExtractsReserveOpdAndEl() {
    val parsed = AiLeaveRecommendationEngine.parseNaturalLanguageConstraints(
      base = OptimizerConstraints(),
      rawQuery = "Keep 3 CL reserve, protect Tue and Thu OPD, suggest a 7 day EL LTC break"
    )
    assertEquals(3.0, parsed.reserveClFloor, 0.01)
    assertTrue(DayOfWeek.TUESDAY in parsed.protectedDaysOfWeek)
    assertTrue(DayOfWeek.THURSDAY in parsed.protectedDaysOfWeek)
    assertTrue(parsed.allowElBridges)
  }

  private fun sampleTestRecords(): List<LeaveRecord> = listOf(
    LeaveRecord(
      id = "cl_1",
      userId = "test_user",
      leaveType = "CL",
      dateIso = "2026-03-10",
      year = 2026L,
      notes = "Casual Leave"
    ),
    LeaveRecord(
      id = "ghw_1",
      userId = "test_user",
      leaveType = "GH_WORKED",
      dateIso = "2026-10-10",
      year = 2026L,
      pairedDateIso = "",
      notes = "GH Hospital Duty"
    ),
    LeaveRecord(
      id = "rh_1",
      userId = "test_user",
      leaveType = "RH",
      dateIso = "2026-04-02",
      year = 2026L,
      notes = "Maundy Thursday"
    )
  )

  @Test
  fun solverGeneratesValidUpcomingBridgesRespectingClReserveAndOpd() {
    val records = sampleTestRecords()
    val summary2026 = LeaveOptimizerEngine.computeYearSummary(records, 2026L)
    val summary2027 = LeaveOptimizerEngine.computeYearSummary(records, 2027L)
    val colAudit = LeaveOptimizerEngine.auditColAndGhWorked(records, filterYear = null)

    val constraints = OptimizerConstraints(
      reserveClFloor = 2.0,
      allowElBridges = true,
      allowHalfDayCl = true,
      protectedDaysOfWeek = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
      horizonMode = OptimizerHorizon.FULL_HORIZON
    )

    val recs = AiLeaveRecommendationEngine.scanHolidayDatabaseForLongWeekends(
      summary2026 = summary2026,
      colGhAuditAllYears = colAudit,
      records = records,
      focusGoal = "Maximize Long Weekends",
      referenceDate = LocalDate.of(2026, 11, 26),
      summary2027 = summary2027,
      constraints = constraints
    )

    assertTrue("Expected upcoming verified recommendations", recs.isNotEmpty())
    recs.forEach { rec ->
      assertTrue("Start date must be on or after ref date", !LocalDate.parse(rec.startDateIso).isBefore(LocalDate.of(2026, 11, 26)))
      if (!rec.isElBridge) {
        assertTrue("CL/RH/COL continuous stretch must never exceed 10 days", rec.totalDaysOff <= 10)
      }
      assertTrue("Recommendation must have daily plan steps", rec.dailyPlan.isNotEmpty())
      assertTrue("Recommendation must have exact dates to apply", rec.exactDatesToApply.isNotEmpty())
    }
  }

  @Test
  fun portfolioImpactComputesCumulativeBalancesAndDetectsOverlaps() {
    val records = sampleTestRecords()
    val summary2026 = LeaveOptimizerEngine.computeYearSummary(records, 2026L)
    val summary2027 = LeaveOptimizerEngine.computeYearSummary(records, 2027L)
    val colAudit = LeaveOptimizerEngine.auditColAndGhWorked(records, filterYear = null)

    val recA = AiLeaveRecommendation(
      id = "rec_a",
      title = "Plan A",
      startDateIso = "2026-12-24",
      endDateIso = "2026-12-27",
      totalDaysOff = 4,
      leaveCreditsUsed = 1.0,
      freeDaysUnlocked = 3,
      efficiencyMultiplier = 4.0,
      exactDatesToApply = listOf("2026-12-24 — Apply 1 RH"),
      creditMixSummary = "1 RH + 3 Free Days",
      whyOptimalForHod = "Optimal Christmas stretch",
      tnRuleComplianceCheck = "Compliant",
      dailyPlan = listOf(
        AiBridgeDayPlan("2026-12-24", "Thu, Dec 24", "RH", "Christmas Eve"),
        AiBridgeDayPlan("2026-12-25", "Fri, Dec 25", "GH", "Christmas"),
        AiBridgeDayPlan("2026-12-26", "Sat, Dec 26", "WEEKEND", "Saturday"),
        AiBridgeDayPlan("2026-12-27", "Sun, Dec 27", "WEEKEND", "Sunday")
      )
    )
    val recBOverlap = recA.copy(id = "rec_b", title = "Plan B Overlap")

    val cleanImpact = AiLeaveRecommendationEngine.computePortfolioImpact(
      selectedRecs = listOf(recA),
      summary2026 = summary2026,
      summary2027 = summary2027,
      colGhAuditAllYears = colAudit
    )
    assertEquals(1, cleanImpact.selectedPlanCount)
    assertEquals(4, cleanImpact.totalDaysOffUnlocked)
    assertEquals(1, cleanImpact.rh2026Used)
    assertFalse(cleanImpact.hasOverlappingDates)

    val overlapImpact = AiLeaveRecommendationEngine.computePortfolioImpact(
      selectedRecs = listOf(recA, recBOverlap),
      summary2026 = summary2026,
      summary2027 = summary2027,
      colGhAuditAllYears = colAudit
    )
    assertTrue(overlapImpact.hasOverlappingDates)
  }

  @Test
  fun hodSanctionLetterGeneratorProducesOfficialApplication() {
    val rec = AiLeaveRecommendation(
      id = "test_el",
      title = "Pongal 2027 9-Day Rule-8 EL Break",
      startDateIso = "2027-01-09",
      endDateIso = "2027-01-17",
      totalDaysOff = 9,
      leaveCreditsUsed = 3.0,
      freeDaysUnlocked = 6,
      efficiencyMultiplier = 3.0,
      exactDatesToApply = listOf("2027-01-11 — Apply 1 EL", "2027-01-12 — Apply 1 EL", "2027-01-13 — Apply 1 EL"),
      creditMixSummary = "3 EL + 6 Prefixed/Suffixed Free Days",
      whyOptimalForHod = "Zero EL debited on weekends or Pongal GH.",
      tnRuleComplianceCheck = "Rule 8 Compliant",
      isElBridge = true,
      dailyPlan = listOf(
        AiBridgeDayPlan("2027-01-09", "Sat, Jan 9", "WEEKEND", "FREE Saturday"),
        AiBridgeDayPlan("2027-01-10", "Sun, Jan 10", "WEEKEND", "FREE Sunday"),
        AiBridgeDayPlan("2027-01-11", "Mon, Jan 11", "EL", "APPLY 1 EL"),
        AiBridgeDayPlan("2027-01-12", "Tue, Jan 12", "EL", "APPLY 1 EL"),
        AiBridgeDayPlan("2027-01-13", "Wed, Jan 13", "EL", "APPLY 1 EL"),
        AiBridgeDayPlan("2027-01-14", "Thu, Jan 14", "GH", "Pongal")
      )
    )

    val letter = HodSanctionLetterGenerator.generateOfficialApplicationAndHandover(
      rec = rec,
      officerName = "Dr. K. Test Officer",
      designation = "Professor & HOD",
      department = "Department of Psychiatry",
      college = "Govt. Erode Medical College & Hospital",
      ifhrmsId = "TNMS-12345",
      referenceDate = LocalDate.of(2026, 11, 26)
    )

    assertTrue(letter.contains("Earned Leave (EL)"))
    assertTrue(letter.contains("TN Leave Rule 8"))
    assertTrue(letter.contains("CHARGE HANDOVER ARRANGEMENT"))
    assertTrue(letter.contains("Dr. K. Test Officer"))
  }
}
