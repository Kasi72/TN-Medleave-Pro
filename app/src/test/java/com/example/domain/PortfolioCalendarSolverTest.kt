package com.example.domain

import com.example.data.LeaveRecord
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PortfolioCalendarSolverTest {

  private fun baseRecords(): List<LeaveRecord> = listOf(
    LeaveRecord(
      id = "cl_taken_1",
      userId = "u1",
      leaveType = "CL",
      dateIso = "2026-03-10",
      year = 2026L,
      notes = "CL 1"
    ),
    LeaveRecord(
      id = "cl_taken_2",
      userId = "u1",
      leaveType = "CL",
      dateIso = "2026-06-15",
      year = 2026L,
      notes = "CL 2"
    ),
    LeaveRecord(
      id = "rh_taken_1",
      userId = "u1",
      leaveType = "RH",
      dateIso = "2026-04-02",
      year = 2026L,
      notes = "Maundy Thursday"
    ),
    LeaveRecord(
      id = "ghw_unused_1",
      userId = "u1",
      leaveType = "GH_WORKED",
      dateIso = "2026-10-10",
      year = 2026L,
      pairedDateIso = "",
      notes = "GH Hospital Duty"
    )
  )

  @Test
  fun weekendPolicyDefaultIsSecondSaturdayAndSundayOnly() {
    val defaults = OptimizerConstraints()
    assertEquals(WeekendPolicy.SECOND_SATURDAY_AND_SUNDAY, defaults.effectiveWeekendPolicy())

    // Dec 2026 Saturdays: 5 (1st), 12 (2nd), 19 (3rd), 26 (4th)
    assertFalse(defaults.isWeekendDay(LocalDate.of(2026, 12, 5)))
    assertTrue(defaults.isWeekendDay(LocalDate.of(2026, 12, 12)))
    assertFalse(defaults.isWeekendDay(LocalDate.of(2026, 12, 19)))
    assertFalse(defaults.isWeekendDay(LocalDate.of(2026, 12, 26)))
    assertTrue(defaults.isWeekendDay(LocalDate.of(2026, 12, 13))) // Sunday
  }

  @Test
  fun jointPortfolioNeverDoubleSpendsQuotaOrOverlapsDates() {
    val records = baseRecords()
    val summary2026 = LeaveOptimizerEngine.computeYearSummary(records, 2026L)
    val summary2027 = LeaveOptimizerEngine.computeYearSummary(records, 2027L)
    val colAudit = LeaveOptimizerEngine.auditColAndGhWorked(records, filterYear = null)

    val constraints = OptimizerConstraints(
      reserveClFloor = 2.0,
      allowElBridges = false,
      allowHalfDayCl = true,
      horizonMode = OptimizerHorizon.FULL_HORIZON,
      weekendPolicy = WeekendPolicy.SECOND_SATURDAY_AND_SUNDAY
    )

    val portfolio = PortfolioCalendarSolver.solveJointPortfolio(
      summary2026 = summary2026,
      colGhAuditAllYears = colAudit,
      records = records,
      focusGoal = "Maximize Long Weekends",
      summary2027 = summary2027,
      referenceDate = LocalDate.of(2026, 11, 26),
      constraints = constraints
    )

    assertTrue("Expected non-empty joint portfolio", portfolio.isNotEmpty())

    // Verify ZERO overlapping actionable dates and strict shared quota conservation
    val impact = AiLeaveRecommendationEngine.computePortfolioImpact(
      selectedRecs = portfolio,
      summary2026 = summary2026,
      summary2027 = summary2027,
      colGhAuditAllYears = colAudit,
      constraints = constraints
    )
    assertFalse("Joint portfolio must never overlap dates across cards", impact.hasOverlappingDates)
    assertFalse("Joint portfolio must respect CL safety reserve floor", impact.violatesClReserve)
    assertEquals("Joint portfolio must never exceed statutory quota", null, impact.exceedsQuotaMessage)
    assertTrue(impact.colUsed <= colAudit.availableUnusedGhCount)
    assertTrue(impact.rh2026Used <= summary2026.rhRemaining)
    assertTrue(impact.rh2027Used <= summary2027.rhRemaining)
  }

  @Test
  fun pinnedAndBlockedDatesAreStrictlyRespected() {
    val records = baseRecords()
    val summary2026 = LeaveOptimizerEngine.computeYearSummary(records, 2026L)
    val summary2027 = LeaveOptimizerEngine.computeYearSummary(records, 2027L)
    val colAudit = LeaveOptimizerEngine.auditColAndGhWorked(records, filterYear = null)

    val pinnedDate = "2026-12-24"
    val blockedDate = "2026-12-28"

    val constraints = OptimizerConstraints(
      blockedDatesIso = setOf(blockedDate),
      pinnedDates = mapOf(pinnedDate to "CL"),
      horizonMode = OptimizerHorizon.FULL_HORIZON
    )

    val portfolio = PortfolioCalendarSolver.solveJointPortfolio(
      summary2026 = summary2026,
      colGhAuditAllYears = colAudit,
      records = records,
      focusGoal = "Maximize Long Weekends",
      summary2027 = summary2027,
      referenceDate = LocalDate.of(2026, 11, 26),
      constraints = constraints
    )

    val allActionSteps = portfolio.flatMap { it.dailyPlan }.filter {
      !it.isAlreadyLogged && it.creditType in setOf("CL", "HALF_CL", "RH", "COL", "EL")
    }
    assertFalse(
      "Blocked date $blockedDate must never be scheduled for leave",
      allActionSteps.any { it.dateIso == blockedDate }
    )
    val pinnedStep = allActionSteps.firstOrNull { it.dateIso == pinnedDate }
    assertTrue("Pinned date $pinnedDate must be included in portfolio", pinnedStep != null)
    assertEquals("CL", pinnedStep!!.creditType)
    assertTrue(pinnedStep.isPinned)
  }

  @Test
  fun virtualColPlannedWorkHolidayCreatesLinkedVirtualCreditAndPairsWithin180Days() {
    val records = baseRecords().filter { it.leaveType != "GH_WORKED" } // Zero real unused GH credits
    val summary2026 = LeaveOptimizerEngine.computeYearSummary(records, 2026L)
    val summary2027 = LeaveOptimizerEngine.computeYearSummary(records, 2027L)
    val colAudit = LeaveOptimizerEngine.auditColAndGhWorked(records, filterYear = null)
    assertEquals(0, colAudit.availableUnusedGhCount)

    val workHolidayIso = "2026-12-25" // Christmas marked as Work Duty -> +1 Virtual COL
    val constraints = OptimizerConstraints(
      strictZeroCl = true,
      plannedToWorkHolidays = setOf(workHolidayIso),
      horizonMode = OptimizerHorizon.FULL_HORIZON
    )

    val portfolio = PortfolioCalendarSolver.solveJointPortfolio(
      summary2026 = summary2026,
      colGhAuditAllYears = colAudit,
      records = records,
      focusGoal = "Zero-CL Combos",
      summary2027 = summary2027,
      referenceDate = LocalDate.of(2026, 11, 26),
      constraints = constraints
    )

    val colSteps = portfolio.flatMap { it.dailyPlan }.filter { it.creditType == "COL" && !it.isAlreadyLogged }
    assertTrue("Virtual +1 COL credit must generate a COL bridge even when real GH pool is empty", colSteps.isNotEmpty())
    val virtualStep = colSteps.first()
    assertEquals("virtual-credit-$workHolidayIso", virtualStep.linkedCreditId)
    assertTrue(virtualStep.isVirtualCredit)

    // Ensure 2026-12-25 itself is NOT booked as leave since officer plans to work that holiday
    assertFalse(
      portfolio.flatMap { it.dailyPlan }.any {
        it.dateIso == workHolidayIso && it.creditType in setOf("CL", "HALF_CL", "RH", "COL", "EL")
      }
    )

    // Verify paired date falls cleanly within [workHoliday .. +180 days]
    val earned = LocalDate.parse(workHolidayIso)
    val availed = LocalDate.parse(virtualStep.dateIso)
    assertFalse(availed.isBefore(earned))
    assertFalse(availed.isAfter(earned.plusDays(180)))
  }

  @Test
  fun tnRule10DayCeilingAndRule8ElIsolationEnforced() {
    val records = baseRecords()
    val summary2026 = LeaveOptimizerEngine.computeYearSummary(records, 2026L)
    val summary2027 = LeaveOptimizerEngine.computeYearSummary(records, 2027L)
    val colAudit = LeaveOptimizerEngine.auditColAndGhWorked(records, filterYear = null)

    val constraints = OptimizerConstraints(
      allowElBridges = true,
      allowHalfDayCl = true,
      protectedDaysOfWeek = setOf(DayOfWeek.TUESDAY),
      horizonMode = OptimizerHorizon.FULL_HORIZON
    )

    val portfolio = PortfolioCalendarSolver.solveJointPortfolio(
      summary2026 = summary2026,
      colGhAuditAllYears = colAudit,
      records = records,
      focusGoal = "Earned Leave (EL) + LTC 7-9d Break",
      summary2027 = summary2027,
      referenceDate = LocalDate.of(2026, 11, 26),
      constraints = constraints
    )

    portfolio.forEach { rec ->
      if (!rec.isElBridge) {
        assertTrue("Non-EL block ${rec.title} exceeded 10-day ceiling (${rec.totalDaysOff}d)", rec.totalDaysOff <= 10)
        assertFalse(rec.dailyPlan.any { it.creditType == "EL" })
      } else {
        assertFalse(
          "EL block must never mix CL/RH/COL in the same continuous spell",
          rec.dailyPlan.any { it.creditType in setOf("CL", "HALF_CL", "RH", "COL") }
        )
      }
    }
  }

  @Test
  fun midWeekModeProducesNonZeroValidAllocation() {
    val records = baseRecords()
    val summary2026 = LeaveOptimizerEngine.computeYearSummary(records, 2026L)
    val summary2027 = LeaveOptimizerEngine.computeYearSummary(records, 2027L)
    val colAudit = LeaveOptimizerEngine.auditColAndGhWorked(records, filterYear = null)

    val portfolio = PortfolioCalendarSolver.solveJointPortfolio(
      summary2026 = summary2026,
      colGhAuditAllYears = colAudit,
      records = records,
      focusGoal = "Spread Evenly",
      summary2027 = summary2027,
      referenceDate = LocalDate.of(2026, 11, 26),
      constraints = OptimizerConstraints(
        spreadMode = SpreadMode.SPREAD_EVENLY,
        proximityMode = ProximityMode.MID_WEEK,
        horizonMode = OptimizerHorizon.FULL_HORIZON
      )
    )

    assertTrue(portfolio.isNotEmpty())
    val impact = AiLeaveRecommendationEngine.computePortfolioImpact(
      selectedRecs = portfolio,
      summary2026 = summary2026,
      summary2027 = summary2027,
      colGhAuditAllYears = colAudit
    )
    assertFalse(impact.hasOverlappingDates)
    assertEquals(null, impact.exceedsQuotaMessage)
  }

  @Test
  fun geminiValidationGateBlocksDoubleSpendAndBlockedDates() {
    val records = baseRecords()
    val summary2026 = LeaveOptimizerEngine.computeYearSummary(records, 2026L)
    val summary2027 = LeaveOptimizerEngine.computeYearSummary(records, 2027L)
    val colAudit = LeaveOptimizerEngine.auditColAndGhWorked(records, filterYear = null)

    val blockedIso = "2026-12-29"
    val constraints = OptimizerConstraints(
      blockedDatesIso = setOf(blockedIso),
      strictZeroCl = true,
      horizonMode = OptimizerHorizon.FULL_HORIZON
    )

    val badGeminiCardWithBlocked = AiLeaveRecommendation(
      id = "bad_blocked",
      title = "Blocked Date Proposal",
      startDateIso = "2026-12-29",
      endDateIso = "2026-12-29",
      totalDaysOff = 1,
      leaveCreditsUsed = 1.0,
      freeDaysUnlocked = 0,
      efficiencyMultiplier = 1.0,
      creditMixSummary = "1 CL",
      whyOptimalForHod = "Invalid",
      tnRuleComplianceCheck = "Invalid",
      dailyPlan = listOf(
        AiBridgeDayPlan("2026-12-29", "Tue, Dec 29", "CL", "APPLY 1 CL")
      )
    )

    val rawGemini = AiRecommendationUiState.Success(
      focusGoal = "Zero-CL",
      executiveSummary = "Test",
      recommendations = listOf(badGeminiCardWithBlocked),
      analyzedHolidayCount = 40
    )

    val deterministicFallback = PortfolioCalendarSolver.solveJointPortfolio(
      summary2026 = summary2026,
      colGhAuditAllYears = colAudit,
      records = records,
      focusGoal = "Zero-CL",
      summary2027 = summary2027,
      referenceDate = LocalDate.of(2026, 11, 26),
      constraints = constraints
    )

    val repaired = AiLeaveRecommendationEngine.validateAndRepairGeminiRecommendations(
      raw = rawGemini,
      deterministicFallback = deterministicFallback,
      summary2026 = summary2026,
      summary2027 = summary2027,
      colGhAuditAllYears = colAudit,
      records = records,
      today = LocalDate.of(2026, 11, 26),
      constraints = constraints
    )

    val allActionDates = repaired.recommendations
      .flatMap { it.dailyPlan }
      .filter { !it.isAlreadyLogged && it.creditType in setOf("CL", "HALF_CL", "RH", "COL", "EL") }

    assertFalse(
      "Validation gate must reject blocked date $blockedIso from Gemini output",
      allActionDates.any { it.dateIso == blockedIso }
    )
    assertFalse(
      "Validation gate must reject CL when strictZeroCl is enabled",
      allActionDates.any { it.creditType == "CL" || it.creditType == "HALF_CL" }
    )
  }
}
