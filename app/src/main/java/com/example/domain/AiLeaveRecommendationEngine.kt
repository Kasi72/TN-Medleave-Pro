package com.example.domain

import android.util.Log
import com.example.data.BridgeDayStep
import com.example.data.ColGhAuditSummary
import com.example.data.LeaveCategory
import com.example.data.LeaveRecord
import com.example.data.SmartLeaveBridge
import com.example.data.TnHolidayItem
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "AiLeaveOptimizer"

data class AiBridgeDayPlan(
  val dateIso: String,
  val dayLabel: String,
  val creditType: String, // "WEEKEND", "GH", "RH", "CL", "HALF_CL", "COL", "EL"
  val description: String,
  val isHalfDay: Boolean = false,
  val isAlreadyLogged: Boolean = false,
  val isProtectedOpdDay: Boolean = false
)

data class AiLeaveRecommendation(
  val id: String,
  val title: String,
  val startDateIso: String,
  val endDateIso: String,
  val totalDaysOff: Int,
  val leaveCreditsUsed: Double,
  val freeDaysUnlocked: Int = 0,
  val efficiencyMultiplier: Double,
  val exactDatesToApply: List<String> = emptyList(),
  val creditMixSummary: String,
  val projectedBalanceAfter: String = "",
  val portfolioBalanceAfter: String = "",
  val whyOptimalForHod: String,
  val tnRuleComplianceCheck: String,
  val dailyPlan: List<AiBridgeDayPlan>,
  val expiryUrgencyTag: String = "",
  val opdImpactNote: String = "",
  val hasOpdConflict: Boolean = false,
  val ltcOdSynergyNote: String = "",
  val isHalfDayBridge: Boolean = false,
  val isElBridge: Boolean = false,
  val verificationStatus: String = "VERIFIED_RULES"
) {
  fun toSmartLeaveBridge(): SmartLeaveBridge {
    val steps = dailyPlan.map { day ->
      val mappedBadge = when (day.creditType.uppercase(Locale.US)) {
        "WEEKEND" -> "WEEKEND"
        "GH" -> "GH"
        "RH" -> "RH"
        "COL" -> "COL"
        "EL" -> "EL"
        "HALF_CL" -> "HALF_CL"
        else -> "LEAVE_CREDIT"
      }
      val descWithPrefix = buildString {
        if (day.isHalfDay && !day.description.contains("0.5", ignoreCase = true) && !day.description.contains("half", ignoreCase = true)) {
          append("[0.5 CL Half-Day] ")
        }
        append(day.description)
      }
      BridgeDayStep(
        dateIso = day.dateIso,
        dayLabel = day.dayLabel,
        badgeType = mappedBadge,
        description = descWithPrefix
      )
    }
    val complianceFull = buildString {
      append(tnRuleComplianceCheck)
      if (opdImpactNote.isNotBlank()) append(" • $opdImpactNote")
      if (whyOptimalForHod.isNotBlank()) append(" • $whyOptimalForHod")
    }
    return SmartLeaveBridge(
      id = id,
      title = title,
      startDateIso = startDateIso,
      endDateIso = endDateIso,
      totalDaysOff = totalDaysOff,
      leaveCreditsRequired = leaveCreditsUsed.roundToInt().coerceAtLeast(1),
      efficiencyRatio = efficiencyMultiplier.toFloat(),
      recommendedMix = creditMixSummary,
      dayByDayBreakdown = steps,
      tnRuleComplianceNote = complianceFull
    )
  }
}

sealed interface AiRecommendationUiState {
  data object Idle : AiRecommendationUiState
  data object Loading : AiRecommendationUiState
  data class Success(
    val focusGoal: String,
    val executiveSummary: String,
    val recommendations: List<AiLeaveRecommendation>,
    val analyzedHolidayCount: Int = 0,
    val appliedConstraintsSummary: String = ""
  ) : AiRecommendationUiState
  data class Error(
    val focusGoal: String,
    val message: String
  ) : AiRecommendationUiState
}

object AiLeaveRecommendationEngine {

  private val isoDateRegex = Regex("""^\d{4}-\d{2}-\d{2}$""")
  private val isoFmt = DateTimeFormatter.ISO_LOCAL_DATE

  private val recommendationSchema = Schema.obj(
    properties = mapOf(
      "executiveSummary" to Schema.string(
        description = "Concise 2-3 sentence strategic overview explaining how the officer maximizes holidays while protecting CL safety reserves, OPD days, and expiring COL credits."
      ),
      "recommendations" to Schema.array(
        description = "4 to 5 optimal upcoming long-weekend or EL prefix/suffix leave recommendations aligned strictly with the user's constraints.",
        items = Schema.obj(
          properties = mapOf(
            "id" to Schema.string(description = "Unique slug identifier, e.g. ai_christmas_2026"),
            "title" to Schema.string(description = "Executive title of the break, e.g. Pongal 2027 5-Day Harvest Bridge"),
            "startDateIso" to Schema.string(description = "Start date of total stretch in YYYY-MM-DD"),
            "endDateIso" to Schema.string(description = "End date of total stretch in YYYY-MM-DD"),
            "totalDaysOff" to Schema.integer(description = "Total consecutive calendar days off"),
            "leaveCreditsUsed" to Schema.double(description = "Total personal leave credits debited"),
            "freeDaysUnlocked" to Schema.integer(description = "Number of free Saturday, Sunday, and GH days unlocked"),
            "efficiencyMultiplier" to Schema.double(description = "Ratio totalDaysOff / leaveCreditsUsed"),
            "exactDatesToApply" to Schema.array(
              description = "Exact action lines formatted `YYYY-MM-DD — Apply 1 <RH|COL|CL|EL> (<Note>)` or `YYYY-MM-DD — Apply 0.5 CL (<Note>)`",
              items = Schema.string()
            ),
            "creditMixSummary" to Schema.string(description = "Short credit breakdown, e.g. '1 RH + 1 COL + 3 Free Days'"),
            "projectedBalanceAfter" to Schema.string(description = "Remaining leave balances after applying this plan"),
            "whyOptimalForHod" to Schema.string(description = "Why this plan beats alternatives for clinical schedule, expiry urgency, and ROI"),
            "tnRuleComplianceCheck" to Schema.string(description = "Verification under TN Leave Rules 1933 & FR Annexure VII"),
            "dailyPlan" to Schema.array(
              description = "Chronological day-by-day entries from startDateIso to endDateIso",
              items = Schema.obj(
                properties = mapOf(
                  "dateIso" to Schema.string(description = "YYYY-MM-DD"),
                  "dayLabel" to Schema.string(description = "Short day and date label, e.g. 'Thu, Dec 24'"),
                  "creditType" to Schema.enumeration(
                    values = listOf("WEEKEND", "GH", "RH", "CL", "HALF_CL", "COL", "EL"),
                    description = "Must be one of: WEEKEND, GH, RH, CL, HALF_CL, COL, EL"
                  ),
                  "description" to Schema.string(description = "Holiday name or exact leave action")
                )
              )
            )
          ),
          optionalProperties = listOf("freeDaysUnlocked", "projectedBalanceAfter")
        )
      )
    )
  )

  private val systemInstructionContent = content {
    text(
      """
      You are the Senior AI Leave Strategist for a Tamil Nadu Government Medical Officer / HOD (Department of Psychiatry, TN Medical Service).
      Your job is to select, refine, and explain the MOST INTELLIGENT upcoming leave combinations using ONLY verified dates from the Tamil Nadu Holiday Database (GH & RH), live credit balances, 6-month FIFO COL expiry rules, Earned Leave Rule 8, and clinical OPD constraints.

      MANDATORY STATUTORY RULES (TN LEAVE RULES 1933 & FR ANNEXURE VII):
      1. CL / RH / COL 10-DAY CEILING: Total continuous absence (including prefixed, suffixed, and intervening Saturdays, Sundays, GH, and ANY adjacent already-logged CL/RH/COL in the ledger) MUST NEVER exceed 10 days.
      2. NON-COMBINATION: CL/RH/COL can NEVER be combined in continuation with Earned Leave (EL) or Medical Leave (ML) without returning to duty between them.
      3. EARNED LEAVE (RULE 8) BOUNDARY RULE: Weekends/GH *inside* an EL spell ARE debited as EL; weekends/GH *strictly prefixed before or suffixed after* an EL spell are FREE (0 EL debit). Always start EL on Monday (or after GH) and end on Friday (or before GH).
      4. RESTRICTED HOLIDAYS (RH): Strictly ≤ 3/calendar year, and ONLY on official notified TN RL dates.
      5. COMPENSATORY OFF (COL): 1-to-1 FIFO against an unused GH Worked date within 180 days (6 months).
      6. HALF-DAY CL (0.5 CL): Valid on working days (FN/AN) to stretch weekends/holidays with minimal debit.
      7. Never recommend past dates. Respect the officer's CL safety reserve floor and protected OPD weekdays whenever possible.
      """.trimIndent()
    )
  }

  private val optimizerGenerationConfig = generationConfig {
    responseMimeType = "application/json"
    responseSchema = recommendationSchema
    temperature = 0.2f
    maxOutputTokens = 8192
  }

  private val primaryGenerativeModel by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.8-flash",
      generationConfig = optimizerGenerationConfig,
      systemInstruction = systemInstructionContent
    )
  }

  private val liteFallbackGenerativeModel by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.5-flash-lite",
      generationConfig = optimizerGenerationConfig,
      systemInstruction = systemInstructionContent
    )
  }

  /**
   * Parses natural-language optimizer asks (e.g. "keep 3 cl reserve", "protect tue thu opd",
   * "7 day EL LTC break", "zero cl") into concrete solver constraints merged with UI toggles.
   */
  fun parseNaturalLanguageConstraints(
    base: OptimizerConstraints,
    rawQuery: String = base.customNaturalPrompt
  ): OptimizerConstraints {
    val q = rawQuery.trim().lowercase(Locale.US)
    if (q.isEmpty()) return base

    var out = base

    // Parse "keep/reserve/save X cl"
    val reserveMatch = Regex("""(?:keep|reserve|save|protect|hold)\s+(\d+(?:\.\d+)?)\s*cl""").find(q)
    if (reserveMatch != null) {
      val floor = reserveMatch.groupValues[1].toDoubleOrNull()?.coerceIn(0.0, 10.0)
      if (floor != null) out = out.copy(reserveClFloor = maxOf(out.reserveClFloor, floor))
    }

    if (q.contains("zero cl") || q.contains("zero-cl") || q.contains("no cl") || q.contains("without cl")) {
      out = out.copy(strictZeroCl = true)
    }

    if (q.contains("earned leave") || Regex("""\bel\b""").containsMatchIn(q) ||
      q.contains("ltc") || q.contains("long break") || q.contains("7 day") || q.contains("7-day") ||
      q.contains("8 day") || q.contains("9 day") || q.contains("vacation")
    ) {
      out = out.copy(allowElBridges = true)
    }

    if (q.contains("half day") || q.contains("half-day") || q.contains("0.5") ||
      Regex("""\b(?:fn|an)\b""").containsMatchIn(q)
    ) {
      out = out.copy(allowHalfDayCl = true)
    }

    // Parse protected OPD days mentioned in text
    val parsedOpd = out.protectedDaysOfWeek.toMutableSet()
    if (q.contains("mon")) parsedOpd.add(DayOfWeek.MONDAY)
    if (q.contains("tue")) parsedOpd.add(DayOfWeek.TUESDAY)
    if (q.contains("wed")) parsedOpd.add(DayOfWeek.WEDNESDAY)
    if (q.contains("thu")) parsedOpd.add(DayOfWeek.THURSDAY)
    if (q.contains("fri")) parsedOpd.add(DayOfWeek.FRIDAY)
    if (q.contains("sat") && (q.contains("opd") || q.contains("avoid") || q.contains("protect"))) {
      parsedOpd.add(DayOfWeek.SATURDAY)
    }
    if (parsedOpd != out.protectedDaysOfWeek) {
      out = out.copy(protectedDaysOfWeek = parsedOpd)
    }

    // Horizon hints
    when {
      q.contains("60 day") || q.contains("next month") || q.contains("soon") || q.contains("urgent") ->
        out = out.copy(horizonMode = OptimizerHorizon.NEXT_60_DAYS)
      q.contains("december") || q.contains("year end") || q.contains("year-end") || q.contains("lapse") || q.contains("2026 only") ->
        out = out.copy(horizonMode = OptimizerHorizon.REST_OF_2026)
      q.contains("pongal") || q.contains("q1 2027") || q.contains("january 2027") ->
        if (out.horizonMode == OptimizerHorizon.REST_OF_2026) {
          out = out.copy(horizonMode = OptimizerHorizon.Q1_Q2_2027)
        }
    }

    return out
  }

  /**
   * Computes joint balance conservation, lapse prevention, and reserve safety across a user-selected
   * portfolio of recommendations.
   */
  fun computePortfolioImpact(
    selectedRecs: List<AiLeaveRecommendation>,
    summary2026: YearCreditSummary,
    summary2027: YearCreditSummary?,
    colGhAuditAllYears: ColGhAuditSummary,
    constraints: OptimizerConstraints = OptimizerConstraints()
  ): PortfolioImpactSummary {
    var cl26 = 0.0
    var rh26 = 0
    var col = 0
    var el = 0
    var cl27 = 0.0
    var rh27 = 0
    var totalDaysOff = 0
    val seenActionDates = mutableSetOf<String>()
    var overlapDetected = false

    selectedRecs.forEach { rec ->
      totalDaysOff += rec.totalDaysOff
      rec.dailyPlan.forEach { step ->
        if (step.isAlreadyLogged) return@forEach
        val year = step.dateIso.take(4).toIntOrNull() ?: 2026
        val type = step.creditType.uppercase(Locale.US)
        if (type in setOf("CL", "HALF_CL", "RH", "COL", "EL")) {
          if (!seenActionDates.add(step.dateIso)) {
            overlapDetected = true
          }
        }
        when (type) {
          "HALF_CL" -> if (year == 2026) cl26 += 0.5 else cl27 += 0.5
          "CL" -> {
            val delta = if (step.isHalfDay) 0.5 else 1.0
            if (year == 2026) cl26 += delta else cl27 += delta
          }
          "RH" -> if (year == 2026) rh26++ else rh27++
          "COL" -> col++
          "EL" -> el++
        }
      }
    }

    val remCl26 = (summary2026.clRemaining - cl26).coerceAtLeast(0.0)
    val remRh26 = (summary2026.rhRemaining - rh26).coerceAtLeast(0)
    val remCol = (colGhAuditAllYears.availableUnusedGhCount - col).coerceAtLeast(0)
    val baseCl27 = summary2027?.clRemaining ?: 12.0
    val baseRh27 = summary2027?.rhRemaining ?: 3
    val remCl27 = (baseCl27 - cl27).coerceAtLeast(0.0)
    val remRh27 = (baseRh27 - rh27).coerceAtLeast(0)

    val exceedsMsg = buildList {
      if (cl26 > summary2026.clRemaining) add("2026 CL overdrawn (${formatHalf(cl26)}/${formatHalf(summary2026.clRemaining)})")
      if (rh26 > summary2026.rhRemaining) add("2026 RH overdrawn ($rh26/${summary2026.rhRemaining})")
      if (col > colGhAuditAllYears.availableUnusedGhCount) add("COL exceeds ${colGhAuditAllYears.availableUnusedGhCount} GH credits")
      if (cl27 > baseCl27) add("2027 CL overdrawn")
      if (rh27 > baseRh27) add("2027 RH overdrawn")
      if (overlapDetected) add("Selected plans share overlapping leave dates")
    }.joinToString(" • ").ifBlank { null }

    val violatesReserve = constraints.reserveClFloor > 0.0 && (
      (cl26 > 0.0 && remCl26 < constraints.reserveClFloor) ||
        (cl27 > 0.0 && remCl27 < constraints.reserveClFloor)
      )

    return PortfolioImpactSummary(
      selectedPlanCount = selectedRecs.size,
      totalDaysOffUnlocked = totalDaysOff,
      totalCreditsDebited = cl26 + rh26 + col + el + cl27 + rh27,
      cl2026Used = cl26,
      rh2026Used = rh26,
      colUsed = col,
      elUsed = el,
      cl2027Used = cl27,
      rh2027Used = rh27,
      remainingCl2026 = remCl26,
      remainingRh2026 = remRh26,
      remainingColPool = remCol,
      remainingCl2027 = remCl27,
      remainingRh2027 = remRh27,
      dec2026LapseSavedDays = (cl26 + rh26),
      violatesClReserve = violatesReserve,
      hasOverlappingDates = overlapDetected,
      exceedsQuotaMessage = exceedsMsg
    )
  }

  /**
   * Core Phase-1 Constraint Solver:
   * Evaluates upcoming 2026–2027 bridges with adjacency-aware 10-day & EL-collision checks,
   * half-day 0.5 CL extensions, EL Mon–Fri prefix/suffix spells, 180-day COL FIFO expiry decay,
   * protected OPD day filtering, CL safety reserve floors, and sequential portfolio accounting.
   */
  fun scanHolidayDatabaseForLongWeekends(
    summary2026: YearCreditSummary,
    colGhAuditAllYears: ColGhAuditSummary,
    records: List<LeaveRecord>,
    focusGoal: String = "",
    summary2027: YearCreditSummary? = null,
    referenceDate: LocalDate = LocalDate.now(),
    constraints: OptimizerConstraints = OptimizerConstraints()
  ): List<AiLeaveRecommendation> {
    val today = if (referenceDate.year < 2026) LocalDate.of(2026, 11, 26) else referenceDate
    val effective = parseNaturalLanguageConstraints(constraints, "$focusGoal ${constraints.customNaturalPrompt}")

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

    val allGh = TnHolidays2027Catalog.allSyncedPublicHolidays()
    val allRh = TnHolidays2027Catalog.allSyncedRestrictedHolidays()
    val ghMap = allGh.associateBy { it.dateIso }
    val rhMap = allRh.associateBy { it.dateIso }

    val recordsByDate = records.groupBy { it.dateIso }
    val existingLeaveDates = records.filter {
      it.category in setOf(LeaveCategory.CL, LeaveCategory.RH, LeaveCategory.COL, LeaveCategory.PL, LeaveCategory.EL, LeaveCategory.ML)
    }.map { it.dateIso }.toSet()
    val existingRegularDates = records.filter {
      it.category == LeaveCategory.EL || it.category == LeaveCategory.ML
    }.map { it.dateIso }.toSet()
    val existingCasualFamilyDates = records.filter {
      it.category in setOf(LeaveCategory.CL, LeaveCategory.RH, LeaveCategory.COL, LeaveCategory.PL)
    }.map { it.dateIso }.toSet()

    val unusedGhSorted = colGhAuditAllYears.unusedGhRecords
      .sortedBy { it.dateIso }
    val goalLower = (focusGoal + " " + effective.customNaturalPrompt).lowercase(Locale.US)
    val preferZeroCl = effective.strictZeroCl || goalLower.contains("zero-cl") || goalLower.contains("rh + col")
    val preferFifoCol = effective.prioritizeExpiringCol || goalLower.contains("fifo") || goalLower.contains("expiring gh")
    val preferMegabridges = goalLower.contains("megabridge") || goalLower.contains("festival")

    fun isFreeCalendarDay(date: LocalDate): Boolean {
      val iso = date.format(isoFmt)
      val isSat = effective.includeSaturdays && date.dayOfWeek == DayOfWeek.SATURDAY
      return ghMap.containsKey(iso) || date.dayOfWeek == DayOfWeek.SUNDAY || isSat
    }

    fun isFestivalDay(date: LocalDate): Boolean {
      val iso = date.format(isoFmt)
      return ghMap.containsKey(iso) || rhMap.containsKey(iso)
    }

    /**
     * Checks adjacency against already-logged leaves in [records] so the 10-day continuous
     * absence cap and EL+CL non-combination rules cannot be violated by bordering an existing spell.
     */
    fun checkAdjacencyAndCollision(
      start: LocalDate,
      end: LocalDate,
      isElSpell: Boolean
    ): Pair<Boolean, Int> {
      // Check internal collisions
      var d = start
      while (!d.isAfter(end)) {
        val iso = d.format(isoFmt)
        val recs = recordsByDate[iso].orEmpty()
        if (isElSpell && recs.any { it.category in setOf(LeaveCategory.CL, LeaveCategory.RH, LeaveCategory.COL) }) {
          return false to 0
        }
        if (!isElSpell && recs.any { it.category == LeaveCategory.EL || it.category == LeaveCategory.ML }) {
          return false to 0
        }
        if (recs.any { it.category == LeaveCategory.GH_WORKED || it.category == LeaveCategory.ON_DUTY }) {
          return false to 0
        }
        d = d.plusDays(1)
      }

      // Walk backwards & forwards across contiguous weekends/GHs and existing logged leaves
      var combinedStart = start
      var guard = 0
      while (guard < 16) {
        val prev = combinedStart.minusDays(1)
        val prevIso = prev.format(isoFmt)
        val touchesIncompatible = if (isElSpell) prevIso in existingCasualFamilyDates else prevIso in existingRegularDates
        if (touchesIncompatible) return false to 0
        if (isFreeCalendarDay(prev) || prevIso in existingLeaveDates) {
          combinedStart = prev
          guard++
        } else {
          break
        }
      }

      var combinedEnd = end
      guard = 0
      while (guard < 16) {
        val next = combinedEnd.plusDays(1)
        val nextIso = next.format(isoFmt)
        val touchesIncompatible = if (isElSpell) nextIso in existingCasualFamilyDates else nextIso in existingRegularDates
        if (touchesIncompatible) return false to 0
        if (isFreeCalendarDay(next) || nextIso in existingLeaveDates) {
          combinedEnd = next
          guard++
        } else {
          break
        }
      }

      val trueContinuousStretch = ChronoUnit.DAYS.between(combinedStart, combinedEnd).toInt() + 1
      if (!isElSpell && effective.enforce10DayCap && trueContinuousStretch > TamilNaduLeaveRulesService.CL_MAX_STRETCH_INCLUDING_HOLIDAYS) {
        return false to trueContinuousStretch
      }
      return true to trueContinuousStretch
    }

    fun pickExpiringColForDate(
      targetDate: LocalDate,
      usedGhIds: Set<String>
    ): Pair<LeaveRecord, Long>? {
      val candidates = unusedGhSorted.filter { it.id !in usedGhIds }.mapNotNull { gh ->
        val ghDate = runCatching { LocalDate.parse(gh.dateIso, isoFmt) }.getOrNull() ?: return@mapNotNull null
        val ageDays = ChronoUnit.DAYS.between(ghDate, targetDate)
        if (ageDays in 0..TamilNaduLeaveRulesService.COL_EXPIRY_WINDOW_DAYS) {
          gh to (TamilNaduLeaveRulesService.COL_EXPIRY_WINDOW_DAYS - ageDays)
        } else {
          null
        }
      }
      // FIFO: consume the earliest-expiring valid GH Worked credit first!
      return candidates.minByOrNull { it.second }
    }

    fun buildDynamicWindow(
      start: LocalDate,
      end: LocalDate,
      halfDayTargetIso: String? = null
    ): AiLeaveRecommendation? {
      if (start.isBefore(horizonStart) || end.isAfter(horizonEnd) || end.isBefore(start)) return null
      val totalDays = ChronoUnit.DAYS.between(start, end).toInt() + 1
      val maxCap = if (effective.enforce10DayCap) 10 else 14
      if (totalDays < 3 || totalDays > maxCap) return null

      val (safeAdjacency, trueCombinedStretch) = checkAdjacencyAndCollision(start, end, isElSpell = false)
      if (!safeAdjacency) return null

      // Enforce safety reserve floor per year
      val availCl2026 = (summary2026.clRemaining - effective.reserveClFloor).coerceAtLeast(0.0)
      val availCl2027 = ((summary2027?.clRemaining ?: 12.0) - effective.reserveClFloor).coerceAtLeast(0.0)
      var remCl2026 = availCl2026
      var remRh2026 = summary2026.rhRemaining
      var remCl2027 = availCl2027
      var remRh2027 = summary2027?.rhRemaining ?: 3
      val usedGhIds = mutableSetOf<String>()

      var creditsUsed = 0.0
      var freeDays = 0
      var minColDaysToExpiry: Long? = null
      var opdConflictCount = 0
      val opdConflictDays = mutableListOf<String>()
      val applyDates = mutableListOf<String>()
      val dailySteps = mutableListOf<AiBridgeDayPlan>()
      val anchorNames = mutableListOf<String>()

      var cursor = start
      while (!cursor.isAfter(end)) {
        val iso = cursor.format(isoFmt)
        val dayShort = "${cursor.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)}, ${
          cursor.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
        } ${cursor.dayOfMonth}"
        val ghItem: TnHolidayItem? = ghMap[iso]
        val rhItem: TnHolidayItem? = rhMap[iso]
        val isWeekend = cursor.dayOfWeek == DayOfWeek.SUNDAY ||
          (effective.includeSaturdays && cursor.dayOfWeek == DayOfWeek.SATURDAY)
        val existingDayRecs = recordsByDate[iso].orEmpty()
        val alreadyLogged = existingDayRecs.any {
          it.category in setOf(LeaveCategory.CL, LeaveCategory.RH, LeaveCategory.COL, LeaveCategory.PL)
        }
        val isOpdDay = cursor.dayOfWeek in effective.protectedDaysOfWeek ||
          iso in effective.protectedDatesIso

        val is2026 = cursor.year == 2026
        val canUseRh = if (is2026) remRh2026 > 0 else remRh2027 > 0
        val canUseFullCl = if (is2026) remCl2026 >= 1.0 else remCl2027 >= 1.0
        val canUseHalfCl = if (is2026) remCl2026 >= 0.5 else remCl2027 >= 0.5
        val colCandidate = pickExpiringColForDate(cursor, usedGhIds)

        when {
          ghItem != null -> {
            freeDays++
            anchorNames.add(ghItem.nameEnglish.substringBefore(" ("))
            dailySteps.add(
              AiBridgeDayPlan(
                dateIso = iso,
                dayLabel = dayShort,
                creditType = "GH",
                description = "FREE GH: ${ghItem.nameEnglish} (${ghItem.nameTamil})"
              )
            )
          }
          isWeekend -> {
            freeDays++
            val wkndDesc = if (rhItem != null) {
              "FREE Weekend • ${rhItem.nameEnglish}"
            } else {
              "FREE ${cursor.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} Weekend"
            }
            dailySteps.add(
              AiBridgeDayPlan(
                dateIso = iso,
                dayLabel = dayShort,
                creditType = "WEEKEND",
                description = wkndDesc
              )
            )
          }
          alreadyLogged -> {
            val loggedType = existingDayRecs.first().category.code
            dailySteps.add(
              AiBridgeDayPlan(
                dateIso = iso,
                dayLabel = dayShort,
                creditType = loggedType,
                description = "ALREADY LOGGED: ${existingDayRecs.first().category.shortLabel}",
                isAlreadyLogged = true,
                isProtectedOpdDay = isOpdDay
              )
            )
          }
          halfDayTargetIso == iso && effective.allowHalfDayCl && !preferZeroCl && canUseHalfCl -> {
            val session = if (cursor.dayOfWeek == DayOfWeek.MONDAY) "FN" else "AN"
            creditsUsed += 0.5
            if (is2026) remCl2026 -= 0.5 else remCl2027 -= 0.5
            if (isOpdDay) {
              opdConflictCount++
              opdConflictDays.add(dayShort)
            }
            applyDates.add("$iso — Apply 0.5 CL ($session Half-Day Bridge)")
            dailySteps.add(
              AiBridgeDayPlan(
                dateIso = iso,
                dayLabel = dayShort,
                creditType = "HALF_CL",
                description = "APPLY 0.5 CL ($session Half-Day Bridge)",
                isHalfDay = true,
                isProtectedOpdDay = isOpdDay
              )
            )
          }
          rhItem != null && canUseRh && !preferFifoCol -> {
            creditsUsed += 1.0
            if (is2026) remRh2026-- else remRh2027--
            if (isOpdDay) {
              opdConflictCount++
              opdConflictDays.add(dayShort)
            }
            val cleanRhName = rhItem.nameEnglish.substringBefore(" (")
            anchorNames.add(cleanRhName)
            applyDates.add("$iso — Apply 1 RH ($cleanRhName)")
            dailySteps.add(
              AiBridgeDayPlan(
                dateIso = iso,
                dayLabel = dayShort,
                creditType = "RH",
                description = "APPLY 1 RH: ${rhItem.nameEnglish}",
                isProtectedOpdDay = isOpdDay
              )
            )
          }
          colCandidate != null && (preferFifoCol || preferZeroCl || !canUseFullCl || rhItem == null) -> {
            val (pairedGh, daysLeftToExpiry) = colCandidate
            usedGhIds.add(pairedGh.id)
            creditsUsed += 1.0
            minColDaysToExpiry = minOf(minColDaysToExpiry ?: daysLeftToExpiry, daysLeftToExpiry)
            if (isOpdDay) {
              opdConflictCount++
              opdConflictDays.add(dayShort)
            }
            applyDates.add("$iso — Apply 1 COL (vs GH ${pairedGh.dateIso}, ${daysLeftToExpiry}d left)")
            dailySteps.add(
              AiBridgeDayPlan(
                dateIso = iso,
                dayLabel = dayShort,
                creditType = "COL",
                description = "APPLY 1 COL (FIFO vs GH ${pairedGh.dateIso})",
                isProtectedOpdDay = isOpdDay
              )
            )
          }
          rhItem != null && canUseRh -> {
            creditsUsed += 1.0
            if (is2026) remRh2026-- else remRh2027--
            if (isOpdDay) {
              opdConflictCount++
              opdConflictDays.add(dayShort)
            }
            val cleanRhName = rhItem.nameEnglish.substringBefore(" (")
            anchorNames.add(cleanRhName)
            applyDates.add("$iso — Apply 1 RH ($cleanRhName)")
            dailySteps.add(
              AiBridgeDayPlan(
                dateIso = iso,
                dayLabel = dayShort,
                creditType = "RH",
                description = "APPLY 1 RH: ${rhItem.nameEnglish}",
                isProtectedOpdDay = isOpdDay
              )
            )
          }
          else -> {
            if (preferZeroCl) return null
            if (!canUseFullCl) {
              if (effective.allowHalfDayCl && canUseHalfCl && (cursor == start || cursor == end)) {
                val session = if (cursor.dayOfWeek == DayOfWeek.MONDAY) "FN" else "AN"
                creditsUsed += 0.5
                if (is2026) remCl2026 -= 0.5 else remCl2027 -= 0.5
                if (isOpdDay) {
                  opdConflictCount++
                  opdConflictDays.add(dayShort)
                }
                applyDates.add("$iso — Apply 0.5 CL ($session Half-Day)")
                dailySteps.add(
                  AiBridgeDayPlan(
                    dateIso = iso,
                    dayLabel = dayShort,
                    creditType = "HALF_CL",
                    description = "APPLY 0.5 CL ($session Half-Day)",
                    isHalfDay = true,
                    isProtectedOpdDay = isOpdDay
                  )
                )
              } else {
                return null
              }
            } else {
              creditsUsed += 1.0
              if (is2026) remCl2026 -= 1.0 else remCl2027 -= 1.0
              if (isOpdDay) {
                opdConflictCount++
                opdConflictDays.add(dayShort)
              }
              applyDates.add("$iso — Apply 1 CL")
              dailySteps.add(
                AiBridgeDayPlan(
                  dateIso = iso,
                  dayLabel = dayShort,
                  creditType = "CL",
                  description = "APPLY 1 CL (Casual Leave Bridge)",
                  isProtectedOpdDay = isOpdDay
                )
              )
            }
          }
        }
        cursor = cursor.plusDays(1)
      }

      if (creditsUsed < 0.5 || creditsUsed > 4.5 || freeDays < 2 || applyDates.isEmpty()) return null

      val mult = ((totalDays.toDouble() / creditsUsed) * 10.0).roundToInt() / 10.0
      if (mult < 1.8) return null

      val rhUsedCount = dailySteps.count { it.creditType == "RH" && !it.isAlreadyLogged }
      val colUsedCount = dailySteps.count { it.creditType == "COL" && !it.isAlreadyLogged }
      val fullClUsedCount = dailySteps.count { it.creditType == "CL" && !it.isAlreadyLogged }
      val halfClUsedCount = dailySteps.count { it.creditType == "HALF_CL" && !it.isAlreadyLogged }
      val isHalfBridge = halfClUsedCount > 0

      val mixParts = buildList {
        if (rhUsedCount > 0) add("$rhUsedCount RH")
        if (colUsedCount > 0) add("$colUsedCount COL")
        if (fullClUsedCount > 0) add("$fullClUsedCount CL")
        if (halfClUsedCount > 0) add("${halfClUsedCount * 0.5} CL (½d)")
        add("$freeDays Free GH/Weekend")
      }

      val distinctAnchors = anchorNames.distinct().take(2)
      val halfTag = if (isHalfBridge) " (½-Day Flex)" else ""
      val headlineFestival = when {
        start.monthValue == 12 && start.year == 2026 && end.year == 2027 ->
          "Christmas → New Year ${totalDays}-Day Grand Finale$halfTag"
        start.monthValue == 1 && start.dayOfMonth in 12..17 && start.year == 2027 ->
          "Pongal 2027 ${totalDays}-Day Harvest Megabridge$halfTag"
        distinctAnchors.isNotEmpty() ->
          "${distinctAnchors.joinToString(" & ")} ${totalDays}-Day Bridge$halfTag"
        else ->
          "${start.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)} ${start.year} ${totalDays}-Day Break$halfTag"
      }

      val startIso = start.format(isoFmt)
      val endIso = end.format(isoFmt)

      val actualCl26Left = (remCl2026 + effective.reserveClFloor).coerceAtMost(summary2026.clRemaining)
      val actualCl27Left = (remCl2027 + effective.reserveClFloor).coerceAtMost(summary2027?.clRemaining ?: 12.0)
      val actualColLeft = (unusedGhSorted.size - colUsedCount).coerceAtLeast(0)
      val balanceNote = if (end.year == 2026) {
        "2026 Left: ${formatHalf(actualCl26Left)} CL • $remRh2026 RH • $actualColLeft Free GH"
      } else {
        "2027 Left: ${formatHalf(actualCl27Left)} CL • $remRh2027 RH • $actualColLeft Free GH"
      }

      val urgencyTag = when {
        colUsedCount > 0 && minColDaysToExpiry != null && minColDaysToExpiry <= 45L ->
          "URGENT COL (${minColDaysToExpiry}d left)"
        colUsedCount > 0 && minColDaysToExpiry != null ->
          "FIFO COL (${minColDaysToExpiry}d validity)"
        start.year == 2026 && (fullClUsedCount + rhUsedCount > 0) ->
          "SAVES DEC 31 LAPSE"
        else -> ""
      }

      val opdNote = when {
        effective.protectedDaysOfWeek.isEmpty() && effective.protectedDatesIso.isEmpty() -> ""
        opdConflictCount == 0 -> "Duty/OPD-Safe: Zero protected clinical shifts touched"
        else -> "Duty/OPD Notice: Touches ${opdConflictDays.joinToString(", ")} — arrange cover"
      }

      val rationale = buildString {
        append("Unlocks $totalDays consecutive days off (${startIso} → ${endIso}) for ${formatHalf(creditsUsed)} credit(s) [${mult}x ROI]. ")
        if (colUsedCount > 0) {
          append("Consumes expiring GH-Worked credit FIFO before 180-day expiry. ")
        }
        if (effective.reserveClFloor > 0.0 && fullClUsedCount + halfClUsedCount > 0) {
          append("Protects your ≥${formatHalf(effective.reserveClFloor)} CL emergency reserve. ")
        } else if (rhUsedCount > 0 && fullClUsedCount == 0 && halfClUsedCount == 0) {
          append("Zero CL debit by leveraging notified TN Restricted Holiday.")
        } else if (start.year == 2026) {
          append("Prevents 2026 credits from lapsing on Dec 31.")
        }
      }.trim()

      return AiLeaveRecommendation(
        id = "db_opt_${startIso}_${endIso}${if (isHalfBridge) "_half" else ""}",
        title = headlineFestival,
        startDateIso = startIso,
        endDateIso = endIso,
        totalDaysOff = totalDays,
        leaveCreditsUsed = creditsUsed,
        freeDaysUnlocked = freeDays,
        efficiencyMultiplier = mult,
        exactDatesToApply = applyDates,
        creditMixSummary = mixParts.joinToString(" + "),
        projectedBalanceAfter = balanceNote,
        whyOptimalForHod = rationale,
        tnRuleComplianceCheck = "Verified ≤10d Continuous Stretch ($trueCombinedStretch d incl. ledger adjacency) • 0 debit for $freeDays Weekend/GH days.",
        dailyPlan = dailySteps,
        expiryUrgencyTag = urgencyTag,
        opdImpactNote = opdNote,
        hasOpdConflict = opdConflictCount > 0,
        isHalfDayBridge = isHalfBridge,
        verificationStatus = "VERIFIED_RULES"
      )
    }

    fun buildOptimalElBridge(monStart: LocalDate, weeks: Int = 1): AiLeaveRecommendation? {
      if (!effective.allowElBridges) return null
      val friEnd = monStart.plusDays((weeks * 7L) - 3L) // Mon..Fri = 5d for 1 wk
      // Snap prefix & suffix free days OUTSIDE the EL spell (Rule 8!)
      var visualStart = monStart
      while (visualStart.isAfter(horizonStart) && isFreeCalendarDay(visualStart.minusDays(1))) {
        visualStart = visualStart.minusDays(1)
      }
      var visualEnd = friEnd
      while (visualEnd.isBefore(horizonEnd) && isFreeCalendarDay(visualEnd.plusDays(1))) {
        visualEnd = visualEnd.plusDays(1)
      }
      if (visualStart.isBefore(horizonStart) || visualEnd.isAfter(horizonEnd)) return null

      val (safeAdj, _) = checkAdjacencyAndCollision(visualStart, visualEnd, isElSpell = true)
      if (!safeAdj) return null

      val totalDays = ChronoUnit.DAYS.between(visualStart, visualEnd).toInt() + 1
      val dailySteps = mutableListOf<AiBridgeDayPlan>()
      val applyDates = mutableListOf<String>()
      var elDebited = 0
      var freeDays = 0
      var opdConflictCount = 0
      val opdConflictDays = mutableListOf<String>()

      var d = visualStart
      while (!d.isAfter(visualEnd)) {
        val iso = d.format(isoFmt)
        val dayShort = "${d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)}, ${
          d.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
        } ${d.dayOfMonth}"
        val ghItem = ghMap[iso]
        val isPrefOrSuf = d.isBefore(monStart) || d.isAfter(friEnd)
        val isOpdDay = d.dayOfWeek in effective.protectedDaysOfWeek ||
          iso in effective.protectedDatesIso
        when {
          isPrefOrSuf && ghItem != null -> {
            freeDays++
            dailySteps.add(AiBridgeDayPlan(iso, dayShort, "GH", "FREE Prefix/Suffix GH: ${ghItem.nameEnglish}"))
          }
          isPrefOrSuf -> {
            freeDays++
            dailySteps.add(AiBridgeDayPlan(iso, dayShort, "WEEKEND", "FREE Prefix/Suffix Weekend (0 EL Debit)"))
          }
          else -> {
            // Inside [monStart..friEnd] — Rule 8 debits every day inside EL bounds
            elDebited++
            if (isOpdDay) {
              opdConflictCount++
              opdConflictDays.add(dayShort)
            }
            val note = if (ghItem != null) "APPLY 1 EL (Intervening GH inside spell)" else "APPLY 1 EL (Rule 8)"
            applyDates.add("$iso — Apply 1 EL")
            dailySteps.add(
              AiBridgeDayPlan(
                dateIso = iso,
                dayLabel = dayShort,
                creditType = "EL",
                description = note,
                isProtectedOpdDay = isOpdDay
              )
            )
          }
        }
        d = d.plusDays(1)
      }

      if (elDebited <= 0 || freeDays < 4) return null
      val mult = ((totalDays.toDouble() / elDebited) * 10.0).roundToInt() / 10.0
      val startIso = visualStart.format(isoFmt)
      val endIso = visualEnd.format(isoFmt)
      val opdNote = when {
        effective.protectedDaysOfWeek.isEmpty() && effective.protectedDatesIso.isEmpty() -> ""
        opdConflictCount == 0 -> "Duty/OPD-Safe: Zero protected clinical shifts touched"
        else -> "Duty/OPD Notice: Touches ${opdConflictDays.joinToString(", ")} — arrange cover"
      }

      return AiLeaveRecommendation(
        id = "db_el_${startIso}_$endIso",
        title = "Rule-8 Optimal EL ${totalDays}-Day Break (${monStart.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)})",
        startDateIso = startIso,
        endDateIso = endIso,
        totalDaysOff = totalDays,
        leaveCreditsUsed = elDebited.toDouble(),
        freeDaysUnlocked = freeDays,
        efficiencyMultiplier = mult,
        exactDatesToApply = applyDates,
        creditMixSummary = "$elDebited EL + $freeDays Free Prefixed/Suffixed Days",
        projectedBalanceAfter = "0 CL/RH used — preserves full CL emergency reserve",
        whyOptimalForHod = "Starts Monday & ends Friday so $freeDays weekend/GH days are prefixed & suffixed FREE under TN Leave Rule 8 (zero trapped weekend debits).",
        tnRuleComplianceCheck = "TN Leave Rules 1933 Rule 8 Compliant: Zero weekends trapped inside EL boundaries; not combined with CL.",
        dailyPlan = dailySteps,
        expiryUrgencyTag = "0 CL USED • EL BOUNDARY OPTIMAL",
        opdImpactNote = opdNote,
        hasOpdConflict = opdConflictCount > 0,
        ltcOdSynergyNote = "Eligible to pair with 2024–2027 LTC Block & up to 10d EL Encashment.",
        isElBridge = true,
        verificationStatus = "VERIFIED_RULES"
      )
    }

    // Collect upcoming GH/RH festival anchors
    val upcomingAnchors = (allGh + allRh)
      .mapNotNull { runCatching { LocalDate.parse(it.dateIso, isoFmt) }.getOrNull() }
      .filter { !it.isBefore(horizonStart) && !it.isAfter(horizonEnd) }
      .distinct()
      .sorted()

    val candidateWindows = linkedSetOf<Pair<LocalDate, LocalDate>>()
    for (anchor in upcomingAnchors) {
      for (back in 0..4) {
        for (fwd in 0..5) {
          var s = anchor.minusDays(back.toLong())
          var e = anchor.plusDays(fwd.toLong())
          while (s.isAfter(horizonStart) && isFreeCalendarDay(s.minusDays(1))) {
            s = s.minusDays(1)
          }
          while (e.isBefore(horizonEnd) && isFreeCalendarDay(e.plusDays(1))) {
            e = e.plusDays(1)
          }
          val len = ChronoUnit.DAYS.between(s, e).toInt() + 1
          if (len in 3..10 && !s.isBefore(horizonStart) && !e.isAfter(horizonEnd) &&
            (isFestivalDay(s) || isFreeCalendarDay(s) || isFestivalDay(e) || isFreeCalendarDay(e))
          ) {
            candidateWindows.add(s to e)
          }
        }
      }
    }

    // Also evaluate pure Fri+Mon / Thu+Fri weekend-adjacent slots when FIFO COL or Dec 31 burn-down needs options
    var weekCursor = horizonStart
    while (!weekCursor.isAfter(horizonEnd.minusDays(3))) {
      if (weekCursor.dayOfWeek == DayOfWeek.FRIDAY) {
        var s = weekCursor
        while (s.isAfter(horizonStart) && isFreeCalendarDay(s.minusDays(1))) s = s.minusDays(1)
        var e = weekCursor.plusDays(2) // Sun
        while (e.isBefore(horizonEnd) && isFreeCalendarDay(e.plusDays(1))) e = e.plusDays(1)
        if (!s.isBefore(horizonStart) && !e.isAfter(horizonEnd)) {
          candidateWindows.add(s to e)
        }
      }
      weekCursor = weekCursor.plusDays(1)
    }

    val builtCandidates = mutableListOf<AiLeaveRecommendation>()
    candidateWindows.forEach { (s, e) ->
      buildDynamicWindow(s, e, halfDayTargetIso = null)?.let { builtCandidates.add(it) }
      if (effective.allowHalfDayCl && !preferZeroCl) {
        if (!isFreeCalendarDay(s)) {
          buildDynamicWindow(s, e, halfDayTargetIso = s.format(isoFmt))?.let { builtCandidates.add(it) }
        }
        if (!isFreeCalendarDay(e) && e != s) {
          buildDynamicWindow(s, e, halfDayTargetIso = e.format(isoFmt))?.let { builtCandidates.add(it) }
        }
      }
    }

    // Add EL Rule-8 Mon..Fri candidates when enabled
    if (effective.allowElBridges) {
      var monCursor = horizonStart
      while (monCursor.dayOfWeek != DayOfWeek.MONDAY && !monCursor.isAfter(horizonEnd)) {
        monCursor = monCursor.plusDays(1)
      }
      var elAdded = 0
      while (!monCursor.isAfter(horizonEnd.minusDays(6)) && elAdded < 10) {
        val nextWeekMon = monCursor
        val adjacentHolidayBoost = isFreeCalendarDay(nextWeekMon.minusDays(3)) ||
          isFreeCalendarDay(nextWeekMon.plusDays(5)) ||
          ghMap.containsKey(nextWeekMon.format(isoFmt)) ||
          elAdded < 3
        if (adjacentHolidayBoost) {
          buildOptimalElBridge(nextWeekMon, weeks = 1)?.let {
            builtCandidates.add(it)
            elAdded++
          }
        }
        monCursor = monCursor.plusWeeks(2)
      }
    }

    fun scoreRec(rec: AiLeaveRecommendation): Double {
      val colCount = rec.dailyPlan.count { it.creditType == "COL" && !it.isAlreadyLogged }
      val clCount = rec.dailyPlan.count { (it.creditType == "CL" || it.creditType == "HALF_CL") && !it.isAlreadyLogged }
      val rhCount = rec.dailyPlan.count { it.creditType == "RH" && !it.isAlreadyLogged }
      val recStart = runCatching { LocalDate.parse(rec.startDateIso, isoFmt) }.getOrDefault(today)
      val daysAway = ChronoUnit.DAYS.between(today, recStart).coerceAtLeast(0L)
      val recencyBoost = when {
        daysAway <= 30 -> 1.8
        daysAway <= 60 -> 1.3
        daysAway <= 120 -> 0.75
        else -> 0.15
      }
      val year2026LapseBoost = if (recStart.year == 2026 && (clCount > 0 || rhCount > 0)) 2.1 else 0.0
      val opdPenalty = if (rec.hasOpdConflict) -2.4 else 0.75
      val elBonus = if (rec.isElBridge && effective.allowElBridges) 2.5 else 0.0
      val colUrgencyBonus = if (rec.expiryUrgencyTag.contains("URGENT COL")) 3.5 else 0.0

      return when {
        preferFifoCol ->
          (colCount * 4.8) + colUrgencyBonus + (rec.efficiencyMultiplier * 1.4) + recencyBoost + opdPenalty + elBonus
        preferZeroCl ->
          (if (clCount == 0) 6.0 else -6.0) + (rhCount * 1.4) + (colCount * 1.4) + (rec.efficiencyMultiplier * 1.5) + recencyBoost + opdPenalty
        preferMegabridges ->
          (rec.totalDaysOff * 1.65) + rec.efficiencyMultiplier + (if (rec.totalDaysOff >= 5) 3.2 else 0.0) + recencyBoost + year2026LapseBoost + opdPenalty + elBonus
        else ->
          (rec.efficiencyMultiplier * 1.9) + (rec.totalDaysOff * 0.45) + recencyBoost + year2026LapseBoost + colUrgencyBonus + opdPenalty + elBonus - (clCount * 0.15)
      }
    }

    val ranked = builtCandidates.sortedWith(
      compareByDescending<AiLeaveRecommendation> { scoreRec(it) }
        .thenByDescending { it.efficiencyMultiplier }
        .thenByDescending { it.totalDaysOff }
        .thenBy { it.startDateIso }
    )

    // Pick up to 5 diverse recommendations and run sequential portfolio credit allocation
    val selected = mutableListOf<AiLeaveRecommendation>()
    for (cand in ranked) {
      if (selected.size >= 5) break
      val candStart = LocalDate.parse(cand.startDateIso, isoFmt)
      val candEnd = LocalDate.parse(cand.endDateIso, isoFmt)
      val overlapsHeavily = selected.any { existing ->
        val exStart = LocalDate.parse(existing.startDateIso, isoFmt)
        val exEnd = LocalDate.parse(existing.endDateIso, isoFmt)
        val overlapDays = maxOf(
          0L,
          minOf(candEnd.toEpochDay(), exEnd.toEpochDay()) - maxOf(candStart.toEpochDay(), exStart.toEpochDay()) + 1L
        )
        overlapDays >= 2L
      }
      val sameMonthCount = selected.count { it.startDateIso.take(7) == cand.startDateIso.take(7) }
      if (!overlapsHeavily && sameMonthCount < 2) {
        selected.add(cand)
      }
    }

    return annotatePortfolioBalances(
      plans = selected.ifEmpty { ranked.take(5) },
      summary2026 = summary2026,
      summary2027 = summary2027,
      colGhAuditAllYears = colGhAuditAllYears,
      constraints = effective
    )
  }

  private fun annotatePortfolioBalances(
    plans: List<AiLeaveRecommendation>,
    summary2026: YearCreditSummary,
    summary2027: YearCreditSummary?,
    colGhAuditAllYears: ColGhAuditSummary,
    constraints: OptimizerConstraints
  ): List<AiLeaveRecommendation> {
    val running = mutableListOf<AiLeaveRecommendation>()
    return plans.map { plan ->
      running.add(plan)
      val impact = computePortfolioImpact(
        selectedRecs = running,
        summary2026 = summary2026,
        summary2027 = summary2027,
        colGhAuditAllYears = colGhAuditAllYears,
        constraints = constraints
      )
      val cumulativeNote = if (running.size == 1) {
        "If #1 booked → '26 Left: ${formatHalf(impact.remainingCl2026)} CL • ${impact.remainingRh2026} RH • ${impact.remainingColPool} GH"
      } else {
        "Cumulative #1–#${running.size} (${impact.totalDaysOffUnlocked}d off) → '26 Left: ${formatHalf(impact.remainingCl2026)} CL • ${impact.remainingRh2026} RH • '27 Left: ${formatHalf(impact.remainingCl2027)} CL • ${impact.remainingColPool} GH"
      }
      plan.copy(portfolioBalanceAfter = cumulativeNote)
    }
  }

  suspend fun generateOptimalRecommendations(
    focusGoal: String,
    summary2026: YearCreditSummary,
    colGhAuditAllYears: ColGhAuditSummary,
    records: List<LeaveRecord>,
    summary2027: YearCreditSummary? = null,
    constraints: OptimizerConstraints = OptimizerConstraints(),
    officerContextNote: String = "HOD, Department of Psychiatry, Government Erode Medical College & Hospital"
  ): Result<AiRecommendationUiState.Success> {
    val today = LocalDate.now().let { if (it.year < 2026) LocalDate.of(2026, 11, 26) else it }
    val todayIso = today.format(isoFmt)
    val effective = parseNaturalLanguageConstraints(constraints, "$focusGoal ${constraints.customNaturalPrompt}")

    val allGh = TnHolidays2027Catalog.allSyncedPublicHolidays()
    val allRh = TnHolidays2027Catalog.allSyncedRestrictedHolidays()
    val allHolidaysCount = allGh.size + allRh.size

    val deterministicCandidates = scanHolidayDatabaseForLongWeekends(
      summary2026 = summary2026,
      colGhAuditAllYears = colGhAuditAllYears,
      records = records,
      focusGoal = focusGoal,
      summary2027 = summary2027,
      referenceDate = today,
      constraints = effective
    )

    return try {
      val unusedGhDates = colGhAuditAllYears.unusedGhRecords
        .sortedBy { it.dateIso }
        .joinToString(", ") { gh ->
          val ghLocal = runCatching { LocalDate.parse(gh.dateIso, isoFmt) }.getOrNull()
          val expDate = ghLocal?.plusDays(180)?.format(isoFmt) ?: "6m"
          "${gh.dateIso} (Expires $expDate • ${gh.notes.ifBlank { "Hospital GH Duty" }})"
        }.ifBlank { "None currently unclaimed" }

      val upcomingLoggedLeaves = records
        .filter { it.dateIso >= todayIso }
        .sortedBy { it.dateIso }
        .joinToString("; ") { "${it.dateIso}: ${it.category.code} (${it.notes})" }
        .ifBlank { "None logged yet for upcoming dates" }

      val upcomingGhText = allGh
        .filter { it.dateIso >= todayIso }
        .joinToString("\n") {
          "- ${it.dateIso} (${it.dayOfWeek}): ${it.nameEnglish} / ${it.nameTamil} [TN Public Holiday / GH]"
        }

      val upcomingRhText = allRh
        .filter { it.dateIso >= todayIso }
        .joinToString("\n") {
          "- ${it.dateIso} (${it.dayOfWeek}): ${it.nameEnglish} / ${it.nameTamil} [Official TN RH]"
        }

      val preScannedText = deterministicCandidates.joinToString("\n") { cand ->
        "* ${cand.title} (${cand.startDateIso} to ${cand.endDateIso}): ${cand.totalDaysOff}d off for ${cand.leaveCreditsUsed} credit(s) [${cand.efficiencyMultiplier}x]. Mix: ${cand.creditMixSummary}. Apply: ${cand.exactDatesToApply.joinToString("; ")}"
      }

      val remCl27 = summary2027?.clRemaining ?: 12.0
      val remRh27 = summary2027?.rhRemaining ?: 3
      val opdDaysStr = if (effective.protectedDaysOfWeek.isEmpty()) {
        "None specified"
      } else {
        effective.protectedDaysOfWeek.sortedBy { it.value }.joinToString(", ") {
          it.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        }
      }

      val prompt = """
        Today's date is $todayIso.
        Officer Profile: $officerContextNote.
        Analyze the synced TN Government Holiday DB ($todayIso through 2027-12-31) alongside the deterministic pre-verified candidates and constraints below to produce 4 to 5 optimal, non-conflicting leave plans.

        SELECTED OPTIMIZATION STRATEGY & CONSTRAINTS:
        - Strategy Focus: "$focusGoal"
        - Custom User Ask: "${effective.customNaturalPrompt.ifBlank { "None" }}"
        - Emergency CL Safety Reserve Floor: Keep ≥ ${formatHalf(effective.reserveClFloor)} CL untouched
        - Strict Zero-CL Mode: ${effective.strictZeroCl}
        - Allow Earned Leave (EL Rule 8 Prefix/Suffix): ${effective.allowElBridges}
        - Allow Half-Day 0.5 CL (FN/AN): ${effective.allowHalfDayCl}
        - Protected OPD / Clinical Duty Weekdays (prefer avoiding): $opdDaysStr
        - Horizon Window: ${effective.horizonMode.label}

        LIVE REMAINING LEAVE CREDITS:
        - 2026 CL Remaining: ${summary2026.clRemaining} / ${summary2026.clQuota} (lapses Dec 31, 2026)
        - 2026 RH Remaining: ${summary2026.rhRemaining} / ${summary2026.rhQuota} (lapses Dec 31, 2026)
        - 2027 CL Remaining: $remCl27 / 12 | 2027 RH Remaining: $remRh27 / 3
        - Unused GH Worked Credits Ready for COL (FIFO, 180-day expiry): ${colGhAuditAllYears.availableUnusedGhCount} ($unusedGhDates)

        UPCOMING LEAVES ALREADY LOGGED BY OFFICER:
        $upcomingLoggedLeaves

        VERIFIED ALGORITHMIC CANDIDATES FROM SOLVER:
        $preScannedText

        UPCOMING SYNCED TN GOVERNMENT PUBLIC HOLIDAYS (GH):
        $upcomingGhText

        UPCOMING SYNCED TN RESTRICTED HOLIDAYS (RH / RL):
        $upcomingRhText

        INSTRUCTIONS:
        1. Prefer the verified algorithmic candidates when valid, or refine credit pairings strictly obeying TN Leave Rules 1933 & FR Annexure VII.
        2. Every recommended date must be >= $todayIso and respect the ${formatHalf(effective.reserveClFloor)} CL reserve floor.
        3. Use exact creditType values from ["WEEKEND", "GH", "RH", "CL", "HALF_CL", "COL", "EL"].
        4. Format `exactDatesToApply` items as `YYYY-MM-DD — Apply 1 <RH|COL|CL|EL> (<Reason>)` or `YYYY-MM-DD — Apply 0.5 CL (<Session>)`.
        5. Explain clearly in `executiveSummary` and `whyOptimalForHod` how the sequence preserves CL reserves, respects OPD days, and avoids Dec 31 / 180-day COL expiry.
      """.trimIndent()

      val responseText = runGeminiWithResilientTimeout(prompt)

      val rawParsed = parseStructuredRecommendations(
        focusGoal = focusGoal,
        jsonString = responseText,
        analyzedHolidayCount = allHolidaysCount
      )

      val validated = validateAndRepairGeminiRecommendations(
        raw = rawParsed,
        deterministicFallback = deterministicCandidates,
        summary2026 = summary2026,
        summary2027 = summary2027,
        colGhAuditAllYears = colGhAuditAllYears,
        records = records,
        today = today,
        constraints = effective
      )
      Result.success(validated)
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "AI Leave Optimizer timed out", e)
      Result.failure(
        IllegalStateException("Gemini AI timed out after waiting for a response. Check network connectivity and tap Retry.")
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "AI Leave Optimizer generation failed", e)
      val cleanMsg = e.localizedMessage?.takeIf { it.isNotBlank() } ?: e::class.java.simpleName
      Result.failure(IllegalStateException("Gemini AI error: $cleanMsg"))
    }
  }

  /**
   * Deterministic post-validation gate over Gemini AI output:
   * - Filters out hallucinated past dates or invalid RL dates
   * - Re-verifies 10-day adjacency stretch and EL/CL non-combination against [records]
   * - Recomputes exact credit debits, free days, efficiency multiplier, OPD conflict flags,
   *   and sequential portfolio balances; merges verified fallback plans if any Gemini card failed validation.
   */
  private fun validateAndRepairGeminiRecommendations(
    raw: AiRecommendationUiState.Success,
    deterministicFallback: List<AiLeaveRecommendation>,
    summary2026: YearCreditSummary,
    summary2027: YearCreditSummary?,
    colGhAuditAllYears: ColGhAuditSummary,
    records: List<LeaveRecord>,
    today: LocalDate,
    constraints: OptimizerConstraints
  ): AiRecommendationUiState.Success {
    val ghMap = TnHolidays2027Catalog.allSyncedPublicHolidays().associateBy { it.dateIso }
    val rhMap = TnHolidays2027Catalog.allSyncedRestrictedHolidays().associateBy { it.dateIso }
    val existingByDate = records.groupBy { it.dateIso }
    val unusedGhSorted = colGhAuditAllYears.unusedGhRecords.sortedBy { it.dateIso }

    val validatedList = raw.recommendations.mapNotNull { rec ->
      val start = runCatching { LocalDate.parse(rec.startDateIso, isoFmt) }.getOrNull() ?: return@mapNotNull null
      val end = runCatching { LocalDate.parse(rec.endDateIso, isoFmt) }.getOrNull() ?: return@mapNotNull null
      if (start.isBefore(today) || end.isBefore(start)) return@mapNotNull null

      val isElPlan = rec.dailyPlan.any { it.creditType.equals("EL", ignoreCase = true) }
      val totalDays = ChronoUnit.DAYS.between(start, end).toInt() + 1
      if (!isElPlan && constraints.enforce10DayCap && totalDays > 10) return@mapNotNull null

      // Validate day steps deterministically
      var freeDays = 0
      var creditsDebited = 0.0
      var opdConflicts = 0
      val opdConflictLabels = mutableListOf<String>()
      val verifiedSteps = mutableListOf<AiBridgeDayPlan>()
      val repairedApplyLines = mutableListOf<String>()

      rec.dailyPlan.forEach { step ->
        val d = runCatching { LocalDate.parse(step.dateIso, isoFmt) }.getOrNull() ?: return@forEach
        val iso = d.format(isoFmt)
        val gh = ghMap[iso]
        val rh = rhMap[iso]
        val isWknd = d.dayOfWeek == DayOfWeek.SUNDAY ||
          (constraints.includeSaturdays && d.dayOfWeek == DayOfWeek.SATURDAY)
        val isOpd = d.dayOfWeek in constraints.protectedDaysOfWeek ||
          iso in constraints.protectedDatesIso
        val loggedRecs = existingByDate[iso].orEmpty()
        val alreadyLogged = loggedRecs.any {
          it.category in setOf(LeaveCategory.CL, LeaveCategory.RH, LeaveCategory.COL, LeaveCategory.PL, LeaveCategory.EL)
        }

        val requestedType = step.creditType.uppercase(Locale.US)
        val resolvedType = when {
          gh != null && requestedType != "EL" -> "GH"
          isWknd && requestedType != "EL" -> "WEEKEND"
          requestedType == "RH" && rh == null -> {
            // Gemini put RH on a non-RL date — safely repair to COL if available, else CL
            val validCol = unusedGhSorted.any { g ->
              val gd = runCatching { LocalDate.parse(g.dateIso, isoFmt) }.getOrNull()
              gd != null && ChronoUnit.DAYS.between(gd, d) in 0..180
            }
            if (validCol) "COL" else "CL"
          }
          else -> requestedType
        }

        val isHalf = resolvedType == "HALF_CL" || step.isHalfDay || step.description.contains("0.5", ignoreCase = true)
        when (resolvedType) {
          "GH", "WEEKEND" -> freeDays++
          "HALF_CL" -> if (!alreadyLogged) creditsDebited += 0.5
          "CL", "RH", "COL", "EL" -> if (!alreadyLogged) creditsDebited += if (isHalf) 0.5 else 1.0
        }

        if (resolvedType !in setOf("GH", "WEEKEND") && !alreadyLogged) {
          if (isOpd) {
            opdConflicts++
            opdConflictLabels.add(step.dayLabel)
          }
          val unitStr = if (isHalf) "0.5 CL" else "1 $resolvedType"
          val descHint = rh?.nameEnglish?.substringBefore(" (") ?: step.description.take(28)
          repairedApplyLines.add("$iso — Apply $unitStr ($descHint)")
        }

        verifiedSteps.add(
          step.copy(
            creditType = resolvedType,
            isHalfDay = isHalf,
            isAlreadyLogged = alreadyLogged,
            isProtectedOpdDay = isOpd && resolvedType !in setOf("GH", "WEEKEND")
          )
        )
      }

      if (verifiedSteps.isEmpty() || creditsDebited <= 0.0) return@mapNotNull null

      // Check TN Rule 10-day adjacency against existing ledger
      if (!isElPlan && constraints.enforce10DayCap) {
        val adjBefore = (1..6).takeWhile { off ->
          val p = start.minusDays(off.toLong())
          val pIso = p.format(isoFmt)
          ghMap.containsKey(pIso) || p.dayOfWeek == DayOfWeek.SUNDAY ||
            (constraints.includeSaturdays && p.dayOfWeek == DayOfWeek.SATURDAY) ||
            existingByDate[pIso]?.any { it.category in setOf(LeaveCategory.CL, LeaveCategory.RH, LeaveCategory.COL) } == true
        }.size
        val adjAfter = (1..6).takeWhile { off ->
          val n = end.plusDays(off.toLong())
          val nIso = n.format(isoFmt)
          ghMap.containsKey(nIso) || n.dayOfWeek == DayOfWeek.SUNDAY ||
            (constraints.includeSaturdays && n.dayOfWeek == DayOfWeek.SATURDAY) ||
            existingByDate[nIso]?.any { it.category in setOf(LeaveCategory.CL, LeaveCategory.RH, LeaveCategory.COL) } == true
        }.size
        if (totalDays + adjBefore + adjAfter > 10) return@mapNotNull null
      }

      val mult = ((totalDays / creditsDebited) * 10.0).roundToInt() / 10.0
      val opdNote = when {
        constraints.protectedDaysOfWeek.isEmpty() && constraints.protectedDatesIso.isEmpty() -> ""
        opdConflicts == 0 -> "Duty/OPD-Safe: Zero protected clinical shifts touched"
        else -> "Duty/OPD Notice: Touches ${opdConflictLabels.joinToString(", ")} — arrange cover"
      }
      val hasCol = verifiedSteps.any { it.creditType == "COL" && !it.isAlreadyLogged }
      val urgency = when {
        hasCol -> "FIFO COL TRACKED"
        start.year == 2026 -> "SAVES DEC 31 LAPSE"
        else -> ""
      }

      rec.copy(
        totalDaysOff = totalDays,
        leaveCreditsUsed = creditsDebited,
        freeDaysUnlocked = freeDays,
        efficiencyMultiplier = mult,
        exactDatesToApply = repairedApplyLines.ifEmpty { rec.exactDatesToApply },
        dailyPlan = verifiedSteps,
        opdImpactNote = opdNote,
        hasOpdConflict = opdConflicts > 0,
        expiryUrgencyTag = urgency,
        isHalfDayBridge = verifiedSteps.any { it.isHalfDay },
        isElBridge = isElPlan,
        ltcOdSynergyNote = if (isElPlan) "Eligible for 2024–27 LTC & 10d EL Encashment" else "",
        verificationStatus = "GEMINI + TN RULES VERIFIED"
      )
    }.toMutableList()

    // Backfill with deterministic solver picks if any Gemini pick failed statutory verification
    for (fb in deterministicFallback) {
      if (validatedList.size >= 4) break
      val overlaps = validatedList.any { it.startDateIso == fb.startDateIso && it.endDateIso == fb.endDateIso }
      if (!overlaps) validatedList.add(fb)
    }

    val annotated = annotatePortfolioBalances(
      plans = validatedList.take(5),
      summary2026 = summary2026,
      summary2027 = summary2027,
      colGhAuditAllYears = colGhAuditAllYears,
      constraints = constraints
    )

    return raw.copy(
      recommendations = annotated,
      appliedConstraintsSummary = constraints.buildStrategySummary("")
    )
  }

  private suspend fun runGeminiWithResilientTimeout(prompt: String): String {
    return try {
      withTimeout(50_000L) {
        val response = primaryGenerativeModel.generateContent(prompt)
        response.text?.takeIf { it.isNotBlank() }
          ?: throw IllegalStateException("Empty structured response from gemini-3.8-flash.")
      }
    } catch (e: CancellationException) {
      if (e !is TimeoutCancellationException) throw e
      Log.w(TAG, "Primary gemini-3.8-flash timed out; attempting gemini-3.5-flash-lite fallback", e)
      withTimeout(30_000L) {
        val fallbackResp = liteFallbackGenerativeModel.generateContent(prompt)
        fallbackResp.text?.takeIf { it.isNotBlank() }
          ?: throw e
      }
    } catch (primaryErr: Exception) {
      Log.w(TAG, "Primary gemini-3.8-flash failed (${primaryErr.localizedMessage}); trying gemini-3.5-flash-lite", primaryErr)
      withTimeout(30_000L) {
        val fallbackResp = liteFallbackGenerativeModel.generateContent(prompt)
        fallbackResp.text?.takeIf { it.isNotBlank() }
          ?: throw primaryErr
      }
    }
  }

  private fun parseStructuredRecommendations(
    focusGoal: String,
    jsonString: String,
    analyzedHolidayCount: Int
  ): AiRecommendationUiState.Success {
    val trimmed = jsonString.trim()
    val cleanJson = if (trimmed.startsWith("{")) {
      trimmed
    } else {
      val firstBrace = trimmed.indexOf('{')
      val lastBrace = trimmed.lastIndexOf('}')
      if (firstBrace >= 0 && lastBrace > firstBrace) {
        trimmed.substring(firstBrace, lastBrace + 1)
      } else {
        trimmed
      }
    }

    val root = JSONObject(cleanJson)
    val executiveSummary = root.optString(
      "executiveSummary",
      "Optimized long-weekend & leave bridges verified under Tamil Nadu Leave Rules 1933 & FR Annexure VII."
    ).ifBlank {
      "Optimized long-weekend & leave bridges verified under Tamil Nadu Leave Rules 1933 & FR Annexure VII."
    }

    val recArray = root.optJSONArray("recommendations")
    val list = mutableListOf<AiLeaveRecommendation>()

    if (recArray != null) {
      for (i in 0 until recArray.length()) {
        val item = recArray.optJSONObject(i) ?: continue
        val rawStartIso = item.optString("startDateIso", "").trim()
        val rawEndIso = item.optString("endDateIso", "").trim()
        val parsedStart = runCatching { LocalDate.parse(rawStartIso, isoFmt) }.getOrNull()

        val dailyArray = item.optJSONArray("dailyPlan")
        val dailySteps = mutableListOf<AiBridgeDayPlan>()
        if (dailyArray != null) {
          for (j in 0 until dailyArray.length()) {
            val stepObj = dailyArray.optJSONObject(j) ?: continue
            val stepDateRaw = stepObj.optString("dateIso", "").trim()
            val resolvedDateIso = if (isoDateRegex.matches(stepDateRaw)) {
              stepDateRaw
            } else if (parsedStart != null) {
              parsedStart.plusDays(j.toLong()).format(isoFmt)
            } else {
              ""
            }
            if (resolvedDateIso.isEmpty()) continue

            val parsedStepDate = runCatching { LocalDate.parse(resolvedDateIso, isoFmt) }.getOrNull()
            val fallbackDayLabel = parsedStepDate?.let { d ->
              "${d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)}, ${
                d.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
              } ${d.dayOfMonth}"
            } ?: resolvedDateIso

            val rawCredit = stepObj.optString("creditType", "CL").trim().uppercase(Locale.US)
            val desc = stepObj.optString("description", rawCredit)
            val normalizedCredit = when {
              rawCredit.contains("WEEK") -> "WEEKEND"
              rawCredit == "GH" || rawCredit.contains("PUBLIC") -> "GH"
              rawCredit == "RH" || rawCredit.contains("RESTRICT") -> "RH"
              rawCredit == "COL" || rawCredit.contains("COMP") -> "COL"
              rawCredit == "EL" || rawCredit.contains("EARNED") -> "EL"
              rawCredit == "HALF_CL" || desc.contains("0.5", ignoreCase = true) -> "HALF_CL"
              else -> "CL"
            }

            dailySteps.add(
              AiBridgeDayPlan(
                dateIso = resolvedDateIso,
                dayLabel = stepObj.optString("dayLabel", fallbackDayLabel).ifBlank { fallbackDayLabel },
                creditType = normalizedCredit,
                description = desc,
                isHalfDay = normalizedCredit == "HALF_CL"
              )
            )
          }
        }

        val startIso = if (isoDateRegex.matches(rawStartIso)) {
          rawStartIso
        } else {
          dailySteps.firstOrNull()?.dateIso ?: continue
        }
        val endIso = if (isoDateRegex.matches(rawEndIso)) {
          rawEndIso
        } else {
          dailySteps.lastOrNull()?.dateIso ?: startIso
        }

        val applyArray = item.optJSONArray("exactDatesToApply")
        val exactApplyList = mutableListOf<String>()
        if (applyArray != null) {
          for (k in 0 until applyArray.length()) {
            val s = applyArray.optString(k, "").trim()
            if (s.isNotEmpty()) exactApplyList.add(s)
          }
        }
        if (exactApplyList.isEmpty()) {
          dailySteps
            .filter { it.creditType in setOf("RH", "COL", "CL", "HALF_CL", "EL") }
            .forEach { step ->
              val unit = if (step.isHalfDay) "0.5 CL" else "1 ${step.creditType}"
              exactApplyList.add("${step.dateIso} — Apply $unit")
            }
        }

        val actionCredits = dailySteps.sumOf {
          when (it.creditType) {
            "HALF_CL" -> 0.5
            "RH", "COL", "CL", "EL" -> if (it.isHalfDay) 0.5 else 1.0
            else -> 0.0
          }
        }
        val totalDays = item.optInt("totalDaysOff", dailySteps.size.coerceAtLeast(1)).coerceIn(1, 30)
        val rawCredits = item.optDouble("leaveCreditsUsed", actionCredits.coerceAtLeast(0.5))
        val creditsUsed = if (rawCredits.isNaN() || rawCredits <= 0.0) {
          actionCredits.coerceAtLeast(0.5)
        } else {
          rawCredits
        }
        val freeUnlocked = item.optInt(
          "freeDaysUnlocked",
          dailySteps.count { it.creditType == "GH" || it.creditType == "WEEKEND" }
        ).coerceAtLeast(0)
        val rawMult = item.optDouble("efficiencyMultiplier", (totalDays / creditsUsed))
        val multiplier = if (rawMult.isNaN() || rawMult <= 0.0) {
          ((totalDays / creditsUsed) * 10.0).roundToInt() / 10.0
        } else {
          (rawMult * 10.0).roundToInt() / 10.0
        }

        list.add(
          AiLeaveRecommendation(
            id = item.optString("id", "ai_rec_$i").ifBlank { "ai_rec_$i" },
            title = item.optString("title", "Optimal Long-Weekend Bridge #${i + 1}"),
            startDateIso = startIso,
            endDateIso = endIso,
            totalDaysOff = totalDays,
            leaveCreditsUsed = creditsUsed,
            freeDaysUnlocked = freeUnlocked,
            efficiencyMultiplier = multiplier,
            exactDatesToApply = exactApplyList,
            creditMixSummary = item.optString("creditMixSummary", "${formatHalf(creditsUsed)} Credits + $freeUnlocked Free Days"),
            projectedBalanceAfter = item.optString("projectedBalanceAfter", ""),
            whyOptimalForHod = item.optString("whyOptimalForHod", ""),
            tnRuleComplianceCheck = item.optString(
              "tnRuleComplianceCheck",
              "Compliant with TN Leave Rules 1933 & FR Annexure VII."
            ),
            dailyPlan = dailySteps
          )
        )
      }
    }

    if (list.isEmpty()) {
      throw IllegalStateException("Gemini AI returned 0 valid bridge recommendations. Please retry.")
    }

    return AiRecommendationUiState.Success(
      focusGoal = focusGoal,
      executiveSummary = executiveSummary,
      recommendations = list,
      analyzedHolidayCount = analyzedHolidayCount
    )
  }

  private fun formatHalf(v: Double): String =
    if (v % 1.0 == 0.0) v.toInt().toString() else String.format(Locale.US, "%.1f", v)
}
