package com.example.domain

import android.content.Context
import com.example.data.ApprovalStage
import com.example.data.DutyShift
import com.example.data.DutyShiftType
import com.example.data.LeaveCategory
import com.example.data.LeaveRecord
import com.example.data.LeaveWorkflowMeta
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class WatchdogColUrgency(val label: String) {
  EXPIRED("Expired"),
  CRITICAL("Critical (≤21d)"),
  EXPIRING_SOON("Expiring Soon (≤60d)"),
  HEALTHY("Safe (>60d)")
}

enum class BurnDownRiskLevel(val label: String) {
  HIGH_LAPSE_RISK("High Lapse Risk"),
  PACE_UP("Pace Up Usage"),
  ON_TRACK("On Track")
}

enum class PaperworkAlertType(val label: String) {
  JOINING_REPORT_DUE("Joining Report Due"),
  SUBMIT_TO_HOD_SOON("Submit to HOD Soon"),
  SANCTION_PENDING("Await HOD Sanction")
}

data class ColExpiryItem(
  val recordId: String,
  val ghDateIso: String,
  val expiryDateIso: String,
  val daysRemaining: Long,
  val urgency: WatchdogColUrgency,
  val notes: String
)

data class QuotaBurnDownMetrics(
  val year: Int,
  val clUsed: Double,
  val clLimit: Double,
  val clRemaining: Double,
  val clRiskLevel: BurnDownRiskLevel,
  val rlUsed: Double,
  val rlLimit: Double,
  val rlRemaining: Double,
  val rlRiskLevel: BurnDownRiskLevel,
  val daysRemainingInYear: Long,
  val monthsRemainingInYear: Double,
  val recommendedClPerMonth: Double
)

data class PendingPaperworkAlert(
  val recordId: String,
  val leaveType: String,
  val dateIso: String,
  val alertType: PaperworkAlertType,
  val headline: String,
  val actionLabel: String
)

data class ExpiryWatchdogSummary(
  val expiringCols: List<ColExpiryItem>,
  val criticalColCount: Int,
  val expiringSoonColCount: Int,
  val expiredColCount: Int,
  val archivedLapsedCount: Int = 0,
  val burnDown: QuotaBurnDownMetrics,
  val paperworkAlerts: List<PendingPaperworkAlert>
) {
  val totalUrgentAlerts: Int
    get() = criticalColCount + expiringSoonColCount + expiredColCount + paperworkAlerts.size
}

/**
 * Persists user-cleared / archived lapsed (>6-month) GH-Worked credit keys per user
 * so historical duty entries stay intact in the Service Register ledger while no longer
 * cluttering the active 6-Month COL Expiry Watchdog.
 */
object LapsedColArchiveStore {
  private const val PREFS_NAME = "lapsed_col_archive_prefs"
  private const val KEY_PREFIX = "cleared_lapsed_keys_"

  fun getClearedKeys(context: Context, userId: String): Set<String> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getStringSet("$KEY_PREFIX$userId", emptySet())?.toSet().orEmpty()
  }

  fun clearItems(context: Context, userId: String, items: Collection<ColExpiryItem>): Set<String> {
    if (items.isEmpty()) return getClearedKeys(context, userId)
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val updated = getClearedKeys(context, userId).toMutableSet()
    items.forEach { item ->
      if (item.recordId.isNotBlank()) updated.add(item.recordId)
      if (item.ghDateIso.isNotBlank()) updated.add(item.ghDateIso)
    }
    prefs.edit().putStringSet("$KEY_PREFIX$userId", updated).apply()
    return updated.toSet()
  }

  fun restoreAll(context: Context, userId: String): Set<String> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().remove("$KEY_PREFIX$userId").apply()
    return emptySet()
  }
}

data class DutyConflictWarning(
  val shiftId: String,
  val dateIso: String,
  val shiftType: DutyShiftType,
  val departmentNote: String,
  val isHardConflict: Boolean
)

data class GhColCaptureCandidate(
  val shift: DutyShift,
  val holidayLabel: String
)

/**
 * Deterministic Tamil Nadu Government COL 6-month (180-day) expiry watchdog,
 * year-end CL/RL burn-down pacing advisor, and leave paperwork alert engine.
 */
object ExpiryWatchdogEngine {

  private val isoFmt: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

  fun evaluate(
    records: List<LeaveRecord>,
    workflowsByRecordId: Map<String, LeaveWorkflowMeta>,
    today: LocalDate = LocalDate.now(),
    clearedLapsedRecordKeys: Set<String> = emptySet()
  ): ExpiryWatchdogSummary {
    val activeYear = today.year.coerceIn(2026, 2030)
    val audit = LeaveOptimizerEngine.auditColAndGhWorked(records)

    // 1. Evaluate every unclaimed GH_WORKED credit against TN Govt 6-month COL validity rule
    var archivedLapsedCount = 0
    val expiringCols = audit.unusedGhRecords.mapNotNull { rec ->
      val ghDate = runCatching { LocalDate.parse(rec.dateIso, isoFmt) }.getOrNull() ?: return@mapNotNull null
      val expiryDate = ghDate.plusMonths(6)
      val daysLeft = ChronoUnit.DAYS.between(today, expiryDate)
      val urgency = when {
        daysLeft < 0L -> WatchdogColUrgency.EXPIRED
        daysLeft <= 21L -> WatchdogColUrgency.CRITICAL
        daysLeft <= 60L -> WatchdogColUrgency.EXPIRING_SOON
        else -> WatchdogColUrgency.HEALTHY
      }
      if (urgency == WatchdogColUrgency.EXPIRED &&
        (rec.id in clearedLapsedRecordKeys || rec.dateIso in clearedLapsedRecordKeys)
      ) {
        archivedLapsedCount++
        return@mapNotNull null
      }
      ColExpiryItem(
        recordId = rec.id,
        ghDateIso = rec.dateIso,
        expiryDateIso = expiryDate.format(isoFmt),
        daysRemaining = daysLeft,
        urgency = urgency,
        notes = rec.notes.ifBlank { "Govt. Holiday Worked Credit" }
      )
    }.sortedBy { it.daysRemaining }

    // 2. Evaluate CL (12/yr) and RH/RL (3/yr) year-end burn-down pacing
    val yearRecords = records.filter { it.year.toInt() == activeYear }
    val clUsed = yearRecords
      .filter { it.category == LeaveCategory.CL }
      .sumOf { it.dayValue }
    val rlUsed = yearRecords
      .filter { it.category == LeaveCategory.RH }
      .sumOf { it.dayValue }

    val clLimit = 12.0
    val rlLimit = 3.0
    val clRemaining = (clLimit - clUsed).coerceAtLeast(0.0)
    val rlRemaining = (rlLimit - rlUsed).coerceAtLeast(0.0)

    val yearEnd = LocalDate.of(activeYear, 12, 31)
    val daysLeftInYear = ChronoUnit.DAYS.between(today, yearEnd).coerceAtLeast(1L)
    val monthsLeft = (daysLeftInYear / 30.4).coerceAtLeast(0.25)
    val clPerMonthNeeded = clRemaining / monthsLeft

    val clRisk = when {
      clRemaining >= 2.0 && daysLeftInYear <= 45L -> BurnDownRiskLevel.HIGH_LAPSE_RISK
      clRemaining >= 4.0 && daysLeftInYear <= 95L -> BurnDownRiskLevel.HIGH_LAPSE_RISK
      clPerMonthNeeded >= 1.5 && clRemaining >= 2.0 -> BurnDownRiskLevel.PACE_UP
      else -> BurnDownRiskLevel.ON_TRACK
    }
    val rlRisk = when {
      rlRemaining >= 1.0 && daysLeftInYear <= 60L -> BurnDownRiskLevel.HIGH_LAPSE_RISK
      rlRemaining >= 2.0 && daysLeftInYear <= 130L -> BurnDownRiskLevel.PACE_UP
      else -> BurnDownRiskLevel.ON_TRACK
    }

    val burnDown = QuotaBurnDownMetrics(
      year = activeYear,
      clUsed = clUsed,
      clLimit = clLimit,
      clRemaining = clRemaining,
      clRiskLevel = clRisk,
      rlUsed = rlUsed,
      rlLimit = rlLimit,
      rlRemaining = rlRemaining,
      rlRiskLevel = rlRisk,
      daysRemainingInYear = daysLeftInYear,
      monthsRemainingInYear = monthsLeft,
      recommendedClPerMonth = clPerMonthNeeded
    )

    // 3. Identify actionable paperwork reminders (missing Joining Reports or unsubmitted upcoming leaves)
    val leaveTypesNeedingPaperwork = setOf(
      LeaveCategory.CL,
      LeaveCategory.COL,
      LeaveCategory.EL,
      LeaveCategory.ML,
      LeaveCategory.RH
    )
    val paperworkAlerts = mutableListOf<PendingPaperworkAlert>()
    records
      .filter { it.category in leaveTypesNeedingPaperwork }
      .sortedByDescending { it.dateIso }
      .forEach { rec ->
        val recDate = runCatching { LocalDate.parse(rec.dateIso, isoFmt) }.getOrNull() ?: return@forEach
        val wf = workflowsByRecordId[rec.id]
        val stage = wf?.stageEnum ?: ApprovalStage.PLANNED
        val daysFromToday = ChronoUnit.DAYS.between(today, recDate)

        // Past EL / ML or explicitly SANCTIONED leave without Joining Report filed (within last 45 days)
        val needsJoiningReport = daysFromToday in -45L..-1L &&
          stage != ApprovalStage.JOINING_FILED &&
          (stage == ApprovalStage.SANCTIONED || rec.category == LeaveCategory.EL || rec.category == LeaveCategory.ML)

        if (needsJoiningReport) {
          paperworkAlerts.add(
            PendingPaperworkAlert(
              recordId = rec.id,
              leaveType = rec.leaveType,
              dateIso = rec.dateIso,
              alertType = PaperworkAlertType.JOINING_REPORT_DUE,
              headline = "${rec.leaveType} (${rec.dateIso}) ended — Joining Report pending",
              actionLabel = "File Joining"
            )
          )
        } else if (daysFromToday in 0L..14L && stage == ApprovalStage.PLANNED) {
          paperworkAlerts.add(
            PendingPaperworkAlert(
              recordId = rec.id,
              leaveType = rec.leaveType,
              dateIso = rec.dateIso,
              alertType = PaperworkAlertType.SUBMIT_TO_HOD_SOON,
              headline = "Upcoming ${rec.leaveType} on ${rec.dateIso} not marked Submitted",
              actionLabel = "Track Slip"
            )
          )
        } else if (daysFromToday in 0L..7L && stage == ApprovalStage.SUBMITTED) {
          paperworkAlerts.add(
            PendingPaperworkAlert(
              recordId = rec.id,
              leaveType = rec.leaveType,
              dateIso = rec.dateIso,
              alertType = PaperworkAlertType.SANCTION_PENDING,
              headline = "${rec.leaveType} (${rec.dateIso}) awaiting HOD sanction slip",
              actionLabel = "Attach Slip"
            )
          )
        }
      }

    return ExpiryWatchdogSummary(
      expiringCols = expiringCols,
      criticalColCount = expiringCols.count { it.urgency == WatchdogColUrgency.CRITICAL },
      expiringSoonColCount = expiringCols.count { it.urgency == WatchdogColUrgency.EXPIRING_SOON },
      expiredColCount = expiringCols.count { it.urgency == WatchdogColUrgency.EXPIRED },
      archivedLapsedCount = archivedLapsedCount,
      burnDown = burnDown,
      paperworkAlerts = paperworkAlerts.take(6)
    )
  }
}

/**
 * Pure domain engine for clinical duty roster conflicts and automatic GH-Worked → +1 COL capture eligibility.
 */
object DutyConflictEngine {

  private val isoFmt: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

  fun findDutyConflicts(
    candidateDatesIso: Collection<String>,
    dutyShifts: List<DutyShift>
  ): List<DutyConflictWarning> {
    if (candidateDatesIso.isEmpty() || dutyShifts.isEmpty()) return emptyList()
    val dateSet = candidateDatesIso.toSet()
    return dutyShifts
      .filter { it.dateIso in dateSet }
      .map { shift ->
        val type = shift.shiftTypeEnum
        DutyConflictWarning(
          shiftId = shift.id,
          dateIso = shift.dateIso,
          shiftType = type,
          departmentNote = shift.departmentNote,
          isHardConflict = type.isHardConflict
        )
      }
      .sortedBy { it.dateIso }
  }

  /**
   * Returns a capture candidate if a logged duty shift falls on a Tamil Nadu Public Holiday (GH)
   * or Sunday (or is explicitly tagged GH_EMERGENCY) and has not yet been claimed as GH_WORKED.
   */
  fun findUnclaimedGhColCandidates(
    dutyShifts: List<DutyShift>,
    existingRecords: List<LeaveRecord>
  ): List<GhColCaptureCandidate> {
    val existingGhDates = existingRecords
      .filter { it.category == LeaveCategory.GH_WORKED }
      .map { it.dateIso }
      .toSet()

    val publicHolidayByDate = TnHolidays2027Catalog.allSyncedPublicHolidays()
      .associateBy { it.dateIso }

    return dutyShifts.mapNotNull { shift ->
      if (shift.colClaimed || shift.dateIso in existingGhDates) return@mapNotNull null
      val parsed = runCatching { LocalDate.parse(shift.dateIso, isoFmt) }.getOrNull() ?: return@mapNotNull null
      val ghItem = publicHolidayByDate[shift.dateIso]
      val isSunday = parsed.dayOfWeek == DayOfWeek.SUNDAY
      val isExplicitGhDuty = shift.shiftTypeEnum == DutyShiftType.GH_EMERGENCY

      when {
        ghItem != null -> GhColCaptureCandidate(shift, "TN Govt Holiday: ${ghItem.nameEnglish}")
        isExplicitGhDuty -> GhColCaptureCandidate(shift, "GH Emergency Duty")
        isSunday -> GhColCaptureCandidate(shift, "Sunday Hospital Duty")
        else -> null
      }
    }.sortedByDescending { it.shift.dateIso }
  }

  fun findUnclaimedGhCaptureForDate(
    dateIso: String,
    dutyShifts: List<DutyShift>,
    existingRecords: List<LeaveRecord>
  ): GhColCaptureCandidate? {
    return findUnclaimedGhColCandidates(dutyShifts, existingRecords)
      .firstOrNull { it.shift.dateIso == dateIso }
  }

  fun extractProtectedDutyDates(dutyShifts: List<DutyShift>): Set<String> {
    return dutyShifts
      .filter { it.shiftTypeEnum.isHardConflict }
      .map { it.dateIso }
      .toSet()
  }
}

/**
 * Generates authentic Tamil Nadu Government Return-to-Duty Joining Reports,
 * concise WhatsApp/SMS clinical handover notes, and RFC-5545 `.ics` calendar invites.
 */
object JoiningReportAndShareGenerator {

  private val isoFmt: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
  private val displayFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy (EEE)", Locale.ENGLISH)
  private val icsDateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.US)

  fun formatDisplayDate(dateIso: String): String {
    return runCatching { LocalDate.parse(dateIso, isoFmt).format(displayFmt) }.getOrDefault(dateIso)
  }

  fun computeDefaultJoiningDateIso(leaveEndDateIso: String): String {
    val end = runCatching { LocalDate.parse(leaveEndDateIso, isoFmt) }.getOrNull()
      ?: return LocalDate.now().format(isoFmt)
    val ghSet = TnHolidays2027Catalog.allSyncedPublicHolidays().map { it.dateIso }.toSet()
    var next = end.plusDays(1)
    var guard = 0
    while (guard < 10 && (next.dayOfWeek == DayOfWeek.SUNDAY || next.format(isoFmt) in ghSet)) {
      next = next.plusDays(1)
      guard++
    }
    return next.format(isoFmt)
  }

  fun generateOfficialJoiningReportText(
    officerName: String,
    designation: String,
    department: String,
    institution: String,
    record: LeaveRecord,
    workflow: LeaveWorkflowMeta?
  ): String {
    val joinDateIso = workflow?.joiningDateIso?.takeIf { it.isNotBlank() }
      ?: computeDefaultJoiningDateIso(record.dateIso)
    val session = workflow?.joiningSession?.takeIf { it == "FN" || it == "AN" } ?: "FN"
    val sessionLong = if (session == "FN") "Forenoon (F.N.)" else "Afternoon (A.N.)"
    val orderRef = workflow?.sanctionOrderNo?.takeIf { it.isNotBlank() }
      ?: "Leave Application / HOD Sanction dated ${formatDisplayDate(record.dateIso)}"
    val reliever = workflow?.relievingOfficer?.takeIf { it.isNotBlank() }
      ?: "Department Duty Medical Officer"
    val cat = record.category
    val durationText = if (record.isHalfDay) "0.5 day (Half-Day)" else "1 day"

    return buildString {
      appendLine("JOINING REPORT (RETURN TO DUTY)")
      appendLine("TAMIL NADU GOVERNMENT MEDICAL SERVICE")
      appendLine("══════════════════════════════════════════════")
      appendLine("Date: ${formatDisplayDate(joinDateIso)}")
      appendLine()
      appendLine("From:")
      appendLine("  $officerName")
      appendLine("  $designation, Dept. of $department")
      appendLine("  $institution")
      appendLine()
      appendLine("To:")
      appendLine("  The Dean / Medical Superintendent,")
      appendLine("  $institution")
      appendLine("  (Through: The Head of Department, Dept. of $department)")
      appendLine()
      appendLine("Respected Sir / Madam,")
      appendLine()
      appendLine("Sub: Establishment – Tamil Nadu Medical Service – Returning to duty after availing ${cat.fullTitle} (${cat.shortLabel}) – Joining Report submitted – Reg.")
      appendLine("Ref: $orderRef")
      appendLine()
      appendLine("With reference to the above, I have the honour to report myself for duty on the $sessionLong of ${formatDisplayDate(joinDateIso)} after availing $durationText of ${cat.fullTitle} (${cat.shortLabel}) on ${formatDisplayDate(record.dateIso)}.")
      if (record.pairedDateIso.isNotBlank()) {
        appendLine("Compensatory Off Reference: In lieu of GH Duty worked on ${formatDisplayDate(record.pairedDateIso)}.")
      }
      appendLine()
      appendLine("I have resumed full clinical, OPD, and ward charges from $reliever. Kindly accept my joining report and make necessary entries in the Service Register / Attendance Ledger.")
      appendLine()
      appendLine("Thanking you,")
      appendLine("Yours faithfully,")
      appendLine()
      appendLine("($officerName)")
      appendLine("$designation, Dept. of $department")
      appendLine(institution)
      appendLine()
      appendLine("Forwarded & Verified by HOD: ____________________")
    }.trim()
  }

  fun generateClinicalHandoverMessage(
    officerName: String,
    designation: String,
    department: String,
    institution: String,
    leaveLabel: String,
    startDateIso: String,
    endDateIso: String,
    workflow: LeaveWorkflowMeta? = null
  ): String {
    val joinDateIso = workflow?.joiningDateIso?.takeIf { it.isNotBlank() }
      ?: computeDefaultJoiningDateIso(endDateIso)
    val session = workflow?.joiningSession?.takeIf { it == "FN" || it == "AN" } ?: "FN"
    val reliever = workflow?.relievingOfficer?.takeIf { it.isNotBlank() } ?: "Unit Duty Officer"
    val orderRef = workflow?.sanctionOrderNo?.takeIf { it.isNotBlank() } ?: "Pending Office Ref"
    val rangeText = if (startDateIso == endDateIso) {
      formatDisplayDate(startDateIso)
    } else {
      "${formatDisplayDate(startDateIso)} → ${formatDisplayDate(endDateIso)}"
    }

    return buildString {
      appendLine("*Clinical Charge Handover — $institution*")
      appendLine("• Officer: $officerName ($designation, $department)")
      appendLine("• Leave: $leaveLabel")
      appendLine("• Dates: $rangeText")
      appendLine("• Return to Duty: ${formatDisplayDate(joinDateIso)} ($session)")
      appendLine("• Relieving Coverage: $reliever")
      appendLine("• Sanction Ref: $orderRef")
      if (!workflow?.workflowNotes.isNullOrBlank()) {
        appendLine("• Clinical Note: ${workflow?.workflowNotes}")
      }
      append("Sent via TN MedLeave Pro")
    }
  }

  fun buildIcsCalendarContent(
    title: String,
    startDateIso: String,
    endDateIso: String,
    description: String,
    location: String
  ): String {
    val start = runCatching { LocalDate.parse(startDateIso, isoFmt) }.getOrElse { LocalDate.now() }
    val end = runCatching { LocalDate.parse(endDateIso, isoFmt) }.getOrElse { start }
    // RFC-5545 all-day DTEND is exclusive (day after last day)
    val dtStart = start.format(icsDateFmt)
    val dtEndExclusive = end.plusDays(1).format(icsDateFmt)
    val uid = "tnmedleave-${dtStart}-${dtEndExclusive}@gemch.tn.gov.in"

    fun escapeIcs(raw: String): String = raw
      .replace("\\", "\\\\")
      .replace(",", "\\,")
      .replace(";", "\\;")
      .replace("\n", "\\n")

    return buildString {
      append("BEGIN:VCALENDAR\r\n")
      append("VERSION:2.0\r\n")
      append("PRODID:-//TN MedLeave Pro//Tamil Nadu Govt Medical Service//EN\r\n")
      append("CALSCALE:GREGORIAN\r\n")
      append("BEGIN:VEVENT\r\n")
      append("UID:$uid\r\n")
      append("DTSTART;VALUE=DATE:$dtStart\r\n")
      append("DTEND;VALUE=DATE:$dtEndExclusive\r\n")
      append("SUMMARY:${escapeIcs(title)}\r\n")
      append("DESCRIPTION:${escapeIcs(description)}\r\n")
      append("LOCATION:${escapeIcs(location)}\r\n")
      append("END:VEVENT\r\n")
      append("END:VCALENDAR\r\n")
    }
  }
}
