package com.example.domain

import com.example.data.ColGhAuditSummary
import com.example.data.LeaveCategory
import com.example.data.LeaveRecord
import com.example.data.TnHolidayItem
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Pure Kotlin whole-year calendar matrix & joint non-overlapping portfolio solver.
 * Combines whole-year calendar ROI + monthly spread + FIFO expiry-bound COL pairing +
 * virtual worked-holiday COLs + pin/block steering with strict Tamil Nadu Leave Rules 1933.
 */
object PortfolioCalendarSolver {

  private val isoFmt = DateTimeFormatter.ISO_LOCAL_DATE

  private data class ActiveColCredit(
    val id: String,
    val earnedDate: LocalDate,
    val earnedDateIso: String,
    val expiryDate: LocalDate,
    val expiryDateIso: String,
    val isVirtual: Boolean,
    var consumed: Boolean = false
  )

  private data class CalendarCell(
    val date: LocalDate,
    val dateIso: String,
    val ghItem: TnHolidayItem?,
    val rhItem: TnHolidayItem?,
    val isWeekend: Boolean,
    val isWorkedHoliday: Boolean,
    val isWeekendOrGh: Boolean,
    val isRestrictedHoliday: Boolean,
    val hasExistingLeave: Boolean,
    val isExistingCasualFamily: Boolean,
    val isExistingRegular: Boolean,
    val isExistingDuty: Boolean,
    val isBlocked: Boolean,
    val isOpdProtected: Boolean,
    val isInitialDayOff: Boolean,
    var plannedType: String? = null, // "CL", "HALF_CL", "RH", "COL", "EL"
    var linkedCreditId: String? = null,
    var isVirtualCredit: Boolean = false,
    var isPinned: Boolean = false
  )

  fun solveJointPortfolio(
    summary2026: YearCreditSummary,
    colGhAuditAllYears: ColGhAuditSummary,
    records: List<LeaveRecord>,
    focusGoal: String = "",
    summary2027: YearCreditSummary? = null,
    referenceDate: LocalDate = LocalDate.now(),
    constraints: OptimizerConstraints = OptimizerConstraints()
  ): List<AiLeaveRecommendation> {
    val today = if (referenceDate.year < 2026) LocalDate.of(2026, 11, 26) else referenceDate
    val effective = AiLeaveRecommendationEngine.parseNaturalLanguageConstraints(
      base = constraints,
      rawQuery = "$focusGoal ${constraints.customNaturalPrompt}"
    )
    val weekendPolicy = effective.effectiveWeekendPolicy()

    val horizonStart = when (effective.horizonMode) {
      OptimizerHorizon.Q1_Q2_2027 -> maxOf(today, LocalDate.of(2027, 1, 1))
      else -> today
    }
    val horizonEnd = when (effective.horizonMode) {
      OptimizerHorizon.NEXT_60_DAYS -> today.plusDays(60)
      OptimizerHorizon.REST_OF_2026 -> LocalDate.of(2026, 12, 31)
      OptimizerHorizon.Q1_Q2_2027 -> LocalDate.of(2027, 6, 30)
      OptimizerHorizon.FULL_HORIZON -> LocalDate.of(2027, 12, 31)
    }
    if (horizonEnd.isBefore(horizonStart)) return emptyList()

    val allGh = TnHolidays2027Catalog.allSyncedPublicHolidays()
    val allRh = TnHolidays2027Catalog.allSyncedRestrictedHolidays()
    val ghMap = allGh.associateBy { it.dateIso }
    val rhMap = allRh.associateBy { it.dateIso }
    val recordsByDate = records.groupBy { it.dateIso }

    val casualSet = setOf(LeaveCategory.CL, LeaveCategory.RH, LeaveCategory.COL, LeaveCategory.PL)
    val regularSet = setOf(LeaveCategory.EL, LeaveCategory.ML)
    val allLeaveSet = casualSet + regularSet

    val goalLower = "$focusGoal ${effective.customNaturalPrompt}".lowercase(Locale.US)
    val preferZeroCl = effective.strictZeroCl || goalLower.contains("zero-cl") || goalLower.contains("rh + col")
    val preferFifoCol = effective.prioritizeExpiringCol || goalLower.contains("fifo") || goalLower.contains("expiring gh")
    val preferMegabridges = goalLower.contains("megabridge") || goalLower.contains("festival")

    // 1. Gather real unused GH_WORKED credits + virtual credits from plannedToWorkHolidays
    val activeCredits = mutableListOf<ActiveColCredit>()
    colGhAuditAllYears.unusedGhRecords.forEach { gh ->
      val earnedDate = runCatching { LocalDate.parse(gh.dateIso, isoFmt) }.getOrNull() ?: return@forEach
      val expiryDate = earnedDate.plusDays(TamilNaduLeaveRulesService.COL_EXPIRY_WINDOW_DAYS.toLong())
      if (!expiryDate.isBefore(horizonStart)) {
        activeCredits.add(
          ActiveColCredit(
            id = gh.id.ifBlank { "gh-${gh.dateIso}" },
            earnedDate = earnedDate,
            earnedDateIso = gh.dateIso,
            expiryDate = expiryDate,
            expiryDateIso = expiryDate.format(isoFmt),
            isVirtual = false
          )
        )
      }
    }
    effective.plannedToWorkHolidays.forEach { workIso ->
      val earnedDate = runCatching { LocalDate.parse(workIso, isoFmt) }.getOrNull() ?: return@forEach
      val expiryDate = earnedDate.plusDays(TamilNaduLeaveRulesService.COL_EXPIRY_WINDOW_DAYS.toLong())
      if (!expiryDate.isBefore(horizonStart)) {
        activeCredits.add(
          ActiveColCredit(
            id = "virtual-credit-$workIso",
            earnedDate = earnedDate,
            earnedDateIso = workIso,
            expiryDate = expiryDate,
            expiryDateIso = expiryDate.format(isoFmt),
            isVirtual = true
          )
        )
      }
    }
    activeCredits.sortWith(compareBy<ActiveColCredit> { it.expiryDate }.thenBy { it.earnedDate }.thenBy { it.id })

    // 2. Build Calendar Matrix (with 14-day boundary padding for TN Rule 10-day & Rule-8 checks)
    val calendar = linkedMapOf<String, CalendarCell>()
    val scanStart = horizonStart.minusDays(14)
    val scanEnd = horizonEnd.plusDays(14)
    var dCursor = scanStart
    while (!dCursor.isAfter(scanEnd)) {
      val iso = dCursor.format(isoFmt)
      val recs = recordsByDate[iso].orEmpty()
      val ghItem = ghMap[iso]
      val rhItem = rhMap[iso]
      val isWknd = weekendPolicy.isWeekend(dCursor)
      val isWorkedHol = iso in effective.plannedToWorkHolidays
      val isWkndOrGh = (isWknd || ghItem != null) && !isWorkedHol
      val hasExistingLeave = recs.any { it.category in allLeaveSet }
      val isExistingCasual = recs.any { it.category in casualSet }
      val isExistingRegular = recs.any { it.category in regularSet }
      val isExistingDuty = recs.any { it.category == LeaveCategory.GH_WORKED || it.category == LeaveCategory.ON_DUTY }

      calendar[iso] = CalendarCell(
        date = dCursor,
        dateIso = iso,
        ghItem = ghItem,
        rhItem = rhItem,
        isWeekend = isWknd,
        isWorkedHoliday = isWorkedHol,
        isWeekendOrGh = isWkndOrGh,
        isRestrictedHoliday = rhItem != null && !isWorkedHol,
        hasExistingLeave = hasExistingLeave,
        isExistingCasualFamily = isExistingCasual,
        isExistingRegular = isExistingRegular,
        isExistingDuty = isExistingDuty,
        isBlocked = iso in effective.blockedDatesIso,
        isOpdProtected = dCursor.dayOfWeek in effective.protectedDaysOfWeek || iso in effective.protectedDatesIso,
        isInitialDayOff = isWkndOrGh || hasExistingLeave
      )
      dCursor = dCursor.plusDays(1)
    }

    val horizonDates = mutableListOf<CalendarCell>()
    var hCursor = horizonStart
    while (!hCursor.isAfter(horizonEnd)) {
      calendar[hCursor.format(isoFmt)]?.let { horizonDates.add(it) }
      hCursor = hCursor.plusDays(1)
    }

    // 3. Shared Remaining Quota Pool (prevents double-spending across all blocks!)
    var clRem2026 = if (preferZeroCl) 0.0 else (summary2026.clRemaining - effective.reserveClFloor).coerceAtLeast(0.0)
    var clRem2027 = if (preferZeroCl) 0.0 else ((summary2027?.clRemaining ?: 12.0) - effective.reserveClFloor).coerceAtLeast(0.0)
    var rhRem2026 = summary2026.rhRemaining.coerceAtLeast(0)
    var rhRem2027 = (summary2027?.rhRemaining ?: 3).coerceAtLeast(0)
    var elRem = if (effective.allowElBridges) (30 - summary2026.elTaken).coerceIn(5, 15) else 0

    // Raw remaining CL (used only when honoring explicit user pins on tight reserves)
    var rawClRem2026 = summary2026.clRemaining.coerceAtLeast(0.0)
    var rawClRem2027 = (summary2027?.clRemaining ?: 12.0).coerceAtLeast(0.0)

    fun isCellOffOrPlanned(cell: CalendarCell, extraDates: Set<String> = emptySet()): Boolean =
      cell.isInitialDayOff || cell.plannedType != null || cell.dateIso in extraDates

    fun getPlannedInMonth(ym: YearMonth): Int =
      horizonDates.count { it.plannedType != null && YearMonth.from(it.date) == ym }

    fun canBookDatesSafely(dates: List<LocalDate>, creditType: String): Boolean {
      val upper = creditType.uppercase(Locale.US)
      val isEl = upper == "EL"
      val proposedSet = dates.map { it.format(isoFmt) }.toSet()

      for (d in dates) {
        if (d.isBefore(horizonStart) || d.isAfter(horizonEnd)) return false
        val cell = calendar[d.format(isoFmt)] ?: return false
        if (cell.isInitialDayOff || cell.plannedType != null || cell.isBlocked || cell.isWorkedHoliday || cell.isExistingDuty) {
          return false
        }
        if (isEl && cell.isExistingCasualFamily) return false
        if (!isEl && cell.isExistingRegular) return false
      }

      // Check TN Leave Rules 1933 adjacency & 10-day ceiling
      val minDate = dates.min()
      val maxDate = dates.max()
      var stretchStart = minDate
      var guard = 0
      while (guard < 14) {
        val prev = stretchStart.minusDays(1)
        val prevCell = calendar[prev.format(isoFmt)] ?: break
        val prevIsEl = prevCell.plannedType == "EL" || prevCell.isExistingRegular
        val prevIsCasual = prevCell.plannedType in setOf("CL", "HALF_CL", "RH", "COL") || prevCell.isExistingCasualFamily
        if (isEl && prevIsCasual) return false
        if (!isEl && prevIsEl) return false
        if (isCellOffOrPlanned(prevCell, proposedSet)) {
          stretchStart = prev
          guard++
        } else {
          break
        }
      }

      var stretchEnd = maxDate
      guard = 0
      while (guard < 14) {
        val next = stretchEnd.plusDays(1)
        val nextCell = calendar[next.format(isoFmt)] ?: break
        val nextIsEl = nextCell.plannedType == "EL" || nextCell.isExistingRegular
        val nextIsCasual = nextCell.plannedType in setOf("CL", "HALF_CL", "RH", "COL") || nextCell.isExistingCasualFamily
        if (isEl && nextIsCasual) return false
        if (!isEl && nextIsEl) return false
        if (isCellOffOrPlanned(nextCell, proposedSet)) {
          stretchEnd = next
          guard++
        } else {
          break
        }
      }

      if (!isEl && effective.enforce10DayCap) {
        val continuousLen = ChronoUnit.DAYS.between(stretchStart, stretchEnd).toInt() + 1
        if (continuousLen > TamilNaduLeaveRulesService.CL_MAX_STRETCH_INCLUDING_HOLIDAYS) {
          return false
        }
      }
      return true
    }

    fun getPotentialRoi(dates: List<LocalDate>, tempSelected: Set<String> = emptySet()): Double {
      if (dates.isEmpty()) return -999.0
      val sorted = dates.sorted()
      val proposedSet = sorted.map { it.format(isoFmt) }.toSet() + tempSelected

      var countBefore = 0
      var p = sorted.first().minusDays(1)
      while (true) {
        val cell = calendar[p.format(isoFmt)] ?: break
        if (!isCellOffOrPlanned(cell, proposedSet)) break
        countBefore++
        p = p.minusDays(1)
      }

      var countAfter = 0
      var n = sorted.last().plusDays(1)
      while (true) {
        val cell = calendar[n.format(isoFmt)] ?: break
        if (!isCellOffOrPlanned(cell, proposedSet)) break
        countAfter++
        n = n.plusDays(1)
      }

      val opdPenalty = sorted.sumOf { d ->
        val cell = calendar[d.format(isoFmt)]
        if (cell?.isOpdProtected == true) -14.0 else 0.0
      }

      if (effective.proximityMode == ProximityMode.MID_WEEK) {
        val midWeekBase = if (countBefore == 0 && countAfter == 0) 5.0 else 0.1
        return midWeekBase + opdPenalty
      }

      val isSandwich = countBefore >= 1 && countAfter >= 1
      val isAdjacent = countBefore >= 1 || countAfter >= 1

      var score = when {
        isSandwich -> when (sorted.size) {
          1 -> 25.0 + countBefore + countAfter
          2 -> 5.0 + countBefore + countAfter
          else -> (countBefore + countAfter).toDouble()
        }
        isAdjacent -> {
          val dow = sorted.first().dayOfWeek
          val isMonOrFri = dow == DayOfWeek.MONDAY || dow == DayOfWeek.FRIDAY
          if (sorted.size == 1 && isMonOrFri) {
            15.0 + maxOf(countBefore, countAfter)
          } else if (sorted.size == 2 && (sorted.first().dayOfWeek == DayOfWeek.MONDAY || sorted.last().dayOfWeek == DayOfWeek.FRIDAY)) {
            4.0 + maxOf(countBefore, countAfter)
          } else {
            0.1 * maxOf(countBefore, countAfter)
          }
        }
        else -> {
          val dow = sorted.first().dayOfWeek
          if (dow == DayOfWeek.TUESDAY || dow == DayOfWeek.WEDNESDAY || dow == DayOfWeek.THURSDAY) -10.0 else 0.0
        }
      }

      val hasRh = sorted.any { calendar[it.format(isoFmt)]?.isRestrictedHoliday == true }
      if (hasRh && isAdjacent) score += 10.0

      if (preferMegabridges && (countBefore + countAfter + sorted.size) >= 5) {
        score += 4.0
      }
      if (sorted.first().year == 2026) {
        score += 1.2
      }
      return score + opdPenalty
    }

    fun pickEligibleColCredit(targetDate: LocalDate, preferVirtualFirst: Boolean = false): ActiveColCredit? {
      val candidates = activeCredits.filter { credit ->
        !credit.consumed &&
          !targetDate.isBefore(credit.earnedDate) &&
          !targetDate.isAfter(credit.expiryDate)
      }
      if (candidates.isEmpty()) return null
      return if (preferVirtualFirst) {
        candidates.sortedWith(
          compareByDescending<ActiveColCredit> { it.isVirtual }
            .thenBy { it.expiryDate }
        ).firstOrNull()
      } else {
        candidates.minByOrNull { it.expiryDate }
      }
    }

    fun tryDebitYearCl(year: Int, amount: Double, allowReserveDipForPin: Boolean): Boolean {
      if (year == 2026) {
        if (clRem2026 >= amount) {
          clRem2026 -= amount
          rawClRem2026 = (rawClRem2026 - amount).coerceAtLeast(0.0)
          return true
        }
        if (allowReserveDipForPin && rawClRem2026 >= amount) {
          rawClRem2026 -= amount
          return true
        }
        return false
      } else {
        if (clRem2027 >= amount) {
          clRem2027 -= amount
          rawClRem2027 = (rawClRem2027 - amount).coerceAtLeast(0.0)
          return true
        }
        if (allowReserveDipForPin && rawClRem2027 >= amount) {
          rawClRem2027 -= amount
          return true
        }
        return false
      }
    }

    fun tryDebitYearRh(year: Int): Boolean =
      if (year == 2026 && rhRem2026 > 0) {
        rhRem2026--
        true
      } else if (year != 2026 && rhRem2027 > 0) {
        rhRem2027--
        true
      } else {
        false
      }

    // -------------------------------------------------------------------------
    // PASS 0: Honor explicit & AUTO Pinned Dates first
    // -------------------------------------------------------------------------
    val sortedPins = effective.pinnedDates.entries
      .mapNotNull { (iso, rawType) ->
        runCatching { LocalDate.parse(iso, isoFmt) }.getOrNull()?.let { d -> Triple(iso, d, rawType.uppercase(Locale.US)) }
      }
      .sortedBy { it.second }

    for ((iso, d, pinType) in sortedPins) {
      val cell = calendar[iso] ?: continue
      if (d.isBefore(horizonStart) || d.isAfter(horizonEnd)) continue
      if (cell.isBlocked || cell.isWorkedHoliday || cell.isInitialDayOff || cell.plannedType != null) continue

      val resolved = when (pinType) {
        "RH" -> if (cell.isRestrictedHoliday && canBookDatesSafely(listOf(d), "RH") && tryDebitYearRh(d.year)) "RH" else null
        "COL" -> {
          val credit = pickEligibleColCredit(d)
          if (credit != null && canBookDatesSafely(listOf(d), "COL")) {
            credit.consumed = true
            cell.linkedCreditId = credit.id
            cell.isVirtualCredit = credit.isVirtual
            "COL"
          } else null
        }
        "EL" -> if (elRem > 0 && canBookDatesSafely(listOf(d), "EL")) {
          elRem--
          "EL"
        } else null
        "HALF_CL" -> if (effective.allowHalfDayCl && canBookDatesSafely(listOf(d), "HALF_CL") &&
          tryDebitYearCl(d.year, 0.5, allowReserveDipForPin = true)
        ) "HALF_CL" else null
        "CL" -> if (canBookDatesSafely(listOf(d), "CL") &&
          tryDebitYearCl(d.year, 1.0, allowReserveDipForPin = true)
        ) "CL" else null
        "AUTO" -> {
          val colCredit = pickEligibleColCredit(d)
          when {
            cell.isRestrictedHoliday && tryDebitYearRh(d.year) && canBookDatesSafely(listOf(d), "RH") -> "RH"
            colCredit != null && canBookDatesSafely(listOf(d), "COL") -> {
              colCredit.consumed = true
              cell.linkedCreditId = colCredit.id
              cell.isVirtualCredit = colCredit.isVirtual
              "COL"
            }
            !preferZeroCl && canBookDatesSafely(listOf(d), "CL") &&
              tryDebitYearCl(d.year, 1.0, allowReserveDipForPin = true) -> "CL"
            !preferZeroCl && effective.allowHalfDayCl && canBookDatesSafely(listOf(d), "HALF_CL") &&
              tryDebitYearCl(d.year, 0.5, allowReserveDipForPin = true) -> "HALF_CL"
            elRem > 0 && canBookDatesSafely(listOf(d), "EL") -> {
              elRem--
              "EL"
            }
            else -> null
          }
        }
        else -> null
      }

      if (resolved != null) {
        cell.plannedType = resolved
        cell.isPinned = true
      }
    }

    val spreadPenalty = effective.spreadMode.penalty

    // -------------------------------------------------------------------------
    // PASS 1: FIFO / Virtual COL Credits & Official Restricted Holidays (RH)
    // -------------------------------------------------------------------------
    fun allocateColCredits(onlyUrgentOrVirtual: Boolean) {
      activeCredits
        .filter { credit ->
          !credit.consumed &&
            (!onlyUrgentOrVirtual || credit.isVirtual || ChronoUnit.DAYS.between(horizonStart, credit.expiryDate) <= 45L)
        }
        .forEach { credit ->
          val bestSlot = horizonDates
            .filter { cell ->
              !cell.date.isBefore(credit.earnedDate) &&
                !cell.date.isAfter(credit.expiryDate) &&
                canBookDatesSafely(listOf(cell.date), "COL")
            }
            .maxWithOrNull(
              compareBy<CalendarCell> { cell ->
                val ym = YearMonth.from(cell.date)
                val baseRoi = getPotentialRoi(listOf(cell.date))
                // Slightly reduce priority for RH dates so RH quota can claim actual RL festivals cleanly
                val rhReservePenalty = if (cell.isRestrictedHoliday && (if (cell.date.year == 2026) rhRem2026 else rhRem2027) > 0) -2.0 else 0.0
                baseRoi + rhReservePenalty - (getPlannedInMonth(ym) * spreadPenalty * 0.6)
              }.thenByDescending { -it.date.toEpochDay() }
            )
          if (bestSlot != null) {
            credit.consumed = true
            bestSlot.plannedType = "COL"
            bestSlot.linkedCreditId = credit.id
            bestSlot.isVirtualCredit = credit.isVirtual
          }
        }
    }

    fun allocateRestrictedHolidays() {
      val rhCandidates = horizonDates
        .filter { it.isRestrictedHoliday && canBookDatesSafely(listOf(it.date), "RH") }
        .sortedByDescending { cell ->
          getPotentialRoi(listOf(cell.date)) - (getPlannedInMonth(YearMonth.from(cell.date)) * spreadPenalty)
        }
      for (cell in rhCandidates) {
        if (!canBookDatesSafely(listOf(cell.date), "RH")) continue
        if (tryDebitYearRh(cell.date.year)) {
          cell.plannedType = "RH"
        }
      }
    }

    if (preferFifoCol) {
      allocateColCredits(onlyUrgentOrVirtual = true)
      allocateRestrictedHolidays()
      allocateColCredits(onlyUrgentOrVirtual = false)
    } else {
      allocateColCredits(onlyUrgentOrVirtual = true)
      allocateRestrictedHolidays()
      allocateColCredits(onlyUrgentOrVirtual = false)
    }

    // -------------------------------------------------------------------------
    // PASS 2: Optional TN Rule-8 Earned Leave (EL) Mon–Fri Prefix/Suffix Break
    // -------------------------------------------------------------------------
    if (effective.allowElBridges && elRem >= 5) {
      val monCandidates = mutableListOf<Pair<List<LocalDate>, Double>>()
      var mon = horizonStart
      while (mon.dayOfWeek != DayOfWeek.MONDAY && !mon.isAfter(horizonEnd)) mon = mon.plusDays(1)
      while (!mon.isAfter(horizonEnd.minusDays(4))) {
        val workWeek = (0L..4L).map { mon.plusDays(it) }
        val nonHolidaysInWeek = workWeek.filter { calendar[it.format(isoFmt)]?.isWeekendOrGh == false }
        if (workWeek.size == 5 && nonHolidaysInWeek.isNotEmpty() && canBookDatesSafely(workWeek, "EL")) {
          var prefixFree = 0
          var pb = mon.minusDays(1)
          while (calendar[pb.format(isoFmt)]?.isWeekendOrGh == true && prefixFree < 5) {
            prefixFree++
            pb = pb.minusDays(1)
          }
          var suffixFree = 0
          var pa = mon.plusDays(5)
          while (calendar[pa.format(isoFmt)]?.isWeekendOrGh == true && suffixFree < 5) {
            suffixFree++
            pa = pa.plusDays(1)
          }
          val totalFree = prefixFree + suffixFree
          val opdHits = workWeek.count { calendar[it.format(isoFmt)]?.isOpdProtected == true }
          if (totalFree >= 2 && elRem >= 5) {
            val score = (totalFree * 4.0) - (opdHits * 6.0) + if (mon.year == 2027 && mon.monthValue == 1) 5.0 else 0.0
            monCandidates.add(workWeek to score)
          }
        }
        mon = mon.plusWeeks(1)
      }

      val bestEl = monCandidates.maxByOrNull { it.second }?.first
      if (bestEl != null && elRem >= bestEl.size && canBookDatesSafely(bestEl, "EL")) {
        bestEl.forEach { d ->
          calendar[d.format(isoFmt)]?.plannedType = "EL"
        }
        elRem -= bestEl.size
      }
    }

    // -------------------------------------------------------------------------
    // PASS 3: Joint Greedy CL / Half-CL Portfolio Expansion
    // -------------------------------------------------------------------------
    fun remainingClForYear(year: Int): Double = if (year == 2026) clRem2026 else clRem2027

    val maxClBlocksCap = 12
    var loopGuard = 0
    while ((clRem2026 >= 0.5 || clRem2027 >= 0.5) && loopGuard < maxClBlocksCap) {
      loopGuard++

      var bestSingleOrPair: Pair<List<LocalDate>, Double>? = null
      var bestSingleIsHalf = false
      var bestRawRoi = Double.NEGATIVE_INFINITY

      for (cell in horizonDates) {
        val remCl = remainingClForYear(cell.date.year)
        if (remCl < 0.5) continue
        if (!canBookDatesSafely(listOf(cell.date), if (remCl >= 1.0) "CL" else "HALF_CL")) continue

        val ym = YearMonth.from(cell.date)
        val monthPenalty = getPlannedInMonth(ym) * spreadPenalty

        // Option A: 1-day Full CL (or 0.5 CL when only 0.5 remains or half-day flex boosts single bridge)
        val raw1 = getPotentialRoi(listOf(cell.date))
        val adj1 = raw1 - monthPenalty
        val canFull1 = remCl >= 1.0
        val useHalf1 = !canFull1 && effective.allowHalfDayCl && remCl >= 0.5
        if ((canFull1 || useHalf1) && (bestSingleOrPair == null || adj1 > bestSingleOrPair.second)) {
          bestSingleOrPair = listOf(cell.date) to adj1
          bestSingleIsHalf = useHalf1
          bestRawRoi = raw1
        }

        // Option B: 2-day contiguous CL bridge (e.g. Thu+Fri or Mon+Tue sandwich)
        if (remCl >= 2.0) {
          val nextDate = cell.date.plusDays(1)
          if (!nextDate.isAfter(horizonEnd) && canBookDatesSafely(listOf(cell.date, nextDate), "CL")) {
            val raw2 = getPotentialRoi(listOf(cell.date, nextDate))
            val adj2 = raw2 - monthPenalty
            if (raw2 > raw1 && (bestSingleOrPair == null || adj2 > bestSingleOrPair.second)) {
              bestSingleOrPair = listOf(cell.date, nextDate) to adj2
              bestSingleIsHalf = false
              bestRawRoi = raw2
            }
          }
        }
      }

      if (bestSingleOrPair != null && (bestRawRoi > 0.0 || effective.proximityMode == ProximityMode.MID_WEEK)) {
        val chosenDates = bestSingleOrPair.first
        val chosenType = if (bestSingleIsHalf) "HALF_CL" else "CL"
        val cost = if (bestSingleIsHalf) 0.5 else 1.0
        var bookedAll = true
        for (d in chosenDates) {
          val cell = calendar[d.format(isoFmt)] ?: continue
          if (canBookDatesSafely(listOf(d), chosenType) && tryDebitYearCl(d.year, cost, allowReserveDipForPin = false)) {
            cell.plannedType = chosenType
          } else {
            bookedAll = false
          }
        }
        if (!bookedAll) break
      } else if (effective.proximityMode == ProximityMode.BRIDGE && (clRem2026 >= 0.5 || clRem2027 >= 0.5)) {
        // Smart fallback when no positive bridge exists but user wants remaining CL utilized cleanly
        val fallbackCell = horizonDates
          .filter { cell ->
            val rem = remainingClForYear(cell.date.year)
            rem >= 0.5 &&
              !cell.isOpdProtected &&
              canBookDatesSafely(listOf(cell.date), if (rem >= 1.0) "CL" else "HALF_CL")
          }
          .minWithOrNull(
            compareBy<CalendarCell> { cell ->
              // Prefer weekend-adjacent or Monday/Friday first
              val prevOff = calendar[cell.date.minusDays(1).format(isoFmt)]?.let { isCellOffOrPlanned(it) } == true
              val nextOff = calendar[cell.date.plusDays(1).format(isoFmt)]?.let { isCellOffOrPlanned(it) } == true
              when {
                prevOff || nextOff -> 0
                cell.date.dayOfWeek == DayOfWeek.MONDAY || cell.date.dayOfWeek == DayOfWeek.FRIDAY -> 1
                else -> 2
              }
            }.thenBy { getPlannedInMonth(YearMonth.from(it.date)) }
              .thenBy { it.dateIso }
          )
        if (fallbackCell != null) {
          val rem = remainingClForYear(fallbackCell.date.year)
          val useHalf = rem < 1.0 && effective.allowHalfDayCl
          val cType = if (useHalf) "HALF_CL" else "CL"
          val cost = if (useHalf) 0.5 else 1.0
          if (tryDebitYearCl(fallbackCell.date.year, cost, allowReserveDipForPin = false)) {
            fallbackCell.plannedType = cType
          } else {
            break
          }
        } else {
          break
        }
      } else {
        break
      }
    }

    // -------------------------------------------------------------------------
    // PASS 4: Group Contiguous Off-Days into Unified Non-Overlapping Blocks
    // -------------------------------------------------------------------------
    val plannedSorted = horizonDates.filter { it.plannedType != null }.sortedBy { it.date }
    val visited = mutableSetOf<String>()
    val blocks = mutableListOf<AiLeaveRecommendation>()

    for (seed in plannedSorted) {
      if (seed.dateIso in visited) continue
      val blockCells = mutableListOf<CalendarCell>()
      val seedIsEl = seed.plannedType == "EL"

      // Walk backwards across contiguous off/planned days (bounded to same EL/casual boundary)
      var back = seed.date
      while (true) {
        val c = calendar[back.format(isoFmt)] ?: break
        val compatible = c.isWeekendOrGh ||
          (seedIsEl && (c.plannedType == "EL" || c.isExistingRegular)) ||
          (!seedIsEl && (c.plannedType in setOf("CL", "HALF_CL", "RH", "COL") || c.isExistingCasualFamily))
        if (!compatible) break
        blockCells.add(0, c)
        if (c.plannedType != null) visited.add(c.dateIso)
        back = back.minusDays(1)
      }

      // Walk forwards
      var fwd = seed.date.plusDays(1)
      while (true) {
        val c = calendar[fwd.format(isoFmt)] ?: break
        val compatible = c.isWeekendOrGh ||
          (seedIsEl && (c.plannedType == "EL" || c.isExistingRegular)) ||
          (!seedIsEl && (c.plannedType in setOf("CL", "HALF_CL", "RH", "COL") || c.isExistingCasualFamily))
        if (!compatible) break
        blockCells.add(c)
        if (c.plannedType != null) visited.add(c.dateIso)
        fwd = fwd.plusDays(1)
      }

      val distinctOrdered = blockCells.distinctBy { it.dateIso }.sortedBy { it.date }
      if (distinctOrdered.isEmpty()) continue

      val startDate = distinctOrdered.first().date
      val endDate = distinctOrdered.last().date
      val totalDays = distinctOrdered.size
      val startIso = startDate.format(isoFmt)
      val endIso = endDate.format(isoFmt)

      var creditsUsed = 0.0
      var freeDays = 0
      var opdConflicts = 0
      val opdConflictLabels = mutableListOf<String>()
      val applyLines = mutableListOf<String>()
      val dailySteps = mutableListOf<AiBridgeDayPlan>()
      val anchorNames = mutableListOf<String>()
      val pinnedUsed = mutableListOf<String>()
      var usesVirtualCol = false
      var minColExpiryDays: Long? = null

      for (cell in distinctOrdered) {
        val dayShort = "${cell.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)}, ${
          cell.date.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
        } ${cell.date.dayOfMonth}"

        when {
          cell.plannedType != null -> {
            val pType = cell.plannedType!!
            val isHalf = pType == "HALF_CL"
            creditsUsed += if (isHalf) 0.5 else 1.0
            if (cell.isPinned) pinnedUsed.add(cell.dateIso)
            if (cell.isVirtualCredit) usesVirtualCol = true
            if (cell.isOpdProtected) {
              opdConflicts++
              opdConflictLabels.add(dayShort)
            }

            val reasonNote = when (pType) {
              "RH" -> {
                val rName = cell.rhItem?.nameEnglish?.substringBefore(" (") ?: "Restricted Holiday"
                anchorNames.add(rName)
                rName
              }
              "COL" -> {
                val credit = activeCredits.firstOrNull { it.id == cell.linkedCreditId }
                val expDays = credit?.let { ChronoUnit.DAYS.between(cell.date, it.expiryDate) }
                if (expDays != null) minColExpiryDays = minOf(minColExpiryDays ?: expDays, expDays)
                if (cell.isVirtualCredit) {
                  "Virtual COL vs Planned Duty ${credit?.earnedDateIso.orEmpty()}"
                } else {
                  "FIFO vs GH ${credit?.earnedDateIso.orEmpty()}, ${expDays ?: 180}d left"
                }
              }
              "HALF_CL" -> if (cell.date.dayOfWeek == DayOfWeek.MONDAY) "FN Half-Day Bridge" else "AN Half-Day Bridge"
              "EL" -> "TN Rule-8 EL"
              else -> if (cell.isPinned) "Pinned CL" else "Casual Leave Bridge"
            }

            val unit = if (isHalf) "0.5 CL" else "1 $pType"
            applyLines.add("${cell.dateIso} — Apply $unit ($reasonNote)")
            dailySteps.add(
              AiBridgeDayPlan(
                dateIso = cell.dateIso,
                dayLabel = dayShort,
                creditType = pType,
                description = "APPLY $unit ($reasonNote)",
                isHalfDay = isHalf,
                isAlreadyLogged = false,
                isProtectedOpdDay = cell.isOpdProtected,
                linkedCreditId = cell.linkedCreditId,
                isVirtualCredit = cell.isVirtualCredit,
                isPinned = cell.isPinned
              )
            )
          }
          cell.ghItem != null -> {
            freeDays++
            val ghTitle = cell.ghItem.nameEnglish.substringBefore(" (")
            anchorNames.add(ghTitle)
            dailySteps.add(
              AiBridgeDayPlan(
                dateIso = cell.dateIso,
                dayLabel = dayShort,
                creditType = "GH",
                description = "FREE GH: ${cell.ghItem.nameEnglish} (${cell.ghItem.nameTamil})"
              )
            )
          }
          cell.isWeekend -> {
            freeDays++
            val wkLabel = when {
              cell.date.dayOfWeek == DayOfWeek.SATURDAY && cell.date.dayOfMonth in 8..14 -> "Second Saturday"
              else -> cell.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
            }
            val rhSuffix = if (cell.rhItem != null) " • ${cell.rhItem.nameEnglish}" else ""
            dailySteps.add(
              AiBridgeDayPlan(
                dateIso = cell.dateIso,
                dayLabel = dayShort,
                creditType = "WEEKEND",
                description = "FREE $wkLabel$rhSuffix"
              )
            )
          }
          else -> {
            // Already logged leave inside stretch
            val loggedType = recordsByDate[cell.dateIso]?.firstOrNull()?.category?.code ?: "CL"
            dailySteps.add(
              AiBridgeDayPlan(
                dateIso = cell.dateIso,
                dayLabel = dayShort,
                creditType = loggedType,
                description = "ALREADY LOGGED: $loggedType",
                isAlreadyLogged = true,
                isProtectedOpdDay = cell.isOpdProtected
              )
            )
          }
        }
      }

      if (creditsUsed <= 0.0 || applyLines.isEmpty()) continue

      val rhCount = dailySteps.count { it.creditType == "RH" && !it.isAlreadyLogged }
      val colCount = dailySteps.count { it.creditType == "COL" && !it.isAlreadyLogged }
      val clFullCount = dailySteps.count { it.creditType == "CL" && !it.isAlreadyLogged }
      val clHalfCount = dailySteps.count { it.creditType == "HALF_CL" && !it.isAlreadyLogged }
      val elCount = dailySteps.count { it.creditType == "EL" && !it.isAlreadyLogged }
      val mult = ((totalDays.toDouble() / creditsUsed) * 10.0).roundToInt() / 10.0

      val mixParts = buildList {
        if (rhCount > 0) add("$rhCount RH")
        if (colCount > 0) add("$colCount COL")
        if (clFullCount > 0) add("$clFullCount CL")
        if (clHalfCount > 0) add("${clHalfCount * 0.5} CL (½d)")
        if (elCount > 0) add("$elCount EL")
        if (freeDays > 0) add("$freeDays Free GH/Wknd")
      }

      val distinctAnchors = anchorNames.distinct().take(2)
      val headline = when {
        elCount > 0 ->
          "Rule-8 Optimal EL ${totalDays}-Day Break (${startDate.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)})"
        startDate.monthValue == 12 && startDate.year == 2026 && endDate.year == 2027 ->
          "Christmas → New Year ${totalDays}-Day Finale"
        startDate.monthValue == 1 && startDate.dayOfMonth in 12..17 && startDate.year == 2027 ->
          "Pongal 2027 ${totalDays}-Day Harvest Bridge"
        distinctAnchors.isNotEmpty() ->
          "${distinctAnchors.joinToString(" & ")} ${totalDays}-Day Bridge"
        totalDays == 1 ->
          "${startDate.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)} ${startDate.dayOfMonth} Mid-Week Rest"
        else ->
          "${startDate.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)} ${startDate.year} ${totalDays}-Day Break"
      }

      val urgency = when {
        usesVirtualCol -> "VIRTUAL +1 COL PLAN"
        colCount > 0 && minColExpiryDays != null && minColExpiryDays <= 45L -> "URGENT COL (${minColExpiryDays}d left)"
        colCount > 0 && minColExpiryDays != null -> "FIFO COL (${minColExpiryDays}d validity)"
        startDate.year == 2026 && (clFullCount + clHalfCount + rhCount > 0) -> "SAVES DEC 31 LAPSE"
        pinnedUsed.isNotEmpty() -> "PINNED DATE LOCKED"
        else -> ""
      }

      val opdNote = when {
        effective.protectedDaysOfWeek.isEmpty() && effective.protectedDatesIso.isEmpty() -> ""
        opdConflicts == 0 -> "Duty/OPD-Safe: Zero protected clinical shifts touched"
        else -> "Duty/OPD Notice: Touches ${opdConflictLabels.joinToString(", ")} — arrange cover"
      }

      val isElPlan = elCount > 0
      val complianceText = if (isElPlan) {
        "TN Leave Rules 1933 Rule 8 Compliant: Zero weekends trapped inside EL bounds; isolated from CL."
      } else {
        "Verified ≤10d Continuous Stretch (${totalDays}d) under ${weekendPolicy.shortChip} • Joint non-overlapping quota."
      }

      val rationale = buildString {
        append("Unlocks $totalDays day(s) off ($startIso → $endIso) for ${formatHalf(creditsUsed)} credit(s) [${mult}x ROI]. ")
        if (usesVirtualCol) append("Links planned Holiday Work Duty (+1 Virtual COL). ")
        else if (colCount > 0) append("Consumes GH-Worked credit FIFO within 180-day window. ")
        if (pinnedUsed.isNotEmpty()) append("Honors ${pinnedUsed.size} pinned date(s). ")
        if (effective.reserveClFloor > 0.0 && (clFullCount + clHalfCount) > 0) {
          append("Protects ≥${formatHalf(effective.reserveClFloor)} CL emergency reserve.")
        } else if (startDate.year == 2026 && (clFullCount + clHalfCount + rhCount) > 0) {
          append("Prevents 2026 credits from lapsing Dec 31.")
        }
      }.trim()

      blocks.add(
        AiLeaveRecommendation(
          id = "port_${startIso}_$endIso",
          title = headline,
          startDateIso = startIso,
          endDateIso = endIso,
          totalDaysOff = totalDays,
          leaveCreditsUsed = creditsUsed,
          freeDaysUnlocked = freeDays,
          efficiencyMultiplier = mult,
          exactDatesToApply = applyLines,
          creditMixSummary = mixParts.joinToString(" + "),
          projectedBalanceAfter = "",
          whyOptimalForHod = rationale,
          tnRuleComplianceCheck = complianceText,
          dailyPlan = dailySteps,
          expiryUrgencyTag = urgency,
          opdImpactNote = opdNote,
          hasOpdConflict = opdConflicts > 0,
          ltcOdSynergyNote = if (isElPlan) "Eligible for 2024–27 LTC & 10d EL Encashment" else "",
          isHalfDayBridge = clHalfCount > 0,
          isElBridge = isElPlan,
          verificationStatus = "JOINT PORTFOLIO VERIFIED",
          usesVirtualCol = usesVirtualCol,
          pinnedDatesUsed = pinnedUsed
        )
      )
    }

    val priorityPinnedOrVirtual = blocks.filter { it.pinnedDatesUsed.isNotEmpty() || it.usesVirtualCol }
    val remainingFill = blocks.filterNot { it.pinnedDatesUsed.isNotEmpty() || it.usesVirtualCol }
      .take((12 - priorityPinnedOrVirtual.size).coerceAtLeast(0))
    val chronological = (priorityPinnedOrVirtual + remainingFill)
      .distinctBy { it.id }
      .sortedBy { it.startDateIso }
    return AiLeaveRecommendationEngine.annotatePortfolioBalances(
      plans = chronological,
      summary2026 = summary2026,
      summary2027 = summary2027,
      colGhAuditAllYears = colGhAuditAllYears,
      constraints = effective
    )
  }

  private fun formatHalf(v: Double): String =
    if (v % 1.0 == 0.0) v.toInt().toString() else String.format(Locale.US, "%.1f", v)
}
