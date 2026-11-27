package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TnHolidayItem
import com.example.domain.HolidaySyncStatus
import com.example.domain.TnHolidaySyncScheduler
import com.example.ui.theme.BadgeClGreen
import com.example.ui.theme.BadgeColCyan
import com.example.ui.theme.BadgePlGold
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.IosSharpBadgeShape
import com.example.ui.theme.IosSharpButtonShape
import com.example.ui.theme.IosSharpCardShape
import com.example.ui.theme.IosSharpWellShape
import com.example.ui.theme.LocalFunctionalActionColors
import com.example.ui.theme.LocalNeumorphicColors
import com.example.ui.theme.StatusVerifiedGreen
import androidx.compose.material3.OutlinedTextFieldDefaults
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HolidaySyncStatusCard(
  syncStatus: HolidaySyncStatus,
  onTriggerSyncNow: (customUrlOrNote: String) -> Unit,
  onAddLocalHolidayToRoom: (TnHolidayItem) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val neu = LocalNeumorphicColors.current
  var showAddOrScrapeDialog by remember { mutableStateOf(false) }
  var selectedIntervalHours by remember {
    mutableStateOf(TnHolidaySyncScheduler.getSyncIntervalHours(context))
  }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .neumorphicExtruded(
        shape = IosSharpCardShape,
        elevation = 5.dp,
        blurRadius = 10.dp,
        accentTint = BadgeColCyan,
        accentTintAlpha = 0.04f,
        borderAlpha = 0.34f,
        neu = neu
      )
      .testTag("holiday_sync_status_card")
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
                accentTint = BadgeColCyan,
                accentTintAlpha = 0.14f,
                neu = neu
              ),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.CloudSync,
              contentDescription = null,
              tint = BadgeColCyan,
              modifier = Modifier.size(20.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "BACKGROUND SYNC • ROOM LOCAL DB",
              style = MaterialTheme.typography.labelMedium,
              color = BadgeColCyan,
              fontSize = 10.sp,
              lineHeight = 13.sp,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "TN Govt Holiday Web Scraper & Cache",
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

        val statusAccent = if (syncStatus.isSyncing) BadgePlGold else EmeraldOptimal
        Box(
          modifier = Modifier
            .neumorphicInset(
              shape = IosSharpBadgeShape,
              depth = 2.dp,
              blurRadius = 4.dp,
              accentTint = statusAccent,
              accentTintAlpha = 0.12f,
              borderColor = statusAccent.copy(alpha = 0.35f),
              neu = neu
            )
        ) {
          Text(
            text = if (syncStatus.isSyncing) "SYNCING..." else syncStatus.workerStateLabel.take(18),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            fontSize = 10.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = statusAccent,
            maxLines = 1,
            softWrap = false
          )
        }
      }

      // WorkManager Auto-Sync & AI Optimizer Knowledge Base Cadence Bar
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .neumorphicInset(
            shape = IosSharpWellShape,
            depth = 2.dp,
            blurRadius = 4.dp,
            accentTint = neu.primaryAccent,
            accentTintAlpha = 0.06f,
            borderColor = neu.primaryAccent.copy(alpha = 0.25f),
            neu = neu
          )
          .padding(horizontal = 10.dp, vertical = 8.dp)
          .testTag("workmanager_cadence_bar")
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "${syncStatus.workerScheduleLabel} • AI KB v${syncStatus.knowledgeBaseRevision}",
              fontSize = 10.5.sp,
              fontWeight = FontWeight.ExtraBold,
              color = neu.primaryAccent,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = if (syncStatus.apiHolidaysParsedCount > 0) {
                "Auto-fetches TN Public API (${syncStatus.apiHolidaysParsedCount} API dates verified)"
              } else {
                "Auto-fetches Nager.Date IN-TN, OpenHolidays & tn.gov.in"
              },
              fontSize = 10.sp,
              color = neu.textSecondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
          Spacer(modifier = Modifier.width(6.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(12L to "12h", 24L to "24h", 168L to "7d").forEach { (hrs, label) ->
              val isChosen = selectedIntervalHours == hrs
              Box(
                modifier = Modifier
                  .then(
                    if (isChosen) {
                      Modifier.neumorphicExtruded(
                        shape = IosSharpBadgeShape,
                        elevation = 1.8.dp,
                        blurRadius = 3.5.dp,
                        surfaceColor = neu.primaryAccent,
                        neu = neu
                      )
                    } else {
                      Modifier.neumorphicExtruded(
                        shape = IosSharpBadgeShape,
                        elevation = 1.5.dp,
                        blurRadius = 3.dp,
                        borderAlpha = 0.22f,
                        neu = neu
                      )
                    }
                  )
                  .clickable {
                    selectedIntervalHours = hrs
                    TnHolidaySyncScheduler.setSyncIntervalHours(context, hrs)
                  }
                  .padding(horizontal = 7.dp, vertical = 4.dp)
                  .testTag("worker_interval_${hrs}h")
              ) {
                Text(
                  text = label,
                  fontFamily = FontFamily.Monospace,
                  fontSize = 9.5.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = if (isChosen) Color.White else neu.textSecondary,
                  maxLines = 1,
                  softWrap = false
                )
              }
            }
          }
        }
      }

      // 4-Pill Geometrically Uniform Telemetry Row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        SyncTelemetryPill(
          label = "Public GH",
          value = "${syncStatus.totalGhCount} Days",
          sub = "0 Credit Off",
          accent = StatusVerifiedGreen,
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
        )
        SyncTelemetryPill(
          label = "Restricted RL",
          value = "${syncStatus.totalRhCount} Days",
          sub = "TN RH List",
          accent = BadgePlGold,
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
        )
        SyncTelemetryPill(
          label = "Feed Source",
          value = syncStatus.lastSyncSource.take(12),
          sub = "Web / API",
          accent = BadgeColCyan,
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
        )
        SyncTelemetryPill(
          label = "Last Synced",
          value = syncStatus.lastSyncedFormatted.substringBefore(","),
          sub = syncStatus.lastSyncedFormatted.substringAfter(",", "Active").trim(),
          accent = BadgeClGreen,
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
        )
      }

      Text(
        text = syncStatus.statusSummary,
        style = MaterialTheme.typography.bodyMedium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = neu.textSecondary
      )

      if (syncStatus.newlySyncedItems.isNotEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .neumorphicInset(
              shape = IosSharpWellShape,
              depth = 2.5.dp,
              blurRadius = 5.dp,
              accentTint = EmeraldOptimal,
              accentTintAlpha = 0.08f,
              borderColor = EmeraldOptimal.copy(alpha = 0.30f),
              neu = neu
            )
        ) {
          Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = EmeraldOptimal,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Latest Verified Holidays Upserted to Room DB:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = EmeraldOptimal
              )
            }
            syncStatus.newlySyncedItems.forEach { line ->
              Text(
                text = "• $line",
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontFamily = FontFamily.Monospace,
                color = neu.textPrimary
              )
            }
          }
        }
      }

      // Action Buttons: [Scrape / Add G.O.] and [Sync Latest Calendar Now]
      val actions = LocalFunctionalActionColors.current
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedButton(
          onClick = { showAddOrScrapeDialog = true },
          shape = IosSharpButtonShape,
          border = BorderStroke(1.2.dp, actions.createPrimary),
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .testTag("open_custom_holiday_sync_dialog_button")
        ) {
          Icon(
            imageVector = Icons.Default.AddCircleOutline,
            contentDescription = null,
            tint = actions.createPrimary,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Add / Scrape G.O.",
            color = actions.createPrimary,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
          )
        }

        Button(
          onClick = { onTriggerSyncNow("") },
          enabled = !syncStatus.isSyncing,
          shape = IosSharpButtonShape,
          colors = ButtonDefaults.buttonColors(
            containerColor = actions.editUtility,
            contentColor = Color.White
          ),
          modifier = Modifier
            .weight(1.25f)
            .fillMaxHeight()
            .testTag("trigger_holiday_sync_button")
        ) {
          if (syncStatus.isSyncing) {
            CircularProgressIndicator(
              color = Color.White,
              strokeWidth = 2.dp,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Syncing TN Calendar...",
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
          } else {
            Icon(
              imageVector = Icons.Default.Sync,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Sync TN Calendar Now",
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

  if (showAddOrScrapeDialog) {
    AddOrScrapeHolidayDialog(
      onDismiss = { showAddOrScrapeDialog = false },
      onScrapeUrlOrNote = { urlOrNote ->
        showAddOrScrapeDialog = false
        onTriggerSyncNow(urlOrNote)
      },
      onAddDirectHoliday = { item ->
        showAddOrScrapeDialog = false
        onAddLocalHolidayToRoom(item)
      }
    )
  }
}

@Composable
private fun SyncTelemetryPill(
  label: String,
  value: String,
  sub: String,
  accent: Color,
  modifier: Modifier = Modifier
) {
  val neu = LocalNeumorphicColors.current
  Box(
    modifier = modifier.neumorphicInset(
      shape = IosSharpButtonShape,
      depth = 2.5.dp,
      blurRadius = 5.dp,
      accentTint = accent,
      accentTintAlpha = 0.06f,
      borderColor = accent.copy(alpha = 0.25f),
      neu = neu
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 8.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = label,
        fontSize = 9.sp,
        lineHeight = 11.sp,
        color = neu.textSecondary,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = value,
        fontSize = 12.sp,
        lineHeight = 15.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        color = accent,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = sub,
        fontSize = 9.sp,
        lineHeight = 11.sp,
        color = neu.textSecondary,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun AddOrScrapeHolidayDialog(
  onDismiss: () -> Unit,
  onScrapeUrlOrNote: (String) -> Unit,
  onAddDirectHoliday: (TnHolidayItem) -> Unit
) {
  val neu = LocalNeumorphicColors.current
  var dateIso by remember { mutableStateOf("2026-03-30") }
  var nameEnglish by remember {
    mutableStateOf("Bannari Amman Festival (Erode District Local Holiday)")
  }
  var nameTamil by remember {
    mutableStateOf("பண்ணாரி அம்மன் குண்டம் திருவிழா (ஈரோடு உள்ளூர் விடுமுறை)")
  }
  var isRestricted by remember { mutableStateOf(false) }
  var customUrlOrGoNote by remember {
    mutableStateOf("https://www.tn.gov.in/holiday_list.php")
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = IosSharpCardShape,
    containerColor = neu.baseBg,
    title = {
      ExecutiveDialogHeader(
        icon = Icons.Default.Storage,
        accent = BadgeColCyan,
        title = "Update Local Holiday DB",
        subtitle = "TN GAZETTE • ERODE COLLECTORATE"
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Option 1: Scrape a TN Govt Portal URL or paste a G.O. Notification text for Gemini to extract:",
          style = MaterialTheme.typography.bodyMedium,
          fontSize = 12.sp,
          color = neu.textSecondary
        )
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .neumorphicInset(
              shape = IosSharpWellShape,
              depth = 3.dp,
              blurRadius = 6.dp,
              neu = neu
            )
        ) {
          OutlinedTextField(
            value = customUrlOrGoNote,
            onValueChange = { customUrlOrGoNote = it },
            label = { Text("TN Portal URL or G.O. Text") },
            singleLine = true,
            shape = IosSharpWellShape,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = neu.primaryAccent.copy(alpha = 0.55f),
              unfocusedBorderColor = Color.Transparent,
              focusedContainerColor = Color.Transparent,
              unfocusedContainerColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth()
          )
        }
        val dialogActions = LocalFunctionalActionColors.current
        Button(
          onClick = { onScrapeUrlOrNote(customUrlOrGoNote) },
          shape = IosSharpButtonShape,
          colors = ButtonDefaults.buttonColors(
            containerColor = dialogActions.editUtility,
            contentColor = Color.White
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Scrape & Sync via Background Service", fontWeight = FontWeight.Bold)
        }

        Text(
          text = "Option 2: Add a Notified Erode Collectorate / DME Holiday Directly to Room DB:",
          style = MaterialTheme.typography.bodyMedium,
          fontSize = 12.sp,
          color = neu.textSecondary
        )
        DatePickerOutlinedField(
          value = dateIso,
          onValueChange = { dateIso = it.trim() },
          label = "Holiday Date (YYYY-MM-DD)",
          modifier = Modifier.fillMaxWidth()
        )
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .neumorphicInset(
              shape = IosSharpWellShape,
              depth = 3.dp,
              blurRadius = 6.dp,
              neu = neu
            )
        ) {
          OutlinedTextField(
            value = nameEnglish,
            onValueChange = { nameEnglish = it },
            label = { Text("Holiday Name (English)") },
            singleLine = true,
            shape = IosSharpWellShape,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = neu.primaryAccent.copy(alpha = 0.55f),
              unfocusedBorderColor = Color.Transparent,
              focusedContainerColor = Color.Transparent,
              unfocusedContainerColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth()
          )
        }
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          listOf(
            false to "Public Holiday (GH)",
            true to "Restricted (RH / RL)"
          ).forEach { (restrictedFlag, labelText) ->
            val selected = isRestricted == restrictedFlag
            val chipAccent = if (restrictedFlag) BadgePlGold else EmeraldOptimal
            Box(
              modifier = Modifier
                .weight(1f)
                .then(
                  if (selected) {
                    Modifier.neumorphicInset(
                      shape = IosSharpBadgeShape,
                      depth = 2.dp,
                      blurRadius = 4.dp,
                      accentTint = chipAccent,
                      accentTintAlpha = 0.14f,
                      borderColor = chipAccent.copy(alpha = 0.55f),
                      neu = neu
                    )
                  } else {
                    Modifier.neumorphicExtruded(
                      shape = IosSharpBadgeShape,
                      elevation = 2.dp,
                      blurRadius = 4.dp,
                      borderAlpha = 0.20f,
                      neu = neu
                    )
                  }
                )
                .clickable { isRestricted = restrictedFlag },
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = labelText,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) chipAccent else neu.textSecondary,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
              )
            }
          }
        }
      }
    },
    confirmButton = {
      val saveActions = LocalFunctionalActionColors.current
      Button(
        onClick = {
          val parsedDate = runCatching { LocalDate.parse(dateIso) }.getOrNull()
          val dayOfWeek = parsedDate?.dayOfWeek?.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
            ?: "Monday"
          onAddDirectHoliday(
            TnHolidayItem(
              dateIso = dateIso,
              dayOfWeek = dayOfWeek,
              nameEnglish = nameEnglish.ifBlank { "Erode District Local Holiday" },
              nameTamil = nameTamil.ifBlank { nameEnglish },
              isRestrictedHoliday = isRestricted,
              monthNameTamil = ""
            )
          )
        },
        shape = IosSharpButtonShape,
        colors = ButtonDefaults.buttonColors(
          containerColor = saveActions.createPrimary,
          contentColor = Color.White
        )
      ) {
        Text("Save to Local Room DB", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
