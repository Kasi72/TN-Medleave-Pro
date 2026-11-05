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
      val mappedBadge = when (day.creditType.uppercase()) {
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

  private val recommendationSchema = Schema.obj(
    properties = mapOf(
      "executiveSummary" to Schema.string(
        description = "Concise 2-sentence strategic overview of how the HOD can maximize long weekends using the holiday database and remaining leave credits."
      ),
      "recommendations" to Schema.array(
        description = "3 to 5 optimal long-weekend leave recommendations ranked by efficiency multiplier (totalDaysOff / leaveCreditsUsed) and alignment with remaining credits.",
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
              description = "Exact date(s) and leave type(s) the user should apply for to unlock this long weekend, e.g. ['Thu, Dec 24, 2026 — Apply 1 RH']",
              items = Schema.string()
            ),
            "creditMixSummary" to Schema.string(description = "Exact credit combination used, e.g. '1 RH (Dec 24) + 3 Free GH/Weekend Days'"),
            "projectedBalanceAfter" to Schema.string(description = "Remaining leave balances after applying for this bridge, e.g. 'Leaves left: 3.0 CL • 1 RH • 3 Free GH'"),
            "whyOptimalForHod" to Schema.string(description = "Why these specific application dates maximize long-weekend efficiency for HOD Dept. of Psychiatry"),
            "tnRuleComplianceCheck" to Schema.string(description = "Verification under Tamil Nadu Leave Rules 1933 & FR Annexure VII (10-day continuous absence limit, zero debit for intervening GH/Weekends)"),
            "dailyPlan" to Schema.array(
              description = "Day-by-day chronological schedule from startDateIso to endDateIso",
              items = Schema.obj(
                properties = mapOf(
                  "dateIso" to Schema.string(description = "YYYY-MM-DD"),
                  "dayLabel" to Schema.string(description = "Short day and date label, e.g. 'Thu, Dec 24'"),
                  "creditType" to Schema.string(
                    description = "Must be one of: WEEKEND, GH, RH, CL, COL"
                  ),
                  "description" to Schema.string(
                    description = "Holiday name or credit action, e.g. 'APPLY: Christmas Eve (RH #2)' or 'FREE: Christmas Day (TN GH)'"
                  )
                )
              )
            )
          )
        )
      )
    )
  )

  private val generativeModel by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.8-flash",
      generationConfig = generationConfig {
        responseMimeType = "application/json"
        responseSchema = recommendationSchema
        temperature = 0.2f
      },
      systemInstruction = content {
        text(
          """
          You are the AI-Powered Leave Optimizer Engine for Dr. Kasi (HOD, Department of Psychiatry, Government Erode Medical College & Hospital, Tamil Nadu Medical Service).
          Your primary mission is to analyze the local Tamil Nadu Government Holiday Database (2026 & 2027 Public Holidays GH and Restricted Holidays RH/RL) alongside the officer's real-time remaining leave credits (CL, RH, and unused GH Worked credits available for COL) to suggest the MOST EFFICIENT DATES TO APPLY FOR LEAVE to maximize long weekends.

          STRICT TAMIL NADU LEAVE RULES (FR ANNEXURE VII & TN LEAVE RULES 1933):
          1. Casual Leave (CL) + Restricted Holiday (RH) + Compensatory Off Leave (COL) can be prefixed, suffixed, or sandwiched with Saturdays, Sundays, and Public Holidays (GH) to create high-multiplier long weekends.
          2. 10-DAY CONTINUOUS ABSENCE CAP: Total continuous absence when using CL/RH/COL (including prefixed, suffixed, and intervening Saturdays, Sundays, and GH) MUST NEVER EXCEED 10 DAYS at a stretch.
          3. ZERO DEBIT FOR INTERVENING HOLIDAYS: When taking CL, RH, or COL, Saturdays, Sundays, and TN Government Holidays (GH) are FREE (0 credits debited).
          4. CREDIT SELECTION PRIORITY FOR MAXIMUM EFFICIENCY:
             - Priority 1: Use Restricted Holiday (RH) on notified RL dates that fall on a Monday, Friday, Tuesday, or Thursday adjacent to a weekend or GH (since RH lapses on Dec 31).
             - Priority 2: Use Compensatory Off (COL) paired with the oldest unused GH Worked credit (since COL expires 6 months after the hospital duty date).
             - Priority 3: Use Casual Leave (CL) on bridge working days (since CL also lapses on Dec 31).
          5. Always populate `exactDatesToApply` with the exact working day(s) the officer needs to submit a leave application for, clearly distinguishing them from the free Weekend/GH days unlocked.
          """.trimIndent()
        )
      }
    )
  }

  /**
   * Deterministically scans the local Room Holiday Database (2026 + 2027 GH & RH) and the user's
   * live remaining leave credits to identify the highest-yield long-weekend bridge opportunities.
   * Used both as immediate zero-latency baseline recommendations and as pre-computed tactical context
   * fed into Gemini AI (`gemini-3.8-flash`).
   */
  fun scanHolidayDatabaseForLongWeekends(
    summary2026: YearCreditSummary,
    colGhAuditAllYears: ColGhAuditSummary,
    records: List<LeaveRecord>
  ): List<AiLeaveRecommendation> {
    val existingDates = records.map { it.dateIso }.toSet()
    val ghMap = (InitialLeaveSeedData.tnGovtPublicHolidays2026 + TnHolidays2027Catalog.tnGovtPublicHolidays2027)
      .associateBy { it.dateIso }
    val rhMap = (InitialLeaveSeedData.tnRestrictedHolidays2026 + TnHolidays2027Catalog.tnRestrictedHolidays2027)
      .associateBy { it.dateIso }

    val oldestUnusedGh = colGhAuditAllYears.unusedGhRecords
      .sortedBy { it.dateIso }
      .firstOrNull()
    val unusedGhCount = colGhAuditAllYears.availableUnusedGhCount
    val clLeft = summary2026.clRemaining
    val rhLeft = summary2026.rhRemaining

    fun buildWindowRecommendation(
      id: String,
      title: String,
      startIso: String,
      endIso: String,
      whyOptimal: String
    ): AiLeaveRecommendation? {
      val start = runCatching { LocalDate.parse(startIso) }.getOrNull() ?: return null
      val end = runCatching { LocalDate.parse(endIso) }.getOrNull() ?: return null
      val totalDays = ChronoUnit.DAYS.between(start, end).toInt() + 1
      if (totalDays <= 0 || totalDays > 10) return null

      var remCl = clLeft
      var remRh = rhLeft
      var remCol = unusedGhCount
      var creditsUsed = 0.0
      var freeDays = 0
      val applyDates = mutableListOf<String>()
      val dailySteps = mutableListOf<AiBridgeDayPlan>()

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

        when {
          ghItem != null -> {
            freeDays++
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
          rhItem != null && (remRh > 0 || iso.startsWith("2027")) -> {
            creditsUsed += 1.0
            if (iso.startsWith("2026")) remRh = (remRh - 1).coerceAtLeast(0)
            val statusSuffix = if (alreadyLogged) " [Already Logged]" else ""
            applyDates.add("$dayShort ($iso) → Apply 1 RH (${rhItem.nameEnglish})$statusSuffix")
            dailySteps.add(
              AiBridgeDayPlan(
                dateIso = iso,
                dayLabel = dayShort,
                creditType = "RH",
                description = "APPLY 1 RH: ${rhItem.nameEnglish} (${rhItem.nameTamil})$statusSuffix"
              )
            )
          }
          remCol > 0 -> {
            creditsUsed += 1.0
            remCol = (remCol - 1).coerceAtLeast(0)
            val pairedNote = oldestUnusedGh?.dateIso?.let { " vs GH $it" } ?: ""
            val statusSuffix = if (alreadyLogged) " [Already Logged]" else ""
            applyDates.add("$dayShort ($iso) → Apply 1 COL$pairedNote$statusSuffix")
            dailySteps.add(
              AiBridgeDayPlan(
                dateIso = iso,
                dayLabel = dayShort,
                creditType = "COL",
                description = "APPLY 1 COL (Comp-Off$pairedNote)$statusSuffix"
              )
            )
          }
          else -> {
            creditsUsed += 1.0
            if (iso.startsWith("2026")) remCl = (remCl - 1.0).coerceAtLeast(0.0)
            val statusSuffix = if (alreadyLogged) " [Already Logged]" else ""
            applyDates.add("$dayShort ($iso) → Apply 1 CL$statusSuffix")
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

      val safeCredits = creditsUsed.coerceAtLeast(1.0)
      val mult = ((totalDays.toDouble() / safeCredits) * 10.0).roundToInt() / 10.0
      val rhUsedCount = dailySteps.count { it.creditType == "RH" }
      val colUsedCount = dailySteps.count { it.creditType == "COL" }
      val clUsedCount = dailySteps.count { it.creditType == "CL" }

      val mixParts = buildList {
        if (rhUsedCount > 0) add("$rhUsedCount RH")
        if (colUsedCount > 0) add("$colUsedCount COL")
        if (clUsedCount > 0) add("$clUsedCount CL")
        add("$freeDays Free GH/Weekend Days")
      }

      val clAfterFmt = if (remCl % 1.0 == 0.0) remCl.toInt().toString() else String.format("%.1f", remCl)

      return AiLeaveRecommendation(
        id = id,
        title = title,
        startDateIso = startIso,
        endDateIso = endIso,
        totalDaysOff = totalDays,
        leaveCreditsUsed = safeCredits,
        freeDaysUnlocked = freeDays,
        efficiencyMultiplier = mult,
        exactDatesToApply = applyDates,
        creditMixSummary = mixParts.joinToString(" + "),
        projectedBalanceAfter = "Post-Apply 2026 Balance: $clAfterFmt CL left • $remRh RH left • $remCol Free GH left",
        whyOptimalForHod = whyOptimal,
        tnRuleComplianceCheck = "Verified ≤10d Continuous Stretch ($totalDays days total) • Zero debit for $freeDays Weekend/GH days under FR Annexure VII.",
        dailyPlan = dailySteps
      )
    }

    return listOfNotNull(
      buildWindowRecommendation(
        id = "db_opt_christmas_4d_2026",
        title = "Christmas & Arudhra Darisanam 4-Day Long Weekend",
        startIso = "2026-12-24",
        endIso = "2026-12-27",
        whyOptimal = "Highest single-credit yield in Dec 2026: Applying on Thu Dec 24 (Notified RH) bridges Fri Dec 25 (Christmas GH) + Sat/Sun into a 4-day long weekend for just 1 credit."
      ),
      buildWindowRecommendation(
        id = "db_opt_pongal_4d_2027",
        title = "2027 Pongal + Thiruvalluvar + Uzhavar Thirunal 4-Day Stretch",
        startIso = "2027-01-14",
        endIso = "2027-01-17",
        whyOptimal = "Zero-credit/minimal-credit 4-day festival weekend: Thu Jan 14 (Pongal GH) + Fri Jan 15 (Thiruvalluvar Day GH) + Sat Jan 16 (Uzhavar Thirunal GH) + Sun Jan 17."
      ),
      buildWindowRecommendation(
        id = "db_opt_karthigai_4d_2026",
        title = "Karthigai Deepam 4-Day Long Weekend Bridge",
        startIso = "2026-11-21",
        endIso = "2026-11-24",
        whyOptimal = "Bridges Sat Nov 21 & Sun Nov 22 with Mon Nov 23 (1 COL from unused GH duty) + Tue Nov 24 (Karthigai Deepam RH) for a 4-day break without touching CL."
      ),
      buildWindowRecommendation(
        id = "db_opt_year_end_9d_2026",
        title = "9-Day Christmas → New Year Grand Finale Bridge",
        startIso = "2026-12-24",
        endIso = "2027-01-01",
        whyOptimal = "Prevents Dec 31 CL/RH lapse while unlocking 9 consecutive days off (Dec 24 – Jan 1) using 5 free GH/Weekend days + 4 leave credits."
      ),
      buildWindowRecommendation(
        id = "db_opt_deepavali_4d_2026",
        title = "Deepavali & Post-Festival 4-Day Recovery Stretch",
        startIso = "2026-11-07",
        endIso = "2026-11-10",
        whyOptimal = "Combines Deepavali weekend (Nov 7–8) with Mon Nov 9 & Tue Nov 10 to create a 4-day recovery block."
      )
    ).sortedByDescending { it.efficiencyMultiplier }
  }

  suspend fun generateOptimalRecommendations(
    focusGoal: String,
    summary2026: YearCreditSummary,
    colGhAuditAllYears: ColGhAuditSummary,
    records2026: List<LeaveRecord>
  ): Result<AiRecommendationUiState.Success> {
    val allHolidaysCount =
      InitialLeaveSeedData.tnGovtPublicHolidays2026.size +
        InitialLeaveSeedData.tnRestrictedHolidays2026.size +
        TnHolidays2027Catalog.tnGovtPublicHolidays2027.size +
        TnHolidays2027Catalog.tnRestrictedHolidays2027.size

    val deterministicCandidates = scanHolidayDatabaseForLongWeekends(
      summary2026 = summary2026,
      colGhAuditAllYears = colGhAuditAllYears,
      records = records2026
    )

    return try {
      val unusedGhDates = colGhAuditAllYears.unusedGhRecords.joinToString(", ") {
        "${it.dateIso} (${it.notes.ifBlank { "Hospital GH Duty" }})"
      }.ifBlank { "None currently unclaimed" }

      val alreadyLogged2026 = records2026
        .sortedBy { it.dateIso }
        .joinToString("; ") { "${it.dateIso}: ${it.category.code} (${it.notes})" }
        .ifBlank { "None logged yet" }

      val ghListText = (InitialLeaveSeedData.tnGovtPublicHolidays2026 + TnHolidays2027Catalog.tnGovtPublicHolidays2027.take(6))
        .joinToString("\n") {
          "- ${it.dateIso} (${it.dayOfWeek}): ${it.nameEnglish} / ${it.nameTamil} [TN Public Holiday / GH]"
        }

      val rhListText = (InitialLeaveSeedData.tnRestrictedHolidays2026 + TnHolidays2027Catalog.tnRestrictedHolidays2027.take(4))
        .joinToString("\n") {
          "- ${it.dateIso} (${it.dayOfWeek}): ${it.nameEnglish} / ${it.nameTamil} [Official TN RH]"
        }

      val preScannedText = deterministicCandidates.joinToString("\n") { cand ->
        "* ${cand.title} (${cand.startDateIso} to ${cand.endDateIso}): ${cand.totalDaysOff}d off for ${cand.leaveCreditsUsed} credit(s) [${cand.efficiencyMultiplier}x]. Apply on: ${cand.exactDatesToApply.joinToString("; ")}"
      }

      val prompt = """
        Analyze the synced Tamil Nadu Government Holiday Database (Room DB) and the officer's remaining leave credits to suggest the 4 most efficient long-weekend leave plans for HOD, Department of Psychiatry (Government Erode Medical College & Hospital).

        SELECTED OPTIMIZATION STRATEGY:
        "$focusGoal"

        LIVE REMAINING LEAVE CREDITS (2026):
        - Casual Leave (CL) Remaining: ${summary2026.clRemaining} of ${summary2026.clQuota} days (Used: ${summary2026.clTaken})
        - Restricted Holiday (RH) Remaining: ${summary2026.rhRemaining} of ${summary2026.rhQuota} days (Used: ${summary2026.rhTaken})
        - Unused GH Worked Credits Ready to Avail as COL: ${colGhAuditAllYears.availableUnusedGhCount} credits ($unusedGhDates)
        - 2026 COL Taken: ${summary2026.colTaken} | 2026 GH Worked Logged: ${summary2026.ghWorked}
        - 2026 Earned Leave (EL) Taken: ${summary2026.elTaken}d | Medical Leave (ML) Taken: ${summary2026.mlTaken}d

        EXISTING 2026 LEAVES ALREADY LOGGED BY OFFICER:
        $alreadyLogged2026

        ALGORITHMIC PRE-SCANNED LONG-WEEKEND CANDIDATES FROM HOLIDAY DB:
        $preScannedText

        SYNCED TAMIL NADU GOVERNMENT PUBLIC HOLIDAYS (GH - 0 Credits Used):
        $ghListText

        SYNCED TAMIL NADU RESTRICTED HOLIDAYS (RH / RL LIST - Uses RH Quota):
        $rhListText

        INSTRUCTIONS:
        1. Suggest 4 high-efficiency long-weekend bridges where applying for 1 to 4 leave days unlocks 4 to 9 consecutive days off.
        2. Explicitly list in `exactDatesToApply` the exact dates and leave types (RH, COL, or CL) the user should apply for.
        3. Ensure every recommendation strictly obeys the TN 10-day maximum continuous absence rule (totalDaysOff <= 10).
        4. Calculate `efficiencyMultiplier` as `totalDaysOff / leaveCreditsUsed` (round to 1 decimal place) and `freeDaysUnlocked` as the count of Weekend + GH days inside the span.
        5. Provide `projectedBalanceAfter` showing how many CL, RH, and unused GH credits remain after applying for that long weekend.
        6. Include a complete chronological `dailyPlan` for every date from `startDateIso` to `endDateIso` using creditType in ["WEEKEND", "GH", "RH", "COL", "CL"].
      """.trimIndent()

      val responseText = withTimeout(60_000L) {
        val response = generativeModel.generateContent(prompt)
        response.text ?: throw IllegalStateException("Empty structured response from Gemini.")
      }

      val parsed = parseStructuredRecommendations(
        focusGoal = focusGoal,
        jsonString = responseText,
        analyzedHolidayCount = allHolidaysCount
      )
      Result.success(parsed)
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "AI Leave Optimizer timed out", e)
      Result.failure(IllegalStateException("AI Leave Optimizer timed out after 60s. Please tap Generate again."))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "AI Leave Optimizer generation failed", e)
      Result.failure(e)
    }
  }

  private fun parseStructuredRecommendations(
    focusGoal: String,
    jsonString: String,
    analyzedHolidayCount: Int
  ): AiRecommendationUiState.Success {
    val root = JSONObject(jsonString)
    val executiveSummary = root.optString(
      "executiveSummary",
      "Optimized long-weekend leave bridges combining Tamil Nadu Government Holidays, Restricted Holidays (RH), and unused GH Worked (COL) credits."
    )
    val recArray = root.optJSONArray("recommendations")
    val list = mutableListOf<AiLeaveRecommendation>()

    if (recArray != null) {
      for (i in 0 until recArray.length()) {
        val item = recArray.optJSONObject(i) ?: continue
        val dailyArray = item.optJSONArray("dailyPlan")
        val dailySteps = mutableListOf<AiBridgeDayPlan>()
        if (dailyArray != null) {
          for (j in 0 until dailyArray.length()) {
            val stepObj = dailyArray.optJSONObject(j) ?: continue
            dailySteps.add(
              AiBridgeDayPlan(
                dateIso = stepObj.optString("dateIso", ""),
                dayLabel = stepObj.optString("dayLabel", ""),
                creditType = stepObj.optString("creditType", "CL"),
                description = stepObj.optString("description", "")
              )
            )
          }
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
            .filter { it.creditType.uppercase() in setOf("RH", "COL", "CL") }
            .forEach { step ->
              exactApplyList.add("${step.dayLabel} (${step.dateIso}) — Apply 1 ${step.creditType.uppercase()}")
            }
        }

        val totalDays = item.optInt("totalDaysOff", dailySteps.size.coerceAtLeast(1))
        val creditsUsed = item.optDouble("leaveCreditsUsed", 1.0).coerceAtLeast(0.5)
        val freeUnlocked = item.optInt(
          "freeDaysUnlocked",
          dailySteps.count { it.creditType.uppercase() == "GH" || it.creditType.uppercase() == "WEEKEND" }
        )
        val multiplier = item.optDouble(
          "efficiencyMultiplier",
          ((totalDays / creditsUsed) * 10.0).roundToInt() / 10.0
        )

        list.add(
          AiLeaveRecommendation(
            id = item.optString("id", "ai_rec_$i"),
            title = item.optString("title", "Optimal Long-Weekend Bridge #${i + 1}"),
            startDateIso = item.optString("startDateIso", dailySteps.firstOrNull()?.dateIso ?: "2026-12-24"),
            endDateIso = item.optString("endDateIso", dailySteps.lastOrNull()?.dateIso ?: "2026-12-27"),
            totalDaysOff = totalDays,
            leaveCreditsUsed = creditsUsed,
            freeDaysUnlocked = freeUnlocked,
            efficiencyMultiplier = multiplier,
            exactDatesToApply = exactApplyList,
            creditMixSummary = item.optString("creditMixSummary", ""),
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

    return AiRecommendationUiState.Success(
      focusGoal = focusGoal,
      executiveSummary = executiveSummary,
      recommendations = list,
      analyzedHolidayCount = analyzedHolidayCount
    )
  }
}
