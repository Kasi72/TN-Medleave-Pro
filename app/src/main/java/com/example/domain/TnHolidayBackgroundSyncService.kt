package com.example.domain

import android.content.Context
import com.example.data.InitialLeaveSeedData
import com.example.data.TnHolidayItem
import com.example.data.local.LocalHolidayRepository
import com.example.data.local.TnHolidayEntity
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class HolidaySyncStatus(
  val isSyncing: Boolean = false,
  val totalGhCount: Int = 46,
  val totalRhCount: Int = 57,
  val gh2026Count: Int = 23,
  val rh2026Count: Int = 31,
  val gh2027Count: Int = 23,
  val rh2027Count: Int = 26,
  val lastSyncedFormatted: String = "Initial 2026–2027 Seed Ready",
  val lastSyncSource: String = "WorkManager Public API + TN G.O. (2026–2027)",
  val statusSummary: String = "WorkManager background sync active — AI Optimizer knowledge base hydrated with 2026–2027 TN GH/RL calendars.",
  val newlySyncedItems: List<String> = emptyList(),
  val hasError: Boolean = false,
  val remoteConfigVersion: String = "2026.11-GO-Ms-842-DME-Erode",
  val remoteConfigBanner: String = "Dynamic Tamil Nadu 2026–2027 Holiday DB Active via WorkManager + Public API",
  val workerScheduleLabel: String = "WorkManager • Daily (24h)",
  val workerStateLabel: String = "AUTO-SYNC SCHEDULED",
  val apiHolidaysParsedCount: Int = 0,
  val knowledgeBaseRevision: Long = 1L
)

object TnHolidayBackgroundSyncService {

  private const val PREFS_NAME = "tn_holiday_sync_prefs"
  private const val KEY_LAST_SYNC_TIME = "last_sync_time_str"
  private const val KEY_LAST_SYNC_EPOCH = "last_sync_epoch_ms"
  private const val KEY_LAST_SOURCE = "last_sync_source"
  private const val KEY_LAST_SUMMARY = "last_sync_summary"
  private const val KEY_AUTO_ROLLOVER_SYNC = "auto_rollover_sync_enabled"
  private const val KEY_LAST_ROLLOVER_YEAR = "last_rollover_synced_year"

  private val scrapedHolidaySchema = Schema.obj(
    properties = mapOf(
      "sourceVerificationNote" to Schema.string(
        description = "Short 1-sentence confirmation of the scraped/verified Tamil Nadu 2026 & 2027 Government Holiday (GH) and Restricted Holiday (RH/RL) calendars and any Erode District / DME updates."
      ),
      "holidays" to Schema.array(
        description = "Verified 2026 and 2027 (or future year) Tamil Nadu Government Holidays (GH) or Restricted Holidays (RH/RL) extracted or confirmed during sync.",
        items = Schema.obj(
          properties = mapOf(
            "dateIso" to Schema.string(description = "YYYY-MM-DD in 2026, 2027, or target year"),
            "dayOfWeek" to Schema.string(description = "Full English weekday name, e.g. Wednesday"),
            "nameEnglish" to Schema.string(description = "Official holiday name in English"),
            "nameTamil" to Schema.string(description = "Official holiday name in Tamil script"),
            "isRestrictedHoliday" to Schema.boolean(description = "true if Restricted Holiday (RL/RH), false if Public Holiday (GH)"),
            "monthNameTamil" to Schema.string(description = "Tamil month name, e.g. ஜனவரி or மார்ச்")
          )
        )
      )
    )
  )

  private val scraperExtractorModel by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.8-flash",
      generationConfig = generationConfig {
        responseMimeType = "application/json"
        responseSchema = scrapedHolidaySchema
        temperature = 0.1f
      },
      systemInstruction = content {
        text(
          """
          You are the automated Multi-Year Tamil Nadu Government Holiday Web-Scraper & G.O. Parser for TN MedLeave Pro (Government Erode Medical College & Hospital).
          Given scraped HTML/JSON content from Tamil Nadu government portals or public holiday APIs (for 2026, 2027, or any upcoming year), extract verified Tamil Nadu Government Public Holidays (GH), Erode District Collectorate Local Holidays, and Restricted Holidays (RH / RL).
          Always output valid ISO dates (YYYY-MM-DD), accurate English and Tamil names, and whether each item is a Public Holiday (isRestrictedHoliday = false) or Restricted Holiday (isRestrictedHoliday = true).
          """.trimIndent()
        )
      }
    )
  }

  fun isAutoYearRolloverSyncEnabled(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_AUTO_ROLLOVER_SYNC, true)
  }

  fun setAutoYearRolloverSyncEnabled(context: Context, enabled: Boolean) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putBoolean(KEY_AUTO_ROLLOVER_SYNC, enabled)
      .apply()
    if (enabled) {
      TnHolidaySyncScheduler.schedulePeriodicSync(context, replaceExisting = false)
    } else {
      TnHolidaySyncScheduler.cancelPeriodicSync(context)
    }
  }

  fun readPersistedSyncStatus(
    context: Context,
    ghCount: Int,
    rhCount: Int
  ): HolidaySyncStatus {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val gh2026 = InitialLeaveSeedData.tnGovtPublicHolidays2026.size
    val rh2026 = InitialLeaveSeedData.tnRestrictedHolidays2026.size
    val gh2027 = TnHolidays2027Catalog.tnGovtPublicHolidays2027.size
    val rh2027 = TnHolidays2027Catalog.tnRestrictedHolidays2027.size
    val totalGh = maxOf(ghCount, gh2026 + gh2027)
    val totalRh = maxOf(rhCount, rh2026 + rh2027)

    val lastTime = prefs.getString(KEY_LAST_SYNC_TIME, null) ?: "Seeded 2026 & 2027"
    val lastSource = prefs.getString(KEY_LAST_SOURCE, null) ?: "Public API + TN G.O. (2026 & 2027)"
    val lastSummary = prefs.getString(KEY_LAST_SUMMARY, null)
      ?: "Room DB & AI Knowledge Base active: 2026 ($gh2026 GH + $rh2026 RL) & 2027 ($gh2027 GH + $rh2027 RL)."

    val rcSnapshot = TnHolidayRemoteConfigService.getPersistedSnapshot(context)
    val isAutoEnabled = isAutoYearRolloverSyncEnabled(context)
    return HolidaySyncStatus(
      isSyncing = false,
      totalGhCount = totalGh,
      totalRhCount = totalRh,
      gh2026Count = gh2026,
      rh2026Count = rh2026,
      gh2027Count = gh2027,
      rh2027Count = rh2027,
      lastSyncedFormatted = lastTime,
      lastSyncSource = lastSource,
      statusSummary = lastSummary,
      remoteConfigVersion = rcSnapshot.catalogVersion,
      remoteConfigBanner = rcSnapshot.announcementBanner,
      workerScheduleLabel = TnHolidaySyncScheduler.formatScheduleLabel(context),
      workerStateLabel = if (isAutoEnabled) "AUTO-SYNC SCHEDULED" else "WORKER PAUSED",
      knowledgeBaseRevision = TnHolidays2027Catalog.knowledgeBaseRevision
    )
  }

  fun shouldRunAutomaticBackgroundSync(context: Context): Boolean {
    if (!isAutoYearRolloverSyncEnabled(context)) return false
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val lastEpoch = prefs.getLong(KEY_LAST_SYNC_EPOCH, 0L)
    val lastRolloverYear = prefs.getInt(KEY_LAST_ROLLOVER_YEAR, 2026)
    val currentCalendarYear = LocalDate.now().year
    // Immediately trigger if a new calendar year (e.g. 2027) has started or if older than 6 hours
    if (currentCalendarYear > lastRolloverYear) return true
    return (System.currentTimeMillis() - lastEpoch) > 6 * 3600 * 1000L
  }

  /**
   * Executes the multi-year background sync pipeline (2026 + 2027 + automatic year rollover):
   * 1. Ensures the local Room database is seeded with both 2026 and 2027 TN GH & RH/RL records.
   * 2. Scrapes live web/API endpoints (`tn.gov.in` holiday list & Nager.Date `IN` Public Holidays API for 2026 & 2027+).
   * 3. Uses Firebase AI Logic (`gemini-3.8-flash` structured JSON output) to parse & verify any 2026/2027+
   *    GH and RH/RL updates or Erode District / DME notified holidays from the scraped payload.
   * 4. Upserts the verified records into the local Room database (`tn_holidays` table) and refreshes
   *    both 2026 and 2027 in-memory optimizer caches so `LeaveOptimizerEngine`, `TamilNaduLeaveRulesService`,
   *    `SmartRangeSplicerEngine`, and `AiLeaveRecommendationEngine` immediately use them for calculation.
   */
  suspend fun syncLatestHolidaysToLocalDb(
    context: Context,
    localRepo: LocalHolidayRepository,
    customSourceUrlOrNote: String = ""
  ): Result<HolidaySyncStatus> = withContext(Dispatchers.IO) {
    try {
      // Step 1: Ensure Room DB has baseline 2026 AND 2027 TN GH & RH/RL holidays
      localRepo.ensureSeededWithDefaultsIfEmpty()

      // Step 1.5: Fetch & activate dynamic Tamil Nadu holiday updates from Firebase Remote Config
      val rcSnapshot = TnHolidayRemoteConfigService.fetchAndApplyRemoteHolidays(
        context = context,
        localRepo = localRepo,
        forceZeroCacheInterval = customSourceUrlOrNote.contains("RemoteConfig", ignoreCase = true)
      )

      val currentYear = LocalDate.now().year.coerceAtLeast(2026)
      val nextYear = currentYear + 1

      // Step 2: Deterministic Public API & Official TN Govt Portal Fetch (Nager.Date IN-TN + OpenHolidays IN-TN + tn.gov.in)
      val apiFetch = TnPublicHolidayApiClient.fetchOfficialTamilNaduHolidays(
        years = listOf(currentYear, nextYear),
        customSourceUrl = customSourceUrlOrNote
      )
      val successfulEndpoint = apiFetch.primarySourceLabel
      val combinedScrapedSnippet = apiFetch.rawSnippetForAi

      // Step 3: Convert deterministic Public API holidays directly into Room entities (works even if AI is offline/throttled)
      val nowMillis = System.currentTimeMillis()
      val newEntities = mutableListOf<TnHolidayEntity>()
      val syncedNames = mutableListOf<String>()
      val seenDateTypeKeys = mutableSetOf<String>()
      var verificationSummary = ""

      apiFetch.holidays.forEach { apiItem ->
        val dedupeKey = "${apiItem.dateIso}_${apiItem.isRestrictedHoliday}"
        if (seenDateTypeKeys.add(dedupeKey)) {
          newEntities.add(
            TnHolidayEntity.fromDomainModel(
              item = apiItem,
              sourceOrigin = "Public API ($successfulEndpoint)",
              syncedAt = nowMillis
            )
          )
          syncedNames.add("${apiItem.dateIso}: ${apiItem.nameEnglish} (${if (apiItem.isRestrictedHoliday) "RH" else "GH"})")
        }
      }

      // Step 3.5: Optional Gemini AI enrichment for Tamil names & Erode District / DME G.O. notifications
      val aiParsedResult = runCatching {
        withTimeout(22_000L) {
          val prompt = """
            Analyze the following scraped $currentYear & $nextYear holiday feed / Tamil Nadu Government notification context and return verified Tamil Nadu Public Holidays (GH) and Restricted Holidays (RH/RL) for $currentYear and $nextYear (including Erode District / Perundurai Local Holiday updates) to upsert into our local Room database for leave optimization calculations.

            PUBLIC API / PORTAL CONTENT ($successfulEndpoint — ${apiFetch.holidays.size} dates pre-parsed):
            ${combinedScrapedSnippet.ifBlank { "Standard 2026 & 2027 TN G.O. Public (Misc) Dept GH & RL verification check" }}

            USER / OFFICE SUPPLEMENTAL NOTE:
            ${customSourceUrlOrNote.ifBlank { "Verify both 2026 and 2027 Tamil Nadu GH & RH/RL calendars (including Jan 2027 Pongal cluster, 2027 Erode Bannari Amman Local Holiday, and 2027 Deepavali/Christmas bridges)." }}
          """.trimIndent()

          val response = scraperExtractorModel.generateContent(prompt)
          response.text ?: ""
        }
      }

      val jsonText = aiParsedResult.getOrNull().orEmpty()
      if (jsonText.isNotBlank()) {
        runCatching {
          val root = JSONObject(jsonText)
          verificationSummary = root.optString("sourceVerificationNote", "")
          val arr = root.optJSONArray("holidays")
          if (arr != null) {
            for (i in 0 until arr.length()) {
              val obj = arr.optJSONObject(i) ?: continue
              val dateIso = obj.optString("dateIso", "").trim()
              if (dateIso.length != 10) continue
              val parsedDate = runCatching { LocalDate.parse(dateIso) }.getOrNull() ?: continue
              if (parsedDate.year < 2026 || parsedDate.year > 2030) continue
              val dayOfWeek = obj.optString(
                "dayOfWeek",
                parsedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
              )
              val nameEn = obj.optString("nameEnglish", "").trim()
              if (nameEn.isEmpty()) continue
              val nameTa = obj.optString("nameTamil", nameEn).trim()
              val isRh = obj.optBoolean("isRestrictedHoliday", true)
              val monthTa = obj.optString("monthNameTamil", "")

              val item = TnHolidayItem(
                dateIso = dateIso,
                dayOfWeek = dayOfWeek,
                nameEnglish = nameEn,
                nameTamil = nameTa,
                isRestrictedHoliday = isRh,
                monthNameTamil = monthTa
              )
              newEntities.add(
                TnHolidayEntity.fromDomainModel(
                  item = item,
                  sourceOrigin = "Synced via $successfulEndpoint (${parsedDate.year})",
                  syncedAt = nowMillis
                )
              )
              syncedNames.add("$dateIso: $nameEn (${if (isRh) "RH" else "GH"})")
            }
          }
        }
      }

      // Always ensure both 2026 and 2027 Erode District Collectorate Local Holidays (Bannari Amman Festival)
      // and key 2027 rollover dates are present even if offline
      if (newEntities.isEmpty()) {
        val fallbackMultiYearItems = listOf(
          TnHolidayItem(
            dateIso = "2026-03-30",
            dayOfWeek = "Monday",
            nameEnglish = "Bannari Amman Festival (Erode District Local RH)",
            nameTamil = "பண்ணாரி அம்மன் குண்டம் திருவிழா (ஈரோடு உள்ளூர் விடுமுறை)",
            isRestrictedHoliday = true,
            monthNameTamil = "மார்ச்"
          ),
          TnHolidayItem(
            dateIso = "2027-03-29",
            dayOfWeek = "Monday",
            nameEnglish = "Bannari Amman Festival 2027 (Erode District Local RH)",
            nameTamil = "பண்ணாரி அம்மன் குண்டம் திருவிழா 2027 (ஈரோடு உள்ளூர் விடுமுறை)",
            isRestrictedHoliday = true,
            monthNameTamil = "மார்ச்"
          )
        )
        fallbackMultiYearItems.forEach { item ->
          newEntities.add(
            TnHolidayEntity.fromDomainModel(
              item = item,
              sourceOrigin = "Erode Collectorate / GEMCH Multi-Year Sync",
              syncedAt = nowMillis
            )
          )
          syncedNames.add("${item.dateIso}: ${item.nameEnglish}")
        }
      }

      // Step 4: Upsert into Room Database & refresh 2026 + 2027 AI Optimizer knowledge base caches
      localRepo.upsertSyncedHolidays(newEntities)
      TnHolidays2027Catalog.notifyKnowledgeBaseRefreshed(successfulEndpoint)

      val gh2026 = InitialLeaveSeedData.tnGovtPublicHolidays2026.size
      val rh2026 = InitialLeaveSeedData.tnRestrictedHolidays2026.size
      val gh2027 = TnHolidays2027Catalog.tnGovtPublicHolidays2027.size
      val rh2027 = TnHolidays2027Catalog.tnRestrictedHolidays2027.size
      val totalGh = gh2026 + gh2027
      val totalRh = rh2026 + rh2027

      val timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM, hh:mm a"))
      val apiNote = if (apiFetch.holidays.isNotEmpty()) {
        "Parsed ${apiFetch.holidays.size} public API dates from $successfulEndpoint. "
      } else {
        ""
      }
      val finalSummary = verificationSummary.ifBlank {
        "${apiNote}AI Optimizer knowledge base refreshed: 2026 ($gh2026 GH + $rh2026 RL) & 2027 ($gh2027 GH + $rh2027 RL)."
      }

      context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_LAST_SYNC_TIME, timeStr)
        .putLong(KEY_LAST_SYNC_EPOCH, nowMillis)
        .putInt(KEY_LAST_ROLLOVER_YEAR, currentYear)
        .putString(KEY_LAST_SOURCE, successfulEndpoint)
        .putString(KEY_LAST_SUMMARY, finalSummary)
        .apply()

      Result.success(
        HolidaySyncStatus(
          isSyncing = false,
          totalGhCount = totalGh,
          totalRhCount = totalRh,
          gh2026Count = gh2026,
          rh2026Count = rh2026,
          gh2027Count = gh2027,
          rh2027Count = rh2027,
          lastSyncedFormatted = timeStr,
          lastSyncSource = if (apiFetch.fetchedFromNetwork) successfulEndpoint else "Firebase RC (${rcSnapshot.catalogVersion}) + Room Seed",
          statusSummary = finalSummary,
          newlySyncedItems = (syncedNames.take(4) + rcSnapshot.parsedHolidaySummaries.take(2)).distinct().take(6),
          hasError = false,
          remoteConfigVersion = rcSnapshot.catalogVersion,
          remoteConfigBanner = rcSnapshot.announcementBanner,
          workerScheduleLabel = TnHolidaySyncScheduler.formatScheduleLabel(context),
          workerStateLabel = if (apiFetch.fetchedFromNetwork) "API + AI KB SYNCED" else "ROOM SEED SYNCED",
          apiHolidaysParsedCount = apiFetch.holidays.size,
          knowledgeBaseRevision = TnHolidays2027Catalog.knowledgeBaseRevision
        )
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      val gh2026 = InitialLeaveSeedData.tnGovtPublicHolidays2026.size
      val rh2026 = InitialLeaveSeedData.tnRestrictedHolidays2026.size
      val gh2027 = TnHolidays2027Catalog.tnGovtPublicHolidays2027.size
      val rh2027 = TnHolidays2027Catalog.tnRestrictedHolidays2027.size
      Result.failure(
        IllegalStateException(
          "Multi-year Room DB active (2026: $gh2026 GH/$rh2026 RL • 2027: $gh2027 GH/$rh2027 RL): ${e.localizedMessage}"
        )
      )
    }
  }
}
