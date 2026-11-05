package com.example.domain

import com.example.data.InitialLeaveSeedData
import com.example.data.LeaveCategory
import com.example.data.LeaveRecord
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class DraftCalendarCellState(
  val dateIso: String,
  val dayOfMonth: Int,
  val isWeekend: Boolean,
  val ghName: String?,
  val rhName: String?,
  val existingLoggedCategory: LeaveCategory?,
  val draftedCategory: LeaveCategory?,
  val isAutoDebitedInterveningEl: Boolean = false,
  val isFreePrefixedOrSuffixedHoliday: Boolean = false
)

data class LeaveBuilderAnalysisReport(
  val totalDaysOffIncludingAttachedHolidays: Int,
  // Earned Leave (EL) Specific Impact
  val currentEstimatedElBalance: Int,
  val elDirectDaysDrafted: Int,
  val elTrappedInterveningHolidayDays: Int,
  val elTotalDebitedDays: Int,
  val elProjectedRemainingBalance: Int,
  val elFreePrefixSuffixDays: Int,
  // Casual / Restricted / COL Impact
  val clDraftedDays: Double,
  val clProjectedRemaining: Double,
  val rhDraftedDays: Int,
  val rhProjectedRemaining: Int,
  val colDraftedDays: Int,
  val colUnusedGhProjectedRemaining: Int,
  val mlDraftedDays: Int,
  // Auto-classified calendar annotations
  val trappedElHolidayDates: Set<String>,
  val freeAttachedHolidayDates: Set<String>,
  // Compliance & Optimization Feedback
  val isFullyCompliant: Boolean,
  val maxContiguousStretchDays: Int,
  val complianceViolations: List<String>,
  val smartOptimizationWarnings: List<String>,
  val statutorySummaryVerdict: String
)

object LeaveBuilderAnalyzer {

  private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE

  /**
   * Analyzes the user's drag-and-drop `draftBlocks` (dateIso -> LeaveCategory) in real time
   * against the Tamil Nadu Leave Rules, 1933 (Earned Leave Rule 8, Medical Leave Rule 15)
   * and Fundamental Rules Annexure VII (CL / RH / COL 10-day ceiling & non-combination rules).
   */
  fun analyzeDraft(
    draftBlocks: Map<String, LeaveCategory>,
    summary2026: YearCreditSummary,
    unusedGhCreditsCount: Int,
    allRecords: List<LeaveRecord>
  ): LeaveBuilderAnalysisReport {
    val serviceAccount = TamilNaduLeaveRulesService.computeServiceLeaveAccount(allRecords)
    // Permanent HOD accrues 30 EL/year up to 240 max cap; baseline credit pool after 2023-2026 usage
    val currentElPool = (180 - serviceAccount.elTotalTaken2023To2026)
      .coerceIn(15, TamilNaduLeaveRulesService.EL_MAX_ACCUMULATION_CAP_DAYS)

    if (draftBlocks.isEmpty()) {
      return LeaveBuilderAnalysisReport(
        totalDaysOffIncludingAttachedHolidays = 0,
        currentEstimatedElBalance = currentElPool,
        elDirectDaysDrafted = 0,
        elTrappedInterveningHolidayDays = 0,
        elTotalDebitedDays = 0,
        elProjectedRemainingBalance = currentElPool,
        elFreePrefixSuffixDays = 0,
        clDraftedDays = 0.0,
        clProjectedRemaining = summary2026.clRemaining,
        rhDraftedDays = 0,
        rhProjectedRemaining = summary2026.rhRemaining,
        colDraftedDays = 0,
        colUnusedGhProjectedRemaining = unusedGhCreditsCount,
        mlDraftedDays = 0,
        trappedElHolidayDates = emptySet(),
        freeAttachedHolidayDates = emptySet(),
        isFullyCompliant = true,
        maxContiguousStretchDays = 0,
        complianceViolations = emptyList(),
        smartOptimizationWarnings = listOf(
          "Drag any leave block (EL, CL, RH, COL, ML) onto the calendar grid below or tap a day cell to simulate your leave stretch."
        ),
        statutorySummaryVerdict = "Ready • Drag & Drop Leave Blocks onto Calendar"
      )
    }

    val ghMap = InitialLeaveSeedData.tnGovtPublicHolidays2026.associateBy { it.dateIso }
    val rhMap = InitialLeaveSeedData.tnRestrictedHolidays2026.associateBy { it.dateIso }

    fun isWeekendOrGh(date: LocalDate): Boolean {
      val iso = date.format(isoFormatter)
      return date.dayOfWeek == DayOfWeek.SATURDAY ||
        date.dayOfWeek == DayOfWeek.SUNDAY ||
        ghMap.containsKey(iso)
    }

    val sortedDraftEntries = draftBlocks.entries
      .mapNotNull { (iso, cat) ->
        runCatching { LocalDate.parse(iso, isoFormatter) to cat }.getOrNull()
      }
      .sortedBy { it.first }

    // 1. Detect trapped intervening holidays between EL blocks (TN Leave Rule 8)
    // Under TN Leave Rule 8, if an officer takes EL before and after a weekend/GH without returning to duty,
    // all intervening weekend/GH days are automatically counted as part of the EL spell and debited!
    val elDates = sortedDraftEntries.filter { it.second == LeaveCategory.EL }.map { it.first }
    val trappedElDates = mutableSetOf<String>()
    if (elDates.size >= 2) {
      for (i in 0 until elDates.size - 1) {
        val d1 = elDates[i]
        val d2 = elDates[i + 1]
        val gapDays = ChronoUnit.DAYS.between(d1, d2).toInt() - 1
        if (gapDays in 1..6) {
          var allInterveningAreHolidaysOrEl = true
          val candidateIsos = mutableListOf<String>()
          var probe = d1.plusDays(1)
          while (probe.isBefore(d2)) {
            val probeIso = probe.format(isoFormatter)
            val draftedOnProbe = draftBlocks[probeIso]
            if (draftedOnProbe == LeaveCategory.EL) {
              // already explicitly EL
            } else if (draftedOnProbe == null && isWeekendOrGh(probe)) {
              candidateIsos.add(probeIso)
            } else {
              allInterveningAreHolidaysOrEl = false
            }
            probe = probe.plusDays(1)
          }
          if (allInterveningAreHolidaysOrEl) {
            trappedElDates.addAll(candidateIsos)
          }
        }
      }
    }

    // 2. Build contiguous absence clusters (drafted dates + trapped EL dates + contiguous weekends/GHs)
    val activeLeaveDatesSet = sortedDraftEntries.map { it.first }.toMutableSet()
    trappedElDates.forEach { iso ->
      runCatching { LocalDate.parse(iso, isoFormatter) }.getOrNull()?.let { activeLeaveDatesSet.add(it) }
    }

    val minDate = activeLeaveDatesSet.minOrNull()!!
    val maxDate = activeLeaveDatesSet.maxOrNull()!!

    // Expand backwards and forwards for contiguous weekends/GHs attached to any drafted leave
    val allOffDates = activeLeaveDatesSet.toMutableSet()
    val freeAttachedDates = mutableSetOf<String>()

    // Also include intervening weekends/GHs between CL/RH/COL blocks in the same stretch
    var scan = minDate
    while (!scan.isAfter(maxDate)) {
      val scanIso = scan.format(isoFormatter)
      if (scan !in allOffDates && isWeekendOrGh(scan)) {
        // Check if bounded by leave dates within 4 days
        val hasPrior = (1..4).any { allOffDates.contains(scan.minusDays(it.toLong())) }
        val hasNext = (1..4).any { allOffDates.contains(scan.plusDays(it.toLong())) }
        if (hasPrior && hasNext) {
          allOffDates.add(scan)
          if (scanIso !in trappedElDates) {
            freeAttachedDates.add(scanIso)
          }
        }
      }
      scan = scan.plusDays(1)
    }

    // Prefix before minDate
    var probeBefore = minDate.minusDays(1)
    var prefixCount = 0
    while (isWeekendOrGh(probeBefore) && prefixCount < 5) {
      allOffDates.add(probeBefore)
      freeAttachedDates.add(probeBefore.format(isoFormatter))
      prefixCount++
      probeBefore = probeBefore.minusDays(1)
    }

    // Suffix after maxDate
    var probeAfter = maxDate.plusDays(1)
    var suffixCount = 0
    while (isWeekendOrGh(probeAfter) && suffixCount < 5) {
      allOffDates.add(probeAfter)
      freeAttachedDates.add(probeAfter.format(isoFormatter))
      suffixCount++
      probeAfter = probeAfter.plusDays(1)
    }

    // Segment `allOffDates` into contiguous calendar clusters
    val sortedAllOff = allOffDates.sorted()
    val clusters = mutableListOf<List<LocalDate>>()
    var currentCluster = mutableListOf<LocalDate>()
    for (d in sortedAllOff) {
      if (currentCluster.isEmpty() || ChronoUnit.DAYS.between(currentCluster.last(), d) == 1L) {
        currentCluster.add(d)
      } else {
        clusters.add(currentCluster)
        currentCluster = mutableListOf(d)
      }
    }
    if (currentCluster.isNotEmpty()) {
      clusters.add(currentCluster)
    }

    val violations = mutableListOf<String>()
    val warnings = mutableListOf<String>()
    var maxStretch = 0
    var elFreePrefixSuffixCount = 0

    clusters.forEach { cluster ->
      if (cluster.size > maxStretch) maxStretch = cluster.size
      val categoriesInCluster = cluster.mapNotNull { d ->
        draftBlocks[d.format(isoFormatter)]
      }.toSet()

      val hasRegularLeave = categoriesInCluster.any { it == LeaveCategory.EL || it == LeaveCategory.ML }
      val hasCasualFamily = categoriesInCluster.any {
        it == LeaveCategory.CL || it == LeaveCategory.RH || it == LeaveCategory.COL
      }

      // Check Rule 1: Cannot combine EL/ML with CL/RH/COL in the same continuous absence
      if (hasRegularLeave && hasCasualFamily) {
        violations.add(
          "TN FR Annexure VII Violation: Earned Leave / Medical Leave (EL/ML) CANNOT be combined in continuation with Casual Leave (CL), Restricted Holiday (RH), or COL (${cluster.first()} → ${cluster.last()})."
        )
      }

      // Check Rule 2: 10-Day Maximum Continuous Absence Ceiling for CL/RH/COL
      if (hasCasualFamily && !hasRegularLeave && cluster.size > TamilNaduLeaveRulesService.CL_MAX_STRETCH_INCLUDING_HOLIDAYS) {
        violations.add(
          "Exceeds TN 10-Day Continuous Absence Cap (FR Annexure VII): Stretch from ${cluster.first()} to ${cluster.last()} is ${cluster.size} days (Max allowed for CL/RH/COL including weekends & GH is 10 days)."
        )
      }

      if (categoriesInCluster.contains(LeaveCategory.EL)) {
        val attachedFreeInCluster = cluster.count { d ->
          d.format(isoFormatter) in freeAttachedDates
        }
        elFreePrefixSuffixCount += attachedFreeInCluster
      }
    }

    // Count credits per category
    val elDirectCount = draftBlocks.values.count { it == LeaveCategory.EL }
    val elTrappedCount = trappedElDates.size
    val elTotalDebited = elDirectCount + elTrappedCount
    val elRemainingAfterDraft = (currentElPool - elTotalDebited).coerceAtLeast(0)

    val clDrafted = draftBlocks.entries.count { (iso, cat) ->
      cat == LeaveCategory.CL && !isWeekendOrGh(LocalDate.parse(iso, isoFormatter))
    }.toDouble()
    val clRemainingAfter = summary2026.clRemaining - clDrafted

    val rhDrafted = draftBlocks.values.count { it == LeaveCategory.RH }
    val rhRemainingAfter = summary2026.rhRemaining - rhDrafted

    val colDrafted = draftBlocks.values.count { it == LeaveCategory.COL }
    val colRemainingAfter = unusedGhCreditsCount - colDrafted

    val mlDrafted = draftBlocks.values.count { it == LeaveCategory.ML }

    // Check Rule 3: RH date validity against 2026 TN RL list
    draftBlocks.forEach { (iso, cat) ->
      if (cat == LeaveCategory.RH && !rhMap.containsKey(iso)) {
        val dateObj = runCatching { LocalDate.parse(iso, isoFormatter) }.getOrNull()
        if (dateObj?.year == 2026) {
          violations.add(
            "RH Eligibility Violation: $iso is NOT on the official 2026 Tamil Nadu Restricted Holiday (RL) list."
          )
        }
      }
      if (cat == LeaveCategory.EL && isWeekendOrGh(LocalDate.parse(iso, isoFormatter))) {
        warnings.add(
          "Optimization Tip: You placed an EL block directly on a Weekend/GH ($iso). Prefix or suffix weekends outside your EL start/end dates so they are 100% FREE!"
        )
      }
    }

    // Check Rule 4: Trapped Intervening Holidays inside EL
    if (elTrappedCount > 0) {
      val trappedList = trappedElDates.sorted().joinToString(", ")
      warnings.add(
        "TN Leave Rule 8 Alert: $elTrappedCount intervening Weekend/GH day(s) ($trappedList) are bracketed inside your EL blocks and WILL BE DEBITED from your Earned Leave balance! Tip: End EL on Friday or use CL/COL/RH for short bridges ≤10 days."
      )
    } else if (elDirectCount > 0 && elFreePrefixSuffixCount > 0) {
      warnings.add(
        "Optimal EL Alignment: $elFreePrefixSuffixCount adjacent Weekend/GH day(s) are prefixed/suffixed for FREE without debiting your Earned Leave balance!"
      )
    }

    // Check Rule 5: Quota Balances
    if (clRemainingAfter < 0) {
      violations.add(
        "CL Quota Exceeded: Draft uses $clDrafted CL, exceeding your remaining ${summary2026.clRemaining} CL for 2026."
      )
    }
    if (rhRemainingAfter < 0) {
      violations.add(
        "RH Quota Exceeded: Draft uses $rhDrafted RH, exceeding your remaining ${summary2026.rhRemaining} RH for 2026."
      )
    }
    if (colRemainingAfter < 0) {
      violations.add(
        "Unpaired COL Warning: Draft uses $colDrafted COL, but you only have $unusedGhCreditsCount unused GH Worked credit(s) available."
      )
    }

    if (warnings.isEmpty() && violations.isEmpty()) {
      warnings.add(
        "100% TN Leave Rules 1933 & FR Annexure VII Compliant: ${allOffDates.size} total days off with optimal leave balance preservation."
      )
    }

    val verdict = when {
      violations.isNotEmpty() -> "Non-Compliant (${violations.size} Rule Issue${if (violations.size > 1) "s" else ""})"
      elTrappedCount > 0 -> "Compliant • Warning: $elTrappedCount Weekend/GH Debited as EL"
      else -> "100% TN Govt Rule Compliant • ${allOffDates.size}d Total Off"
    }

    return LeaveBuilderAnalysisReport(
      totalDaysOffIncludingAttachedHolidays = allOffDates.size,
      currentEstimatedElBalance = currentElPool,
      elDirectDaysDrafted = elDirectCount,
      elTrappedInterveningHolidayDays = elTrappedCount,
      elTotalDebitedDays = elTotalDebited,
      elProjectedRemainingBalance = elRemainingAfterDraft,
      elFreePrefixSuffixDays = elFreePrefixSuffixCount,
      clDraftedDays = clDrafted,
      clProjectedRemaining = clRemainingAfter,
      rhDraftedDays = rhDrafted,
      rhProjectedRemaining = rhRemainingAfter,
      colDraftedDays = colDrafted,
      colUnusedGhProjectedRemaining = colRemainingAfter,
      mlDraftedDays = mlDrafted,
      trappedElHolidayDates = trappedElDates,
      freeAttachedHolidayDates = freeAttachedDates,
      isFullyCompliant = violations.isEmpty(),
      maxContiguousStretchDays = maxStretch,
      complianceViolations = violations,
      smartOptimizationWarnings = warnings,
      statutorySummaryVerdict = verdict
    )
  }
}
