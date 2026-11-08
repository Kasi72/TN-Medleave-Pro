package com.example.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WarningAmber
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ColGhAuditSummary
import com.example.data.LeaveCategory
import com.example.data.LeaveRecord
import com.example.domain.AcademicOdSubQuotaEngine
import com.example.domain.CalendarIcsSyncService
import com.example.domain.ExecutiveDigestShareService
import com.example.domain.HalfDaySession
import com.example.domain.LtcBlockState
import com.example.domain.LtcBlockTrackerStore
import com.example.domain.OdSubCategory
import com.example.domain.SmartRangeSplicerEngine
import com.example.domain.WhatIfLeaveSimulatorEngine
import com.example.domain.YearCreditSummary
import com.example.ui.theme.BadgeClGreen
import com.example.ui.theme.BadgeColCyan
import com.example.ui.theme.BadgeGhLime
import com.example.ui.theme.BadgeMlOrange
import com.example.ui.theme.BadgeOnDutyBlue
import com.example.ui.theme.BadgePlGold
import com.example.ui.theme.BadgeRhSlate
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.IosDarkDockSurface
import com.example.ui.theme.IosSharpBadgeShape
import com.example.ui.theme.IosSharpButtonShape
import com.example.ui.theme.IosSharpMicroShape
import com.example.ui.theme.IosSharpWellShape
import com.example.ui.theme.IosSquircleGlassFill
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppThemeMode
import com.example.ui.theme.NeonAquaAccent
import com.example.ui.theme.NeonCyanBright
import com.example.ui.theme.NeonCyanText
import com.example.ui.theme.StatusAlertRed
import com.example.ui.theme.StatusPendingOrange
import com.example.ui.theme.StatusVerifiedGreen

// ============================================================================
// APPLE iOS 18 PRO SHARP-EDGED SEGMENTED SUB-TAB SWITCHER
// ============================================================================

data class SubTabOption(
  val index: Int,
  val title: String,
  val badge: String? = null,
  val accentColor: Color = EmeraldOptimal
)

@Composable
fun EnterpriseSubTabSwitcher(
  tabs: List<SubTabOption>,
  selectedIndex: Int,
  onSelectTab: (Int) -> Unit,
  modifier: Modifier = Modifier
) {
  val themeMode = LocalAppThemeMode.current
  val isWhiteTheme = themeMode == AppThemeMode.WHITE_CLINICAL
  val isGreenTheme = themeMode == AppThemeMode.GREEN_EMERALD

  val trackColor = when (themeMode) {
    AppThemeMode.WHITE_CLINICAL -> Color(0xFFE5E5EA) // Authentic iOS UISegmentedControl track
    AppThemeMode.GREEN_EMERALD -> Color(0xFF063B36)
    AppThemeMode.DARK_OBSIDIAN -> IosDarkDockSurface
  }
  val trackBorderColor = when (themeMode) {
    AppThemeMode.WHITE_CLINICAL -> Color(0xFFCBD5E1)
    AppThemeMode.GREEN_EMERALD -> Color(0xFF34D399).copy(alpha = 0.45f)
    AppThemeMode.DARK_OBSIDIAN -> NeonCyanText.copy(alpha = 0.38f)
  }

  Surface(
    shape = IosSharpWellShape,
    color = trackColor,
    tonalElevation = 4.dp,
    shadowElevation = 3.dp,
    modifier = modifier
      .fillMaxWidth()
      .border(
        width = 1.dp,
        color = trackBorderColor,
        shape = IosSharpWellShape
      )
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(3.5.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      tabs.forEachIndexed { idx, tab ->
        val isSelected = tab.index == selectedIndex
        val activeThumbBg = when {
          isSelected && isWhiteTheme -> Color.White
          isSelected && isGreenTheme -> Color(0xFF047857)
          isSelected -> IosSquircleGlassFill
          isWhiteTheme -> Color.Transparent
          isGreenTheme -> Color(0xFF042F2E)
          else -> Color(0xFF0C2138)
        }
        val bgColor by animateColorAsState(
          targetValue = activeThumbBg,
          animationSpec = spring(),
          label = "subtab_bg"
        )
        val activeAccent = when {
          isWhiteTheme -> EmeraldOptimal
          isGreenTheme -> Color(0xFF6EE7B7)
          else -> NeonAquaAccent
        }
        val inactiveText = when {
          isWhiteTheme -> Color(0xFF334155)
          isGreenTheme -> Color(0xFFD1FAE5).copy(alpha = 0.85f)
          else -> NeonCyanBright
        }

        Surface(
          shape = IosSharpButtonShape,
          color = bgColor,
          shadowElevation = if (isSelected) 3.dp else 0.dp,
          border = BorderStroke(
            width = if (isSelected) 1.25.dp else 0.75.dp,
            color = if (isSelected) {
              activeAccent
            } else {
              if (isWhiteTheme) Color(0xFFCBD5E1).copy(alpha = 0.55f)
              else NeonCyanText.copy(alpha = 0.22f)
            }
          ),
          modifier = Modifier
            .height(35.dp)
            .clip(IosSharpButtonShape)
            .clickable { onSelectTab(tab.index) }
            .testTag("subtab_option_${tab.index}")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
          ) {
            Box(
              modifier = Modifier
                .size(width = 6.dp, height = 6.dp)
                .clip(IosSharpMicroShape)
                .background(
                  if (isSelected) {
                    activeAccent
                  } else {
                    inactiveText.copy(alpha = 0.55f)
                  }
                )
            )
            Text(
              text = tab.title,
              style = MaterialTheme.typography.labelLarge,
              fontSize = 11.5.sp,
              lineHeight = 14.sp,
              letterSpacing = (-0.15).sp,
              fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
              color = if (isSelected) {
                if (isWhiteTheme) Color(0xFF0F172A) else activeAccent
              } else {
                inactiveText
              },
              maxLines = 1,
              softWrap = false
            )
            if (!tab.badge.isNullOrBlank()) {
              Surface(
                shape = IosSharpMicroShape,
                color = if (isSelected) {
                  activeAccent.copy(alpha = 0.18f)
                } else {
                  if (isWhiteTheme) Color(0xFFCBD5E1).copy(alpha = 0.45f)
                  else NeonCyanText.copy(alpha = 0.14f)
                }
              ) {
                Text(
                  text = tab.badge,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontSize = 8.5.sp,
                  lineHeight = 10.sp,
                  letterSpacing = 0.2.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = if (isSelected) activeAccent else inactiveText,
                  maxLines = 1,
                  softWrap = false,
                  modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
              }
            }
          }
        }

        // Authentic iOS UISegmentedControl vertical hairline separator
        if (idx < tabs.lastIndex && !isSelected && tabs[idx + 1].index != selectedIndex) {
          Box(
            modifier = Modifier
              .width(0.8.dp)
              .height(16.dp)
              .background(
                if (isWhiteTheme) Color(0xFFCBD5E1)
                else Color.White.copy(alpha = 0.14f)
              )
          )
        }
      }
    }
  }
}

// ============================================================================
// UPGRADE #1: IN-PLACE EDIT DIALOG & UNDO DELETE BANNER CARD
// ============================================================================

@Composable
fun UndoDeleteBannerCard(
  deletedRecord: LeaveRecord,
  onUndoDelete: () -> Unit,
  onDismissUndo: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = StatusPendingOrange.copy(alpha = 0.14f),
    border = BorderStroke(1.2.dp, StatusPendingOrange.copy(alpha = 0.50f)),
    modifier = modifier
      .fillMaxWidth()
      .testTag("undo_delete_banner_card")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "DELETED: ${ClinicalDateFormatter.formatFriendlyWithDay(deletedRecord.dateIso)} • ${deletedRecord.category.shortLabel}",
          fontFamily = JetBrainsMonoFontFamily,
          fontSize = 11.sp,
          lineHeight = 14.sp,
          fontWeight = FontWeight.Bold,
          color = StatusPendingOrange,
          maxLines = 1,
          softWrap = false
        )
        Text(
          text = deletedRecord.notes.ifBlank { deletedRecord.category.fullTitle },
          style = MaterialTheme.typography.bodySmall,
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Button(
          onClick = onUndoDelete,
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = EmeraldOptimal,
            contentColor = Color.White
          ),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
          modifier = Modifier
            .height(32.dp)
            .testTag("undo_delete_restore_btn")
        ) {
          Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "UNDO",
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
          )
        }
        IconButton(
          onClick = onDismissUndo,
          modifier = Modifier.size(28.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Clear,
            contentDescription = "Dismiss Undo",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(15.dp)
          )
        }
      }
    }
  }
}

@Composable
fun EditLeaveRecordDialog(
  record: LeaveRecord,
  onDismiss: () -> Unit,
  onSaveEditedRecord: (LeaveRecord) -> Unit
) {
  var selectedCat by remember { mutableStateOf(record.category) }
  var dateIso by remember { mutableStateOf(record.dateIso) }
  var pairedDateIso by remember { mutableStateOf(record.pairedDateIso) }
  var notes by remember {
    mutableStateOf(
      record.notes
        .replace("[FN] ", "")
        .replace("[AN] ", "")
    )
  }
  var selectedSession by remember {
    mutableStateOf(HalfDaySession.fromNotesOrFlag(record.notes, record.isHalfDay))
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(26.dp),
    title = {
      ExecutiveDialogHeader(
        icon = Icons.Default.Edit,
        accent = selectedCat.accentColor(),
        title = "Edit Leave / Duty Entry",
        subtitle = "RECORD ${record.id.take(10).uppercase()} • ${ClinicalDateFormatter.formatFriendlyDate(dateIso)}"
      )
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.verticalScroll(rememberScrollState())
      ) {
        Text(
          text = "Category:",
          style = MaterialTheme.typography.labelLarge
        )
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          LeaveCategory.entries.forEach { cat ->
            val isSel = cat == selectedCat
            val accent = cat.accentColor()
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (isSel) accent else accent.copy(alpha = 0.12f),
              modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                  selectedCat = cat
                  if (cat != LeaveCategory.CL) {
                    selectedSession = HalfDaySession.FULL_DAY
                  }
                }
            ) {
              Text(
                text = cat.shortLabel,
                color = if (isSel) Color.White else accent,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
              )
            }
          }
        }

        DatePickerOutlinedField(
          value = dateIso,
          onValueChange = { dateIso = it.trim() },
          label = "Date (YYYY-MM-DD)",
          modifier = Modifier.fillMaxWidth()
        )

        if (selectedCat == LeaveCategory.CL) {
          Text(
            text = "Session (Full Day vs Forenoon FN / Afternoon AN):",
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
              ) {
                Box(
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 7.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = sess.badgeLabel,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
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

        DatePickerOutlinedField(
          value = pairedDateIso,
          onValueChange = { pairedDateIso = it.trim() },
          label = "Paired GH Worked / Duty Date (Optional)",
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Reason / Remarks / Order Ref") },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val isoRegex = Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")
          if (dateIso.matches(isoRegex)) {
            val newYear = dateIso.take(4).toLongOrNull() ?: record.year
            val isHalf = selectedSession != HalfDaySession.FULL_DAY
            val sessionPrefix = when (selectedSession) {
              HalfDaySession.FORENOON_FN -> "[FN] "
              HalfDaySession.AFTERNOON_AN -> "[AN] "
              HalfDaySession.FULL_DAY -> ""
            }
            val updatedStatus = if (pairedDateIso.isNotBlank()) "VERIFIED_GREEN" else record.highlightStatus
            onSaveEditedRecord(
              record.copy(
                leaveType = selectedCat.code,
                dateIso = dateIso,
                year = newYear,
                pairedDateIso = pairedDateIso,
                notes = "$sessionPrefix${notes.ifBlank { selectedCat.fullTitle }}".trim(),
                highlightStatus = updatedStatus,
                isHalfDay = isHalf
              )
            )
          }
        },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.testTag("save_edited_leave_btn")
      ) {
        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Update Entry")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

// ============================================================================
// UPGRADE #2: GLOBAL INSTANT SEARCH & MULTI-YEAR CROSS-FILTER BAR IN LEDGER
// ============================================================================

@Composable
fun GlobalLedgerSearchAndFilterCard(
  searchQuery: String,
  onSearchQueryChange: (String) -> Unit,
  searchAllYears: Boolean,
  onToggleSearchAllYears: () -> Unit,
  onlyHalfDays: Boolean,
  onToggleOnlyHalfDays: () -> Unit,
  onlyPairedOrRef: Boolean,
  onToggleOnlyPairedOrRef: () -> Unit,
  resultCount: Int,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(22.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    shadowElevation = 2.dp,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    modifier = modifier
      .fillMaxWidth()
      .testTag("global_ledger_search_card")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = {
          Text(
            text = "Search date, Pongal, Univ Exam, Order Ref, FN/AN...",
            fontSize = 12.sp
          )
        },
        leadingIcon = {
          Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search Ledger",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
          )
        },
        trailingIcon = {
          if (searchQuery.isNotBlank()) {
            IconButton(onClick = { onSearchQueryChange("") }) {
              Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Clear Search",
                modifier = Modifier.size(16.dp)
              )
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("ledger_global_search_input")
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        SearchQuickTogglePill(
          label = if (searchAllYears) "Scope: All Years (2023–2027)" else "Scope: Selected Year Only",
          isActive = searchAllYears,
          accent = EmeraldOptimal,
          onClick = onToggleSearchAllYears
        )
        SearchQuickTogglePill(
          label = "½ Day (FN/AN) Only",
          isActive = onlyHalfDays,
          accent = BadgeClGreen,
          onClick = onToggleOnlyHalfDays
        )
        SearchQuickTogglePill(
          label = "Paired / Cert Ref",
          isActive = onlyPairedOrRef,
          accent = BadgeColCyan,
          onClick = onToggleOnlyPairedOrRef
        )
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surfaceVariant
        ) {
          Text(
            text = "$resultCount MATCHES",
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun SearchQuickTogglePill(
  label: String,
  isActive: Boolean,
  accent: Color,
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = if (isActive) accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    border = BorderStroke(
      1.dp,
      if (isActive) accent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    ),
    modifier = Modifier
      .clip(RoundedCornerShape(10.dp))
      .clickable { onClick() }
  ) {
    Text(
      text = label,
      fontFamily = JetBrainsMonoFontFamily,
      fontSize = 10.sp,
      lineHeight = 12.sp,
      fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
      color = if (isActive) accent else MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
      softWrap = false,
      modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
    )
  }
}

// ============================================================================
// UPGRADE #4: ACADEMIC ON-DUTY (OD) SUB-QUOTA TRACKER CARD (DME / TNDrMGRMU)
// ============================================================================

@Composable
fun AcademicOdSubQuotaCard(
  allRecords: List<LeaveRecord>,
  selectedYear: Long,
  onQuickLogSubOd: (odSubCategory: OdSubCategory, dateIso: String, notes: String) -> Unit,
  modifier: Modifier = Modifier
) {
  val summary = remember(allRecords, selectedYear) {
    AcademicOdSubQuotaEngine.analyzeOdSubQuotas(allRecords, selectedYear)
  }
  var activeSubOdDialog by remember { mutableStateOf<OdSubCategory?>(null) }

  Surface(
    shape = RoundedCornerShape(26.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 3.dp,
    shadowElevation = 4.dp,
    modifier = modifier
      .fillMaxWidth()
      .testTag("academic_od_sub_quota_card")
      .border(
        width = 1.2.dp,
        brush = Brush.linearGradient(
          colors = listOf(
            BadgeOnDutyBlue.copy(alpha = 0.50f),
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
            color = BadgeOnDutyBlue.copy(alpha = 0.16f),
            border = BorderStroke(1.dp, BadgeOnDutyBlue.copy(alpha = 0.40f)),
            modifier = Modifier.size(40.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.School,
                contentDescription = "Academic On-Duty Sub-Quota Tracker",
                tint = BadgeOnDutyBlue,
                modifier = Modifier.size(20.dp)
              )
            }
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "On-Duty (OD) Academic Sub-Quotas ($selectedYear)",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "TN Dr. M.G.R. Med. Univ. Examiner, CME/ANCIPS, DME & Court Board Caps",
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
          color = BadgeOnDutyBlue.copy(alpha = 0.15f),
          border = BorderStroke(1.dp, BadgeOnDutyBlue.copy(alpha = 0.40f))
        ) {
          Text(
            text = "${summary.totalOdUsed}d OD TOTAL",
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = BadgeOnDutyBlue,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
          )
        }
      }

      summary.subQuotas.forEach { item ->
        val subAccent = when (item.subCategory) {
          OdSubCategory.UNIV_EXAM -> BadgeOnDutyBlue
          OdSubCategory.CME_CONF -> EmeraldOptimal
          OdSubCategory.DME_NMC -> BadgePlGold
          OdSubCategory.COURT_BOARD -> BadgeColCyan
        }
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
          border = BorderStroke(1.dp, subAccent.copy(alpha = 0.30f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
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
                    color = subAccent.copy(alpha = 0.18f)
                  ) {
                    Text(
                      text = item.subCategory.shortTag,
                      fontFamily = JetBrainsMonoFontFamily,
                      fontSize = 9.sp,
                      fontWeight = FontWeight.Bold,
                      color = subAccent,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                  Text(
                    text = item.subCategory.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                  )
                }
                Text(
                  text = item.subCategory.ruleNote,
                  style = MaterialTheme.typography.bodySmall,
                  fontSize = 10.sp,
                  lineHeight = 13.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  softWrap = false,
                  overflow = TextOverflow.Ellipsis
                )
              }

              Spacer(modifier = Modifier.width(8.dp))

              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Text(
                  text = "${item.usedDays}/${item.ceilingDays}d",
                  fontFamily = JetBrainsMonoFontFamily,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = subAccent
                )
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = subAccent.copy(alpha = 0.16f),
                  border = BorderStroke(1.dp, subAccent.copy(alpha = 0.40f)),
                  modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { activeSubOdDialog = item.subCategory }
                ) {
                  Text(
                    text = "+ Log",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = subAccent,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                  )
                }
              }
            }

            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
              Box(
                modifier = Modifier
                  .fillMaxWidth(item.utilizationFraction.coerceIn(0.05f, 1f))
                  .height(5.dp)
                  .clip(RoundedCornerShape(3.dp))
                  .background(subAccent)
              )
            }
          }
        }
      }
    }
  }

  activeSubOdDialog?.let { subCat ->
    var odDate by remember { mutableStateOf("$selectedYear-11-18") }
    var odNotes by remember { mutableStateOf(subCat.title) }
    AlertDialog(
      onDismissRequest = { activeSubOdDialog = null },
      shape = RoundedCornerShape(26.dp),
      title = {
        ExecutiveDialogHeader(
          icon = Icons.Default.School,
          accent = BadgeOnDutyBlue,
          title = "Log ${subCat.shortTag} On-Duty",
          subtitle = "DME / UNIVERSITY SUB-QUOTA • ${ClinicalDateFormatter.formatFriendlyDate(odDate)}"
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = subCat.ruleNote,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          DatePickerOutlinedField(
            value = odDate,
            onValueChange = { odDate = it.trim() },
            label = "On-Duty Date (YYYY-MM-DD)",
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = odNotes,
            onValueChange = { odNotes = it },
            label = { Text("College / University / Order Details") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (odDate.matches(Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$"))) {
              onQuickLogSubOd(subCat, odDate, "[${subCat.code}] $odNotes")
              activeSubOdDialog = null
            }
          },
          shape = RoundedCornerShape(12.dp)
        ) {
          Text("Save On-Duty")
        }
      },
      dismissButton = {
        TextButton(onClick = { activeSubOdDialog = null }) {
          Text("Cancel")
        }
      }
    )
  }
}

// ============================================================================
// UPGRADE #5: 1-TAP "HOLIDAY / SUNDAY CALL DUTY → COMP-OFF" DUAL LOGGER CARD
// ============================================================================

@Composable
fun DualHolidayDutyColLoggerCard(
  selectedYear: Long,
  onLogDualDutyAndCol: (dutyDateIso: String, colDateIso: String, dutyNotes: String) -> Unit,
  modifier: Modifier = Modifier
) {
  var dutyDateIso by remember { mutableStateOf("$selectedYear-11-08") }
  var colDateIso by remember { mutableStateOf("$selectedYear-11-23") }
  var dutyNotes by remember {
    mutableStateOf("24-Hr Psychiatry Holiday / Sunday Call Duty at GEMC&H")
  }
  var alsoScheduleColNow by remember { mutableStateOf(true) }

  Surface(
    shape = RoundedCornerShape(26.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 3.dp,
    shadowElevation = 4.dp,
    modifier = modifier
      .fillMaxWidth()
      .testTag("dual_holiday_duty_col_logger_card")
      .border(
        width = 1.2.dp,
        brush = Brush.linearGradient(
          colors = listOf(
            BadgeGhLime.copy(alpha = 0.50f),
            BadgeColCyan.copy(alpha = 0.40f)
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
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Surface(
          shape = RoundedCornerShape(13.dp),
          color = BadgeGhLime.copy(alpha = 0.16f),
          border = BorderStroke(1.dp, BadgeGhLime.copy(alpha = 0.40f)),
          modifier = Modifier.size(40.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.LocalHospital,
              contentDescription = "Dual Holiday Duty & Comp-Off Logger",
              tint = BadgeGhLime,
              modifier = Modifier.size(20.dp)
            )
          }
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "1-Tap Holiday Call Duty ↔ Comp-Off Dual Logger",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = "Logs GH Worked duty + In-Lieu + paired COL in one atomic verified step",
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

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DatePickerOutlinedField(
          value = dutyDateIso,
          onValueChange = { dutyDateIso = it.trim() },
          label = "GH / Call Duty Date",
          modifier = Modifier.weight(1f)
        )
        if (alsoScheduleColNow) {
          DatePickerOutlinedField(
            value = colDateIso,
            onValueChange = { colDateIso = it.trim() },
            label = "Paired COL Leave Date",
            modifier = Modifier.weight(1f)
          )
        }
      }

      OutlinedTextField(
        value = dutyNotes,
        onValueChange = { dutyNotes = it },
        label = { Text("Hospital Turn / Call Duty Description") },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
      )

      Surface(
        shape = RoundedCornerShape(14.dp),
        color = EmeraldOptimal.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, EmeraldOptimal.copy(alpha = 0.28f)),
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .clickable { alsoScheduleColNow = !alsoScheduleColNow }
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Checkbox(
            checked = alsoScheduleColNow,
            onCheckedChange = { alsoScheduleColNow = it }
          )
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Simultaneously pair & book COL date (100% Verified)",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = EmeraldOptimal,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = if (alsoScheduleColNow) {
                "Auto-links ${ClinicalDateFormatter.formatFriendlyDate(dutyDateIso)} duty → ${ClinicalDateFormatter.formatFriendlyDate(colDateIso)} COL"
              } else {
                "Banks ${ClinicalDateFormatter.formatFriendlyDate(dutyDateIso)} as an unused GH credit for future COL"
              },
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 10.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }

      Button(
        onClick = {
          val isoRegex = Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")
          if (dutyDateIso.matches(isoRegex)) {
            val targetCol = if (alsoScheduleColNow && colDateIso.matches(isoRegex)) colDateIso else ""
            onLogDualDutyAndCol(dutyDateIso, targetCol, dutyNotes)
          }
        },
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = EmeraldOptimal,
          contentColor = Color.White
        ),
        modifier = Modifier
          .fillMaxWidth()
          .height(44.dp)
          .testTag("submit_dual_duty_col_btn")
      ) {
        Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(17.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (alsoScheduleColNow) "Log GH Duty + Paired COL (100% Verified)" else "Bank Unclaimed GH Duty Credit",
          fontFamily = JetBrainsMonoFontFamily,
          fontSize = 11.5.sp,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          softWrap = false
        )
      }
    }
  }
}

// ============================================================================
// UPGRADE #6: TN GOVT LTC (LEAVE TRAVEL CONCESSION) & ENCASHMENT TRACKER CARD
// ============================================================================

@Composable
fun TnGovtLtcBlockTrackerCard(
  onShowStatusBanner: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var ltcState by remember { mutableStateOf(LtcBlockTrackerStore.readState(context)) }
  var showEditLtcDialog by remember { mutableStateOf(false) }

  Surface(
    shape = RoundedCornerShape(26.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 3.dp,
    shadowElevation = 4.dp,
    modifier = modifier
      .fillMaxWidth()
      .testTag("tn_govt_ltc_block_tracker_card")
      .border(
        width = 1.2.dp,
        color = BadgePlGold.copy(alpha = 0.40f),
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
                imageVector = Icons.Default.FlightTakeoff,
                contentDescription = "TN Govt LTC Block Tracker",
                tint = BadgePlGold,
                modifier = Modifier.size(20.dp)
              )
            }
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "TN Govt LTC & Leave Encashment Block",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = ltcState.blockPeriodLabel,
              style = MaterialTheme.typography.bodySmall,
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              softWrap = false
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { showEditLtcDialog = true }
            .testTag("edit_ltc_block_btn")
        ) {
          Text(
            text = "UPDATE LTC",
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
          )
        }
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        LtcStatusTile(
          title = "HOME TOWN LTC",
          statusText = if (ltcState.homeTownAvailed) "AVAILED" else "ELIGIBLE",
          subText = ltcState.homeTownDateNote,
          accent = if (ltcState.homeTownAvailed) BadgeColCyan else EmeraldOptimal,
          onClick = {
            val updated = ltcState.copy(homeTownAvailed = !ltcState.homeTownAvailed)
            ltcState = updated
            LtcBlockTrackerStore.saveState(context, updated)
          },
          modifier = Modifier.weight(1f).fillMaxHeight()
        )
        LtcStatusTile(
          title = "ALL-INDIA LTC",
          statusText = if (ltcState.allIndiaAvailed) "AVAILED" else "ELIGIBLE",
          subText = ltcState.allIndiaDestination,
          accent = if (ltcState.allIndiaAvailed) BadgeColCyan else EmeraldOptimal,
          onClick = {
            val updated = ltcState.copy(allIndiaAvailed = !ltcState.allIndiaAvailed)
            ltcState = updated
            LtcBlockTrackerStore.saveState(context, updated)
          },
          modifier = Modifier.weight(1f).fillMaxHeight()
        )
        LtcStatusTile(
          title = "LTC EL ENCASH",
          statusText = "${ltcState.elEncashedWithLtcDays} / 10d",
          subText = "10d EL Encashable",
          accent = BadgePlGold,
          onClick = { showEditLtcDialog = true },
          modifier = Modifier.weight(1f).fillMaxHeight()
        )
      }
    }
  }

  if (showEditLtcDialog) {
    var destInput by remember { mutableStateOf(ltcState.allIndiaDestination) }
    var encashInput by remember { mutableStateOf(ltcState.elEncashedWithLtcDays.toString()) }
    AlertDialog(
      onDismissRequest = { showEditLtcDialog = false },
      shape = RoundedCornerShape(26.dp),
      title = {
        ExecutiveDialogHeader(
          icon = Icons.Default.FlightTakeoff,
          accent = BadgePlGold,
          title = "Update 2024–2027 LTC Block",
          subtitle = "HOME TOWN & ALL-INDIA LTC • 10D EL ENCASHMENT"
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = destInput,
            onValueChange = { destInput = it },
            label = { Text("All-India LTC Destination / Order Note") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = encashInput,
            onValueChange = { encashInput = it.filter { ch -> ch.isDigit() } },
            label = { Text("EL Days Encashed with LTC (Max 10 Days)") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val updated = ltcState.copy(
              allIndiaDestination = destInput.trim().ifBlank { "Eligible" },
              elEncashedWithLtcDays = (encashInput.toIntOrNull() ?: 0).coerceIn(0, 10)
            )
            ltcState = updated
            LtcBlockTrackerStore.saveState(context, updated)
            showEditLtcDialog = false
            onShowStatusBanner("Updated 2024–2027 TN Govt LTC Block status!")
          },
          shape = RoundedCornerShape(12.dp)
        ) {
          Text("Save LTC Block")
        }
      },
      dismissButton = {
        TextButton(onClick = { showEditLtcDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
private fun LtcStatusTile(
  title: String,
  statusText: String,
  subText: String,
  accent: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = accent.copy(alpha = 0.10f),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.30f)),
    modifier = modifier
      .clip(RoundedCornerShape(14.dp))
      .clickable { onClick() }
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 10.dp, vertical = 9.dp),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = title,
        fontFamily = JetBrainsMonoFontFamily,
        fontSize = 8.sp,
        lineHeight = 10.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        softWrap = false
      )
      Text(
        text = statusText,
        fontFamily = JetBrainsMonoFontFamily,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.ExtraBold,
        color = accent,
        maxLines = 1,
        softWrap = false
      )
      Text(
        text = subText,
        style = MaterialTheme.typography.bodySmall,
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
// UPGRADE #7: PHONE CALENDAR SYNC & .ICS CALENDAR EXPORT CARD
// ============================================================================

@Composable
fun PhoneCalendarIcsExportCard(
  allRecords: List<LeaveRecord>,
  onShowStatusBanner: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current

  Surface(
    shape = RoundedCornerShape(24.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 3.dp,
    shadowElevation = 3.dp,
    modifier = modifier
      .fillMaxWidth()
      .testTag("phone_calendar_ics_export_card")
      .border(
        width = 1.dp,
        color = EmeraldOptimal.copy(alpha = 0.35f),
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
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = EmeraldOptimal.copy(alpha = 0.14f),
          border = BorderStroke(1.dp, EmeraldOptimal.copy(alpha = 0.35f)),
          modifier = Modifier.size(38.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.CalendarMonth,
              contentDescription = "Google Calendar & .ICS Sync",
              tint = EmeraldOptimal,
              modifier = Modifier.size(20.dp)
            )
          }
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Google / Phone Calendar & .ICS Sync",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false
          )
          Text(
            text = "Export 2026–2027 Leaves & TN Govt Holidays to Google Calendar / Outlook",
            style = MaterialTheme.typography.bodySmall,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
          )
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
            CalendarIcsSyncService.exportAndShareIcsFile(context, allRecords)
              .onSuccess {
                onShowStatusBanner("Generated 2026–2027 .ICS Calendar file for Google Calendar / Outlook!")
              }
              .onFailure { err ->
                onShowStatusBanner("ICS export error: ${err.localizedMessage}")
              }
          },
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = EmeraldOptimal,
            contentColor = Color.White
          ),
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .testTag("export_ics_calendar_btn")
        ) {
          Icon(Icons.Default.EventAvailable, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Export .ICS File",
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
        }

        OutlinedButton(
          onClick = {
            val ok = CalendarIcsSyncService.addSingleLeaveToPhoneCalendar(
              context = context,
              title = "HOD Psychiatry Year-End / Christmas Bridge",
              startDateIso = "2026-12-24",
              endDateIso = "2026-12-27",
              description = "4-Day Holiday Bridge (Thu Dec 24 RH + Fri Dec 25 Christmas GH + Sat–Sun Weekend)"
            )
            if (ok) {
              onShowStatusBanner("Opened Phone Calendar with pre-filled Holiday Bridge event!")
            } else {
              onShowStatusBanner("Use Export .ICS File to import into your calendar app.")
            }
          },
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, BadgeColCyan.copy(alpha = 0.50f)),
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .testTag("sync_phone_calendar_btn")
        ) {
          Text(
            text = "+ Phone Calendar",
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = BadgeColCyan
          )
        }
      }
    }
  }
}

// ============================================================================
// UPGRADE #9: 1-TAP WHATSAPP / CLIPBOARD EXECUTIVE DIGEST BAR
// ============================================================================

@Composable
fun ExecutiveDigestShareBar(
  titleLabel: String,
  digestContentProvider: () -> String,
  onShowStatusBanner: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current

  Surface(
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    border = BorderStroke(1.dp, EmeraldOptimal.copy(alpha = 0.30f)),
    modifier = modifier
      .fillMaxWidth()
      .testTag("executive_digest_share_bar")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 9.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "EXECUTIVE WHATSAPP / E-SECTION DIGEST",
          fontFamily = JetBrainsMonoFontFamily,
          fontSize = 9.sp,
          lineHeight = 11.sp,
          fontWeight = FontWeight.Bold,
          color = EmeraldOptimal,
          maxLines = 1,
          softWrap = false
        )
        Text(
          text = titleLabel,
          style = MaterialTheme.typography.bodySmall,
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable {
              val text = digestContentProvider()
              ExecutiveDigestShareService.copyDigestToClipboard(context, titleLabel, text)
              onShowStatusBanner("Copied formatted $titleLabel to clipboard!")
            }
            .testTag("copy_digest_btn")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(
              imageVector = Icons.Default.ContentCopy,
              contentDescription = "Copy Digest",
              tint = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.size(13.dp)
            )
            Text(
              text = "Copy",
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = EmeraldOptimal.copy(alpha = 0.16f),
          border = BorderStroke(1.dp, EmeraldOptimal.copy(alpha = 0.45f)),
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable {
              val text = digestContentProvider()
              ExecutiveDigestShareService.shareDigestToWhatsAppOrApps(
                context = context,
                subject = titleLabel,
                digestText = text
              )
            }
            .testTag("share_whatsapp_digest_btn")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Share,
              contentDescription = "Share to WhatsApp",
              tint = EmeraldOptimal,
              modifier = Modifier.size(13.dp)
            )
            Text(
              text = "WhatsApp",
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = EmeraldOptimal
            )
          }
        }
      }
    }
  }
}

// ============================================================================
// UPGRADE #10: INTERACTIVE "WHAT-IF" LEAVE BALANCE SIMULATOR CARD (SANDBOX)
// ============================================================================

@Composable
fun WhatIfLeaveSimulatorCard(
  summary2026: YearCreditSummary,
  unusedGhCredits: Int,
  onCommitSimulatedWorkingDays: (category: LeaveCategory, workingDateIsos: List<String>, notes: String) -> Unit,
  modifier: Modifier = Modifier
) {
  var simStart by remember { mutableStateOf("2026-12-24") }
  var simEnd by remember { mutableStateOf("2026-12-30") }
  var simCategory by remember { mutableStateOf(LeaveCategory.CL) }

  val simResult = remember(simStart, simEnd, simCategory, summary2026, unusedGhCredits) {
    WhatIfLeaveSimulatorEngine.simulateTripImpact(
      startDateIso = simStart,
      endDateIso = simEnd,
      category = simCategory,
      summary2026 = summary2026,
      unusedGhCredits = unusedGhCredits
    )
  }

  val splicePreview = remember(simStart, simEnd) {
    SmartRangeSplicerEngine.analyzeRangeForAutoSkip(simStart, simEnd)
  }

  Surface(
    shape = RoundedCornerShape(26.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 3.dp,
    shadowElevation = 4.dp,
    modifier = modifier
      .fillMaxWidth()
      .testTag("what_if_leave_simulator_card")
      .border(
        width = 1.2.dp,
        brush = Brush.linearGradient(
          colors = listOf(
            EmeraldOptimal.copy(alpha = 0.50f),
            BadgeColCyan.copy(alpha = 0.35f)
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
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Surface(
          shape = RoundedCornerShape(13.dp),
          color = EmeraldOptimal.copy(alpha = 0.16f),
          border = BorderStroke(1.dp, EmeraldOptimal.copy(alpha = 0.40f)),
          modifier = Modifier.size(40.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.Science,
              contentDescription = "What-If Leave Balance Simulator",
              tint = EmeraldOptimal,
              modifier = Modifier.size(20.dp)
            )
          }
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "\"What-If\" Leave Balance Simulator (Sandbox)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = "Preview Before vs. After quota balances & auto-skip weekends/GH before booking",
            style = MaterialTheme.typography.bodySmall,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      // Category Selector for Simulation (CL | RH | COL | EL)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        listOf(LeaveCategory.CL, LeaveCategory.RH, LeaveCategory.COL, LeaveCategory.EL).forEach { cat ->
          val isSel = simCategory == cat
          val accent = cat.accentColor()
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isSel) accent.copy(alpha = 0.20f) else MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(
              width = if (isSel) 1.2.dp else 1.dp,
              color = if (isSel) accent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            ),
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(10.dp))
              .clickable { simCategory = cat }
          ) {
            Box(
              modifier = Modifier.padding(vertical = 7.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "Simulate ${cat.shortLabel}",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSel) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false
              )
            }
          }
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DatePickerOutlinedField(
          value = simStart,
          onValueChange = { simStart = it.trim() },
          label = "Trip Start",
          modifier = Modifier.weight(1f)
        )
        DatePickerOutlinedField(
          value = simEnd,
          onValueChange = { simEnd = it.trim() },
          label = "Trip End",
          modifier = Modifier.weight(1f)
        )
      }

      if (simResult != null && splicePreview != null) {
        val isAlert = simResult.exceedsTenDayContinuousCap || simResult.exceedsAvailableQuota
        val verdictColor = if (isAlert) StatusAlertRed else EmeraldOptimal

        // Before vs. After Balance Comparison Row
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          BeforeAfterSimPill(
            label = "CL BALANCE",
            beforeVal = "${simResult.clBefore}d",
            afterVal = "${simResult.clAfter}d",
            accent = BadgeClGreen,
            modifier = Modifier.weight(1f).fillMaxHeight()
          )
          BeforeAfterSimPill(
            label = "RH BALANCE",
            beforeVal = "${simResult.rhBefore}d",
            afterVal = "${simResult.rhAfter}d",
            accent = Color(0xFF38BDF8),
            modifier = Modifier.weight(1f).fillMaxHeight()
          )
          BeforeAfterSimPill(
            label = "FREE GH POOL",
            beforeVal = "${simResult.colFreeBefore}d",
            afterVal = "${simResult.colFreeAfter}d",
            accent = BadgeColCyan,
            modifier = Modifier.weight(1f).fillMaxHeight()
          )
        }

        Surface(
          shape = RoundedCornerShape(14.dp),
          color = verdictColor.copy(alpha = 0.11f),
          border = BorderStroke(1.dp, verdictColor.copy(alpha = 0.35f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Text(
              text = simResult.verdictHeadline,
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = verdictColor
            )
            Text(
              text = simResult.verdictDetail,
              style = MaterialTheme.typography.bodySmall,
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        if (splicePreview.workingDaysToDebit.isNotEmpty() && !isAlert) {
          Button(
            onClick = {
              val friendlyRange = ClinicalDateFormatter.formatRangeFriendly(simStart, simEnd)
              onCommitSimulatedWorkingDays(
                simCategory,
                splicePreview.workingDaysToDebit.map { it.dateIso },
                "Booked via What-If Simulator ($friendlyRange, weekends/GH auto-skipped)"
              )
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = EmeraldOptimal,
              contentColor = Color.White
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("commit_simulated_trip_btn")
          ) {
            Text(
              text = "Commit ${splicePreview.workingDaysToDebit.size} Working Day(s) as ${simCategory.shortLabel} (Skip ${splicePreview.skippedWeekendOrGhDays.size} Wknd/GH)",
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 11.sp,
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

@Composable
private fun BeforeAfterSimPill(
  label: String,
  beforeVal: String,
  afterVal: String,
  accent: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = accent.copy(alpha = 0.10f),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.30f)),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 10.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      Text(
        text = label,
        fontFamily = JetBrainsMonoFontFamily,
        fontSize = 8.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        softWrap = false
      )
      Text(
        text = "$beforeVal → $afterVal",
        fontFamily = JetBrainsMonoFontFamily,
        fontSize = 13.sp,
        fontWeight = FontWeight.ExtraBold,
        color = accent,
        maxLines = 1,
        softWrap = false
      )
      Text(
        text = "Before → After",
        fontSize = 9.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        softWrap = false
      )
    }
  }
}
