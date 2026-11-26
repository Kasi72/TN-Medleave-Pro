package com.example.domain

import android.util.Log
import com.example.data.BridgeDayStep
import com.example.data.ColGhAuditSummary
import com.example.data.InitialLeaveSeedData
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
  val creditType: String, // "WEEKEND", "GH", "RH", "CL", "COL"
  val description: String
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
  val whyOptimalForHod: String,
  val tnRuleComplianceCheck: String,
  val dailyPlan: List<AiBridgeDayPlan>
) {
  fun toSmartLeaveBridge(): SmartLeaveBridge {
    val steps = dailyPlan.map { day ->
      val mappedBadge = when (day.creditType.uppercase(Locale.US)) {
        "WEEKEND" -> "WEEKEND"
        "GH" -> "GH"
        "RH" -> "RH"
        "COL" -> "COL"
        else -> "LEAVE_CREDIT"
      }
      BridgeDayStep(
        dateIso = day.dateIso,
        dayLabel = day.dayLabel,
        badgeType = mappedBadge,
        description = day.description
      )
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
      tnRuleComplianceNote = "$tnRuleComplianceCheck • $whyOptimalForHod"
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
    val analyzedHolidayCount: Int = 0
  ) : AiRecommendationUiState
  data class Error(
    val focusGoal: String,
    val message: String
  ) : AiRecommendationUiState
}

object AiLeaveRecommendationEngine {

  private val isoDateRegex = Regex("""^\d{4}-\d{2}-\d{2}$""")

  private val recommendationSchema = Schema.obj(
    properties = mapOf(
      "executiveSummary" to Schema.string(
        description = "Concise 2-sentence strategic overview of how the HOD can maximize upcoming long weekends using the holiday database and remaining leave credits."
      ),
      "recommendations" to Schema.array(
        description = "4 optimal upcoming long-weekend leave recommendations ranked by efficiency multiplier (totalDaysOff / leaveCreditsUsed) and alignment with the chosen strategy.",
        items = Schema.obj(
          properties = mapOf(
            "id" to Schema.string(description = "Unique slug identifier, e.g. ai_christmas_long_wknd_2026"),
            "title" to Schema.string(description = "Short executive title of the long weekend stretch, e.g. Christmas & Arudhra 4-Day Long Weekend"),
            "startDateIso" to Schema.string(description = "Start date of the total long weekend in YYYY-MM-DD format"),
            "endDateIso" to Schema.string(description = "End date of the total long weekend in YYYY-MM-DD format"),
            "totalDaysOff" to Schema.integer(description = "Total consecutive calendar days off including weekends and public holidays"),
            "leaveCreditsUsed" to Schema.double(description = "Total personal leave credits debited (CL + RH + COL combined)"),
            "freeDaysUnlocked" to Schema.integer(description = "Number of free Saturday, Sunday, and Public Holiday (GH) days unlocked without debiting leave balance"),
            "efficiencyMultiplier" to Schema.double(description = "Ratio of totalDaysOff divided by leaveCreditsUsed (e.g. 4 days off / 1 credit = 4.0)"),
            "exactDatesToApply" to Schema.array(
              description = "Exact date(s) and leave type(s) the user should apply for to unlock this long weekend, starting with YYYY-MM-DD, e.g. ['2026-12-24 — Apply 1 RH (Arudhra Darisanam)']",
              items = Schema.string()
            ),
            "creditMixSummary" to Schema.string(description = "Exact credit combination used, e.g. '1 RH (Dec 24) + 3 Free GH/Weekend Days'"),
            "projectedBalanceAfter" to Schema.string(description = "Remaining leave balances after applying for this bridge, e.g. '2026 Left: 3 CL • 1 RH | 2027 Left: 12 CL • 3 RH'"),
            "whyOptimalForHod" to Schema.string(description = "Why these specific application dates maximize long-weekend efficiency for HOD Dept. of Psychiatry"),
            "tnRuleComplianceCheck" to Schema.string(description = "Verification under Tamil Nadu Leave Rules 1933 & FR Annexure VII (10-day continuous absence limit, zero debit for intervening GH/Weekends)"),
            "dailyPlan" to Schema.array(
              description = "Day-by-day chronological schedule from startDateIso to endDateIso",
              items = Schema.obj(
                properties = mapOf(
                  "dateIso" to Schema.string(description = "YYYY-MM-DD"),
                  "dayLabel" to Schema.string(description = "Short day and date label, e.g. 'Thu, Dec 24'"),
                  "creditType" to Schema.enumeration(
                    values = listOf("WEEKEND", "GH", "RH", "CL", "COL"),
                    description = "Must be one of: WEEKEND, GH, RH, CL, COL"
                  ),
                  "description" to Schema.string(
                    description = "Holiday name or credit action, e.g. 'APPLY 1 RH: Christmas Eve' or 'FREE GH: Christmas Day'"
                  )
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
      You are the AI-Powered Leave Optimizer Engine for Dr. Kasi (HOD, Department of Psychiatry, Government Erode Medical College & Hospital, Tamil Nadu Medical Service).
      Your primary mission is to analyze the synced Tamil Nadu Government Holiday Database (2026 & 2027 Public Holidays GH and Restricted Holidays RH/RL) alongside the officer's real-time remaining leave credits (CL, RH, and unused GH Worked credits available for COL) to suggest the MOST EFFICIENT UPCOMING DATES TO APPLY FOR LEAVE to maximize long weekends.

      STRICT TAMIL NADU LEAVE RULES (FR ANNEXURE VII & TN LEAVE RULES 1933):
      1. Casual Leave (CL) + Restricted Holiday (RH) + Compensatory Off Leave (COL) can be prefixed, suffixed, or sandwiched with Saturdays, Sundays, and Public Holidays (GH) to create high-multiplier long weekends.
      2. 10-DAY CONTINUOUS ABSENCE CAP: Total continuous absence when using CL/RH/COL (including prefixed, suffixed, and intervening Saturdays, Sundays, and GH) MUST NEVER EXCEED 10 DAYS at a single stretch.
      3. ZERO DEBIT FOR INTERVENING HOLIDAYS: When taking CL, RH, or COL, Saturdays, Sundays, and TN Government Holidays (GH) are FREE (0 credits debited).
      4. CREDIT SELECTION PRIORITY FOR MAXIMUM EFFICIENCY:
         - Priority 1: Use Restricted Holiday (RH) on notified RL dates that fall adjacent to a weekend or GH (max 3 RH/calendar year; lapses Dec 31).
         - Priority 2: Use Compensatory Off (COL) paired FIFO with the oldest unused GH Worked credit (since COL expires 6 months / 180 days after the hospital duty date).
         - Priority 3: Use Casual Leave (CL) on bridge working days (max 12 CL/calendar year; lapses Dec 31).
      5. Never recommend dates in the past. Only recommend UPCOMING dates on or after the provided current date.
      6. Format each entry in `exactDatesToApply` starting with `YYYY-MM-DD — Apply 1 <TYPE> (<Holiday/Note>)`.
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
   * Dynamically scans the synced Tamil Nadu Government Holiday Database (2026 + 2027 GH & RH)
   * from today forward alongside live remaining CL, RH, and unused GH-Worked (COL) balances to
   * discover and rank upcoming long-weekend bridges tailored to the selected [focusGoal].
   */
  fun scanHolidayDatabaseForLongWeekends(
    summary2026: YearCreditSummary,
    colGhAuditAllYears: ColGhAuditSummary,
    records: List<LeaveRecord>,
    focusGoal: String = "",
    summary2027: YearCreditSummary? = null,
    referenceDate: LocalDate = LocalDate.now()
  ): List<AiLeaveRecommendation> {
    val today = if (referenceDate.year < 2026) LocalDate.of(2026, 11, 25) else referenceDate
    val horizonEnd = LocalDate.of(2027, 12, 31)
    val existingDates = records.map { it.dateIso }.toSet()

    val allGh = TnHolidays2027Catalog.allSyncedPublicHolidays()
    val allRh = TnHolidays2027Catalog.allSyncedRestrictedHolidays()
    val ghMap = allGh.associateBy { it.dateIso }
    val rhMap = allRh.associateBy { it.dateIso }

    val unusedGhSorted = colGhAuditAllYears.unusedGhRecords.sortedBy { it.dateIso }
    val unusedGhCount = colGhAuditAllYears.availableUnusedGhCount
    val goalLower = focusGoal.lowercase(Locale.US)
    val preferZeroCl = goalLower.contains("zero-cl") || goalLower.contains("rh + col")
    val preferFifoCol = goalLower.contains("fifo") || goalLower.contains("expiring gh")
    val preferMegabridges = goalLower.contains("megabridge") || goalLower.contains("festival")

    fun isFreeCalendarDay(date: LocalDate): Boolean {
      val iso = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
      return ghMap.containsKey(iso) ||
        date.dayOfWeek == DayOfWeek.SATURDAY ||
        date.dayOfWeek == DayOfWeek.SUNDAY
    }

    fun isFestivalDay(date: LocalDate): Boolean {
      val iso = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
      return ghMap.containsKey(iso) || rhMap.containsKey(iso)
    }

    fun buildDynamicWindow(start: LocalDate, end: LocalDate): AiLeaveRecommendation? {
      if (start.isBefore(today) || end.isAfter(horizonEnd) || end.isBefore(start)) return null
      val totalDays = ChronoUnit.DAYS.between(start, end).toInt() + 1
      if (totalDays < 3 || totalDays > 9) return null // Keep comfortably <= 10-day TN cap

      var remCl2026 = summary2026.clRemaining
      var remRh2026 = summary2026.rhRemaining
      var remCl2027 = summary2027?.clRemaining ?: 12.0
      var remRh2027 = summary2027?.rhRemaining ?: 3
      var remCol = unusedGhCount
      var colIdx = 0

      var creditsUsed = 0.0
      var freeDays = 0
      val applyDates = mutableListOf<String>()
      val dailySteps = mutableListOf<AiBridgeDayPlan>()
      val anchorNames = mutableListOf<String>()

      var cursor = start
      while (!cursor.isAfter(end)) {
        val iso = cursor.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val dayShort = "${cursor.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)}, ${
          cursor.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
        } ${cursor.dayOfMonth}"
        val ghItem: TnHolidayItem? = ghMap[iso]
        val rhItem: TnHolidayItem? = rhMap[iso]
        val isWeekend = cursor.dayOfWeek == DayOfWeek.SATURDAY || cursor.dayOfWeek == DayOfWeek.SUNDAY
        val alreadyLogged = iso in existingDates
        val is2026 = cursor.year == 2026
        val canUseRh = if (is2026) remRh2026 > 0 else remRh2027 > 0
        val canUseCl = if (is2026) remCl2026 >= 1.0 else remCl2027 >= 1.0

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
          rhItem != null && canUseRh && !preferFifoCol -> {
            creditsUsed += 1.0
            if (is2026) remRh2026-- else remRh2027--
            anchorNames.add(rhItem.nameEnglish.substringBefore(" ("))
            val statusSuffix = if (alreadyLogged) " [Already Logged]" else ""
            val cleanRhName = rhItem.nameEnglish.substringBefore(" (")
            applyDates.add("$iso — Apply 1 RH ($cleanRhName)$statusSuffix")
            dailySteps.add(
              AiBridgeDayPlan(
                dateIso = iso,
                dayLabel = dayShort,
                creditType = "RH",
                description = "APPLY 1 RH: ${rhItem.nameEnglish} (${rhItem.nameTamil})$statusSuffix"
              )
            )
          }
          remCol > 0 && (preferFifoCol || preferZeroCl || !canUseCl || rhItem == null) -> {
            val pairedGh = unusedGhSorted.getOrNull(colIdx)
            val ghValidForCol = if (pairedGh != null) {
              val ghDate = runCatching { LocalDate.parse(pairedGh.dateIso) }.getOrNull()
              ghDate == null || ChronoUnit.DAYS.between(ghDate, cursor) in 0..180
            } else {
              true
            }
            if (ghValidForCol) {
              creditsUsed += 1.0
              remCol--
              colIdx++
              val pairedNote = pairedGh?.dateIso?.let { " vs GH $it" } ?: ""
              val statusSuffix = if (alreadyLogged) " [Already Logged]" else ""
              applyDates.add("$iso — Apply 1 COL$pairedNote$statusSuffix")
              dailySteps.add(
                AiBridgeDayPlan(
                  dateIso = iso,
                  dayLabel = dayShort,
                  creditType = "COL",
                  description = "APPLY 1 COL (Comp-Off$pairedNote)$statusSuffix"
                )
              )
            } else if (rhItem != null && canUseRh) {
              creditsUsed += 1.0
              if (is2026) remRh2026-- else remRh2027--
              anchorNames.add(rhItem.nameEnglish.substringBefore(" ("))
              val cleanRhName = rhItem.nameEnglish.substringBefore(" (")
              applyDates.add("$iso — Apply 1 RH ($cleanRhName)")
              dailySteps.add(
                AiBridgeDayPlan(
                  dateIso = iso,
                  dayLabel = dayShort,
                  creditType = "RH",
                  description = "APPLY 1 RH: ${rhItem.nameEnglish} (${rhItem.nameTamil})"
                )
              )
            } else {
              if (preferZeroCl) return null
              creditsUsed += 1.0
              if (is2026) remCl2026 = (remCl2026 - 1.0).coerceAtLeast(0.0) else remCl2027 = (remCl2027 - 1.0).coerceAtLeast(0.0)
              val statusSuffix = if (alreadyLogged) " [Already Logged]" else ""
              applyDates.add("$iso — Apply 1 CL$statusSuffix")
              dailySteps.add(
                AiBridgeDayPlan(
                  dateIso = iso,
                  dayLabel = dayShort,
                  creditType = "CL",
                  description = "APPLY 1 CL (Casual Leave Bridge Day)$statusSuffix"
                )
              )
            }
          }
          rhItem != null && canUseRh -> {
            creditsUsed += 1.0
            if (is2026) remRh2026-- else remRh2027--
            anchorNames.add(rhItem.nameEnglish.substringBefore(" ("))
            val statusSuffix = if (alreadyLogged) " [Already Logged]" else ""
            val cleanRhName = rhItem.nameEnglish.substringBefore(" (")
            applyDates.add("$iso — Apply 1 RH ($cleanRhName)$statusSuffix")
            dailySteps.add(
              AiBridgeDayPlan(
                dateIso = iso,
                dayLabel = dayShort,
                creditType = "RH",
                description = "APPLY 1 RH: ${rhItem.nameEnglish} (${rhItem.nameTamil})$statusSuffix"
              )
            )
          }
          else -> {
            if (preferZeroCl) return null
            creditsUsed += 1.0
            if (is2026) remCl2026 = (remCl2026 - 1.0).coerceAtLeast(0.0) else remCl2027 = (remCl2027 - 1.0).coerceAtLeast(0.0)
            val statusSuffix = if (alreadyLogged) " [Already Logged]" else ""
            applyDates.add("$iso — Apply 1 CL$statusSuffix")
            dailySteps.add(
              AiBridgeDayPlan(
                dateIso = iso,
                dayLabel = dayShort,
                creditType = "CL",
                description = "APPLY 1 CL (Casual Leave Bridge Day)$statusSuffix"
              )
            )
          }
        }
        cursor = cursor.plusDays(1)
      }

      if (creditsUsed < 1.0 || creditsUsed > 4.0 || freeDays < 2) return null

      val mult = ((totalDays.toDouble() / creditsUsed) * 10.0).roundToInt() / 10.0
      if (mult < 1.8) return null

      val rhUsedCount = dailySteps.count { it.creditType == "RH" }
      val colUsedCount = dailySteps.count { it.creditType == "COL" }
      val clUsedCount = dailySteps.count { it.creditType == "CL" }

      val mixParts = buildList {
        if (rhUsedCount > 0) add("$rhUsedCount RH")
        if (colUsedCount > 0) add("$colUsedCount COL")
        if (clUsedCount > 0) add("$clUsedCount CL")
        add("$freeDays Free GH/Weekend Days")
      }

      val distinctAnchors = anchorNames.distinct().take(2)
      val headlineFestival = when {
        start.monthValue == 12 && start.year == 2026 && end.year == 2027 ->
          "Christmas → New Year ${totalDays}-Day Grand Finale"
        start.monthValue == 1 && start.dayOfMonth in 12..17 && start.year == 2027 ->
          "Pongal 2027 ${totalDays}-Day Harvest Megabridge"
        distinctAnchors.isNotEmpty() ->
          "${distinctAnchors.joinToString(" & ")} ${totalDays}-Day Bridge"
        else ->
          "${start.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)} ${start.year} ${totalDays}-Day Long Weekend"
      }

      val startIso = start.format(DateTimeFormatter.ISO_LOCAL_DATE)
      val endIso = end.format(DateTimeFormatter.ISO_LOCAL_DATE)
      val clFmt26 = if (remCl2026 % 1.0 == 0.0) remCl2026.toInt().toString() else String.format(Locale.US, "%.1f", remCl2026)
      val clFmt27 = if (remCl2027 % 1.0 == 0.0) remCl2027.toInt().toString() else String.format(Locale.US, "%.1f", remCl2027)
      val balanceNote = if (end.year == 2026) {
        "2026 Left: $clFmt26 CL • $remRh2026 RH • $remCol Free GH"
      } else {
        "2027 Left: $clFmt27 CL • $remRh2027 RH • $remCol Free GH"
      }

      val rationale = buildString {
        append("Unlocks $totalDays consecutive days off (${startIso} → ${endIso}) for only ${creditsUsed.toInt()} leave credit(s) [${mult}x ROI]. ")
        if (colUsedCount > 0 && unusedGhSorted.isNotEmpty()) {
          append("Consumes FIFO GH-Worked credit (${unusedGhSorted.first().dateIso}) before 6-month expiry. ")
        }
        if (rhUsedCount > 0 && clUsedCount == 0) {
          append("Preserves Casual Leave (CL) balance by leveraging notified TN Restricted Holiday.")
        } else if (start.year == 2026) {
          append("Utilizes 2026 credits before Dec 31 lapse.")
        } else {
          append("Zero-waste bridge from 2027 TN Holiday calendar.")
        }
      }

      return AiLeaveRecommendation(
        id = "db_opt_${startIso}_$endIso",
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
        tnRuleComplianceCheck = "Verified ≤10d Continuous Stretch ($totalDays days total) • Zero debit for $freeDays Weekend/GH days under FR Annexure VII.",
        dailyPlan = dailySteps
      )
    }

    // Collect upcoming GH/RH festival anchors and expand contiguous bridge windows around them
    val upcomingAnchors = (allGh + allRh)
      .mapNotNull { runCatching { LocalDate.parse(it.dateIso) }.getOrNull() }
      .filter { !it.isBefore(today) && !it.isAfter(horizonEnd) }
      .distinct()
      .sorted()

    val candidateWindows = linkedSetOf<Pair<LocalDate, LocalDate>>()
    for (anchor in upcomingAnchors) {
      for (back in 0..4) {
        for (fwd in 0..5) {
          var s = anchor.minusDays(back.toLong())
          var e = anchor.plusDays(fwd.toLong())
          // Snap leading & trailing boundaries outward to capture adjacent free days (Sat/Sun/GH)
          while (s.isAfter(today) && isFreeCalendarDay(s.minusDays(1))) {
            s = s.minusDays(1)
          }
          while (e.isBefore(horizonEnd) && isFreeCalendarDay(e.plusDays(1))) {
            e = e.plusDays(1)
          }
          val len = ChronoUnit.DAYS.between(s, e).toInt() + 1
          if (len in 3..9 && !s.isBefore(today) && (isFestivalDay(s) || isFreeCalendarDay(s) || isFestivalDay(e) || isFreeCalendarDay(e))) {
            candidateWindows.add(s to e)
          }
        }
      }
    }

    // Ensure key known strategic clusters are always evaluated when still upcoming
    listOf(
      LocalDate.of(2026, 12, 24) to LocalDate.of(2026, 12, 27),
      LocalDate.of(2026, 12, 24) to LocalDate.of(2027, 1, 1),
      LocalDate.of(2026, 12, 31) to LocalDate.of(2027, 1, 3),
      LocalDate.of(2027, 1, 13) to LocalDate.of(2027, 1, 17),
      LocalDate.of(2027, 1, 21) to LocalDate.of(2027, 1, 24),
      LocalDate.of(2027, 1, 23) to LocalDate.of(2027, 1, 26)
    ).forEach { (s, e) ->
      if (!s.isBefore(today)) candidateWindows.add(s to e)
    }

    val built = candidateWindows.mapNotNull { (s, e) -> buildDynamicWindow(s, e) }

    fun scoreRec(rec: AiLeaveRecommendation): Double {
      val colCount = rec.dailyPlan.count { it.creditType == "COL" }
      val clCount = rec.dailyPlan.count { it.creditType == "CL" }
      val rhCount = rec.dailyPlan.count { it.creditType == "RH" }
      val recStart = runCatching { LocalDate.parse(rec.startDateIso) }.getOrDefault(today)
      val daysAway = ChronoUnit.DAYS.between(today, recStart).coerceAtLeast(0L)
      val recencyBoost = when {
        daysAway <= 45 -> 1.25
        daysAway <= 90 -> 0.85
        daysAway <= 180 -> 0.45
        else -> 0.0
      }

      return when {
        preferFifoCol ->
          (colCount * 4.5) + rec.efficiencyMultiplier * 1.4 + recencyBoost - (clCount * 0.6)
        preferZeroCl ->
          (if (clCount == 0) 5.0 else -5.0) + (rhCount * 1.2) + (colCount * 1.2) + rec.efficiencyMultiplier * 1.5 + recencyBoost
        preferMegabridges ->
          (rec.totalDaysOff * 1.6) + rec.efficiencyMultiplier + (if (rec.totalDaysOff >= 5) 3.0 else 0.0) + recencyBoost
        else ->
          (rec.efficiencyMultiplier * 2.0) + (rec.totalDaysOff * 0.45) + recencyBoost - (clCount * 0.15)
      }
    }

    // Pick top 5 diverse (mostly non-overlapping, at most 1 mega-extension per cluster) recommendations
    val ranked = built.sortedWith(
      compareByDescending<AiLeaveRecommendation> { scoreRec(it) }
        .thenByDescending { it.efficiencyMultiplier }
        .thenByDescending { it.totalDaysOff }
        .thenBy { it.startDateIso }
    )

    val selected = mutableListOf<AiLeaveRecommendation>()
    for (cand in ranked) {
      if (selected.size >= 5) break
      val candStart = LocalDate.parse(cand.startDateIso)
      val candEnd = LocalDate.parse(cand.endDateIso)
      val overlapsHeavily = selected.any { existing ->
        val exStart = LocalDate.parse(existing.startDateIso)
        val exEnd = LocalDate.parse(existing.endDateIso)
        // Allow at most a 4-day vs 9-day combo in Dec/Jan only if credit costs differ significantly, otherwise avoid duplicates
        val overlapDays = maxOf(
          0L,
          minOf(candEnd.toEpochDay(), exEnd.toEpochDay()) - maxOf(candStart.toEpochDay(), exStart.toEpochDay()) + 1L
        )
        overlapDays >= 3L && cand.leaveCreditsUsed == existing.leaveCreditsUsed
      }
      val sameMonthCount = selected.count { it.startDateIso.take(7) == cand.startDateIso.take(7) }
      if (!overlapsHeavily && sameMonthCount < 2) {
        selected.add(cand)
      }
    }

    return selected.ifEmpty { ranked.take(5) }
  }

  suspend fun generateOptimalRecommendations(
    focusGoal: String,
    summary2026: YearCreditSummary,
    colGhAuditAllYears: ColGhAuditSummary,
    records: List<LeaveRecord>,
    summary2027: YearCreditSummary? = null
  ): Result<AiRecommendationUiState.Success> {
    val today = LocalDate.now().let { if (it.year < 2026) LocalDate.of(2026, 11, 25) else it }
    val todayIso = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

    val allGh = TnHolidays2027Catalog.allSyncedPublicHolidays()
    val allRh = TnHolidays2027Catalog.allSyncedRestrictedHolidays()
    val allHolidaysCount = allGh.size + allRh.size

    val deterministicCandidates = scanHolidayDatabaseForLongWeekends(
      summary2026 = summary2026,
      colGhAuditAllYears = colGhAuditAllYears,
      records = records,
      focusGoal = focusGoal,
      summary2027 = summary2027,
      referenceDate = today
    )

    return try {
      val unusedGhDates = colGhAuditAllYears.unusedGhRecords
        .sortedBy { it.dateIso }
        .joinToString(", ") {
          "${it.dateIso} (${it.notes.ifBlank { "Hospital GH Duty" }})"
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
        "* ${cand.title} (${cand.startDateIso} to ${cand.endDateIso}): ${cand.totalDaysOff}d off for ${cand.leaveCreditsUsed} credit(s) [${cand.efficiencyMultiplier}x]. Apply on: ${cand.exactDatesToApply.joinToString("; ")}"
      }

      val remCl27 = summary2027?.clRemaining ?: 12.0
      val remRh27 = summary2027?.rhRemaining ?: 3

      val prompt = """
        Today's date is $todayIso.
        Analyze the synced Tamil Nadu Government Holiday Database ($todayIso through 2027-12-31) and the officer's remaining leave credits to suggest the 4 most efficient UPCOMING long-weekend leave plans for HOD, Department of Psychiatry (Government Erode Medical College & Hospital).

        SELECTED OPTIMIZATION STRATEGY:
        "$focusGoal"

        LIVE REMAINING LEAVE CREDITS:
        - 2026 Casual Leave (CL) Remaining: ${summary2026.clRemaining} of ${summary2026.clQuota} days (Used: ${summary2026.clTaken}) — lapses Dec 31, 2026
        - 2026 Restricted Holiday (RH) Remaining: ${summary2026.rhRemaining} of ${summary2026.rhQuota} days (Used: ${summary2026.rhTaken}) — lapses Dec 31, 2026
        - 2027 Casual Leave (CL) Remaining: $remCl27 of 12 days | 2027 Restricted Holiday (RH) Remaining: $remRh27 of 3 days
        - Unused GH Worked Credits Ready to Avail as COL (FIFO, 6-month expiry): ${colGhAuditAllYears.availableUnusedGhCount} credits ($unusedGhDates)

        UPCOMING LEAVES ALREADY LOGGED BY OFFICER:
        $upcomingLoggedLeaves

        ALGORITHMIC PRE-SCANNED UPCOMING LONG-WEEKEND CANDIDATES FROM HOLIDAY DB:
        $preScannedText

        UPCOMING SYNCED TAMIL NADU GOVERNMENT PUBLIC HOLIDAYS (GH - 0 Credits Used):
        $upcomingGhText

        UPCOMING SYNCED TAMIL NADU RESTRICTED HOLIDAYS (RH / RL LIST - Uses RH Quota):
        $upcomingRhText

        INSTRUCTIONS:
        1. Suggest 4 distinct, high-efficiency UPCOMING long-weekend bridges (ALL dates must be >= $todayIso) where applying for 1 to 4 leave days unlocks 3 to 9 consecutive days off.
        2. Tailor selection and credit choices strictly to "$focusGoal".
        3. Format each item in `exactDatesToApply` starting with ISO date: `YYYY-MM-DD — Apply 1 <RH|COL|CL> (<Reason>)`.
        4. Obey the TN 10-day maximum continuous absence rule (totalDaysOff <= 10).
        5. Calculate `efficiencyMultiplier` as `totalDaysOff / leaveCreditsUsed` (rounded to 1 decimal place) and `freeDaysUnlocked` as the count of Weekend + GH days inside the span.
        6. Include a complete chronological `dailyPlan` for every calendar date from `startDateIso` to `endDateIso` using exact creditType values from ["WEEKEND", "GH", "RH", "COL", "CL"].
      """.trimIndent()

      val responseText = runGeminiWithResilientTimeout(prompt)

      val parsed = parseStructuredRecommendations(
        focusGoal = focusGoal,
        jsonString = responseText,
        analyzedHolidayCount = allHolidaysCount
      )
      Result.success(parsed)
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "AI Leave Optimizer timed out", e)
      Result.failure(
        IllegalStateException("Gemini AI timed out after waiting for a response. Check network connectivity and tap Retry.")
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "AI Leave Optimizer generation failed", e)
      val cleanMsg = e.localizedMessage?.takeIf { it.isNotBlank() }
        ?: e::class.java.simpleName
      Result.failure(
        IllegalStateException("Gemini AI error: $cleanMsg")
      )
    }
  }

  private suspend fun runGeminiWithResilientTimeout(prompt: String): String {
    return try {
      withTimeout(45_000L) {
        val response = primaryGenerativeModel.generateContent(prompt)
        response.text?.takeIf { it.isNotBlank() }
          ?: throw IllegalStateException("Empty structured response from gemini-3.8-flash.")
      }
    } catch (e: CancellationException) {
      // Distinguish TimeoutCancellationException (which extends CancellationException) vs real parent cancellation
      if (e !is TimeoutCancellationException) throw e
      Log.w(TAG, "Primary gemini-3.8-flash timed out; attempting gemini-3.5-flash-lite fallback", e)
      withTimeout(25_000L) {
        val fallbackResp = liteFallbackGenerativeModel.generateContent(prompt)
        fallbackResp.text?.takeIf { it.isNotBlank() }
          ?: throw e
      }
    } catch (primaryErr: Exception) {
      Log.w(TAG, "Primary gemini-3.8-flash failed (${primaryErr.localizedMessage}); trying gemini-3.5-flash-lite", primaryErr)
      withTimeout(25_000L) {
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
      "Optimized long-weekend leave bridges combining Tamil Nadu Government Holidays, Restricted Holidays (RH), and unused GH Worked (COL) credits."
    ).ifBlank {
      "Optimized long-weekend leave bridges combining Tamil Nadu Government Holidays, Restricted Holidays (RH), and unused GH Worked (COL) credits."
    }

    val recArray = root.optJSONArray("recommendations")
    val list = mutableListOf<AiLeaveRecommendation>()

    if (recArray != null) {
      for (i in 0 until recArray.length()) {
        val item = recArray.optJSONObject(i) ?: continue
        val rawStartIso = item.optString("startDateIso", "").trim()
        val rawEndIso = item.optString("endDateIso", "").trim()
        val parsedStart = runCatching { LocalDate.parse(rawStartIso) }.getOrNull()

        val dailyArray = item.optJSONArray("dailyPlan")
        val dailySteps = mutableListOf<AiBridgeDayPlan>()
        if (dailyArray != null) {
          for (j in 0 until dailyArray.length()) {
            val stepObj = dailyArray.optJSONObject(j) ?: continue
            val stepDateRaw = stepObj.optString("dateIso", "").trim()
            val resolvedDateIso = if (isoDateRegex.matches(stepDateRaw)) {
              stepDateRaw
            } else if (parsedStart != null) {
              parsedStart.plusDays(j.toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE)
            } else {
              ""
            }
            if (resolvedDateIso.isEmpty()) continue

            val parsedStepDate = runCatching { LocalDate.parse(resolvedDateIso) }.getOrNull()
            val fallbackDayLabel = parsedStepDate?.let { d ->
              "${d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)}, ${
                d.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
              } ${d.dayOfMonth}"
            } ?: resolvedDateIso

            val rawCredit = stepObj.optString("creditType", "CL").trim().uppercase(Locale.US)
            val normalizedCredit = when {
              rawCredit.contains("WEEK") -> "WEEKEND"
              rawCredit == "GH" || rawCredit.contains("PUBLIC") -> "GH"
              rawCredit == "RH" || rawCredit.contains("RESTRICT") -> "RH"
              rawCredit == "COL" || rawCredit.contains("COMP") -> "COL"
              else -> "CL"
            }

            dailySteps.add(
              AiBridgeDayPlan(
                dateIso = resolvedDateIso,
                dayLabel = stepObj.optString("dayLabel", fallbackDayLabel).ifBlank { fallbackDayLabel },
                creditType = normalizedCredit,
                description = stepObj.optString("description", normalizedCredit)
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
            .filter { it.creditType in setOf("RH", "COL", "CL") }
            .forEach { step ->
              exactApplyList.add("${step.dateIso} — Apply 1 ${step.creditType}")
            }
        }

        val actionStepsCount = dailySteps.count { it.creditType in setOf("RH", "COL", "CL") }
        val totalDays = item.optInt("totalDaysOff", dailySteps.size.coerceAtLeast(1)).coerceIn(1, 10)
        val rawCredits = item.optDouble("leaveCreditsUsed", actionStepsCount.toDouble().coerceAtLeast(1.0))
        val creditsUsed = if (rawCredits.isNaN() || rawCredits <= 0.0) {
          actionStepsCount.toDouble().coerceAtLeast(1.0)
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
            creditMixSummary = item.optString("creditMixSummary", "${creditsUsed.toInt()} Credits + $freeUnlocked Free Days"),
            projectedBalanceAfter = item.optString("projectedBalanceAfter", ""),
            whyOptimalForHod = item.optString("whyOptimalForHod", ""),
            tnRuleComplianceCheck = item.optString(
              "tnRuleComplianceCheck",
              "Compliant with TN Leave Rules 1933 & FR Annexure VII (≤10 days continuous absence)."
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
}
