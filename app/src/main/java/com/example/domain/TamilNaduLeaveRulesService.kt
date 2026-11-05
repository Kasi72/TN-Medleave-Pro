package com.example.domain

import com.example.data.InitialLeaveSeedData
import com.example.data.LeaveCategory
import com.example.data.LeaveRecord
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Represents a structured Tamil Nadu Government Leave Rule definition under the
 * Tamil Nadu Leave Rules, 1933 & Fundamental Rules (Annexure VII) applicable to
 * Regular / Permanent Medical Officers & HODs in Tamil Nadu Medical Service (TNMS).
 */
data class TnLeaveRuleSpec(
  val category: LeaveCategory,
  val statutoryCitation: String,
  val annualEntitlementDesc: String,
  val maxAccumulationDays: Int?,
  val maxContinuousStretchDays: Int,
  val canCombineWithCasualLeave: Boolean,
  val canPrefixSuffixHolidays: Boolean,
  val interveningHolidaysCountAsLeave: Boolean,
  val expiryOrCarryForwardRule: String,
  val medicalOfficerSpecialClause: String
)

/**
 * Result of validating a proposed leave spell against Tamil Nadu Government Leave Rules.
 */
data class TnRuleValidationResult(
  val isCompliant: Boolean,
  val category: LeaveCategory,
  val totalCalendarDays: Int,
  val netLeaveDebitedDays: Double,
  val prefixedSuffixedHolidayDays: Int,
  val interveningHolidayDays: Int,
  val statutoryReference: String,
  val violations: List<String>,
  val optimizationAdvice: String
)

/**
 * Multi-Year Service Account Balance for Earned Leave (EL) and Unearned Leave on Medical Certificate (ML)
 * under Tamil Nadu Leave Rules, 1933.
 */
data class TnServiceLeaveAccount(
  val elCreditedPerHalfYear: Int = 15,
  val elAnnualCredit: Int = 30,
  val elMaxAccumulationCap: Int = 240,
  val elMaxPerSpell: Int = 180,
  val elSurrenderEncashmentMaxDays: Int = 15,
  val elTotalTaken2023To2026: Int,
  val mlCareerCeilingDays: Int = 540,
  val mlMaxWithoutMedicalBoardSingleSpell: Int = 60,
  val mlTotalTaken2023To2026: Int,
  val mlRemainingCareerQuota: Int,
  val clAnnualQuota: Int = 12,
  val clMaxTotalAbsenceStretchWithHolidays: Int = 10,
  val rhAnnualQuota: Int = 3,
  val colValidityMonthsFromDuty: Int = 6
)

/**
 * Authoritative Business Logic Service defining the Tamil Nadu Government Leave Rules
 * (TN Leave Rules, 1933 & TN Fundamental Rules Annexure VII) for the Leave Optimizer Engine.
 */
object TamilNaduLeaveRulesService {

  // Statutory Constants (TN Leave Rules, 1933 & Fundamental Rules)
  const val CL_ANNUAL_QUOTA_DAYS = 12
  const val CL_MAX_STRETCH_INCLUDING_HOLIDAYS = 10
  const val RH_ANNUAL_QUOTA_DAYS = 3
  const val EL_HALF_YEARLY_CREDIT_DAYS = 15 // Jan 1 & Jul 1 for Permanent Officers
  const val EL_ANNUAL_CREDIT_DAYS = 30
  const val EL_MAX_ACCUMULATION_CAP_DAYS = 240
  const val EL_MAX_CONTINUOUS_SPELL_DAYS = 180
  const val ML_MAX_CAREER_QUOTA_DAYS = 540 // 18 Months in entire service on full pay
  const val ML_MEDICAL_BOARD_THRESHOLD_DAYS = 60
  const val COL_EXPIRY_WINDOW_DAYS = 180 // 6 months validity from In-Lieu / GH Worked date

  private val ruleSpecs: Map<LeaveCategory, TnLeaveRuleSpec> = mapOf(
    LeaveCategory.CL to TnLeaveRuleSpec(
      category = LeaveCategory.CL,
      statutoryCitation = "TN Fundamental Rules, Annexure VII (Casual Leave Rules)",
      annualEntitlementDesc = "12 days per calendar year (Jan 1 – Dec 31); may be availed as full or ½ day.",
      maxAccumulationDays = 12,
      maxContinuousStretchDays = CL_MAX_STRETCH_INCLUDING_HOLIDAYS,
      canCombineWithCasualLeave = true,
      canPrefixSuffixHolidays = true,
      interveningHolidaysCountAsLeave = false,
      expiryOrCarryForwardRule = "Lapses automatically on December 31 if unavailed.",
      medicalOfficerSpecialClause = "Total absence (CL + RH + COL + prefixed/suffixed/intervening Govt Holidays & Sundays) MUST NOT exceed 10 days at a time. Cannot be combined with Earned Leave (EL) or Medical Leave (ML)."
    ),
    LeaveCategory.RH to TnLeaveRuleSpec(
      category = LeaveCategory.RH,
      statutoryCitation = "TN Fundamental Rules, Annexure VII & Annual G.O. on Restricted Holidays (RL)",
      annualEntitlementDesc = "3 days per calendar year chosen from the notified TN Restricted Holiday (RL) list (31 festivals in 2026).",
      maxAccumulationDays = 3,
      maxContinuousStretchDays = CL_MAX_STRETCH_INCLUDING_HOLIDAYS,
      canCombineWithCasualLeave = true,
      canPrefixSuffixHolidays = true,
      interveningHolidaysCountAsLeave = false,
      expiryOrCarryForwardRule = "Lapses on December 31 if unavailed.",
      medicalOfficerSpecialClause = "Treated akin to Casual Leave; can be combined with CL, COL, and Government Holidays subject to the 10-day total continuous absence ceiling."
    ),
    LeaveCategory.COL to TnLeaveRuleSpec(
      category = LeaveCategory.COL,
      statutoryCitation = "TN Fundamental Rules, Annexure VII (Compensatory Casual Leave / Turn Duty Rules)",
      annualEntitlementDesc = "1 day COL credit earned for each declared Holiday / Turn Duty worked ('In Lieu of' / 'GH Worked').",
      maxAccumulationDays = 10,
      maxContinuousStretchDays = CL_MAX_STRETCH_INCLUDING_HOLIDAYS,
      canCombineWithCasualLeave = true,
      canPrefixSuffixHolidays = true,
      interveningHolidaysCountAsLeave = false,
      expiryOrCarryForwardRule = "Must be availed within 6 months (180 days) from the date of the holiday/turn duty worked.",
      medicalOfficerSpecialClause = "Every COL availed by a Medical Officer/HOD must be 1-to-1 accounted against a specific 'GH Worked' / 'In Lieu of' duty date and stays within the 10-day continuous stretch rule."
    ),
    LeaveCategory.IN_LIEU to TnLeaveRuleSpec(
      category = LeaveCategory.IN_LIEU,
      statutoryCitation = "TN Medical Code & Turn Duty Register Rules",
      annualEntitlementDesc = "Records the exact holiday/weekend turn duty date cited when taking a COL.",
      maxAccumulationDays = null,
      maxContinuousStretchDays = 365,
      canCombineWithCasualLeave = true,
      canPrefixSuffixHolidays = true,
      interveningHolidaysCountAsLeave = false,
      expiryOrCarryForwardRule = "Valid for 6 months from duty date for claiming COL.",
      medicalOfficerSpecialClause = "Must cross-verify 1-to-1 with the Hospital Duty Roster and GH Worked register."
    ),
    LeaveCategory.PL to TnLeaveRuleSpec(
      category = LeaveCategory.PL,
      statutoryCitation = "TN Public Holiday / Hospital Turn Permission Rules",
      annualEntitlementDesc = "Public Holiday off or compensatory holiday permission availed by clinical HOD/Faculty.",
      maxAccumulationDays = null,
      maxContinuousStretchDays = CL_MAX_STRETCH_INCLUDING_HOLIDAYS,
      canCombineWithCasualLeave = true,
      canPrefixSuffixHolidays = true,
      interveningHolidaysCountAsLeave = false,
      expiryOrCarryForwardRule = "Calendar year accounting.",
      medicalOfficerSpecialClause = "Can bridge weekends and RH/CL spells up to the 10-day continuous absence limit."
    ),
    LeaveCategory.GH_WORKED to TnLeaveRuleSpec(
      category = LeaveCategory.GH_WORKED,
      statutoryCitation = "TN Medical Service Hospital Holiday Duty Rules",
      annualEntitlementDesc = "Hospital / Department duty performed on a notified TN Government Public Holiday.",
      maxAccumulationDays = null,
      maxContinuousStretchDays = 365,
      canCombineWithCasualLeave = true,
      canPrefixSuffixHolidays = true,
      interveningHolidaysCountAsLeave = false,
      expiryOrCarryForwardRule = "Generates 1 Compensatory Off (COL) credit valid for 6 months.",
      medicalOfficerSpecialClause = "Audited 1-to-1 against COL taken so every COL is backed by a verified GH Worked date."
    ),
    LeaveCategory.EL to TnLeaveRuleSpec(
      category = LeaveCategory.EL,
      statutoryCitation = "Tamil Nadu Leave Rules, 1933 — Rules 8 to 12 (Earned Leave)",
      annualEntitlementDesc = "Credited in advance at 15 days on Jan 1 and 15 days on Jul 1 (30 days/year) for permanent officers.",
      maxAccumulationDays = EL_MAX_ACCUMULATION_CAP_DAYS,
      maxContinuousStretchDays = EL_MAX_CONTINUOUS_SPELL_DAYS,
      canCombineWithCasualLeave = false,
      canPrefixSuffixHolidays = true,
      interveningHolidaysCountAsLeave = true,
      expiryOrCarryForwardRule = "Accumulates up to a maximum of 240 days. Unavailed credit beyond 240 days lapses unless availed.",
      medicalOfficerSpecialClause = "CRITICAL OPTIMIZATION RULE: Unlike CL, public holidays/weekends falling *inside* an EL spell (intervening holidays) ARE counted as part of Earned Leave and debited from your EL account! However, holidays strictly *prefixed* before start date or *suffixed* after end date are NOT debited. Never bracket a weekend inside a single EL spell—prefix or suffix it instead."
    ),
    LeaveCategory.ML to TnLeaveRuleSpec(
      category = LeaveCategory.ML,
      statutoryCitation = "Tamil Nadu Leave Rules, 1933 — Rule 15 (Unearned Leave on Medical Certificate)",
      annualEntitlementDesc = "Up to 540 days (18 months) in entire service on full pay for permanent Government servants with ≥2 years regular service.",
      maxAccumulationDays = ML_MAX_CAREER_QUOTA_DAYS,
      maxContinuousStretchDays = EL_MAX_CONTINUOUS_SPELL_DAYS,
      canCombineWithCasualLeave = false,
      canPrefixSuffixHolidays = true,
      interveningHolidaysCountAsLeave = true,
      expiryOrCarryForwardRule = "Lifetime service ceiling of 540 days.",
      medicalOfficerSpecialClause = "Spells exceeding 60 days (or repeated short spells) require reference to the Regional Medical Board. Can be combined in continuation with Earned Leave (EL), but NEVER with Casual Leave (CL/RH/COL)."
    ),
    LeaveCategory.ON_DUTY to TnLeaveRuleSpec(
      category = LeaveCategory.ON_DUTY,
      statutoryCitation = "TN Fundamental Rules — Rule 9(6) (Other Duty / Official Deputations)",
      annualEntitlementDesc = "Treated as full duty for DME reviews, TN Dr. MGR Medical University exams, NMC inspections, and official conferences.",
      maxAccumulationDays = null,
      maxContinuousStretchDays = 365,
      canCombineWithCasualLeave = true,
      canPrefixSuffixHolidays = true,
      interveningHolidaysCountAsLeave = false,
      expiryOrCarryForwardRule = "No leave account debit.",
      medicalOfficerSpecialClause = "Requires official relieving/attendance certificate from DME / University / Dean."
    ),
    LeaveCategory.WEEK_OFF to TnLeaveRuleSpec(
      category = LeaveCategory.WEEK_OFF,
      statutoryCitation = "TN Hospital 24-Hour Call & Weekly Off Roster Rules",
      annualEntitlementDesc = "Scheduled weekly off following continuous hospital call duty.",
      maxAccumulationDays = null,
      maxContinuousStretchDays = 10,
      canCombineWithCasualLeave = true,
      canPrefixSuffixHolidays = true,
      interveningHolidaysCountAsLeave = false,
      expiryOrCarryForwardRule = "Must be availed in the same roster week.",
      medicalOfficerSpecialClause = "Does not debit CL or EL balance."
    )
  )

  fun getAllRuleSpecs(): List<TnLeaveRuleSpec> = LeaveCategory.entries.mapNotNull { ruleSpecs[it] }

  fun getRuleSpec(category: LeaveCategory): TnLeaveRuleSpec =
    ruleSpecs[category] ?: ruleSpecs.getValue(LeaveCategory.CL)

  /**
   * Computes the officer's multi-year Earned Leave (EL) & Medical Leave (ML) service account
   * metrics across all recorded years (2023–2026).
   */
  fun computeServiceLeaveAccount(allRecords: List<LeaveRecord>): TnServiceLeaveAccount {
    val elTotal = allRecords.count { it.category == LeaveCategory.EL }
    val mlTotal = allRecords.count { it.category == LeaveCategory.ML }
    return TnServiceLeaveAccount(
      elTotalTaken2023To2026 = elTotal,
      mlTotalTaken2023To2026 = mlTotal,
      mlRemainingCareerQuota = (ML_MAX_CAREER_QUOTA_DAYS - mlTotal).coerceAtLeast(0)
    )
  }

  /**
   * Validates any proposed leave spell (startDate .. endDate) for a specific `LeaveCategory`
   * against the Tamil Nadu Leave Rules, 1933 and Fundamental Rules Annexure VII:
   * - Calculates exact leave debit (accounting for whether intervening holidays count as leave,
   *   which is TRUE for EL/ML and FALSE for CL/RH/COL).
   * - Checks the 10-day maximum continuous absence ceiling for CL/RH/COL.
   * - Checks the 6-month (180-day) validity window when pairing COL with a GH Worked date.
   * - Checks the 60-day Medical Board threshold for ML.
   * - Produces actionable optimization advice (e.g., warning when an EL spell traps a weekend
   *   or GH inside the spell instead of prefixing/suffixing it).
   */
  fun validateAndOptimizeLeaveSpell(
    category: LeaveCategory,
    startDateIso: String,
    endDateIso: String,
    pairedDutyDateIso: String = "",
    clRemainingInYear: Double = 3.0,
    rhRemainingInYear: Int = 2,
    isHalfDay: Boolean = false
  ): TnRuleValidationResult {
    val spec = getRuleSpec(category)
    return try {
      val fmt = DateTimeFormatter.ISO_LOCAL_DATE
      val start = LocalDate.parse(startDateIso, fmt)
      val end = LocalDate.parse(endDateIso, fmt)

      if (end.isBefore(start)) {
        return TnRuleValidationResult(
          isCompliant = false,
          category = category,
          totalCalendarDays = 0,
          netLeaveDebitedDays = 0.0,
          prefixedSuffixedHolidayDays = 0,
          interveningHolidayDays = 0,
          statutoryReference = spec.statutoryCitation,
          violations = listOf("End date ($endDateIso) cannot be earlier than start date ($startDateIso)."),
          optimizationAdvice = "Select an end date on or after $startDateIso."
        )
      }

      val ghMap = TnHolidays2027Catalog.allSyncedPublicHolidays().associateBy { it.dateIso }
      val rhMap = TnHolidays2027Catalog.allSyncedRestrictedHolidays().associateBy { it.dateIso }

      fun isPublicHolidayOrWeekend(d: LocalDate): Boolean {
        val iso = d.format(fmt)
        return d.dayOfWeek == DayOfWeek.SATURDAY ||
          d.dayOfWeek == DayOfWeek.SUNDAY ||
          ghMap.containsKey(iso)
      }

      val totalDaysInSpell = ChronoUnit.DAYS.between(start, end).toInt() + 1

      // Count contiguous prefixed holidays/weekends immediately preceding `start`
      var prefixDays = 0
      var probeBefore = start.minusDays(1)
      while (isPublicHolidayOrWeekend(probeBefore) && prefixDays < 7) {
        prefixDays++
        probeBefore = probeBefore.minusDays(1)
      }

      // Count contiguous suffixed holidays/weekends immediately following `end`
      var suffixDays = 0
      var probeAfter = end.plusDays(1)
      while (isPublicHolidayOrWeekend(probeAfter) && suffixDays < 7) {
        suffixDays++
        probeAfter = probeAfter.plusDays(1)
      }

      var interveningHolidays = 0
      var workingDaysInSpell = 0
      var rhEligibleInSpell = 0
      var d = start
      while (!d.isAfter(end)) {
        val iso = d.format(fmt)
        if (isPublicHolidayOrWeekend(d)) {
          interveningHolidays++
        } else {
          workingDaysInSpell++
          if (rhMap.containsKey(iso)) {
            rhEligibleInSpell++
          }
        }
        d = d.plusDays(1)
      }

      val violations = mutableListOf<String>()
      val totalContinuousAbsence = totalDaysInSpell + prefixDays + suffixDays

      // Net leave debited calculation:
      // Under TN Leave Rules:
      // - For EL and ML: Intervening public holidays/weekends INSIDE the spell ARE debited as EL/ML!
      // - For CL, RH, COL: Public holidays/weekends are NOT debited as leave, but count toward the 10-day max stretch.
      val netDebited: Double = when {
        isHalfDay && totalDaysInSpell == 1 -> 0.5
        spec.interveningHolidaysCountAsLeave -> totalDaysInSpell.toDouble()
        else -> workingDaysInSpell.toDouble()
      }

      // Check 1: 10-Day Continuous Stretch Rule for CL / RH / COL (Annexure VII)
      if (category in listOf(LeaveCategory.CL, LeaveCategory.RH, LeaveCategory.COL, LeaveCategory.PL)) {
        if (totalContinuousAbsence > CL_MAX_STRETCH_INCLUDING_HOLIDAYS) {
          violations.add(
            "Exceeds TN 10-Day Continuous Absence Rule (Annexure VII): Spell ($totalDaysInSpell days) + prefixed/suffixed holidays (${prefixDays + suffixDays} days) = $totalContinuousAbsence days (>10 days max)."
          )
        }
      }

      // Check 2: Annual CL Quota
      if (category == LeaveCategory.CL && netDebited > clRemainingInYear) {
        violations.add(
          "Insufficient 2026 CL Balance: Requires $netDebited CL, but only $clRemainingInYear CL remaining out of 12."
        )
      }

      // Check 3: Annual RH Eligibility & Quota
      if (category == LeaveCategory.RH) {
        if (totalDaysInSpell > rhRemainingInYear) {
          violations.add(
            "Insufficient 2026 RH Balance: Requires $totalDaysInSpell RH, but only $rhRemainingInYear RH remaining out of 3."
          )
        }
        if (totalDaysInSpell == 1 && !rhMap.containsKey(startDateIso)) {
          violations.add(
            "$startDateIso is not on the notified 2026 Tamil Nadu Restricted Holiday (RL) list."
          )
        }
      }

      // Check 4: COL 6-Month Validity & GH Worked Pairing
      if (category == LeaveCategory.COL && pairedDutyDateIso.isNotBlank()) {
        try {
          val dutyDate = LocalDate.parse(pairedDutyDateIso, fmt)
          val gapDays = ChronoUnit.DAYS.between(dutyDate, start)
          if (gapDays > COL_EXPIRY_WINDOW_DAYS) {
            violations.add(
              "TN Turn Duty Rule Warning: COL on $startDateIso is $gapDays days after GH Worked ($pairedDutyDateIso), exceeding the 6-month (180-day) validity window."
            )
          }
        } catch (_: Exception) {
        }
      }

      // Check 5: Earned Leave (EL) 180-Day Single Spell Ceiling & Trapped Intervening Holidays
      if (category == LeaveCategory.EL && totalDaysInSpell > EL_MAX_CONTINUOUS_SPELL_DAYS) {
        violations.add(
          "Exceeds TN Earned Leave Rule 8 ceiling of $EL_MAX_CONTINUOUS_SPELL_DAYS days at a stretch."
        )
      }

      // Check 6: Medical Leave (ML) 60-Day Medical Board Reference
      if (category == LeaveCategory.ML && totalDaysInSpell > ML_MEDICAL_BOARD_THRESHOLD_DAYS) {
        violations.add(
          "TN Medical Leave Rule 15 Note: UEL on Medical Certificate exceeding 60 days ($totalDaysInSpell days) requires clearance from the Regional Medical Board."
        )
      }

      // Build smart optimization advice
      val advice = buildString {
        when (category) {
          LeaveCategory.EL -> {
            if (interveningHolidays > 0) {
              append("💡 TN Earned Leave Optimizer Tip: Your EL spell ($startDateIso to $endDateIso) encloses $interveningHolidays weekend/GH day(s). Under TN Leave Rule 8, intervening holidays inside an EL spell ARE debited from your EL balance ($netDebited EL debited). If possible, start your EL on Monday or end on Friday so weekends are prefixed/suffixed for FREE! ")
            } else {
              append("✅ Optimal EL Spell: Zero weekends/GH trapped inside the spell. Plus ${prefixDays + suffixDays} adjacent weekend/GH day(s) are prefixed/suffixed for FREE without debiting your EL account!")
            }
          }
          LeaveCategory.ML -> {
            append("ℹ️ TN Medical Leave (UEL-MC) Rule 15: Debits $netDebited day(s) from your 540-day career ceiling. Remember: ML can be combined in continuation with Earned Leave (EL), but never with CL/RH/COL.")
          }
          LeaveCategory.CL, LeaveCategory.COL, LeaveCategory.RH -> {
            append("✅ Total absence is $totalContinuousAbsence days (including ${prefixDays + suffixDays + interveningHolidays} free weekend/GH days) for only $netDebited leave credit(s) debited.")
            if (rhEligibleInSpell > 0 && category != LeaveCategory.RH) {
              append(" Includes $rhEligibleInSpell notified Restricted Holiday(s) — consider using an RH credit instead of CL!")
            }
          }
          else -> {
            append("Validated under ${spec.statutoryCitation}.")
          }
        }
      }

      TnRuleValidationResult(
        isCompliant = violations.isEmpty(),
        category = category,
        totalCalendarDays = totalContinuousAbsence,
        netLeaveDebitedDays = netDebited,
        prefixedSuffixedHolidayDays = prefixDays + suffixDays,
        interveningHolidayDays = interveningHolidays,
        statutoryReference = spec.statutoryCitation,
        violations = violations,
        optimizationAdvice = advice
      )
    } catch (e: Exception) {
      TnRuleValidationResult(
        isCompliant = false,
        category = category,
        totalCalendarDays = 0,
        netLeaveDebitedDays = 0.0,
        prefixedSuffixedHolidayDays = 0,
        interveningHolidayDays = 0,
        statutoryReference = spec.statutoryCitation,
        violations = listOf("Invalid date format. Use YYYY-MM-DD."),
        optimizationAdvice = ""
      )
    }
  }
}
