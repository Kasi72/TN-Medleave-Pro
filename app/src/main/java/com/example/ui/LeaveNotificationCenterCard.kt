package com.example.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.SmartLeaveBridge
import com.example.domain.AiRecommendationUiState
import com.example.domain.LeaveAlertType
import com.example.domain.LeaveNotificationAlertItem
import com.example.domain.LeaveNotificationHelper
import com.example.ui.theme.BadgeClGreen
import com.example.ui.theme.BadgeColCyan
import com.example.ui.theme.BadgePlGold
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.IosSharpBadgeShape
import com.example.ui.theme.IosSharpButtonShape
import com.example.ui.theme.IosSharpCardShape
import com.example.ui.theme.IosSharpMicroShape
import com.example.ui.theme.IosSharpWellShape
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalFunctionalActionColors
import com.example.ui.theme.LocalNeumorphicColors
import com.example.ui.theme.StatusAlertRed
import com.example.ui.theme.StatusVerifiedGreen

@Composable
fun SmartHolidayNotificationCard(
  state: LeaveUiState.Success,
  aiState: AiRecommendationUiState,
  onOpenFullNotificationCenter: () -> Unit,
  onApplyBridge: (SmartLeaveBridge) -> Unit,
  onShowStatusBanner: (String) -> Unit
) {
  val context = LocalContext.current
  LaunchedEffect(Unit) {
    LeaveNotificationHelper.ensureNotificationChannels(context)
  }

  var hasPermission by remember {
    mutableStateOf(LeaveNotificationHelper.hasNotificationPermission(context))
  }
  var leadDays by remember {
    mutableIntStateOf(LeaveNotificationHelper.getLeadDaysPreference(context))
  }
  var autoNotifyAi by remember {
    mutableStateOf(LeaveNotificationHelper.isAutoNotifyAiEnabled(context))
  }
  var scheduledIds by remember {
    mutableStateOf(LeaveNotificationHelper.getScheduledAlertIds(context))
  }

  val alerts = remember(state.allRecords, state.smartBridges, aiState) {
    LeaveNotificationHelper.computeSmartNotificationAlerts(state, aiState)
  }

  var pendingActionAfterPermission by remember { mutableStateOf<(() -> Unit)?>(null) }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    hasPermission = granted
    if (granted) {
      pendingActionAfterPermission?.invoke()
      pendingActionAfterPermission = null
    } else {
      onShowStatusBanner("Notification permission is required to display system push alerts.")
    }
  }

  fun runWithNotificationPermission(action: () -> Unit) {
    if (LeaveNotificationHelper.hasNotificationPermission(context)) {
      hasPermission = true
      action()
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      pendingActionAfterPermission = action
      permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    } else {
      action()
    }
  }

  // Automatically push a system notification when new AI recommendations arrive if autoNotifyAi is enabled
  LaunchedEffect(aiState) {
    if (aiState is AiRecommendationUiState.Success &&
      autoNotifyAi &&
      LeaveNotificationHelper.hasNotificationPermission(context)
    ) {
      val topAiAlert = alerts.firstOrNull { it.alertType == LeaveAlertType.AI_RECOMMENDED_BRIDGE }
      if (topAiAlert != null) {
        LeaveNotificationHelper.postNotificationNow(context, topAiAlert)
      }
    }
  }

  val neu = LocalNeumorphicColors.current
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .neumorphicExtruded(
        shape = IosSharpCardShape,
        elevation = 5.dp,
        blurRadius = 10.dp,
        accentTint = neu.primaryAccent,
        accentTintAlpha = 0.03f,
        borderAlpha = 0.30f,
        neu = neu
      )
      .testTag("smart_holiday_notification_card")
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // Header Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .neumorphicInset(
                shape = IosSharpButtonShape,
                depth = 2.5.dp,
                blurRadius = 5.dp,
                accentTint = neu.primaryAccent,
                accentTintAlpha = 0.14f,
                neu = neu
              ),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.NotificationsActive,
              contentDescription = null,
              tint = neu.primaryAccent,
              modifier = Modifier.size(20.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "PROACTIVE LEAVE & HOLIDAY ALERTS",
              style = MaterialTheme.typography.labelMedium,
              color = neu.primaryAccent,
              fontSize = 10.sp,
              lineHeight = 13.sp,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "Smart Push Notification System",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = neu.textPrimary,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        val statusAccent = if (hasPermission) neu.primaryAccent else BadgePlGold
        Box(
          modifier = Modifier
            .neumorphicInset(
              shape = IosSharpBadgeShape,
              depth = 2.dp,
              blurRadius = 4.dp,
              accentTint = statusAccent,
              accentTintAlpha = 0.12f,
              borderColor = statusAccent.copy(alpha = 0.40f),
              neu = neu
            )
        ) {
          Text(
            text = if (hasPermission) "${alerts.size} Active" else "Enable Push",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = JetBrainsMonoFontFamily,
            color = statusAccent,
            maxLines = 1,
            softWrap = false
          )
        }
      }

      Text(
        text = "Alerts you ahead of upcoming Tamil Nadu Government Holidays (GH), Restricted Holidays (RH), unused GH Worked COL credits, and optimal 4x–4.5x leave bridges.",
        style = MaterialTheme.typography.bodyMedium,
        color = neu.textSecondary,
        fontSize = 12.sp,
        lineHeight = 16.sp
      )

      // Lead-Time Selector Inset Trough
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .neumorphicInset(
            shape = IosSharpWellShape,
            depth = 2.5.dp,
            blurRadius = 5.dp,
            neu = neu
          )
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          listOf(1 to "1d Ahead", 3 to "3d Ahead", 7 to "7d Ahead").forEach { (days, label) ->
            val isSelected = leadDays == days
            Box(
              modifier = Modifier
                .weight(1f)
                .height(34.dp)
                .then(
                  if (isSelected) {
                    Modifier.neumorphicExtruded(
                      shape = IosSharpButtonShape,
                      elevation = 2.5.dp,
                      blurRadius = 5.dp,
                      surfaceColor = neu.primaryAccent,
                      neu = neu
                    )
                  } else {
                    Modifier.clip(IosSharpButtonShape)
                  }
                )
                .clickable {
                  leadDays = days
                  LeaveNotificationHelper.setLeadDaysPreference(context, days)
                  onShowStatusBanner("Holiday & Optimal Leave reminder lead-time set to $days day(s) before.")
                },
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = label,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else neu.textSecondary,
                maxLines = 1,
                softWrap = false
              )
            }
          }
        }
      }

      // Preview Top 2 Priority Alerts right inside the card
      alerts.take(2).forEach { alert ->
        val isScheduled = alert.id in scheduledIds
        CompactAlertPreviewRow(
          alert = alert,
          isScheduled = isScheduled,
          onPushNow = {
            runWithNotificationPermission {
              val posted = LeaveNotificationHelper.postNotificationNow(context, alert)
              if (posted) {
                onShowStatusBanner("Push notification sent: '${alert.title}'")
              }
            }
          },
          onToggleSchedule = {
            runWithNotificationPermission {
              val nowScheduled = LeaveNotificationHelper.toggleScheduledReminder(
                context = context,
                alert = alert,
                leadDays = leadDays
              )
              scheduledIds = LeaveNotificationHelper.getScheduledAlertIds(context)
              if (nowScheduled) {
                LeaveNotificationHelper.postNotificationNow(context, alert)
                onShowStatusBanner("Scheduled reminder ($leadDays days ahead) & sent push alert for '${alert.title}'!")
              } else {
                onShowStatusBanner("Cancelled scheduled reminder for '${alert.title}'.")
              }
            }
          }
        )
      }

      // Action Buttons: Send Digest Notification Now & Open Full Alert Center
      val actions = LocalFunctionalActionColors.current
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedButton(
          onClick = onOpenFullNotificationCenter,
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.2.dp, actions.aiOptimize),
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .testTag("open_notification_center_button")
        ) {
          Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            tint = actions.aiOptimize,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "All ${alerts.size} Alerts",
            color = actions.aiOptimize,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
          )
        }

        Button(
          onClick = {
            runWithNotificationPermission {
              val count = LeaveNotificationHelper.postExecutiveDigestNotification(context, alerts)
              if (count > 0) {
                onShowStatusBanner("Dispatched $count optimal leave & holiday push notifications to your status bar!")
              }
            }
          },
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = actions.shareHandover,
            contentColor = Color.White
          ),
          modifier = Modifier
            .weight(1.3f)
            .fillMaxHeight()
            .testTag("send_all_leave_notifications_button")
        ) {
          Icon(
            imageVector = Icons.Default.Send,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Push Optimal Alerts Now",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }
  }
}

@Composable
private fun CompactAlertPreviewRow(
  alert: LeaveNotificationAlertItem,
  isScheduled: Boolean,
  onPushNow: () -> Unit,
  onToggleSchedule: () -> Unit
) {
  val accentColor = when (alert.alertType) {
    LeaveAlertType.AI_RECOMMENDED_BRIDGE -> EmeraldOptimal
    LeaveAlertType.HIGH_YIELD_BRIDGE -> BadgeColCyan
    LeaveAlertType.UPCOMING_HOLIDAY -> BadgePlGold
    LeaveAlertType.COL_CREDIT_EXPIRY -> StatusAlertRed
  }

  val neu = LocalNeumorphicColors.current
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .neumorphicInset(
        shape = IosSharpWellShape,
        depth = 2.5.dp,
        blurRadius = 5.dp,
        accentTint = accentColor,
        accentTintAlpha = 0.05f,
        borderColor = accentColor.copy(alpha = 0.25f),
        neu = neu
      )
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "${alert.alertType.badgeLabel} • ${alert.countdownBadge}",
          color = accentColor,
          fontSize = 10.sp,
          lineHeight = 13.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = JetBrainsMonoFontFamily,
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(
          modifier = Modifier.neumorphicInset(
            shape = IosSharpMicroShape,
            depth = 1.5.dp,
            blurRadius = 3.dp,
            accentTint = accentColor,
            accentTintAlpha = 0.14f,
            borderColor = accentColor.copy(alpha = 0.28f),
            neu = neu
          )
        ) {
          Text(
            text = alert.efficiencyBadge,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            color = accentColor,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
          )
        }
      }

      Text(
        text = alert.title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        color = MaterialTheme.colorScheme.onSurface
      )

      Text(
        text = alert.subtitle,
        style = MaterialTheme.typography.bodyMedium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .then(
              if (isScheduled) {
                Modifier.neumorphicExtruded(
                  shape = IosSharpMicroShape,
                  elevation = 2.dp,
                  blurRadius = 4.dp,
                  surfaceColor = EmeraldOptimal,
                  neu = neu
                )
              } else {
                Modifier.neumorphicExtruded(
                  shape = IosSharpMicroShape,
                  elevation = 2.dp,
                  blurRadius = 4.dp,
                  borderAlpha = 0.22f,
                  neu = neu
                )
              }
            )
            .clickable { onToggleSchedule() }
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = if (isScheduled) Icons.Default.AlarmOn else Icons.Default.Schedule,
              contentDescription = null,
              tint = if (isScheduled) Color.White else neu.textSecondary,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (isScheduled) "Reminder Active" else "Remind Me",
              fontSize = 11.sp,
              lineHeight = 14.sp,
              fontWeight = FontWeight.Bold,
              color = if (isScheduled) Color.White else neu.textSecondary,
              maxLines = 1,
              softWrap = false
            )
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
          modifier = Modifier
            .neumorphicExtruded(
              shape = IosSharpMicroShape,
              elevation = 2.dp,
              blurRadius = 4.dp,
              surfaceColor = accentColor,
              neu = neu
            )
            .clickable { onPushNow() }
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.NotificationsActive,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Push Alert",
              fontSize = 11.sp,
              lineHeight = 14.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White,
              maxLines = 1,
              softWrap = false
            )
          }
        }
      }
    }
  }
}

@Composable
fun SmartNotificationCenterDialog(
  state: LeaveUiState.Success,
  aiState: AiRecommendationUiState,
  onDismiss: () -> Unit,
  onApplyBridge: (SmartLeaveBridge) -> Unit,
  onShowStatusBanner: (String) -> Unit,
  onOptimizeInAi: (String) -> Unit = {}
) {
  val context = LocalContext.current
  val designation = remember { HodProfileSettingsStore.getDesignation(context) }
  var selectedFilter by remember { mutableStateOf<LeaveAlertType?>(null) }
  var leadDays by remember {
    mutableIntStateOf(LeaveNotificationHelper.getLeadDaysPreference(context))
  }
  var autoNotifyAi by remember {
    mutableStateOf(LeaveNotificationHelper.isAutoNotifyAiEnabled(context))
  }
  var scheduledIds by remember {
    mutableStateOf(LeaveNotificationHelper.getScheduledAlertIds(context))
  }
  var localBanner by remember { mutableStateOf<String?>(null) }

  val allAlerts = remember(state.allRecords, state.smartBridges, aiState) {
    LeaveNotificationHelper.computeSmartNotificationAlerts(state, aiState)
  }
  val filteredAlerts = remember(allAlerts, selectedFilter) {
    if (selectedFilter == null) allAlerts else allAlerts.filter { it.alertType == selectedFilter }
  }

  var pendingActionAfterPermission by remember { mutableStateOf<(() -> Unit)?>(null) }
  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) {
      pendingActionAfterPermission?.invoke()
      pendingActionAfterPermission = null
    } else {
      localBanner = "Please allow notification permission to receive Android system alerts."
    }
  }

  fun runWithPermission(action: () -> Unit) {
    if (LeaveNotificationHelper.hasNotificationPermission(context)) {
      action()
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      pendingActionAfterPermission = action
      permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    } else {
      action()
    }
  }

  val neu = LocalNeumorphicColors.current
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 12.dp, vertical = 20.dp)
        .neumorphicExtruded(
          shape = IosSharpCardShape,
          elevation = 8.dp,
          blurRadius = 16.dp,
          borderAlpha = 0.35f,
          neu = neu
        )
        .testTag("smart_notification_center_dialog")
    ) {
      IosAmbientGlassBackdrop(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
          // Neumorphic Executive Header Banner
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(start = 14.dp, top = 14.dp, end = 14.dp, bottom = 6.dp)
              .neumorphicExtruded(
                shape = IosSharpCardShape,
                elevation = 4.dp,
                blurRadius = 8.dp,
                accentTint = neu.primaryAccent,
                accentTintAlpha = 0.04f,
                borderAlpha = 0.30f,
                neu = neu
              )
              .padding(horizontal = 16.dp, vertical = 14.dp)
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.weight(1f)
                ) {
                  Box(
                    modifier = Modifier
                      .size(38.dp)
                      .neumorphicInset(
                        shape = IosSharpButtonShape,
                        depth = 2.5.dp,
                        blurRadius = 5.dp,
                        accentTint = neu.primaryAccent,
                        accentTintAlpha = 0.14f,
                        neu = neu
                      ),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = Icons.Default.NotificationsActive,
                      contentDescription = null,
                      tint = neu.primaryAccent,
                      modifier = Modifier.size(20.dp)
                    )
                  }
                  Spacer(modifier = Modifier.width(10.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "${designation.uppercase()} • SMART PUSH ALERTS",
                      style = MaterialTheme.typography.labelMedium,
                      color = neu.primaryAccent,
                      fontSize = 10.sp,
                      lineHeight = 13.sp,
                      maxLines = 1,
                      softWrap = false,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(
                      text = "Holiday & Optimal Leave Notifications",
                      style = MaterialTheme.typography.titleMedium,
                      color = neu.textPrimary,
                      fontWeight = FontWeight.Bold,
                      maxLines = 1,
                      softWrap = false,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }

                Box(
                  modifier = Modifier
                    .size(34.dp)
                    .neumorphicExtruded(
                      shape = CircleShape,
                      elevation = 2.5.dp,
                      blurRadius = 5.dp,
                      neu = neu
                    )
                    .clickable { onDismiss() }
                    .testTag("close_notification_center_button"),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Notification Center",
                    tint = neu.textPrimary,
                    modifier = Modifier.size(17.dp)
                  )
                }
              }

              // Auto-Notify AI Switch Row
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Auto-Push on AI Leave Analysis",
                    color = neu.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = "Automatically post a push alert when Gemini identifies a new 4x+ bridge",
                    color = neu.textSecondary,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                  )
                }
                Spacer(modifier = Modifier.width(10.dp))
                NeumorphicSwitch(
                  checked = autoNotifyAi,
                  onCheckedChange = { enabled ->
                    autoNotifyAi = enabled
                    LeaveNotificationHelper.setAutoNotifyAiEnabled(context, enabled)
                  },
                  accentColor = neu.primaryAccent
                )
              }

              // Filter Chips
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState())
                  .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                val filters = listOf(
                  null to "All (${allAlerts.size})",
                  LeaveAlertType.HIGH_YIELD_BRIDGE to "Optimal Bridges",
                  LeaveAlertType.AI_RECOMMENDED_BRIDGE to "AI Windows",
                  LeaveAlertType.UPCOMING_HOLIDAY to "Upcoming GH / RH",
                  LeaveAlertType.COL_CREDIT_EXPIRY to "Unused GH / COL"
                )
                filters.forEach { (type, label) ->
                  val isSelected = selectedFilter == type
                  Box(
                    modifier = Modifier
                      .height(32.dp)
                      .then(
                        if (isSelected) {
                          Modifier.neumorphicExtruded(
                            shape = IosSharpBadgeShape,
                            elevation = 2.5.dp,
                            blurRadius = 5.dp,
                            surfaceColor = neu.primaryAccent,
                            neu = neu
                          )
                        } else {
                          Modifier.neumorphicInset(
                            shape = IosSharpBadgeShape,
                            depth = 2.dp,
                            blurRadius = 4.dp,
                            neu = neu
                          )
                        }
                      )
                      .clickable { selectedFilter = type },
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = label,
                      color = if (isSelected) Color.White else neu.textSecondary,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      maxLines = 1,
                      softWrap = false,
                      modifier = Modifier.padding(horizontal = 12.dp)
                    )
                  }
                }
              }
            }
          }

        // Local confirmation banner inside modal
        localBanner?.let { msg ->
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 6.dp)
              .neumorphicInset(
                shape = IosSharpWellShape,
                depth = 2.dp,
                blurRadius = 4.dp,
                accentTint = EmeraldOptimal,
                accentTintAlpha = 0.10f,
                borderColor = EmeraldOptimal.copy(alpha = 0.35f),
                neu = neu
              )
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = EmeraldOptimal,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = msg,
                style = MaterialTheme.typography.labelLarge,
                color = EmeraldOptimal
              )
            }
          }
        }

        // Alert Cards List
        LazyColumn(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          items(filteredAlerts, key = { it.id }) { alert ->
            val isScheduled = alert.id in scheduledIds
            FullNotificationAlertCard(
              alert = alert,
              isScheduled = isScheduled,
              leadDays = leadDays,
              onPushNow = {
                runWithPermission {
                  val ok = LeaveNotificationHelper.postNotificationNow(context, alert)
                  if (ok) {
                    val msg = "Dispatched push notification for '${alert.title}'!"
                    localBanner = msg
                    onShowStatusBanner(msg)
                  }
                }
              },
              onToggleReminder = {
                runWithPermission {
                  val active = LeaveNotificationHelper.toggleScheduledReminder(
                    context = context,
                    alert = alert,
                    leadDays = leadDays
                  )
                  scheduledIds = LeaveNotificationHelper.getScheduledAlertIds(context)
                  if (active) {
                    LeaveNotificationHelper.postNotificationNow(context, alert)
                    val friendlyDate = ClinicalDateFormatter.formatFriendlyDate(alert.targetDateIso)
                    val msg = "Reminder scheduled ($leadDays days before $friendlyDate) & push notification sent!"
                    localBanner = msg
                    onShowStatusBanner(msg)
                  } else {
                    localBanner = "Cancelled reminder for '${alert.title}'."
                  }
                }
              },
              onApplyBridge = { bridge ->
                onApplyBridge(bridge)
                localBanner = "Applied '${bridge.title}' directly to your ${state.selectedYear} Leave Ledger!"
              },
              onOptimizeInAi = { goalHint ->
                onDismiss()
                onOptimizeInAi(goalHint)
              }
            )
          }
        }

        // Bottom Bar: Push Executive Digest Notification
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .neumorphicExtruded(
              shape = IosSharpButtonShape,
              elevation = 4.dp,
              blurRadius = 8.dp,
              surfaceColor = neu.primaryAccent,
              neu = neu
            )
            .clickable {
              runWithPermission {
                val sent = LeaveNotificationHelper.postExecutiveDigestNotification(
                  context = context,
                  alerts = allAlerts
                )
                val msg = "Sent $sent optimal leave & holiday push alerts to your Android notification shade!"
                localBanner = msg
                onShowStatusBanner(msg)
              }
            }
            .height(48.dp)
            .testTag("modal_send_digest_notification_button"),
          contentAlignment = Alignment.Center
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 14.dp)
          ) {
            Icon(
              imageVector = Icons.Default.NotificationsActive,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Send All Optimal Leave & Holiday Push Alerts Now",
              fontWeight = FontWeight.Bold,
              color = Color.White,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }
      }
    }
  }
}

@Composable
private fun FullNotificationAlertCard(
  alert: LeaveNotificationAlertItem,
  isScheduled: Boolean,
  leadDays: Int,
  onPushNow: () -> Unit,
  onToggleReminder: () -> Unit,
  onApplyBridge: (SmartLeaveBridge) -> Unit,
  onOptimizeInAi: (String) -> Unit = {}
) {
  val neu = LocalNeumorphicColors.current
  val accentColor = when (alert.alertType) {
    LeaveAlertType.AI_RECOMMENDED_BRIDGE -> EmeraldOptimal
    LeaveAlertType.HIGH_YIELD_BRIDGE -> BadgeColCyan
    LeaveAlertType.UPCOMING_HOLIDAY -> BadgePlGold
    LeaveAlertType.COL_CREDIT_EXPIRY -> StatusAlertRed
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .neumorphicExtruded(
        shape = IosSharpCardShape,
        elevation = 4.5.dp,
        blurRadius = 9.dp,
        accentTint = accentColor,
        accentTintAlpha = 0.03f,
        borderAlpha = 0.28f,
        neu = neu
      )
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(accentColor)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "${alert.alertType.badgeLabel} • ${alert.countdownBadge}",
            color = accentColor,
            fontSize = 10.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = JetBrainsMonoFontFamily,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
          )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
          modifier = Modifier.neumorphicInset(
            shape = IosSharpMicroShape,
            depth = 1.5.dp,
            blurRadius = 3.dp,
            accentTint = accentColor,
            accentTintAlpha = 0.14f,
            borderColor = accentColor.copy(alpha = 0.30f),
            neu = neu
          )
        ) {
          Text(
            text = alert.efficiencyBadge,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = accentColor,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
          )
        }
      }

      Text(
        text = alert.title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )

      Text(
        text = alert.subtitle,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontSize = 12.sp
      )

      Text(
        text = alert.bodyText,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp,
        lineHeight = 16.sp
      )

      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

      val actionsModal = LocalFunctionalActionColors.current
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = onToggleReminder,
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(
            1.dp,
            if (isScheduled) actionsModal.verifyApprove else MaterialTheme.colorScheme.outline
          ),
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
        ) {
          Icon(
            imageVector = if (isScheduled) Icons.Default.AlarmOn else Icons.Default.Schedule,
            contentDescription = null,
            tint = if (isScheduled) actionsModal.verifyApprove else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(15.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = if (isScheduled) "Scheduled (${leadDays}d)" else "Remind (${leadDays}d)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isScheduled) actionsModal.verifyApprove else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
          )
        }

        Button(
          onClick = onPushNow,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = actionsModal.shareHandover,
            contentColor = Color.White
          ),
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
        ) {
          Icon(
            imageVector = Icons.Default.NotificationsActive,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(15.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Push Alert Now",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      if (alert.bridgeToApply != null && !alert.isAlreadyLoggedInLedger) {
        Button(
          onClick = { onApplyBridge(alert.bridgeToApply) },
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = actionsModal.createPrimary,
            contentColor = Color.White
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(15.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "One-Tap Apply Recommended Dates to Ledger",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      if (!alert.optimizerGoalHint.isNullOrBlank()) {
        OutlinedButton(
          onClick = { onOptimizeInAi(alert.optimizerGoalHint) },
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.2.dp, actionsModal.aiOptimize),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("optimize_alert_in_ai_${alert.id}")
        ) {
          Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = actionsModal.aiOptimize,
            modifier = Modifier.size(15.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Optimize in AI Planner →",
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = actionsModal.aiOptimize,
            maxLines = 1,
            softWrap = false
          )
        }
      }
    }
  }
}
