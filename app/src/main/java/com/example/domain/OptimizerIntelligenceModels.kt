package com.example.domain

import android.content.Context
import com.example.data.LeaveCategory
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

enum class OptimizerHorizon(val label: String, val shortChip: String) {
  NEXT_60_DAYS("Next 60 Days", "60d"),
  REST_OF_2026("Rest of 2026", "2026 Left"),
  Q1_Q2_2027("H1 2027 (Jan–Jun)", "H1 2027"),
  FULL_HORIZON("Full 2026–2027", "All '26–27")
}

enum class WeekendPolicy(val label: String, val shortChip: String) {
  SECOND_SATURDAY_AND_SUNDAY("2nd Sat & Sun (TN Govt)", "2nd Sat+Sun"),
  ALL_SATURDAYS_AND_SUNDAY("All Sat & Sun", "Sat+Sun"),
  SUNDAY_ONLY("Sundays Only", "Sun Only");

  fun isWeekend(date: LocalDate): Boolean = when (this) {
    SECOND_SATURDAY_AND_SUNDAY ->
      date.dayOfWeek == DayOfWeek.SUNDAY ||
        (date.dayOfWeek == DayOfWeek.SATURDAY && date.dayOfMonth in 8..14)
    ALL_SATURDAYS_AND_SUNDAY ->
      date.dayOfWeek == DayOfWeek.SUNDAY || date.dayOfWeek == DayOfWeek.SATURDAY
    SUNDAY_ONLY ->
      date.dayOfWeek == DayOfWeek.SUNDAY
  }
}

enum class SpreadMode(val label: String, val shortChip: String, val penalty: Double) {
  BALANCED("Balanced ROI", "Balanced", 1.5),
  SPREAD_EVENLY("Spread Evenly", "Spread", 3.0)
}

enum class ProximityMode(val label: String, val shortChip: String) {
  BRIDGE("Bridge Weekends", "Bridge"),
  MID_WEEK("Mid-Week Breaks", "Mid-Week")
}

data class OptimizerConstraints(
  val reserveClFloor: Double = 0.0,
  val allowElBridges: Boolean = false,
  val allowHalfDayCl: Boolean = true,
  val strictZeroCl: Boolean = false,
  val prioritizeExpiringCol: Boolean = true,
  val includeSaturdays: Boolean = false,
  val enforce10DayCap: Boolean = true,
  val horizonMode: OptimizerHorizon = OptimizerHorizon.FULL_HORIZON,
  val protectedDaysOfWeek: Set<DayOfWeek> = emptySet(),
  val protectedDatesIso: Set<String> = emptySet(),
  val customNaturalPrompt: String = "",
  val weekendPolicy: WeekendPolicy = WeekendPolicy.SECOND_SATURDAY_AND_SUNDAY,
  val spreadMode: SpreadMode = SpreadMode.BALANCED,
  val proximityMode: ProximityMode = ProximityMode.BRIDGE,
  val blockedDatesIso: Set<String> = emptySet(),
  val pinnedDates: Map<String, String> = emptyMap(), // YYYY-MM-DD -> "AUTO" | "CL" | "RH" | "COL" | "EL"
  val plannedToWorkHolidays: Set<String> = emptySet(), // YYYY-MM-DD
  val hasPendingRecalc: Boolean = false
) {
  fun effectiveWeekendPolicy(): WeekendPolicy =
    if (includeSaturdays && weekendPolicy == WeekendPolicy.SECOND_SATURDAY_AND_SUNDAY) {
      WeekendPolicy.ALL_SATURDAYS_AND_SUNDAY
    } else {
      weekendPolicy
    }

  fun isWeekendDay(date: LocalDate): Boolean = effectiveWeekendPolicy().isWeekend(date)

  val hasSteeringOverrides: Boolean
    get() = blockedDatesIso.isNotEmpty() || pinnedDates.isNotEmpty() || plannedToWorkHolidays.isNotEmpty()

  fun buildStrategySummary(baseFocusTitle: String): String {
    val parts = mutableListOf<String>()
    if (baseFocusTitle.isNotBlank()) parts.add(baseFocusTitle)
    parts.add("Wknd: ${effectiveWeekendPolicy().shortChip}")
    if (spreadMode != SpreadMode.BALANCED) parts.add(spreadMode.label)
    if (proximityMode != ProximityMode.BRIDGE) parts.add(proximityMode.label)
    if (reserveClFloor > 0.0) {
      val clStr = if (reserveClFloor % 1.0 == 0.0) reserveClFloor.toInt().toString() else String.format(Locale.US, "%.1f", reserveClFloor)
      parts.add("Reserve ≥$clStr CL")
    }
    if (strictZeroCl) parts.add("Zero-CL (RH+COL Only)")
    if (allowElBridges) parts.add("Allow EL Prefix/Suffix")
    if (allowHalfDayCl) parts.add("0.5 CL Half-Day Allowed")
    if (prioritizeExpiringCol) parts.add("Expiring COL FIFO")
    if (protectedDaysOfWeek.isNotEmpty()) {
      val opd = protectedDaysOfWeek.sortedBy { it.value }.joinToString("/") {
        it.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
      }
      parts.add("Protect $opd OPD")
    }
    if (protectedDatesIso.isNotEmpty()) {
      parts.add("Shield ${protectedDatesIso.size} Roster Duty Date(s)")
    }
    if (pinnedDates.isNotEmpty()) parts.add("${pinnedDates.size} Pinned")
    if (blockedDatesIso.isNotEmpty()) parts.add("${blockedDatesIso.size} Blocked")
    if (plannedToWorkHolidays.isNotEmpty()) parts.add("+${plannedToWorkHolidays.size} Virtual COL")
    parts.add("Horizon: ${horizonMode.label}")
    if (customNaturalPrompt.isNotBlank()) {
      parts.add("Custom Ask: \"${customNaturalPrompt.trim()}\"")
    }
    return parts.joinToString(" • ")
  }
}

data class OptimizerSteeringSnapshot(
  val weekendPolicy: WeekendPolicy = WeekendPolicy.SECOND_SATURDAY_AND_SUNDAY,
  val spreadMode: SpreadMode = SpreadMode.BALANCED,
  val proximityMode: ProximityMode = ProximityMode.BRIDGE,
  val blockedDatesIso: Set<String> = emptySet(),
  val pinnedDates: Map<String, String> = emptyMap(),
  val plannedToWorkHolidays: Set<String> = emptySet()
)

object OptimizerSteeringStore {
  private const val PREFS_NAME = "tn_medleave_optimizer_steering_v1"
  private const val KEY_JSON = "steering_snapshot_json"

  fun read(context: Context): OptimizerSteeringSnapshot {
    val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getString(KEY_JSON, null) ?: return OptimizerSteeringSnapshot()
    return runCatching {
      val obj = JSONObject(raw)
      val weekend = runCatching {
        WeekendPolicy.valueOf(obj.optString("weekendPolicy", WeekendPolicy.SECOND_SATURDAY_AND_SUNDAY.name))
      }.getOrDefault(WeekendPolicy.SECOND_SATURDAY_AND_SUNDAY)
      val spread = runCatching {
        SpreadMode.valueOf(obj.optString("spreadMode", SpreadMode.BALANCED.name))
      }.getOrDefault(SpreadMode.BALANCED)
      val proximity = runCatching {
        ProximityMode.valueOf(obj.optString("proximityMode", ProximityMode.BRIDGE.name))
      }.getOrDefault(ProximityMode.BRIDGE)

      val blocked = linkedSetOf<String>()
      obj.optJSONArray("blockedDatesIso")?.let { arr ->
        for (i in 0 until arr.length()) {
          val v = arr.optString(i, "").trim()
          if (v.isNotEmpty()) blocked.add(v)
        }
      }

      val pinned = linkedMapOf<String, String>()
      obj.optJSONObject("pinnedDates")?.let { pinObj ->
        pinObj.keys().forEach { k ->
          val v = pinObj.optString(k, "AUTO").trim()
          if (k.isNotBlank() && v.isNotEmpty()) pinned[k] = v
        }
      }

      val workHolidays = linkedSetOf<String>()
      obj.optJSONArray("plannedToWorkHolidays")?.let { arr ->
        for (i in 0 until arr.length()) {
          val v = arr.optString(i, "").trim()
          if (v.isNotEmpty()) workHolidays.add(v)
        }
      }

      OptimizerSteeringSnapshot(
        weekendPolicy = weekend,
        spreadMode = spread,
        proximityMode = proximity,
        blockedDatesIso = blocked,
        pinnedDates = pinned,
        plannedToWorkHolidays = workHolidays
      )
    }.getOrDefault(OptimizerSteeringSnapshot())
  }

  fun save(context: Context, constraints: OptimizerConstraints) {
    val obj = JSONObject().apply {
      put("weekendPolicy", constraints.effectiveWeekendPolicy().name)
      put("spreadMode", constraints.spreadMode.name)
      put("proximityMode", constraints.proximityMode.name)
      put("blockedDatesIso", JSONArray(constraints.blockedDatesIso.sorted()))
      put("pinnedDates", JSONObject().apply {
        constraints.pinnedDates.toSortedMap().forEach { (k, v) -> put(k, v) }
      })
      put("plannedToWorkHolidays", JSONArray(constraints.plannedToWorkHolidays.sorted()))
    }
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putString(KEY_JSON, obj.toString())
      .apply()
  }
}

data class PortfolioImpactSummary(
  val selectedPlanCount: Int,
  val totalDaysOffUnlocked: Int,
  val totalCreditsDebited: Double,
  val cl2026Used: Double,
  val rh2026Used: Int,
  val colUsed: Int,
  val elUsed: Int,
  val cl2027Used: Double,
  val rh2027Used: Int,
  val remainingCl2026: Double,
  val remainingRh2026: Int,
  val remainingColPool: Int,
  val remainingCl2027: Double,
  val remainingRh2027: Int,
  val dec2026LapseSavedDays: Double,
  val violatesClReserve: Boolean,
  val hasOverlappingDates: Boolean,
  val exceedsQuotaMessage: String? = null,
  val virtualColUsed: Int = 0
)

data class BuilderPreloadRequest(
  val requestId: Long = System.currentTimeMillis(),
  val title: String,
  val targetYear: Int,
  val targetMonth: Int,
  val draftBlocks: Map<String, LeaveCategory>
)

data class TentativeHoldRecord(
  val dateIso: String,
  val creditType: String, // "CL", "HALF_CL", "RH", "COL", "EL"
  val bridgeId: String,
  val bridgeTitle: String,
  val note: String,
  val createdAtMillis: Long = System.currentTimeMillis()
)

object TentativePlanStore {
  private const val PREFS_NAME = "tn_medleave_tentative_holds_prefs"
  private const val KEY_HOLDS_JSON = "tentative_holds_json_v1"

  fun readHolds(context: Context): Map<String, TentativeHoldRecord> {
    val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getString(KEY_HOLDS_JSON, null) ?: return emptyMap()
    return runCatching {
      val arr = JSONArray(raw)
      val out = linkedMapOf<String, TentativeHoldRecord>()
      for (i in 0 until arr.length()) {
        val obj = arr.optJSONObject(i) ?: continue
        val dateIso = obj.optString("dateIso", "").trim()
        if (dateIso.isNotEmpty()) {
          out[dateIso] = TentativeHoldRecord(
            dateIso = dateIso,
            creditType = obj.optString("creditType", "CL"),
            bridgeId = obj.optString("bridgeId", ""),
            bridgeTitle = obj.optString("bridgeTitle", "Planned Bridge"),
            note = obj.optString("note", ""),
            createdAtMillis = obj.optLong("createdAtMillis", 0L)
          )
        }
      }
      out
    }.getOrDefault(emptyMap())
  }

  fun saveHolds(context: Context, holds: Map<String, TentativeHoldRecord>) {
    val arr = JSONArray()
    holds.values.sortedBy { it.dateIso }.forEach { hold ->
      arr.put(
        JSONObject().apply {
          put("dateIso", hold.dateIso)
          put("creditType", hold.creditType)
          put("bridgeId", hold.bridgeId)
          put("bridgeTitle", hold.bridgeTitle)
          put("note", hold.note)
          put("createdAtMillis", hold.createdAtMillis)
        }
      )
    }
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putString(KEY_HOLDS_JSON, arr.toString())
      .apply()
  }
}

object HodSanctionLetterGenerator {

  fun generateOfficialApplicationAndHandover(
    rec: AiLeaveRecommendation,
    officerName: String,
    designation: String,
    department: String,
    college: String,
    ifhrmsId: String,
    referenceDate: LocalDate = LocalDate.now()
  ): String {
    val dateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.ENGLISH)
    val todayFormatted = referenceDate.format(dateFmt)
    val actionDays = rec.dailyPlan.filter {
      it.creditType.uppercase(Locale.US) in setOf("CL", "HALF_CL", "RH", "COL", "EL")
    }
    val hasEl = actionDays.any { it.creditType.equals("EL", ignoreCase = true) }
    val hasCol = actionDays.any { it.creditType.equals("COL", ignoreCase = true) }
    val hasRh = actionDays.any { it.creditType.equals("RH", ignoreCase = true) }
    val hasCl = actionDays.any {
      it.creditType.equals("CL", ignoreCase = true) || it.creditType.equals("HALF_CL", ignoreCase = true)
    }

    val leaveHeadings = buildList {
      if (hasEl) add("Earned Leave (EL)")
      if (hasCl) add("Casual Leave (CL)")
      if (hasRh) add("Restricted Holiday (RH)")
      if (hasCol) add("Compensatory Off Leave (COL)")
    }.ifEmpty { listOf("Leave") }.joinToString(" / ")

    val startFriendly = runCatching { LocalDate.parse(rec.startDateIso).format(dateFmt) }.getOrDefault(rec.startDateIso)
    val endFriendly = runCatching { LocalDate.parse(rec.endDateIso).format(dateFmt) }.getOrDefault(rec.endDateIso)
    val prefixSuffixNote = rec.dailyPlan
      .filter { it.creditType in setOf("GH", "WEEKEND") }
      .joinToString(", ") { "${it.dayLabel} (${it.creditType})" }

    return buildString {
      appendLine("OFFICE OF THE HEAD OF DEPARTMENT — $department")
      appendLine(college.uppercase(Locale.ENGLISH))
      appendLine("Date: $todayFormatted")
      appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
      appendLine()
      appendLine("From:")
      appendLine("  $officerName")
      appendLine("  $designation, $department")
      appendLine("  $college")
      appendLine("  IFHRMS / TNMS ID: $ifhrmsId")
      appendLine()
      appendLine("To:")
      appendLine("  The Dean,")
      appendLine("  $college")
      appendLine()
      appendLine("Respected Sir / Madam,")
      appendLine()
      appendLine("Sub: Request for grant of $leaveHeadings — ${rec.title} — Reg.")
      appendLine("Ref: Tamil Nadu Leave Rules, 1933 & FR Annexure VII.")
      appendLine()
      appendLine("  I kindly request permission to avail $leaveHeadings as detailed below (${rec.creditMixSummary}):")
      appendLine()
      actionDays.forEachIndexed { idx, step ->
        val dStr = runCatching { LocalDate.parse(step.dateIso).format(dateFmt) }.getOrDefault(step.dateIso)
        val unitLabel = if (step.isHalfDay || step.creditType == "HALF_CL") "0.5 Day (AN/FN)" else "1 Day"
        appendLine("  ${idx + 1}. $dStr (${step.dayLabel}) — ${step.creditType} [$unitLabel]: ${step.description}")
      }
      if (prefixSuffixNote.isNotBlank()) {
        appendLine()
        appendLine("  Permission is also requested to prefix / suffix the intervening & adjacent Holidays/Weekends:")
        appendLine("  $prefixSuffixNote")
      }
      appendLine()
      appendLine("  Total continuous absence period: $startFriendly to $endFriendly (${rec.totalDaysOff} days, compliant with ${if (hasEl) "TN Leave Rule 8" else "≤10-day FR Annexure VII limit"}). Station-leave permission is requested if applicable.")
      appendLine()
      appendLine("CHARGE HANDOVER ARRANGEMENT:")
      appendLine("  During the above period, routine Psychiatry OPD, Inpatient Ward Rounds, ECT/Emergency Liaison, and Medico-Legal calls will be held in-charge by the Senior Faculty Member / Duty Assistant Professor on rotation without disruption to patient care.")
      appendLine()
      appendLine("Thanking you,")
      appendLine("Yours faithfully,")
      appendLine("$officerName")
      appendLine("$designation • $department")
    }
  }
}
