package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ColAuditStatus
import com.example.data.ColGhAuditItem
import com.example.data.LeaveRecord
import com.example.domain.ExecutiveDigestShareService
import com.example.domain.TnHolidays2027Catalog
import com.example.ui.theme.BadgeColCyan
import com.example.ui.theme.BadgeGhLime
import com.example.ui.theme.BadgeInLieuLavender
import com.example.ui.theme.BadgePlGold
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.IosSharpBadgeShape
import com.example.ui.theme.IosSharpButtonShape
import com.example.ui.theme.IosSharpCardShape
import com.example.ui.theme.IosSharpWellShape
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalNeumorphicColors
import com.example.ui.theme.StatusAlertRed
import com.example.ui.theme.StatusPendingOrange
import com.example.ui.theme.StatusVerifiedGreen
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun ColGhAuditTabScreen(
  state: LeaveUiState.Success,
  onSelectYear: (Long) -> Unit,
  onPairColWithGh: (colRecordId: String, ghDateIso: String) -> Unit,
  onAutoReconcileYear: (Long?) -> Unit,
  onAvailUnusedGhAsCol: (newColDateIso: String, ghRecord: LeaveRecord, notes: String) -> Unit,
  onOpenPdfExportModal: () -> Unit = {},
  onLogDualDutyAndCol: (dutyDateIso: String, colDateIso: String, dutyNotes: String) -> Unit = { _, _, _ -> },
  onShowStatusBanner: (String) -> Unit = {}
) {
  var showAllYears by remember { mutableStateOf(false) }
  var selectedStatusFilter by remember { mutableStateOf<ColAuditStatus?>(null) }
  var manualPairItem by remember { mutableStateOf<ColGhAuditItem?>(null) }
  var claimUnusedGhRecord by remember { mutableStateOf<LeaveRecord?>(null) }
  var activeSubTab by remember { mutableStateOf(0) }

  val audit = if (showAllYears) state.colGhAuditAllYears else state.colGhAuditYear
  val displayedItems = if (selectedStatusFilter == null) {
    audit.items
  } else {
    audit.items.filter { it.status == selectedStatusFilter }
  }

  val subTabs = remember(audit.items.size, audit.availableUnusedGhCount) {
    listOf(
      SubTabOption(
        index = 0,
        title = "3-Col Ledger",
        badge = "${audit.items.size}",
        accentColor = EmeraldOptimal
      ),
      SubTabOption(
        index = 1,
        title = "Unused GH Pool",
        badge = "${audit.availableUnusedGhCount} GH",
        accentColor = BadgeGhLime
      ),
      SubTabOption(
        index = 2,
        title = "6M Expiry Radar",
        badge = "FIFO",
        accentColor = BadgePlGold
      ),
      SubTabOption(
        index = 3,
        title = "Dual Log & Export",
        badge = "1-TAP",
        accentColor = BadgeColCyan
      )
    )
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .testTag("col_gh_audit_tab_list"),
    contentPadding = PaddingValues(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 104.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // 1. Executive 3-Way COL <-> In Lieu <-> GH Worked Audit Banner (Enterprise Mesh & Specular Glass)
    item {
      val matchRatio = if (audit.totalColTaken > 0) {
        (audit.fullyAccountedColCount.toFloat() / audit.totalColTaken.toFloat()).coerceIn(0f, 1f)
      } else {
        1f
      }
      val matchPct = (matchRatio * 100).toInt()

      val neu = LocalNeumorphicColors.current
      EnterpriseMeshHeroContainer(
        cornerRadius = 22.dp,
        primaryGlowColor = neu.primaryAccent,
        secondaryGlowColor = neu.secondaryAccent,
        contentPadding = PaddingValues(18.dp)
      ) {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              modifier = Modifier.weight(1f),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = null,
                tint = neu.primaryAccent,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "COL ↔ GH WORKED AUDIT",
                style = MaterialTheme.typography.labelMedium,
                color = neu.primaryAccent,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                softWrap = false
              )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
              modifier = Modifier
                .height(32.dp)
                .neumorphicExtruded(
                  shape = IosSharpBadgeShape,
                  elevation = 2.5.dp,
                  blurRadius = 5.dp,
                  accentTint = neu.primaryAccent,
                  accentTintAlpha = 0.08f,
                  borderAlpha = 0.38f,
                  neu = neu
                )
                .clickable { showAllYears = !showAllYears }
                .padding(horizontal = 12.dp)
                .testTag("toggle_audit_all_years"),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = if (showAllYears) "All Years (23–27)" else "Year: ${state.selectedYear}",
                style = MaterialTheme.typography.labelMedium,
                color = neu.textPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // 4 Geometrically Uniform Audit Metrics Inset Wells
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .height(androidx.compose.foundation.layout.IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            AuditStatMiniCard(
              label = "COL Taken",
              value = "${audit.totalColTaken}",
              sub = "${audit.fullyAccountedColCount} Verified",
              accent = Color(0xFF0284C7),
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
            )
            AuditStatMiniCard(
              label = "GH Worked",
              value = "${audit.totalGhWorked}",
              sub = "Hospital Duty",
              accent = Color(0xFF0D9488),
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
            )
            AuditStatMiniCard(
              label = "Unpaired",
              value = "${audit.unaccountedColCount}",
              sub = if (audit.unaccountedColCount == 0) "100% Clean" else "Needs Pair",
              accent = if (audit.unaccountedColCount == 0) EmeraldOptimal else Color(0xFFDC2626),
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
            )
            AuditStatMiniCard(
              label = "Unused GH",
              value = "${audit.availableUnusedGhCount}",
              sub = "Ready for COL",
              accent = Color(0xFFD97706),
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Live Reconciliation Integrity Telemetry Bar (Inset Trough)
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "3-COLUMN LEDGER RECONCILIATION INTEGRITY",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                color = neu.textSecondary,
                maxLines = 1,
                softWrap = false
              )
              Text(
                text = "$matchPct% VERIFIED",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (matchPct == 100) neu.primaryAccent else Color(0xFFD97706),
                maxLines = 1,
                softWrap = false
              )
            }
            NeumorphicLinearProgress(
              progress = matchRatio,
              accentColor = neu.primaryAccent,
              height = 8.dp
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          // One-Tap Auto-Reconcile Button
          Button(
            onClick = {
              onAutoReconcileYear(if (showAllYears) null else state.selectedYear)
            },
            shape = IosSharpButtonShape,
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("auto_reconcile_col_gh_button")
          ) {
            Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (audit.unaccountedColCount > 0) {
                "Auto-Reconcile & Pair ${audit.unaccountedColCount} Unpaired COL"
              } else {
                "100% of COLs Accounted & Paired (Re-Verify)"
              },
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
              maxLines = 1,
              softWrap = false,
              overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
          }
        }
      }
    }

    // Sub-Tab Switcher to keep Audit screen clean & organized
    item {
      EnterpriseSubTabSwitcher(
        tabs = subTabs,
        selectedIndex = activeSubTab,
        onSelectTab = { activeSubTab = it }
      )
    }

    when (activeSubTab) {
      0 -> {
        // Year Selector (when not in All-Years mode)
        if (!showAllYears) {
          item {
            IosSegmentedYearSelector(
              years = TnHolidays2027Catalog.supportedServiceYears,
              selectedYear = state.selectedYear,
              onSelectYear = onSelectYear
            )
          }
        }

        // Filter Pills by Audit Status (Neumorphic Soft-UI Pills)
        item {
          val filterNeu = LocalNeumorphicColors.current
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState())
              .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            val allSel = selectedStatusFilter == null
            Box(
              modifier = Modifier
                .height(36.dp)
                .then(
                  if (allSel) {
                    Modifier.neumorphicExtruded(
                      shape = IosSharpBadgeShape,
                      elevation = 2.5.dp,
                      blurRadius = 5.dp,
                      surfaceColor = filterNeu.primaryAccent,
                      neu = filterNeu
                    )
                  } else {
                    Modifier.neumorphicInset(
                      shape = IosSharpBadgeShape,
                      depth = 2.dp,
                      blurRadius = 4.dp,
                      neu = filterNeu
                    )
                  }
                )
                .clickable { selectedStatusFilter = null },
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "All (${audit.items.size})",
                color = if (allSel) Color.White else filterNeu.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(horizontal = 14.dp)
              )
            }

            val statusFilters = listOf(
              Triple(ColAuditStatus.VERIFIED_MATCH, "Verified (${audit.fullyAccountedColCount})", if (filterNeu.isDark) Color(0xFF4ADE80) else Color(0xFF15803D)),
              Triple(ColAuditStatus.UNPAIRED_COL, "Unpaired COL", if (filterNeu.isDark) Color(0xFFF87171) else Color(0xFFB91C1C)),
              Triple(ColAuditStatus.DOUBLE_CLAIMED_ALERT, "Double-Claimed (${audit.doubleClaimedCount})", if (filterNeu.isDark) Color(0xFFF87171) else Color(0xFFB91C1C)),
              Triple(ColAuditStatus.MISSING_IN_GH_COLUMN, "Missing GH", if (filterNeu.isDark) Color(0xFFFBBF24) else Color(0xFFB45309)),
              Triple(ColAuditStatus.AVAILABLE_GH_CREDIT, "Unused GH (${audit.availableUnusedGhCount})", if (filterNeu.isDark) Color(0xFF34D399) else Color(0xFF047857))
            )

            statusFilters.forEach { (st, label, color) ->
              val sel = selectedStatusFilter == st
              Box(
                modifier = Modifier
                  .height(36.dp)
                  .then(
                    if (sel) {
                      Modifier.neumorphicExtruded(
                        shape = IosSharpBadgeShape,
                        elevation = 2.5.dp,
                        blurRadius = 5.dp,
                        surfaceColor = color,
                        neu = filterNeu
                      )
                    } else {
                      Modifier.neumorphicInset(
                        shape = IosSharpBadgeShape,
                        depth = 2.dp,
                        blurRadius = 4.dp,
                        accentTint = color,
                        accentTintAlpha = 0.08f,
                        borderColor = color.copy(alpha = 0.28f),
                        neu = filterNeu
                      )
                    }
                  )
                  .clickable { selectedStatusFilter = if (sel) null else st },
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = label,
                  color = if (sel) Color.White else color,
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp,
                  maxLines = 1,
                  softWrap = false,
                  modifier = Modifier.padding(horizontal = 14.dp)
                )
              }
            }
          }
        }

        // Column Header Legend
        item {
          val legendNeu = LocalNeumorphicColors.current
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .neumorphicInset(
                shape = IosSharpButtonShape,
                depth = 2.dp,
                blurRadius = 4.dp,
                neu = legendNeu
              )
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 9.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "COL AVAILED",
                style = MaterialTheme.typography.labelMedium,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                color = if (legendNeu.isDark) Color(0xFF22D3EE) else Color(0xFF0E7490),
                fontWeight = FontWeight.ExtraBold
              )
              Text(
                text = "↔",
                style = MaterialTheme.typography.labelMedium,
                color = legendNeu.textSecondary
              )
              Text(
                text = "GH DUTY / IN LIEU",
                style = MaterialTheme.typography.labelMedium,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                color = legendNeu.primaryAccent,
                fontWeight = FontWeight.ExtraBold
              )
            }
          }
        }

        // Reconciliation Rows
        items(displayedItems, key = { it.id }) { item ->
          ColGhAuditSquircleCard(
            item = item,
            onQuickPairSuggested = {
              if (item.colRecordId.isNotEmpty() && item.suggestedFixGhDateIso.isNotEmpty()) {
                onPairColWithGh(item.colRecordId, item.suggestedFixGhDateIso)
              }
            },
            onOpenCustomPairModal = {
              manualPairItem = item
            },
            onSyncInLieuToGhColumn = {
              if (item.colRecordId.isNotEmpty() && item.inLieuDateIso.isNotEmpty() && item.inLieuDateIso != "MISSING") {
                onPairColWithGh(item.colRecordId, item.inLieuDateIso)
              }
            },
            onClaimUnusedGhCredit = {
              val rec = state.allRecords.firstOrNull { it.id == item.ghWorkedRecordId }
              if (rec != null) {
                claimUnusedGhRecord = rec
              }
            }
          )
        }
      }

      1 -> {
        // Sub-Tab 1: Unused GH Pool (Ready to Claim as COL)
        val unusedItems = audit.items.filter { it.status == ColAuditStatus.AVAILABLE_GH_CREDIT }
        items(unusedItems, key = { "fifo_${it.id}" }) { item ->
          ColGhAuditSquircleCard(
            item = item,
            onQuickPairSuggested = {},
            onOpenCustomPairModal = { manualPairItem = item },
            onSyncInLieuToGhColumn = {},
            onClaimUnusedGhCredit = {
              val rec = state.allRecords.firstOrNull { it.id == item.ghWorkedRecordId }
              if (rec != null) {
                claimUnusedGhRecord = rec
              }
            }
          )
        }
      }

      2 -> {
        // Sub-Tab 2: 6-Month Comp-Off (COL) Expiry Countdown & FIFO Oldest-Credit Radar
        item {
          ColSixMonthExpiryRadarCard(
            unusedGhRecords = audit.unusedGhRecords,
            onClaimFifoGhAsCol = { ghRec -> claimUnusedGhRecord = ghRec }
          )
        }
      }

      3 -> {
        // Sub-Tab 3: 1-Tap Holiday/Sunday Call Duty -> Comp-Off Dual Logger + Share & PDF Export
        item {
          DualHolidayDutyColLoggerCard(
            selectedYear = state.selectedYear,
            onLogDualDutyAndCol = onLogDualDutyAndCol
          )
        }

        item {
          ExecutiveDigestShareBar(
            titleLabel = "Share COL ↔ GH Reconciliation Digest (Dean's Office / WhatsApp)",
            digestContentProvider = {
              ExecutiveDigestShareService.buildColGhAuditDigest(
                yearLabel = if (showAllYears) "All Years (2023–2027)" else state.selectedYear.toString(),
                audit = audit
              )
            },
            onShowStatusBanner = onShowStatusBanner
          )
        }

        item {
          OfficialPdfExportBannerCard(
            selectedYear = state.selectedYear,
            onOpenPdfExportModal = onOpenPdfExportModal
          )
        }
      }
    }
  }

  // Modal 1: Pair an unaccounted COL with any Unused GH Worked credit (or custom GH date)
  manualPairItem?.let { auditItem ->
    val neu = LocalNeumorphicColors.current
    val unusedCandidates = state.colGhAuditAllYears.unusedGhRecords
    var customGhDate by remember {
      mutableStateOf(
        auditItem.suggestedFixGhDateIso.ifBlank {
          if (auditItem.inLieuDateIso != "MISSING") auditItem.inLieuDateIso else "${auditItem.year}-01-01"
        }
      )
    }

    AlertDialog(
      onDismissRequest = { manualPairItem = null },
      shape = IosSharpCardShape,
      containerColor = neu.baseBg,
      title = {
        ExecutiveDialogHeader(
          icon = Icons.Default.Link,
          accent = BadgeColCyan,
          title = "Pair COL with GH Worked",
          subtitle = "COL DATE: ${ClinicalDateFormatter.formatFriendlyDate(auditItem.colDateIso)}"
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Select an available unclaimed GH Worked date below or enter a specific GH Worked date to account for this COL:",
            style = MaterialTheme.typography.bodyMedium,
            color = neu.textSecondary
          )

          if (unusedCandidates.isNotEmpty()) {
            Text(
              text = "Available Unused GH Worked Credits:",
              style = MaterialTheme.typography.labelLarge,
              color = EmeraldOptimal
            )
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 4.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              unusedCandidates.take(6).forEach { gh ->
                val isChosen = customGhDate == gh.dateIso
                Box(
                  modifier = Modifier
                    .then(
                      if (isChosen) {
                        Modifier.neumorphicExtruded(
                          shape = IosSharpButtonShape,
                          elevation = 2.5.dp,
                          blurRadius = 5.dp,
                          surfaceColor = BadgeGhLime,
                          neu = neu
                        )
                      } else {
                        Modifier.neumorphicInset(
                          shape = IosSharpButtonShape,
                          depth = 2.dp,
                          blurRadius = 4.dp,
                          accentTint = BadgeGhLime,
                          accentTintAlpha = 0.10f,
                          borderColor = BadgeGhLime.copy(alpha = 0.35f),
                          neu = neu
                        )
                      }
                    )
                    .clickable { customGhDate = gh.dateIso }
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = "${ClinicalDateFormatter.formatFriendlyDate(gh.dateIso)} • ${gh.notes.ifBlank { "GH Duty" }}",
                      color = if (isChosen) Color(0xFF042F2E) else if (neu.isDark) BadgeGhLime else EmeraldOptimal,
                      fontWeight = FontWeight.Bold,
                      fontSize = 12.sp
                    )
                  }
                }
              }
            }
          }

          DatePickerOutlinedField(
            value = customGhDate,
            onValueChange = { customGhDate = it.trim() },
            label = "GH Worked Date (YYYY-MM-DD)",
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (customGhDate.matches(Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$"))) {
              onPairColWithGh(auditItem.colRecordId, customGhDate)
              manualPairItem = null
            }
          },
          shape = IosSharpButtonShape,
          colors = ButtonDefaults.buttonColors(containerColor = EmeraldOptimal)
        ) {
          Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Confirm Pairing")
        }
      },
      dismissButton = {
        TextButton(onClick = { manualPairItem = null }) {
          Text("Cancel")
        }
      }
    )
  }

  // Modal 2: Avail an Unused GH Worked credit as a new COL Leave
  claimUnusedGhRecord?.let { ghRec ->
    val neu = LocalNeumorphicColors.current
    var newColDate by remember { mutableStateOf("${ghRec.year}-11-23") }
    var colNotes by remember { mutableStateOf("COL availed against GH Worked on ${ghRec.dateIso}") }

    AlertDialog(
      onDismissRequest = { claimUnusedGhRecord = null },
      shape = IosSharpCardShape,
      containerColor = neu.baseBg,
      title = {
        ExecutiveDialogHeader(
          icon = Icons.Default.EventAvailable,
          accent = EmeraldOptimal,
          title = "Avail COL for GH Worked",
          subtitle = "GH DUTY: ${ClinicalDateFormatter.formatFriendlyDate(ghRec.dateIso)}"
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "You worked on Government Holiday ${ClinicalDateFormatter.formatFriendlyDate(ghRec.dateIso)} (${ghRec.notes.ifBlank { "Hospital Duty" }}) and haven't linked a COL to it yet. Pick a date to avail this COL:",
            style = MaterialTheme.typography.bodyMedium,
            color = neu.textSecondary
          )
          DatePickerOutlinedField(
            value = newColDate,
            onValueChange = { newColDate = it.trim() },
            label = "COL Leave Date to Avail (YYYY-MM-DD)",
            modifier = Modifier.fillMaxWidth()
          )
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
              value = colNotes,
              onValueChange = { colNotes = it },
              label = { Text("Notes") },
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
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (newColDate.matches(Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$"))) {
              onAvailUnusedGhAsCol(newColDate, ghRec, colNotes)
              claimUnusedGhRecord = null
            }
          },
          shape = IosSharpButtonShape,
          colors = ButtonDefaults.buttonColors(containerColor = EmeraldOptimal)
        ) {
          Icon(Icons.Default.EventAvailable, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Log & Pair COL")
        }
      },
      dismissButton = {
        TextButton(onClick = { claimUnusedGhRecord = null }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
private fun AuditStatMiniCard(
  label: String,
  value: String,
  sub: String,
  accent: Color,
  modifier: Modifier = Modifier
) {
  val neu = com.example.ui.theme.LocalNeumorphicColors.current
  Box(
    modifier = modifier
      .neumorphicInset(
        shape = com.example.ui.theme.IosSharpButtonShape,
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
        .padding(horizontal = 9.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
      horizontalAlignment = Alignment.Start
    ) {
      Text(
        text = label,
        color = neu.textSecondary,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        softWrap = false,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
      )
      Text(
        text = value,
        color = accent,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 22.sp,
        fontFamily = JetBrainsMonoFontFamily,
        maxLines = 1,
        softWrap = false
      )
      Text(
        text = sub,
        color = neu.textSecondary,
        fontSize = 9.sp,
        lineHeight = 11.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        softWrap = false,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun ColGhAuditSquircleCard(
  item: ColGhAuditItem,
  onQuickPairSuggested: () -> Unit,
  onOpenCustomPairModal: () -> Unit,
  onSyncInLieuToGhColumn: () -> Unit,
  onClaimUnusedGhCredit: () -> Unit
) {
  val neu = LocalNeumorphicColors.current
  val greenContrast = if (neu.isDark) Color(0xFF4ADE80) else Color(0xFF047857)
  val cyanContrast = if (neu.isDark) Color(0xFF22D3EE) else Color(0xFF0E7490)
  val amberContrast = if (neu.isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
  val redContrast = if (neu.isDark) Color(0xFFF87171) else Color(0xFFB91C1C)

  val (statusAccent, statusShortTitle, statusIcon) = when (item.status) {
    ColAuditStatus.VERIFIED_MATCH -> Triple(greenContrast, "VERIFIED 1:1 MATCH", Icons.Default.CheckCircle)
    ColAuditStatus.DOUBLE_CLAIMED_ALERT -> Triple(redContrast, "DOUBLE-CLAIMED DUTY", Icons.Default.ErrorOutline)
    ColAuditStatus.UNPAIRED_COL -> Triple(redContrast, "UNPAIRED COL LEAVE", Icons.Default.ErrorOutline)
    ColAuditStatus.MISSING_IN_GH_COLUMN -> Triple(amberContrast, "MISSING IN GH LOG", Icons.Default.WarningAmber)
    ColAuditStatus.AVAILABLE_GH_CREDIT -> Triple(greenContrast, "UNCLAIMED GH CREDIT", Icons.Default.EventAvailable)
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .neumorphicExtruded(
        shape = IosSharpCardShape,
        elevation = 5.dp,
        blurRadius = 10.dp,
        accentTint = statusAccent,
        accentTintAlpha = 0.03f,
        borderAlpha = 0.30f,
        neu = neu
      )
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // Top Status Header Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .weight(1f, fill = false)
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
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              imageVector = statusIcon,
              contentDescription = null,
              tint = statusAccent,
              modifier = Modifier.size(14.dp)
            )
            Text(
              text = statusShortTitle,
              fontFamily = JetBrainsMonoFontFamily,
              color = statusAccent,
              fontSize = 10.5.sp,
              lineHeight = 13.sp,
              fontWeight = FontWeight.ExtraBold,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (item.status == ColAuditStatus.AVAILABLE_GH_CREDIT) {
          val parsedDutyDate = runCatching { LocalDate.parse(item.ghWorkedDateIso.trim()) }.getOrNull()
          val expiryDate = parsedDutyDate?.plusMonths(6)
          val daysLeft = expiryDate?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) }
          val (countdownLabel, countdownColor) = when {
            daysLeft == null -> "FY ${item.year}" to neu.textSecondary
            daysLeft < 0 -> "EXPIRED" to redContrast
            daysLeft <= 30 -> "${daysLeft}d LEFT" to amberContrast
            else -> "${daysLeft}d LEFT" to greenContrast
          }
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Box(
              modifier = Modifier
                .neumorphicInset(
                  shape = IosSharpBadgeShape,
                  depth = 2.dp,
                  blurRadius = 4.dp,
                  accentTint = countdownColor,
                  accentTintAlpha = 0.12f,
                  borderColor = countdownColor.copy(alpha = 0.35f),
                  neu = neu
                )
            ) {
              Text(
                text = countdownLabel,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = countdownColor,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
            Text(
              text = "FY ${item.year}",
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = neu.textSecondary,
              maxLines = 1,
              softWrap = false
            )
          }
        } else {
          Text(
            text = "FY ${item.year}",
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = neu.textSecondary,
            maxLines = 1,
            softWrap = false
          )
        }
      }

      if (item.status == ColAuditStatus.AVAILABLE_GH_CREDIT) {
        // ================================================================
        // CRISP LAYOUT FOR UNUSED GH WORKED CREDIT (Zero Truncation)
        // ================================================================
        val parsedDutyDate = runCatching { LocalDate.parse(item.ghWorkedDateIso.trim()) }.getOrNull()
        val dutyDateFriendly = ClinicalDateFormatter.formatFriendlyDate(item.ghWorkedDateIso)
        val dutyDow = parsedDutyDate?.dayOfWeek?.getDisplayName(
          java.time.format.TextStyle.FULL,
          java.util.Locale.ENGLISH
        ) ?: "Govt. Holiday"
        val expiryFriendly = parsedDutyDate?.plusMonths(6)?.let {
          ClinicalDateFormatter.formatFriendlyDate(it.toString())
        } ?: "6 Months"
        val dutySourceNote = item.ghDutyNotes.ifBlank {
          item.auditExplanation
            .substringAfter("(", "")
            .substringBefore(")", "")
            .ifBlank { "Hospital GH Duty" }
        }

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .neumorphicInset(
              shape = IosSharpWellShape,
              depth = 2.5.dp,
              blurRadius = 5.dp,
              accentTint = statusAccent,
              accentTintAlpha = 0.04f,
              borderColor = neu.hairlineDivider,
              neu = neu
            )
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.Top
            ) {
              Column(
                modifier = Modifier.weight(1.15f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
              ) {
                Text(
                  text = "GH DUTY PERFORMED",
                  fontFamily = JetBrainsMonoFontFamily,
                  fontSize = 9.5.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = statusAccent,
                  letterSpacing = 0.4.sp,
                  maxLines = 1,
                  softWrap = false
                )
                Text(
                  text = dutyDateFriendly,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = neu.textPrimary,
                  maxLines = 1,
                  softWrap = false
                )
                Text(
                  text = "$dutyDow • Hospital Duty",
                  fontSize = 11.5.sp,
                  color = neu.textSecondary,
                  fontWeight = FontWeight.Medium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }

              Spacer(modifier = Modifier.width(10.dp))

              Column(
                modifier = Modifier.weight(0.95f),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(3.dp)
              ) {
                Text(
                  text = "AVAIL BEFORE (180D)",
                  fontFamily = JetBrainsMonoFontFamily,
                  fontSize = 9.5.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = cyanContrast,
                  letterSpacing = 0.4.sp,
                  maxLines = 1,
                  softWrap = false
                )
                Text(
                  text = expiryFriendly,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontSize = 14.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = neu.textPrimary,
                  maxLines = 1,
                  softWrap = false
                )
                Text(
                  text = "Balance: 1.0 COL Day",
                  fontSize = 11.sp,
                  color = statusAccent,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  softWrap = false
                )
              }
            }

            HorizontalDivider(color = neu.hairlineDivider)

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(6.dp)
                  .clip(CircleShape)
                  .background(statusAccent)
              )
              Text(
                text = "$dutySourceNote • Ready to claim as 1-day COL off",
                style = MaterialTheme.typography.bodySmall,
                color = neu.textSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
              )
            }
          }
        }

        // Crisp, Zero-Truncation Action Button
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .neumorphicExtruded(
              shape = IosSharpButtonShape,
              elevation = 3.dp,
              blurRadius = 6.dp,
              surfaceColor = cyanContrast,
              neu = neu
            )
            .clickable { onClaimUnusedGhCredit() },
          contentAlignment = Alignment.Center
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 14.dp)
          ) {
            Icon(
              imageVector = Icons.Default.EventAvailable,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Claim COL Leave ($dutyDateFriendly)",
              fontSize = 13.sp,
              fontWeight = FontWeight.ExtraBold,
              color = Color.White,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      } else {
        // ================================================================
        // 2-COLUMN RECONCILIATION LAYOUT (Verified / Unpaired / Duplicate)
        // ================================================================
        val colFriendly = ClinicalDateFormatter.formatFriendlyDate(item.colDateIso)
        val colDow = ClinicalDateFormatter.dayOfWeekShort(item.colDateIso) ?: "COL"
        val ghHeadline = when (item.status) {
          ColAuditStatus.VERIFIED_MATCH -> ClinicalDateFormatter.formatFriendlyDate(item.ghWorkedDateIso)
          ColAuditStatus.DOUBLE_CLAIMED_ALERT -> "Duplicate Duty"
          ColAuditStatus.MISSING_IN_GH_COLUMN -> "Not in GH Log"
          ColAuditStatus.UNPAIRED_COL -> "Not Linked"
          else -> item.ghWorkedDateIso
        }
        val ghSubline = when {
          item.status == ColAuditStatus.VERIFIED_MATCH -> {
            val dow = ClinicalDateFormatter.dayOfWeekShort(item.ghWorkedDateIso) ?: "GH"
            "$dow • Hospital Duty"
          }
          item.inLieuDateIso.isNotBlank() && item.inLieuDateIso != "MISSING" && item.inLieuDateIso != "—" -> {
            "In Lieu: ${ClinicalDateFormatter.formatFriendlyDate(item.inLieuDateIso)}"
          }
          else -> "No duty date cited"
        }

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .neumorphicInset(
              shape = IosSharpWellShape,
              depth = 2.5.dp,
              blurRadius = 5.dp,
              accentTint = statusAccent,
              accentTintAlpha = 0.04f,
              borderColor = neu.hairlineDivider,
              neu = neu
            )
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
              Text(
                text = "COL AVAILED",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 9.5.sp,
                color = cyanContrast,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                softWrap = false
              )
              Text(
                text = colFriendly,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = neu.textPrimary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = "$colDow • Leave Taken",
                fontSize = 11.sp,
                color = neu.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }

            Box(
              modifier = Modifier
                .padding(horizontal = 8.dp)
                .size(28.dp)
                .neumorphicExtruded(
                  shape = CircleShape,
                  elevation = 2.dp,
                  blurRadius = 4.dp,
                  neu = neu
                ),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Link,
                contentDescription = null,
                tint = statusAccent,
                modifier = Modifier.size(14.dp)
              )
            }

            Column(
              modifier = Modifier.weight(1f),
              horizontalAlignment = Alignment.End,
              verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
              Text(
                text = "GH DUTY LINK",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 9.5.sp,
                color = statusAccent,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                softWrap = false
              )
              Text(
                text = ghHeadline,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = if (item.status == ColAuditStatus.VERIFIED_MATCH) greenContrast else statusAccent,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = ghSubline,
                fontSize = 11.sp,
                color = neu.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }

        Text(
          text = item.auditExplanation,
          style = MaterialTheme.typography.bodySmall,
          color = neu.textSecondary,
          fontSize = 12.sp,
          lineHeight = 16.sp
        )

        // Action Buttons for Unaccounted / Missing items
        when (item.status) {
          ColAuditStatus.UNPAIRED_COL,
          ColAuditStatus.DOUBLE_CLAIMED_ALERT -> {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              if (item.suggestedFixGhDateIso.isNotEmpty()) {
                Box(
                  modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .neumorphicExtruded(
                      shape = IosSharpButtonShape,
                      elevation = 3.dp,
                      blurRadius = 6.dp,
                      surfaceColor = neu.primaryAccent,
                      neu = neu
                    )
                    .clickable { onQuickPairSuggested() },
                  contentAlignment = Alignment.Center
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 10.dp)
                  ) {
                    Icon(Icons.Default.AddLink, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "Pair (${ClinicalDateFormatter.formatFriendlyDate(item.suggestedFixGhDateIso)})",
                      fontSize = 11.5.sp,
                      fontWeight = FontWeight.ExtraBold,
                      color = Color.White,
                      maxLines = 1,
                      softWrap = false,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }
              }
              Box(
                modifier = Modifier
                  .height(40.dp)
                  .neumorphicExtruded(
                    shape = IosSharpButtonShape,
                    elevation = 2.5.dp,
                    blurRadius = 5.dp,
                    accentTint = cyanContrast,
                    accentTintAlpha = 0.12f,
                    borderAlpha = 0.35f,
                    neu = neu
                  )
                  .clickable { onOpenCustomPairModal() },
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "Choose GH",
                  color = cyanContrast,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 11.5.sp,
                  maxLines = 1,
                  softWrap = false,
                  modifier = Modifier.padding(horizontal = 14.dp)
                )
              }
            }
          }

          ColAuditStatus.MISSING_IN_GH_COLUMN -> {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              if (item.suggestedFixGhDateIso.isNotEmpty()) {
                Box(
                  modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .neumorphicExtruded(
                      shape = IosSharpButtonShape,
                      elevation = 3.dp,
                      blurRadius = 6.dp,
                      surfaceColor = neu.primaryAccent,
                      neu = neu
                    )
                    .clickable { onQuickPairSuggested() },
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = "Pair (${ClinicalDateFormatter.formatFriendlyDate(item.suggestedFixGhDateIso)})",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 10.dp)
                  )
                }
              }
              Box(
                modifier = Modifier
                  .weight(1f)
                  .height(40.dp)
                  .neumorphicExtruded(
                    shape = IosSharpButtonShape,
                    elevation = 2.5.dp,
                    blurRadius = 5.dp,
                    accentTint = greenContrast,
                    accentTintAlpha = 0.12f,
                    borderAlpha = 0.35f,
                    neu = neu
                  )
                  .clickable { onSyncInLieuToGhColumn() },
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "+ Log ${ClinicalDateFormatter.formatFriendlyDate(item.inLieuDateIso)}",
                  color = greenContrast,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 11.5.sp,
                  maxLines = 1,
                  softWrap = false,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.padding(horizontal = 10.dp)
                )
              }
            }
          }

          ColAuditStatus.VERIFIED_MATCH,
          ColAuditStatus.AVAILABLE_GH_CREDIT -> Unit
        }
      }
    }
  }
}
