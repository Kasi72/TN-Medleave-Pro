package com.example.domain

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.FileProvider
import com.example.data.BridgeDayStep
import com.example.data.ColGhAuditSummary
import com.example.data.InitialLeaveSeedData
import com.example.data.LeaveCategory
import com.example.data.LeaveRecord
import com.example.data.SmartLeaveBridge
import com.example.data.TnHolidayItem
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

// ============================================================================
// UPGRADE #3: 2027 SERVICE YEAR ROLLOVER & PONGAL 2027 HORIZON
// ============================================================================

object TnHolidays2027Catalog {
  val supportedServiceYears: List<Long> = listOf(2023L, 2024L, 2025L, 2026L, 2027L)

  // Full 2027 Tamil Nadu Government Public Holidays (GH) Baseline + Synced Cache
  val defaultGovtPublicHolidays2027: List<TnHolidayItem> = listOf(
    TnHolidayItem("2027-01-01", "Friday", "New Year's Day", "ஆங்கிலப் புத்தாண்டு", false, "ஜனவரி"),
    TnHolidayItem("2027-01-14", "Thursday", "Pongal", "பொங்கல்", false, "ஜனவரி"),
    TnHolidayItem("2027-01-15", "Friday", "Thiruvalluvar Day / Mattu Pongal", "திருவள்ளுவர் தினம் / மாட்டுப் பொங்கல்", false, "ஜனவரி"),
    TnHolidayItem("2027-01-16", "Saturday", "Uzhavar Thirunal", "உழவர் திருநாள்", false, "ஜனவரி"),
    TnHolidayItem("2027-01-21", "Thursday", "Thaipoosam", "தைப்பூசம்", false, "ஜனவரி"),
    TnHolidayItem("2027-01-26", "Tuesday", "Republic Day", "குடியரசு தினம்", false, "ஜனவரி"),
    TnHolidayItem("2027-03-10", "Wednesday", "Ramzan (Id-ul-Fitr)", "ரம்ஜான்", false, "மார்ச்"),
    TnHolidayItem("2027-03-26", "Friday", "Good Friday", "புனித வெள்ளி", false, "மார்ச்"),
    TnHolidayItem("2027-04-08", "Thursday", "Telugu New Year / Ugadi", "தெலுங்கு வருடப்பிறப்பு", false, "ஏப்ரல்"),
    TnHolidayItem("2027-04-14", "Wednesday", "Tamil New Year / Dr. B.R. Ambedkar Birthday", "தமிழ்ப் புத்தாண்டு", false, "ஏப்ரல்"),
    TnHolidayItem("2027-04-19", "Monday", "Mahavir Jayanti", "மகாவீரர் ஜெயந்தி", false, "ஏப்ரல்"),
    TnHolidayItem("2027-05-01", "Saturday", "May Day", "மே தினம்", false, "மே"),
    TnHolidayItem("2027-05-17", "Monday", "Bakrid (Id-ul-Azha)", "பக்ரீத்", false, "மே"),
    TnHolidayItem("2027-06-15", "Tuesday", "Muharram", "மொஹரம்", false, "ஜூன்"),
    TnHolidayItem("2027-08-15", "Sunday", "Independence Day", "சுதந்திர தினம்", false, "ஆகஸ்ட்"),
    TnHolidayItem("2027-08-16", "Monday", "Milad-un-Nabi", "மிலாடி நபி", false, "ஆகஸ்ட்"),
    TnHolidayItem("2027-08-25", "Wednesday", "Krishna Jayanthi", "கிருஷ்ண ஜெயந்தி", false, "ஆகஸ்ட்"),
    TnHolidayItem("2027-09-04", "Saturday", "Vinayakar Chathurthi", "விநாயகர் சதுர்த்தி", false, "செப்டம்பர்"),
    TnHolidayItem("2027-10-02", "Saturday", "Gandhi Jayanti", "காந்தி ஜெயந்தி", false, "அக்டோபர்"),
    TnHolidayItem("2027-10-09", "Saturday", "Ayutha Pooja", "ஆயுத பூஜை", false, "அக்டோபர்"),
    TnHolidayItem("2027-10-10", "Sunday", "Vijaya Dasami", "விஜயதசமி", false, "அக்டோபர்"),
    TnHolidayItem("2027-10-29", "Friday", "Deepavali", "தீபாவளி", false, "அக்டோபர்"),
    TnHolidayItem("2027-12-25", "Saturday", "Christmas", "கிறிஸ்துமஸ்", false, "டிசம்பர்")
  )

  // Full 2027 Tamil Nadu Restricted Holidays (RH / RL) Baseline + Synced Cache
  val defaultRestrictedHolidays2027: List<TnHolidayItem> = listOf(
    TnHolidayItem("2027-01-13", "Wednesday", "Bhogi (Notified TN RH)", "போகிப் பண்டிகை", true, "ஜனவரி"),
    TnHolidayItem("2027-01-22", "Friday", "Arudhra Darisanam (RH)", "ஆருத்ரா தரிசனம்", true, "ஜனவரி"),
    TnHolidayItem("2027-02-08", "Monday", "First Day of Ramadan (RH)", "ரம்ஜான் முதல் நாள்", true, "பிப்ரவரி"),
    TnHolidayItem("2027-02-10", "Wednesday", "Ash Wednesday (RH)", "சாம்பல் புதன்", true, "பிப்ரவரி"),
    TnHolidayItem("2027-02-20", "Saturday", "Maasi Magam (RH)", "மாசி மகம்", true, "பிப்ரவரி"),
    TnHolidayItem("2027-03-04", "Thursday", "Ayya Vaikunda Swami Birthday (RH)", "அய்யா வைகுண்ட சாமி பிறந்த நாள்", true, "மார்ச்"),
    TnHolidayItem("2027-03-06", "Saturday", "Maha Shivaratri / Shab-e-Qadr (RH)", "மகா சிவராத்திரி", true, "மார்ச்"),
    TnHolidayItem("2027-03-25", "Thursday", "Maundy Thursday (RH)", "பெரிய வியாழன்", true, "மார்ச்"),
    TnHolidayItem("2027-03-28", "Sunday", "Easter Sunday (RH)", "ஈஸ்டர்", true, "மார்ச்"),
    TnHolidayItem("2027-03-29", "Monday", "Bannari Amman Festival (Erode Local RH)", "பண்ணாரி அம்மன் குண்டம் திருவிழா", true, "மார்ச்"),
    TnHolidayItem("2027-05-16", "Sunday", "Arfa - Hajj Pilgrimage (RH)", "அர்பா-ஹஜ் யாத்திரை", true, "மே"),
    TnHolidayItem("2027-05-20", "Thursday", "Buddha Purnima / Chitra Pournami (RH)", "புத்த பூர்ணிமா/சித்ரா பௌர்ணமி", true, "மே"),
    TnHolidayItem("2027-06-06", "Sunday", "Hijri New Year (RH)", "ஹிஜிரி வருடப்பிறப்பு", true, "ஜூன்"),
    TnHolidayItem("2027-08-03", "Tuesday", "Aadi Perukku (RH)", "ஆடி பெருக்கு", true, "ஆகஸ்ட்"),
    TnHolidayItem("2027-08-13", "Friday", "Varalakshmi Viratham (RH)", "வரலட்சுமி விரதம்", true, "ஆகஸ்ட்"),
    TnHolidayItem("2027-08-17", "Tuesday", "Rig / Yajur Upakarma / Avani Avittam (RH)", "ஆவணி அவிட்டம்", true, "ஆகஸ்ட்"),
    TnHolidayItem("2027-08-18", "Wednesday", "Gayathri Japam (RH)", "காயத்ரி ஜெபம்", true, "ஆகஸ்ட்"),
    TnHolidayItem("2027-09-12", "Sunday", "Onam (RH)", "ஓணம்", true, "செப்டம்பர்"),
    TnHolidayItem("2027-09-30", "Thursday", "Mahalaya Amavasai (RH)", "மகாளய அமாவாசை", true, "செப்டம்பர்"),
    TnHolidayItem("2027-10-28", "Thursday", "Deepavali Eve / Nonbu (RH)", "தீபாவளி நோம்பு", true, "அக்டோபர்"),
    TnHolidayItem("2027-11-02", "Tuesday", "All Souls' Day (RH)", "கல்லறைத் திருநாள்", true, "நவம்பர்"),
    TnHolidayItem("2027-11-24", "Wednesday", "Guru Tegh Bahadur Martyrdom Day (RH)", "குரு தேக் பகதூர் தினம்", true, "நவம்பர்"),
    TnHolidayItem("2027-12-10", "Friday", "Vaikunta Ekadasi (RH)", "வைகுண்ட ஏகாதசி", true, "டிசம்பர்"),
    TnHolidayItem("2027-12-13", "Monday", "Karthigai Deepam (RH)", "கார்த்திகை தீபம்", true, "டிசம்பர்"),
    TnHolidayItem("2027-12-24", "Friday", "Christmas Eve (Notified TN RH)", "கிறிஸ்துமஸ் ஈவ்", true, "டிசம்பர்"),
    TnHolidayItem("2027-12-31", "Friday", "New Year's Eve (Notified TN RH)", "புத்தாண்டு ஈவ்", true, "டிசம்பர்")
  )

  @Volatile
  var tnGovtPublicHolidays2027: List<TnHolidayItem> = defaultGovtPublicHolidays2027
    private set

  @Volatile
  var tnRestrictedHolidays2027: List<TnHolidayItem> = defaultRestrictedHolidays2027
    private set

  fun updateLiveHolidayCache2027(
    ghList2027Plus: List<TnHolidayItem>,
    rhList2027Plus: List<TnHolidayItem>
  ) {
    if (ghList2027Plus.isNotEmpty()) {
      tnGovtPublicHolidays2027 = ghList2027Plus.sortedBy { it.dateIso }
    }
    if (rhList2027Plus.isNotEmpty()) {
      tnRestrictedHolidays2027 = rhList2027Plus.sortedBy { it.dateIso }
    }
  }

  fun allSyncedPublicHolidays(): List<TnHolidayItem> =
    (InitialLeaveSeedData.tnGovtPublicHolidays2026 + tnGovtPublicHolidays2027)
      .distinctBy { it.dateIso }
      .sortedBy { it.dateIso }

  fun allSyncedRestrictedHolidays(): List<TnHolidayItem> =
    (InitialLeaveSeedData.tnRestrictedHolidays2026 + tnRestrictedHolidays2027)
      .distinctBy { "${it.dateIso}_${it.nameEnglish}" }
      .sortedBy { it.dateIso }

  val smartBridges2027: List<SmartLeaveBridge> = listOf(
    SmartLeaveBridge(
      id = "bridge_pongal_2027_mega",
      title = "Jan 2027 Pongal 5-Day Harvest Bridge",
      startDateIso = "2027-01-13",
      endDateIso = "2027-01-17",
      totalDaysOff = 5,
      leaveCreditsRequired = 1,
      efficiencyRatio = 5.0f,
      recommendedMix = "1 RH (Wed Jan 13 Bhogi) + Thu–Sat Pongal GH + Sun Weekend",
      dayByDayBreakdown = listOf(
        BridgeDayStep("2027-01-13", "Wed", "RH", "Bhogi Festival (1 RH Credit)"),
        BridgeDayStep("2027-01-14", "Thu", "GH", "Pongal Public Holiday"),
        BridgeDayStep("2027-01-15", "Fri", "GH", "Thiruvalluvar Day / Mattu Pongal GH"),
        BridgeDayStep("2027-01-16", "Sat", "GH", "Uzhavar Thirunal GH"),
        BridgeDayStep("2027-01-17", "Sun", "WEEKEND", "Sunday Weekly Off")
      ),
      tnRuleComplianceNote = "5 consecutive days off using only 1 RH credit (within TN 10-day continuous absence cap)."
    ),
    SmartLeaveBridge(
      id = "bridge_republic_day_2027",
      title = "Jan 2027 Republic Day 4-Day Bridge",
      startDateIso = "2027-01-23",
      endDateIso = "2027-01-26",
      totalDaysOff = 4,
      leaveCreditsRequired = 1,
      efficiencyRatio = 4.0f,
      recommendedMix = "Sat–Sun Weekend + 1 CL/COL (Mon Jan 25) + Tue Jan 26 Republic Day GH",
      dayByDayBreakdown = listOf(
        BridgeDayStep("2027-01-23", "Sat", "WEEKEND", "Saturday Weekend"),
        BridgeDayStep("2027-01-24", "Sun", "WEEKEND", "Sunday Weekend"),
        BridgeDayStep("2027-01-25", "Mon", "CL", "1 CL or 2026 Carry-Forward COL"),
        BridgeDayStep("2027-01-26", "Tue", "GH", "Republic Day Public Holiday")
      ),
      tnRuleComplianceNote = "Uses 1 CL or an unavailed late-2026 GH Worked COL credit within its 6-month validity window."
    )
  )
}

// ============================================================================
// UPGRADE #4: ON-DUTY (OD) ACADEMIC SUB-QUOTA TRACKER (TN MES / DME NORMS)
// ============================================================================

enum class OdSubCategory(
  val code: String,
  val shortTag: String,
  val title: String,
  val annualCeilingDays: Int,
  val ruleNote: String
) {
  UNIV_EXAM(
    code = "OD_EXAM",
    shortTag = "UNIV EXAM",
    title = "TN Dr. M.G.R. Med. Univ. Examiner",
    annualCeilingDays = 15,
    ruleNote = "External / Internal Practical & Theory Valuation (Max 15d/yr under DME norms)"
  ),
  CME_CONF(
    code = "OD_CME",
    shortTag = "CME / CONF",
    title = "ANCIPS / IPS / CME / Workshop",
    annualCeilingDays = 10,
    ruleNote = "Scientific Conferences, CME & Academic Paper Presentation (Max 10d/yr)"
  ),
  DME_NMC(
    code = "OD_DME",
    shortTag = "DME / NMC",
    title = "DME / NMC / Official Inspection",
    annualCeilingDays = 12,
    ruleNote = "Directorate of Medical Education, NMC Assessment & DMHP Official Duty"
  ),
  COURT_BOARD(
    code = "OD_BOARD",
    shortTag = "COURT / BOARD",
    title = "Medical Board & Court Psychiatry",
    annualCeilingDays = 15,
    ruleNote = "Statutory Psychiatric Medical Board, Forensic Court Evidence & Mental Health Review Board"
  );

  companion object {
    fun inferFromNotes(notes: String): OdSubCategory {
      val lower = notes.lowercase(Locale.ENGLISH)
      return when {
        lower.contains("[od_cme]") || lower.contains("cme") || lower.contains("conf") || lower.contains("ancips") || lower.contains("workshop") -> CME_CONF
        lower.contains("[od_dme]") || lower.contains("dme") || lower.contains("nmc") || lower.contains("inspection") || lower.contains("dmhp") -> DME_NMC
        lower.contains("[od_board]") || lower.contains("court") || lower.contains("board") || lower.contains("forensic") || lower.contains("prison") -> COURT_BOARD
        else -> UNIV_EXAM
      }
    }
  }
}

data class OdSubQuotaItem(
  val subCategory: OdSubCategory,
  val usedDays: Int,
  val ceilingDays: Int,
  val remainingDays: Int,
  val utilizationFraction: Float,
  val records: List<LeaveRecord>
)

data class AcademicOdSummary(
  val year: Long,
  val totalOdUsed: Int,
  val totalOdCeiling: Int,
  val subQuotas: List<OdSubQuotaItem>
)

object AcademicOdSubQuotaEngine {
  fun analyzeOdSubQuotas(allRecords: List<LeaveRecord>, year: Long): AcademicOdSummary {
    val odRecords = allRecords.filter { it.year == year && it.category == LeaveCategory.ON_DUTY }
    val grouped = odRecords.groupBy { OdSubCategory.inferFromNotes(it.notes) }

    val items = OdSubCategory.entries.map { sub ->
      val recs = grouped[sub].orEmpty()
      val used = recs.size
      val rem = (sub.annualCeilingDays - used).coerceAtLeast(0)
      val frac = (used.toFloat() / sub.annualCeilingDays.toFloat()).coerceIn(0f, 1f)
      OdSubQuotaItem(
        subCategory = sub,
        usedDays = used,
        ceilingDays = sub.annualCeilingDays,
        remainingDays = rem,
        utilizationFraction = frac,
        records = recs
      )
    }

    return AcademicOdSummary(
      year = year,
      totalOdUsed = odRecords.size,
      totalOdCeiling = OdSubCategory.entries.sumOf { it.annualCeilingDays },
      subQuotas = items
    )
  }
}

// ============================================================================
// UPGRADE #6: TN GOVT LTC (LEAVE TRAVEL CONCESSION) & ENCASHMENT TRACKER
// ============================================================================

data class LtcBlockState(
  val blockPeriodLabel: String = "2024 – 2027 (4-Year TN Govt Block)",
  val homeTownAvailed: Boolean = false,
  val homeTownDateNote: String = "Eligible (Biennial Sub-Block)",
  val allIndiaAvailed: Boolean = false,
  val allIndiaDestination: String = "Not Yet Availed in 2024–2027 Block",
  val elEncashedWithLtcDays: Int = 0, // Up to 10 days EL can be encashed with LTC
  val maxElEncashmentWithLtc: Int = 10
)

object LtcBlockTrackerStore {
  private const val PREFS_LTC = "tn_medleave_ltc_block_prefs"

  fun readState(context: Context): LtcBlockState {
    val prefs = context.getSharedPreferences(PREFS_LTC, Context.MODE_PRIVATE)
    return LtcBlockState(
      blockPeriodLabel = prefs.getString("block_label", "2024 – 2027 (4-Year TN Govt Block)") ?: "2024 – 2027 (4-Year TN Govt Block)",
      homeTownAvailed = prefs.getBoolean("home_town_availed", false),
      homeTownDateNote = prefs.getString("home_town_note", "Available for 2026–2027") ?: "Available for 2026–2027",
      allIndiaAvailed = prefs.getBoolean("all_india_availed", false),
      allIndiaDestination = prefs.getString("all_india_dest", "Available (Combine with EL / CL)") ?: "Available (Combine with EL / CL)",
      elEncashedWithLtcDays = prefs.getInt("el_encashed_ltc", 0)
    )
  }

  fun saveState(context: Context, state: LtcBlockState) {
    context.getSharedPreferences(PREFS_LTC, Context.MODE_PRIVATE)
      .edit()
      .putString("block_label", state.blockPeriodLabel)
      .putBoolean("home_town_availed", state.homeTownAvailed)
      .putString("home_town_note", state.homeTownDateNote)
      .putBoolean("all_india_availed", state.allIndiaAvailed)
      .putString("all_india_dest", state.allIndiaDestination)
      .putInt("el_encashed_ltc", state.elEncashedWithLtcDays.coerceIn(0, 10))
      .apply()
  }
}

// ============================================================================
// UPGRADE #7: PHONE CALENDAR SYNC & .ICS CALENDAR FILE EXPORT
// ============================================================================

object CalendarIcsSyncService {

  fun addSingleLeaveToPhoneCalendar(
    context: Context,
    title: String,
    startDateIso: String,
    endDateIso: String,
    description: String
  ): Boolean {
    return runCatching {
      val start = LocalDate.parse(startDateIso)
      val end = LocalDate.parse(endDateIso).plusDays(1) // All-day event end is exclusive
      val startMillis = start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
      val endMillis = end.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

      val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, "TN MedLeave: $title")
        putExtra(CalendarContract.Events.DESCRIPTION, description)
        putExtra(CalendarContract.Events.EVENT_LOCATION, "Dept. of Psychiatry, Govt. Erode Medical College & Hospital")
        putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
      true
    }.getOrDefault(false)
  }

  fun buildIcsCalendarContent(
    records: List<LeaveRecord>,
    includeHolidays2026And2027: Boolean = true
  ): String {
    val sb = StringBuilder()
    sb.appendLine("BEGIN:VCALENDAR")
    sb.appendLine("VERSION:2.0")
    sb.appendLine("PRODID:-//TN MedLeave Pro//HOD Psychiatry GEMCH//EN")
    sb.appendLine("CALSCALE:GREGORIAN")
    sb.appendLine("METHOD:PUBLISH")
    sb.appendLine("X-WR-CALNAME:TN MedLeave Pro (HOD Psychiatry)")

    // 1. User Leave & Duty Entries (2026–2027)
    records.filter { it.year >= 2026L }.forEach { rec ->
      val d = runCatching { LocalDate.parse(rec.dateIso) }.getOrNull() ?: return@forEach
      val dtStart = d.toString().replace("-", "")
      val dtEnd = d.plusDays(1).toString().replace("-", "")
      val summary = "[${rec.category.shortLabel}] ${rec.notes.ifBlank { rec.category.fullTitle }}"
      val desc = buildString {
        append("Category: ${rec.category.fullTitle}\\n")
        if (rec.pairedDateIso.isNotBlank()) append("Paired GH/Duty: ${rec.pairedDateIso}\\n")
        append("Status: ${rec.highlightStatus}")
      }
      sb.appendLine("BEGIN:VEVENT")
      sb.appendLine("UID:medleave-${rec.id.ifBlank { "${rec.dateIso}-${rec.leaveType}" }}@gemch.tn.gov.in")
      sb.appendLine("DTSTART;VALUE=DATE:$dtStart")
      sb.appendLine("DTEND;VALUE=DATE:$dtEnd")
      sb.appendLine("SUMMARY:${escapeIcs(summary)}")
      sb.appendLine("DESCRIPTION:${escapeIcs(desc)}")
      sb.appendLine("LOCATION:Govt. Erode Medical College & Hospital")
      sb.appendLine("END:VEVENT")
    }

    // 2. Official 2026 & 2027 TN Govt Holidays
    if (includeHolidays2026And2027) {
      val allHolidays = InitialLeaveSeedData.tnGovtPublicHolidays2026 +
        TnHolidays2027Catalog.tnGovtPublicHolidays2027
      allHolidays.forEach { hol ->
        val d = runCatching { LocalDate.parse(hol.dateIso) }.getOrNull() ?: return@forEach
        val dtStart = d.toString().replace("-", "")
        val dtEnd = d.plusDays(1).toString().replace("-", "")
        sb.appendLine("BEGIN:VEVENT")
        sb.appendLine("UID:tnholiday-${hol.dateIso}@gemch.tn.gov.in")
        sb.appendLine("DTSTART;VALUE=DATE:$dtStart")
        sb.appendLine("DTEND;VALUE=DATE:$dtEnd")
        sb.appendLine("SUMMARY:[TN GH] ${escapeIcs(hol.nameEnglish)} (${escapeIcs(hol.nameTamil)})")
        sb.appendLine("DESCRIPTION:Official Tamil Nadu Government Public Holiday")
        sb.appendLine("END:VEVENT")
      }
    }

    sb.appendLine("END:VCALENDAR")
    return sb.toString()
  }

  fun exportAndShareIcsFile(
    context: Context,
    records: List<LeaveRecord>
  ): Result<File> {
    return runCatching {
      val exportDir = File(context.cacheDir, "reports").apply { mkdirs() }
      val icsFile = File(exportDir, "TN_MedLeave_Pro_Calendar_2026_2027.ics")
      icsFile.writeText(buildIcsCalendarContent(records, true), Charsets.UTF_8)

      val uri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        icsFile
      )
      val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/calendar"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(
          Intent.EXTRA_SUBJECT,
          "TN MedLeave Pro - 2026-2027 Leave & TN Holiday Calendar (.ICS)"
        )
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(
        Intent.createChooser(shareIntent, "Export .ICS to Google Calendar / Outlook").apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      )
      icsFile
    }
  }

  private fun escapeIcs(text: String): String =
    text.replace(",", "\\,").replace(";", "\\;").replace("\n", "\\n")
}

// ============================================================================
// UPGRADE #8: "AUTO-SKIP WEEKENDS & PUBLIC HOLIDAYS (GH)" SMART RANGE SPLICER
// ============================================================================

data class RangeDaySlice(
  val dateIso: String,
  val dayShort: String,
  val isWeekend: Boolean,
  val isPublicHoliday: Boolean,
  val holidayName: String?,
  val willDebitLeaveCredit: Boolean
)

data class SmartRangeSplicePreview(
  val totalCalendarDays: Int,
  val workingDaysToDebit: List<RangeDaySlice>,
  val skippedWeekendOrGhDays: List<RangeDaySlice>,
  val allSlices: List<RangeDaySlice>
)

object SmartRangeSplicerEngine {
  private fun ghDatesMap(): Map<String, String> =
    TnHolidays2027Catalog.allSyncedPublicHolidays()
      .associate { it.dateIso to it.nameEnglish }

  fun analyzeRangeForAutoSkip(
    startDateIso: String,
    endDateIso: String
  ): SmartRangeSplicePreview? {
    val start = runCatching { LocalDate.parse(startDateIso) }.getOrNull() ?: return null
    val end = runCatching { LocalDate.parse(endDateIso) }.getOrNull() ?: return null
    if (end.isBefore(start)) return null
    val totalDays = ChronoUnit.DAYS.between(start, end).toInt() + 1
    if (totalDays <= 0 || totalDays > 45) return null

    val liveGhMap = ghDatesMap()
    val slices = (0 until totalDays).map { offset ->
      val d = start.plusDays(offset.toLong())
      val iso = d.toString()
      val dowShort = d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase()
      val isWknd = d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY
      val ghName = liveGhMap[iso]
      val isGh = ghName != null
      RangeDaySlice(
        dateIso = iso,
        dayShort = dowShort,
        isWeekend = isWknd,
        isPublicHoliday = isGh,
        holidayName = ghName,
        willDebitLeaveCredit = !isWknd && !isGh
      )
    }

    return SmartRangeSplicePreview(
      totalCalendarDays = totalDays,
      workingDaysToDebit = slices.filter { it.willDebitLeaveCredit },
      skippedWeekendOrGhDays = slices.filter { !it.willDebitLeaveCredit },
      allSlices = slices
    )
  }
}

// ============================================================================
// UPGRADE #9: 1-TAP WHATSAPP / CLIPBOARD EXECUTIVE DIGEST SHARE SERVICE
// ============================================================================

object ExecutiveDigestShareService {

  fun buildExecutiveQuotaDigest(
    summary: YearCreditSummary,
    audit: ColGhAuditSummary,
    elNetBalance: Int
  ): String {
    val clLeftFmt = if (summary.clRemaining % 1.0 == 0.0) {
      summary.clRemaining.toInt().toString()
    } else {
      String.format("%.1f", summary.clRemaining)
    }
    val clUsedFmt = if (summary.clTaken % 1.0 == 0.0) {
      summary.clTaken.toInt().toString()
    } else {
      String.format("%.1f", summary.clTaken)
    }
    return buildString {
      appendLine("🏥 *GOVT. ERODE MEDICAL COLLEGE & HOSPITAL*")
      appendLine("*Dept. of Psychiatry — HOD Leave Statement (${summary.year})*")
      appendLine("━━━━━━━━━━━━━━━━━━━━━━")
      appendLine("• *Casual Leave (CL)*: $clLeftFmt / 12 left ($clUsedFmt availed)")
      appendLine("• *Restricted Holiday (RH)*: ${summary.rhRemaining} / 3 left (${summary.rhTaken} availed)")
      appendLine("• *Compensatory Off (COL)*: ${audit.fullyAccountedColCount}/${audit.totalColTaken} Paired with GH Worked")
      appendLine("• *Unused GH Duty Credits*: ${audit.availableUnusedGhCount} available")
      appendLine("• *Public Holiday Leave (PL)*: ${summary.plTaken} availed")
      appendLine("• *On-Duty (Univ Exam/DME)*: ${summary.onDutyTaken} days")
      appendLine("• *Earned Leave (EL Bank)*: ${elNetBalance}d net accumulated")
      appendLine("━━━━━━━━━━━━━━━━━━━━━━")
      appendLine("✓ _Verified under Tamil Nadu Leave Rules 1933 & FR Annexure VII_")
    }
  }

  fun buildColGhAuditDigest(
    yearLabel: String,
    audit: ColGhAuditSummary
  ): String {
    return buildString {
      appendLine("📋 *COL ↔ GH WORKED RECONCILIATION STATEMENT ($yearLabel)*")
      appendLine("*HOD, Dept. of Psychiatry • GEMC&H*")
      appendLine("━━━━━━━━━━━━━━━━━━━━━━")
      appendLine("• Total COL Availed: ${audit.totalColTaken}")
      appendLine("• Total GH Worked Duty: ${audit.totalGhWorked}")
      appendLine("• 1-to-1 Verified Pairs: ${audit.fullyAccountedColCount}/${audit.totalColTaken}")
      appendLine("• Available Unused GH Credits: ${audit.availableUnusedGhCount}")
      appendLine("━━━━━━━━━━━━━━━━━━━━━━")
      audit.items.take(15).forEachIndexed { idx, item ->
        val colPart = if (item.colDateIso.isNotBlank()) "COL ${item.colDateIso}" else "Unused Duty"
        val ghPart = if (item.ghWorkedDateIso.isNotBlank()) "GH ${item.ghWorkedDateIso}" else "Unpaired"
        appendLine("${idx + 1}. $colPart ↔ $ghPart [${item.status.name}]")
      }
      appendLine("━━━━━━━━━━━━━━━━━━━━━━")
      appendLine("✓ _Generated by TN MedLeave Pro Audit Engine_")
    }
  }

  fun copyDigestToClipboard(context: Context, label: String, digestText: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, digestText))
  }

  fun shareDigestToWhatsAppOrApps(context: Context, subject: String, digestText: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_SUBJECT, subject)
      putExtra(Intent.EXTRA_TEXT, digestText)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(
      Intent.createChooser(intent, "Share Executive Digest via WhatsApp / Email").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    )
  }
}

// ============================================================================
// UPGRADE #10: INTERACTIVE "WHAT-IF" LEAVE BALANCE SIMULATOR ENGINE
// ============================================================================

data class WhatIfSimulationResult(
  val startDateIso: String,
  val endDateIso: String,
  val simulatedCategory: LeaveCategory,
  val totalCalendarDaysOff: Int,
  val workingDaysDebited: Double,
  val freeWeekendAndGhDays: Int,
  val clBefore: Double,
  val clAfter: Double,
  val rhBefore: Int,
  val rhAfter: Int,
  val colFreeBefore: Int,
  val colFreeAfter: Int,
  val exceedsTenDayContinuousCap: Boolean,
  val exceedsAvailableQuota: Boolean,
  val verdictHeadline: String,
  val verdictDetail: String
)

object WhatIfLeaveSimulatorEngine {

  fun simulateTripImpact(
    startDateIso: String,
    endDateIso: String,
    category: LeaveCategory,
    summary2026: YearCreditSummary,
    unusedGhCredits: Int
  ): WhatIfSimulationResult? {
    val splice = SmartRangeSplicerEngine.analyzeRangeForAutoSkip(startDateIso, endDateIso)
      ?: return null

    val workingCount = splice.workingDaysToDebit.size
    val freeCount = splice.skippedWeekendOrGhDays.size

    val clBefore = summary2026.clRemaining
    val rhBefore = summary2026.rhRemaining
    val colBefore = unusedGhCredits

    val clDebit = if (category == LeaveCategory.CL) workingCount.toDouble() else 0.0
    val rhDebit = if (category == LeaveCategory.RH) workingCount else 0
    val colDebit = if (category == LeaveCategory.COL) workingCount else 0

    val clAfter = (clBefore - clDebit).coerceAtLeast(0.0)
    val rhAfter = (rhBefore - rhDebit).coerceAtLeast(0)
    val colAfter = (colBefore - colDebit).coerceAtLeast(0)

    val exceedsTenDay = category in listOf(LeaveCategory.CL, LeaveCategory.RH) &&
      splice.totalCalendarDays > 10
    val exceedsQuota = when (category) {
      LeaveCategory.CL -> clDebit > clBefore
      LeaveCategory.RH -> rhDebit > rhBefore
      LeaveCategory.COL -> colDebit > colBefore
      else -> false
    }

    val headline = when {
      exceedsTenDay -> "⚠ EXCEEDS TN 10-DAY CONTINUOUS ABSENCE CAP (${splice.totalCalendarDays}d)"
      exceedsQuota -> "⚠ INSUFFICIENT ${category.shortLabel} BALANCE (Needs $workingCount, Left: ${
        when (category) {
          LeaveCategory.CL -> clBefore.toString()
          LeaveCategory.RH -> rhBefore.toString()
          else -> colBefore.toString()
        }
      })"
      else -> "✓ COMPLIANT TRIP: ${splice.totalCalendarDays}d Off for ${workingCount}d ${category.shortLabel} Debit"
    }

    val detail = buildString {
      append("Span includes $workingCount working day(s) and $freeCount weekend/GH day(s). ")
      if (exceedsTenDay) {
        append("Convert spell to Earned Leave (EL) or split into ≤10-day spells under FR Annexure VII.")
      } else if (exceedsQuota) {
        append("Consider mixing CL + RH + COL or using Earned Leave (EL).")
      } else {
        append("Post-trip balance retains ${String.format("%.1f", clAfter)} CL, $rhAfter RH, and $colAfter unused GH credits.")
      }
    }

    return WhatIfSimulationResult(
      startDateIso = startDateIso,
      endDateIso = endDateIso,
      simulatedCategory = category,
      totalCalendarDaysOff = splice.totalCalendarDays,
      workingDaysDebited = workingCount.toDouble(),
      freeWeekendAndGhDays = freeCount,
      clBefore = clBefore,
      clAfter = clAfter,
      rhBefore = rhBefore,
      rhAfter = rhAfter,
      colFreeBefore = colBefore,
      colFreeAfter = colAfter,
      exceedsTenDayContinuousCap = exceedsTenDay,
      exceedsAvailableQuota = exceedsQuota,
      verdictHeadline = headline,
      verdictDetail = detail
    )
  }
}
