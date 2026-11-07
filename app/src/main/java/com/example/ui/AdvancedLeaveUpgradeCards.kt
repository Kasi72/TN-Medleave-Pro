package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import com.example.data.LeaveCategory
import com.example.data.LeaveRecord
import com.example.domain.BurnDownSlotSuggestion
import com.example.domain.ColExpiryRadarEngine
import com.example.domain.ColExpiryUrgency
import com.example.domain.CsvImportRow
import com.example.domain.CsvSpreadsheetService
import com.example.domain.ElMasterPassbookEngine
import com.example.domain.LedgerEnrichmentStore
import com.example.domain.RecordAttachmentMeta
import com.example.domain.SrReconciliationState
import com.example.domain.UnusedGhExpiryItem
import com.example.domain.YearCreditSummary
import com.example.domain.YearEndBurnDownPlanner
import com.example.ui.theme.BadgeClGreen
import com.example.ui.theme.BadgeColCyan
import com.example.ui.theme.BadgeElNeutral
import com.example.ui.theme.BadgeMlOrange
import com.example.ui.theme.BadgeOnDutyBlue
import com.example.ui.theme.BadgePlGold
import com.example.ui.theme.BadgeRhSlate
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StatusAlertRed
import com.example.ui.theme.StatusPendingOrange
import com.example.ui.theme.StatusVerifiedGreen
import com.example.widget.MedLeaveQuotaAppWidget

// ============================================================================
// UPGRADE #2: 6-MONTH COMP-OFF (COL) EXPIRY COUNTDOWN & FIFO RADAR CARD
// ============================================================================

@Composable
fun ColSixMonthExpiryRadarCard(
  unusedGhRecords: List<LeaveRecord>,
  onClaimFifoGhAsCol: (ghRecord: LeaveRecord) -> Unit,
  modifier: Modifier = Modifier
) {
  val expiryItems = remember(unusedGhRecords) {
    ColExpiryRadarEngine.analyzeUnusedGhCredits(unusedGhRecords)
  }
  val criticalCount = expiryItems.count {
    it.urgency == ColExpiryUrgency.CRITICAL_WITHIN_30D || it.urgency == ColExpiryUrgency.WARNING_31_TO_60D
  }

  Surface(
    shape = RoundedCornerShape(26.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 3.dp,
    shadowElevation = 4.dp,
    modifier = modifier
      .fillMaxWidth()
      .testTag("col_six_month_expiry_radar_card")
      .border(
        width = 1.2.dp,
        brush = Brush.linearGradient(
          colors = listOf(
            BadgePlGold.copy(alpha = 0.50f),
            BadgeColCyan.copy(alpha = 0.25f)
          )
        ),
        shape = RoundedCornerShape(26.dp)
      )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            shape = RoundedCornerShape(13.dp),
            color = BadgePlGold.copy(alpha = 0.16f),
            border = BorderStroke(1.dp, BadgePlGold.copy(alpha = 0.40f)),
            modifier = Modifier.size(40.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.HourglassBottom,
                contentDescription = "6-Month COL Expiry Radar",
                tint = BadgePlGold,
                modifier = Modifier.size(20.dp)
              )
            }
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "6-Month COL Expiry & FIFO Radar",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "TN Rule: Comp-Off must be availed within 6 months of GH Worked duty",
              style = MaterialTheme.typography.bodySmall,
              fontSize = 11.sp,
              lineHeight = 14.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = if (criticalCount > 0) {
            StatusPendingOrange.copy(alpha = 0.16f)
          } else {
            EmeraldOptimal.copy(alpha = 0.14f)
          },
          border = BorderStroke(
            1.dp,
            if (criticalCount > 0) StatusPendingOrange.copy(alpha = 0.45f) else EmeraldOptimal.copy(alpha = 0.40f)
          )
        ) {
          Text(
            text = "${expiryItems.size} UNUSED GH",
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (criticalCount > 0) StatusPendingOrange else EmeraldOptimal,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
          )
        }
      }

      if (expiryItems.isEmpty()) {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = EmeraldOptimal.copy(alpha = 0.10f),
          border = BorderStroke(1.dp, EmeraldOptimal.copy(alpha = 0.30f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = null,
              tint = EmeraldOptimal,
              modifier = Modifier.size(18.dp)
            )
            Text(
              text = "All GH Worked credits have been paired & availed — zero risk of 6-month credit lapse!",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      } else {
        // FIFO Priority Recommendation Banner for Oldest Unused GH Credit
        val fifoOldest = expiryItems.first()
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = BadgeColCyan.copy(alpha = 0.10f),
          border = BorderStroke(1.dp, BadgeColCyan.copy(alpha = 0.35f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "FIFO PRIORITY #1 • CONSUME FIRST",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 9.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.Bold,
                color = BadgeColCyan,
                maxLines = 1,
                softWrap = false
              )
              Text(
                text = "GH Duty ${ClinicalDateFormatter.formatFriendlyDate(fifoOldest.dutyDateIso)} (${fifoOldest.holidayName})",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = "6M Expiry Window: ${ClinicalDateFormatter.formatFriendlyDate(fifoOldest.expiryDateIso)}",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false
              )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
              onClick = { onClaimFifoGhAsCol(fifoOldest.ghRecord) },
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = BadgeColCyan,
                contentColor = Color(0xFF042F2E)
              ),
              contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              modifier = Modifier
                .height(34.dp)
                .testTag("fifo_claim_oldest_gh_btn")
            ) {
              Text(
                text = "Use FIFO #1",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
              )
            }
          }
        }

        // Individual Unused GH Countdown Rows
        expiryItems.take(5).forEachIndexed { idx, item ->
          UnusedGhCountdownRow(
            rank = idx + 1,
            item = item,
            onAvailClick = { onClaimFifoGhAsCol(item.ghRecord) }
          )
        }
      }
    }
  }
}

@Composable
private fun UnusedGhCountdownRow(
  rank: Int,
  item: UnusedGhExpiryItem,
  onAvailClick: () -> Unit
) {
  val badgeAccent = when (item.urgency) {
    ColExpiryUrgency.CRITICAL_WITHIN_30D -> StatusAlertRed
    ColExpiryUrgency.WARNING_31_TO_60D -> StatusPendingOrange
    ColExpiryUrgency.ACTIVE_VALID -> EmeraldOptimal
    ColExpiryUrgency.PRIOR_YEAR_CARRY -> BadgePlGold
  }
  val friendlyDutyDate = ClinicalDateFormatter.formatFriendlyDate(item.dutyDateIso)
  val friendlyExpiryDate = ClinicalDateFormatter.formatFriendlyDate(item.expiryDateIso)
  val countdownLabel = when {
    item.ghRecord.year < 2026L -> "HISTORICAL (${item.ghRecord.year})"
    item.daysRemainingToExpiry < 0L -> "6M WINDOW PASSED ($friendlyExpiryDate)"
    item.daysRemainingToExpiry == 0L -> "EXPIRES TODAY ($friendlyExpiryDate)"
    else -> "${item.daysRemainingToExpiry}d LEFT • EXP $friendlyExpiryDate"
  }
  val statutoryProgress = remember(item.daysRemainingToExpiry, item.ghRecord.year) {
    if (item.ghRecord.year < 2026L) {
      1f
    } else {
      (item.daysRemainingToExpiry.toFloat() / 180f).coerceIn(0.06f, 1f)
    }
  }

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    border = BorderStroke(1.dp, badgeAccent.copy(alpha = 0.28f)),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .clickable { onAvailClick() }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 9.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = badgeAccent.copy(alpha = 0.16f)
          ) {
            Text(
              text = "#$rank",
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 10.sp,
              lineHeight = 12.sp,
              fontWeight = FontWeight.Bold,
              color = badgeAccent,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
            )
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "$friendlyDutyDate • ${item.holidayName}",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = countdownLabel,
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 10.sp,
              lineHeight = 12.sp,
              fontWeight = FontWeight.Bold,
              color = badgeAccent,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = badgeAccent.copy(alpha = 0.15f),
          border = BorderStroke(1.dp, badgeAccent.copy(alpha = 0.35f))
        ) {
          Text(
            text = "+ Avail COL",
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Bold,
            color = badgeAccent,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
          )
        }
      }

      // 180-Day Statutory Countdown Progress Bar
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(4.dp)
          .clip(RoundedCornerShape(2.dp))
          .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth(statutoryProgress)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(badgeAccent)
        )
      }
    }
  }
}

// ============================================================================
// UPGRADE #3: EARNED LEAVE (EL) MASTER PASSBOOK & HALF-YEARLY ACCRUAL TRACKER
// ============================================================================

@Composable
fun EarnedLeaveMasterPassbookCard(
  allRecords: List<LeaveRecord>,
  onShowStatusBanner: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var openingEl2023 by remember {
    mutableStateOf(LedgerEnrichmentStore.getOpeningElBalance2023(context))
  }
  var surrenderedDays by remember {
    mutableStateOf(LedgerEnrichmentStore.getElSurrenderedDays(context))
  }
  var showHalfYearlyTable by remember { mutableStateOf(false) }
  var showConfigDialog by remember { mutableStateOf(false) }

  val passbook = remember(allRecords, openingEl2023, surrenderedDays) {
    ElMasterPassbookEngine.computePassbook(
      allRecords = allRecords,
      openingBalanceJan2023 = openingEl2023,
      elSurrenderedDays = surrenderedDays
    )
  }

  val ceilingProgress =
    (passbook.currentNetElBalance.toFloat() / passbook.maxEncashmentCeiling.toFloat()).coerceIn(0f, 1f)

  Surface(
    shape = RoundedCornerShape(26.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 3.dp,
    shadowElevation = 4.dp,
    modifier = modifier
      .fillMaxWidth()
      .testTag("earned_leave_master_passbook_card")
      .border(
        width = 1.2.dp,
        brush = Brush.linearGradient(
          colors = listOf(
            BadgePlGold.copy(alpha = 0.45f),
            EmeraldOptimal.copy(alpha = 0.30f)
          )
        ),
        shape = RoundedCornerShape(26.dp)
      )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            shape = RoundedCornerShape(13.dp),
            color = BadgePlGold.copy(alpha = 0.16f),
            border = BorderStroke(1.dp, BadgePlGold.copy(alpha = 0.40f)),
            modifier = Modifier.size(40.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = "Earned Leave Master Passbook",
                tint = BadgePlGold,
                modifier = Modifier.size(20.dp)
              )
            }
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Earned Leave (EL) Master Passbook",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "+15 EL auto-credited every Jan 1 & Jul 1 under TN FR Rules",
              style = MaterialTheme.typography.bodySmall,
              fontSize = 11.sp,
              lineHeight = 14.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { showConfigDialog = true }
            .testTag("configure_el_passbook_btn")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Edit,
              contentDescription = "Edit Opening EL",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(12.dp)
            )
            Text(
              text = "CALIBRATE",
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 9.sp,
              lineHeight = 11.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              softWrap = false
            )
          }
        }
      }

      // 4-Pill Career EL & ML Telemetry Row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        PassbookStatPill(
          label = "NET EL BANK",
          value = "${passbook.currentNetElBalance}d",
          sub = "of 240d Max",
          accent = EmeraldOptimal,
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
        )
        PassbookStatPill(
          label = "ACCRUED (23-26)",
          value = "+${passbook.totalAccrued2023To2026}d",
          sub = "8 Half-Years",
          accent = BadgePlGold,
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
        )
        PassbookStatPill(
          label = "EL AVAILED",
          value = "-${passbook.totalElAvailed2023To2026}d",
          sub = "${passbook.elSurrenderedDays}d Surrend.",
          accent = BadgeColCyan,
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
        )
        PassbookStatPill(
          label = "CAREER ML",
          value = "${passbook.totalMlAvailedCareer}d",
          sub = "${passbook.mlCareerLimit - passbook.totalMlAvailedCareer}d Left",
          accent = BadgeMlOrange,
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
        )
      }

      // Retirement Encashment Progress Bar (240 / 300 Days Ceiling)
      Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "RETIREMENT ENCASHMENT ACCUMULATION (240d TARGET)",
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 9.sp,
            lineHeight = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false
          )
          Text(
            text = "${passbook.currentNetElBalance} / 240 DAYS (${(ceilingProgress * 100).toInt()}%)",
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Bold,
            color = EmeraldOptimal,
            maxLines = 1,
            softWrap = false
          )
        }
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(7.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth(ceilingProgress.coerceIn(0.05f, 1f))
              .height(7.dp)
              .clip(RoundedCornerShape(4.dp))
              .background(
                Brush.horizontalGradient(
                  colors = listOf(EmeraldOptimal, BadgePlGold)
                )
              )
          )
        }
      }

      // Expandable Half-Yearly Accrual Passbook Table
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .clickable { showHalfYearlyTable = !showHalfYearlyTable }
          .testTag("toggle_el_half_yearly_table")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 9.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = if (showHalfYearlyTable) {
              "Hide Half-Yearly Jan 1 / Jul 1 Accrual Passbook"
            } else {
              "View Half-Yearly Jan 1 / Jul 1 Accrual Passbook (2023–2026)"
            },
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
          )
          Icon(
            imageVector = if (showHalfYearlyTable) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
          )
        }
      }

      AnimatedVisibility(visible = showHalfYearlyTable) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          passbook.halfYearRows.forEach { row ->
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = row.periodLabel,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false
                  )
                  Text(
                    text = "Open ${row.openingBalance}d • +${row.statutoryAccrual}d Credit (${row.creditDateIso}) • -${row.elAvailedInHalfYear}d Used",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 9.sp,
                    lineHeight = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                  )
                }
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = EmeraldOptimal.copy(alpha = 0.14f)
                ) {
                  Text(
                    text = "${row.closingBalance}d EL",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldOptimal,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  if (showConfigDialog) {
    var openInput by remember { mutableStateOf(openingEl2023.toString()) }
    var surrInput by remember { mutableStateOf(surrenderedDays.toString()) }
    AlertDialog(
      onDismissRequest = { showConfigDialog = false },
      shape = RoundedCornerShape(24.dp),
      title = {
        Text(
          text = "Calibrate Service Register EL Passbook",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Align your Opening EL Balance (as on Jan 1, 2023 in your GEMC&H Service Register) and any Surrendered Leave (SLS) days:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          OutlinedTextField(
            value = openInput,
            onValueChange = { openInput = it.filter { ch -> ch.isDigit() } },
            label = { Text("Opening EL Balance on Jan 1, 2023 (Days)") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = surrInput,
            onValueChange = { surrInput = it.filter { ch -> ch.isDigit() } },
            label = { Text("EL Surrendered / Encashed (Days)") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val newOpen = openInput.toIntOrNull()?.coerceIn(0, 300) ?: openingEl2023
            val newSurr = surrInput.toIntOrNull()?.coerceIn(0, 120) ?: surrenderedDays
            openingEl2023 = newOpen
            surrenderedDays = newSurr
            LedgerEnrichmentStore.setOpeningElBalance2023(context, newOpen)
            LedgerEnrichmentStore.setElSurrenderedDays(context, newSurr)
            showConfigDialog = false
            onShowStatusBanner("Updated EL Passbook (Opening 2023: ${newOpen}d, Surrendered: ${newSurr}d)")
          },
          shape = RoundedCornerShape(12.dp)
        ) {
          Text("Save Passbook")
        }
      },
      dismissButton = {
        TextButton(onClick = { showConfigDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
private fun PassbookStatPill(
  label: String,
  value: String,
  sub: String,
  accent: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = accent.copy(alpha = 0.10f),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 8.dp, vertical = 9.dp),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = label,
        fontFamily = JetBrainsMonoFontFamily,
        fontSize = 8.sp,
        lineHeight = 10.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = value,
        fontFamily = JetBrainsMonoFontFamily,
        fontSize = 15.sp,
        lineHeight = 19.sp,
        fontWeight = FontWeight.ExtraBold,
        color = accent,
        maxLines = 1,
        softWrap = false
      )
      Text(
        text = sub,
        fontFamily = JetBrainsMonoFontFamily,
        fontSize = 9.sp,
        lineHeight = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

// ============================================================================
// UPGRADE #9: DECEMBER CL & RH LAPSE BURN-DOWN PLANNER ("ZERO-WASTE YEAR-END")
// ============================================================================

@Composable
fun DecemberLapseBurnDownCard(
  summary2026: YearCreditSummary,
  records2026: List<LeaveRecord>,
  onQuickLogSlot: (leaveType: String, dateIso: String, notes: String) -> Unit,
  onCommitAllBurnDownSlots: (List<BurnDownSlotSuggestion>) -> Unit,
  modifier: Modifier = Modifier
) {
  val plan = remember(summary2026, records2026) {
    YearEndBurnDownPlanner.generateBurnDownPlan(summary2026, records2026)
  }

  Surface(
    shape = RoundedCornerShape(26.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 3.dp,
    shadowElevation = 4.dp,
    modifier = modifier
      .fillMaxWidth()
      .testTag("december_lapse_burndown_card")
      .border(
        width = 1.2.dp,
        brush = Brush.linearGradient(
          colors = listOf(
            StatusPendingOrange.copy(alpha = 0.50f),
            EmeraldOptimal.copy(alpha = 0.35f)
          )
        ),
        shape = RoundedCornerShape(26.dp)
      )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            shape = RoundedCornerShape(13.dp),
            color = StatusPendingOrange.copy(alpha = 0.16f),
            border = BorderStroke(1.dp, StatusPendingOrange.copy(alpha = 0.40f)),
            modifier = Modifier.size(40.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = "December Lapse Burn-Down Planner",
                tint = StatusPendingOrange,
                modifier = Modifier.size(20.dp)
              )
            }
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Dec 31 CL & RH Lapse Burn-Down",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "Zero-Waste Year-End • ${plan.daysUntilYearEnd}d (${plan.remainingWorkingWeeks} wks) left until Dec 31 lapse",
              style = MaterialTheme.typography.bodySmall,
              fontSize = 11.sp,
              lineHeight = 14.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        if (plan.suggestedSlots.isNotEmpty()) {
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = { onCommitAllBurnDownSlots(plan.suggestedSlots) },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = EmeraldOptimal,
              contentColor = Color.White
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            modifier = Modifier
              .height(34.dp)
              .testTag("commit_all_burndown_btn")
          ) {
            Text(
              text = "Book All (${plan.suggestedSlots.size})",
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              softWrap = false
            )
          }
        }
      }

      Text(
        text = plan.complianceSummary,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      plan.suggestedSlots.take(5).forEach { slot ->
        val slotAccent = if (slot.suggestedCategory == LeaveCategory.RH) {
          Color(0xFF38BDF8)
        } else {
          BadgeClGreen
        }
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
          border = BorderStroke(1.dp, slotAccent.copy(alpha = 0.30f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = slotAccent.copy(alpha = 0.16f)
                ) {
                  Text(
                    text = "${slot.suggestedCategory.code} • ${slot.dayOfWeekLabel}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = slotAccent,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
                Text(
                  text = "${slot.dateIso} (${slot.consecutiveDaysOff}d Stretch)",
                  fontFamily = JetBrainsMonoFontFamily,
                  fontSize = 11.sp,
                  lineHeight = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  softWrap = false
                )
              }
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "${slot.title} — ${slot.strategicReason}",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
              )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
              shape = RoundedCornerShape(10.dp),
              color = slotAccent.copy(alpha = 0.16f),
              border = BorderStroke(1.dp, slotAccent.copy(alpha = 0.40f)),
              modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable {
                  onQuickLogSlot(
                    slot.suggestedCategory.code,
                    slot.dateIso,
                    "Year-End Burn-Down Plan: ${slot.title}"
                  )
                }
            ) {
              Text(
                text = "+ Book ${slot.suggestedCategory.code}",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Bold,
                color = slotAccent,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
              )
            }
          }
        }
      }
    }
  }
}

// ============================================================================
// UPGRADE #8: 2-WAY EXCEL / CSV SPREADSHEET EXPORT & IMPORT HUB CARD
// ============================================================================

@Composable
fun CsvSpreadsheetHubCard(
  allRecords: List<LeaveRecord>,
  metaMap: Map<String, RecordAttachmentMeta>,
  onImportCsvRows: (List<CsvImportRow>) -> Unit,
  onShowStatusBanner: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var showPasteCsvDialog by remember { mutableStateOf(false) }

  val csvFilePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    if (uri != null) {
      val res = CsvSpreadsheetService.parseCsvFromUri(context, uri)
      res.onSuccess { rows ->
        if (rows.isNotEmpty()) {
          onImportCsvRows(rows)
        } else {
          onShowStatusBanner("No valid YYYY-MM-DD rows found in selected CSV file.")
        }
      }.onFailure { err ->
        onShowStatusBanner("CSV Import failed: ${err.localizedMessage}")
      }
    }
  }

  Surface(
    shape = RoundedCornerShape(24.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 3.dp,
    shadowElevation = 3.dp,
    modifier = modifier
      .fillMaxWidth()
      .testTag("csv_spreadsheet_hub_card")
      .border(
        width = 1.dp,
        color = BadgeColCyan.copy(alpha = 0.35f),
        shape = RoundedCornerShape(24.dp)
      )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = BadgeColCyan.copy(alpha = 0.14f),
            border = BorderStroke(1.dp, BadgeColCyan.copy(alpha = 0.35f)),
            modifier = Modifier.size(38.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.TableView,
                contentDescription = "Excel CSV Import & Export",
                tint = BadgeColCyan,
                modifier = Modifier.size(20.dp)
              )
            }
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "2-Way Excel / CSV Spreadsheet Sync",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "${allRecords.size} total entries • Preserves FN/AN, Paired GH, SR Status & Order Ref",
              style = MaterialTheme.typography.bodySmall,
              fontSize = 11.sp,
              lineHeight = 14.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = {
            CsvSpreadsheetService.exportAndShareCsv(context, allRecords, metaMap)
              .onSuccess {
                onShowStatusBanner("Exported ${allRecords.size} records to Excel-compatible CSV!")
              }
              .onFailure { err ->
                onShowStatusBanner("CSV Export error: ${err.localizedMessage}")
              }
          },
          shape = RoundedCornerShape(13.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = EmeraldOptimal,
            contentColor = Color.White
          ),
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .testTag("export_csv_btn")
        ) {
          Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Export .CSV",
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
          )
        }

        OutlinedButton(
          onClick = { csvFilePickerLauncher.launch("text/*") },
          shape = RoundedCornerShape(13.dp),
          border = BorderStroke(1.dp, BadgeColCyan.copy(alpha = 0.50f)),
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .testTag("import_csv_file_btn")
        ) {
          Icon(
            Icons.Default.FileUpload,
            contentDescription = null,
            tint = BadgeColCyan,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Import File",
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = BadgeColCyan,
            maxLines = 1,
            softWrap = false
          )
        }

        OutlinedButton(
          onClick = { showPasteCsvDialog = true },
          shape = RoundedCornerShape(13.dp),
          border = BorderStroke(1.dp, BadgePlGold.copy(alpha = 0.50f)),
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .testTag("paste_csv_rows_btn")
        ) {
          Text(
            text = "Paste CSV",
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = BadgePlGold,
            maxLines = 1,
            softWrap = false
          )
        }
      }
    }
  }

  if (showPasteCsvDialog) {
    var csvTextInput by remember {
      mutableStateOf("2026-11-20,CL,FULL,,Friday Recharge CL\n2026-12-24,RH,FULL,,Christmas Eve RH Bridge")
    }
    AlertDialog(
      onDismissRequest = { showPasteCsvDialog = false },
      shape = RoundedCornerShape(24.dp),
      title = {
        Text(
          text = "Paste Spreadsheet / CSV Rows",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Paste rows in format: YYYY-MM-DD, Category (CL/RH/COL/GH_WORKED/EL/OD), Session (FULL/FN/AN), PairedDate, Notes",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          OutlinedTextField(
            value = csvTextInput,
            onValueChange = { csvTextInput = it },
            label = { Text("CSV Rows") },
            minLines = 4,
            maxLines = 8,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val rows = CsvSpreadsheetService.parseCsvText(csvTextInput)
            showPasteCsvDialog = false
            if (rows.isNotEmpty()) {
              onImportCsvRows(rows)
            } else {
              onShowStatusBanner("No valid YYYY-MM-DD rows found to import.")
            }
          },
          shape = RoundedCornerShape(12.dp)
        ) {
          Text("Import Rows")
        }
      },
      dismissButton = {
        TextButton(onClick = { showPasteCsvDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}

// ============================================================================
// UPGRADE #10: NATIVE ANDROID HOME SCREEN WIDGET PREVIEW & PIN CARD
// ============================================================================

@Composable
fun HomeScreenWidgetPreviewCard(
  summary2026: YearCreditSummary,
  unusedGhCount: Int,
  nextHolidayLabel: String,
  onShowStatusBanner: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val clDisplay = "${formatDaysCompact(summary2026.clRemaining)} / 12"
  val rhDisplay = "${summary2026.rhRemaining} / 3"
  val colDisplay = "$unusedGhCount READY"

  Surface(
    shape = RoundedCornerShape(26.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 3.dp,
    shadowElevation = 4.dp,
    modifier = modifier
      .fillMaxWidth()
      .testTag("home_screen_widget_preview_card")
      .border(
        width = 1.2.dp,
        color = EmeraldOptimal.copy(alpha = 0.35f),
        shape = RoundedCornerShape(26.dp)
      )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = EmeraldOptimal.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, EmeraldOptimal.copy(alpha = 0.38f)),
            modifier = Modifier.size(38.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.Widgets,
                contentDescription = "Android Home Screen Widget",
                tint = EmeraldOptimal,
                modifier = Modifier.size(20.dp)
              )
            }
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Android Home Screen Live Widget",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "4×2 Native Launcher Widget • Auto-synced with 2026 Firestore Ledger",
              style = MaterialTheme.typography.bodySmall,
              fontSize = 11.sp,
              lineHeight = 14.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Button(
          onClick = {
            MedLeaveQuotaAppWidget.syncAndRefreshWidget(
              context = context,
              clText = clDisplay,
              rhText = rhDisplay,
              colText = colDisplay,
              nextHolidayText = nextHolidayLabel,
              updatedLabel = "SYNCED"
            )
            val pinned = MedLeaveQuotaAppWidget.requestPinHomeWidget(context)
            onShowStatusBanner(
              if (pinned) {
                "Synced & requested Android Launcher to pin TN MedLeave Pro Widget!"
              } else {
                "Widget data synced! Long-press your Android Home Screen → Widgets → TN MedLeave Pro."
              }
            )
          },
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = EmeraldOptimal,
            contentColor = Color.White
          ),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier
            .height(34.dp)
            .testTag("pin_home_widget_btn")
        ) {
          Text(
            text = "Pin / Sync Widget",
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
          )
        }
      }

      // Live In-App Replica of the Native Android Home Screen Widget
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(20.dp))
          .background(
            Brush.linearGradient(
              colors = listOf(
                Color(0xFF061527),
                Color(0xFF0B2545),
                Color(0xFF052E2B)
              )
            )
          )
          .border(1.dp, Color(0xFF34D399).copy(alpha = 0.45f), RoundedCornerShape(20.dp))
          .padding(14.dp)
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "TN MEDLEAVE PRO • HOD PSYCHIATRY",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 9.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF34D399),
                maxLines = 1,
                softWrap = false
              )
              Text(
                text = "Govt. Erode Medical College • 2026 Live Quota",
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = Color(0xFFBAE6FD),
                maxLines = 1,
                softWrap = false
              )
            }
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color.White.copy(alpha = 0.12f)
            ) {
              Text(
                text = "HOME WIDGET",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 8.sp,
                lineHeight = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6EE7B7),
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
              )
            }
          }

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            WidgetMiniPreviewPill(
              label = "CL LEFT",
              value = clDisplay,
              accent = Color(0xFF4ADE80),
              modifier = Modifier.weight(1f).fillMaxHeight()
            )
            WidgetMiniPreviewPill(
              label = "RH LEFT",
              value = rhDisplay,
              accent = Color(0xFF38BDF8),
              modifier = Modifier.weight(1f).fillMaxHeight()
            )
            WidgetMiniPreviewPill(
              label = "FREE GH",
              value = colDisplay,
              accent = Color(0xFFFBBF24),
              modifier = Modifier.weight(1f).fillMaxHeight()
            )
          }

          Text(
            text = nextHolidayLabel,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 10.sp,
            lineHeight = 13.sp,
            color = Color(0xFFE2E8F0),
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
private fun WidgetMiniPreviewPill(
  label: String,
  value: String,
  accent: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = Color.White.copy(alpha = 0.09f),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      Text(
        text = label,
        fontFamily = JetBrainsMonoFontFamily,
        fontSize = 8.sp,
        lineHeight = 10.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFFCBD5E1),
        maxLines = 1,
        softWrap = false
      )
      Text(
        text = value,
        fontFamily = JetBrainsMonoFontFamily,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Bold,
        color = accent,
        maxLines = 1,
        softWrap = false
      )
    }
  }
}

// ============================================================================
// UPGRADE #5 & #6: ON-DUTY / EXAM CERTIFICATE VAULT & IFHRMS SR DIALOG
// ============================================================================

@Composable
fun CertificateVaultAndSrDialog(
  record: LeaveRecord,
  initialMeta: RecordAttachmentMeta,
  onDismiss: () -> Unit,
  onSaveMeta: (RecordAttachmentMeta) -> Unit
) {
  var selectedSrState by remember { mutableStateOf(initialMeta.srState) }
  var orderRefNo by remember { mutableStateOf(initialMeta.orderRefNo) }
  var issuingAuthority by remember {
    mutableStateOf(
      initialMeta.issuingAuthority.ifBlank {
        when (record.category) {
          LeaveCategory.ON_DUTY -> "TN Dr. M.G.R. Medical University / DME Chennai"
          LeaveCategory.GH_WORKED -> "Dean / RMO, Govt. Erode Medical College & Hospital"
          LeaveCategory.ML -> "Medical Board, GEMC&H"
          else -> "Dean's Office (E-Section), GEMC&H"
        }
      }
    )
  }
  var certificateUri by remember { mutableStateOf(initialMeta.certificatePhotoUri) }

  // Zero-permission Android Photo Picker (compliant with Google Play policy)
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      certificateUri = uri.toString()
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(26.dp),
    title = {
      Column {
        Text(
          text = "Certificate Vault & Office SR Verification",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "${record.dateIso} • ${record.category.fullTitle}",
          fontFamily = JetBrainsMonoFontFamily,
          fontSize = 11.sp,
          color = record.category.accentColor()
        )
      }
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        // 1. IFHRMS / Service Register (SR) Verification Status Selector
        Text(
          text = "1. IFHRMS / Dean's Office Service Register (SR) Status:",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          SrReconciliationState.entries.forEach { st ->
            val isSelected = selectedSrState == st
            val stAccent = when (st) {
              SrReconciliationState.SR_VERIFIED -> StatusVerifiedGreen
              SrReconciliationState.SUBMITTED_OFFICE -> BadgeColCyan
              SrReconciliationState.PENDING_SUBMISSION -> StatusPendingOrange
            }
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (isSelected) stAccent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
              border = BorderStroke(
                width = if (isSelected) 1.3.dp else 1.dp,
                color = if (isSelected) stAccent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
              ),
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { selectedSrState = st }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(stAccent)
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = st.shortBadge,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = stAccent
                  )
                  Text(
                    text = st.description,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }
        }

        // 2. University Exam / DME / Hospital Order Reference
        Text(
          text = "2. Attendance Certificate / Order Reference:",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold
        )
        OutlinedTextField(
          value = orderRefNo,
          onValueChange = { orderRefNo = it },
          label = { Text("Order / Certificate Ref. No. (e.g. Univ. Exam / C2 / 2026)") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = issuingAuthority,
          onValueChange = { issuingAuthority = it },
          label = { Text("Issuing Institution / Authority") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        )

        // 3. Zero-Permission Photo Picker for Certificate Scan
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedButton(
            onClick = {
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (certificateUri.isBlank()) "Attach Certificate Photo" else "Change Attached Photo",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        if (certificateUri.isNotBlank()) {
          val context = LocalContext.current
          val decodedBitmap = remember(certificateUri) {
            runCatching {
              context.contentResolver.openInputStream(Uri.parse(certificateUri))?.use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
              }
            }.getOrNull()
          }
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = EmeraldOptimal.copy(alpha = 0.10f),
            border = BorderStroke(1.dp, EmeraldOptimal.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "✓ CERTIFICATE SCAN ATTACHED",
                  fontFamily = JetBrainsMonoFontFamily,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = EmeraldOptimal
                )
                TextButton(onClick = { certificateUri = "" }) {
                  Text("Remove", fontSize = 11.sp, color = StatusAlertRed)
                }
              }
              if (decodedBitmap != null) {
                Image(
                  bitmap = decodedBitmap,
                  contentDescription = "Attached Attendance Certificate",
                  contentScale = ContentScale.Crop,
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(10.dp))
                )
              } else {
                Text(
                  text = "URI: $certificateUri",
                  fontFamily = JetBrainsMonoFontFamily,
                  fontSize = 9.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSaveMeta(
            initialMeta.copy(
              srState = selectedSrState,
              orderRefNo = orderRefNo.trim(),
              issuingAuthority = issuingAuthority.trim(),
              certificatePhotoUri = certificateUri
            )
          )
        },
        shape = RoundedCornerShape(12.dp)
      ) {
        Text("Save Vault & SR Status")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

private fun formatDaysCompact(value: Double): String {
  return if (value % 1.0 == 0.0) {
    value.toInt().toString()
  } else {
    String.format("%.1f", value)
  }
}
