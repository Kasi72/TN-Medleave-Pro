package com.example.domain

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.InitialLeaveSeedData
import com.example.data.SmartLeaveBridge
import com.example.ui.LeaveUiState
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

enum class LeaveAlertType(val badgeLabel: String) {
  AI_RECOMMENDED_BRIDGE("AI OPTIMAL BRIDGE"),
  HIGH_YIELD_BRIDGE("SMART LEAVE BRIDGE"),
  UPCOMING_HOLIDAY("UPCOMING TN GH / RH"),
  COL_CREDIT_EXPIRY("UNUSED GH → COL ALERT")
}

data class LeaveNotificationAlertItem(
  val id: String,
  val alertType: LeaveAlertType,
  val title: String,
  val subtitle: String,
  val bodyText: String,
  val targetDateIso: String,
  val countdownBadge: String,
  val efficiencyBadge: String,
  val complianceTag: String,
  val isAlreadyLoggedInLedger: Boolean = false,
  val bridgeToApply: SmartLeaveBridge? = null
)

class LeaveAlertReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    LeaveNotificationHelper.ensureNotificationChannels(context)
    if (!LeaveNotificationHelper.hasNotificationPermission(context)) return

    val alertId = intent.getStringExtra(EXTRA_ALERT_ID) ?: "scheduled_leave_alert"
    val title = intent.getStringExtra(EXTRA_TITLE) ?: "Upcoming Optimal Leave & Holiday Alert"
    val subtitle = intent.getStringExtra(EXTRA_SUBTITLE) ?: "Dept. of Psychiatry • GEMCH"
    val body = intent.getStringExtra(EXTRA_BODY)
      ?: "Review your upcoming Tamil Nadu Government Holiday & Restricted Holiday bridge opportunity."
    val channelId = intent.getStringExtra(EXTRA_CHANNEL_ID)
      ?: LeaveNotificationHelper.CHANNEL_OPTIMAL_BRIDGES

    LeaveNotificationHelper.dispatchSystemNotification(
      context = context,
      notificationId = alertId.hashCode(),
      channelId = channelId,
      title = title,
      subText = subtitle,
      body = body
    )
  }

  companion object {
    const val EXTRA_ALERT_ID = "extra_alert_id"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_SUBTITLE = "extra_subtitle"
    const val EXTRA_BODY = "extra_body"
    const val EXTRA_CHANNEL_ID = "extra_channel_id"
  }
}

object LeaveNotificationHelper {

  const val CHANNEL_OPTIMAL_BRIDGES = "tn_medleave_optimal_bridges"
  const val CHANNEL_HOLIDAY_REMINDERS = "tn_medleave_holiday_reminders"
  private const val PREFS_NAME = "tn_medleave_notification_prefs"
  private const val KEY_SCHEDULED_IDS = "scheduled_alert_ids"
  private const val KEY_LEAD_DAYS = "lead_days_before_holiday"
  private const val KEY_AUTO_NOTIFY_AI = "auto_notify_ai_recommendations"
  private const val KEY_MASTER_NOTIFICATIONS = "master_notifications_enabled"
  private const val KEY_COL_EXPIRY_ALERTS = "col_expiry_alerts_enabled"
  private const val KEY_UPCOMING_HOLIDAY_ALERTS = "upcoming_holiday_alerts_enabled"
  private const val KEY_YEAR_END_BURN_ALERTS = "year_end_burn_alerts_enabled"
  private const val KEY_CALENDAR_SYNC_ALERTS = "calendar_sync_alerts_enabled"

  fun ensureNotificationChannels(context: Context) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val bridgeChannel = NotificationChannel(
      CHANNEL_OPTIMAL_BRIDGES,
      "Optimal Leave Bridge Recommendations",
      NotificationManager.IMPORTANCE_HIGH
    ).apply {
      description =
        "Push alerts for high-efficiency leave bridges (CL, RH, COL) and AI recommendation engine insights."
      enableVibration(true)
    }

    val holidayChannel = NotificationChannel(
      CHANNEL_HOLIDAY_REMINDERS,
      "Upcoming TN Govt Holidays & COL Credits",
      NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
      description =
        "Advance notifications for upcoming Tamil Nadu Government Holidays (GH), Restricted Holidays (RH), and unused GH Worked COL credits."
    }

    manager.createNotificationChannel(bridgeChannel)
    manager.createNotificationChannel(holidayChannel)
  }

  fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
  }

  fun isMasterNotificationsEnabled(context: Context): Boolean {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getBoolean(KEY_MASTER_NOTIFICATIONS, true)
  }

  fun setMasterNotificationsEnabled(context: Context, enabled: Boolean) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putBoolean(KEY_MASTER_NOTIFICATIONS, enabled)
      .apply()
  }

  fun getLeadDaysPreference(context: Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(KEY_LEAD_DAYS, 3)
  }

  fun setLeadDaysPreference(context: Context, leadDays: Int) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putInt(KEY_LEAD_DAYS, leadDays)
      .apply()
  }

  fun isAutoNotifyAiEnabled(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_AUTO_NOTIFY_AI, true)
  }

  fun setAutoNotifyAiEnabled(context: Context, enabled: Boolean) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putBoolean(KEY_AUTO_NOTIFY_AI, enabled)
      .apply()
  }

  fun isColExpiryAlertEnabled(context: Context): Boolean {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getBoolean(KEY_COL_EXPIRY_ALERTS, true)
  }

  fun setColExpiryAlertEnabled(context: Context, enabled: Boolean) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putBoolean(KEY_COL_EXPIRY_ALERTS, enabled)
      .apply()
  }

  fun isUpcomingHolidayAlertEnabled(context: Context): Boolean {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getBoolean(KEY_UPCOMING_HOLIDAY_ALERTS, true)
  }

  fun setUpcomingHolidayAlertEnabled(context: Context, enabled: Boolean) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putBoolean(KEY_UPCOMING_HOLIDAY_ALERTS, enabled)
      .apply()
  }

  fun isYearEndBurnDownAlertEnabled(context: Context): Boolean {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getBoolean(KEY_YEAR_END_BURN_ALERTS, true)
  }

  fun setYearEndBurnDownAlertEnabled(context: Context, enabled: Boolean) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putBoolean(KEY_YEAR_END_BURN_ALERTS, enabled)
      .apply()
  }

  fun isCalendarSyncAlertEnabled(context: Context): Boolean {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getBoolean(KEY_CALENDAR_SYNC_ALERTS, true)
  }

  fun setCalendarSyncAlertEnabled(context: Context, enabled: Boolean) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putBoolean(KEY_CALENDAR_SYNC_ALERTS, enabled)
      .apply()
  }

  fun sendTestVerificationNotification(context: Context): Boolean {
    ensureNotificationChannels(context)
    if (!hasNotificationPermission(context) || !isMasterNotificationsEnabled(context)) {
      return false
    }
    dispatchSystemNotification(
      context = context,
      notificationId = 88401,
      channelId = CHANNEL_OPTIMAL_BRIDGES,
      title = "TN MedLeave Pro • Push Alerts Verified",
      subText = "Dept. of Psychiatry • GEMCH",
      body = "Smart alerts are active (${getLeadDaysPreference(context)}d lead window) for Leave Bridges, 180-Day COL Expiry, and 2026–2027 Holiday Sync."
    )
    return true
  }

  fun getScheduledAlertIds(context: Context): Set<String> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getStringSet(KEY_SCHEDULED_IDS, emptySet())?.toSet() ?: emptySet()
  }

  /**
   * Analyzes:
   * 1) Live AI Leave Recommendation Engine output (if generated)
   * 2) Deterministic Smart Leave Bridges (from LeaveOptimizerEngine)
   * 3) Upcoming 2026 Tamil Nadu Government Holidays (GH) & Restricted Holidays (RH / RL)
   * 4) Unused GH Worked hospital duty credits available for COL pairing
   *
   * Returns a prioritized list of actionable notification alerts tailored for HOD Psychiatry.
   */
  fun computeSmartNotificationAlerts(
    state: LeaveUiState.Success,
    aiState: AiRecommendationUiState,
    referenceDate: LocalDate = LocalDate.now()
  ): List<LeaveNotificationAlertItem> {
    val alerts = mutableListOf<LeaveNotificationAlertItem>()
    val loggedDates2026 = state.allRecords
      .filter { it.year == 2026L }
      .map { it.dateIso }
      .toSet()

    // 1. Surface AI-Generated Recommendations first when available
    if (aiState is AiRecommendationUiState.Success) {
      aiState.recommendations.take(3).forEach { rec ->
        val startLocal = runCatching { LocalDate.parse(rec.startDateIso) }.getOrNull()
        val countdown = formatCountdownBadge(referenceDate, startLocal)
        val requiredDates = rec.dailyPlan
          .filter { it.creditType.uppercase() in setOf("RH", "CL", "COL") }
          .map { it.dateIso }
        val alreadyLogged = requiredDates.isNotEmpty() && requiredDates.all { it in loggedDates2026 }

        alerts.add(
          LeaveNotificationAlertItem(
            id = "ai_alert_${rec.id}",
            alertType = LeaveAlertType.AI_RECOMMENDED_BRIDGE,
            title = "AI Optimal Window: ${rec.title}",
            subtitle = "${rec.startDateIso} → ${rec.endDateIso} • ${rec.creditMixSummary}",
            bodyText = "${rec.totalDaysOff} consecutive days off using only ${formatCleanDouble(rec.leaveCreditsUsed)} leave credits (${rec.efficiencyMultiplier}x yield). ${rec.whyOptimalForHod}",
            targetDateIso = rec.startDateIso,
            countdownBadge = countdown,
            efficiencyBadge = "${rec.efficiencyMultiplier}x • ${rec.totalDaysOff}d Off",
            complianceTag = rec.tnRuleComplianceCheck,
            isAlreadyLoggedInLedger = alreadyLogged,
            bridgeToApply = rec.toSmartLeaveBridge()
          )
        )
      }
    }

    // 2. Unused GH Worked Credits -> COL Alert (6-Month Statutory Rule)
    val unusedGhList = state.colGhAuditAllYears.unusedGhRecords
    if (unusedGhList.isNotEmpty()) {
      val sampleDates = unusedGhList.take(3).joinToString(", ") { it.dateIso }
      alerts.add(
        LeaveNotificationAlertItem(
          id = "unused_gh_col_credit_alert",
          alertType = LeaveAlertType.COL_CREDIT_EXPIRY,
          title = "${unusedGhList.size} Unused GH Worked Credits Ready for COL",
          subtitle = "Earned on: $sampleDates • Combine with RH/Weekends",
          bodyText = "You have ${unusedGhList.size} unclaimed Government Holiday hospital duty credit(s) ($sampleDates). Under TN Leave Rules, pair them as Compensatory Off Leave (COL) adjacent to an upcoming weekend or Restricted Holiday for 0 CL impact.",
          targetDateIso = unusedGhList.first().dateIso,
          countdownBadge = "ACTIONABLE CREDIT",
          efficiencyBadge = "${unusedGhList.size} COL Free",
          complianceTag = "TN COL Rule: Avail within 6 months of GH Duty",
          isAlreadyLoggedInLedger = false,
          bridgeToApply = null
        )
      )
    }

    // 3. Upcoming Curated High-Yield Smart Bridges (prioritizing upcoming dates relative to referenceDate, then highest efficiency)
    val sortedBridges = state.smartBridges.sortedWith(
      compareBy<SmartLeaveBridge> {
        val d = runCatching { LocalDate.parse(it.startDateIso) }.getOrNull()
        if (d != null && !d.isBefore(referenceDate)) 0 else 1
      }.thenBy {
        runCatching { LocalDate.parse(it.startDateIso) }.getOrNull() ?: LocalDate.of(2026, 12, 31)
      }
    )

    sortedBridges.forEach { bridge ->
      val startLocal = runCatching { LocalDate.parse(bridge.startDateIso) }.getOrNull()
      val countdown = formatCountdownBadge(referenceDate, startLocal)
      val leaveSteps = bridge.dayByDayBreakdown.filter {
        it.badgeType == "RH" || it.badgeType == "LEAVE_CREDIT" || it.badgeType == "COL"
      }
      val alreadyLogged = leaveSteps.isNotEmpty() && leaveSteps.all { it.dateIso in loggedDates2026 }

      alerts.add(
        LeaveNotificationAlertItem(
          id = "bridge_alert_${bridge.id}",
          alertType = LeaveAlertType.HIGH_YIELD_BRIDGE,
          title = bridge.title,
          subtitle = "${bridge.startDateIso} → ${bridge.endDateIso} • ${bridge.recommendedMix}",
          bodyText = "Unlock ${bridge.totalDaysOff} consecutive days off for only ${bridge.leaveCreditsRequired} leave credit(s) (${bridge.efficiencyRatio}x multiplier). ${bridge.tnRuleComplianceNote}",
          targetDateIso = bridge.startDateIso,
          countdownBadge = countdown,
          efficiencyBadge = "${bridge.efficiencyRatio}x • ${bridge.totalDaysOff}d Off",
          complianceTag = "TN FR Annexure VII ≤10d Continuous Cap Verified",
          isAlreadyLoggedInLedger = alreadyLogged,
          bridgeToApply = bridge
        )
      )
    }

    // 4. Upcoming Official 2026 Tamil Nadu Government Holidays (GH) & Restricted Holidays (RH)
    val all2026Holidays = (
      InitialLeaveSeedData.tnGovtPublicHolidays2026 +
        InitialLeaveSeedData.tnRestrictedHolidays2026
      ).sortedBy { it.dateIso }

    val upcomingHolidays = all2026Holidays.filter { hol ->
      val d = runCatching { LocalDate.parse(hol.dateIso) }.getOrNull()
      d != null && !d.isBefore(referenceDate)
    }.ifEmpty {
      all2026Holidays.takeLast(5)
    }

    upcomingHolidays.take(5).forEach { hol ->
      val d = runCatching { LocalDate.parse(hol.dateIso) }.getOrNull()
      val countdown = formatCountdownBadge(referenceDate, d)
      val isLogged = hol.dateIso in loggedDates2026
      val typeLabel = if (hol.isRestrictedHoliday) "Restricted Holiday (RH / RL)" else "TN Govt Public Holiday (GH)"
      val bridgeHint = when (hol.dayOfWeek.lowercase()) {
        "friday", "monday" -> "Direct 3-Day Long Weekend Opportunity!"
        "thursday" -> "Pair Friday as 1 CL/COL for a 4-Day Stretch (4.0x Yield)!"
        "tuesday" -> "Pair Monday as 1 CL/COL for a 4-Day Stretch (4.0x Yield)!"
        else -> "Combine with adjacent weekend or unused GH Worked credit."
      }

      alerts.add(
        LeaveNotificationAlertItem(
          id = "holiday_alert_${hol.dateIso}_${hol.nameEnglish.take(8)}",
          alertType = LeaveAlertType.UPCOMING_HOLIDAY,
          title = "${hol.nameEnglish} (${hol.dayOfWeek})",
          subtitle = "${hol.dateIso} • $typeLabel",
          bodyText = "$typeLabel on ${hol.dateIso} (${hol.dayOfWeek}). $bridgeHint Remaining 2026 quota: ${formatCleanDouble(state.summary2026.clRemaining)} CL, ${state.summary2026.rhRemaining} RH.",
          targetDateIso = hol.dateIso,
          countdownBadge = countdown,
          efficiencyBadge = if (hol.isRestrictedHoliday) "TN RH Quota" else "0 Credit GH",
          complianceTag = bridgeHint,
          isAlreadyLoggedInLedger = isLogged,
          bridgeToApply = null
        )
      )
    }

    return alerts
  }

  fun postNotificationNow(
    context: Context,
    alert: LeaveNotificationAlertItem
  ): Boolean {
    ensureNotificationChannels(context)
    if (!hasNotificationPermission(context)) return false

    val channelId = when (alert.alertType) {
      LeaveAlertType.AI_RECOMMENDED_BRIDGE,
      LeaveAlertType.HIGH_YIELD_BRIDGE -> CHANNEL_OPTIMAL_BRIDGES
      LeaveAlertType.UPCOMING_HOLIDAY,
      LeaveAlertType.COL_CREDIT_EXPIRY -> CHANNEL_HOLIDAY_REMINDERS
    }

    dispatchSystemNotification(
      context = context,
      notificationId = alert.id.hashCode(),
      channelId = channelId,
      title = alert.title,
      subText = "${alert.alertType.badgeLabel} • ${alert.efficiencyBadge}",
      body = "${alert.subtitle}\n${alert.bodyText}"
    )
    return true
  }

  fun postExecutiveDigestNotification(
    context: Context,
    alerts: List<LeaveNotificationAlertItem>
  ): Int {
    ensureNotificationChannels(context)
    if (!hasNotificationPermission(context) || alerts.isEmpty()) return 0

    val topAlerts = alerts.take(3)
    topAlerts.forEach { alert ->
      postNotificationNow(context, alert)
    }

    // Also post a summary digest notification
    val openAppIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
      context,
      9001,
      openAppIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val inboxStyle = NotificationCompat.InboxStyle()
      .setBigContentTitle("HOD Psychiatry • ${alerts.size} Optimal Leave & Holiday Alerts")
      .setSummaryText("Govt. Erode Medical College • TN Leave Optimizer")

    alerts.take(5).forEach { item ->
      inboxStyle.addLine("• ${item.efficiencyBadge}: ${item.title} (${item.targetDateIso})")
    }

    val summaryNotification = NotificationCompat.Builder(context, CHANNEL_OPTIMAL_BRIDGES)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle("TN MedLeave Pro: ${alerts.size} Optimal Leave & Holiday Alerts")
      .setContentText(topAlerts.first().title + " (${topAlerts.first().efficiencyBadge})")
      .setSubText("HOD Psychiatry • Leave Optimizer")
      .setStyle(inboxStyle)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .build()

    return try {
      NotificationManagerCompat.from(context).notify(99001, summaryNotification)
      topAlerts.size
    } catch (_: SecurityException) {
      0
    }
  }

  fun toggleScheduledReminder(
    context: Context,
    alert: LeaveNotificationAlertItem,
    leadDays: Int
  ): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val currentSet = (prefs.getStringSet(KEY_SCHEDULED_IDS, emptySet()) ?: emptySet()).toMutableSet()
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val channelId = when (alert.alertType) {
      LeaveAlertType.AI_RECOMMENDED_BRIDGE,
      LeaveAlertType.HIGH_YIELD_BRIDGE -> CHANNEL_OPTIMAL_BRIDGES
      LeaveAlertType.UPCOMING_HOLIDAY,
      LeaveAlertType.COL_CREDIT_EXPIRY -> CHANNEL_HOLIDAY_REMINDERS
    }

    val broadcastIntent = Intent(context, LeaveAlertReceiver::class.java).apply {
      putExtra(LeaveAlertReceiver.EXTRA_ALERT_ID, alert.id)
      putExtra(LeaveAlertReceiver.EXTRA_TITLE, alert.title)
      putExtra(
        LeaveAlertReceiver.EXTRA_SUBTITLE,
        "${alert.alertType.badgeLabel} • ${alert.efficiencyBadge}"
      )
      putExtra(LeaveAlertReceiver.EXTRA_BODY, "${alert.subtitle}\n${alert.bodyText}")
      putExtra(LeaveAlertReceiver.EXTRA_CHANNEL_ID, channelId)
    }

    val pendingIntent = PendingIntent.getBroadcast(
      context,
      alert.id.hashCode(),
      broadcastIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    return if (alert.id in currentSet) {
      alarmManager.cancel(pendingIntent)
      currentSet.remove(alert.id)
      prefs.edit().putStringSet(KEY_SCHEDULED_IDS, currentSet).apply()
      false
    } else {
      val targetLocal = runCatching { LocalDate.parse(alert.targetDateIso) }.getOrNull()
      val triggerEpochMillis = if (targetLocal != null) {
        val reminderDate = targetLocal.minusDays(leadDays.toLong())
        val effectiveDate = if (reminderDate.isAfter(LocalDate.now())) {
          reminderDate
        } else {
          LocalDate.now()
        }
        val zone = ZoneId.systemDefault()
        val nineAmEpoch = effectiveDate.atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        if (nineAmEpoch > System.currentTimeMillis() + 5_000L) {
          nineAmEpoch
        } else {
          System.currentTimeMillis() + 10_000L
        }
      } else {
        System.currentTimeMillis() + 10_000L
      }

      // Use standard inexact AlarmManager.set(...) which requires zero exact-alarm permissions
      alarmManager.set(
        AlarmManager.RTC_WAKEUP,
        triggerEpochMillis,
        pendingIntent
      )
      currentSet.add(alert.id)
      prefs.edit().putStringSet(KEY_SCHEDULED_IDS, currentSet).apply()
      true
    }
  }

  internal fun dispatchSystemNotification(
    context: Context,
    notificationId: Int,
    channelId: String,
    title: String,
    subText: String,
    body: String
  ) {
    val openAppIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
      context,
      notificationId,
      openAppIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, channelId)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(title)
      .setSubText(subText)
      .setContentText(body.lineSequence().firstOrNull() ?: body)
      .setStyle(
        NotificationCompat.BigTextStyle()
          .setBigContentTitle(title)
          .setSummaryText(subText)
          .bigText(body)
      )
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .build()

    try {
      NotificationManagerCompat.from(context).notify(notificationId, notification)
    } catch (_: SecurityException) {
      // Handled gracefully if permission was revoked
    }
  }

  private fun formatCountdownBadge(referenceDate: LocalDate, targetDate: LocalDate?): String {
    if (targetDate == null) return "2026 WINDOW"
    val diff = ChronoUnit.DAYS.between(referenceDate, targetDate)
    val monthDay = targetDate.format(DateTimeFormatter.ofPattern("MMM dd"))
    return when {
      diff == 0L -> "TODAY ($monthDay)"
      diff == 1L -> "TOMORROW ($monthDay)"
      diff in 2L..60L -> "IN ${diff}D ($monthDay)"
      diff > 60L -> monthDay.uppercase()
      else -> "2026 ($monthDay)"
    }
  }

  private fun formatCleanDouble(value: Double): String {
    return if (value % 1.0 == 0.0) {
      value.toInt().toString()
    } else {
      String.format(java.util.Locale.US, "%.1f", value)
    }
  }
}
