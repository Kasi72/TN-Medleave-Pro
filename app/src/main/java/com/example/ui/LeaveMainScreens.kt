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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.InitialLeaveSeedData
import com.example.data.LeaveCategory
import com.example.data.LeaveRecord
import com.example.data.SmartLeaveBridge
import com.example.data.TnHolidayItem
import com.example.domain.BurnDownSlotSuggestion
import com.example.domain.CsvImportRow
import com.example.domain.ExecutiveDigestShareService
import com.example.domain.LedgerEnrichmentStore
import com.example.domain.RecordAttachmentMeta
import com.example.domain.SrReconciliationState
import com.example.domain.SmartDuplicateLeaveGuardEngine
import com.example.domain.TnHolidays2027Catalog
import com.example.ui.theme.BadgeClGreen
import com.example.ui.theme.BadgeColCyan
import com.example.ui.theme.BadgeElNeutral
import com.example.ui.theme.BadgeGhLime
import com.example.ui.theme.BadgeMlOrange
import com.example.ui.theme.BadgeOnDutyBlue
import com.example.ui.theme.BadgePlGold
import com.example.ui.theme.BadgeRhSlate
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StatusAlertRed
import com.example.ui.theme.StatusPendingOrange
import com.example.ui.theme.StatusVerifiedGreen

@Composable
fun OverviewTabScreen(
  state: LeaveUiState.Success,
  onSelectYear: (Long) -> Unit,
  onNavigateToTab: (Int) -> Unit,
  onOpenAddDialog: () -> Unit,
  onApplyBridge: (SmartLeaveBridge) -> Unit,
  onOpenPdfExportModal: () -> Unit = {},
  onQuickLogLeave: (leaveType: String, dateIso: String, notes: String) -> Unit = { _, _, _ -> },
  onOpenHolidayDashboard: () -> Unit = { onNavigateToTab(4) },
  onCommitBurnDownSlots: (List<BurnDownSlotSuggestion>) -> Unit = {},
  onShowStatusBanner: (String) -> Unit = {},
  onLogDualDutyAndCol: (dutyDateIso: String, colDateIso: String, dutyNotes: String) -> Unit = { _, _, _ -> }
) {
  val context = LocalContext.current
  val summary = state.yearSummary
  var activeSubTab by remember { mutableStateOf(0) }

  val overviewSubTabs = remember(summary.year, state.colGhAuditYear.availableUnusedGhCount) {
    listOf(
      SubTabOption(
        index = 0,
        title = "Radial Gauge",
        badge = "${summary.year}",
        accentColor = EmeraldOptimal
      ),
      SubTabOption(
        index = 1,
        title = "Quota Matrix",
        badge = "6 CARDS",
        accentColor = BadgePlGold
      ),
      SubTabOption(
        index = 2,
        title = "EL & LTC Bank",
        badge = "FR / LTC",
        accentColor = BadgeColCyan
      ),
      SubTabOption(
        index = 3,
        title = "OD & Duty Log",
        badge = "OD / COL",
        accentColor = BadgeOnDutyBlue
      ),
      SubTabOption(
        index = 4,
        title = "Clinical Charts",
        badge = "CHARTS",
        accentColor = BadgeClGreen
      ),
      SubTabOption(
        index = 5,
        title = "Widget & Alerts",
        badge = "${state.colGhAuditYear.availableUnusedGhCount} GH",
        accentColor = StatusPendingOrange
      )
    )
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .testTag("overview_tab_list"),
    contentPadding = PaddingValues(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 104.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Executive HOD Identity & Live Credit Hero Card (Enterprise Mesh & Specular Glass)
    item {
      val officerName = remember(state.selectedYear, state.allRecords.size) {
        HodProfileSettingsStore.getOfficerName(context)
      }
      val officerRoleAndDept = remember(state.selectedYear, state.allRecords.size) {
        "${HodProfileSettingsStore.getDesignation(context)} • ${HodProfileSettingsStore.getDepartment(context)}"
      }
      val officerCollege = remember(state.selectedYear, state.allRecords.size) {
        HodProfileSettingsStore.getCollege(context)
      }
      val officerIfhrmsId = remember(state.selectedYear, state.allRecords.size) {
        HodProfileSettingsStore.getIfhrmsId(context)
      }

      EnterpriseMeshHeroContainer(
        cornerRadius = 28.dp,
        primaryGlowColor = Color(0xFF10B981),
        secondaryGlowColor = Color(0xFF38BDF8)
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
              Surface(
                shape = RoundedCornerShape(15.dp),
                color = Color(0xFF34D399).copy(alpha = 0.22f),
                modifier = Modifier
                  .size(46.dp)
                  .border(1.dp, Color(0xFF34D399).copy(alpha = 0.45f), RoundedCornerShape(15.dp))
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.LocalHospital,
                    contentDescription = null,
                    tint = Color(0xFF34D399),
                    modifier = Modifier.size(24.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = officerName,
                  style = MaterialTheme.typography.titleMedium,
                  color = Color.White,
                  fontWeight = FontWeight.ExtraBold,
                  maxLines = 1,
                  softWrap = false,
                  overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                  text = "$officerRoleAndDept • $officerCollege",
                  style = MaterialTheme.typography.bodyMedium,
                  color = Color(0xFFBAE6FD),
                  fontSize = 11.5.sp,
                  lineHeight = 14.sp,
                  maxLines = 1,
                  softWrap = false,
                  overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
              }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color(0xFF10B981).copy(alpha = 0.20f),
              modifier = Modifier
                .height(30.dp)
                .border(1.dp, Color(0xFF34D399).copy(alpha = 0.50f), RoundedCornerShape(12.dp))
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF34D399))
                )
                Text(
                  text = officerIfhrmsId.ifBlank { "TN MES" },
                  fontFamily = JetBrainsMonoFontFamily,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = Color(0xFF6EE7B7),
                  maxLines = 1,
                  softWrap = false
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(18.dp))

          // Quick Remaining Quota Bar (Geometrically Uniform 3-Pill Row, Zero Overlap + Micro Progress)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .height(androidx.compose.foundation.layout.IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            HeroStatPill(
              label = "CL Left (${summary.year})",
              value = "${formatDoubleDays(summary.clRemaining)} / 12",
              sub = "${formatDoubleDays(summary.clTaken)} availed",
              accent = Color(0xFF4ADE80),
              progressFraction = (summary.clRemaining.toFloat() / 12f).coerceIn(0f, 1f),
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
            )
            HeroStatPill(
              label = "RH Left (${summary.year})",
              value = "${summary.rhRemaining} / 3",
              sub = "${summary.rhTaken} availed",
              accent = Color(0xFF38BDF8),
              progressFraction = (summary.rhRemaining.toFloat() / 3f).coerceIn(0f, 1f),
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
            )
            HeroStatPill(
              label = "COL Paired",
              value = "${summary.colTaken} / ${summary.inLieuWorked}",
              sub = "In-Lieu Credits",
              accent = Color(0xFFFBBF24),
              progressFraction = if (summary.inLieuWorked > 0) {
                (summary.colTaken.toFloat() / summary.inLieuWorked.toFloat()).coerceIn(0f, 1f)
              } else {
                1f
              },
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
            )
          }
        }
      }
    }

    // Live Enterprise Governance & Telemetry Strip
    item {
      EnterpriseExecutiveStatusStrip(
        pairedColCount = state.colGhAuditYear.fullyAccountedColCount,
        totalColCount = state.colGhAuditYear.totalColTaken,
        unusedGhCount = state.colGhAuditYear.availableUnusedGhCount,
        onClickAudit = { onNavigateToTab(2) },
        onClickOptimizer = { onNavigateToTab(3) }
      )
    }

    // Year Segmented Control (2023 | 2024 | 2025 | 2026 | 2027)
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        EnterpriseSectionHeader(
          indexTag = "01 • TEMPORAL LEDGER SCOPE",
          title = "Service Leave Year",
          badgeText = "${state.allRecords.count { it.year == state.selectedYear }} RECORDS IN ${state.selectedYear}",
          badgeColor = BadgeColCyan
        )
        IosSegmentedYearSelector(
          years = TnHolidays2027Catalog.supportedServiceYears,
          selectedYear = state.selectedYear,
          onSelectYear = onSelectYear
        )
      }
    }

    // Enterprise Sub-Tab Switcher so Overview never feels long or cluttered
    item {
      EnterpriseSubTabSwitcher(
        tabs = overviewSubTabs,
        selectedIndex = activeSubTab,
        onSelectTab = { activeSubTab = it }
      )
    }

    when (activeSubTab) {
      0 -> {
        // SUB-TAB 0: QUOTA & GAUGES
        // Premium Circular Progress Indicator: Remaining Leave Balance vs. Used Leave Credits
        item {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            EnterpriseSectionHeader(
              indexTag = "01B • RADIAL QUOTA TELEMETRY",
              title = "Leave Balance vs. Used Credits",
              subtitle = "Multi-ring circular gauge comparing remaining statutory reserve against used credits",
              badgeText = "${summary.year} LIVE GAUGE",
              badgeColor = EmeraldOptimal,
              onBadgeClick = { onNavigateToTab(1) }
            )
            EnterpriseLeaveBalanceCircularCard(
              summary = summary,
              colGhAudit = state.colGhAuditYear,
              onNavigateToLedger = { onNavigateToTab(1) },
              onNavigateToAudit = { onNavigateToTab(2) }
            )
          }
        }

        // Wave-2 Upgrade #9: 1-Tap WhatsApp / Clipboard Executive Digest Bar
        item {
          ExecutiveDigestShareBar(
            titleLabel = "1-Tap HOD Executive Leave Digest (WhatsApp / Dean's Office)",
            digestContentProvider = {
              val elPassbook = com.example.domain.ElMasterPassbookEngine.computePassbook(
                allRecords = state.allRecords,
                openingBalanceJan2023 = LedgerEnrichmentStore.getOpeningElBalance2023(context),
                elSurrenderedDays = LedgerEnrichmentStore.getElSurrenderedDays(context)
              )
              ExecutiveDigestShareService.buildExecutiveQuotaDigest(
                summary = summary,
                audit = state.colGhAuditYear,
                elNetBalance = elPassbook.currentNetElBalance
              )
            },
            onShowStatusBanner = onShowStatusBanner
          )
        }
      }

      1 -> {
        // SUB-TAB 1: QUOTA MATRIX & SPOTLIGHT
        // Bento Grid of Leave Categories for Selected Year (Symmetric 2x3 Equal-Height Cards)
        item {
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EnterpriseSectionHeader(
              indexTag = "02 • STATUTORY QUOTA MATRIX",
              title = "${state.selectedYear} Leave & Duty Portfolio",
              badgeText = "TAP CARD TO DRILL DOWN",
              badgeColor = BadgePlGold
            )
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .height(androidx.compose.foundation.layout.IntrinsicSize.Min),
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              SquircleCreditCard(
                title = "Casual Leave (CL)",
                code = "CL",
                mainStat = "${formatDoubleDays(summary.clTaken)} / 12",
                subStat = "${formatDoubleDays(summary.clRemaining)} days left in ${summary.year}",
                accent = BadgeClGreen,
                modifier = Modifier
                  .weight(1f)
                  .fillMaxHeight(),
                onClick = { onNavigateToTab(1) }
              )
              SquircleCreditCard(
                title = "Restricted Holiday",
                code = "RH",
                mainStat = "${summary.rhTaken} / 3",
                subStat = "${summary.rhRemaining} RL credits available",
                accent = BadgeRhSlate,
                modifier = Modifier
                  .weight(1f)
                  .fillMaxHeight(),
                onClick = { onNavigateToTab(4) }
              )
            }

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .height(androidx.compose.foundation.layout.IntrinsicSize.Min),
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              SquircleCreditCard(
                title = "COL ↔ GH Audit",
                code = "COL ↔ GH",
                mainStat = "${state.colGhAuditYear.fullyAccountedColCount}/${state.colGhAuditYear.totalColTaken} Paired",
                subStat = if (state.colGhAuditYear.unaccountedColCount == 0) {
                  "100% paired • ${state.colGhAuditYear.availableUnusedGhCount} GH free"
                } else {
                  "${state.colGhAuditYear.unaccountedColCount} unpaired • ${state.colGhAuditYear.availableUnusedGhCount} GH free"
                },
                accent = BadgeColCyan,
                modifier = Modifier
                  .weight(1f)
                  .fillMaxHeight(),
                onClick = { onNavigateToTab(2) }
              )
              SquircleCreditCard(
                title = "Public Holiday & GH",
                code = "PL / GH",
                mainStat = "${summary.ghWorked} GH • ${summary.plTaken} PL",
                subStat = "Tap to audit COL ↔ GH",
                accent = BadgePlGold,
                modifier = Modifier
                  .weight(1f)
                  .fillMaxHeight(),
                onClick = { onNavigateToTab(2) }
              )
            }

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .height(androidx.compose.foundation.layout.IntrinsicSize.Min),
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              SquircleCreditCard(
                title = "Earned & Medical",
                code = "EL / ML",
                mainStat = "${summary.elTaken} EL • ${summary.mlTaken} ML",
                subStat = "TN Leave Rules, 1933",
                accent = BadgeMlOrange,
                modifier = Modifier
                  .weight(1f)
                  .fillMaxHeight(),
                onClick = { onNavigateToTab(1) }
              )
              SquircleCreditCard(
                title = "On Duty & Week Off",
                code = "OD / WO",
                mainStat = "${summary.onDutyTaken} OD • ${summary.weekOffTaken} WO",
                subStat = "Univ Exam / DME / Call Off",
                accent = BadgeOnDutyBlue,
                modifier = Modifier
                  .weight(1f)
                  .fillMaxHeight(),
                onClick = { onNavigateToTab(1) }
              )
            }
          }
        }

        // Official Medical College Office A4 PDF Export Banner Card
        item {
          OfficialPdfExportBannerCard(
            selectedYear = state.selectedYear,
            onOpenPdfExportModal = onOpenPdfExportModal
          )
        }

        // Top Recommended Optimization Spotlight Card
        state.smartBridges.firstOrNull { it.id == "bridge_dec_mega_year_end_2026" }?.let { megaBridge ->
          item {
            Surface(
              shape = RoundedCornerShape(26.dp),
              color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
              modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, EmeraldOptimal.copy(alpha = 0.4f), RoundedCornerShape(26.dp))
            ) {
              Column(modifier = Modifier.padding(18.dp)) {
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
                      imageVector = Icons.Default.AutoAwesome,
                      contentDescription = null,
                      tint = EmeraldOptimal,
                      modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "OPTIMAL 2026–27 RECOMMENDATION",
                      style = MaterialTheme.typography.labelMedium,
                      color = EmeraldOptimal,
                      maxLines = 1,
                      softWrap = false,
                      overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                  }
                  Spacer(modifier = Modifier.width(8.dp))
                  Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = EmeraldOptimal
                  ) {
                    Text(
                      text = "${megaBridge.totalDaysOff} DAYS OFF",
                      style = MaterialTheme.typography.labelMedium,
                      color = Color.White,
                      maxLines = 1,
                      softWrap = false,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                  }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = megaBridge.title,
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = megaBridge.tnRuleComplianceNote,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                  Button(
                    onClick = { onNavigateToTab(3) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldOptimal),
                    modifier = Modifier.weight(1f)
                  ) {
                    Text(
                      text = "View All Bridges",
                      maxLines = 1,
                      softWrap = false
                    )
                  }
                  Button(
                    onClick = { onApplyBridge(megaBridge) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                      containerColor = MaterialTheme.colorScheme.primary
                    )
                  ) {
                    Text(
                      text = "Apply Plan",
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

      2 -> {
        // SUB-TAB 2: EL & LTC BANK
        // Earned Leave (EL) Master Career Passbook & Half-Yearly Accrual Tracker
        item {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            EnterpriseSectionHeader(
              indexTag = "03 • TN FR CAREER PASSBOOK",
              title = "Earned Leave (EL) Master Bank",
              subtitle = "Automatic +15d Jan 1 / Jul 1 accruals & 240-day retirement encashment progress",
              badgeText = "FR RULES",
              badgeColor = BadgePlGold
            )
            EarnedLeaveMasterPassbookCard(
              allRecords = state.allRecords,
              onShowStatusBanner = onShowStatusBanner
            )
          }
        }

        // Wave-2 Upgrade #6: TN Govt LTC & 10-Day EL Encashment Block Tracker
        item {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            EnterpriseSectionHeader(
              indexTag = "04 • LTC & 10-DAY EL ENCASHMENT",
              title = "TN Govt LTC Block Tracker",
              subtitle = "2024–2027 4-Year Block (Home Town & Anywhere in India + 60d Lifetime EL Cap)",
              badgeText = "BLOCK 24–27",
              badgeColor = EmeraldOptimal
            )
            TnGovtLtcBlockTrackerCard(
              onShowStatusBanner = onShowStatusBanner
            )
          }
        }
      }

      3 -> {
        // SUB-TAB 3: OD & DUTY LOG
        // Wave-2 Upgrade #4: Academic On-Duty (OD) Sub-Quota Tracker
        item {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            EnterpriseSectionHeader(
              indexTag = "05 • TN MES ACADEMIC ON-DUTY",
              title = "On-Duty (OD) Sub-Quota Tracker",
              subtitle = "Univ Exam (15d) • CME / IPS Conf (10d) • DME / NMC (12d) • Court / Med Board (15d)",
              badgeText = "OD QUOTAS",
              badgeColor = BadgeOnDutyBlue
            )
            AcademicOdSubQuotaCard(
              allRecords = state.allRecords,
              selectedYear = state.selectedYear,
              onQuickLogSubOd = { _, dateIso, taggedNotes ->
                onQuickLogLeave("ON_DUTY", dateIso, taggedNotes)
              }
            )
          }
        }

        // Wave-2 Upgrade #5: 1-Tap Holiday/Sunday Call Duty -> Comp-Off Dual Logger
        item {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            EnterpriseSectionHeader(
              indexTag = "06 • 1-TAP DUAL RECONCILIATION",
              title = "Holiday Call Duty → Comp-Off Logger",
              subtitle = "Simultaneously registers GH Worked + In-Lieu + Paired COL with 100% green audit",
              badgeText = "3-COL SYNC",
              badgeColor = BadgeColCyan
            )
            DualHolidayDutyColLoggerCard(
              selectedYear = state.selectedYear,
              onLogDualDutyAndCol = onLogDualDutyAndCol
            )
          }
        }
      }

      4 -> {
        // SUB-TAB 4: CLINICAL CHARTS & RULES
        item {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            EnterpriseSectionHeader(
              indexTag = "02 • CLINICAL TELEMETRY",
              title = "Psychiatry Leave Intelligence",
              badgeText = "LIVE CANVAS CHARTS",
              badgeColor = EmeraldOptimal
            )
            PsychiatryLeaveAnalyticsSection(state = state)
          }
        }

        // Tamil Nadu Medical Officer Leave Rules Quick Reference Card (Tier-3 Collapsible Accordion)
        item {
          ExecutiveCollapsibleAccordionCard(
            icon = Icons.Default.Verified,
            accent = EmeraldOptimal,
            title = "Tamil Nadu Leave Rules (HOD / Medical Service)",
            subtitle = "Statutory quick-reference for CL, RH, 10-day continuous cap, COL & PL",
            badgeText = "FR / TN RULES",
            initiallyExpanded = false
          ) {
            RuleItemBullet("12 Casual Leaves (CL) + 3 Restricted Holidays (RH) per calendar year (lapse on Dec 31).")
            RuleItemBullet("Max 10 Days Continuous Absence: CL + RH + Prefixed/Suffixed/Intervening Govt Holidays cannot exceed 10 days at a stretch.")
            RuleItemBullet("Compensatory Off (COL): Earned for 'In Lieu of' special/weekend turn duty; must be availed within 6 months.")
            RuleItemBullet("Public Holiday Leave (PL): Paired with declared Government Holidays (GH Worked) performed at the hospital.")
          }
        }
      }

      5 -> {
        // SUB-TAB 5: WIDGET & ALERTS
        // Upcoming Government Holidays & Leave Bridge Opportunity Dashboard
        item {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            EnterpriseSectionHeader(
              indexTag = "07 • HOLIDAY & BRIDGE RADAR",
              title = "Upcoming Government Holidays",
              subtitle = "Live status chips highlight 'Leave Bridge' stretch opportunities across GH & RH dates",
              badgeText = "LEAVE BRIDGE VIEW",
              badgeColor = EmeraldOptimal,
              onBadgeClick = onOpenHolidayDashboard
            )
            UpcomingHolidayDashboardSection(
              state = state,
              onQuickLogLeave = onQuickLogLeave,
              onApplyBridge = onApplyBridge,
              onOpenFullHolidayDashboard = onOpenHolidayDashboard
            )
          }
        }

        // 6-Month Comp-Off (COL) Expiry Countdown & FIFO Oldest-Credit Radar
        item {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            EnterpriseSectionHeader(
              indexTag = "08 • STATUTORY 6M COMP-OFF EXPIRY",
              title = "COL Expiry & FIFO Duty Radar",
              subtitle = "Oldest unavailed GH Worked credits ranked first so zero Comp-Offs lapse",
              badgeText = "AUDIT VIEW",
              badgeColor = BadgePlGold,
              onBadgeClick = { onNavigateToTab(2) }
            )
            ColSixMonthExpiryRadarCard(
              unusedGhRecords = state.colGhAuditYear.unusedGhRecords,
              onClaimFifoGhAsCol = { _ -> onNavigateToTab(2) }
            )
          }
        }

        // December CL & RH Lapse Burn-Down Planner ("Zero-Waste Year-End")
        item {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            EnterpriseSectionHeader(
              indexTag = "09 • YEAR-END LAPSE GUARD",
              title = "Dec 31 CL & RH Burn-Down Plan",
              subtitle = "Spaced non-violating Friday/Monday/RH dates before annual quota lapses",
              badgeText = "ZERO WASTE",
              badgeColor = StatusPendingOrange
            )
            DecemberLapseBurnDownCard(
              summary2026 = state.summary2026,
              records2026 = state.allRecords.filter { it.year == 2026L },
              onQuickLogSlot = onQuickLogLeave,
              onCommitAllBurnDownSlots = onCommitBurnDownSlots
            )
          }
        }

        // Native Android Home Screen Widget Preview & 1-Tap Launcher Pin
        item {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            EnterpriseSectionHeader(
              indexTag = "10 • ANDROID LAUNCHER GLANCE",
              title = "Native Home Screen Widget",
              subtitle = "Live 4×2 launcher widget displaying CL, RH, Free GH & next holiday bridge",
              badgeText = "APPWIDGET",
              badgeColor = EmeraldOptimal
            )
            HomeScreenWidgetPreviewCard(
              summary2026 = state.summary2026,
              unusedGhCount = state.colGhAuditYear.availableUnusedGhCount,
              nextHolidayLabel = "★ Next Bridge: Nov 08 Deepavali • Dec 24–27 Christmas • Jan 14–17 Pongal '27",
              onShowStatusBanner = onShowStatusBanner
            )
          }
        }

        // Wave-2 Upgrade #7: Phone Calendar Sync & .ICS Export
        item {
          PhoneCalendarIcsExportCard(
            allRecords = state.allRecords,
            onShowStatusBanner = onShowStatusBanner
          )
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(80.dp))
    }
  }
}

@Composable
private fun HeroStatPill(
  label: String,
  value: String,
  sub: String,
  accent: Color,
  modifier: Modifier = Modifier,
  progressFraction: Float = 0.75f
) {
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = Color.White.copy(alpha = 0.09f),
    modifier = modifier.border(1.dp, accent.copy(alpha = 0.42f), RoundedCornerShape(18.dp))
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 11.dp, vertical = 11.dp),
      verticalArrangement = Arrangement.SpaceBetween,
      horizontalAlignment = Alignment.Start
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = label,
            color = Color(0xFFCBD5E1),
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
          )
          Spacer(modifier = Modifier.width(4.dp))
          MiniCircularQuotaRing(
            remainingFraction = progressFraction,
            accentColor = accent,
            trackColor = Color.White.copy(alpha = 0.16f),
            size = 20.dp,
            strokeWidth = 2.6.dp
          )
        }
        Text(
          text = value,
          color = accent,
          fontWeight = FontWeight.Bold,
          fontSize = 16.sp,
          lineHeight = 20.sp,
          fontFamily = JetBrainsMonoFontFamily,
          maxLines = 1,
          softWrap = false
        )
        Text(
          text = sub,
          color = Color.White.copy(alpha = 0.82f),
          fontSize = 10.sp,
          lineHeight = 12.sp,
          maxLines = 1,
          softWrap = false,
          overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Glassmorphic micro-telemetry bar
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(4.dp)
          .clip(RoundedCornerShape(2.dp))
          .background(Color.White.copy(alpha = 0.14f))
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth(progressFraction.coerceIn(0.08f, 1f))
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(accent)
        )
      }
    }
  }
}

@Composable
private fun RuleItemBullet(text: String) {
  Row(
    modifier = Modifier.padding(vertical = 4.dp),
    verticalAlignment = Alignment.Top
  ) {
    Text(
      text = "•",
      color = EmeraldOptimal,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(end = 8.dp)
    )
    Text(
      text = text,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
fun LedgerTabScreen(
  state: LeaveUiState.Success,
  onSelectYear: (Long) -> Unit,
  onSelectCategory: (LeaveCategory?) -> Unit,
  onDeleteRecord: (String) -> Unit,
  onOpenAddDialog: () -> Unit,
  onImportCsvRows: (List<CsvImportRow>) -> Unit = {},
  onShowStatusBanner: (String) -> Unit = {},
  lastDeletedRecord: LeaveRecord? = null,
  onDeleteWithUndo: (LeaveRecord) -> Unit = { onDeleteRecord(it.id) },
  onUndoDelete: () -> Unit = {},
  onDismissUndo: () -> Unit = {},
  onUpdateRecord: (LeaveRecord) -> Unit = {},
  onQuickLogLeave: (leaveType: String, dateIso: String, notes: String) -> Unit = { _, _, _ -> },
  onScrubDuplicates: () -> Unit = {}
) {
  val context = LocalContext.current
  var metaVersion by remember { mutableStateOf(0) }
  var selectedSrFilter by remember { mutableStateOf<SrReconciliationState?>(null) }
  var activeVaultRecord by remember { mutableStateOf<Pair<LeaveRecord, RecordAttachmentMeta>?>(null) }
  var recordBeingEdited by remember { mutableStateOf<LeaveRecord?>(null) }

  // Wave-2 Upgrade #2: Instant Search & Multi-Year Cross-Filter state
  var searchQuery by remember { mutableStateOf("") }
  var searchAllYears by remember { mutableStateOf(false) }
  var onlyHalfDays by remember { mutableStateOf(false) }
  var onlyPairedOrRef by remember { mutableStateOf(false) }

  // Sub-Tab Switcher state
  var activeSubTab by remember { mutableStateOf(0) }

  val allMetaMap = remember(metaVersion, state.allRecords) {
    LedgerEnrichmentStore.readAllMeta(context)
  }

  val displayedRecords = remember(
    state.allRecords,
    state.selectedYear,
    state.selectedCategory,
    selectedSrFilter,
    allMetaMap,
    searchQuery,
    searchAllYears,
    onlyHalfDays,
    onlyPairedOrRef
  ) {
    val basePool = if (searchAllYears) {
      state.allRecords.filter { rec ->
        state.selectedCategory == null || rec.category == state.selectedCategory
      }
    } else {
      state.filteredRecords
    }

    val q = searchQuery.trim().lowercase()
    basePool.filter { rec ->
      val meta = LedgerEnrichmentStore.getMetaForRecord(rec, allMetaMap)
      val srMatch = selectedSrFilter == null || meta.srState == selectedSrFilter
      val halfMatch = !onlyHalfDays || rec.isHalfDay || rec.notes.contains("[FN]") || rec.notes.contains("[AN]")
      val pairedMatch = !onlyPairedOrRef || rec.pairedDateIso.isNotBlank() || meta.orderRefNo.isNotBlank()
      val queryMatch = if (q.isEmpty()) {
        true
      } else {
        rec.dateIso.lowercase().contains(q) ||
          rec.leaveType.lowercase().contains(q) ||
          rec.category.fullTitle.lowercase().contains(q) ||
          rec.notes.lowercase().contains(q) ||
          rec.pairedDateIso.lowercase().contains(q) ||
          meta.orderRefNo.lowercase().contains(q) ||
          meta.srState.shortBadge.lowercase().contains(q)
      }
      srMatch && halfMatch && pairedMatch && queryMatch
    }.sortedByDescending { it.dateIso }
  }

  val ledgerSubTabs = remember(displayedRecords.size, state.allRecords.size) {
    listOf(
      SubTabOption(
        index = 0,
        title = "Leave Entries",
        badge = "${displayedRecords.size}",
        accentColor = EmeraldOptimal
      ),
      SubTabOption(
        index = 1,
        title = "IFHRMS SR Vault",
        badge = "VAULT",
        accentColor = BadgePlGold
      ),
      SubTabOption(
        index = 2,
        title = "Academic OD",
        badge = "OD QUOTA",
        accentColor = BadgeOnDutyBlue
      ),
      SubTabOption(
        index = 3,
        title = "CSV & .ICS Sync",
        badge = "SYNC",
        accentColor = BadgeColCyan
      )
    )
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .testTag("ledger_tab_list"),
    contentPadding = PaddingValues(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 104.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // 1. Year Selector (2023 .. 2027)
    item {
      IosSegmentedYearSelector(
        years = TnHolidays2027Catalog.supportedServiceYears,
        selectedYear = state.selectedYear,
        onSelectYear = onSelectYear
      )
    }

    // 2. Wave-2 Upgrade #1: 5-Second Undo Delete Banner when an entry was just deleted
    if (lastDeletedRecord != null) {
      item {
        UndoDeleteBannerCard(
          deletedRecord = lastDeletedRecord,
          onUndoDelete = onUndoDelete,
          onDismissUndo = onDismissUndo
        )
      }
    }

    // 3. Enterprise Sub-Tab Switcher
    item {
      EnterpriseSubTabSwitcher(
        tabs = ledgerSubTabs,
        selectedIndex = activeSubTab,
        onSelectTab = { activeSubTab = it }
      )
    }

    when (activeSubTab) {
      0 -> {
        // SUB-TAB 0: SEARCH & ENTRIES
        // Live Smart Duplicate & Conflict Shield Banner
        item {
          val duplicateGroups = remember(state.allRecords) {
            SmartDuplicateLeaveGuardEngine.scanLedgerForExistingDuplicates(state.allRecords)
          }
          val totalRedundantEntries = duplicateGroups.size
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
              containerColor = if (totalRedundantEntries == 0) {
                EmeraldOptimal.copy(alpha = 0.08f)
              } else {
                StatusAlertRed.copy(alpha = 0.12f)
              }
            ),
            modifier = Modifier
              .fillMaxWidth()
              .border(
                1.dp,
                if (totalRedundantEntries == 0) EmeraldOptimal.copy(alpha = 0.3f) else StatusAlertRed.copy(alpha = 0.45f),
                RoundedCornerShape(16.dp)
              )
              .testTag("smart_duplicate_shield_banner")
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
              ) {
                Icon(
                  imageVector = if (totalRedundantEntries == 0) Icons.Default.Verified else Icons.Default.Info,
                  contentDescription = "Duplicate Guard Shield",
                  tint = if (totalRedundantEntries == 0) EmeraldOptimal else StatusAlertRed,
                  modifier = Modifier.size(18.dp)
                )
                Column {
                  Text(
                    text = if (totalRedundantEntries == 0) {
                      "7-LAYER DUPLICATE & CONFLICT GUARD ACTIVE"
                    } else {
                      "$totalRedundantEntries DUPLICATE / CONFLICTING ENTRIES DETECTED"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (totalRedundantEntries == 0) EmeraldOptimal else StatusAlertRed,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = if (totalRedundantEntries == 0) {
                      "Zero duplicate dates, cross-category collisions, or double-spent COL duty dates."
                    } else {
                      "Tap 'Scrub Duplicates' to auto-remove redundant clones while preserving canonical records."
                    },
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              Spacer(modifier = Modifier.width(8.dp))

              Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (totalRedundantEntries == 0) {
                  EmeraldOptimal.copy(alpha = 0.16f)
                } else {
                  StatusAlertRed
                },
                modifier = Modifier
                  .clip(RoundedCornerShape(10.dp))
                  .clickable { onScrubDuplicates() }
                  .testTag("scrub_duplicates_button")
              ) {
                Text(
                  text = if (totalRedundantEntries == 0) "Audit Clean" else "Scrub ($totalRedundantEntries)",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (totalRedundantEntries == 0) EmeraldOptimal else Color.White,
                  maxLines = 1,
                  softWrap = false,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
              }
            }
          }
        }

        // Wave-2 Upgrade #2: Global Instant Search & Multi-Year Cross-Filter Bar
        item {
          GlobalLedgerSearchAndFilterCard(
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            searchAllYears = searchAllYears,
            onToggleSearchAllYears = { searchAllYears = !searchAllYears },
            onlyHalfDays = onlyHalfDays,
            onToggleOnlyHalfDays = { onlyHalfDays = !onlyHalfDays },
            onlyPairedOrRef = onlyPairedOrRef,
            onToggleOnlyPairedOrRef = { onlyPairedOrRef = !onlyPairedOrRef },
            resultCount = displayedRecords.size
          )
        }

        // Horizontal Squircle Category Filter Chips matching Spreadsheet columns (Uniform 38.dp Height)
        item {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            val allSelected = state.selectedCategory == null
            val scopeCount = if (searchAllYears) {
              state.allRecords.size
            } else {
              state.allRecords.count { it.year == state.selectedYear }
            }
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = if (allSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
              modifier = Modifier
                .height(38.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable { onSelectCategory(null) }
                .testTag("filter_chip_all")
            ) {
              Box(
                modifier = Modifier.padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "All ($scopeCount)",
                  color = if (allSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 13.sp,
                  maxLines = 1,
                  softWrap = false
                )
              }
            }

            LeaveCategory.entries.forEach { cat ->
              val count = if (searchAllYears) {
                state.allRecords.count { it.category == cat }
              } else {
                state.allRecords.count { it.year == state.selectedYear && it.category == cat }
              }
              val isSelected = state.selectedCategory == cat
              val accent = cat.accentColor()
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) accent else accent.copy(alpha = 0.12f),
                modifier = Modifier
                  .height(38.dp)
                  .clip(RoundedCornerShape(14.dp))
                  .clickable { onSelectCategory(cat) }
                  .testTag("filter_chip_${cat.code}")
              ) {
                Box(
                  modifier = Modifier.padding(horizontal = 14.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = "${cat.shortLabel} ($count)",
                    color = if (isSelected) Color.White else accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    softWrap = false
                  )
                }
              }
            }
          }
        }

        // Category Rule Context Banner when a specific column is selected
        state.selectedCategory?.let { cat ->
          item {
            Surface(
              shape = RoundedCornerShape(18.dp),
              color = cat.accentColor().copy(alpha = 0.1f),
              modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, cat.accentColor().copy(alpha = 0.3f), RoundedCornerShape(18.dp))
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.Info,
                  contentDescription = null,
                  tint = cat.accentColor(),
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(
                    text = "${cat.fullTitle} (${if (searchAllYears) "2023–2027" else state.selectedYear.toString()})",
                    style = MaterialTheme.typography.titleMedium,
                    color = cat.accentColor(),
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = cat.tnRuleSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }
        }

        if (displayedRecords.isEmpty()) {
          item {
            Surface(
              shape = RoundedCornerShape(24.dp),
              color = MaterialTheme.colorScheme.surface,
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Icon(
                  imageVector = Icons.Default.EventAvailable,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                  text = "No entries matching current filter",
                  style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                  onClick = onOpenAddDialog,
                  shape = RoundedCornerShape(14.dp)
                ) {
                  Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Log Leave / Duty")
                }
              }
            }
          }
        } else {
          items(displayedRecords, key = { it.id }) { record ->
            val recordMeta = LedgerEnrichmentStore.getMetaForRecord(record, allMetaMap)
            LeaveRecordSquircleRow(
              record = record,
              onDelete = { _ -> onDeleteWithUndo(record) },
              meta = recordMeta,
              onToggleSrState = { updatedMeta ->
                LedgerEnrichmentStore.saveMetaForRecord(context, updatedMeta)
                metaVersion++
                onShowStatusBanner("Updated ${record.dateIso} (${record.leaveType}) → ${updatedMeta.srState.shortBadge}")
              },
              onOpenCertificateVault = { rec, meta ->
                activeVaultRecord = rec to meta
              },
              onEditRecord = { rec ->
                recordBeingEdited = rec
              }
            )
          }
        }
      }

      1 -> {
        // SUB-TAB 1: SR RECONCILIATION & ACADEMIC OD SUB-QUOTAS
        item {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            val allSrSelected = selectedSrFilter == null
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (allSrSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
              modifier = Modifier
                .height(32.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { selectedSrFilter = null }
                .testTag("sr_filter_all")
            ) {
              Box(
                modifier = Modifier.padding(horizontal = 11.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "All SR Status",
                  fontFamily = JetBrainsMonoFontFamily,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (allSrSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  softWrap = false
                )
              }
            }

            SrReconciliationState.entries.forEach { srState ->
              val isSel = selectedSrFilter == srState
              val srColor = when (srState) {
                SrReconciliationState.SR_VERIFIED -> StatusVerifiedGreen
                SrReconciliationState.SUBMITTED_OFFICE -> BadgeColCyan
                SrReconciliationState.PENDING_SUBMISSION -> StatusPendingOrange
              }
              val count = state.filteredRecords.count {
                LedgerEnrichmentStore.getMetaForRecord(it, allMetaMap).srState == srState
              }
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSel) srColor.copy(alpha = 0.20f) else srColor.copy(alpha = 0.08f),
                modifier = Modifier
                  .height(32.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .border(
                    width = 1.dp,
                    color = if (isSel) srColor else srColor.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(12.dp)
                  )
                  .clickable {
                    selectedSrFilter = if (isSel) null else srState
                  }
                  .testTag("sr_filter_${srState.code}")
              ) {
                Box(
                  modifier = Modifier.padding(horizontal = 11.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = "${srState.shortBadge} ($count)",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = srColor,
                    maxLines = 1,
                    softWrap = false
                  )
                }
              }
            }
          }
        }

        items(displayedRecords, key = { "sr_${it.id}" }) { record ->
          val recordMeta = LedgerEnrichmentStore.getMetaForRecord(record, allMetaMap)
          LeaveRecordSquircleRow(
            record = record,
            onDelete = { _ -> onDeleteWithUndo(record) },
            meta = recordMeta,
            onToggleSrState = { updatedMeta ->
              LedgerEnrichmentStore.saveMetaForRecord(context, updatedMeta)
              metaVersion++
              onShowStatusBanner("Updated ${record.dateIso} (${record.leaveType}) → ${updatedMeta.srState.shortBadge}")
            },
            onOpenCertificateVault = { rec, meta ->
              activeVaultRecord = rec to meta
            },
            onEditRecord = { rec ->
              recordBeingEdited = rec
            }
          )
        }
      }

      2 -> {
        // SUB-TAB 2: ACADEMIC ON-DUTY (OD) SUB-QUOTA TRACKER
        item {
          AcademicOdSubQuotaCard(
            allRecords = state.allRecords,
            selectedYear = state.selectedYear,
            onQuickLogSubOd = { _, dateIso, taggedNotes ->
              onQuickLogLeave("ON_DUTY", dateIso, taggedNotes)
            }
          )
        }
      }

      3 -> {
        // SUB-TAB 3: EXCEL CSV & .ICS CALENDAR SYNC
        item {
          CsvSpreadsheetHubCard(
            allRecords = state.allRecords,
            metaMap = allMetaMap,
            onImportCsvRows = onImportCsvRows,
            onShowStatusBanner = onShowStatusBanner
          )
        }

        item {
          PhoneCalendarIcsExportCard(
            allRecords = state.allRecords,
            onShowStatusBanner = onShowStatusBanner
          )
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(84.dp))
    }
  }

  activeVaultRecord?.let { (rec, meta) ->
    CertificateVaultAndSrDialog(
      record = rec,
      initialMeta = meta,
      onDismiss = { activeVaultRecord = null },
      onSaveMeta = { saved ->
        LedgerEnrichmentStore.saveMetaForRecord(context, saved)
        metaVersion++
        activeVaultRecord = null
        onShowStatusBanner("Saved Certificate Vault & IFHRMS SR Status for ${rec.dateIso}!")
      }
    )
  }

  recordBeingEdited?.let { recToEdit ->
    EditLeaveRecordDialog(
      record = recToEdit,
      onDismiss = { recordBeingEdited = null },
      onSaveEditedRecord = { updated ->
        onUpdateRecord(updated)
        recordBeingEdited = null
      }
    )
  }
}

@Composable
fun OptimizerTabScreen(
  state: LeaveUiState.Success,
  aiRecommendationState: com.example.domain.AiRecommendationUiState,
  onGenerateAiRecommendations: (String) -> Unit,
  onRunCustomRange: (String, String) -> Unit,
  onApplyBridge: (SmartLeaveBridge) -> Unit,
  onOpenNotificationCenter: () -> Unit = {},
  onShowStatusBanner: (String) -> Unit = {},
  onCommitDraftLeaves: (Map<String, LeaveCategory>) -> Unit = {},
  holidaySyncStatus: com.example.domain.HolidaySyncStatus? = null,
  onTriggerHolidaySync: (String) -> Unit = {},
  onAddLocalHolidayToRoom: (TnHolidayItem) -> Unit = {},
  onQuickLogLeave: (leaveType: String, dateIso: String, notes: String) -> Unit = { _, _, _ -> },
  onCommitBurnDownSlots: (List<BurnDownSlotSuggestion>) -> Unit = {},
  onCommitSimulatedWorkingDays: (category: LeaveCategory, workingDateIsos: List<String>, notes: String) -> Unit = { _, _, _ -> }
) {
  var startInput by remember { mutableStateOf("2026-12-24") }
  var endInput by remember { mutableStateOf("2027-01-01") }
  var activeSubTab by remember { mutableStateOf(0) }

  val optimizerSubTabs = remember(state.smartBridges.size) {
    listOf(
      SubTabOption(
        index = 0,
        title = "AI Optimizer",
        badge = "GEMINI",
        accentColor = EmeraldOptimal
      ),
      SubTabOption(
        index = 1,
        title = "M3 Calendar",
        badge = "RANGE",
        accentColor = BadgeColCyan
      ),
      SubTabOption(
        index = 2,
        title = "2026–27 Bridges",
        badge = "${state.smartBridges.size}",
        accentColor = BadgePlGold
      ),
      SubTabOption(
        index = 3,
        title = "What-If & Range",
        badge = "SIM",
        accentColor = BadgeColCyan
      ),
      SubTabOption(
        index = 4,
        title = "Leave Builder",
        badge = "DRAG/TAP",
        accentColor = BadgeOnDutyBlue
      ),
      SubTabOption(
        index = 5,
        title = "Dec Burn & Rules",
        badge = "FR 1933",
        accentColor = StatusPendingOrange
      )
    )
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .testTag("optimizer_tab_list"),
    contentPadding = PaddingValues(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 104.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Enterprise Sub-Tab Switcher so Optimizer is crisp and organized
    item {
      EnterpriseSubTabSwitcher(
        tabs = optimizerSubTabs,
        selectedIndex = activeSubTab,
        onSelectTab = { activeSubTab = it }
      )
    }

    when (activeSubTab) {
      0 -> {
        // Quick-Launch Squircle Banner to Open the Material 3 Visual DateRangePicker
        item {
          Surface(
            shape = RoundedCornerShape(18.dp),
            color = com.example.ui.theme.IosDarkDockSurface,
            tonalElevation = 3.dp,
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(18.dp))
              .border(
                1.dp,
                com.example.ui.theme.NeonCyanText.copy(alpha = 0.42f),
                RoundedCornerShape(18.dp)
              )
              .clickable { activeSubTab = 1 }
              .testTag("open_m3_calendar_banner_btn")
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
              ) {
                Icon(
                  imageVector = Icons.Default.CalendarMonth,
                  contentDescription = null,
                  tint = com.example.ui.theme.NeonAquaAccent,
                  modifier = Modifier.size(20.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "VISUAL MATERIAL 3 HOLIDAY & BRIDGE RANGE PICKER",
                    style = MaterialTheme.typography.labelSmall,
                    color = com.example.ui.theme.NeonAquaAccent,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = "Tap to highlight upcoming TN Govt Holidays & Long-Weekend Windows",
                    style = MaterialTheme.typography.bodySmall,
                    color = com.example.ui.theme.NeonCyanBright,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = com.example.ui.theme.NeonAquaAccent.copy(alpha = 0.18f),
                modifier = Modifier.padding(start = 8.dp)
              ) {
                Text(
                  text = "OPEN M3 →",
                  fontFamily = JetBrainsMonoFontFamily,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = com.example.ui.theme.NeonAquaAccent,
                  maxLines = 1,
                  softWrap = false,
                  modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                )
              }
            }
          }
        }

        // SUB-TAB 0: AI LEAVE OPTIMIZER ENGINE
        item {
          AiLeaveRecommendationSection(
            state = state,
            aiState = aiRecommendationState,
            onGenerateRecommendations = onGenerateAiRecommendations,
            onApplyBridge = onApplyBridge
          )
        }
      }

      1 -> {
        // SUB-TAB 1: VISUAL MATERIAL 3 HOLIDAY & LONG-WEEKEND DATERANGEPICKER
        item {
          OptimizerMaterial3CalendarSection(
            state = state,
            onRunCustomRange = onRunCustomRange,
            onApplyBridge = onApplyBridge,
            onCommitWorkingDaysBatch = onCommitSimulatedWorkingDays,
            onShowStatusBanner = onShowStatusBanner
          )
        }
      }

      2 -> {
        // SUB-TAB 2: CURATED 2026 & 2027 BRIDGES + HOLIDAY DB SYNC
        if (holidaySyncStatus != null) {
          item {
            HolidaySyncStatusCard(
              syncStatus = holidaySyncStatus,
              onTriggerSyncNow = onTriggerHolidaySync,
              onAddLocalHolidayToRoom = onAddLocalHolidayToRoom
            )
          }
        }

        item {
          Text(
            text = "High-Multiplier TN Holiday + RH Bridges (2026 & 2027)",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
          )
        }

        items(state.smartBridges, key = { it.id }) { bridge ->
          SmartBridgeCard(
            bridge = bridge,
            onApply = { onApplyBridge(bridge) }
          )
        }
      }

      3 -> {
        // SUB-TAB 3: WHAT-IF SIMULATOR & RANGE CALCULATOR
        item {
          WhatIfLeaveSimulatorCard(
            summary2026 = state.summary2026,
            unusedGhCredits = state.colGhAuditYear.availableUnusedGhCount,
            onCommitSimulatedWorkingDays = onCommitSimulatedWorkingDays
          )
        }

        // Interactive Tactical Range Calculator
        item {
          Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(18.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Interactive Leave Stretch Calculator (TN 10-Day Rule)",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.weight(1f)
                )
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = com.example.ui.theme.IosDarkDockSurface,
                  modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { activeSubTab = 1 }
                ) {
                  Text(
                    text = "M3 Picker",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = com.example.ui.theme.NeonAquaAccent,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Enter any 2026–2027 date range to auto-detect Weekends, TN Govt Holidays (GH), and Restricted Holidays (RH):",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.height(12.dp))
              Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DatePickerOutlinedField(
                  value = startInput,
                  onValueChange = { startInput = it.trim() },
                  label = "Start Date",
                  modifier = Modifier.weight(1f)
                )
                DatePickerOutlinedField(
                  value = endInput,
                  onValueChange = { endInput = it.trim() },
                  label = "End Date",
                  modifier = Modifier.weight(1f)
                )
              }
              Spacer(modifier = Modifier.height(10.dp))
              Button(
                onClick = { onRunCustomRange(startInput, endInput) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("analyze_range_button")
              ) {
                Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Calculate Optimal Credit Mix")
              }

              state.customAnalysis?.let { analysis ->
                Spacer(modifier = Modifier.height(12.dp))
                if (!analysis.isValid) {
                  Text(
                    text = analysis.errorMessage,
                    color = StatusAlertRed,
                    style = MaterialTheme.typography.bodyMedium
                  )
                } else {
                  Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (analysis.exceedsTenDayRule) StatusAlertRed.copy(alpha = 0.1f) else EmeraldOptimal.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                      Text(
                        text = "${analysis.totalDays} Total Days Off • Only ${analysis.creditsNeeded} Leave Credits Required (${analysis.freeDays} Free Weekend/GH Days)",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (analysis.exceedsTenDayRule) StatusAlertRed else EmeraldOptimal
                      )
                      Spacer(modifier = Modifier.height(4.dp))
                      Text(
                        text = analysis.recommendationSummary,
                        style = MaterialTheme.typography.bodyMedium
                      )
                      if (analysis.elComparisonAdvice.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                          text = analysis.elComparisonAdvice,
                          style = MaterialTheme.typography.bodyMedium,
                          color = MaterialTheme.colorScheme.primary,
                          fontSize = 12.sp
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

      4 -> {
        // SUB-TAB 4: INTERACTIVE LEAVE BUILDER
        item {
          InteractiveLeaveBuilderSection(
            state = state,
            onCommitDraftLeaves = onCommitDraftLeaves
          )
        }
      }

      5 -> {
        // SUB-TAB 5: DECEMBER LAPSE BURN-DOWN, SMART ALERTS & TN LEAVE RULES 1933 ENGINE
        item {
          DecemberLapseBurnDownCard(
            summary2026 = state.summary2026,
            records2026 = state.allRecords.filter { it.year == 2026L },
            onQuickLogSlot = onQuickLogLeave,
            onCommitAllBurnDownSlots = onCommitBurnDownSlots
          )
        }

        item {
          SmartHolidayNotificationCard(
            state = state,
            aiState = aiRecommendationState,
            onOpenFullNotificationCenter = onOpenNotificationCenter,
            onApplyBridge = onApplyBridge,
            onShowStatusBanner = onShowStatusBanner
          )
        }

        // Tamil Nadu Leave Rules 1933 & Fundamental Rules Statutory Engine Card (Tier-3 Collapsible Accordion)
        item {
          val serviceAccount = remember(state.allRecords) {
            com.example.domain.TamilNaduLeaveRulesService.computeServiceLeaveAccount(state.allRecords)
          }
          val ruleSpecs = remember {
            com.example.domain.TamilNaduLeaveRulesService.getAllRuleSpecs()
          }

          ExecutiveCollapsibleAccordionCard(
            icon = Icons.Default.Verified,
            accent = EmeraldOptimal,
            title = "TN Leave Rules 1933 & FR Statutory Engine",
            subtitle = "EL (+15d/half-yr), UEL-MC (540d cap), CL, COL & RH statutory citations",
            badgeText = "FR 1933",
            initiallyExpanded = false
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .height(androidx.compose.foundation.layout.IntrinsicSize.Min),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Surface(
                shape = RoundedCornerShape(16.dp),
                color = BadgeElNeutral.copy(alpha = 0.12f),
                modifier = Modifier
                  .weight(1f)
                  .fillMaxHeight()
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                  verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Text(
                    text = "Earned Leave (EL Rule 8)",
                    style = MaterialTheme.typography.labelLarge,
                    lineHeight = 15.sp,
                    color = BadgeElNeutral,
                    maxLines = 1,
                    softWrap = false,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                  )
                  Text(
                    text = "+15d / Half-Yr (Max 240d)",
                    style = MaterialTheme.typography.titleMedium,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                  )
                  Text(
                    text = "${serviceAccount.elTotalTaken2023To2026}d availed • Intervening GH debited",
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                  )
                }
              }

              Surface(
                shape = RoundedCornerShape(16.dp),
                color = BadgeMlOrange.copy(alpha = 0.12f),
                modifier = Modifier
                  .weight(1f)
                  .fillMaxHeight()
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                  verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Text(
                    text = "Medical Leave (UEL-MC)",
                    style = MaterialTheme.typography.labelLarge,
                    lineHeight = 15.sp,
                    color = BadgeMlOrange,
                    maxLines = 1,
                    softWrap = false,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                  )
                  Text(
                    text = "${serviceAccount.mlRemainingCareerQuota} / 540d Left",
                    style = MaterialTheme.typography.titleMedium,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                  )
                  Text(
                    text = "${serviceAccount.mlTotalTaken2023To2026}d availed • >60d Med Board",
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            ruleSpecs.filter {
              it.category in listOf(
                LeaveCategory.EL,
                LeaveCategory.ML,
                LeaveCategory.CL,
                LeaveCategory.COL,
                LeaveCategory.RH
              )
            }.forEach { spec ->
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 5.dp)
                  .clip(RoundedCornerShape(14.dp))
                  .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                  .padding(10.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "${spec.category.shortLabel} — ${spec.category.fullTitle}",
                    style = MaterialTheme.typography.labelLarge,
                    color = spec.category.accentColor(),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                      .weight(1f)
                      .padding(end = 8.dp)
                  )
                  Text(
                    text = "Max Stretch: ${spec.maxContinuousStretchDays}d",
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 11.sp,
                    maxLines = 1,
                    softWrap = false
                  )
                }
                Text(
                  text = spec.statutoryCitation,
                  fontSize = 10.sp,
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = spec.medicalOfficerSpecialClause,
                  style = MaterialTheme.typography.bodyMedium,
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(84.dp))
    }
  }
}

@Composable
private fun SmartBridgeCard(
  bridge: SmartLeaveBridge,
  onApply: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(24.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, EmeraldOptimal.copy(alpha = 0.28f), RoundedCornerShape(24.dp))
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = EmeraldOptimal.copy(alpha = 0.14f),
          modifier = Modifier
            .weight(1f, fill = false)
            .padding(end = 8.dp)
        ) {
          Text(
            text = "${bridge.efficiencyRatio}x MULTIPLIER • ${bridge.totalDaysOff} DAYS OFF",
            style = MaterialTheme.typography.labelMedium,
            color = EmeraldOptimal,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
          )
        }
        Text(
          text = "Uses ${bridge.leaveCreditsRequired} credit(s)",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
          maxLines = 1,
          softWrap = false
        )
      }

      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = bridge.title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = bridge.recommendedMix,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
      )

      Spacer(modifier = Modifier.height(12.dp))

      bridge.dayByDayBreakdown.forEach { step ->
        val badgeColor = when (step.badgeType) {
          "GH" -> BadgeGhLime
          "RH" -> BadgeRhSlate
          "WEEKEND" -> BadgeColCyan
          else -> BadgeClGreen
        }
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = badgeColor.copy(alpha = 0.16f),
            modifier = Modifier.width(86.dp)
          ) {
            Text(
              text = step.badgeType.replace("_", " "),
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = badgeColor,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = step.dayLabel,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(90.dp)
          )
          Text(
            text = step.description,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = bridge.tnRuleComplianceNote,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.sp
      )

      Spacer(modifier = Modifier.height(12.dp))
      Button(
        onClick = onApply,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldOptimal),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("apply_bridge_${bridge.id}")
      ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Auto-Log Recommended Credits to 2026 Ledger")
      }
    }
  }
}

@Composable
fun HolidaysCatalogTabScreen(
  state: LeaveUiState.Success,
  onQuickLogHoliday: (leaveType: String, dateIso: String, notes: String) -> Unit,
  holidaySyncStatus: com.example.domain.HolidaySyncStatus? = null,
  onTriggerHolidaySync: (String) -> Unit = {},
  onAddLocalHolidayToRoom: (TnHolidayItem) -> Unit = {},
  onApplyBridge: (SmartLeaveBridge) -> Unit = {}
) {
  EnterpriseHolidayDashboardScreen(
    state = state,
    onQuickLogHoliday = onQuickLogHoliday,
    onApplyBridge = onApplyBridge,
    holidaySyncStatus = holidaySyncStatus,
    onTriggerHolidaySync = onTriggerHolidaySync,
    onAddLocalHolidayToRoom = onAddLocalHolidayToRoom
  )
}

fun formatDoubleDays(value: Double): String {
  return if (value % 1.0 == 0.0) {
    value.toInt().toString()
  } else {
    String.format("%.1f", value)
  }
}
