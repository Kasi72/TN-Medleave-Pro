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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.ui.theme.JetBrainsMonoFontFamily
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

  Surface(
    shape = RoundedCornerShape(26.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    border = BorderStroke(1.dp, EmeraldOptimal.copy(alpha = 0.3f)),
    modifier = Modifier
      .fillMaxWidth()
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
              .clip(RoundedCornerShape(12.dp))
              .background(EmeraldOptimal.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.NotificationsActive,
              contentDescription = null,
              tint = EmeraldOptimal,
              modifier = Modifier.size(20.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "PROACTIVE LEAVE & HOLIDAY ALERTS",
              style = MaterialTheme.typography.labelMedium,
              color = EmeraldOptimal,
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
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = if (hasPermission) EmeraldOptimal.copy(alpha = 0.14f) else BadgePlGold.copy(alpha = 0.16f)
        ) {
          Text(
            text = if (hasPermission) "${alerts.size} Active" else "Enable Push",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = JetBrainsMonoFontFamily,
            color = if (hasPermission) EmeraldOptimal else BadgePlGold,
            maxLines = 1,
            softWrap = false
          )
        }
      }

      Text(
        text = "Alerts you ahead of upcoming Tamil Nadu Government Holidays (GH), Restricted Holidays (RH), unused GH Worked COL credits, and optimal 4x–4.5x leave bridges.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp,
        lineHeight = 16.sp
      )

      // Lead-Time Selector & Auto-AI Push Toggle Row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        listOf(1 to "1d Ahead", 3 to "3d Ahead", 7 to "7d Ahead").forEach { (days, label) ->
          val isSelected = leadDays == days
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) EmeraldOptimal else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
              .weight(1f)
              .height(34.dp)
              .clip(RoundedCornerShape(12.dp))
              .clickable {
                leadDays = days
                LeaveNotificationHelper.setLeadDaysPreference(context, days)
                onShowStatusBanner("Holiday & Optimal Leave reminder lead-time set to $days day(s) before.")
              }
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(
                text = label,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
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
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedButton(
          onClick = onOpenFullNotificationCenter,
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.2.dp, EmeraldOptimal),
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .testTag("open_notification_center_button")
        ) {
          Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            tint = EmeraldOptimal,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "All ${alerts.size} Alerts",
            color = EmeraldOptimal,
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
          colors = ButtonDefaults.buttonColors(containerColor = EmeraldOptimal),
          modifier = Modifier
            .weight(1.3f)
            .fillMaxHeight()
            .testTag("send_all_leave_notifications_button")
        ) {
          Icon(
            imageVector = Icons.Default.Send,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Push Optimal Alerts Now",
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

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = accentColor.copy(alpha = 0.08f),
    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f)),
    modifier = Modifier.fillMaxWidth()
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
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = accentColor.copy(alpha = 0.16f)
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
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = if (isScheduled) EmeraldOptimal else MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onToggleSchedule() }
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = if (isScheduled) Icons.Default.AlarmOn else Icons.Default.Schedule,
              contentDescription = null,
              tint = if (isScheduled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (isScheduled) "Reminder Active" else "Remind Me",
              fontSize = 11.sp,
              lineHeight = 14.sp,
              fontWeight = FontWeight.Bold,
              color = if (isScheduled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              softWrap = false
            )
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = accentColor,
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
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
  onShowStatusBanner: (String) -> Unit
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

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(26.dp),
      color = MaterialTheme.colorScheme.background,
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 12.dp, vertical = 20.dp)
        .testTag("smart_notification_center_dialog")
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // Header Banner
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(
              Brush.linearGradient(
                colors = listOf(
                  Color(0xFF0A2540),
                  Color(0xFF0F3443),
                  Color(0xFF065F46)
                )
              )
            )
            .padding(horizontal = 18.dp, vertical = 16.dp)
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
                Icon(
                  imageVector = Icons.Default.NotificationsActive,
                  contentDescription = null,
                  tint = Color(0xFF34D399),
                  modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "${designation.uppercase()} • SMART PUSH ALERTS",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF34D399),
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = "Holiday & Optimal Leave Notifications",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }

              IconButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_notification_center_button")
              ) {
                Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = "Close Notification Center",
                  tint = Color.White
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
                  color = Color.White,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "Automatically post a push alert when Gemini identifies a new 4x+ bridge",
                  color = Color(0xFFBAE6FD),
                  fontSize = 11.sp,
                  lineHeight = 14.sp
                )
              }
              Switch(
                checked = autoNotifyAi,
                onCheckedChange = { enabled ->
                  autoNotifyAi = enabled
                  LeaveNotificationHelper.setAutoNotifyAiEnabled(context, enabled)
                },
                colors = SwitchDefaults.colors(
                  checkedThumbColor = Color.White,
                  checkedTrackColor = Color(0xFF10B981)
                )
              )
            }

            // Filter Chips
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
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
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = if (isSelected) Color(0xFF10B981) else Color.White.copy(alpha = 0.12f),
                  modifier = Modifier
                    .height(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { selectedFilter = type }
                ) {
                  Box(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = label,
                      color = if (isSelected) Color(0xFF052E16) else Color.White,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      maxLines = 1,
                      softWrap = false
                    )
                  }
                }
              }
            }
          }
        }

        // Local confirmation banner inside modal
        localBanner?.let { msg ->
          Surface(
            color = EmeraldOptimal.copy(alpha = 0.15f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
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
              }
            )
          }
        }

        // Bottom Bar: Push Executive Digest Notification
        Surface(
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 4.dp,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Button(
              onClick = {
                runWithPermission {
                  val sent = LeaveNotificationHelper.postExecutiveDigestNotification(
                    context = context,
                    alerts = allAlerts
                  )
                  val msg = "Sent $sent optimal leave & holiday push alerts to your Android notification shade!"
                  localBanner = msg
                  onShowStatusBanner(msg)
                }
              },
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(containerColor = EmeraldOptimal),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("modal_send_digest_notification_button")
            ) {
              Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Send All Optimal Leave & Holiday Push Alerts Now",
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
  }
}

@Composable
private fun FullNotificationAlertCard(
  alert: LeaveNotificationAlertItem,
  isScheduled: Boolean,
  leadDays: Int,
  onPushNow: () -> Unit,
  onToggleReminder: () -> Unit,
  onApplyBridge: (SmartLeaveBridge) -> Unit
) {
  val accentColor = when (alert.alertType) {
    LeaveAlertType.AI_RECOMMENDED_BRIDGE -> EmeraldOptimal
    LeaveAlertType.HIGH_YIELD_BRIDGE -> BadgeColCyan
    LeaveAlertType.UPCOMING_HOLIDAY -> BadgePlGold
    LeaveAlertType.COL_CREDIT_EXPIRY -> StatusAlertRed
  }

  Surface(
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
    modifier = Modifier.fillMaxWidth()
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

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = accentColor.copy(alpha = 0.14f)
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
            if (isScheduled) StatusVerifiedGreen else MaterialTheme.colorScheme.outline
          ),
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
        ) {
          Icon(
            imageVector = if (isScheduled) Icons.Default.AlarmOn else Icons.Default.Schedule,
            contentDescription = null,
            tint = if (isScheduled) StatusVerifiedGreen else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(15.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = if (isScheduled) "Scheduled (${leadDays}d)" else "Remind (${leadDays}d)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isScheduled) StatusVerifiedGreen else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
          )
        }

        Button(
          onClick = onPushNow,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = accentColor),
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
        ) {
          Icon(
            imageVector = Icons.Default.NotificationsActive,
            contentDescription = null,
            modifier = Modifier.size(15.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Push Alert Now",
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
            containerColor = EmeraldOptimal.copy(alpha = 0.14f),
            contentColor = EmeraldOptimal
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(15.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "One-Tap Apply Recommended Dates to 2026 Ledger",
            fontSize = 12.sp,
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
