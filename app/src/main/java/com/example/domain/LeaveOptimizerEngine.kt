package com.example.domain

import com.example.data.BridgeDayStep
import com.example.data.ColAuditStatus
import com.example.data.ColGhAuditItem
import com.example.data.ColGhAuditSummary
import com.example.data.InitialLeaveSeedData
import com.example.data.LeaveCategory
import com.example.data.LeaveRecord
import com.example.data.SmartLeaveBridge
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class YearCreditSummary(
  val year: Long,
  val clTaken: Double,
  val clQuota: Int = 12,
  val clRemaining: Double,
  val rhTaken: Int,
  val rhQuota: Int = 3,
  val rhRemaining: Int,
  val colTaken: Int,
  val inLieuWorked: Int,
  val colPendingCredits: Int,
  val plTaken: Int,
  val ghWorked: Int,
  val unpairedAlertCount: Int,
  val elTaken: Int,
  val mlTaken: Int,
  val onDutyTaken: Int,
  val weekOffTaken: Int
)

object LeaveOptimizerEngine {

  fun computeYearSummary(records: List<LeaveRecord>, year: Long): YearCreditSummary {
    val yearRecords = records.filter { it.year == year }
    val clTaken = yearRecords.filter { it.category == LeaveCategory.CL }.sumOf { it.dayValue }
    val rhTaken = yearRecords.count { it.category == LeaveCategory.RH }
    val colTaken = yearRecords.count { it.category == LeaveCategory.COL }
    val inLieuWorked = yearRecords.count { it.category == LeaveCategory.IN_LIEU }
    val plTaken = yearRecords.count { it.category == LeaveCategory.PL }
    val ghWorked = yearRecords.count { it.category == LeaveCategory.GH_WORKED }
    val elTaken = yearRecords.count { it.category == LeaveCategory.EL }
    val mlTaken = yearRecords.count { it.category == LeaveCategory.ML }
    val onDutyTaken = yearRecords.count { it.category == LeaveCategory.ON_DUTY }
    val weekOffTaken = yearRecords.count { it.category == LeaveCategory.WEEK_OFF }
    val unpairedAlerts = yearRecords.count {
      it.highlightStatus == "ALERT_RED" || it.highlightStatus == "PENDING_ORANGE"
    }

    return YearCreditSummary(
      year = year,
      clTaken = clTaken,
      clQuota = 12,
      clRemaining = (12.0 - clTaken).coerceAtLeast(0.0),
      rhTaken = rhTaken,
      rhQuota = 3,
      rhRemaining = (3 - rhTaken).coerceAtLeast(0),
      colTaken = colTaken,
      inLieuWorked = inLieuWorked,
      colPendingCredits = (ghWorked - colTaken).coerceAtLeast(0),
      plTaken = plTaken,
      ghWorked = ghWorked,
      unpairedAlertCount = unpairedAlerts,
      elTaken = elTaken,
      mlTaken = mlTaken,
      onDutyTaken = onDutyTaken,
      weekOffTaken = weekOffTaken
    )
  }

  /**
   * Performs a comprehensive 3-way reconciliation across:
   * 1) COL (Compensatory Off Leave taken)
   * 2) In Lieu Of (Duty date cited against each COL)
   * 3) GH Worked (Actual Government Holiday hospital duty logged)
   *
   * Ensures every COL is accounted for against a unique GH Worked date, detects double-claimed
   * GH Worked dates (like 2022-10-08), identifies unaccounted COLs (like 2026-09-07 in 2025),
   * and surfaces all unused GH Worked credits available for pairing.
   */
  fun auditColAndGhWorked(
    allRecords: List<LeaveRecord>,
    filterYear: Long? = null
  ): ColGhAuditSummary {
    val allCols = allRecords
      .filter { it.category == LeaveCategory.COL }
      .sortedBy { it.dateIso }
    val allInLieu = allRecords
      .filter { it.category == LeaveCategory.IN_LIEU }
      .sortedBy { it.dateIso }
    val allGhWorked = allRecords
      .filter { it.category == LeaveCategory.GH_WORKED }
      .sortedBy { it.dateIso }

    // Track how many COLs claim the same duty date
    val dutyClaimCounts = mutableMapOf<String, Int>()
    allCols.forEach { col ->
      val dutyDate = col.pairedDateIso.trim()
      if (dutyDate.isNotEmpty()) {
        dutyClaimCounts[dutyDate] = (dutyClaimCounts[dutyDate] ?: 0) + 1
      }
    }

    // Track which GH_WORKED record IDs have been consumed by a valid 1-to-1 COL pairing
    val consumedGhRecordIds = mutableSetOf<String>()
    val ghByDate = allGhWorked.groupBy { it.dateIso }

    // First pass: identify unused GH_WORKED records across all years so we can suggest optimal fixes
    val claimedDatesSet = dutyClaimCounts.keys
    val initiallyUnclaimedGh = allGhWorked.filter { it.dateIso !in claimedDatesSet }.toMutableList()

    val auditItems = mutableListOf<ColGhAuditItem>()
    val unpairedCols = mutableListOf<LeaveRecord>()
    var verifiedCount = 0
    var unaccountedCount = 0
    var doubleClaimedCount = 0

    // Track how many times we've seen a duty date as we iterate through COLs
    val seenDutyDateCount = mutableMapOf<String, Int>()

    allCols.forEach { col ->
      val dutyDate = col.pairedDateIso.trim()
      val seenIdx = (seenDutyDateCount[dutyDate] ?: 0) + 1
      if (dutyDate.isNotEmpty()) {
        seenDutyDateCount[dutyDate] = seenIdx
      }

      val matchingGhCandidates = ghByDate[dutyDate].orEmpty()
      // Find an unconsumed GH record for this date if available
      val availableGh = matchingGhCandidates.firstOrNull { it.id !in consumedGhRecordIds }

      when {
        dutyDate.isEmpty() -> {
          unaccountedCount++
          unpairedCols.add(col)
          val suggestedGh = initiallyUnclaimedGh.firstOrNull { it.year == col.year }
            ?: initiallyUnclaimedGh.firstOrNull()
          auditItems.add(
            ColGhAuditItem(
              id = "audit_col_${col.id}",
              year = col.year,
              colRecordId = col.id,
              colDateIso = col.dateIso,
              inLieuDateIso = "MISSING",
              ghWorkedRecordId = "",
              ghWorkedDateIso = "UNPAIRED",
              status = ColAuditStatus.UNPAIRED_COL,
              auditExplanation = "Unaccounted COL on ${col.dateIso}: No 'In Lieu of' or 'GH Worked' date is linked.",
              suggestedFixGhDateIso = suggestedGh?.dateIso ?: "",
              suggestedFixGhRecordId = suggestedGh?.id ?: ""
            )
          )
        }

        availableGh != null && seenIdx == 1 -> {
          consumedGhRecordIds.add(availableGh.id)
          verifiedCount++
          auditItems.add(
            ColGhAuditItem(
              id = "audit_col_${col.id}",
              year = col.year,
              colRecordId = col.id,
              colDateIso = col.dateIso,
              inLieuDateIso = dutyDate,
              ghWorkedRecordId = availableGh.id,
              ghWorkedDateIso = availableGh.dateIso,
              status = ColAuditStatus.VERIFIED_MATCH,
              auditExplanation = "100% Accounted: COL (${col.dateIso}) ↔ In Lieu ($dutyDate) ↔ GH Worked (${availableGh.dateIso}).",
              ghDutyNotes = availableGh.notes
            )
          )
        }

        (dutyClaimCounts[dutyDate] ?: 0) > 1 && availableGh == null -> {
          doubleClaimedCount++
          unaccountedCount++
          unpairedCols.add(col)
          val suggestedGh = initiallyUnclaimedGh.firstOrNull { it.year == col.year }
            ?: initiallyUnclaimedGh.firstOrNull()
          auditItems.add(
            ColGhAuditItem(
              id = "audit_col_${col.id}",
              year = col.year,
              colRecordId = col.id,
              colDateIso = col.dateIso,
              inLieuDateIso = dutyDate,
              ghWorkedRecordId = "",
              ghWorkedDateIso = "$dutyDate (Duplicate #${seenIdx})",
              status = ColAuditStatus.DOUBLE_CLAIMED_ALERT,
              auditExplanation = "Audit Alert: GH Worked on $dutyDate was claimed by ${dutyClaimCounts[dutyDate]} COL entries! Re-pair this COL to an unused GH Worked credit.",
              suggestedFixGhDateIso = suggestedGh?.dateIso ?: "",
              suggestedFixGhRecordId = suggestedGh?.id ?: ""
            )
          )
        }

        else -> {
          // Duty date is in 'In Lieu of' column, but missing from 'GH Worked' column
          // (e.g. 2022-10-05 in 2023, 2024-11-09 or 2025-03-13 in 2025, 2026-04-09 in 2026)
          unaccountedCount++
          unpairedCols.add(col)
          val suggestedGh = initiallyUnclaimedGh.firstOrNull { it.year == col.year }
            ?: initiallyUnclaimedGh.firstOrNull()
          auditItems.add(
            ColGhAuditItem(
              id = "audit_col_${col.id}",
              year = col.year,
              colRecordId = col.id,
              colDateIso = col.dateIso,
              inLieuDateIso = dutyDate,
              ghWorkedRecordId = "",
              ghWorkedDateIso = "Not in GH Worked Col",
              status = ColAuditStatus.MISSING_IN_GH_COLUMN,
              auditExplanation = "COL (${col.dateIso}) cites In-Lieu date $dutyDate, which is not listed in the GH Worked column (or can be paired with an unused GH Worked credit).",
              suggestedFixGhDateIso = suggestedGh?.dateIso ?: "",
              suggestedFixGhRecordId = suggestedGh?.id ?: ""
            )
          )
        }
      }
    }

    // Now list all unused GH_WORKED credits that have NOT been consumed by any COL
    val unusedGhRecords = allGhWorked.filter { it.id !in consumedGhRecordIds }
    unusedGhRecords.forEach { gh ->
      val cleanDuty = gh.notes.ifBlank { "Hospital GH Duty" }
      auditItems.add(
        ColGhAuditItem(
          id = "audit_gh_${gh.id}",
          year = gh.year,
          colRecordId = "",
          colDateIso = "AVAILABLE CREDIT",
          inLieuDateIso = "—",
          ghWorkedRecordId = gh.id,
          ghWorkedDateIso = gh.dateIso,
          status = ColAuditStatus.AVAILABLE_GH_CREDIT,
          auditExplanation = "$cleanDuty — Eligible for 1-day COL pairing or new COL leave within 6 months.",
          ghDutyNotes = cleanDuty
        )
      )
    }

    val filteredItems = if (filterYear == null) {
      auditItems
    } else {
      auditItems.filter { it.year == filterYear }
    }

    val scopedCols = if (filterYear == null) allCols else allCols.filter { it.year == filterYear }
    val scopedGh = if (filterYear == null) allGhWorked else allGhWorked.filter { it.year == filterYear }
    val scopedInLieu = if (filterYear == null) allInLieu else allInLieu.filter { it.year == filterYear }

    return ColGhAuditSummary(
      totalColTaken = scopedCols.size,
      totalGhWorked = scopedGh.size,
      totalInLieuEntries = scopedInLieu.size,
      fullyAccountedColCount = filteredItems.count { it.status == ColAuditStatus.VERIFIED_MATCH },
      unaccountedColCount = filteredItems.count {
        it.status == ColAuditStatus.UNPAIRED_COL ||
          it.status == ColAuditStatus.DOUBLE_CLAIMED_ALERT ||
          it.status == ColAuditStatus.MISSING_IN_GH_COLUMN
      },
      availableUnusedGhCount = filteredItems.count { it.status == ColAuditStatus.AVAILABLE_GH_CREDIT },
      doubleClaimedCount = filteredItems.count { it.status == ColAuditStatus.DOUBLE_CLAIMED_ALERT },
      items = filteredItems,
      unusedGhRecords = if (filterYear == null) unusedGhRecords else unusedGhRecords.filter { it.year == filterYear },
      unpairedColRecords = if (filterYear == null) unpairedCols else unpairedCols.filter { it.year == filterYear }
    )
  }

  /**
   * Dynamically generates optimal Tamil Nadu Government Leave Bridges for 2026–2027 & upcoming festivals
   * combining Weekends (Sat/Sun), Government Public Holidays (GH), Restricted Holidays (RH),
   * and optimal CL / COL / EL credits while strictly adhering to TN Leave Rules 1933 & FR Annexure VII.
   */
  fun generateOptimalLeaveBridges(
    summary2026: YearCreditSummary,
    records2026: List<LeaveRecord>,
    allRecords: List<LeaveRecord> = records2026,
    colGhAuditAllYears: ColGhAuditSummary? = null,
    summary2027: YearCreditSummary? = null
  ): List<SmartLeaveBridge> {
    val audit = colGhAuditAllYears ?: auditColAndGhWorked(allRecords, filterYear = null)
    val dynamic2026Plus = AiLeaveRecommendationEngine.scanHolidayDatabaseForLongWeekends(
      summary2026 = summary2026,
      colGhAuditAllYears = audit,
      records = allRecords,
      focusGoal = "Maximize Long Weekends (Highest ROI)",
      summary2027 = summary2027,
      constraints = OptimizerConstraints(
        allowHalfDayCl = true,
        prioritizeExpiringCol = true
      )
    )
    if (dynamic2026Plus.isNotEmpty()) {
      return dynamic2026Plus.map { it.toSmartLeaveBridge() }
    }
    return TnHolidays2027Catalog.smartBridges2027
  }

  /**
   * Custom Date Range Optimizer for the interactive planner in the Optimizer tab.
   */
  fun analyzeCustomRange(
    startIso: String,
    endIso: String,
    summary2026: YearCreditSummary
  ): CustomRangeAnalysis {
    return try {
      val fmt = DateTimeFormatter.ISO_LOCAL_DATE
      val start = LocalDate.parse(startIso, fmt)
      val end = LocalDate.parse(endIso, fmt)
      if (end.isBefore(start)) {
        return CustomRangeAnalysis(
          isValid = false,
          errorMessage = "End date must be on or after start date."
        )
      }
      val totalDays = ChronoUnit.DAYS.between(start, end).toInt() + 1
      if (totalDays > 30) {
        return CustomRangeAnalysis(
          isValid = false,
          errorMessage = "Please select a range up to 30 days for tactical optimization."
        )
      }

      val ghSet = TnHolidays2027Catalog.allSyncedPublicHolidays().associateBy { it.dateIso }
      val rhSet = TnHolidays2027Catalog.allSyncedRestrictedHolidays().associateBy { it.dateIso }

      val steps = mutableListOf<BridgeDayStep>()
      var weekendCount = 0
      var ghCount = 0
      var rhOpportunities = 0
      var workingDaysNeedingCredit = 0

      var curr = start
      while (!curr.isAfter(end)) {
        val iso = curr.format(fmt)
        val dow = curr.dayOfWeek
        val gh = ghSet[iso]
        val rh = rhSet[iso]
        when {
          gh != null -> {
            ghCount++
            steps.add(BridgeDayStep(iso, "${dow.name.take(3)}, $iso", "GH", "GH: ${gh.nameEnglish} (${gh.nameTamil})"))
          }
          dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY -> {
            weekendCount++
            val extra = if (rh != null) " • Also RL: ${rh.nameTamil}" else ""
            steps.add(BridgeDayStep(iso, "${dow.name.take(3)}, $iso", "WEEKEND", "Weekend Off$extra"))
          }
          rh != null -> {
            rhOpportunities++
            steps.add(BridgeDayStep(iso, "${dow.name.take(3)}, $iso", "RH", "Eligible RH: ${rh.nameEnglish} (${rh.nameTamil})"))
          }
          else -> {
            workingDaysNeedingCredit++
            steps.add(BridgeDayStep(iso, "${dow.name.take(3)}, $iso", "LEAVE_CREDIT", "Working Day — Use CL / COL / PL Credit"))
          }
        }
        curr = curr.plusDays(1)
      }

      val usableRh = minOf(rhOpportunities, summary2026.rhRemaining)
      val remainingAfterRh = workingDaysNeedingCredit + (rhOpportunities - usableRh)
      val exceedsClStretch = totalDays > TamilNaduLeaveRulesService.CL_MAX_STRETCH_INCLUDING_HOLIDAYS

      // Evaluate both CL/RH/COL mode and Earned Leave (EL) mode via TamilNaduLeaveRulesService
      val clRuleCheck = TamilNaduLeaveRulesService.validateAndOptimizeLeaveSpell(
        category = LeaveCategory.CL,
        startDateIso = startIso,
        endDateIso = endIso,
        clRemainingInYear = summary2026.clRemaining,
        rhRemainingInYear = summary2026.rhRemaining
      )
      val elRuleCheck = TamilNaduLeaveRulesService.validateAndOptimizeLeaveSpell(
        category = LeaveCategory.EL,
        startDateIso = startIso,
        endDateIso = endIso,
        clRemainingInYear = summary2026.clRemaining,
        rhRemainingInYear = summary2026.rhRemaining
      )

      val recommendation = buildString {
        if (exceedsClStretch) {
          append("⚠️ TN Fundamental Rules Annexure VII Alert: Total continuous absence is ${clRuleCheck.totalCalendarDays} days (>10 days max for CL/RH/COL). ")
          append("Switch to Earned Leave (EL — Rule 8, max 180 days) OR split into two spells separated by hospital duty. ")
        } else {
          append("✅ TN Annexure VII Compliant (${clRuleCheck.totalCalendarDays} total days incl. prefix/suffix ≤ 10-day limit). ")
        }
        append("Free days inside range: ${weekendCount + ghCount} (${weekendCount} Weekend + $ghCount GH). ")
        if (usableRh > 0) {
          append("Optimal Mix: $usableRh RH + ")
        }
        append("$remainingAfterRh CL/COL credits ( vs ${elRuleCheck.netLeaveDebitedDays.toInt()} EL days if taken as Earned Leave, since EL counts intervening holidays!).")
      }

      CustomRangeAnalysis(
        isValid = true,
        totalDays = totalDays,
        freeDays = weekendCount + ghCount,
        rhSuggested = usableRh,
        creditsNeeded = usableRh + remainingAfterRh,
        exceedsTenDayRule = exceedsClStretch,
        recommendationSummary = recommendation,
        elComparisonAdvice = elRuleCheck.optimizationAdvice,
        steps = steps
      )
    } catch (e: Exception) {
      CustomRangeAnalysis(
        isValid = false,
        errorMessage = "Use YYYY-MM-DD format (e.g., 2026-12-24)."
      )
    }
  }
}

data class CustomRangeAnalysis(
  val isValid: Boolean,
  val errorMessage: String = "",
  val totalDays: Int = 0,
  val freeDays: Int = 0,
  val rhSuggested: Int = 0,
  val creditsNeeded: Int = 0,
  val exceedsTenDayRule: Boolean = false,
  val recommendationSummary: String = "",
  val elComparisonAdvice: String = "",
  val steps: List<BridgeDayStep> = emptyList()
)
