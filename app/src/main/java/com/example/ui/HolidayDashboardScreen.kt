package com.example.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.data.InitialLeaveSeedData
import com.example.data.SmartLeaveBridge
import com.example.data.TnHolidayItem
import com.example.domain.HolidaySyncStatus
import com.example.ui.theme.BadgeClGreen
import com.example.ui.theme.BadgeColCyan
import com.example.ui.theme.BadgeGhLime
import com.example.ui.theme.BadgePlGold
import com.example.ui.theme.BadgeRhSlate
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class HolidayBridgeTier(
  val chipLabel: String,
  val accentColor: Color,
  val isLeaveBridgeOpportunity: Boolean
) {
  MEGA_BRIDGE(
    chipLabel = "★ LEAVE BRIDGE • 4–9 DAY STRETCH",
    accentColor = Color(0xFF10B981),
    isLeaveBridgeOpportunity = true
  ),
  LONG_WEEKEND_BRIDGE(
    chipLabel = "★ LEAVE BRIDGE • LONG WEEKEND",
    accentColor = Color(0xFF0EA5E9),
    isLeaveBridgeOpportunity = true
  ),
  MIDWEEK_BRIDGE(
    chipLabel = "LEAVE BRIDGE • 5-DAY COMBO",
    accentColor = Color(0xFFD97706),
    isLeaveBridgeOpportunity = true
  ),
  DUTY_COL_OPPORTUNITY(
    chipLabel = "WEEKEND GH • +1 COL CREDIT",
    accentColor = Color(0xFF84CC16),
    isLeaveBridgeOpportunity = false
  )
}

data class EnrichedHolidayDashboardItem(
  val item: TnHolidayItem,
  val parsedDate: LocalDate,
  val monthShort: String,
  val dayNumber: String,
  val weekdayShort: String,
  val daysFromToday: Long,
  val isUpcoming: Boolean,
  val bridgeTier: HolidayBridgeTier,
  val bridgeOpportunityTitle: String,
  val bridgeTacticalRecipe: String,
  val totalStretchDays: Int,
  val leaveCreditsNeeded: Int,
  val existingLedgerType: String?,
  val matchingSmartBridge: SmartLeaveBridge?
)

object HolidayDashboardAnalyzer {

  // Reference date aligned with workspace clock (2026-11-04) so Nov & Dec 2026 holidays highlight as upcoming
  private val referenceToday: LocalDate = LocalDate.of(2026, 11, 4)

  fun buildDashboardItems(
    ghList: List<TnHolidayItem>,
    rhList: List<TnHolidayItem>,
    takenByDate: Map<String, String>,
    smartBridges: List<SmartLeaveBridge>
  ): List<EnrichedHolidayDashboardItem> {
    val fullGh = ghList + com.example.domain.TnHolidays2027Catalog.tnGovtPublicHolidays2027
    val fullRh = rhList + com.example.domain.TnHolidays2027Catalog.tnRestrictedHolidays2027
    val allDatesSet = (fullGh.map { it.dateIso } + fullRh.map { it.dateIso }).toSet()
    val combined = (fullGh + fullRh)
      .distinctBy { "${it.dateIso}_${it.isRestrictedHoliday}_${it.nameEnglish}" }
      .sortedBy { it.dateIso }

    return combined.mapNotNull { item ->
      val date = runCatching {
        LocalDate.parse(item.dateIso, DateTimeFormatter.ISO_LOCAL_DATE)
      }.getOrNull() ?: return@mapNotNull null

      val daysDiff = ChronoUnit.DAYS.between(referenceToday, date)
      val isUpcoming = daysDiff >= 0L
      val monthShort = date.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase()
      val dayNumber = String.format("%02d", date.dayOfMonth)
      val weekdayShort = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase()

      val matchBridge = smartBridges.firstOrNull { bridge ->
        item.dateIso >= bridge.startDateIso && item.dateIso <= bridge.endDateIso
      }

      val prevDayIso = date.minusDays(1).toString()
      val nextDayIso = date.plusDays(1).toString()
      val hasAdjacentHoliday = prevDayIso in allDatesSet || nextDayIso in allDatesSet

      val (tier, oppTitle, recipe, stretchDays, creditsReq) = when {
        matchBridge != null -> {
          val freeWeekendOrGhDays = (matchBridge.totalDaysOff - matchBridge.leaveCreditsRequired).coerceAtLeast(0)
          val friendlySpan = ClinicalDateFormatter.formatRangeFriendly(matchBridge.startDateIso, matchBridge.endDateIso)
          Quintuple(
            HolidayBridgeTier.MEGA_BRIDGE,
            "Leave Bridge Opportunity (${matchBridge.totalDaysOff} Days Off)",
            "${matchBridge.title}: Uses ${matchBridge.leaveCreditsRequired} credit(s) + $freeWeekendOrGhDays weekend/GH days ($friendlySpan).",
            matchBridge.totalDaysOff,
            matchBridge.leaveCreditsRequired
          )
        }
        date.dayOfWeek == DayOfWeek.TUESDAY -> {
          val monIso = date.minusDays(1).format(DateTimeFormatter.ofPattern("MMM d"))
          Quintuple(
            HolidayBridgeTier.MEGA_BRIDGE,
            "Monday Bridge Opportunity (4-Day Stretch)",
            "Take 1 CL/COL on Monday ($monIso) to bridge Sat–Sun weekend with ${item.nameEnglish} for 4 consecutive days off.",
            4,
            1
          )
        }
        date.dayOfWeek == DayOfWeek.THURSDAY -> {
          val friIso = date.plusDays(1).format(DateTimeFormatter.ofPattern("MMM d"))
          Quintuple(
            HolidayBridgeTier.MEGA_BRIDGE,
            "Friday Bridge Opportunity (4-Day Stretch)",
            "Take 1 CL/COL on Friday ($friIso) to bridge ${item.nameEnglish} into the Sat–Sun weekend for 4 consecutive days off.",
            4,
            1
          )
        }
        date.dayOfWeek == DayOfWeek.MONDAY -> {
          val tueIso = date.plusDays(1).format(DateTimeFormatter.ofPattern("MMM d"))
          Quintuple(
            HolidayBridgeTier.LONG_WEEKEND_BRIDGE,
            if (hasAdjacentHoliday) "Clustered Holiday Bridge (4–5 Days)" else "3-Day Long Weekend (+1 CL → 4 Days)",
            if (item.isRestrictedHoliday) {
              "Avail 1 RH credit on Monday to unlock a 3-day weekend (Sat–Mon), or add Tue ($tueIso) CL for 4 days off."
            } else {
              "Automatic 3-day weekend (Sat–Mon) with 0 credits, or bridge Tue ($tueIso) with 1 CL/COL for a 4-day stretch."
            },
            if (item.isRestrictedHoliday) 3 else 4,
            1
          )
        }
        date.dayOfWeek == DayOfWeek.FRIDAY -> {
          val thuIso = date.minusDays(1).format(DateTimeFormatter.ofPattern("MMM d"))
          Quintuple(
            HolidayBridgeTier.LONG_WEEKEND_BRIDGE,
            if (hasAdjacentHoliday) "Clustered Holiday Bridge (4–5 Days)" else "3-Day Long Weekend (+1 CL → 4 Days)",
            if (item.isRestrictedHoliday) {
              "Avail 1 RH credit on Friday to unlock a 3-day weekend (Fri–Sun), or add Thu ($thuIso) CL for 4 days off."
            } else {
              "Automatic 3-day weekend (Fri–Sun) with 0 credits, or bridge Thu ($thuIso) with 1 CL/COL for a 4-day stretch."
            },
            if (item.isRestrictedHoliday) 3 else 4,
            1
          )
        }
        date.dayOfWeek == DayOfWeek.WEDNESDAY -> {
          Quintuple(
            HolidayBridgeTier.MIDWEEK_BRIDGE,
            "Mid-Week 5-Day Bridge Opportunity",
            "Bridge Mon–Tue or Thu–Fri with 2 CL/COL credits around ${item.nameEnglish} to unlock a 5-day break (compliant with TN 10-Day Rule).",
            5,
            2
          )
        }
        else -> {
          // Saturday or Sunday
          if (!item.isRestrictedHoliday && date.plusDays(1).toString() in allDatesSet) {
            Quintuple(
              HolidayBridgeTier.MEGA_BRIDGE,
              "Weekend GH + Monday RL Bridge",
              "Pair ${item.nameEnglish} (${date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)}) with Monday's Restricted Holiday or log GH Worked for +1 COL credit.",
              3,
              1
            )
          } else {
            Quintuple(
              HolidayBridgeTier.DUTY_COL_OPPORTUNITY,
              "Weekend Holiday Duty (+1 COL Credit)",
              "Falls on ${date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)}. Log 'GH Worked' if on hospital turn duty to earn +1 Compensatory Off Leave (COL).",
              2,
              0
            )
          }
        }
      }

      EnrichedHolidayDashboardItem(
        item = item,
        parsedDate = date,
        monthShort = monthShort,
        dayNumber = dayNumber,
        weekdayShort = weekdayShort,
        daysFromToday = daysDiff,
        isUpcoming = isUpcoming,
        bridgeTier = tier,
        bridgeOpportunityTitle = oppTitle,
        bridgeTacticalRecipe = recipe,
        totalStretchDays = stretchDays,
        leaveCreditsNeeded = creditsReq,
        existingLedgerType = takenByDate[item.dateIso],
        matchingSmartBridge = matchBridge
      )
    }
  }

  private data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
  )
}

/**
 * Compact Executive Holiday Dashboard Section for the Overview Tab:
 * Highlights upcoming 2026 Tamil Nadu Government Holidays & Leave Bridge opportunities with
 * clean typography, status chips, and one-tap actions.
 */
@Composable
fun UpcomingHolidayDashboardSection(
  state: LeaveUiState.Success,
  onQuickLogLeave: (leaveType: String, dateIso: String, notes: String) -> Unit,
  onApplyBridge: (SmartLeaveBridge) -> Unit,
  onOpenFullHolidayDashboard: () -> Unit,
  modifier: Modifier = Modifier
) {
  val takenByDate = remember(state.allRecords) {
    state.allRecords.filter { it.year == 2026L }.associate { it.dateIso to it.leaveType }
  }
  val allDashboardItems = remember(state.allRecords, state.smartBridges) {
    HolidayDashboardAnalyzer.buildDashboardItems(
      ghList = InitialLeaveSeedData.tnGovtPublicHolidays2026,
      rhList = InitialLeaveSeedData.tnRestrictedHolidays2026,
      takenByDate = takenByDate,
      smartBridges = state.smartBridges
    )
  }

  val upcomingHighlights = remember(allDashboardItems) {
    val upcoming = allDashboardItems.filter { it.isUpcoming }
    if (upcoming.isNotEmpty()) upcoming.take(5) else allDashboardItems.takeLast(5)
  }
  val totalBridgeOpportunities = remember(allDashboardItems) {
    allDashboardItems.count { it.bridgeTier.isLeaveBridgeOpportunity }
  }
  val upcomingBridgeCount = remember(allDashboardItems) {
    allDashboardItems.count { it.isUpcoming && it.bridgeTier.isLeaveBridgeOpportunity }
  }

  Surface(
    shape = RoundedCornerShape(26.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 3.dp,
    shadowElevation = 4.dp,
    border = BorderStroke(1.2.dp, EmeraldOptimal.copy(alpha = 0.28f)),
    modifier = modifier
      .fillMaxWidth()
      .testTag("upcoming_holiday_dashboard_section")
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Top Summary Row with KPI Pills
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        HolidayDashboardMiniStatPill(
          label = "UPCOMING (2026)",
          value = "${allDashboardItems.count { it.isUpcoming }} Holidays",
          sub = "Nov – Dec 2026 Window",
          accent = BadgeColCyan,
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
        )
        HolidayDashboardMiniStatPill(
          label = "LEAVE BRIDGES",
          value = "$upcomingBridgeCount Active",
          sub = "$totalBridgeOpportunities total in 2026",
          accent = EmeraldOptimal,
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
        )
        HolidayDashboardMiniStatPill(
          label = "CREDITS READY",
          value = "${state.summary2026.clRemaining.toInt()} CL • ${state.summary2026.rhRemaining} RH",
          sub = "Available to bridge",
          accent = BadgePlGold,
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
        )
      }

      // Upcoming Holidays List with Leave Bridge Status Chips
      upcomingHighlights.forEach { enriched ->
        EnterpriseHolidayBridgeCard(
          enriched = enriched,
          onQuickLogHoliday = onQuickLogLeave,
          onApplySmartBridge = onApplyBridge
        )
      }

      // Open Full Holiday Dashboard Button
      Button(
        onClick = onOpenFullHolidayDashboard,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("open_full_holiday_dashboard_button")
      ) {
        Icon(
          imageVector = Icons.Default.CalendarMonth,
          contentDescription = null,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Explore Full 2026 Holiday & Leave Bridge Dashboard (${allDashboardItems.size})",
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp,
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}

/**
 * Full-Screen Enterprise Holiday Dashboard View (`HolidaysCatalogTabScreen` replacement/upgrade):
 * Visualizes upcoming and full-year Tamil Nadu Government Holidays (GH) and Restricted Holidays (RH)
 * with filterable 'Leave Bridge Opportunity' status chips, countdown badges, and one-tap logging.
 */
@Composable
fun EnterpriseHolidayDashboardScreen(
  state: LeaveUiState.Success,
  onQuickLogHoliday: (leaveType: String, dateIso: String, notes: String) -> Unit,
  onApplyBridge: (SmartLeaveBridge) -> Unit = {},
  holidaySyncStatus: HolidaySyncStatus? = null,
  onTriggerHolidaySync: (String) -> Unit = {},
  onAddLocalHolidayToRoom: (TnHolidayItem) -> Unit = {}
) {
  // Filter modes:
  // 0 = Upcoming & Leave Bridges (Default)
  // 1 = Leave Bridge Opportunities Only (All 2026)
  // 2 = TN Public Holidays GH (23)
  // 3 = TN Restricted Holidays RH/RL (31)
  var selectedFilter by rememberSaveable { mutableIntStateOf(0) }

  val takenDates2026 = remember(state.allRecords) {
    state.allRecords.filter { it.year == 2026L }.associate { it.dateIso to it.leaveType }
  }
  val liveRhList = remember(holidaySyncStatus) { InitialLeaveSeedData.tnRestrictedHolidays2026 }
  val liveGhList = remember(holidaySyncStatus) { InitialLeaveSeedData.tnGovtPublicHolidays2026 }

  val allItems = remember(liveGhList, liveRhList, takenDates2026, state.smartBridges) {
    HolidayDashboardAnalyzer.buildDashboardItems(
      ghList = liveGhList,
      rhList = liveRhList,
      takenByDate = takenDates2026,
      smartBridges = state.smartBridges
    )
  }

  val filteredItems = remember(allItems, selectedFilter) {
    when (selectedFilter) {
      0 -> {
        val upcoming = allItems.filter { it.isUpcoming }
        if (upcoming.isNotEmpty()) upcoming else allItems
      }
      1 -> allItems.filter { it.bridgeTier.isLeaveBridgeOpportunity }
      2 -> allItems.filter { !it.item.isRestrictedHoliday }
      else -> allItems.filter { it.item.isRestrictedHoliday }
    }
  }

  val bridgeCount = remember(allItems) {
    allItems.count { it.bridgeTier.isLeaveBridgeOpportunity }
  }
  val upcomingCount = remember(allItems) {
    allItems.count { it.isUpcoming }
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .testTag("holidays_tab_list"),
    contentPadding = PaddingValues(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 104.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // 1. Enterprise Holiday Dashboard Hero Banner
    item {
      EnterpriseMeshHeroContainer(
        cornerRadius = 28.dp,
        primaryGlowColor = Color(0xFF10B981),
        secondaryGlowColor = Color(0xFF0EA5E9),
        contentPadding = PaddingValues(20.dp)
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                Icon(
                  imageVector = Icons.Default.AutoAwesome,
                  contentDescription = null,
                  tint = Color(0xFF34D399),
                  modifier = Modifier.size(16.dp)
                )
                Text(
                  text = "TAMIL NADU G.O. HOLIDAY INTELLIGENCE",
                  fontFamily = JetBrainsMonoFontFamily,
                  fontSize = 10.sp,
                  lineHeight = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF6EE7B7),
                  maxLines = 1,
                  softWrap = false
                )
              }
              Spacer(modifier = Modifier.height(3.dp))
              Text(
                text = "2026 Holiday & Leave Bridge Dashboard",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
              )
            }

            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color.White.copy(alpha = 0.14f),
              border = BorderStroke(1.dp, Color(0xFF34D399).copy(alpha = 0.45f))
            ) {
              Text(
                text = "${liveGhList.size} GH • ${liveRhList.size} RH",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6EE7B7),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
              )
            }
          }

          // 3 Uniform Hero KPI Pills
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            HolidayHeroGlassPill(
              label = "Upcoming",
              value = "$upcomingCount Left",
              sub = "Nov–Dec 2026",
              accent = Color(0xFF38BDF8),
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
            )
            HolidayHeroGlassPill(
              label = "Leave Bridges",
              value = "$bridgeCount Windows",
              sub = "3 to 9 Days Off",
              accent = Color(0xFF4ADE80),
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
            )
            HolidayHeroGlassPill(
              label = "RH Quota Left",
              value = "${state.summary2026.rhRemaining} / 3",
              sub = "${state.summary2026.clRemaining.toInt()} CL Available",
              accent = Color(0xFFFBBF24),
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
            )
          }
        }
      }
    }

    // 2. Background Room DB Sync Card
    if (holidaySyncStatus != null) {
      item {
        HolidaySyncStatusCard(
          syncStatus = holidaySyncStatus,
          onTriggerSyncNow = onTriggerHolidaySync,
          onAddLocalHolidayToRoom = onAddLocalHolidayToRoom
        )
      }
    }

    // 3. Filter Chip Bar (Upcoming | Leave Bridges | Public GH | Restricted RH)
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        HolidayFilterPill(
          label = "Upcoming Next ($upcomingCount)",
          selected = selectedFilter == 0,
          accent = EmeraldOptimal,
          onClick = { selectedFilter = 0 },
          testTag = "filter_upcoming_holidays"
        )
        HolidayFilterPill(
          label = "★ Leave Bridges ($bridgeCount)",
          selected = selectedFilter == 1,
          accent = BadgeColCyan,
          onClick = { selectedFilter = 1 },
          testTag = "filter_bridge_holidays"
        )
        HolidayFilterPill(
          label = "Public GH (${liveGhList.size})",
          selected = selectedFilter == 2,
          accent = BadgeGhLime,
          onClick = { selectedFilter = 2 },
          testTag = "toggle_gh_list"
        )
        HolidayFilterPill(
          label = "Restricted RH (${liveRhList.size})",
          selected = selectedFilter == 3,
          accent = BadgeRhSlate,
          onClick = { selectedFilter = 3 },
          testTag = "toggle_rh_list"
        )
      }
    }

    // 4. Premium Holiday & Leave Bridge Opportunity Cards
    items(
      items = filteredItems,
      key = { "${it.item.isRestrictedHoliday}_${it.item.dateIso}_${it.item.nameEnglish}" }
    ) { enriched ->
      EnterpriseHolidayBridgeCard(
        enriched = enriched,
        onQuickLogHoliday = onQuickLogHoliday,
        onApplySmartBridge = onApplyBridge
      )
    }

    item {
      Spacer(modifier = Modifier.height(84.dp))
    }
  }
}

/**
 * Individual Holiday Dashboard Card featuring:
 * - Clean, premium typography (Plus Jakarta Display + Tamil Subtitle + JetBrains Mono Date Emblem)
 * - Status chips indicating whether the holiday is a 'Leave Bridge' opportunity (`★ LEAVE BRIDGE • 4–9 DAY STRETCH`, `★ LEAVE BRIDGE • LONG WEEKEND`, etc.), Public GH vs Restricted RH, and live countdown
 * - Tactical bridge recipe explanation and one-tap action button
 */
@Composable
fun EnterpriseHolidayBridgeCard(
  enriched: EnrichedHolidayDashboardItem,
  onQuickLogHoliday: (leaveType: String, dateIso: String, notes: String) -> Unit,
  onApplySmartBridge: (SmartLeaveBridge) -> Unit = {}
) {
  val item = enriched.item
  val isBridge = enriched.bridgeTier.isLeaveBridgeOpportunity
  val tierAccent = enriched.bridgeTier.accentColor
  val categoryAccent = if (item.isRestrictedHoliday) BadgeColCyan else BadgePlGold

  Surface(
    shape = RoundedCornerShape(22.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = if (isBridge) 3.dp else 1.dp,
    shadowElevation = if (isBridge) 3.dp else 1.dp,
    border = BorderStroke(
      width = if (isBridge) 1.3.dp else 1.dp,
      brush = Brush.linearGradient(
        colors = if (isBridge) {
          listOf(tierAccent.copy(alpha = 0.55f), categoryAccent.copy(alpha = 0.25f))
        } else {
          listOf(
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
          )
        }
      )
    ),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("holiday_dashboard_card_${item.dateIso}")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          if (isBridge) {
            Brush.horizontalGradient(
              colors = listOf(
                tierAccent.copy(alpha = 0.06f),
                Color.Transparent
              )
            )
          } else {
            Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
          }
        )
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Top Status Chips Row: [Leave Bridge Status Chip] + [GH / RH Type Chip] + [Countdown / Logged Chip]
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // 1. Leave Bridge Opportunity Status Chip
        Surface(
          shape = RoundedCornerShape(9.dp),
          color = tierAccent.copy(alpha = 0.15f),
          border = BorderStroke(1.dp, tierAccent.copy(alpha = 0.40f))
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            if (isBridge) {
              Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = tierAccent,
                modifier = Modifier.size(12.dp)
              )
            }
            Text(
              text = enriched.bridgeTier.chipLabel,
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 10.sp,
              lineHeight = 13.sp,
              fontWeight = FontWeight.Bold,
              color = tierAccent,
              maxLines = 1,
              softWrap = false
            )
          }
        }

        // 2. Public GH vs Restricted RH Chip
        Surface(
          shape = RoundedCornerShape(9.dp),
          color = categoryAccent.copy(alpha = 0.14f)
        ) {
          Text(
            text = if (item.isRestrictedHoliday) "RESTRICTED (RH)" else "GOVT HOLIDAY (GH)",
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 10.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.Bold,
            color = categoryAccent,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }

        // 3. Countdown or Logged Status Chip
        if (enriched.existingLedgerType != null) {
          Surface(
            shape = RoundedCornerShape(9.dp),
            color = EmeraldOptimal.copy(alpha = 0.16f)
          ) {
            Text(
              text = "✓ LOGGED: ${enriched.existingLedgerType}",
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 10.sp,
              lineHeight = 13.sp,
              fontWeight = FontWeight.Bold,
              color = EmeraldOptimal,
              maxLines = 1,
              softWrap = false,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        } else if (enriched.isUpcoming) {
          val countdownLabel = when (enriched.daysFromToday) {
            0L -> "TODAY"
            1L -> "TOMORROW"
            else -> "IN ${enriched.daysFromToday} DAYS"
          }
          Surface(
            shape = RoundedCornerShape(9.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
          ) {
            Text(
              text = countdownLabel,
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 10.sp,
              lineHeight = 13.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              softWrap = false,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }
      }

      // Main Content Row: Date Calendar Emblem + Clean Premium Typography
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Left Monospace Date Squircle Emblem
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = tierAccent.copy(alpha = 0.12f),
          border = BorderStroke(1.dp, tierAccent.copy(alpha = 0.30f)),
          modifier = Modifier.width(66.dp)
        ) {
          Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
          ) {
            Text(
              text = enriched.monthShort,
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 10.sp,
              lineHeight = 12.sp,
              fontWeight = FontWeight.Bold,
              color = tierAccent,
              maxLines = 1,
              softWrap = false
            )
            Text(
              text = enriched.dayNumber,
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 20.sp,
              lineHeight = 23.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              softWrap = false
            )
            Text(
              text = enriched.weekdayShort,
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 10.sp,
              lineHeight = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              softWrap = false
            )
          }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Holiday Title, Tamil Script Name & Bridge Opportunity Headline
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
          Text(
            text = item.nameEnglish,
            style = MaterialTheme.typography.titleMedium,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = "${item.nameTamil} • ${item.dayOfWeek} (${item.monthNameTamil})",
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
          ) {
            Box(
              modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(tierAccent)
            )
            Text(
              text = enriched.bridgeOpportunityTitle,
              style = MaterialTheme.typography.labelLarge,
              fontSize = 12.sp,
              lineHeight = 16.sp,
              fontWeight = FontWeight.Bold,
              color = tierAccent,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }

      // Tactical Bridge Recipe Box + Action Button
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = enriched.bridgeTacticalRecipe,
              style = MaterialTheme.typography.bodyMedium,
              fontSize = 11.sp,
              lineHeight = 15.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Spacer(modifier = Modifier.width(10.dp))

          if (enriched.matchingSmartBridge != null && enriched.existingLedgerType == null) {
            Surface(
              shape = RoundedCornerShape(11.dp),
              color = EmeraldOptimal,
              modifier = Modifier
                .clip(RoundedCornerShape(11.dp))
                .clickable { onApplySmartBridge(enriched.matchingSmartBridge) }
                .testTag("apply_holiday_bridge_${item.dateIso}")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.TrendingUp,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(14.dp)
                )
                Text(
                  text = "Bridge (${enriched.totalStretchDays}d)",
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp,
                  lineHeight = 14.sp,
                  maxLines = 1,
                  softWrap = false
                )
              }
            }
          } else if (enriched.existingLedgerType == null) {
            Surface(
              shape = RoundedCornerShape(11.dp),
              color = categoryAccent.copy(alpha = 0.16f),
              border = BorderStroke(1.dp, categoryAccent.copy(alpha = 0.40f)),
              modifier = Modifier
                .clip(RoundedCornerShape(11.dp))
                .clickable {
                  if (item.isRestrictedHoliday) {
                    onQuickLogHoliday(
                      "RH",
                      item.dateIso,
                      "${item.nameEnglish} (${item.nameTamil}) - RL 2026"
                    )
                  } else {
                    onQuickLogHoliday(
                      "GH_WORKED",
                      item.dateIso,
                      "GH Duty Worked: ${item.nameEnglish} (${item.nameTamil})"
                    )
                  }
                }
                .testTag("quick_log_holiday_${item.dateIso}")
            ) {
              Text(
                text = if (item.isRestrictedHoliday) "+ Log RH" else "+ GH Duty",
                color = categoryAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun HolidayDashboardMiniStatPill(
  label: String,
  value: String,
  sub: String,
  accent: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = accent.copy(alpha = 0.09f),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 10.dp, vertical = 9.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      Text(
        text = label,
        fontFamily = JetBrainsMonoFontFamily,
        fontSize = 9.sp,
        lineHeight = 11.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = value,
        fontFamily = JetBrainsMonoFontFamily,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Bold,
        color = accent,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = sub,
        style = MaterialTheme.typography.bodyMedium,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun HolidayHeroGlassPill(
  label: String,
  value: String,
  sub: String,
  accent: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = Color.White.copy(alpha = 0.09f),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.40f)),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 10.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
      Text(
        text = label,
        color = Color(0xFFCBD5E1),
        fontSize = 10.sp,
        lineHeight = 12.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        softWrap = false
      )
      Text(
        text = value,
        fontFamily = JetBrainsMonoFontFamily,
        color = accent,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        maxLines = 1,
        softWrap = false
      )
      Text(
        text = sub,
        color = Color.White.copy(alpha = 0.80f),
        fontSize = 10.sp,
        lineHeight = 12.sp,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun HolidayFilterPill(
  label: String,
  selected: Boolean,
  accent: Color,
  onClick: () -> Unit,
  testTag: String
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = if (selected) accent else accent.copy(alpha = 0.12f),
    border = BorderStroke(
      width = 1.dp,
      color = if (selected) Color.White.copy(alpha = 0.25f) else accent.copy(alpha = 0.30f)
    ),
    modifier = Modifier
      .height(38.dp)
      .clip(RoundedCornerShape(14.dp))
      .clickable { onClick() }
      .testTag(testTag)
  ) {
    Box(
      modifier = Modifier.padding(horizontal = 14.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = label,
        color = if (selected) Color.White else accent,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 15.sp,
        maxLines = 1,
        softWrap = false
      )
    }
  }
}
