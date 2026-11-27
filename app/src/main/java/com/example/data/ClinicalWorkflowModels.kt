package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties

enum class ApprovalStage(
  val code: String,
  val label: String,
  val shortLabel: String,
  val stepIndex: Int
) {
  PLANNED("PLANNED", "Planned Draft", "Planned", 0),
  SUBMITTED("SUBMITTED", "Submitted to HOD", "Submitted", 1),
  SANCTIONED("SANCTIONED", "Sanctioned", "Sanctioned", 2),
  JOINING_FILED("JOINING_FILED", "Joining Report Filed", "Joined", 3);

  fun nextStage(): ApprovalStage = when (this) {
    PLANNED -> SUBMITTED
    SUBMITTED -> SANCTIONED
    SANCTIONED -> JOINING_FILED
    JOINING_FILED -> JOINING_FILED
  }

  companion object {
    fun fromCode(code: String): ApprovalStage =
      entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: PLANNED
  }
}

enum class DutyShiftType(
  val code: String,
  val label: String,
  val shortLabel: String,
  val isHardConflict: Boolean
) {
  CALL_24H("CALL_24H", "24h Emergency Call", "24h Call", true),
  NIGHT_DUTY("NIGHT_DUTY", "Night Duty", "Night", true),
  OPD_DUTY("OPD_DUTY", "OPD Clinic Duty", "OPD", true),
  WARD_OT("WARD_OT", "Ward / OT Rounds", "Ward/OT", false),
  EXAM_DME("EXAM_DME", "Exam / DME Inspection", "Exam/DME", true),
  GH_EMERGENCY("GH_EMERGENCY", "GH Emergency Duty", "GH Duty", true);

  companion object {
    fun fromCode(code: String): DutyShiftType =
      entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: CALL_24H
  }
}

@Keep
@IgnoreExtraProperties
data class LeaveWorkflowMeta(
  @DocumentId val id: String = "",
  val userId: String = "",
  val recordId: String = "",
  val stage: String = ApprovalStage.PLANNED.code,
  val sanctionOrderNo: String = "",
  val relievingOfficer: String = "",
  val relievingContact: String = "",
  val joiningDateIso: String = "",
  val joiningSession: String = "FN",
  val slipLocalFileName: String = "",
  val workflowNotes: String = "",
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null
) {
  val stageEnum: ApprovalStage
    get() = ApprovalStage.fromCode(stage)

  fun toCreatePayload(authenticatedUid: String): Map<String, Any> {
    val cleanSession = if (joiningSession.equals("AN", ignoreCase = true)) "AN" else "FN"
    val cleanJoiningDate = joiningDateIso.trim().takeIf {
      it.matches(Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$"))
    }.orEmpty()
    return mapOf(
      "userId" to authenticatedUid,
      "recordId" to recordId.trim().take(128),
      "stage" to stageEnum.code,
      "sanctionOrderNo" to sanctionOrderNo.trim().take(80),
      "relievingOfficer" to relievingOfficer.trim().take(100),
      "relievingContact" to relievingContact.trim().take(40),
      "joiningDateIso" to cleanJoiningDate,
      "joiningSession" to cleanSession,
      "slipLocalFileName" to slipLocalFileName.trim().take(160),
      "workflowNotes" to workflowNotes.trim().take(500),
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    )
  }

  fun toUpdatePayload(): Map<String, Any> {
    val cleanSession = if (joiningSession.equals("AN", ignoreCase = true)) "AN" else "FN"
    val cleanJoiningDate = joiningDateIso.trim().takeIf {
      it.matches(Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$"))
    }.orEmpty()
    return mapOf(
      "stage" to stageEnum.code,
      "sanctionOrderNo" to sanctionOrderNo.trim().take(80),
      "relievingOfficer" to relievingOfficer.trim().take(100),
      "relievingContact" to relievingContact.trim().take(40),
      "joiningDateIso" to cleanJoiningDate,
      "joiningSession" to cleanSession,
      "slipLocalFileName" to slipLocalFileName.trim().take(160),
      "workflowNotes" to workflowNotes.trim().take(500),
      "updatedAt" to FieldValue.serverTimestamp()
    )
  }
}

@Keep
@IgnoreExtraProperties
data class DutyShift(
  @DocumentId val id: String = "",
  val userId: String = "",
  val dateIso: String = "2026-01-01",
  val year: Long = 2026L,
  val shiftType: String = DutyShiftType.CALL_24H.code,
  val departmentNote: String = "",
  val colClaimed: Boolean = false,
  val linkedGhRecordId: String = "",
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null
) {
  val shiftTypeEnum: DutyShiftType
    get() = DutyShiftType.fromCode(shiftType)

  fun toCreatePayload(authenticatedUid: String): Map<String, Any> {
    return mapOf(
      "userId" to authenticatedUid,
      "dateIso" to dateIso.trim().take(10),
      "year" to year.coerceIn(2020L, 2035L),
      "shiftType" to shiftTypeEnum.code,
      "departmentNote" to departmentNote.trim().take(200),
      "colClaimed" to colClaimed,
      "linkedGhRecordId" to linkedGhRecordId.trim().take(128),
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    )
  }
}
