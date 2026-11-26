package com.example.domain

import com.example.data.InitialLeaveSeedData
import com.example.data.LeaveCategory
import com.example.data.LeaveRecord
import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiLeaveRecommendationEngineTest {

  private fun sampleRecords2026(): List<LeaveRecord> {
    return InitialLeaveSeedData.getAllHistoricalSeeds().mapIndexed { idx, s ->
      LeaveRecord(
        id = "seed_$idx",
        userId = "test_user",
        leaveType = s.leaveType,
        dateIso = s.dateIso,
        year = s.year,
        pairedDateIso = s.pairedDateIso,
        notes = s.notes,
        highlightStatus = s.highlightStatus,
        isHalfDay = s.isHalfDay
      )
    }
  }

  @Test
  fun `scanHolidayDatabaseForLongWeekends returns upcoming TN compliant bridges for all strategies`() {
    val records = sampleRecords2026()
    val summary2026 = LeaveOptimizerEngine.computeYearSummary(records, 2026L)
    val summary2027 = LeaveOptimizerEngine.computeYearSummary(records, 2027L)
    val auditAll = LeaveOptimizerEngine.auditColAndGhWorked(records, filterYear = null)
    val today = LocalDate.of(2026, 11, 25)

    val strategies = listOf(
      "Maximize Long Weekends (Highest ROI)",
      "Consume Expiring GH Worked (FIFO COL)",
      "Zero-CL Combos (RH + COL Only)",
      "Festival Megabridges (Pongal / Puja / Christmas)"
    )

    strategies.forEach { goal ->
      val recs = AiLeaveRecommendationEngine.scanHolidayDatabaseForLongWeekends(
        summary2026 = summary2026,
        colGhAuditAllYears = auditAll,
        records = records,
        focusGoal = goal,
        summary2027 = summary2027,
        referenceDate = today
      )
      assertTrue("Strategy '$goal' should yield recommendations", recs.isNotEmpty())
      recs.forEach { rec ->
        val start = LocalDate.parse(rec.startDateIso)
        assertFalse("Start date ${rec.startDateIso} must not be before $today", start.isBefore(today))
        assertTrue("TN 10-day cap violated for ${rec.title}: ${rec.totalDaysOff}", rec.totalDaysOff <= 10)
        assertTrue("Exact dates to apply must not be empty for ${rec.title}", rec.exactDatesToApply.isNotEmpty())
        assertTrue("Daily plan must cover whole stretch for ${rec.title}", rec.dailyPlan.size == rec.totalDaysOff)

        val bridge = rec.toSmartLeaveBridge()
        assertTrue("SmartLeaveBridge should retain steps", bridge.dayByDayBreakdown.size == rec.totalDaysOff)
      }
    }

    val zeroClRecs = AiLeaveRecommendationEngine.scanHolidayDatabaseForLongWeekends(
      summary2026 = summary2026,
      colGhAuditAllYears = auditAll,
      records = records,
      focusGoal = "Zero-CL Combos (RH + COL Only)",
      summary2027 = summary2027,
      referenceDate = today
    )
    assertTrue(
      "Zero-CL strategy should prioritize zero-CL bridges",
      zeroClRecs.first().dailyPlan.none { it.creditType == LeaveCategory.CL.code }
    )
  }
}
