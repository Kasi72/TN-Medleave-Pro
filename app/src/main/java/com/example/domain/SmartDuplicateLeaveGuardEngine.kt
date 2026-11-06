package com.example.domain

import com.example.data.InitialLeaveSeedData
import com.example.data.LeaveCategory
import com.example.data.LeaveRecord
import java.time.DayOfWeek
import java.time.LocalDate

enum class DuplicateCheckVerdict {
  CLEAR_TO_LOG,
  PARTIAL_OVERLAP_AUTO_SPLICED,
  BLOCKED_DUPLICATE_OR_CONFLICT
}

data class DateCollisionDetail(
  val dateIso: String,
  val existingRecordId: String,
  val existingCategory: LeaveCategory,
  val existingNotes: String,
  val reason: String,
  val isHardBlock: Boolean = true
)

data class DuplicateGuardEvaluation(
  val verdict: DuplicateCheckVerdict,
  val allowedDatesToLog: List<String>,
  val blockedCollisions: List<DateCollisionDetail>,
  val suggestedReplacementGhDutyIso: String? = null,
  val headlineTitle: String,
  val detailedExplanation: String
) {
  val canProceed: Boolean
    get() = verdict != DuplicateCheckVerdict.BLOCKED_DUPLICATE_OR_CONFLICT && allowedDatesToLog.isNotEmpty()
}

data class DuplicateGroupItem(
  val duplicateRecord: LeaveRecord,
  val keptMasterRecord: LeaveRecord,
  val reason: String
)

object SmartDuplicateLeaveGuardEngine {

  /**
   * Evaluates a proposed single-day or multi-day leave/duty entry against all existing records
   * in the user's ledger using 7 layers of Tamil Nadu Medical Service rules:
   * 1. Exact Same-Date + Same-Category duplicate check
   * 2. Cross-Category Same-Date mutual exclusivity (e.g. CL vs COL/RH/EL/ML/OD on the same date)
   * 3. Half-Day Session intelligence (allows [FN] + [AN] on the same date, blocks [FN]+[FN] or Full+Half)
   * 4. 3-Column COL Triad awareness (allows GH_WORKED + IN_LIEU on the duty date, blocks double COL)
   * 5. Double-Spend COL Paired Duty check (blocks pairing a new COL with a GH_WORKED date already consumed by another COL)
   * 6. Sunday / Tamil Nadu Public Holiday (GH) waste guard for regular leave types
   * 7. Multi-day range partial-overlap auto-splicing (skips already-logged dates and keeps net-new dates)
   */
  fun evaluateProposedEntry(
    proposedCategory: LeaveCategory,
    candidateDatesIso: List<String>,
    pairedDutyDateIso: String,
    proposedNotes: String,
    isHalfDay: Boolean,
    existingRecords: List<LeaveRecord>
  ): DuplicateGuardEvaluation {
    val cleanDates = candidateDatesIso.map { it.trim() }.filter { it.length == 10 }.distinct().sorted()
    if (cleanDates.isEmpty()) {
      return DuplicateGuardEvaluation(
        verdict = DuplicateCheckVerdict.BLOCKED_DUPLICATE_OR_CONFLICT,
        allowedDatesToLog = emptyList(),
        blockedCollisions = emptyList(),
        headlineTitle = "Invalid or Empty Date Range",
        detailedExplanation = "Enter a valid YYYY-MM-DD date to check for duplicates."
      )
    }

    val collisions = mutableListOf<DateCollisionDetail>()
    val cleanPairedIso = pairedDutyDateIso.trim()

    // Layer 5: Check if a proposed COL or IN_LIEU is trying to reuse a GH_WORKED duty credit
    // that is ALREADY consumed by a different COL date in the ledger.
    var suggestedAlternativeGh: String? = null
    if (proposedCategory == LeaveCategory.COL && cleanPairedIso.isNotBlank()) {
      val alreadyConsumingCol = existingRecords.firstOrNull { rec ->
        rec.category == LeaveCategory.COL &&
          rec.pairedDateIso.trim() == cleanPairedIso &&
          rec.dateIso !in cleanDates
      }
      if (alreadyConsumingCol != null) {
        // Find an alternative unused GH_WORKED credit (FIFO oldest first)
        val consumedGhDates = existingRecords
          .filter { it.category == LeaveCategory.COL && it.pairedDateIso.isNotBlank() }
          .map { it.pairedDateIso.trim() }
          .toSet()
        suggestedAlternativeGh = existingRecords
          .filter { it.category == LeaveCategory.GH_WORKED && it.dateIso !in consumedGhDates }
          .minByOrNull { it.dateIso }
          ?.dateIso

        collisions.add(
          DateCollisionDetail(
            dateIso = cleanDates.first(),
            existingRecordId = alreadyConsumingCol.id,
            existingCategory = LeaveCategory.COL,
            existingNotes = alreadyConsumingCol.notes,
            reason = "Double-Spend Blocked: GH Worked duty on $cleanPairedIso is already consumed by COL on ${alreadyConsumingCol.dateIso}" +
              (if (suggestedAlternativeGh != null) ". Suggested FIFO unused GH credit: $suggestedAlternativeGh" else "."),
            isHardBlock = true
          )
        )
      }
    }

    val allowedDates = mutableListOf<String>()
    val proposedSessionTag = extractSessionTag(proposedNotes, isHalfDay)

    for (dateIso in cleanDates) {
      val sameDateRecords = existingRecords.filter { it.dateIso.trim() == dateIso }

      // Layer 6: Check if user is trying to log a regular leave debit (CL, RH, COL, EL) on a Sunday or TN Public Holiday (GH)
      val parsedLocal = runCatching { LocalDate.parse(dateIso) }.getOrNull()
      val isSunday = parsedLocal?.dayOfWeek == DayOfWeek.SUNDAY
      val matchedGh = findTnPublicHolidayName(dateIso)
      if (proposedCategory in setOf(LeaveCategory.CL, LeaveCategory.RH, LeaveCategory.COL, LeaveCategory.PL) &&
        (isSunday || matchedGh != null)
      ) {
        val offReason = if (matchedGh != null) "TN Public Holiday ($matchedGh)" else "Sunday Weekly Off"
        collisions.add(
          DateCollisionDetail(
            dateIso = dateIso,
            existingRecordId = "statutory_off_$dateIso",
            existingCategory = proposedCategory,
            existingNotes = offReason,
            reason = "$dateIso is already a statutory off ($offReason) — debiting ${proposedCategory.shortLabel} is blocked to prevent credit waste.",
            isHardBlock = true
          )
        )
        continue
      }

      if (sameDateRecords.isEmpty()) {
        allowedDates.add(dateIso)
        continue
      }

      // Check existing records on this exact date
      var dateBlocked = false
      for (existing in sameDateRecords) {
        val existingCat = existing.category

        // Layer 4: 3-Column COL Triad exception:
        // On a Holiday Duty date, GH_WORKED and IN_LIEU are allowed to coexist on the same dateIso
        val isTriadCompatible =
          (proposedCategory == LeaveCategory.IN_LIEU && existingCat == LeaveCategory.GH_WORKED) ||
            (proposedCategory == LeaveCategory.GH_WORKED && existingCat == LeaveCategory.IN_LIEU)

        if (isTriadCompatible) {
          // However, if an IN_LIEU on this duty date is already paired to the exact same COL date, block exact duplicate IN_LIEU
          if (proposedCategory == existingCat) {
            collisions.add(
              DateCollisionDetail(
                dateIso = dateIso,
                existingRecordId = existing.id,
                existingCategory = existingCat,
                existingNotes = existing.notes,
                reason = "Duplicate ${existingCat.shortLabel} already exists on $dateIso.",
                isHardBlock = true
              )
            )
            dateBlocked = true
            break
          }
          continue
        }

        // Layer 3: Half-Day Session compatibility check (Forenoon [FN] + Afternoon [AN] on the same day)
        val existingSessionTag = extractSessionTag(existing.notes, existing.isHalfDay)
        val isComplementaryHalfDay = isHalfDay &&
          existing.isHalfDay &&
          ((proposedSessionTag == "FN" && existingSessionTag == "AN") ||
            (proposedSessionTag == "AN" && existingSessionTag == "FN"))

        if (isComplementaryHalfDay) {
          // Forenoon + Afternoon half-days complement each other — allow!
          continue
        }

        // Layer 1 & 2: Exact Same-Category Duplicate OR Cross-Category Collision on the same date
        val conflictReason = if (existingCat == proposedCategory) {
          if (isHalfDay && existing.isHalfDay && proposedSessionTag == existingSessionTag) {
            "Duplicate Half-Day [$proposedSessionTag] ${proposedCategory.shortLabel} already logged on $dateIso."
          } else {
            "Exact Duplicate Blocked: ${proposedCategory.shortLabel} (${proposedCategory.fullTitle}) is already logged on $dateIso."
          }
        } else {
          "Date Collision Blocked: $dateIso is already booked as ${existingCat.shortLabel} (${existing.notes.ifBlank { existingCat.fullTitle }})."
        }

        collisions.add(
          DateCollisionDetail(
            dateIso = dateIso,
            existingRecordId = existing.id,
            existingCategory = existingCat,
            existingNotes = existing.notes,
            reason = conflictReason,
            isHardBlock = true
          )
        )
        dateBlocked = true
        break
      }

      if (!dateBlocked) {
        allowedDates.add(dateIso)
      }
    }

    // If a double-spend COL pairing collision occurred, block the entire COL entry
    val hasColDoubleSpend = collisions.any { it.reason.startsWith("Double-Spend Blocked:") }
    if (hasColDoubleSpend) {
      return DuplicateGuardEvaluation(
        verdict = DuplicateCheckVerdict.BLOCKED_DUPLICATE_OR_CONFLICT,
        allowedDatesToLog = emptyList(),
        blockedCollisions = collisions,
        suggestedReplacementGhDutyIso = suggestedAlternativeGh,
        headlineTitle = "⛔ COL Duty Credit Already Used",
        detailedExplanation = collisions.first { it.reason.startsWith("Double-Spend Blocked:") }.reason
      )
    }

    return when {
      allowedDates.isEmpty() -> {
        val firstCollision = collisions.firstOrNull()
        DuplicateGuardEvaluation(
          verdict = DuplicateCheckVerdict.BLOCKED_DUPLICATE_OR_CONFLICT,
          allowedDatesToLog = emptyList(),
          blockedCollisions = collisions,
          suggestedReplacementGhDutyIso = suggestedAlternativeGh,
          headlineTitle = "⛔ Duplicate / Conflicting Leave Rejected",
          detailedExplanation = firstCollision?.reason
            ?: "All selected date(s) already have leave entries in your ledger."
        )
      }
      collisions.isNotEmpty() -> {
        val skippedDatesStr = collisions.joinToString(", ") { "${it.dateIso} (${it.existingCategory.shortLabel})" }
        DuplicateGuardEvaluation(
          verdict = DuplicateCheckVerdict.PARTIAL_OVERLAP_AUTO_SPLICED,
          allowedDatesToLog = allowedDates,
          blockedCollisions = collisions,
          suggestedReplacementGhDutyIso = suggestedAlternativeGh,
          headlineTitle = "⚡ Smart Range De-Duplication Active (${allowedDates.size} New • ${collisions.size} Skipped)",
          detailedExplanation = "Auto-skipping ${collisions.size} already-logged/off date(s): $skippedDatesStr. Only ${allowedDates.joinToString(", ")} will be debited."
        )
      }
      else -> {
        DuplicateGuardEvaluation(
          verdict = DuplicateCheckVerdict.CLEAR_TO_LOG,
          allowedDatesToLog = allowedDates,
          blockedCollisions = emptyList(),
          suggestedReplacementGhDutyIso = null,
          headlineTitle = "✓ Zero Duplicates • Clear to Book (${allowedDates.size} ${if (allowedDates.size == 1) "Date" else "Dates"})",
          detailedExplanation = "Verified against ${existingRecords.size} ledger records — no overlapping leave or double-spent COL credits."
        )
      }
    }
  }

  /**
   * Scans the entire ledger for any historical duplicate records on the same date (or duplicate COLs
   * consuming the same GH Worked date) so the user can scrub them in 1 tap.
   */
  fun scanLedgerForExistingDuplicates(allRecords: List<LeaveRecord>): List<DuplicateGroupItem> {
    val duplicatesToRemove = mutableListOf<DuplicateGroupItem>()
    val seenDateAndType = mutableMapOf<String, LeaveRecord>()
    val seenSameDateLeave = mutableMapOf<String, LeaveRecord>()
    val seenColPairedDuty = mutableMapOf<String, LeaveRecord>()

    // Sort so VERIFIED_GREEN and older/seeded records are kept as master
    val prioritized = allRecords.sortedWith(
      compareBy<LeaveRecord> {
        when (it.highlightStatus) {
          "VERIFIED_GREEN" -> 0
          "NORMAL" -> 1
          else -> 2
        }
      }.thenBy { it.id }
    )

    for (rec in prioritized) {
      val dateIso = rec.dateIso.trim()
      val cat = rec.category
      val sessionKey = if (rec.isHalfDay) extractSessionTag(rec.notes, true) else "FULL"
      val exactKey = "${dateIso}_${cat.code}_$sessionKey"

      // 1. Exact same date + category + session duplicate
      val existingExact = seenDateAndType[exactKey]
      if (existingExact != null) {
        duplicatesToRemove.add(
          DuplicateGroupItem(
            duplicateRecord = rec,
            keptMasterRecord = existingExact,
            reason = "Duplicate ${cat.shortLabel} on $dateIso (already recorded in #${existingExact.id.takeLast(5)})"
          )
        )
        continue
      }
      seenDateAndType[exactKey] = rec

      // 2. Cross-category leave collision on the same date (excluding GH_WORKED <-> IN_LIEU triad)
      if (cat !in setOf(LeaveCategory.GH_WORKED, LeaveCategory.IN_LIEU) && !rec.isHalfDay) {
        val existingLeaveOnDate = seenSameDateLeave[dateIso]
        if (existingLeaveOnDate != null && existingLeaveOnDate.id != rec.id) {
          duplicatesToRemove.add(
            DuplicateGroupItem(
              duplicateRecord = rec,
              keptMasterRecord = existingLeaveOnDate,
              reason = "Conflicting ${cat.shortLabel} on $dateIso (date already booked as ${existingLeaveOnDate.category.shortLabel})"
            )
          )
          continue
        }
        seenSameDateLeave[dateIso] = rec
      }

      // 3. Duplicate COL consuming the exact same GH Worked date
      if (cat == LeaveCategory.COL && rec.pairedDateIso.isNotBlank()) {
        val dutyIso = rec.pairedDateIso.trim()
        val existingColForDuty = seenColPairedDuty[dutyIso]
        if (existingColForDuty != null && existingColForDuty.id != rec.id) {
          duplicatesToRemove.add(
            DuplicateGroupItem(
              duplicateRecord = rec,
              keptMasterRecord = existingColForDuty,
              reason = "Duplicate COL on $dateIso claiming GH Worked $dutyIso (already used by COL on ${existingColForDuty.dateIso})"
            )
          )
          continue
        }
        seenColPairedDuty[dutyIso] = rec
      }
    }

    return duplicatesToRemove
  }

  private fun extractSessionTag(notes: String, isHalfDay: Boolean): String {
    if (!isHalfDay) return "FULL"
    val upper = notes.uppercase()
    return when {
      upper.contains("[FN]") || upper.contains("FORENOON") -> "FN"
      upper.contains("[AN]") || upper.contains("AFTERNOON") -> "AN"
      else -> "HALF"
    }
  }

  private fun findTnPublicHolidayName(dateIso: String): String? {
    val gh2026 = InitialLeaveSeedData.tnGovtPublicHolidays2026.firstOrNull { it.dateIso == dateIso }
    if (gh2026 != null) return gh2026.nameEnglish
    val gh2027 = TnHolidays2027Catalog.tnGovtPublicHolidays2027.firstOrNull { it.dateIso == dateIso }
    return gh2027?.nameEnglish
  }
}
