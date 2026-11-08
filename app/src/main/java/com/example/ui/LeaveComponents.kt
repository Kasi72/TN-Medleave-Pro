package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HighlightStatus
import com.example.data.LeaveCategory
import com.example.data.LeaveRecord
import com.example.domain.ColExpiryRadarEngine
import com.example.domain.HalfDaySession
import com.example.domain.LedgerEnrichmentStore
import com.example.domain.RecordAttachmentMeta
import com.example.domain.SmartRangeSplicerEngine
import com.example.domain.SrReconciliationState
import com.example.ui.theme.BadgeClGreen
import com.example.ui.theme.BadgeColCyan
import com.example.ui.theme.BadgeElNeutral
import com.example.ui.theme.BadgeGhLime
import com.example.ui.theme.BadgeInLieuLavender
import com.example.ui.theme.BadgeMlOrange
import com.example.ui.theme.BadgeOnDutyBlue
import com.example.ui.theme.BadgePlGold
import com.example.ui.theme.BadgeRhSlate
import com.example.ui.theme.BadgeWeekOffTeal
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.IosSharpBadgeShape
import com.example.ui.theme.IosSharpButtonShape
import com.example.ui.theme.IosSharpCardShape
import com.example.ui.theme.IosSharpMicroShape
import com.example.ui.theme.IosSharpWellShape
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StatusAlertRed
import com.example.ui.theme.StatusPendingOrange
import com.example.ui.theme.StatusVerifiedGreen

fun LeaveCategory.accentColor(): Color = when (this) {
  LeaveCategory.CL -> BadgeClGreen
  LeaveCategory.COL -> BadgeColCyan
  LeaveCategory.IN_LIEU -> BadgeInLieuLavender
  LeaveCategory.PL -> BadgePlGold
  LeaveCategory.GH_WORKED -> BadgeGhLime
  LeaveCategory.ML -> BadgeMlOrange
  LeaveCategory.RH -> BadgeRhSlate
  LeaveCategory.EL -> BadgeElNeutral
  LeaveCategory.ON_DUTY -> BadgeOnDutyBlue
  LeaveCategory.WEEK_OFF -> BadgeWeekOffTeal
}

@Composable
fun IosSegmentedYearSelector(
  years: List<Long>,
  selectedYear: Long,
  onSelectYear: (Long) -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = IosSharpWellShape,
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.90f),
    tonalElevation = 2.dp,
    modifier = modifier
      .fillMaxWidth()
      .border(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
        shape = IosSharpWellShape
      )
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(3.dp),
      horizontalArrangement = Arrangement.spacedBy(3.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      years.forEachIndexed { idx, year ->
        val isSelected = year == selectedYear
        val bgColor by animateColorAsState(
          targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
          animationSpec = spring(),
          label = "seg_bg"
        )
        Box(
          modifier = Modifier
            .weight(1f)
            .height(38.dp)
            .clip(IosSharpButtonShape)
            .background(bgColor)
            .then(
              if (isSelected) {
                Modifier.border(
                  width = 1.1.dp,
                  color = Color.White.copy(alpha = 0.35f),
                  shape = IosSharpButtonShape
                )
              } else {
                Modifier
              }
            )
            .clickable { onSelectYear(year) }
            .testTag("year_segment_$year"),
          contentAlignment = Alignment.Center
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            if (isSelected) {
              Box(
                modifier = Modifier
                  .size(width = 5.dp, height = 5.dp)
                  .clip(IosSharpMicroShape)
                  .background(Color(0xFF34D399))
              )
            }
            Text(
              text = year.toString(),
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 13.sp,
              lineHeight = 16.sp,
              letterSpacing = (-0.2).sp,
              fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
              color = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
              maxLines = 1,
              softWrap = false
            )
          }
        }

        // Authentic Apple iOS UISegmentedControl Hairline Divider
        if (idx < years.lastIndex && !isSelected && years[idx + 1] != selectedYear) {
          Box(
            modifier = Modifier
              .width(0.8.dp)
              .height(16.dp)
              .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.38f))
          )
        }
      }
    }
  }
}

@Composable
fun SquircleCreditCard(
  title: String,
  code: String,
  mainStat: String,
  subStat: String,
  accent: Color,
  modifier: Modifier = Modifier,
  progressFraction: Float? = null,
  onClick: (() -> Unit)? = null
) {
  // Automatically infer progress fraction from "X / Y" strings if not explicitly passed
  val computedProgress: Float? = progressFraction ?: remember(mainStat) {
    val clean = mainStat.substringBefore(" ")
    val parts = if (mainStat.contains("/")) mainStat.split("/") else emptyList()
    if (parts.size == 2) {
      val num = parts[0].trim().filter { it.isDigit() || it == '.' }.toFloatOrNull()
      val den = parts[1].trim().takeWhile { it.isDigit() || it == '.' }.toFloatOrNull()
      if (num != null && den != null && den > 0f) (num / den).coerceIn(0f, 1f) else null
    } else if (clean.contains("/")) {
      null
    } else {
      null
    }
  }

  Surface(
    shape = IosSharpCardShape,
    color = Color.Transparent,
    tonalElevation = 0.dp,
    shadowElevation = 3.dp,
    modifier = modifier
      .iosFrostedGlassCard(
        accentColor = accent,
        shape = IosSharpCardShape,
        borderWidth = 1.15.dp,
        borderAlpha = 0.50f
      )
      .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          androidx.compose.ui.graphics.Brush.radialGradient(
            colors = listOf(
              accent.copy(alpha = 0.10f),
              Color.Transparent
            ),
            radius = 320f
          )
        )
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // Sharp Top iOS Accent Laser Strip
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(2.5.dp)
            .background(accent)
        )

        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
          verticalArrangement = Arrangement.SpaceBetween
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Surface(
                shape = IosSharpBadgeShape,
                color = accent.copy(alpha = 0.15f),
                modifier = Modifier.border(
                  width = 0.9.dp,
                  color = accent.copy(alpha = 0.38f),
                  shape = IosSharpBadgeShape
                )
              ) {
                Text(
                  text = code,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.ExtraBold,
                  color = accent,
                  lineHeight = 13.sp,
                  maxLines = 1,
                  softWrap = false,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp)
                )
              }
              if (computedProgress != null) {
                MiniCircularQuotaRing(
                  remainingFraction = (1f - computedProgress).coerceIn(0f, 1f),
                  accentColor = accent,
                  trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                  size = 26.dp,
                  strokeWidth = 3.dp
                )
              } else {
                Box(
                  modifier = Modifier
                    .size(20.dp)
                    .clip(IosSharpMicroShape)
                    .background(accent.copy(alpha = 0.14f)),
                  contentAlignment = Alignment.Center
                ) {
                  Box(
                    modifier = Modifier
                      .size(7.dp)
                      .clip(IosSharpMicroShape)
                      .background(accent)
                  )
                }
              }
            }

            Text(
              text = mainStat,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 19.sp,
              lineHeight = 23.sp,
              letterSpacing = (-0.3).sp,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                lineHeight = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = subStat,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.5.sp,
                lineHeight = 13.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
              )
            }
          }

          if (computedProgress != null) {
            Column(modifier = Modifier.padding(top = 7.dp)) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(4.dp)
                  .clip(IosSharpMicroShape)
                  .background(accent.copy(alpha = 0.16f))
              ) {
                Box(
                  modifier = Modifier
                    .fillMaxWidth(computedProgress.coerceIn(0.06f, 1f))
                    .height(4.dp)
                    .clip(IosSharpMicroShape)
                    .background(accent)
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
fun LeaveRecordSquircleRow(
  record: LeaveRecord,
  onDelete: (String) -> Unit,
  meta: RecordAttachmentMeta? = null,
  onToggleSrState: ((RecordAttachmentMeta) -> Unit)? = null,
  onOpenCertificateVault: ((LeaveRecord, RecordAttachmentMeta) -> Unit)? = null,
  onEditRecord: ((LeaveRecord) -> Unit)? = null
) {
  val context = LocalContext.current
  val cat = record.category
  val accent = cat.accentColor()
  val status = record.statusEnum
  val session = remember(record.notes, record.isHalfDay) {
    HalfDaySession.fromNotesOrFlag(record.notes, record.isHalfDay)
  }
  val resolvedMeta = meta ?: remember(record) {
    LedgerEnrichmentStore.getMetaForRecord(record, LedgerEnrichmentStore.readAllMeta(context))
  }
  val srAccent = when (resolvedMeta.srState) {
    SrReconciliationState.SR_VERIFIED -> StatusVerifiedGreen
    SrReconciliationState.SUBMITTED_OFFICE -> BadgeColCyan
    SrReconciliationState.PENDING_SUBMISSION -> StatusPendingOrange
  }

  val rowGlassAccent = when (status) {
    HighlightStatus.ALERT_RED -> StatusAlertRed
    HighlightStatus.VERIFIED_GREEN -> StatusVerifiedGreen
    HighlightStatus.PENDING_ORANGE -> StatusPendingOrange
    HighlightStatus.NORMAL -> accent
  }

  Surface(
    shape = IosSharpCardShape,
    color = Color.Transparent,
    tonalElevation = 0.dp,
    shadowElevation = 2.dp,
    modifier = Modifier
      .fillMaxWidth()
      .iosFrostedGlassCard(
        accentColor = rowGlassAccent,
        shape = IosSharpCardShape,
        borderWidth = if (status != HighlightStatus.NORMAL) 1.4.dp else 1.1.dp,
        borderAlpha = if (status != HighlightStatus.NORMAL) 0.62f else 0.36f
      )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(13.dp),
      verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
      ) {
        // Sharp-Edged iOS Squircle Category Badge (6.dp)
        Surface(
          shape = IosSharpButtonShape,
          color = accent.copy(alpha = 0.15f),
          border = BorderStroke(1.dp, accent.copy(alpha = 0.40f)),
          modifier = Modifier.size(46.dp)
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            Text(
              text = cat.shortLabel,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.ExtraBold,
              color = accent,
              fontSize = if (cat.shortLabel.length > 4) 10.sp else 13.sp,
              maxLines = 1,
              softWrap = false
            )
          }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
          val friendlyDate = remember(record.dateIso) {
            ClinicalDateFormatter.formatFriendlyDate(record.dateIso)
          }
          val dowTag = remember(record.dateIso) {
            ClinicalDateFormatter.dayOfWeekShort(record.dateIso)
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
          ) {
            Text(
              text = friendlyDate,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 15.sp,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              softWrap = false
            )
            if (dowTag != null) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
              ) {
                Text(
                  text = dowTag,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontSize = 9.sp,
                  lineHeight = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  softWrap = false,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
            if (record.isHalfDay || session != HalfDaySession.FULL_DAY) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = accent.copy(alpha = 0.2f)
              ) {
                Text(
                  text = session.badgeLabel,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontSize = 9.sp,
                  lineHeight = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = accent,
                  maxLines = 1,
                  softWrap = false,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
            if (status != HighlightStatus.NORMAL) {
              val (badgeColor, badgeIcon) = when (status) {
                HighlightStatus.ALERT_RED -> StatusAlertRed to Icons.Default.ErrorOutline
                HighlightStatus.VERIFIED_GREEN -> StatusVerifiedGreen to Icons.Default.CheckCircle
                HighlightStatus.PENDING_ORANGE -> StatusPendingOrange to Icons.Default.Schedule
                else -> accent to Icons.Default.CheckCircle
              }
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = badgeColor.copy(alpha = 0.14f)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = badgeIcon,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(12.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = status.label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = badgeColor,
                    maxLines = 1,
                    softWrap = false
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(3.dp))

          Text(
            text = if (record.notes.isNotBlank()) record.notes else cat.fullTitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )

          if (record.pairedDateIso.isNotBlank()) {
            val friendlyPaired = remember(record.pairedDateIso) {
              ClinicalDateFormatter.formatFriendlyWithDay(record.pairedDateIso)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Link,
                contentDescription = null,
                tint = BadgeColCyan,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Paired Duty / Leave: $friendlyPaired (${record.pairedDateIso})",
                style = MaterialTheme.typography.labelMedium,
                color = BadgeColCyan,
                fontSize = 11.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }

      // Clean Horizontal Action & SR Verification Footer (Replaces cramped 30dp vertical stack)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier
            .weight(1f)
            .horizontalScroll(rememberScrollState())
        ) {
          Surface(
            shape = RoundedCornerShape(9.dp),
            color = srAccent.copy(alpha = 0.13f),
            border = BorderStroke(1.dp, srAccent.copy(alpha = 0.35f)),
            modifier = Modifier
              .clip(RoundedCornerShape(9.dp))
              .clickable {
                if (onToggleSrState != null) {
                  onToggleSrState(resolvedMeta.copy(srState = resolvedMeta.srState.nextToggleState()))
                } else if (onOpenCertificateVault != null) {
                  onOpenCertificateVault(record, resolvedMeta)
                }
              }
              .testTag("sr_toggle_${record.id}")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = null,
                tint = srAccent,
                modifier = Modifier.size(12.dp)
              )
              Text(
                text = resolvedMeta.srState.shortBadge,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 9.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.Bold,
                color = srAccent,
                maxLines = 1,
                softWrap = false
              )
            }
          }

          if (resolvedMeta.orderRefNo.isNotBlank() || resolvedMeta.certificatePhotoUri.isNotBlank()) {
            Surface(
              shape = RoundedCornerShape(9.dp),
              color = BadgeOnDutyBlue.copy(alpha = 0.14f),
              border = BorderStroke(1.dp, BadgeOnDutyBlue.copy(alpha = 0.35f)),
              modifier = Modifier
                .clip(RoundedCornerShape(9.dp))
                .clickable {
                  onOpenCertificateVault?.invoke(record, resolvedMeta)
                }
            ) {
              Text(
                text = buildString {
                  if (resolvedMeta.orderRefNo.isNotBlank()) append("Ref: ${resolvedMeta.orderRefNo}")
                  if (resolvedMeta.certificatePhotoUri.isNotBlank()) {
                    if (isNotEmpty()) append(" • ")
                    append("Cert Photo")
                  }
                },
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 9.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.Bold,
                color = BadgeOnDutyBlue,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          if (onEditRecord != null) {
            Surface(
              shape = RoundedCornerShape(9.dp),
              color = BadgeColCyan.copy(alpha = 0.14f),
              border = BorderStroke(1.dp, BadgeColCyan.copy(alpha = 0.32f)),
              modifier = Modifier
                .clip(RoundedCornerShape(9.dp))
                .clickable { onEditRecord(record) }
                .testTag("edit_leave_${record.id}")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Edit,
                  contentDescription = "Edit Leave Entry In-Place",
                  tint = BadgeColCyan,
                  modifier = Modifier.size(13.dp)
                )
                Text(
                  text = "Edit",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = BadgeColCyan,
                  maxLines = 1,
                  softWrap = false
                )
              }
            }
          }

          if (onOpenCertificateVault != null) {
            val hasVaultData = resolvedMeta.certificatePhotoUri.isNotBlank() || resolvedMeta.orderRefNo.isNotBlank()
            val vaultTint = if (hasVaultData) EmeraldOptimal else MaterialTheme.colorScheme.onSurfaceVariant
            Surface(
              shape = RoundedCornerShape(9.dp),
              color = if (hasVaultData) EmeraldOptimal.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
              border = BorderStroke(1.dp, vaultTint.copy(alpha = 0.32f)),
              modifier = Modifier
                .clip(RoundedCornerShape(9.dp))
                .clickable { onOpenCertificateVault(record, resolvedMeta) }
                .testTag("vault_btn_${record.id}")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Description,
                  contentDescription = "Certificate Vault & SR Status",
                  tint = vaultTint,
                  modifier = Modifier.size(13.dp)
                )
                Text(
                  text = "Vault",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = vaultTint,
                  maxLines = 1,
                  softWrap = false
                )
              }
            }
          }

          Surface(
            shape = RoundedCornerShape(9.dp),
            color = StatusAlertRed.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, StatusAlertRed.copy(alpha = 0.30f)),
            modifier = Modifier
              .clip(RoundedCornerShape(9.dp))
              .clickable { onDelete(record.id) }
              .testTag("delete_leave_${record.id}")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = "Delete Leave Record",
                tint = StatusAlertRed,
                modifier = Modifier.size(14.dp)
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun AddLeaveRecordDialog(
  initialYear: Long,
  unusedGhRecords: List<LeaveRecord> = emptyList(),
  existingRecords: List<LeaveRecord> = emptyList(),
  onDismiss: () -> Unit,
  onConfirm: (leaveType: String, startDate: String, endDate: String, pairedDateIso: String, notes: String, status: String, isHalfDay: Boolean) -> Unit,
  onConfirmWorkingDaysOnly: ((leaveType: String, workingDateIsos: List<String>, pairedDateIso: String, notes: String, isHalfDay: Boolean) -> Unit)? = null
) {
  val context = LocalContext.current
  var selectedCat by remember { mutableStateOf(LeaveCategory.CL) }
  var startDate by remember { mutableStateOf("$initialYear-11-23") }
  var endDate by remember { mutableStateOf("$initialYear-11-23") }
  var pairedDateIso by remember { mutableStateOf("") }
  var notes by remember { mutableStateOf("") }
  var selectedStatus by remember { mutableStateOf(com.example.data.LeaveStatus.APPROVED) }
  var selectedSession by remember { mutableStateOf(HalfDaySession.FULL_DAY) }
  var orderRefNo by remember { mutableStateOf("") }
  var certificatePhotoUri by remember { mutableStateOf("") }
  var autoSkipWeekendsAndGh by remember { mutableStateOf(true) }

  val isHalfDay = selectedSession != HalfDaySession.FULL_DAY
  val isMultiDayRange = startDate != endDate
  val rangeSplicePreview = remember(startDate, endDate) {
    if (startDate != endDate) {
      SmartRangeSplicerEngine.analyzeRangeForAutoSkip(startDate, endDate)
    } else {
      null
    }
  }

  val fifoUnusedGhList = remember(unusedGhRecords) {
    ColExpiryRadarEngine.analyzeUnusedGhCredits(unusedGhRecords)
  }

  // Hyper-Intelligent Live Duplicate & Collision Radar (in scope for both Dialog Body & Confirm Button)
  val duplicateGuardEval = remember(
    selectedCat,
    startDate,
    endDate,
    pairedDateIso,
    notes,
    selectedSession,
    autoSkipWeekendsAndGh,
    existingRecords
  ) {
    val candidateDates = if (autoSkipWeekendsAndGh && isMultiDayRange && rangeSplicePreview != null) {
      rangeSplicePreview.workingDaysToDebit.map { it.dateIso }
    } else {
      runCatching {
        val s = java.time.LocalDate.parse(startDate)
        val e = java.time.LocalDate.parse(endDate)
        buildList {
          var curr = s
          var safety = 0
          while (!curr.isAfter(e) && safety < 31) {
            add(curr.toString())
            curr = curr.plusDays(1)
            safety++
          }
        }
      }.getOrElse { listOf(startDate) }
    }
    val sessionTagPrefix = when (selectedSession) {
      HalfDaySession.FORENOON_FN -> "[FN] "
      HalfDaySession.AFTERNOON_AN -> "[AN] "
      HalfDaySession.FULL_DAY -> ""
    }
    com.example.domain.SmartDuplicateLeaveGuardEngine.evaluateProposedEntry(
      proposedCategory = selectedCat,
      candidateDatesIso = candidateDates,
      pairedDutyDateIso = pairedDateIso,
      proposedNotes = "$sessionTagPrefix$notes",
      isHalfDay = isHalfDay,
      existingRecords = existingRecords
    )
  }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      certificatePhotoUri = uri.toString()
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(28.dp),
    title = {
      ExecutiveDialogHeader(
        icon = Icons.Default.Add,
        accent = selectedCat.accentColor(),
        title = "Log Leave / Hospital Duty",
        subtitle = "SINGLE OR RANGE • FN / AN • 7-LAYER GUARD"
      )
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.verticalScroll(rememberScrollState())
      ) {
        Text(
          text = "Select TN Leave / Duty Category:",
          style = MaterialTheme.typography.labelLarge
        )
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          LeaveCategory.entries.forEach { cat ->
            val selected = cat == selectedCat
            val accent = cat.accentColor()
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = if (selected) accent else accent.copy(alpha = 0.12f),
              modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .clickable {
                  selectedCat = cat
                  if (cat != LeaveCategory.CL) {
                    selectedSession = HalfDaySession.FULL_DAY
                  }
                }
            ) {
              Text(
                text = cat.shortLabel,
                color = if (selected) Color.White else accent,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
              )
            }
          }
        }

        Text(
          text = selectedCat.tnRuleSummary,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 11.sp
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          DatePickerOutlinedField(
            value = startDate,
            onValueChange = {
              startDate = it.trim()
              if (endDate < startDate) endDate = startDate
            },
            label = "Start Date",
            modifier = Modifier
              .weight(1f)
              .testTag("add_leave_date_input")
          )
          DatePickerOutlinedField(
            value = endDate,
            onValueChange = { endDate = it.trim() },
            label = "End Date",
            modifier = Modifier
              .weight(1f)
              .testTag("add_leave_end_date_input")
          )
        }

        // Upgrade #8: "Auto-Skip Weekends & Public Holidays (GH)" Smart Range Splicer
        if (isMultiDayRange && rangeSplicePreview != null) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = EmeraldOptimal.copy(alpha = 0.10f),
            border = BorderStroke(1.dp, EmeraldOptimal.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(10.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { autoSkipWeekendsAndGh = !autoSkipWeekendsAndGh }
              ) {
                Checkbox(
                  checked = autoSkipWeekendsAndGh,
                  onCheckedChange = { autoSkipWeekendsAndGh = it }
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Auto-Skip Weekends & Public Holidays (GH)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldOptimal
                  )
                  Text(
                    text = "Debits ${rangeSplicePreview.workingDaysToDebit.size} working day(s) • Skips ${rangeSplicePreview.skippedWeekendOrGhDays.size} Sat/Sun/GH day(s)",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }
        }

        // Upgrade #4: Explicit Full-Day (1.0) | Forenoon FN (0.5) | Afternoon AN (0.5) Session Selector
        if (selectedCat == LeaveCategory.CL) {
          Text(
            text = "Casual Leave Session (Full Day vs Forenoon FN / Afternoon AN):",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            HalfDaySession.entries.forEach { sess ->
              val isSel = selectedSession == sess
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSel) BadgeClGreen.copy(alpha = 0.20f) else MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(
                  width = if (isSel) 1.2.dp else 1.dp,
                  color = if (isSel) BadgeClGreen else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(10.dp))
                  .clickable { selectedSession = sess }
                  .testTag("session_chip_${sess.code}")
              ) {
                Box(
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 7.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = sess.badgeLabel,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSel) BadgeClGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                  )
                }
              }
            }
          }
        }

        // Upgrade #2: FIFO Auto-Select Oldest Unused GH Credit when logging COL
        if (selectedCat in listOf(LeaveCategory.COL, LeaveCategory.IN_LIEU, LeaveCategory.PL, LeaveCategory.GH_WORKED)) {
          if (selectedCat == LeaveCategory.COL && fifoUnusedGhList.isNotEmpty()) {
            val oldest = fifoUnusedGhList.first()
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = BadgeColCyan.copy(alpha = 0.12f),
              border = BorderStroke(1.dp, BadgeColCyan.copy(alpha = 0.40f)),
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                  pairedDateIso = oldest.dutyDateIso
                  if (notes.isBlank()) {
                    notes = "FIFO Auto-Paired with GH Worked on ${oldest.dutyDateIso} (${oldest.holidayName})"
                  }
                }
                .testTag("fifo_autofill_gh_chip")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "★ FIFO #1 OLDEST UNUSED GH CREDIT: ${ClinicalDateFormatter.formatFriendlyDate(oldest.dutyDateIso)}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = BadgeColCyan
                  )
                  Text(
                    text = "Tap to auto-fill (${oldest.holidayName} • Exp ${ClinicalDateFormatter.formatFriendlyDate(oldest.expiryDateIso)})",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
                Icon(
                  imageVector = Icons.Default.AutoFixHigh,
                  contentDescription = null,
                  tint = BadgeColCyan,
                  modifier = Modifier.size(16.dp)
                )
              }
            }
          }

          DatePickerOutlinedField(
            value = pairedDateIso,
            onValueChange = { pairedDateIso = it.trim() },
            label = "Paired GH Worked / Duty Date (YYYY-MM-DD)",
            modifier = Modifier.fillMaxWidth()
          )
        }

        // Upgrade #5: Inline Order Ref & Certificate Scan Picker for On-Duty / GH Worked / ML
        if (selectedCat in listOf(LeaveCategory.ON_DUTY, LeaveCategory.GH_WORKED, LeaveCategory.ML)) {
          OutlinedTextField(
            value = orderRefNo,
            onValueChange = { orderRefNo = it },
            label = { Text("University / DME / Medical Board Order Ref. No.") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedButton(
            onClick = {
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (certificatePhotoUri.isBlank()) {
                "Attach Attendance Certificate / Order Photo"
              } else {
                "✓ Certificate Photo Attached (Tap to Change)"
              },
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Reason / Remarks (e.g. DME Inspection, Pongal)") },
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("add_leave_notes_input")
        )

        // Live TamilNaduLeaveRulesService Validation Preview
        val liveValidation = remember(selectedCat, startDate, endDate, pairedDateIso, isHalfDay) {
          com.example.domain.TamilNaduLeaveRulesService.validateAndOptimizeLeaveSpell(
            category = selectedCat,
            startDateIso = startDate,
            endDateIso = endDate,
            pairedDutyDateIso = pairedDateIso,
            isHalfDay = isHalfDay
          )
        }

        val dupAccentColor = when (duplicateGuardEval.verdict) {
          com.example.domain.DuplicateCheckVerdict.CLEAR_TO_LOG -> StatusVerifiedGreen
          com.example.domain.DuplicateCheckVerdict.PARTIAL_OVERLAP_AUTO_SPLICED -> BadgePlGold
          com.example.domain.DuplicateCheckVerdict.BLOCKED_DUPLICATE_OR_CONFLICT -> StatusAlertRed
        }

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = dupAccentColor.copy(alpha = 0.13f),
          border = BorderStroke(1.dp, dupAccentColor.copy(alpha = 0.45f)),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("smart_duplicate_guard_banner")
        ) {
          Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Text(
              text = duplicateGuardEval.headlineTitle,
              fontSize = 11.sp,
              fontWeight = FontWeight.ExtraBold,
              color = dupAccentColor
            )
            Text(
              text = duplicateGuardEval.detailedExplanation,
              fontSize = 10.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val suggestedGh = duplicateGuardEval.suggestedReplacementGhDutyIso
            if (suggestedGh != null) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = BadgeColCyan.copy(alpha = 0.18f),
                border = BorderStroke(1.dp, BadgeColCyan.copy(alpha = 0.55f)),
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .clickable {
                    pairedDateIso = suggestedGh
                    notes = "FIFO Auto-Switched to Unused GH Worked on $suggestedGh"
                  }
                  .testTag("switch_to_unused_gh_btn")
              ) {
                Text(
                  text = "⚡ 1-Tap Switch to Unused FIFO GH Duty: $suggestedGh",
                  fontFamily = JetBrainsMonoFontFamily,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = BadgeColCyan,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
              }
            }
          }
        }

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = if (liveValidation.isCompliant) {
            StatusVerifiedGreen.copy(alpha = 0.12f)
          } else {
            StatusAlertRed.copy(alpha = 0.12f)
          },
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text(
              text = if (liveValidation.isCompliant) {
                "✓ TN Rule Check: Debits ${liveValidation.netLeaveDebitedDays}d (${liveValidation.totalCalendarDays}d total stretch)"
              } else {
                liveValidation.violations.firstOrNull() ?: "Rule Alert"
              },
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = if (liveValidation.isCompliant) StatusVerifiedGreen else StatusAlertRed
            )
            if (liveValidation.optimizationAdvice.isNotBlank()) {
              Text(
                text = liveValidation.optimizationAdvice,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        Text(
          text = "Leave Status:",
          style = MaterialTheme.typography.labelLarge
        )
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          com.example.data.LeaveStatus.entries.forEach { st ->
            val sel = selectedStatus == st
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { selectedStatus = st }
            ) {
              Text(
                text = st.displayLabel,
                fontSize = 11.sp,
                color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        enabled = duplicateGuardEval.canProceed,
        onClick = {
          val isoRegex = Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")
          if (startDate.matches(isoRegex) && endDate.matches(isoRegex) && duplicateGuardEval.canProceed) {
            val sessionTag = when (selectedSession) {
              HalfDaySession.FORENOON_FN -> "[FN] "
              HalfDaySession.AFTERNOON_AN -> "[AN] "
              HalfDaySession.FULL_DAY -> ""
            }
            val refTag = if (orderRefNo.isNotBlank()) " [Ref: ${orderRefNo.trim()}]" else ""
            val finalNotes = "$sessionTag${notes.ifBlank { selectedCat.fullTitle }}$refTag".trim()

            if (orderRefNo.isNotBlank() || certificatePhotoUri.isNotBlank()) {
              LedgerEnrichmentStore.saveMetaForRecord(
                context = context,
                meta = RecordAttachmentMeta(
                  recordKey = "${startDate}_${selectedCat.code}",
                  srState = SrReconciliationState.SUBMITTED_OFFICE,
                  orderRefNo = orderRefNo.trim(),
                  issuingAuthority = "TN MES / GEMC&H",
                  certificatePhotoUri = certificatePhotoUri
                )
              )
            }

            if ((autoSkipWeekendsAndGh && isMultiDayRange && rangeSplicePreview != null && onConfirmWorkingDaysOnly != null) ||
              (duplicateGuardEval.verdict == com.example.domain.DuplicateCheckVerdict.PARTIAL_OVERLAP_AUTO_SPLICED && onConfirmWorkingDaysOnly != null)
            ) {
              onConfirmWorkingDaysOnly(
                selectedCat.code,
                duplicateGuardEval.allowedDatesToLog,
                pairedDateIso,
                finalNotes,
                isHalfDay
              )
            } else {
              onConfirm(
                selectedCat.code,
                startDate,
                endDate,
                pairedDateIso,
                finalNotes,
                selectedStatus.code,
                isHalfDay
              )
            }
          }
        },
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.testTag("confirm_add_leave_button")
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = when (duplicateGuardEval.verdict) {
            com.example.domain.DuplicateCheckVerdict.BLOCKED_DUPLICATE_OR_CONFLICT ->
              "Duplicate Blocked"
            com.example.domain.DuplicateCheckVerdict.PARTIAL_OVERLAP_AUTO_SPLICED ->
              "Save ${duplicateGuardEval.allowedDatesToLog.size} Non-Duplicate Day(s)"
            com.example.domain.DuplicateCheckVerdict.CLEAR_TO_LOG ->
              "Save LeaveEntry"
          },
          maxLines = 1,
          softWrap = false
        )
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
