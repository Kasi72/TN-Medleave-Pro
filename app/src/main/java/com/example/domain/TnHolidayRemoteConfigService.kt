package com.example.domain

import android.content.Context
import com.example.R
import com.example.data.TnHolidayItem
import com.example.data.local.LocalHolidayRepository
import com.example.data.local.TnHolidayEntity
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class RemoteConfigHolidaySnapshot(
  val isFetchSuccessful: Boolean,
  val activatedNewKeys: Boolean,
  val catalogVersion: String,
  val announcementBanner: String,
  val upsertedHolidayCount: Int,
  val lastFetchedFormatted: String,
  val fetchSourceLabel: String,
  val rawHolidayJsonPreview: String,
  val parsedHolidaySummaries: List<String>
)

object TnHolidayRemoteConfigService {

  const val KEY_CATALOG_VERSION = "tn_holiday_catalog_version"
  const val KEY_ANNOUNCEMENT_BANNER = "tn_holiday_announcement_banner"
  const val KEY_FORCE_REFRESH_ENABLED = "tn_holiday_force_refresh_enabled"
  const val KEY_HOLIDAYS_REMOTE_JSON = "tn_holidays_remote_json"

  private const val PREFS_NAME = "tn_remote_config_holiday_prefs"
  private const val PREF_LAST_VERSION = "rc_last_catalog_version"
  private const val PREF_LAST_BANNER = "rc_last_announcement_banner"
  private const val PREF_LAST_FETCH_TIME = "rc_last_fetch_time"
  private const val PREF_LAST_SOURCE = "rc_last_source_label"
  private const val PREF_LAST_UPSERT_COUNT = "rc_last_upserted_count"
  private const val PREF_REALTIME_LISTENER_ENABLED = "rc_realtime_listener_enabled"

  @Volatile
  private var isRealtimeListenerAttached = false

  fun isRealtimeConfigListenerEnabled(context: Context): Boolean =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getBoolean(PREF_REALTIME_LISTENER_ENABLED, true)

  fun setRealtimeConfigListenerEnabled(context: Context, enabled: Boolean) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putBoolean(PREF_REALTIME_LISTENER_ENABLED, enabled)
      .apply()
  }

  fun getPersistedSnapshot(context: Context): RemoteConfigHolidaySnapshot {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val version = prefs.getString(PREF_LAST_VERSION, "2026.11-GO-Ms-842-DME-Erode")
      ?: "2026.11-GO-Ms-842-DME-Erode"
    val banner = prefs.getString(
      PREF_LAST_BANNER,
      "G.O. Ms. No. 842 (Public Misc. Dept) & Erode Collectorate 2026–2027 Holiday DB Active via Firebase Remote Config"
    ) ?: ""
    val lastTime = prefs.getString(PREF_LAST_FETCH_TIME, "Active (Default + Cloud RC)")
      ?: "Active (Default + Cloud RC)"
    val source = prefs.getString(PREF_LAST_SOURCE, "Firebase Remote Config + XML Seed")
      ?: "Firebase Remote Config + XML Seed"
    val count = prefs.getInt(PREF_LAST_UPSERT_COUNT, 16)

    val rawJson = runCatching {
      Firebase.remoteConfig.getString(KEY_HOLIDAYS_REMOTE_JSON)
    }.getOrDefault("")

    return RemoteConfigHolidaySnapshot(
      isFetchSuccessful = true,
      activatedNewKeys = false,
      catalogVersion = version,
      announcementBanner = banner,
      upsertedHolidayCount = count,
      lastFetchedFormatted = lastTime,
      fetchSourceLabel = source,
      rawHolidayJsonPreview = rawJson.ifBlank { DEFAULT_SAMPLE_JSON_SNIPPET },
      parsedHolidaySummaries = emptyList()
    )
  }

  /**
   * Attaches Firebase Remote Config's real-time ConfigUpdateListener so that whenever an administrator
   * publishes a new Tamil Nadu G.O. or Erode Collectorate holiday in the Firebase Console, the app
   * immediately activates the config and updates the local Room database without an app store update.
   */
  fun ensureInitializedAndObserveRealtimeUpdates(
    context: Context,
    localRepo: LocalHolidayRepository,
    coroutineScope: CoroutineScope,
    onRemoteHolidaysUpdated: (RemoteConfigHolidaySnapshot) -> Unit
  ) {
    runCatching {
      val remoteConfig = Firebase.remoteConfig
      val settings = remoteConfigSettings {
        minimumFetchIntervalInSeconds = 900L // 15 minutes default background interval
      }
      remoteConfig.setConfigSettingsAsync(settings)
      remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)

      if (!isRealtimeListenerAttached) {
        isRealtimeListenerAttached = true
        remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
          override fun onUpdate(configUpdate: ConfigUpdate) {
            if (!isRealtimeConfigListenerEnabled(context)) return
            val relevantKeys = setOf(
              KEY_HOLIDAYS_REMOTE_JSON,
              KEY_CATALOG_VERSION,
              KEY_ANNOUNCEMENT_BANNER,
              KEY_FORCE_REFRESH_ENABLED
            )
            if (configUpdate.updatedKeys.any { it in relevantKeys }) {
              remoteConfig.activate().addOnCompleteListener {
                coroutineScope.launch {
                  val snapshot = parseAndUpsertActiveRemoteConfig(
                    context = context,
                    remoteConfig = remoteConfig,
                    localRepo = localRepo,
                    activatedNewKeys = true,
                    sourceLabel = "Firebase Realtime Config Push"
                  )
                  onRemoteHolidaysUpdated(snapshot)
                }
              }
            }
          }

          override fun onError(error: FirebaseRemoteConfigException) {
            // Keep existing Room DB holidays intact if realtime stream disconnects temporarily
          }
        })
      }
    }
  }

  /**
   * Fetches and activates the latest Firebase Remote Config parameters from the cloud (or falls back
   * gracefully to in-app XML defaults when offline), parses `tn_holidays_remote_json`, and upserts
   * all dynamic Tamil Nadu GH & RH/RL entries into the local Room database.
   */
  suspend fun fetchAndApplyRemoteHolidays(
    context: Context,
    localRepo: LocalHolidayRepository,
    forceZeroCacheInterval: Boolean = false
  ): RemoteConfigHolidaySnapshot = withContext(Dispatchers.IO) {
    try {
      val remoteConfig = Firebase.remoteConfig
      val intervalSeconds = if (forceZeroCacheInterval) 0L else 900L
      val configSettings = remoteConfigSettings {
        minimumFetchIntervalInSeconds = intervalSeconds
      }
      remoteConfig.setConfigSettingsAsync(configSettings).await()
      remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults).await()

      val fetchResult = runCatching {
        if (forceZeroCacheInterval) {
          remoteConfig.fetch(0L).await()
          remoteConfig.activate().await()
        } else {
          remoteConfig.fetchAndActivate().await()
        }
      }

      val activatedNew = fetchResult.getOrDefault(false)
      val sourceLabel = when (remoteConfig.info.lastFetchStatus) {
        FirebaseRemoteConfig.LAST_FETCH_STATUS_SUCCESS ->
          if (activatedNew) "Firebase Cloud Remote Config (Updated)" else "Firebase Cloud Remote Config (Synced)"
        FirebaseRemoteConfig.LAST_FETCH_STATUS_THROTTLED ->
          "Firebase Remote Config Cache (Active)"
        else ->
          "Firebase Remote Config XML Default Seed"
      }

      parseAndUpsertActiveRemoteConfig(
        context = context,
        remoteConfig = remoteConfig,
        localRepo = localRepo,
        activatedNewKeys = activatedNew,
        sourceLabel = sourceLabel
      )
    } catch (e: Exception) {
      // Even if Firebase cloud fetch fails (e.g., offline), parse & apply the XML defaults
      val fallbackSnapshot = parseAndUpsertRawHolidayJson(
        context = context,
        localRepo = localRepo,
        rawJson = DEFAULT_SAMPLE_JSON_SNIPPET,
        catalogVersion = "2026.11-GO-Ms-842-Offline-Default",
        announcementBanner = "Offline Default Remote Config Active (${e.javaClass.simpleName})",
        sourceLabel = "Remote Config Local Default Fallback",
        activatedNewKeys = false
      )
      fallbackSnapshot
    }
  }

  /**
   * Allows testing or applying a dynamic Remote Config JSON array directly into the Room database
   * so HODs/admins can preview or inject an urgent Collectorate / DME holiday notification JSON
   * and see it reflected in the Leave Optimizer & Calendar immediately.
   */
  suspend fun applyCustomRemoteConfigJsonPayload(
    context: Context,
    localRepo: LocalHolidayRepository,
    customJsonArrayStr: String,
    customVersionTag: String = "2026.11-Custom-RC-Override"
  ): RemoteConfigHolidaySnapshot = withContext(Dispatchers.IO) {
    parseAndUpsertRawHolidayJson(
      context = context,
      localRepo = localRepo,
      rawJson = customJsonArrayStr,
      catalogVersion = customVersionTag,
      announcementBanner = "Applied Dynamic Remote Config JSON Override ($customVersionTag)",
      sourceLabel = "Dynamic Remote Config JSON Override",
      activatedNewKeys = true
    )
  }

  private suspend fun parseAndUpsertActiveRemoteConfig(
    context: Context,
    remoteConfig: FirebaseRemoteConfig,
    localRepo: LocalHolidayRepository,
    activatedNewKeys: Boolean,
    sourceLabel: String
  ): RemoteConfigHolidaySnapshot {
    val catalogVersion = remoteConfig.getString(KEY_CATALOG_VERSION)
      .ifBlank { "2026.11-GO-Ms-842-DME-Erode" }
    val announcementBanner = remoteConfig.getString(KEY_ANNOUNCEMENT_BANNER)
      .ifBlank { "Dynamic Tamil Nadu 2026–2027 Holiday DB Active via Firebase Remote Config" }
    val rawJson = remoteConfig.getString(KEY_HOLIDAYS_REMOTE_JSON)
      .ifBlank { DEFAULT_SAMPLE_JSON_SNIPPET }

    return parseAndUpsertRawHolidayJson(
      context = context,
      localRepo = localRepo,
      rawJson = rawJson,
      catalogVersion = catalogVersion,
      announcementBanner = announcementBanner,
      sourceLabel = sourceLabel,
      activatedNewKeys = activatedNewKeys
    )
  }

  private suspend fun parseAndUpsertRawHolidayJson(
    context: Context,
    localRepo: LocalHolidayRepository,
    rawJson: String,
    catalogVersion: String,
    announcementBanner: String,
    sourceLabel: String,
    activatedNewKeys: Boolean
  ): RemoteConfigHolidaySnapshot {
    val nowMillis = System.currentTimeMillis()
    val entities = mutableListOf<TnHolidayEntity>()
    val summaries = mutableListOf<String>()

    runCatching {
      val trimmed = rawJson.trim()
      val jsonArray = if (trimmed.startsWith("{")) {
        JSONObject(trimmed).optJSONArray("holidays") ?: JSONArray()
      } else {
        JSONArray(trimmed)
      }

      for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.optJSONObject(i) ?: continue
        val dateIso = obj.optString("dateIso", "").trim()
        if (dateIso.length != 10) continue
        val parsedDate = runCatching { LocalDate.parse(dateIso) }.getOrNull() ?: continue
        if (parsedDate.year < 2025 || parsedDate.year > 2032) continue

        val dayOfWeek = obj.optString(
          "dayOfWeek",
          parsedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        )
        val nameEnglish = obj.optString("nameEnglish", "").trim()
        if (nameEnglish.isEmpty()) continue
        val nameTamil = obj.optString("nameTamil", nameEnglish).trim()
        val isRestricted = obj.optBoolean("isRestrictedHoliday", false)
        val monthTamil = obj.optString(
          "monthNameTamil",
          parsedDate.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        )

        val domainItem = TnHolidayItem(
          dateIso = dateIso,
          dayOfWeek = dayOfWeek,
          nameEnglish = nameEnglish,
          nameTamil = nameTamil,
          isRestrictedHoliday = isRestricted,
          monthNameTamil = monthTamil
        )
        entities.add(
          TnHolidayEntity.fromDomainModel(
            item = domainItem,
            sourceOrigin = "Firebase Remote Config ($catalogVersion)",
            syncedAt = nowMillis
          )
        )
        summaries.add("$dateIso • $nameEnglish (${if (isRestricted) "RL/RH" else "GH"})")
      }
    }

    if (entities.isNotEmpty()) {
      localRepo.upsertSyncedHolidays(entities)
    }

    val timeFormatted = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM, hh:mm a"))
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putString(PREF_LAST_VERSION, catalogVersion)
      .putString(PREF_LAST_BANNER, announcementBanner)
      .putString(PREF_LAST_FETCH_TIME, timeFormatted)
      .putString(PREF_LAST_SOURCE, sourceLabel)
      .putInt(PREF_LAST_UPSERT_COUNT, entities.size)
      .apply()

    return RemoteConfigHolidaySnapshot(
      isFetchSuccessful = entities.isNotEmpty(),
      activatedNewKeys = activatedNewKeys,
      catalogVersion = catalogVersion,
      announcementBanner = announcementBanner,
      upsertedHolidayCount = entities.size,
      lastFetchedFormatted = timeFormatted,
      fetchSourceLabel = sourceLabel,
      rawHolidayJsonPreview = rawJson,
      parsedHolidaySummaries = summaries
    )
  }

  val DEFAULT_SAMPLE_JSON_SNIPPET = """
[
  {
    "dateIso": "2026-03-30",
    "dayOfWeek": "Monday",
    "nameEnglish": "Bannari Amman Kundam Festival (Erode Collectorate Local Holiday)",
    "nameTamil": "பண்ணாரி அம்மன் குண்டம் திருவிழா (ஈரோடு உள்ளூர் விடுமுறை)",
    "isRestrictedHoliday": true,
    "monthNameTamil": "மார்ச்"
  },
  {
    "dateIso": "2026-11-08",
    "dayOfWeek": "Sunday",
    "nameEnglish": "Deepavali (TN Govt Public Holiday)",
    "nameTamil": "தீபாவளி பண்டிகை",
    "isRestrictedHoliday": false,
    "monthNameTamil": "நவம்பர்"
  },
  {
    "dateIso": "2026-12-25",
    "dayOfWeek": "Friday",
    "nameEnglish": "Christmas (TN Govt Public Holiday)",
    "nameTamil": "கிறிஸ்துமஸ் பண்டிகை",
    "isRestrictedHoliday": false,
    "monthNameTamil": "டிசம்பர்"
  },
  {
    "dateIso": "2027-01-15",
    "dayOfWeek": "Friday",
    "nameEnglish": "Thai Pongal 2027 (TN Govt Public Holiday)",
    "nameTamil": "தைப் பொங்கல் 2027",
    "isRestrictedHoliday": false,
    "monthNameTamil": "ஜனவரி"
  },
  {
    "dateIso": "2027-03-29",
    "dayOfWeek": "Monday",
    "nameEnglish": "Bannari Amman Kundam Festival 2027 (Erode Collectorate Local Holiday)",
    "nameTamil": "பண்ணாரி அம்மன் குண்டம் திருவிழா 2027 (ஈரோடு உள்ளூர் விடுமுறை)",
    "isRestrictedHoliday": true,
    "monthNameTamil": "மார்ச்"
  }
]
  """.trimIndent()
}
