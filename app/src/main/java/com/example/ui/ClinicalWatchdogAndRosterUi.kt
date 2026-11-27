package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DutyShift
import com.example.data.DutyShiftType
import com.example.domain.BurnDownRiskLevel
import com.example.domain.ColExpiryItem
import com.example.domain.WatchdogColUrgency
import com.example.domain.ExpiryWatchdogSummary
import com.example.domain.GhColCaptureCandidate
import com.example.domain.JoiningReportAndShareGenerator
import com.example.domain.PendingPaperworkAlert
import com.example.ui.theme.BadgeClGreen
import com.example.ui.theme.BadgeColCyan
import com.example.ui.theme.BadgeInLieuLavender
import com.example.ui.theme.BadgeOnDutyBlue
import com.example.ui.theme.BadgePlGold
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.IosSharpBadgeShape
import com.example.ui.theme.IosSharpButtonShape
import com.example.ui.theme.IosSharpCardShape
import com.example.ui.theme.IosSharpWellShape
import com.example.ui.theme.LocalFunctionalActionColors
import com.example.ui.theme.LocalNeumorphicColors
import com.example.ui.theme.StatusAlertRed
import com.example.ui.theme.StatusPendingOrange
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

enum class CalendarLayerFilter(val label: String) {
  ALL("All"),
  LEAVES("Leaves"),
  DUTY("Duty Roster")
}

@Composable
fun ExpiryWatchdogAndActionCard(
  watchdog: ExpiryWatchdogSummary,
  unclaimedGhCandidates: List<GhColCaptureCandidate>,
  onOptimizeExpiringCol: (ColExpiryItem) -> Unit,
  onOpenColAuditTab: () -> Unit,
  onOpenPaperworkAlert: (PendingPaperworkAlert) -> Unit,
  onClaimGhColCandidate: (GhColCaptureCandidate) -> Unit,
  onClearLapsedCol: (ColExpiryItem) -> Unit = {},
  onClearAllLapsedCols: (List<ColExpiryItem>) -> Unit = {},
  onRestoreArchivedLapsedCols: () -> Unit = {},
  isClearingLapsedCols: Boolean = false,
  compactColMode: Boolean = false,
  modifier: Modifier = Modifier
) {
  val neu = LocalNeumorphicColors.current
  val actions = LocalFunctionalActionColors.current
  val lapsedItems = remember(watchdog.expiringCols) {
    watchdog.expiringCols.filter { it.urgency == WatchdogColUrgency.EXPIRED }
  }
  val hasUrgentCol = watchdog.criticalColCount > 0 || watchdog.expiringSoonColCount > 0 || watchdog.expiredColCount > 0
  val headerAccent = when {
    watchdog.criticalColCount > 0 || watchdog.expiredColCount > 0 -> StatusAlertRed
    watchdog.expiringSoonColCount > 0 || watchdog.paperworkAlerts.isNotEmpty() -> StatusPendingOrange
    else -> neu.primaryAccent
  }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .neumorphicExtruded(
        shape = IosSharpCardShape,
        elevation = 5.dp,
        blurRadius = 10.dp,
        accentTint = headerAccent,
        accentTintAlpha = 0.04f,
        borderAlpha = 0.34f,
        neu = neu
      )
      .testTag("expiry_watchdog_card")
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .neumorphicInset(
                shape = IosSharpButtonShape,
                depth = 2.5.dp,
                blurRadius = 5.dp,
                accentTint = headerAccent,
                accentTintAlpha = 0.14f,
                neu = neu
              ),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = if (hasUrgentCol) Icons.Default.WarningAmber else Icons.Default.Schedule,
              contentDescription = null,
              tint = headerAccent,
              modifier = Modifier.size(18.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "EXPIRY WATCHDOG & CLINICAL ACTIONS",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = headerAccent,
              letterSpacing = 0.4.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = if (compactColMode) "6-Month COL Lapse Tracker" else "COL Expiry, Burn-Down & Paperwork",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = neu.textPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        val badgeCount = watchdog.totalUrgentAlerts + unclaimedGhCandidates.size
        if (lapsedItems.isNotEmpty()) {
          // Interactive 1-Tap Crimson Extruded Button to Clear All Lapsed GH Credits from Firestore
          Box(
            modifier = Modifier
              .neumorphicExtruded(
                shape = IosSharpBadgeShape,
                elevation = 3.dp,
                blurRadius = 6.dp,
                surfaceColor = actions.destructiveAlert,
                neu = neu
              )
              .clickable(enabled = !isClearingLapsedCols) { onClearAllLapsedCols(lapsedItems) }
              .padding(horizontal = 10.dp, vertical = 7.dp)
              .testTag("clear_all_lapsed_cols_button")
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              if (isClearingLapsedCols) {
                CircularProgressIndicator(
                  modifier = Modifier.size(12.dp),
                  strokeWidth = 2.dp,
                  color = Color.White
                )
              } else {
                Icon(
                  imageVector = Icons.Default.DeleteSweep,
                  contentDescription = "Clear all lapsed credits",
                  tint = Color.White,
                  modifier = Modifier.size(13.dp)
                )
              }
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = if (isClearingLapsedCols) "Clearing..." else "Clear ${lapsedItems.size} Lapsed",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1,
                softWrap = false
              )
            }
          }
        } else {
          val statusBadgeColor = if (badgeCount > 0) headerAccent else EmeraldOptimal
          Box(
            modifier = Modifier.neumorphicInset(
              shape = IosSharpBadgeShape,
              depth = 2.dp,
              blurRadius = 4.dp,
              accentTint = statusBadgeColor,
              accentTintAlpha = 0.12f,
              borderColor = statusBadgeColor.copy(alpha = 0.38f),
              neu = neu
            )
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              if (badgeCount == 0) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = null,
                  tint = statusBadgeColor,
                  modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
              }
              Text(
                text = if (badgeCount > 0) "$badgeCount ACTION${if (badgeCount > 1) "S" else ""}" else "ALL CLEAR",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = statusBadgeColor,
                maxLines = 1,
                softWrap = false
              )
            }
          }
        }
      }

      // 1. 1-Tap Unclaimed GH Duty → +1 COL Capture Banner (if any logged duty fell on a GH/Sunday)
      if (unclaimedGhCandidates.isNotEmpty() && !compactColMode) {
        val topCandidate = unclaimedGhCandidates.first()
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .neumorphicInset(
              shape = IosSharpWellShape,
              depth = 2.5.dp,
              blurRadius = 5.dp,
              accentTint = EmeraldOptimal,
              accentTintAlpha = 0.12f,
              borderColor = EmeraldOptimal.copy(alpha = 0.40f),
              neu = neu
            )
            .padding(10.dp)
            .testTag("overview_claim_gh_col_banner")
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "GH / SUNDAY DUTY DETECTED • +1 COL READY",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = EmeraldOptimal,
                maxLines = 1
              )
              Text(
                text = "${JoiningReportAndShareGenerator.formatDisplayDate(topCandidate.shift.dateIso)} — ${topCandidate.holidayLabel}",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = neu.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
              modifier = Modifier
                .neumorphicExtruded(
                  shape = IosSharpBadgeShape,
                  elevation = 2.5.dp,
                  blurRadius = 5.dp,
                  surfaceColor = actions.verifyApprove,
                  neu = neu
                )
                .clickable { onClaimGhColCandidate(topCandidate) }
                .padding(horizontal = 10.dp, vertical = 7.dp)
                .testTag("overview_claim_gh_col_button")
            ) {
              Text(
                text = "Claim +1 COL",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1,
                softWrap = false
              )
            }
          }
        }
      }

      // 2. COL 6-Month Expiry Watchdog Strip
      if (watchdog.expiringCols.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "UNCLAIMED GH-WORKED CREDITS (TN 6-MONTH VALIDITY)",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = neu.textSecondary
          )
          val visibleCols = if (compactColMode) watchdog.expiringCols else watchdog.expiringCols.take(3)
          visibleCols.forEach { item ->
            val accent = when (item.urgency) {
              WatchdogColUrgency.EXPIRED, WatchdogColUrgency.CRITICAL -> StatusAlertRed
              WatchdogColUrgency.EXPIRING_SOON -> StatusPendingOrange
              WatchdogColUrgency.HEALTHY -> EmeraldOptimal
            }
            val countdownText = when {
              item.daysRemaining < 0L -> "LAPSED ${-item.daysRemaining}d AGO"
              item.daysRemaining == 0L -> "EXPIRES TODAY"
              else -> "${item.daysRemaining}d LEFT"
            }

            Box(
              modifier = Modifier
                .fillMaxWidth()
                .neumorphicInset(
                  shape = IosSharpWellShape,
                  depth = 2.dp,
                  blurRadius = 4.dp,
                  accentTint = accent,
                  accentTintAlpha = 0.08f,
                  borderColor = accent.copy(alpha = 0.30f),
                  neu = neu
                )
                .padding(10.dp)
                .testTag("col_expiry_row_${item.ghDateIso}")
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = countdownText,
                      fontFamily = FontFamily.Monospace,
                      fontSize = 10.5.sp,
                      fontWeight = FontWeight.ExtraBold,
                      color = accent
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "• Exp ${item.expiryDateIso}",
                      fontFamily = FontFamily.Monospace,
                      fontSize = 10.sp,
                      color = neu.textSecondary
                    )
                  }
                  Text(
                    text = "GH Worked ${item.ghDateIso} • ${item.notes}",
                    fontSize = 11.5.sp,
                    color = neu.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
                Spacer(modifier = Modifier.width(8.dp))
                val isExpiredItem = item.urgency == WatchdogColUrgency.EXPIRED
                if (isExpiredItem) {
                  // Lapsed >6-month credits cannot be claimed under TN rules — provide 1-tap Crimson Clear button
                  Box(
                    modifier = Modifier
                      .neumorphicExtruded(
                        shape = IosSharpBadgeShape,
                        elevation = 2.5.dp,
                        blurRadius = 5.dp,
                        surfaceColor = actions.destructiveAlert,
                        neu = neu
                      )
                      .clickable(enabled = !isClearingLapsedCols) { onClearLapsedCol(item) }
                      .padding(horizontal = 10.dp, vertical = 7.dp)
                      .testTag("clear_lapsed_col_${item.ghDateIso}")
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear lapsed GH credit",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(
                        text = "Clear",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                      )
                    }
                  }
                } else {
                  Box(
                    modifier = Modifier
                      .neumorphicExtruded(
                        shape = IosSharpBadgeShape,
                        elevation = 2.5.dp,
                        blurRadius = 5.dp,
                        surfaceColor = actions.aiOptimize,
                        neu = neu
                      )
                      .clickable { onOptimizeExpiringCol(item) }
                      .padding(horizontal = 9.dp, vertical = 7.dp)
                      .testTag("optimize_expiring_col_${item.ghDateIso}")
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(
                        text = "Use FIFO",
                        fontSize = 10.sp,
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
          if (!compactColMode && watchdog.expiringCols.size > 3) {
            Text(
              text = "+ ${watchdog.expiringCols.size - 3} more unclaimed GH credits in COL & GH Audit →",
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              color = BadgeColCyan,
              modifier = Modifier
                .clickable { onOpenColAuditTab() }
                .padding(vertical = 2.dp)
            )
          }
        }
      } else {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .neumorphicInset(
              shape = IosSharpWellShape,
              depth = 2.dp,
              blurRadius = 4.dp,
              accentTint = EmeraldOptimal,
              accentTintAlpha = 0.06f,
              neu = neu
            )
            .padding(10.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Verified,
              contentDescription = null,
              tint = EmeraldOptimal,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Zero unclaimed COL credits at expiry risk — all GH Worked entries are accounted for.",
              fontSize = 11.5.sp,
              color = neu.textSecondary
            )
          }
        }
      }

      // Archived Lapsed GH Credits Footer Strip (1-Tap Restore so historical records are never lost)
      if (watchdog.archivedLapsedCount > 0) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .neumorphicInset(
              shape = IosSharpWellShape,
              depth = 1.8.dp,
              blurRadius = 4.dp,
              neu = neu
            )
            .padding(horizontal = 10.dp, vertical = 7.dp)
            .testTag("archived_lapsed_cols_footer")
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
              Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = neu.textSecondary,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "${watchdog.archivedLapsedCount} lapsed GH credit${if (watchdog.archivedLapsedCount > 1) "s" else ""} cleared (kept in Ledger)",
                fontSize = 10.5.sp,
                color = neu.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
              modifier = Modifier
                .neumorphicExtruded(
                  shape = IosSharpBadgeShape,
                  elevation = 2.dp,
                  blurRadius = 4.dp,
                  surfaceColor = actions.editUtility,
                  neu = neu
                )
                .clickable { onRestoreArchivedLapsedCols() }
                .padding(horizontal = 9.dp, vertical = 5.dp)
                .testTag("restore_archived_lapsed_cols_button")
            ) {
              Text(
                text = "Restore",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                softWrap = false
              )
            }
          }
        }
      }

      // 3. Year-End CL (12) & RL (3) Burn-Down Pacing Strip
      if (!compactColMode) {
        val bd = watchdog.burnDown
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          BurnDownMiniTile(
            title = "CL Burn-Down (${bd.year})",
            remainingText = "${formatQuotaNumber(bd.clRemaining)} / ${bd.clLimit.toInt()} left",
            paceSub = "${String.format(Locale.US, "%.1f", bd.recommendedClPerMonth)}/mo • ${bd.daysRemainingInYear}d to Dec 31",
            risk = bd.clRiskLevel,
            progressFraction = (bd.clUsed / bd.clLimit).toFloat().coerceIn(0f, 1f),
            modifier = Modifier.weight(1f)
          )
          BurnDownMiniTile(
            title = "RL / RH (${bd.year})",
            remainingText = "${formatQuotaNumber(bd.rlRemaining)} / ${bd.rlLimit.toInt()} left",
            paceSub = if (bd.rlRemaining > 0.0) "Lapses 31 Dec ${bd.year}" else "100% utilized!",
            risk = bd.rlRiskLevel,
            progressFraction = (bd.rlUsed / bd.rlLimit).toFloat().coerceIn(0f, 1f),
            modifier = Modifier.weight(1f)
          )
        }
      }

      // 4. Pending Paperwork Alerts (Joining Reports & HOD Submissions)
      if (!compactColMode && watchdog.paperworkAlerts.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "PAPERWORK & JOINING REPORT REMINDERS",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = neu.textSecondary
          )
          watchdog.paperworkAlerts.take(3).forEach { alert ->
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .neumorphicInset(
                  shape = IosSharpWellShape,
                  depth = 2.dp,
                  blurRadius = 4.dp,
                  accentTint = BadgeInLieuLavender,
                  accentTintAlpha = 0.07f,
                  borderColor = BadgeInLieuLavender.copy(alpha = 0.28f),
                  neu = neu
                )
                .clickable { onOpenPaperworkAlert(alert) }
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .testTag("paperwork_alert_${alert.recordId}")
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                  Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = BadgeInLieuLavender,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = alert.headline,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = neu.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                  )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "${alert.actionLabel} →",
                  fontSize = 10.5.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = BadgeInLieuLavender,
                  maxLines = 1,
                  softWrap = false
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun BurnDownMiniTile(
  title: String,
  remainingText: String,
  paceSub: String,
  risk: BurnDownRiskLevel,
  progressFraction: Float,
  modifier: Modifier = Modifier
) {
  val neu = LocalNeumorphicColors.current
  val accent = when (risk) {
    BurnDownRiskLevel.HIGH_LAPSE_RISK -> StatusAlertRed
    BurnDownRiskLevel.PACE_UP -> StatusPendingOrange
    BurnDownRiskLevel.ON_TRACK -> BadgeClGreen
  }
  Box(
    modifier = modifier.neumorphicInset(
      shape = IosSharpWellShape,
      depth = 2.dp,
      blurRadius = 4.dp,
      accentTint = accent,
      accentTintAlpha = 0.06f,
      borderColor = accent.copy(alpha = 0.25f),
      neu = neu
    )
  ) {
    Column(
      modifier = Modifier.padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = title,
          fontSize = 9.5.sp,
          fontWeight = FontWeight.Bold,
          color = neu.textSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f)
        )
        Text(
          text = when (risk) {
            BurnDownRiskLevel.HIGH_LAPSE_RISK -> "LAPSE RISK"
            BurnDownRiskLevel.PACE_UP -> "PACE UP"
            BurnDownRiskLevel.ON_TRACK -> "ON TRACK"
          },
          fontSize = 8.5.sp,
          fontWeight = FontWeight.ExtraBold,
          fontFamily = FontFamily.Monospace,
          color = accent
        )
      }
      Text(
        text = remainingText,
        fontFamily = FontFamily.Monospace,
        fontSize = 13.sp,
        fontWeight = FontWeight.ExtraBold,
        color = neu.textPrimary
      )
      // Recessed Neumorphic Progress Bar
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(6.dp)
          .clip(CircleShape)
          .background(neu.darkShadow.copy(alpha = 0.22f))
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth(progressFraction.coerceIn(0.04f, 1f))
            .height(6.dp)
            .clip(CircleShape)
            .background(accent)
        )
      }
      Text(
        text = paceSub,
        fontSize = 9.5.sp,
        color = neu.textSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

private fun formatQuotaNumber(value: Double): String {
  return if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.1f", value)
}

@Composable
fun CalendarLayerFilterStrip(
  selectedLayer: CalendarLayerFilter,
  dutyCountInMonth: Int,
  onSelectLayer: (CalendarLayerFilter) -> Unit,
  modifier: Modifier = Modifier
) {
  val neu = LocalNeumorphicColors.current
  Box(
    modifier = modifier
      .fillMaxWidth()
      .neumorphicInset(
        shape = IosSharpButtonShape,
        depth = 2.5.dp,
        blurRadius = 5.dp,
        neu = neu
      )
      .padding(4.dp)
      .testTag("calendar_layer_filter_strip")
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      CalendarLayerFilter.entries.forEach { layer ->
        val isSelected = selectedLayer == layer
        val label = if (layer == CalendarLayerFilter.DUTY && dutyCountInMonth > 0) {
          "${layer.label} ($dutyCountInMonth)"
        } else {
          layer.label
        }
        Box(
          modifier = Modifier
            .weight(1f)
            .then(
              if (isSelected) {
                Modifier.neumorphicExtruded(
                  shape = IosSharpBadgeShape,
                  elevation = 2.5.dp,
                  blurRadius = 5.dp,
                  surfaceColor = if (layer == CalendarLayerFilter.DUTY) BadgeOnDutyBlue else neu.primaryAccent,
                  neu = neu
                )
              } else {
                Modifier
              }
            )
            .clickable { onSelectLayer(layer) }
            .padding(vertical = 8.dp)
            .testTag("calendar_filter_${layer.name.lowercase()}")
          ,
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (isSelected) Color.White else neu.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }
  }
}

@Composable
fun CalendarDutyRosterPanel(
  selectedDateIso: String,
  shiftsForDate: List<DutyShift>,
  ghCaptureCandidateForDate: GhColCaptureCandidate?,
  onAddDutyShift: (dateIso: String, shiftType: DutyShiftType, note: String) -> Unit,
  onAddRecurringWeeklyShift: (year: Int, month: Int, dayOfWeek: DayOfWeek, shiftType: DutyShiftType, note: String) -> Unit,
  onDeleteDutyShift: (shiftId: String) -> Unit,
  onClaimGhCol: (GhColCaptureCandidate) -> Unit,
  modifier: Modifier = Modifier
) {
  val neu = LocalNeumorphicColors.current
  val actions = LocalFunctionalActionColors.current
  var selectedType by remember { mutableStateOf(DutyShiftType.CALL_24H) }
  var shiftNote by remember { mutableStateOf("") }
  var showAddComposer by remember { mutableStateOf(false) }

  val parsedDate = remember(selectedDateIso) {
    runCatching { LocalDate.parse(selectedDateIso) }.getOrNull()
  }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .neumorphicExtruded(
        shape = IosSharpCardShape,
        elevation = 4.dp,
        blurRadius = 8.dp,
        accentTint = BadgeOnDutyBlue,
        accentTintAlpha = 0.04f,
        borderAlpha = 0.30f,
        neu = neu
      )
      .testTag("calendar_duty_roster_panel")
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Icon(
            imageVector = Icons.Default.LocalHospital,
            contentDescription = null,
            tint = BadgeOnDutyBlue,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = "CLINICAL DUTY & ON-CALL ROSTER",
              fontSize = 9.5.sp,
              fontWeight = FontWeight.ExtraBold,
              color = BadgeOnDutyBlue
            )
            Text(
              text = JoiningReportAndShareGenerator.formatDisplayDate(selectedDateIso),
              fontSize = 12.5.sp,
              fontWeight = FontWeight.Bold,
              color = neu.textPrimary
            )
          }
        }

        Box(
          modifier = Modifier
            .neumorphicExtruded(
              shape = IosSharpBadgeShape,
              elevation = 2.5.dp,
              blurRadius = 5.dp,
              surfaceColor = if (showAddComposer) neu.textSecondary else actions.createPrimary,
              neu = neu
            )
            .clickable { showAddComposer = !showAddComposer }
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("toggle_duty_composer_button")
        ) {
          Text(
            text = if (showAddComposer) "Close" else "+ Tag Shift",
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1
          )
        }
      }

      // Glowing 1-Tap GH-Worked → +1 COL Capture Banner
      if (ghCaptureCandidateForDate != null) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .neumorphicInset(
              shape = IosSharpWellShape,
              depth = 2.5.dp,
              blurRadius = 5.dp,
              accentTint = EmeraldOptimal,
              accentTintAlpha = 0.14f,
              borderColor = EmeraldOptimal.copy(alpha = 0.45f),
              neu = neu
            )
            .padding(10.dp)
            .testTag("calendar_gh_col_capture_banner")
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "WORKED THIS HOLIDAY? CLAIM +1 COL",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = EmeraldOptimal
              )
              Text(
                text = ghCaptureCandidateForDate.holidayLabel,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = neu.textPrimary,
                maxLines = 2
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = { onClaimGhCol(ghCaptureCandidateForDate) },
              shape = IosSharpButtonShape,
              colors = ButtonDefaults.buttonColors(
                containerColor = actions.verifyApprove,
                contentColor = Color.White
              ),
              modifier = Modifier.testTag("calendar_claim_gh_col_button")
            ) {
              Icon(
                imageVector = Icons.Default.AddAlert,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text("Claim +1 COL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      // Existing shifts logged on this date
      if (shiftsForDate.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          shiftsForDate.forEach { shift ->
            val shiftColor = when (shift.shiftTypeEnum) {
              DutyShiftType.CALL_24H, DutyShiftType.GH_EMERGENCY -> StatusAlertRed
              DutyShiftType.NIGHT_DUTY, DutyShiftType.EXAM_DME -> BadgePlGold
              DutyShiftType.OPD_DUTY -> BadgeOnDutyBlue
              DutyShiftType.WARD_OT -> BadgeColCyan
            }
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .neumorphicInset(
                  shape = IosSharpWellShape,
                  depth = 2.dp,
                  blurRadius = 4.dp,
                  accentTint = shiftColor,
                  accentTintAlpha = 0.08f,
                  borderColor = shiftColor.copy(alpha = 0.28f),
                  neu = neu
                )
                .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = shift.shiftTypeEnum.label,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold,
                      color = shiftColor
                    )
                    if (shift.colClaimed) {
                      Spacer(modifier = Modifier.width(6.dp))
                      Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "COL Claimed",
                        tint = EmeraldOptimal,
                        modifier = Modifier.size(13.dp)
                      )
                      Spacer(modifier = Modifier.width(2.dp))
                      Text(
                        text = "COL Banked",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldOptimal
                      )
                    }
                  }
                  if (shift.departmentNote.isNotBlank()) {
                    Text(
                      text = shift.departmentNote,
                      fontSize = 11.sp,
                      color = neu.textSecondary
                    )
                  }
                }
                IconButton(
                  onClick = { onDeleteDutyShift(shift.id) },
                  modifier = Modifier
                    .size(32.dp)
                    .testTag("delete_duty_shift_${shift.id}")
                ) {
                  Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove duty shift",
                    tint = neu.textSecondary,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
            }
          }
        }
      } else if (!showAddComposer) {
        Text(
          text = "No duty shifts tagged on this date. Tag 24h Call, OPD, Night, or Exam shifts to shield them in Leave Builder & AI Optimizer.",
          fontSize = 11.sp,
          color = neu.textSecondary
        )
      }

      // Quick-Tag Shift Composer
      AnimatedVisibility(visible = showAddComposer) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            DutyShiftType.entries.forEach { type ->
              val chosen = selectedType == type
              Box(
                modifier = Modifier
                  .then(
                    if (chosen) {
                      Modifier.neumorphicExtruded(
                        shape = IosSharpBadgeShape,
                        elevation = 2.dp,
                        blurRadius = 4.dp,
                        surfaceColor = BadgeOnDutyBlue,
                        neu = neu
                      )
                    } else {
                      Modifier.neumorphicInset(
                        shape = IosSharpBadgeShape,
                        depth = 1.5.dp,
                        blurRadius = 3.dp,
                        neu = neu
                      )
                    }
                  )
                  .clickable { selectedType = type }
                  .padding(horizontal = 10.dp, vertical = 7.dp)
                  .testTag("duty_type_chip_${type.code.lowercase()}")
              ) {
                Text(
                  text = type.shortLabel,
                  fontSize = 10.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (chosen) Color.White else neu.textSecondary,
                  maxLines = 1,
                  softWrap = false
                )
              }
            }
          }

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
            OutlinedTextField(
              value = shiftNote,
              onValueChange = { shiftNote = it.take(180) },
              placeholder = { Text("Optional ward / unit / exam note", fontSize = 12.sp) },
              singleLine = true,
              shape = IosSharpWellShape,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BadgeOnDutyBlue.copy(alpha = 0.55f),
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
              ),
              modifier = Modifier.fillMaxWidth()
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = {
                onAddDutyShift(selectedDateIso, selectedType, shiftNote)
                shiftNote = ""
                showAddComposer = false
              },
              shape = IosSharpButtonShape,
              colors = ButtonDefaults.buttonColors(
                containerColor = actions.createPrimary,
                contentColor = Color.White
              ),
              modifier = Modifier
                .weight(1f)
                .testTag("save_duty_shift_button")
            ) {
              Icon(Icons.Default.EventAvailable, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Save Shift", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }

            if (parsedDate != null) {
              val dayLabel = parsedDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
              Box(
                modifier = Modifier
                  .weight(1f)
                  .neumorphicExtruded(
                    shape = IosSharpButtonShape,
                    elevation = 2.5.dp,
                    blurRadius = 5.dp,
                    surfaceColor = actions.editUtility,
                    neu = neu
                  )
                  .clickable {
                    onAddRecurringWeeklyShift(
                      parsedDate.year,
                      parsedDate.monthValue,
                      parsedDate.dayOfWeek,
                      selectedType,
                      shiftNote
                    )
                    shiftNote = ""
                    showAddComposer = false
                  }
                  .padding(vertical = 11.dp, horizontal = 8.dp)
                  .testTag("repeat_weekly_duty_button"),
                contentAlignment = Alignment.Center
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.Repeat,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                  )
                  Spacer(modifier = Modifier.width(5.dp))
                  Text(
                    text = "Every $dayLabel (Mo)",
                    fontSize = 11.sp,
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
    }
  }
}
