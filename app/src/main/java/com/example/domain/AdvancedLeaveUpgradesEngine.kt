package com.example.domain

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.InitialLeaveSeedData
import com.example.data.LeaveCategory
import com.example.data.LeaveRecord
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import org.json.JSONObject

// ============================================================================
// UPGRADE #4, #5, #6: HALF-DAY FN/AN, CERTIFICATE VAULT & IFHRMS SR STORE
// ============================================================================

enum class HalfDaySession(val code: String, val badgeLabel: String, val fullLabel: String) {
  FULL_DAY("FULL", "1.0 DAY", "Full Working Day (1.0)"),
  FORENOON_FN("FN", "½ DAY (FN)", "Forenoon Session • 8:00 AM – 1:00 PM (0.5)"),
  AFTERNOON_AN("AN", "½ DAY (AN)", "Afternoon Session • 1:00 PM – 4:00 PM (0.5)");

  companion object {
    fun fromNotesOrFlag(notes: String, isHalfDay: Boolean): HalfDaySession {
      return when {
        notes.contains("[FN]", ignoreCase = true) || notes.contains("(FN)", ignoreCase = true) -> FORENOON_FN
        notes.contains("[AN]", ignoreCase = true) || notes.contains("(AN)", ignoreCase = true) -> AFTERNOON_AN
        isHalfDay -> FORENOON_FN
        else -> FULL_DAY
      }
    }
  }
}

enum class SrReconciliationState(
  val code: String,
  val shortBadge: String,
  val description: String
) {
  SR_VERIFIED("SR_VERIFIED", "SR / IFHRMS VERIFIED", "Entered & verified in Dean's Office Service Register / IFHRMS"),
  SUBMITTED_OFFICE("SUBMITTED", "SUBMITTED TO E-SEC", "Application / Certificate handed over to College Office"),
  PENDING_SUBMISSION("PENDING", "SR ENTRY PENDING", "Logged in personal ledger; pending Office SR / IFHRMS entry");

  fun nextToggleState(): SrReconciliationState = when (this) {
    PENDING_SUBMISSION -> SUBMITTED_OFFICE
    SUBMITTED_OFFICE -> SR_VERIFIED
    SR_VERIFIED -> PENDING_SUBMISSION
  }
}

data class RecordAttachmentMeta(
  val recordKey: String,
  val srState: SrReconciliationState = SrReconciliationState.PENDING_SUBMISSION,
  val orderRefNo: String = "",
  val issuingAuthority: String = "",
  val certificatePhotoUri: String = ""
)

object LedgerEnrichmentStore {
  private const val PREFS_NAME = "tn_medleave_enrichment_vault_prefs"
  private const val KEY_META_JSON = "record_meta_json"
  private const val KEY_OPENING_EL_2023 = "opening_el_balance_jan_2023"
  private const val KEY_EL_SURRENDERED_DAYS = "el_surrendered_days_total"

  fun getOpeningElBalance2023(context: Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(KEY_OPENING_EL_2023, 148)
  }

  fun setOpeningElBalance2023(context: Context, days: Int) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putInt(KEY_OPENING_EL_2023, days.coerceIn(0, 300)).apply()
  }

  fun getElSurrenderedDays(context: Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(KEY_EL_SURRENDERED_DAYS, 15)
  }

  fun setElSurrenderedDays(context: Context, days: Int) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putInt(KEY_EL_SURRENDERED_DAYS, days.coerceIn(0, 120)).apply()
  }

  fun recordKey(record: LeaveRecord): String =
    record.id.ifBlank { "${record.dateIso}_${record.leaveType}" }

  fun readAllMeta(context: Context): Map<String, RecordAttachmentMeta> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_META_JSON, null) ?: return emptyMap()
    return runCatching {
      val root = JSONObject(raw)
      val map = mutableMapOf<String, RecordAttachmentMeta>()
      val keys = root.keys()
      while (keys.hasNext()) {
        val k = keys.next()
        val obj = root.optJSONObject(k) ?: continue
        val srCode = obj.optString("srState", "")
        val srState = SrReconciliationState.entries.firstOrNull { it.code == srCode }
          ?: SrReconciliationState.PENDING_SUBMISSION
        map[k] = RecordAttachmentMeta(
          recordKey = k,
          srState = srState,
          orderRefNo = obj.optString("orderRefNo", ""),
          issuingAuthority = obj.optString("issuingAuthority", ""),
          certificatePhotoUri = obj.optString("certificatePhotoUri", "")
        )
      }
      map
    }.getOrDefault(emptyMap())
  }

  fun getMetaForRecord(
    record: LeaveRecord,
    allMeta: Map<String, RecordAttachmentMeta>
  ): RecordAttachmentMeta {
    val key = recordKey(record)
    val stored = allMeta[key] ?: allMeta["${record.dateIso}_${record.leaveType}"]
    if (stored != null) return stored

    // Default heuristic: historical 2023–2025 entries & verified green entries default to SR_VERIFIED
    val defaultSr = when {
      record.year < 2026L -> SrReconciliationState.SR_VERIFIED
      record.highlightStatus == "VERIFIED_GREEN" -> SrReconciliationState.SR_VERIFIED
      else -> SrReconciliationState.PENDING_SUBMISSION
    }
    return RecordAttachmentMeta(
      recordKey = key,
      srState = defaultSr
    )
  }

  fun saveMetaForRecord(context: Context, meta: RecordAttachmentMeta) {
    val current = readAllMeta(context).toMutableMap()
    current[meta.recordKey] = meta
    persistMap(context, current)
  }

  private fun persistMap(context: Context, map: Map<String, RecordAttachmentMeta>) {
    val root = JSONObject()
    map.forEach { (k, v) ->
      val obj = JSONObject().apply {
        put("srState", v.srState.code)
        put("orderRefNo", v.orderRefNo)
        put("issuingAuthority", v.issuingAuthority)
        put("certificatePhotoUri", v.certificatePhotoUri)
      }
      root.put(k, obj)
    }
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putString(KEY_META_JSON, root.toString())
      .apply()
  }
}

// ============================================================================
// UPGRADE #2: 6-MONTH COMP-OFF (COL) EXPIRY COUNTDOWN & FIFO RADAR
// ============================================================================

enum class ColExpiryUrgency(val label: String) {
  CRITICAL_WITHIN_30D("EXPIRING ≤ 30 DAYS"),
  WARNING_31_TO_60D("EXPIRES IN 31–60 DAYS"),
  ACTIVE_VALID("VALID (6M WINDOW)"),
  PRIOR_YEAR_CARRY("PRIOR YEAR CREDIT")
}

data class UnusedGhExpiryItem(
  val ghRecord: LeaveRecord,
  val dutyDateIso: String,
  val expiryDateIso: String,
  val daysRemainingToExpiry: Long,
  val urgency: ColExpiryUrgency,
  val holidayName: String
)

object ColExpiryRadarEngine {
  private val REFERENCE_TODAY: LocalDate = LocalDate.of(2026, 11, 5)

  fun analyzeUnusedGhCredits(
    unusedGhRecords: List<LeaveRecord>,
    referenceDate: LocalDate = REFERENCE_TODAY
  ): List<UnusedGhExpiryItem> {
    val ghMap = InitialLeaveSeedData.tnGovtPublicHolidays2026.associateBy { it.dateIso }
    return unusedGhRecords
      .mapNotNull { rec ->
        val dutyDate = runCatching { LocalDate.parse(rec.dateIso) }.getOrNull() ?: return@mapNotNull null
        val expiryDate = dutyDate.plusMonths(6)
        val daysLeft = ChronoUnit.DAYS.between(referenceDate, expiryDate)
        val urgency = when {
          rec.year < 2026L -> ColExpiryUrgency.PRIOR_YEAR_CARRY
          daysLeft <= 30L -> ColExpiryUrgency.CRITICAL_WITHIN_30D
          daysLeft <= 60L -> ColExpiryUrgency.WARNING_31_TO_60D
          else -> ColExpiryUrgency.ACTIVE_VALID
        }
        val holidayTitle = ghMap[rec.dateIso]?.nameEnglish
          ?: rec.notes.ifBlank { "Hospital Holiday Turn Duty" }
        UnusedGhExpiryItem(
          ghRecord = rec,
          dutyDateIso = rec.dateIso,
          expiryDateIso = expiryDate.toString(),
          daysRemainingToExpiry = daysLeft,
          urgency = urgency,
          holidayName = holidayTitle
        )
      }
      // Sort FIFO: oldest 2026 credits first so the user consumes credits closest to 6-month lapse first
      .sortedWith(
        compareBy<UnusedGhExpiryItem> { it.ghRecord.year != 2026L }
          .thenBy { it.daysRemainingToExpiry }
      )
  }
}

// ============================================================================
// UPGRADE #3: EARNED LEAVE (EL) MASTER PASSBOOK & HALF-YEARLY ACCRUAL TRACKER
// ============================================================================

data class ElHalfYearLedgerRow(
  val periodLabel: String,
  val creditDateIso: String,
  val openingBalance: Int,
  val statutoryAccrual: Int, // +15 days per half year under TN FR Rules
  val elAvailedInHalfYear: Int,
  val closingBalance: Int,
  val mlTakenInHalfYear: Int
)

data class ElMasterPassbookSummary(
  val openingBalanceJan2023: Int,
  val totalAccrued2023To2026: Int,
  val totalElAvailed2023To2026: Int,
  val elSurrenderedDays: Int,
  val currentNetElBalance: Int,
  val maxEncashmentCeiling: Int = 240,
  val extendedCeiling: Int = 300,
  val totalMlAvailedCareer: Int,
  val mlCareerLimit: Int = 180,
  val halfYearRows: List<ElHalfYearLedgerRow>
)

object ElMasterPassbookEngine {
  fun computePassbook(
    allRecords: List<LeaveRecord>,
    openingBalanceJan2023: Int,
    elSurrenderedDays: Int
  ): ElMasterPassbookSummary {
    val years = listOf(2023L, 2024L, 2025L, 2026L)
    val rows = mutableListOf<ElHalfYearLedgerRow>()
    var runningBalance = openingBalanceJan2023
    var totalAccrued = 0
    var totalAvailed = 0

    years.forEach { yr ->
      // H1: Jan 1 – Jun 30 (+15 EL credited on Jan 1)
      val h1El = allRecords.count {
        it.year == yr && it.category == LeaveCategory.EL && it.dateIso <= "$yr-06-30"
      }
      val h1Ml = allRecords.count {
        it.year == yr && it.category == LeaveCategory.ML && it.dateIso <= "$yr-06-30"
      }
      val h1Open = runningBalance
      val h1Close = (h1Open + 15 - h1El).coerceIn(0, 300)
      rows.add(
        ElHalfYearLedgerRow(
          periodLabel = "$yr H1 (Jan–Jun)",
          creditDateIso = "$yr-01-01",
          openingBalance = h1Open,
          statutoryAccrual = 15,
          elAvailedInHalfYear = h1El,
          closingBalance = h1Close,
          mlTakenInHalfYear = h1Ml
        )
      )
      runningBalance = h1Close
      totalAccrued += 15
      totalAvailed += h1El

      // H2: Jul 1 – Dec 31 (+15 EL credited on Jul 1)
      val h2El = allRecords.count {
        it.year == yr && it.category == LeaveCategory.EL && it.dateIso >= "$yr-07-01"
      }
      val h2Ml = allRecords.count {
        it.year == yr && it.category == LeaveCategory.ML && it.dateIso >= "$yr-07-01"
      }
      val h2Open = runningBalance
      val h2Close = (h2Open + 15 - h2El).coerceIn(0, 300)
      rows.add(
        ElHalfYearLedgerRow(
          periodLabel = "$yr H2 (Jul–Dec)",
          creditDateIso = "$yr-07-01",
          openingBalance = h2Open,
          statutoryAccrual = 15,
          elAvailedInHalfYear = h2El,
          closingBalance = h2Close,
          mlTakenInHalfYear = h2Ml
        )
      )
      runningBalance = h2Close
      totalAccrued += 15
      totalAvailed += h2El
    }

    val finalNetBalance = (runningBalance - elSurrenderedDays).coerceIn(0, 300)
    val totalMlCareer = allRecords.count { it.category == LeaveCategory.ML }

    return ElMasterPassbookSummary(
      openingBalanceJan2023 = openingBalanceJan2023,
      totalAccrued2023To2026 = totalAccrued,
      totalElAvailed2023To2026 = totalAvailed,
      elSurrenderedDays = elSurrenderedDays,
      currentNetElBalance = finalNetBalance,
      totalMlAvailedCareer = totalMlCareer,
      halfYearRows = rows.reversed() // Most recent half-year first
    )
  }
}

// ============================================================================
// UPGRADE #9: DECEMBER CL & RH LAPSE BURN-DOWN PLANNER ("ZERO-WASTE YEAR-END")
// ============================================================================

data class BurnDownSlotSuggestion(
  val dateIso: String,
  val dayOfWeekLabel: String,
  val suggestedCategory: LeaveCategory,
  val title: String,
  val strategicReason: String,
  val consecutiveDaysOff: Int
)

data class YearEndBurnDownPlan(
  val clRemainingToBurn: Double,
  val rhRemainingToBurn: Int,
  val daysUntilYearEnd: Long,
  val remainingWorkingWeeks: Int,
  val suggestedSlots: List<BurnDownSlotSuggestion>,
  val complianceSummary: String
)

object YearEndBurnDownPlanner {
  private val REFERENCE_TODAY = LocalDate.of(2026, 11, 5)
  private val YEAR_END = LocalDate.of(2026, 12, 31)

  fun generateBurnDownPlan(
    summary2026: YearCreditSummary,
    records2026: List<LeaveRecord>
  ): YearEndBurnDownPlan {
    val existingDates = records2026.map { it.dateIso }.toSet()
    val daysUntilEnd = ChronoUnit.DAYS.between(REFERENCE_TODAY, YEAR_END).coerceAtLeast(1L)
    val weeksLeft = (daysUntilEnd / 7L).toInt().coerceAtLeast(1)

    val suggestions = mutableListOf<BurnDownSlotSuggestion>()
    var clLeft = summary2026.clRemaining
    var rhLeft = summary2026.rhRemaining

    // Priority 1: Restricted Holiday opportunities in Nov–Dec 2026
    val novDecRhCandidates = listOf(
      Triple("2026-11-24", "Guru Tegh Bahadur Martyrdom Day (RH)", "Tuesday RH — pair with Mon CL for 4-day stretch"),
      Triple("2026-12-24", "Christmas Eve (Notified TN RH)", "Thursday RH before Christmas Day (Fri Dec 25 GH) → 4-Day Long Weekend!"),
      Triple("2026-12-31", "New Year's Eve (Notified TN RH)", "Thursday RH before Jan 1 New Year GH → Year-End Bridge!")
    )

    for ((dateStr, title, reason) in novDecRhCandidates) {
      if (rhLeft <= 0) break
      if (dateStr !in existingDates) {
        val d = LocalDate.parse(dateStr)
        val dow = d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase()
        suggestions.add(
          BurnDownSlotSuggestion(
            dateIso = dateStr,
            dayOfWeekLabel = dow,
            suggestedCategory = LeaveCategory.RH,
            title = title,
            strategicReason = reason,
            consecutiveDaysOff = if (d.dayOfWeek == DayOfWeek.THURSDAY) 4 else 3
          )
        )
        rhLeft--
      }
    }

    // Priority 2: Optimal Friday / Monday Casual Leave (CL) slots across Nov–Dec 2026
    val candidateClDates = listOf(
      Triple("2026-11-09", "Monday Post-Deepavali Bridge", "Bridges Sat Nov 7 + Sun Nov 8 Deepavali weekend into a 3-day break"),
      Triple("2026-11-20", "Friday Mid-November Recharge", "Creates a 3-day Fri–Sun weekend without breaking 10-day rule"),
      Triple("2026-11-23", "Monday Bridge Before RH", "Pairs with Tue Nov 24 RH for a 4-day Sat–Tue stretch"),
      Triple("2026-12-04", "Friday Early-December Long Weekend", "Spaced 3-day Fri–Sun recovery block before University Exams"),
      Triple("2026-12-11", "Friday Mid-December Long Weekend", "Consumes 1.0 CL before year-end lapse window tightens"),
      Triple("2026-12-18", "Friday Pre-Christmas Week Buffer", "3-day Fri–Sun weekend keeping Dec balance on track"),
      Triple("2026-12-28", "Monday Year-End Mega Bridge", "Bridges Christmas Weekend (Dec 25–27) toward New Year week"),
      Triple("2026-12-29", "Tuesday Year-End Mega Bridge", "Continues year-end break within TN 10-day continuous absence cap"),
      Triple("2026-12-30", "Wednesday Year-End Mega Bridge", "Links directly into Thu Dec 31 RH + Fri Jan 1 GH")
    )

    for ((dateStr, title, reason) in candidateClDates) {
      if (clLeft < 0.5) break
      if (dateStr !in existingDates) {
        val d = LocalDate.parse(dateStr)
        val dow = d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase()
        suggestions.add(
          BurnDownSlotSuggestion(
            dateIso = dateStr,
            dayOfWeekLabel = dow,
            suggestedCategory = LeaveCategory.CL,
            title = title,
            strategicReason = reason,
            consecutiveDaysOff = if (dateStr.startsWith("2026-12-2")) 9 else 3
          )
        )
        clLeft -= 1.0
      }
    }

    val sortedSuggestions = suggestions.sortedBy { it.dateIso }
    return YearEndBurnDownPlan(
      clRemainingToBurn = summary2026.clRemaining,
      rhRemainingToBurn = summary2026.rhRemaining,
      daysUntilYearEnd = daysUntilEnd,
      remainingWorkingWeeks = weeksLeft,
      suggestedSlots = sortedSuggestions,
      complianceSummary = if (summary2026.clRemaining <= 0.0 && summary2026.rhRemaining <= 0) {
        "100% of your 2026 CL & RH credits are already scheduled — zero year-end lapse!"
      } else {
        "${summary2026.clRemaining} CL + ${summary2026.rhRemaining} RH will lapse on Dec 31, 2026 if unavailed. Below are ${sortedSuggestions.size} optimal non-violating dates."
      }
    )
  }
}

// ============================================================================
// UPGRADE #8: 2-WAY EXCEL / CSV SPREADSHEET EXPORT & IMPORT SERVICE
// ============================================================================

data class CsvImportRow(
  val year: Long,
  val dateIso: String,
  val leaveType: String,
  val isHalfDay: Boolean,
  val pairedDateIso: String,
  val highlightStatus: String,
  val notes: String
)

object CsvSpreadsheetService {

  fun buildExcelCsvContent(
    records: List<LeaveRecord>,
    metaMap: Map<String, RecordAttachmentMeta>
  ): String {
    val sb = StringBuilder()
    // UTF-8 BOM so Microsoft Excel opens Tamil & symbols properly
    sb.append("\uFEFF")
    sb.appendLine("Year,Date_ISO,Day_Of_Week,Leave_Category,Category_Title,Session_Type,Days_Debited,Paired_GH_Duty_Date,Audit_Status,IFHRMS_SR_Status,Order_Ref_No,Issuing_Authority,Notes")

    val sorted = records.sortedWith(compareByDescending<LeaveRecord> { it.year }.thenBy { it.dateIso })
    for (rec in sorted) {
      val dow = runCatching {
        LocalDate.parse(rec.dateIso).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
      }.getOrDefault("")
      val session = HalfDaySession.fromNotesOrFlag(rec.notes, rec.isHalfDay)
      val meta = LedgerEnrichmentStore.getMetaForRecord(rec, metaMap)

      val row = listOf(
        rec.year.toString(),
        rec.dateIso,
        dow,
        rec.leaveType,
        escapeCsv(rec.category.fullTitle),
        session.code,
        rec.dayValue.toString(),
        rec.pairedDateIso,
        rec.highlightStatus,
        meta.srState.code,
        escapeCsv(meta.orderRefNo),
        escapeCsv(meta.issuingAuthority),
        escapeCsv(rec.notes)
      )
      sb.appendLine(row.joinToString(","))
    }
    return sb.toString()
  }

  fun exportAndShareCsv(
    context: Context,
    records: List<LeaveRecord>,
    metaMap: Map<String, RecordAttachmentMeta>
  ): Result<File> {
    return runCatching {
      val exportDir = File(context.cacheDir, "reports").apply { mkdirs() }
      val csvFile = File(exportDir, "TN_MedLeave_Pro_HOD_Ledger_2023_2026.csv")
      csvFile.writeText(buildExcelCsvContent(records, metaMap), Charsets.UTF_8)

      val uri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        csvFile
      )
      val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(
          Intent.EXTRA_SUBJECT,
          "TN MedLeave Pro - HOD Psychiatry Leave Ledger (2023-2026 CSV)"
        )
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(
        Intent.createChooser(shareIntent, "Export Excel / CSV Leave Ledger").apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      )
      csvFile
    }
  }

  fun parseCsvFromUri(context: Context, uri: Uri): Result<List<CsvImportRow>> {
    return runCatching {
      val contentResolver = context.contentResolver
      val inputStream = contentResolver.openInputStream(uri)
        ?: throw IllegalArgumentException("Unable to open selected CSV file.")
      val text = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { it.readText() }
      parseCsvText(text)
    }
  }

  fun parseCsvText(rawCsv: String): List<CsvImportRow> {
    val cleanText = rawCsv.removePrefix("\uFEFF").trim()
    if (cleanText.isBlank()) return emptyList()

    val lines = cleanText.lines().map { it.trim() }.filter { it.isNotBlank() }
    if (lines.isEmpty()) return emptyList()

    val isoRegex = Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")
    val parsed = mutableListOf<CsvImportRow>()

    lines.forEachIndexed { index, line ->
      val cols = splitCsvLine(line)
      if (index == 0 && cols.any { it.contains("Date", ignoreCase = true) || it.contains("Year", ignoreCase = true) }) {
        return@forEachIndexed
      }

      // Support either our full 13-col export format OR simple 3-5 col format: Date_ISO, Leave_Category, Paired_Date, Notes
      val dateColIdx = cols.indexOfFirst { it.trim().matches(isoRegex) }
      if (dateColIdx >= 0) {
        val dateIso = cols[dateColIdx].trim()
        val year = dateIso.take(4).toLongOrNull() ?: 2026L
        val rawCat = cols.getOrNull(dateColIdx + 2)?.let { candidate ->
          if (LeaveCategory.entries.any { it.code.equals(candidate.trim(), ignoreCase = true) }) {
            candidate.trim().uppercase()
          } else {
            cols.getOrNull(dateColIdx + 1)?.trim()?.uppercase() ?: "CL"
          }
        } ?: cols.getOrNull(dateColIdx + 1)?.trim()?.uppercase() ?: "CL"

        val validCat = LeaveCategory.fromCode(rawCat).code
        val sessionCol = cols.getOrNull(5)?.trim()?.uppercase() ?: ""
        val isHalf = sessionCol in listOf("FN", "AN", "0.5", "HALF") ||
          line.contains("[FN]", ignoreCase = true) ||
          line.contains("[AN]", ignoreCase = true)
        val pairedDate = cols.drop(dateColIdx + 1).firstOrNull { it.trim().matches(isoRegex) }?.trim() ?: ""
        val notesCol = cols.lastOrNull()?.trim()?.takeIf { !it.matches(isoRegex) && it != validCat }
          ?: "Imported via CSV Spreadsheet"

        parsed.add(
          CsvImportRow(
            year = year,
            dateIso = dateIso,
            leaveType = validCat,
            isHalfDay = isHalf,
            pairedDateIso = pairedDate,
            highlightStatus = if (pairedDate.isNotBlank()) "VERIFIED_GREEN" else "NORMAL",
            notes = notesCol
          )
        )
      }
    }
    return parsed
  }

  private fun escapeCsv(value: String): String {
    val cleaned = value.replace("\n", " ").replace("\r", " ").trim()
    return if (cleaned.contains(",") || cleaned.contains("\"")) {
      "\"${cleaned.replace("\"", "\"\"")}\""
    } else {
      cleaned
    }
  }

  private fun splitCsvLine(line: String): List<String> {
    val result = mutableListOf<String>()
    val current = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
      val c = line[i]
      when {
        c == '"' -> {
          if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
            current.append('"')
            i++
          } else {
            inQuotes = !inQuotes
          }
        }
        c == ',' && !inQuotes -> {
          result.add(current.toString())
          current.clear()
        }
        else -> current.append(c)
      }
      i++
    }
    result.add(current.toString())
    return result
  }
}
