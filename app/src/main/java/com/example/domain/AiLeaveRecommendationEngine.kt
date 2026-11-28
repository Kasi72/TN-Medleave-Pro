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
  val isProtectedOpdDay: Boolean = false,
  val linkedCreditId: String? = null,
  val isVirtualCredit: Boolean = false,
  val isPinned: Boolean = false
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
  val verificationStatus: String = "VERIFIED_RULES",
  val usesVirtualCol: Boolean = false,
  val pinnedDatesUsed: List<String> = emptyList()
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
        description = descWithPrefix,
        linkedCreditId = day.linkedCreditId,
        isVirtualCredit = day.isVirtualCredit
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
    var virtualCol = 0
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
          "COL" -> {
            col++
            if (step.isVirtualCredit || step.linkedCreditId?.startsWith("virtual-credit-") == true) {
              virtualCol++
            }
          }
          "EL" -> el++
        }
      }
    }

    val effectiveColPool = colGhAuditAllYears.availableUnusedGhCount + constraints.plannedToWorkHolidays.size
    val remCl26 = (summary2026.clRemaining - cl26).coerceAtLeast(0.0)
    val remRh26 = (summary2026.rhRemaining - rh26).coerceAtLeast(0)
    val remCol = (effectiveColPool - col).coerceAtLeast(0)
    val baseCl27 = summary2027?.clRemaining ?: 12.0
    val baseRh27 = summary2027?.rhRemaining ?: 3
    val remCl27 = (baseCl27 - cl27).coerceAtLeast(0.0)
    val remRh27 = (baseRh27 - rh27).coerceAtLeast(0)

    val exceedsMsg = buildList {
      if (cl26 > summary2026.clRemaining) add("2026 CL overdrawn (${formatHalf(cl26)}/${formatHalf(summary2026.clRemaining)})")
      if (rh26 > summary2026.rhRemaining) add("2026 RH overdrawn ($rh26/${summary2026.rhRemaining})")
      if (col > effectiveColPool) add("COL exceeds $effectiveColPool GH credits")
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
      exceedsQuotaMessage = exceedsMsg,
      virtualColUsed = virtualCol
    )
  }

  /**
   * Core Whole-Year Joint Portfolio Solver:
   * Delegates to [PortfolioCalendarSolver] for strictly non-overlapping, expiry-bound FIFO/Virtual COL,
   * 2nd-Saturday+Sunday default calendar parity, pin/block overrides, spread/proximity scoring,
   * TN 10-day ceiling, and Rule-8 EL prefix/suffix boundary compliance.
   */
  fun scanHolidayDatabaseForLongWeekends(
    summary2026: YearCreditSummary,
    colGhAuditAllYears: ColGhAuditSummary,
    records: List<LeaveRecord>,
    focusGoal: String = "",
    summary2027: YearCreditSummary? = null,
    referenceDate: LocalDate = LocalDate.now(),
    constraints: OptimizerConstraints = OptimizerConstraints()
  ): List<AiLeaveRecommendation> = PortfolioCalendarSolver.solveJointPortfolio(
    summary2026 = summary2026,
    colGhAuditAllYears = colGhAuditAllYears,
    records = records,
    focusGoal = focusGoal,
    summary2027 = summary2027,
    referenceDate = referenceDate,
    constraints = constraints
  )

  internal fun annotatePortfolioBalances(
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
      val singleBalNote = if (plan.startDateIso.startsWith("2026")) {
        "2026 Left: ${formatHalf(impact.remainingCl2026)} CL • ${impact.remainingRh2026} RH • ${impact.remainingColPool} COL Pool"
      } else {
        "2027 Left: ${formatHalf(impact.remainingCl2027)} CL • ${impact.remainingRh2027} RH • ${impact.remainingColPool} COL Pool"
      }
      val cumulativeNote = if (running.size == 1) {
        "If #1 booked → '26 Left: ${formatHalf(impact.remainingCl2026)} CL • ${impact.remainingRh2026} RH • ${impact.remainingColPool} GH"
      } else {
        "Cumulative #1–#${running.size} (${impact.totalDaysOffUnlocked}d off) → '26 Left: ${formatHalf(impact.remainingCl2026)} CL • ${impact.remainingRh2026} RH • '27 Left: ${formatHalf(impact.remainingCl2027)} CL • ${impact.remainingColPool} GH"
      }
      plan.copy(
        projectedBalanceAfter = plan.projectedBalanceAfter.ifBlank { singleBalNote },
        portfolioBalanceAfter = cumulativeNote
      )
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

      val pinnedStr = if (effective.pinnedDates.isEmpty()) "None" else effective.pinnedDates.entries.joinToString(", ") { "${it.key}:${it.value}" }
      val blockedStr = if (effective.blockedDatesIso.isEmpty()) "None" else effective.blockedDatesIso.sorted().joinToString(", ")
      val virtualDutyStr = if (effective.plannedToWorkHolidays.isEmpty()) "None" else effective.plannedToWorkHolidays.sorted().joinToString(", ")

      val prompt = """
        Today's date is $todayIso.
        Officer Profile: $officerContextNote.
        Analyze the synced TN Government Holiday DB ($todayIso through 2027-12-31) alongside the pre-solved STRICT NON-OVERLAPPING JOINT PORTFOLIO candidates and constraints below to produce 4 to 6 conflict-free, jointly affordable leave plans.

        SELECTED OPTIMIZATION STRATEGY & STEERING CONSTRAINTS:
        - Strategy Focus: "$focusGoal"
        - Custom User Ask: "${effective.customNaturalPrompt.ifBlank { "None" }}"
        - Weekend Off Schedule: ${effective.effectiveWeekendPolicy().label} (CRITICAL: Non-weekend Saturdays are working days unless marked GH!)
        - Monthly Spread Mode: ${effective.spreadMode.label} | Proximity Mode: ${effective.proximityMode.label}
        - Emergency CL Safety Reserve Floor: Keep ≥ ${formatHalf(effective.reserveClFloor)} CL untouched
        - Strict Zero-CL Mode: ${effective.strictZeroCl}
        - Allow Earned Leave (EL Rule 8 Prefix/Suffix): ${effective.allowElBridges}
        - Allow Half-Day 0.5 CL (FN/AN): ${effective.allowHalfDayCl}
        - Protected OPD / Clinical Duty Weekdays (prefer avoiding): $opdDaysStr
        - Pinned Dates (MUST be included): $pinnedStr
        - Blocked Dates (MUST NEVER schedule leave): $blockedStr
        - Planned Holiday Work Duties (+1 Virtual COL each, 180d expiry): $virtualDutyStr
        - Horizon Window: ${effective.horizonMode.label}

        LIVE REMAINING LEAVE CREDITS:
        - 2026 CL Remaining: ${summary2026.clRemaining} / ${summary2026.clQuota} (lapses Dec 31, 2026)
        - 2026 RH Remaining: ${summary2026.rhRemaining} / ${summary2026.rhQuota} (lapses Dec 31, 2026)
        - 2027 CL Remaining: $remCl27 / 12 | 2027 RH Remaining: $remRh27 / 3
        - Unused GH Worked Credits Ready for COL (FIFO, 180-day expiry): ${colGhAuditAllYears.availableUnusedGhCount} ($unusedGhDates)

        UPCOMING LEAVES ALREADY LOGGED BY OFFICER:
        $upcomingLoggedLeaves

        JOINT NON-OVERLAPPING SOLVER PORTFOLIO CANDIDATES:
        $preScannedText

        UPCOMING SYNCED TN GOVERNMENT PUBLIC HOLIDAYS (GH):
        $upcomingGhText

        UPCOMING SYNCED TN RESTRICTED HOLIDAYS (RH / RL):
        $upcomingRhText

        INSTRUCTIONS:
        1. Treat the joint solver candidates as a conflict-free portfolio base; refine titles, rationales, and narrative ranking strictly WITHOUT overlapping dates or double-spending CL/RH/COL across cards.
        2. Every recommended date must be >= $todayIso, NOT in Blocked Dates, honor all Pinned Dates, and follow ${effective.effectiveWeekendPolicy().label} strictly.
        3. Respect the ${formatHalf(effective.reserveClFloor)} CL reserve floor and Strict Zero-CL=$effective.strictZeroCl.
        4. Use exact creditType values from ["WEEKEND", "GH", "RH", "CL", "HALF_CL", "COL", "EL"].
        5. Format `exactDatesToApply` items as `YYYY-MM-DD — Apply 1 <RH|COL|CL|EL> (<Reason>)` or `YYYY-MM-DD — Apply 0.5 CL (<Session>)`.
        6. Explain clearly in `executiveSummary` and `whyOptimalForHod` how the portfolio conserves quotas jointly, protects OPD shifts, honors pinned/virtual-duty credits, and avoids Dec 31 / 180-day COL expiry.
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

  private data class VerifierColCredit(
    val id: String,
    val earnedDate: LocalDate,
    val earnedDateIso: String,
    val expiryDate: LocalDate,
    val isVirtual: Boolean,
    var consumed: Boolean = false
  )

  /**
   * Zero-Trust Deterministic Portfolio Verifier over Gemini AI output:
   * - Enforces exact [WeekendPolicy] (e.g., 2nd Saturday & Sundays only), blocked dates, and strictZeroCl
   * - Re-verifies 10-day continuous ceiling & EL/CL isolation against both [records] and earlier cards
   * - Tracks cumulative 2026/2027 CL, RH, EL, and FIFO 180-day COL credit pools (real + virtual) across ALL cards
   *   so zero credits can ever be double-spent or overdrawn
   * - Guarantees pinned dates remain honored by merging verified solver blocks if Gemini dropped or broke any card.
   */
  internal fun validateAndRepairGeminiRecommendations(
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
    val weekendPolicy = constraints.effectiveWeekendPolicy()
    val horizonEnd = when (constraints.horizonMode) {
      OptimizerHorizon.NEXT_60_DAYS -> today.plusDays(60)
      OptimizerHorizon.REST_OF_2026 -> LocalDate.of(2026, 12, 31)
      OptimizerHorizon.Q1_Q2_2027 -> LocalDate.of(2027, 6, 30)
      OptimizerHorizon.FULL_HORIZON -> LocalDate.of(2027, 12, 31)
    }

    // Cumulative portfolio state across all emitted cards
    var remCl2026 = if (constraints.strictZeroCl) 0.0 else (summary2026.clRemaining - constraints.reserveClFloor).coerceAtLeast(0.0)
    var remCl2027 = if (constraints.strictZeroCl) 0.0 else ((summary2027?.clRemaining ?: 12.0) - constraints.reserveClFloor).coerceAtLeast(0.0)
    var remRh2026 = summary2026.rhRemaining.coerceAtLeast(0)
    var remRh2027 = (summary2027?.rhRemaining ?: 3).coerceAtLeast(0)
    val colPool = mutableListOf<VerifierColCredit>().apply {
      colGhAuditAllYears.unusedGhRecords.forEach { gh ->
        val gd = runCatching { LocalDate.parse(gh.dateIso, isoFmt) }.getOrNull() ?: return@forEach
        val exp = gd.plusDays(TamilNaduLeaveRulesService.COL_EXPIRY_WINDOW_DAYS.toLong())
        if (!exp.isBefore(today)) {
          add(VerifierColCredit(gh.id.ifBlank { "gh-${gh.dateIso}" }, gd, gh.dateIso, exp, isVirtual = false))
        }
      }
      constraints.plannedToWorkHolidays.forEach { workIso ->
        val gd = runCatching { LocalDate.parse(workIso, isoFmt) }.getOrNull() ?: return@forEach
        val exp = gd.plusDays(TamilNaduLeaveRulesService.COL_EXPIRY_WINDOW_DAYS.toLong())
        if (!exp.isBefore(today)) {
          add(VerifierColCredit("virtual-credit-$workIso", gd, workIso, exp, isVirtual = true))
        }
      }
      sortWith(compareBy<VerifierColCredit> { it.expiryDate }.thenBy { it.earnedDate })
    }
    val usedActionDates = mutableSetOf<String>()

    fun tryVerifyCard(rec: AiLeaveRecommendation): AiLeaveRecommendation? {
      val start = runCatching { LocalDate.parse(rec.startDateIso, isoFmt) }.getOrNull() ?: return null
      val end = runCatching { LocalDate.parse(rec.endDateIso, isoFmt) }.getOrNull() ?: return null
      if (start.isBefore(today) || end.isAfter(horizonEnd) || end.isBefore(start)) return null

      // Snapshot budgets so rejected cards roll back cleanly without corrupting shared pools
      var snapCl26 = remCl2026
      var snapCl27 = remCl2027
      var snapRh26 = remRh2026
      var snapRh27 = remRh2027
      val reservedCredits = mutableListOf<VerifierColCredit>()
      val cardActionDates = mutableSetOf<String>()

      val totalDays = ChronoUnit.DAYS.between(start, end).toInt() + 1
      val containsEl = rec.dailyPlan.any { it.creditType.equals("EL", ignoreCase = true) }
      if (!containsEl && constraints.enforce10DayCap && totalDays > 10) return null

      var freeDays = 0
      var creditsDebited = 0.0
      var opdConflicts = 0
      val opdConflictLabels = mutableListOf<String>()
      val verifiedSteps = mutableListOf<AiBridgeDayPlan>()
      val repairedApplyLines = mutableListOf<String>()
      var usesVirtualCol = false
      val pinnedInCard = mutableListOf<String>()

      for (step in rec.dailyPlan) {
        val d = runCatching { LocalDate.parse(step.dateIso, isoFmt) }.getOrNull() ?: continue
        val iso = d.format(isoFmt)
        if (d.isBefore(today) || d.isAfter(horizonEnd)) return null

        val gh = ghMap[iso]
        val rh = rhMap[iso]
        val isWorkedHol = iso in constraints.plannedToWorkHolidays
        val isWknd = weekendPolicy.isWeekend(d)
        val isOpd = d.dayOfWeek in constraints.protectedDaysOfWeek || iso in constraints.protectedDatesIso
        val loggedRecs = existingByDate[iso].orEmpty()
        val alreadyLogged = loggedRecs.any {
          it.category in setOf(LeaveCategory.CL, LeaveCategory.RH, LeaveCategory.COL, LeaveCategory.PL, LeaveCategory.EL)
        }

        val rawType = step.creditType.uppercase(Locale.US)
        val isPinnedDay = iso in constraints.pinnedDates

        val resolvedType = when {
          gh != null && !isWorkedHol && rawType != "EL" -> "GH"
          isWknd && !isWorkedHol && rawType != "EL" -> "WEEKEND"
          else -> {
            // Leave slot checks
            if (!alreadyLogged && (iso in constraints.blockedDatesIso || isWorkedHol)) {
              return null
            }
            when (rawType) {
              "RH" -> if (rh != null && !isWorkedHol) "RH" else {
                val eligibleCol = colPool.firstOrNull { !it.consumed && it !in reservedCredits && !d.isBefore(it.earnedDate) && !d.isAfter(it.expiryDate) }
                when {
                  eligibleCol != null -> "COL"
                  !constraints.strictZeroCl -> "CL"
                  else -> return null
                }
              }
              "HALF_CL" -> if (constraints.strictZeroCl || !constraints.allowHalfDayCl) return null else "HALF_CL"
              "CL" -> if (constraints.strictZeroCl) return null else "CL"
              "EL" -> if (!constraints.allowElBridges && !isPinnedDay) return null else "EL"
              else -> rawType
            }
          }
        }

        val isHalf = resolvedType == "HALF_CL" || step.isHalfDay || step.description.contains("0.5", ignoreCase = true)
        var stepLinkedCreditId: String? = null
        var stepIsVirtual = false

        when (resolvedType) {
          "GH", "WEEKEND" -> freeDays++
          else -> {
            if (!alreadyLogged) {
              if (iso in usedActionDates || !cardActionDates.add(iso)) return null
              val cost = if (isHalf) 0.5 else 1.0
              val is2026 = d.year == 2026
              when (resolvedType) {
                "RH" -> {
                  if (is2026) {
                    if (snapRh26 <= 0) return null
                    snapRh26--
                  } else {
                    if (snapRh27 <= 0) return null
                    snapRh27--
                  }
                }
                "COL" -> {
                  val preferredId = step.linkedCreditId
                  val match = colPool.firstOrNull { c ->
                    !c.consumed && c !in reservedCredits &&
                      !d.isBefore(c.earnedDate) && !d.isAfter(c.expiryDate) &&
                      (preferredId == null || c.id == preferredId)
                  } ?: colPool.firstOrNull { c ->
                    !c.consumed && c !in reservedCredits &&
                      !d.isBefore(c.earnedDate) && !d.isAfter(c.expiryDate)
                  } ?: return null
                  reservedCredits.add(match)
                  stepLinkedCreditId = match.id
                  stepIsVirtual = match.isVirtual
                  if (stepIsVirtual) usesVirtualCol = true
                }
                "HALF_CL", "CL" -> {
                  if (is2026) {
                    if (snapCl26 < cost && !isPinnedDay) return null
                    snapCl26 = (snapCl26 - cost).coerceAtLeast(0.0)
                  } else {
                    if (snapCl27 < cost && !isPinnedDay) return null
                    snapCl27 = (snapCl27 - cost).coerceAtLeast(0.0)
                  }
                }
              }

              creditsDebited += cost
              if (isPinnedDay) pinnedInCard.add(iso)
              if (isOpd) {
                opdConflicts++
                opdConflictLabels.add(step.dayLabel)
              }
              val unitStr = if (isHalf) "0.5 CL" else "1 $resolvedType"
              val descHint = rh?.nameEnglish?.substringBefore(" (") ?: step.description.take(28)
              repairedApplyLines.add("$iso — Apply $unitStr ($descHint)")
            }
          }
        }

        verifiedSteps.add(
          step.copy(
            creditType = resolvedType,
            isHalfDay = isHalf,
            isAlreadyLogged = alreadyLogged,
            isProtectedOpdDay = isOpd && resolvedType !in setOf("GH", "WEEKEND"),
            linkedCreditId = stepLinkedCreditId ?: step.linkedCreditId,
            isVirtualCredit = stepIsVirtual || step.isVirtualCredit,
            isPinned = isPinnedDay || step.isPinned
          )
        )
      }

      if (verifiedSteps.isEmpty() || creditsDebited <= 0.0 || cardActionDates.isEmpty()) return null

      // Enforce TN non-combination (no EL adjacent to CL/RH/COL) and 10-day ceiling
      val isElPlan = verifiedSteps.any { it.creditType == "EL" && !it.isAlreadyLogged }
      if (!isElPlan && verifiedSteps.any { it.creditType == "EL" }) return null
      if (!isElPlan && constraints.enforce10DayCap) {
        val adjBefore = (1..6).takeWhile { off ->
          val p = start.minusDays(off.toLong())
          val pIso = p.format(isoFmt)
          val pWorked = pIso in constraints.plannedToWorkHolidays
          (!pWorked && (ghMap.containsKey(pIso) || weekendPolicy.isWeekend(p))) ||
            pIso in usedActionDates ||
            existingByDate[pIso]?.any { it.category in setOf(LeaveCategory.CL, LeaveCategory.RH, LeaveCategory.COL) } == true
        }.size
        val adjAfter = (1..6).takeWhile { off ->
          val n = end.plusDays(off.toLong())
          val nIso = n.format(isoFmt)
          val nWorked = nIso in constraints.plannedToWorkHolidays
          (!nWorked && (ghMap.containsKey(nIso) || weekendPolicy.isWeekend(n))) ||
            nIso in usedActionDates ||
            existingByDate[nIso]?.any { it.category in setOf(LeaveCategory.CL, LeaveCategory.RH, LeaveCategory.COL) } == true
        }.size
        if (totalDays + adjBefore + adjAfter > 10) return null
      }

      // Commit budget deductions
      remCl2026 = snapCl26
      remCl2027 = snapCl27
      remRh2026 = snapRh26
      remRh2027 = snapRh27
      reservedCredits.forEach { it.consumed = true }
      usedActionDates.addAll(cardActionDates)

      val mult = ((totalDays / creditsDebited) * 10.0).roundToInt() / 10.0
      val opdNote = when {
        constraints.protectedDaysOfWeek.isEmpty() && constraints.protectedDatesIso.isEmpty() -> ""
        opdConflicts == 0 -> "Duty/OPD-Safe: Zero protected clinical shifts touched"
        else -> "Duty/OPD Notice: Touches ${opdConflictLabels.joinToString(", ")} — arrange cover"
      }
      val hasCol = verifiedSteps.any { it.creditType == "COL" && !it.isAlreadyLogged }
      val urgency = when {
        usesVirtualCol -> "VIRTUAL +1 COL PLAN"
        hasCol -> "FIFO COL TRACKED"
        start.year == 2026 -> "SAVES DEC 31 LAPSE"
        else -> ""
      }

      return rec.copy(
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
        verificationStatus = "GEMINI + TN RULES VERIFIED",
        usesVirtualCol = usesVirtualCol,
        pinnedDatesUsed = pinnedInCard
      )
    }

    val validatedList = raw.recommendations.mapNotNull { tryVerifyCard(it) }.toMutableList()

    // Ensure every pinned date from user steering is honored; if Gemini missed any pin, backfill from solver
    val coveredPins = validatedList.flatMap { it.pinnedDatesUsed }.toSet()
    val missingPins = constraints.pinnedDates.keys.filter { it !in coveredPins && it !in constraints.blockedDatesIso }
    if (missingPins.isNotEmpty()) {
      deterministicFallback
        .filter { fb -> fb.dailyPlan.any { it.dateIso in missingPins } }
        .forEach { fb ->
          if (validatedList.size < 8) {
            tryVerifyCard(fb)?.let { validatedList.add(it) }
          }
        }
    }

    // Backfill remaining capacity from deterministic solver without double-spending or overlapping
    for (fb in deterministicFallback) {
      if (validatedList.size >= 6) break
      tryVerifyCard(fb)?.let { validatedList.add(it) }
    }

    // Fallback safety: if Gemini failed completely or returned zero usable plans, present the conflict-free solver portfolio
    val finalPlans = if (validatedList.isEmpty()) deterministicFallback else validatedList.sortedBy { it.startDateIso }

    val annotated = annotatePortfolioBalances(
      plans = finalPlans,
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
