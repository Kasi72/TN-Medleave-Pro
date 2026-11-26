package com.example.domain

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.data.local.LocalHolidayRepository
import com.example.data.local.TnHolidayDatabase
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val WORKER_TAG = "TnHolidaySyncWorker"

/**
 * AndroidX WorkManager CoroutineWorker that runs periodically in the background (and on demand)
 * to fetch the latest official Tamil Nadu Government Public (GH) & Restricted (RH/RL) Holiday
 * calendar from public APIs (Nager.Date IN-TN, OpenHolidays IN-TN, tn.gov.in, and Firebase Remote Config),
 * upserts changes into the local Room database (`tn_holidays`), and automatically refreshes the
 * AI Optimizer's live knowledge base (`InitialLeaveSeedData` + `TnHolidays2027Catalog`).
 */
class TnHolidaySyncWorker(
  appContext: Context,
  workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

  override suspend fun doWork(): Result {
    val customSourceUrl = inputData.getString(KEY_INPUT_CUSTOM_SOURCE_URL).orEmpty()
    val isManualTrigger = inputData.getBoolean(KEY_INPUT_IS_MANUAL, false)
    val db = TnHolidayDatabase.getInstance(applicationContext)
    val localRepo = LocalHolidayRepository(db.holidayDao())

    return try {
      val previousCount = db.holidayDao().getHolidayCount()
      val syncResult = TnHolidayBackgroundSyncService.syncLatestHolidaysToLocalDb(
        context = applicationContext,
        localRepo = localRepo,
        customSourceUrlOrNote = customSourceUrl
      )

      syncResult.fold(
        onSuccess = { status ->
          val currentCount = db.holidayDao().getHolidayCount()
          val deltaAdded = (currentCount - previousCount).coerceAtLeast(0)

          // Ensure AI Optimizer's live knowledge base is freshly hydrated & version-bumped
          localRepo.refreshInMemoryCacheFromRoom()
          TnHolidays2027Catalog.notifyKnowledgeBaseRefreshed(status.lastSyncSource)

          // Quietly notify the officer if background sync discovered new official holidays
          if (!isManualTrigger &&
            (deltaAdded > 0 || status.apiHolidaysParsedCount > 0) &&
            LeaveNotificationHelper.isMasterNotificationsEnabled(applicationContext) &&
            LeaveNotificationHelper.isCalendarSyncAlertEnabled(applicationContext) &&
            LeaveNotificationHelper.hasNotificationPermission(applicationContext)
          ) {
            if (deltaAdded > 0) {
              LeaveNotificationHelper.ensureNotificationChannels(applicationContext)
              LeaveNotificationHelper.dispatchSystemNotification(
                context = applicationContext,
                notificationId = 88422,
                channelId = LeaveNotificationHelper.CHANNEL_HOLIDAY_REMINDERS,
                title = "TN Govt Holiday Calendar & AI Optimizer Updated",
                subText = "WorkManager Auto-Sync • ${status.totalGhCount} GH / ${status.totalRhCount} RH",
                body = "Synced $deltaAdded new official holiday date(s) from ${status.lastSyncSource}. AI Leave Optimizer bridges have been refreshed."
              )
            }
          }

          val output = workDataOf(
            KEY_OUT_TOTAL_GH to status.totalGhCount,
            KEY_OUT_TOTAL_RH to status.totalRhCount,
            KEY_OUT_API_PARSED to status.apiHolidaysParsedCount,
            KEY_OUT_DELTA_ADDED to deltaAdded,
            KEY_OUT_SOURCE to status.lastSyncSource,
            KEY_OUT_SYNC_TIME to status.lastSyncedFormatted,
            KEY_OUT_SUMMARY to status.statusSummary,
            KEY_OUT_KB_REVISION to TnHolidays2027Catalog.knowledgeBaseRevision
          )
          Log.i(
            WORKER_TAG,
            "Background TN Holiday sync succeeded: ${status.totalGhCount} GH, ${status.totalRhCount} RH, apiParsed=${status.apiHolidaysParsedCount}, source=${status.lastSyncSource}"
          )
          Result.success(output)
        },
        onFailure = { err ->
          Log.w(WORKER_TAG, "Background TN Holiday sync failed (attempt=$runAttemptCount): ${err.message}")
          if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
            Result.retry()
          } else {
            Result.failure(
              workDataOf(
                KEY_OUT_ERROR to (err.localizedMessage ?: "Holiday API sync failed after retries")
              )
            )
          }
        }
      )
    } catch (e: Exception) {
      Log.w(WORKER_TAG, "Unexpected exception in TnHolidaySyncWorker (attempt=$runAttemptCount)", e)
      if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
        Result.retry()
      } else {
        Result.failure(
          workDataOf(KEY_OUT_ERROR to (e.localizedMessage ?: "Unexpected worker failure"))
        )
      }
    }
  }

  companion object {
    const val MAX_RETRY_ATTEMPTS = 3
    const val KEY_INPUT_CUSTOM_SOURCE_URL = "input_custom_source_url"
    const val KEY_INPUT_IS_MANUAL = "input_is_manual"

    const val KEY_OUT_TOTAL_GH = "out_total_gh"
    const val KEY_OUT_TOTAL_RH = "out_total_rh"
    const val KEY_OUT_API_PARSED = "out_api_parsed"
    const val KEY_OUT_DELTA_ADDED = "out_delta_added"
    const val KEY_OUT_SOURCE = "out_source"
    const val KEY_OUT_SYNC_TIME = "out_sync_time"
    const val KEY_OUT_SUMMARY = "out_summary"
    const val KEY_OUT_KB_REVISION = "out_kb_revision"
    const val KEY_OUT_ERROR = "out_error"
  }
}

/**
 * Schedules, triggers, cancels, and observes the WorkManager background worker that keeps
 * the local Room TN Holiday DB and AI Optimizer knowledge base synced with official public APIs.
 */
object TnHolidaySyncScheduler {

  const val UNIQUE_PERIODIC_WORK_NAME = "tn_govt_holiday_periodic_sync_v1"
  const val UNIQUE_IMMEDIATE_WORK_NAME = "tn_govt_holiday_immediate_sync_v1"
  const val WORK_TAG_HOLIDAY_SYNC = "tag_tn_holiday_sync_worker"

  private const val PREFS_NAME = "tn_holiday_sync_prefs"
  private const val KEY_SYNC_INTERVAL_HOURS = "worker_sync_interval_hours"

  fun getSyncIntervalHours(context: Context): Long {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getLong(KEY_SYNC_INTERVAL_HOURS, 24L)
      .coerceIn(12L, 168L)
  }

  fun setSyncIntervalHours(context: Context, hours: Long) {
    val safeHours = hours.coerceIn(12L, 168L)
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putLong(KEY_SYNC_INTERVAL_HOURS, safeHours)
      .apply()
    if (TnHolidayBackgroundSyncService.isAutoYearRolloverSyncEnabled(context)) {
      schedulePeriodicSync(
        context = context,
        intervalHours = safeHours,
        replaceExisting = true
      )
    }
  }

  fun schedulePeriodicSync(
    context: Context,
    intervalHours: Long = getSyncIntervalHours(context),
    replaceExisting: Boolean = false
  ) {
    val constraints = Constraints.Builder()
      .setRequiredNetworkType(NetworkType.CONNECTED)
      .setRequiresBatteryNotLow(true)
      .build()

    val periodicRequest = PeriodicWorkRequestBuilder<TnHolidaySyncWorker>(
      intervalHours.coerceAtLeast(12L),
      TimeUnit.HOURS,
      2L,
      TimeUnit.HOURS
    )
      .setConstraints(constraints)
      .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15L, TimeUnit.MINUTES)
      .addTag(WORK_TAG_HOLIDAY_SYNC)
      .setInputData(
        Data.Builder()
          .putBoolean(TnHolidaySyncWorker.KEY_INPUT_IS_MANUAL, false)
          .build()
      )
      .build()

    val policy = if (replaceExisting) {
      ExistingPeriodicWorkPolicy.UPDATE
    } else {
      ExistingPeriodicWorkPolicy.KEEP
    }

    WorkManager.getInstance(context.applicationContext)
      .enqueueUniquePeriodicWork(UNIQUE_PERIODIC_WORK_NAME, policy, periodicRequest)
  }

  fun enqueueImmediateSync(
    context: Context,
    customSourceUrl: String = "",
    isManual: Boolean = true
  ) {
    val constraints = Constraints.Builder()
      .setRequiredNetworkType(NetworkType.CONNECTED)
      .build()

    val oneTimeRequest = OneTimeWorkRequestBuilder<TnHolidaySyncWorker>()
      .setConstraints(constraints)
      .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
      .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1L, TimeUnit.MINUTES)
      .addTag(WORK_TAG_HOLIDAY_SYNC)
      .setInputData(
        Data.Builder()
          .putString(TnHolidaySyncWorker.KEY_INPUT_CUSTOM_SOURCE_URL, customSourceUrl)
          .putBoolean(TnHolidaySyncWorker.KEY_INPUT_IS_MANUAL, isManual)
          .build()
      )
      .build()

    WorkManager.getInstance(context.applicationContext)
      .enqueueUniqueWork(UNIQUE_IMMEDIATE_WORK_NAME, ExistingWorkPolicy.REPLACE, oneTimeRequest)
  }

  fun cancelPeriodicSync(context: Context) {
    WorkManager.getInstance(context.applicationContext)
      .cancelUniqueWork(UNIQUE_PERIODIC_WORK_NAME)
  }

  fun observePeriodicWorkState(context: Context): Flow<WorkInfo?> {
    return WorkManager.getInstance(context.applicationContext)
      .getWorkInfosForUniqueWorkFlow(UNIQUE_PERIODIC_WORK_NAME)
      .map { list -> list.firstOrNull() }
  }

  fun observeImmediateWorkState(context: Context): Flow<WorkInfo?> {
    return WorkManager.getInstance(context.applicationContext)
      .getWorkInfosForUniqueWorkFlow(UNIQUE_IMMEDIATE_WORK_NAME)
      .map { list -> list.firstOrNull() }
  }

  fun formatScheduleLabel(context: Context): String {
    if (!TnHolidayBackgroundSyncService.isAutoYearRolloverSyncEnabled(context)) {
      return "Auto-Worker Paused"
    }
    return when (val hrs = getSyncIntervalHours(context)) {
      12L -> "WorkManager • Every 12h"
      24L -> "WorkManager • Daily (24h)"
      168L -> "WorkManager • Weekly"
      else -> "WorkManager • Every ${hrs}h"
    }
  }
}
