package com.example.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class LeaveCategory(
  val code: String,
  val shortLabel: String,
  val fullTitle: String,
  val tnRuleSummary: String,
  val annualLimit: Int?
) {
  CL(
    code = "CL",
    shortLabel = "CL",
    fullTitle = "Casual Leave",
    tnRuleSummary = "12 days/calendar year. Max 10 days at a stretch including holidays.",
    annualLimit = 12
  ),
  COL(
    code = "COL",
    shortLabel = "COL",
    fullTitle = "Compensatory Off Leave",
    tnRuleSummary = "Taken against Government Holiday Worked (GH Worked) / In-Lieu Turn Duty. Must be availed within 6 months.",
    annualLimit = null
  ),
  IN_LIEU(
    code = "IN_LIEU",
    shortLabel = "In Lieu Of",
    fullTitle = "In Lieu Of (GH Duty Credited)",
    tnRuleSummary = "The specific GH Worked / Turn Duty date cited when availing a COL.",
    annualLimit = null
  ),
  PL(
    code = "PL",
    shortLabel = "PL",
    fullTitle = "Public Holiday Leave",
    tnRuleSummary = "Public Holiday off availed by the officer.",
    annualLimit = null
  ),
  GH_WORKED(
    code = "GH_WORKED",
    shortLabel = "GH Worked",
    fullTitle = "Govt. Holiday Worked",
    tnRuleSummary = "Hospital/HOD duty performed on a declared TN Govt Holiday; earns 1 COL credit.",
    annualLimit = null
  ),
  ML(
    code = "ML",
    shortLabel = "ML",
    fullTitle = "Medical Leave (UEL)",
    tnRuleSummary = "Unearned Leave on Medical Certificate (up to 540 days in entire service for permanent officers).",
    annualLimit = null
  ),
  RH(
    code = "RH",
    shortLabel = "RH",
    fullTitle = "Restricted Holiday",
    tnRuleSummary = "3 days per calendar year from the official TN Restricted Holiday (RL) list.",
    annualLimit = 3
  ),
  EL(
    code = "EL",
    shortLabel = "EL",
    fullTitle = "Earned Leave",
    tnRuleSummary = "Credited at 15 days per half-year (Jan 1 & Jul 1). Max accumulation 240 days.",
    annualLimit = 30
  ),
  ON_DUTY(
    code = "ON_DUTY",
    shortLabel = "On Duty",
    fullTitle = "Other Duty (OD)",
    tnRuleSummary = "Official medical college inspection, university exam, DME meeting, or conference.",
    annualLimit = null
  ),
  WEEK_OFF(
    code = "WEEK_OFF",
    shortLabel = "Week Off",
    fullTitle = "Weekly Off",
    tnRuleSummary = "Scheduled weekly off following continuous hospital call duty.",
    annualLimit = null
  );

  companion object {
    fun fromCode(code: String): LeaveCategory =
      entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: CL
  }
}

enum class LeaveStatus(val code: String, val displayLabel: String) {
  APPROVED("APPROVED", "Approved"),
  PENDING("PENDING", "Pending Approval"),
  VERIFIED_PAIRED("VERIFIED_PAIRED", "Verified & Paired (GH/COL)"),
  UNPAIRED_ALERT("UNPAIRED_ALERT", "Unpaired / Attention"),
  REJECTED("REJECTED", "Rejected"),
  CANCELLED("CANCELLED", "Cancelled");

  companion object {
    fun fromCode(code: String): LeaveStatus =
      entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: APPROVED
  }
}

/**
 * Primary Kotlin data class representing a single or multi-day user leave record
 * with leaveType, startDate, endDate, status, and paired duty metadata.
 * Every property defines a default value so Firestore `.toObject(LeaveEntry::class.java)`
 * can instantiate it via the synthetic no-arg constructor.
 */
@IgnoreExtraProperties
data class LeaveEntry(
  @DocumentId val id: String = "",
  val userId: String = "",
  val leaveType: String = LeaveCategory.CL.code,
  val startDate: String = "2026-01-01",
  val endDate: String = "2026-01-01",
  val totalDays: Double = 1.0,
  val status: String = LeaveStatus.APPROVED.code,
  val year: Long = 2026L,
  val pairedDutyDate: String = "",
  val reason: String = "",
  val isHalfDay: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  val categoryEnum: LeaveCategory
    get() = LeaveCategory.fromCode(leaveType)

  val statusEnum: LeaveStatus
    get() = LeaveStatus.fromCode(status)

  fun toWritePayload(authenticatedUid: String, isUpdate: Boolean = false): Map<String, Any> {
    val base = mutableMapOf<String, Any>(
      "userId" to authenticatedUid,
      "leaveType" to leaveType,
      "startDate" to startDate,
      "endDate" to endDate,
      "totalDays" to totalDays,
      "status" to status,
      "year" to year,
      "pairedDutyDate" to pairedDutyDate,
      "reason" to reason,
      "isHalfDay" to isHalfDay,
      "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
    )
    if (isUpdate && createdAt != null) {
      base["createdAt"] = createdAt
    } else {
      base["createdAt"] = com.google.firebase.firestore.FieldValue.serverTimestamp()
    }
    return base
  }
}

enum class HighlightStatus(val code: String, val label: String) {
  NORMAL("NORMAL", "Standard"),
  ALERT_RED("ALERT_RED", "Unpaired / Attention"),
  VERIFIED_GREEN("VERIFIED_GREEN", "Verified & Paired"),
  PENDING_ORANGE("PENDING_ORANGE", "Pending Pairing");

  companion object {
    fun fromCode(code: String): HighlightStatus =
      entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: NORMAL
  }
}

@IgnoreExtraProperties
data class LeaveRecord(
  @DocumentId val id: String = "",
  val userId: String = "",
  val leaveType: String = "CL",
  val dateIso: String = "2026-01-01",
  val year: Long = 2026L,
  val pairedDateIso: String = "",
  val notes: String = "",
  val highlightStatus: String = "NORMAL",
  val isHalfDay: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  val category: LeaveCategory
    get() = LeaveCategory.fromCode(leaveType)

  val statusEnum: HighlightStatus
    get() = HighlightStatus.fromCode(highlightStatus)

  val dayValue: Double
    get() = if (isHalfDay) 0.5 else 1.0

  fun toLeaveEntry(): LeaveEntry {
    val mappedStatus = when (statusEnum) {
      HighlightStatus.VERIFIED_GREEN -> LeaveStatus.VERIFIED_PAIRED.code
      HighlightStatus.ALERT_RED -> LeaveStatus.UNPAIRED_ALERT.code
      HighlightStatus.PENDING_ORANGE -> LeaveStatus.PENDING.code
      HighlightStatus.NORMAL -> LeaveStatus.APPROVED.code
    }
    return LeaveEntry(
      id = id,
      userId = userId,
      leaveType = leaveType,
      startDate = dateIso,
      endDate = dateIso,
      totalDays = dayValue,
      status = mappedStatus,
      year = year,
      pairedDutyDate = pairedDateIso,
      reason = notes,
      isHalfDay = isHalfDay,
      createdAt = createdAt,
      updatedAt = updatedAt
    )
  }
}

data class TnHolidayItem(
  val dateIso: String,
  val dayOfWeek: String,
  val nameEnglish: String,
  val nameTamil: String,
  val isRestrictedHoliday: Boolean,
  val monthNameTamil: String = ""
)

data class SmartLeaveBridge(
  val id: String,
  val title: String,
  val startDateIso: String,
  val endDateIso: String,
  val totalDaysOff: Int,
  val leaveCreditsRequired: Int,
  val efficiencyRatio: Float,
  val recommendedMix: String,
  val dayByDayBreakdown: List<BridgeDayStep>,
  val tnRuleComplianceNote: String
)

data class BridgeDayStep(
  val dateIso: String,
  val dayLabel: String,
  val badgeType: String, // "WEEKEND", "GH", "RH", "LEAVE_CREDIT"
  val description: String
)

enum class ColAuditStatus(val label: String) {
  VERIFIED_MATCH("Verified (COL ↔ In Lieu ↔ GH Worked)"),
  DOUBLE_CLAIMED_ALERT("Double-Claimed Duty Date"),
  UNPAIRED_COL("Unaccounted COL (Needs GH Worked Pair)"),
  AVAILABLE_GH_CREDIT("Available Unused GH Worked Credit"),
  MISSING_IN_GH_COLUMN("Paired in 'In Lieu' (Not in GH Worked Col)")
}

data class ColGhAuditItem(
  val id: String,
  val year: Long,
  val colRecordId: String,
  val colDateIso: String,
  val inLieuDateIso: String,
  val ghWorkedRecordId: String,
  val ghWorkedDateIso: String,
  val status: ColAuditStatus,
  val auditExplanation: String,
  val suggestedFixGhDateIso: String = "",
  val suggestedFixGhRecordId: String = ""
)

data class ColGhAuditSummary(
  val totalColTaken: Int,
  val totalGhWorked: Int,
  val totalInLieuEntries: Int,
  val fullyAccountedColCount: Int,
  val unaccountedColCount: Int,
  val availableUnusedGhCount: Int,
  val doubleClaimedCount: Int,
  val items: List<ColGhAuditItem>,
  val unusedGhRecords: List<LeaveRecord>,
  val unpairedColRecords: List<LeaveRecord>
)
