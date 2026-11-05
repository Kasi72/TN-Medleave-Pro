package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R

class MedLeaveQuotaAppWidget : AppWidgetProvider() {

  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray
  ) {
    for (widgetId in appWidgetIds) {
      updateSingleWidget(context, appWidgetManager, widgetId)
    }
  }

  companion object {
    private const val PREFS_WIDGET = "tn_medleave_home_widget_prefs"
    private const val KEY_CL_TEXT = "cl_text"
    private const val KEY_RH_TEXT = "rh_text"
    private const val KEY_COL_TEXT = "col_text"
    private const val KEY_NEXT_HOLIDAY = "next_holiday_text"
    private const val KEY_UPDATED_TIME = "updated_time_text"

    fun syncAndRefreshWidget(
      context: Context,
      clText: String,
      rhText: String,
      colText: String,
      nextHolidayText: String,
      updatedLabel: String = "LIVE 2026"
    ) {
      context.getSharedPreferences(PREFS_WIDGET, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_CL_TEXT, clText)
        .putString(KEY_RH_TEXT, rhText)
        .putString(KEY_COL_TEXT, colText)
        .putString(KEY_NEXT_HOLIDAY, nextHolidayText)
        .putString(KEY_UPDATED_TIME, updatedLabel)
        .apply()

      val manager = AppWidgetManager.getInstance(context)
      val component = ComponentName(context, MedLeaveQuotaAppWidget::class.java)
      val ids = manager.getAppWidgetIds(component)
      if (ids != null && ids.isNotEmpty()) {
        for (id in ids) {
          updateSingleWidget(context, manager, id)
        }
      }
    }

    fun requestPinHomeWidget(context: Context): Boolean {
      val manager = AppWidgetManager.getInstance(context)
      val provider = ComponentName(context, MedLeaveQuotaAppWidget::class.java)
      return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager.isRequestPinAppWidgetSupported) {
        manager.requestPinAppWidget(provider, null, null)
      } else {
        false
      }
    }

    fun readSnapshot(context: Context): WidgetSnapshot {
      val prefs = context.getSharedPreferences(PREFS_WIDGET, Context.MODE_PRIVATE)
      return WidgetSnapshot(
        clText = prefs.getString(KEY_CL_TEXT, "4.5 / 12") ?: "4.5 / 12",
        rhText = prefs.getString(KEY_RH_TEXT, "1 / 3") ?: "1 / 3",
        colText = prefs.getString(KEY_COL_TEXT, "2 READY") ?: "2 READY",
        nextHolidayText = prefs.getString(
          KEY_NEXT_HOLIDAY,
          "★ Next Bridge: Nov 08 Deepavali • Dec 24–27 Christmas (4d Stretch)"
        ) ?: "",
        updatedLabel = prefs.getString(KEY_UPDATED_TIME, "LIVE 2026") ?: "LIVE 2026"
      )
    }

    private fun updateSingleWidget(
      context: Context,
      appWidgetManager: AppWidgetManager,
      appWidgetId: Int
    ) {
      val snap = readSnapshot(context)
      val views = RemoteViews(context.packageName, R.layout.widget_medleave_quota)

      views.setTextViewText(R.id.widget_cl_value, snap.clText)
      views.setTextViewText(R.id.widget_rh_value, snap.rhText)
      views.setTextViewText(R.id.widget_col_value, snap.colText)
      views.setTextViewText(R.id.widget_next_holiday, snap.nextHolidayText)
      views.setTextViewText(R.id.widget_sync_badge, snap.updatedLabel)

      val launchIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
      val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      val pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, pendingFlags)
      views.setOnClickPendingIntent(R.id.widget_root_container, pendingIntent)

      appWidgetManager.updateAppWidget(appWidgetId, views)
    }
  }
}

data class WidgetSnapshot(
  val clText: String,
  val rhText: String,
  val colText: String,
  val nextHolidayText: String,
  val updatedLabel: String
)
