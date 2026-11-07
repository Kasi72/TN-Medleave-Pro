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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StatusAlertRed
import com.example.ui.theme.StatusPendingOrange
import com.example.ui.theme.StatusVerifiedGreen

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

      EnterpriseMeshHeroContainer(
        cornerRadius = 28.dp,
        primaryGlowColor = Color(0xFF10B981),
        secondaryGlowColor = Color(0xFF0EA5E9),
        contentPadding = PaddingValues(20.dp)
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
                tint = Color(0xFF34D399),
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "COL ↔ GH WORKED AUDIT",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF34D399),
                maxLines = 1,
                softWrap = false
              )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color.White.copy(alpha = 0.14f),
              modifier = Modifier
                .height(32.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                .clickable { showAllYears = !showAllYears }
                .testTag("toggle_audit_all_years")
            ) {
              Box(
                modifier = Modifier.padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = if (showAllYears) "All Years (23–27)" else "Year: ${state.selectedYear}",
                  style = MaterialTheme.typography.labelMedium,
                  color = Color.White,
                  maxLines = 1,
                  softWrap = false
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // 4 Geometrically Uniform Audit Metrics Pill Boxes
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
              accent = Color(0xFF38BDF8),
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
            )
            AuditStatMiniCard(
              label = "GH Worked",
              value = "${audit.totalGhWorked}",
              sub = "Hospital Duty",
              accent = Color(0xFFA3E635),
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
            )
            AuditStatMiniCard(
              label = "Unpaired",
              value = "${audit.unaccountedColCount}",
              sub = if (audit.unaccountedColCount == 0) "100% Clean" else "Needs Pair",
              accent = if (audit.unaccountedColCount == 0) Color(0xFF4ADE80) else Color(0xFFF87171),
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
            )
            AuditStatMiniCard(
              label = "Unused GH",
              value = "${audit.availableUnusedGhCount}",
              sub = "Ready for COL",
              accent = Color(0xFFFBBF24),
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Live Reconciliation Integrity Telemetry Bar
          Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
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
                color = Color(0xFFBAE6FD),
                maxLines = 1,
                softWrap = false
              )
              Text(
                text = "$matchPct% VERIFIED",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (matchPct == 100) Color(0xFF4ADE80) else Color(0xFFFBBF24),
                maxLines = 1,
                softWrap = false
              )
            }
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.14f))
            ) {
              Box(
                modifier = Modifier
                  .fillMaxWidth(matchRatio.coerceIn(0.08f, 1f))
                  .height(6.dp)
                  .clip(RoundedCornerShape(3.dp))
                  .background(
                    Brush.horizontalGradient(
                      colors = listOf(Color(0xFF38BDF8), Color(0xFF34D399))
                    )
                  )
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // One-Tap Auto-Reconcile Button
          Button(
            onClick = {
              onAutoReconcileYear(if (showAllYears) null else state.selectedYear)
            },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = Color(0xFF10B981),
              contentColor = Color(0xFF042F2E)
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

        // Filter Pills by Audit Status (Uniform 38.dp Height)
        item {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            val allSel = selectedStatusFilter == null
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = if (allSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
              modifier = Modifier
                .height(38.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable { selectedStatusFilter = null }
            ) {
              Box(
                modifier = Modifier.padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "All (${audit.items.size})",
                  color = if (allSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp,
                  maxLines = 1,
                  softWrap = false
                )
              }
            }

            val statusFilters = listOf(
              Triple(ColAuditStatus.VERIFIED_MATCH, "Verified (${audit.fullyAccountedColCount})", StatusVerifiedGreen),
              Triple(ColAuditStatus.UNPAIRED_COL, "Unpaired COL", StatusAlertRed),
              Triple(ColAuditStatus.DOUBLE_CLAIMED_ALERT, "Double-Claimed (${audit.doubleClaimedCount})", StatusAlertRed),
              Triple(ColAuditStatus.MISSING_IN_GH_COLUMN, "Missing in GH Col", StatusPendingOrange),
              Triple(ColAuditStatus.AVAILABLE_GH_CREDIT, "Unused GH (${audit.availableUnusedGhCount})", BadgeGhLime)
            )

            statusFilters.forEach { (st, label, color) ->
              val sel = selectedStatusFilter == st
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (sel) color else color.copy(alpha = 0.13f),
                modifier = Modifier
                  .height(38.dp)
                  .clip(RoundedCornerShape(14.dp))
                  .clickable { selectedStatusFilter = if (sel) null else st }
              ) {
                Box(
                  modifier = Modifier.padding(horizontal = 14.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = label,
                    color = if (sel) Color.White else color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    softWrap = false
                  )
                }
              }
            }
          }
        }

        // Column Header Legend (COL Date <-> In Lieu Date <-> GH Worked Date)
        item {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "COL TAKEN",
                style = MaterialTheme.typography.labelMedium,
                color = BadgeColCyan,
                modifier = Modifier.weight(1f)
              )
              Text(
                text = "IN LIEU CITED",
                style = MaterialTheme.typography.labelMedium,
                color = BadgeInLieuLavender,
                modifier = Modifier.weight(1f)
              )
              Text(
                text = "GH WORKED",
                style = MaterialTheme.typography.labelMedium,
                color = BadgeGhLime,
                modifier = Modifier.weight(1f)
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
      shape = RoundedCornerShape(28.dp),
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
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                .horizontalScroll(rememberScrollState()),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              unusedCandidates.take(6).forEach { gh ->
                val isChosen = customGhDate == gh.dateIso
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = if (isChosen) BadgeGhLime else BadgeGhLime.copy(alpha = 0.14f),
                  modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { customGhDate = gh.dateIso }
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = "${ClinicalDateFormatter.formatFriendlyDate(gh.dateIso)} • ${gh.notes.ifBlank { "GH Duty" }}",
                      color = if (isChosen) Color.White else BadgeGhLime,
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
          shape = RoundedCornerShape(14.dp),
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
    var newColDate by remember { mutableStateOf("${ghRec.year}-11-23") }
    var colNotes by remember { mutableStateOf("COL availed against GH Worked on ${ghRec.dateIso}") }

    AlertDialog(
      onDismissRequest = { claimUnusedGhRecord = null },
      shape = RoundedCornerShape(28.dp),
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
            style = MaterialTheme.typography.bodyMedium
          )
          DatePickerOutlinedField(
            value = newColDate,
            onValueChange = { newColDate = it.trim() },
            label = "COL Leave Date to Avail (YYYY-MM-DD)",
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = colNotes,
            onValueChange = { colNotes = it },
            label = { Text("Notes") },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
          )
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
          shape = RoundedCornerShape(14.dp),
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
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = Color.White.copy(alpha = 0.1f),
    modifier = modifier.border(1.dp, accent.copy(alpha = 0.38f), RoundedCornerShape(18.dp))
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
        color = Color(0xFFCBD5E1),
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
        color = Color.White.copy(alpha = 0.82f),
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
  val (borderColor, badgeBg, statusIcon) = when (item.status) {
    ColAuditStatus.VERIFIED_MATCH -> Triple(StatusVerifiedGreen, StatusVerifiedGreen, Icons.Default.CheckCircle)
    ColAuditStatus.DOUBLE_CLAIMED_ALERT -> Triple(StatusAlertRed, StatusAlertRed, Icons.Default.ErrorOutline)
    ColAuditStatus.UNPAIRED_COL -> Triple(StatusAlertRed, StatusAlertRed, Icons.Default.ErrorOutline)
    ColAuditStatus.MISSING_IN_GH_COLUMN -> Triple(StatusPendingOrange, StatusPendingOrange, Icons.Default.WarningAmber)
    ColAuditStatus.AVAILABLE_GH_CREDIT -> Triple(BadgeGhLime, BadgeGhLime, Icons.Default.EventAvailable)
  }

  Surface(
    shape = RoundedCornerShape(22.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    modifier = Modifier
      .fillMaxWidth()
      .border(1.2.dp, borderColor.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      // Status Badge Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = badgeBg.copy(alpha = 0.14f),
          modifier = Modifier.weight(1f, fill = false)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = statusIcon,
              contentDescription = null,
              tint = badgeBg,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = item.status.label,
              style = MaterialTheme.typography.labelLarge,
              color = badgeBg,
              fontSize = 11.sp,
              lineHeight = 14.sp,
              maxLines = 1,
              softWrap = false,
              overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
          text = "Year ${item.year}",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          softWrap = false
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // 3-Column Visual Alignment: COL Date <-> In Lieu Date <-> GH Worked Date
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
          .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "COL Leave",
            fontSize = 10.sp,
            color = BadgeColCyan,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
          )
          Text(
            text = ClinicalDateFormatter.formatFriendlyDate(item.colDateIso),
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
          )
          ClinicalDateFormatter.dayOfWeekShort(item.colDateIso)?.let { dow ->
            Text(
              text = "$dow • COL Availed",
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 9.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              softWrap = false
            )
          }
        }

        Icon(
          imageVector = Icons.Default.Link,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier
            .padding(horizontal = 4.dp)
            .size(16.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "In Lieu Of",
            fontSize = 10.sp,
            color = BadgeInLieuLavender,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
          )
          Text(
            text = ClinicalDateFormatter.formatFriendlyDate(item.inLieuDateIso),
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
          )
          ClinicalDateFormatter.dayOfWeekShort(item.inLieuDateIso)?.let { dow ->
            Text(
              text = "$dow • Cited Duty",
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 9.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              softWrap = false
            )
          }
        }

        Icon(
          imageVector = Icons.Default.Link,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier
            .padding(horizontal = 4.dp)
            .size(16.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "GH Worked",
            fontSize = 10.sp,
            color = BadgeGhLime,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
          )
          Text(
            text = ClinicalDateFormatter.formatFriendlyDate(item.ghWorkedDateIso),
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = if (item.status == ColAuditStatus.VERIFIED_MATCH || item.status == ColAuditStatus.AVAILABLE_GH_CREDIT) {
              BadgeGhLime
            } else {
              StatusAlertRed
            },
            maxLines = 1,
            softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
          )
          ClinicalDateFormatter.dayOfWeekShort(item.ghWorkedDateIso)?.let { dow ->
            Text(
              text = "$dow • Hospital Duty",
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 9.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              softWrap = false
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = item.auditExplanation,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp
      )

      // Action Buttons for Unaccounted / Missing / Unused GH items
      when (item.status) {
        ColAuditStatus.UNPAIRED_COL,
        ColAuditStatus.DOUBLE_CLAIMED_ALERT -> {
          Spacer(modifier = Modifier.height(10.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (item.suggestedFixGhDateIso.isNotEmpty()) {
              Button(
                onClick = onQuickPairSuggested,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldOptimal),
                modifier = Modifier.weight(1f)
              ) {
                Icon(Icons.Default.AddLink, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Pair w/ GH (${ClinicalDateFormatter.formatFriendlyDate(item.suggestedFixGhDateIso)})",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  softWrap = false
                )
              }
            }
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.primaryContainer,
              modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { onOpenCustomPairModal() }
            ) {
              Text(
                text = "Choose GH",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
              )
            }
          }
        }

        ColAuditStatus.MISSING_IN_GH_COLUMN -> {
          Spacer(modifier = Modifier.height(10.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (item.suggestedFixGhDateIso.isNotEmpty()) {
              Button(
                onClick = onQuickPairSuggested,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldOptimal),
                modifier = Modifier.weight(1f)
              ) {
                Text(
                  text = "Pair w/ GH (${ClinicalDateFormatter.formatFriendlyDate(item.suggestedFixGhDateIso)})",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  softWrap = false
                )
              }
            }
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = BadgeGhLime.copy(alpha = 0.16f),
              modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { onSyncInLieuToGhColumn() }
            ) {
              Text(
                text = "+ Add ${ClinicalDateFormatter.formatFriendlyDate(item.inLieuDateIso)} to GH Col",
                color = BadgeGhLime,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
              )
            }
          }
        }

        ColAuditStatus.AVAILABLE_GH_CREDIT -> {
          Spacer(modifier = Modifier.height(10.dp))
          Button(
            onClick = onClaimUnusedGhCredit,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BadgeColCyan),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.EventAvailable, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Avail New COL Leave Against GH (${ClinicalDateFormatter.formatFriendlyDate(item.ghWorkedDateIso)})",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              softWrap = false
            )
          }
        }

        ColAuditStatus.VERIFIED_MATCH -> {
          // Already 100% accounted
        }
      }
    }
  }
}
