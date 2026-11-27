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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.InitialLeaveSeedData
import com.example.data.LeaveCategory
import com.example.data.LeaveRecord
import com.example.data.TnHolidayItem
import com.example.domain.BuilderPreloadRequest
import com.example.domain.TentativeHoldRecord
import com.example.domain.TnHolidays2027Catalog
import com.example.ui.theme.BadgeClGreen
import com.example.ui.theme.BadgeColCyan
import com.example.ui.theme.BadgePlGold
import com.example.ui.theme.BadgeRhSlate
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.IosSharpBadgeShape
import com.example.ui.theme.IosSharpButtonShape
import com.example.ui.theme.IosSharpCardShape
import com.example.ui.theme.IosSharpHeroShape
import com.example.ui.theme.IosSharpMicroShape
import com.example.ui.theme.IosSharpWellShape
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalNeumorphicColors
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val GoldHolidayHighlight = Color(0xFFF59E0B)
private val TealRhHighlight = Color(0xFF14B8A6)
private val CyanColHighlight = Color(0xFF06B6D4)
private val EmeraldLeaveHighlight = Color(0xFF10B981)
private val TentativeHoldViolet = Color(0xFF6366F1)

@Composable
fun IosCalendarTabScreen(
  state: LeaveUiState.Success,
  onSelectYear: (Long) -> Unit,
  onQuickLogLeave: (leaveType: String, dateIso: String, notes: String) -> Unit,
  onOpenFullAddDialog: () -> Unit,
  onCommitDraftLeaves: (Map<String, LeaveCategory>) -> Unit = {},
  holidaySyncStatus: com.example.domain.HolidaySyncStatus? = null,
  onTriggerHolidaySync: (String) -> Unit = {},
  onAddLocalHolidayToRoom: (TnHolidayItem) -> Unit = {},
  initialSubMode: Int = 0,
  onApplyBridge: (com.example.data.SmartLeaveBridge) -> Unit = {},
  tentativeHolds: Map<String, TentativeHoldRecord> = emptyMap(),
  onPromoteTentativeHold: (TentativeHoldRecord) -> Unit = {},
  onClearTentativeHold: (String) -> Unit = {},
  pendingBuilderPreload: BuilderPreloadRequest? = null,
  onBuilderPreloadConsumed: () -> Unit = {},
  dutyShifts: List<com.example.data.DutyShift> = emptyList(),
  onAddDutyShift: (String, com.example.data.DutyShiftType, String) -> Unit = { _, _, _ -> },
  onAddRecurringWeeklyShift: (Int, Int, java.time.DayOfWeek, com.example.data.DutyShiftType, String) -> Unit = { _, _, _, _, _ -> },
  onDeleteDutyShift: (String) -> Unit = {},
  onClaimGhCol: (com.example.domain.GhColCaptureCandidate) -> Unit = {}
) {
  var currentMonthValue by rememberSaveable(state.selectedYear) {
    mutableIntStateOf(
      when (state.selectedYear) {
        2026L -> 11
        2027L -> 1
        else -> 10
      }
    )
  }
  var selectedDateIso by rememberSaveable(state.selectedYear, currentMonthValue) {
    val defaultDay = when {
      state.selectedYear == 2026L && currentMonthValue == 11 -> "08"
      state.selectedYear == 2027L && currentMonthValue == 1 -> "14"
      else -> "15"
    }
    mutableStateOf(String.format("%04d-%02d-%s", state.selectedYear, currentMonthValue, defaultDay))
  }
  var calendarSubMode by rememberSaveable(initialSubMode) { mutableIntStateOf(initialSubMode) }

  val currentYearMonth = remember(state.selectedYear, currentMonthValue) {
    YearMonth.of(state.selectedYear.toInt(), currentMonthValue.coerceIn(1, 12))
  }

  val allPublicHolidays = remember(holidaySyncStatus) {
    InitialLeaveSeedData.tnGovtPublicHolidays2026 + TnHolidays2027Catalog.tnGovtPublicHolidays2027
  }
  val allRestrictedHolidays = remember(holidaySyncStatus) {
    InitialLeaveSeedData.tnRestrictedHolidays2026 + TnHolidays2027Catalog.tnRestrictedHolidays2027
  }

  var selectedLayerName by rememberSaveable { mutableStateOf(CalendarLayerFilter.ALL.name) }
  val selectedLayer = remember(selectedLayerName) {
    CalendarLayerFilter.entries.firstOrNull { it.name == selectedLayerName } ?: CalendarLayerFilter.ALL
  }

  val recordsByDate: Map<String, List<LeaveRecord>> = remember(state.allRecords) {
    state.allRecords.groupBy { it.dateIso }
  }
  val shiftsByDate: Map<String, List<com.example.data.DutyShift>> = remember(dutyShifts) {
    dutyShifts.groupBy { it.dateIso }
  }
  val ghByDate: Map<String, TnHolidayItem> = remember(allPublicHolidays) {
    allPublicHolidays.associateBy { it.dateIso }
  }
  val rhByDate: Map<String, TnHolidayItem> = remember(allRestrictedHolidays) {
    allRestrictedHolidays.associateBy { it.dateIso }
  }

  val monthPrefix = String.format("%04d-%02d", currentYearMonth.year, currentYearMonth.monthValue)
  val monthDutyCount = remember(dutyShifts, monthPrefix) {
    dutyShifts.count { it.dateIso.startsWith(monthPrefix) }
  }
  val monthRecords = remember(state.allRecords, monthPrefix) {
    state.allRecords.filter { it.dateIso.startsWith(monthPrefix) }
  }
  val monthGhList = remember(monthPrefix, allPublicHolidays) {
    allPublicHolidays.filter { it.dateIso.startsWith(monthPrefix) }
  }
  val monthRhList = remember(monthPrefix, allRestrictedHolidays) {
    allRestrictedHolidays.filter { it.dateIso.startsWith(monthPrefix) }
  }

  val calendarSubTabs = remember(monthGhList.size, monthRhList.size, state.selectedYear) {
    listOf(
      SubTabOption(
        index = 0,
        title = "Month Grid",
        badge = "${state.selectedYear}",
        accentColor = EmeraldOptimal
      ),
      SubTabOption(
        index = 1,
        title = "Month GH & RL",
        badge = "${monthGhList.size + monthRhList.size}",
        accentColor = BadgePlGold
      ),
      SubTabOption(
        index = 2,
        title = "Bridge Dash",
        badge = "2026–27",
        accentColor = BadgeColCyan
      ),
      SubTabOption(
        index = 3,
        title = "Leave Builder",
        badge = "TAP/DRAG",
        accentColor = BadgeRhSlate
      )
    )
  }

  Column(modifier = Modifier.fillMaxSize()) {
    PaddingBox {
      EnterpriseSubTabSwitcher(
        tabs = calendarSubTabs,
        selectedIndex = calendarSubMode,
        onSelectTab = { calendarSubMode = it }
      )
    }

    when (calendarSubMode) {
      2 -> {
        HolidaysCatalogTabScreen(
          state = state,
          onQuickLogHoliday = onQuickLogLeave,
          holidaySyncStatus = holidaySyncStatus,
          onTriggerHolidaySync = onTriggerHolidaySync,
          onAddLocalHolidayToRoom = onAddLocalHolidayToRoom,
          onApplyBridge = onApplyBridge
        )
      }
      3 -> {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .testTag("leave_builder_lazy_list"),
          contentPadding = PaddingValues(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 104.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          item {
            InteractiveLeaveBuilderSection(
              state = state,
              onCommitDraftLeaves = onCommitDraftLeaves,
              pendingPreload = pendingBuilderPreload,
              onPreloadConsumed = onBuilderPreloadConsumed,
              dutyShifts = dutyShifts
            )
          }
        }
      }
      1 -> {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .testTag("month_holidays_lazy_list"),
          contentPadding = PaddingValues(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 104.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              IosSegmentedYearSelector(
                years = TnHolidays2027Catalog.supportedServiceYears,
                selectedYear = state.selectedYear,
                onSelectYear = onSelectYear
              )
              MonthScrubberBar(
                selectedYear = state.selectedYear,
                currentMonthValue = currentMonthValue,
                allRecords = state.allRecords,
                allPublicHolidays = allPublicHolidays,
                onSelectMonth = { mNum ->
                  currentMonthValue = mNum
                  selectedDateIso = String.format("%04d-%02d-01", state.selectedYear, mNum)
                }
              )
            }
          }

          item {
            val neu = LocalNeumorphicColors.current
            val goldText = if (neu.isDark) Color(0xFFFDE68A) else Color(0xFFB45309)
            val tealText = if (neu.isDark) Color(0xFF99F6E4) else Color(0xFF0F766E)
            val emeraldText = if (neu.isDark) Color(0xFF6EE7B7) else Color(0xFF047857)

            Box(
              modifier = Modifier
                .fillMaxWidth()
                .neumorphicExtruded(
                  shape = IosSharpCardShape,
                  elevation = 5.dp,
                  blurRadius = 10.dp,
                  accentTint = GoldHolidayHighlight,
                  borderAlpha = 0.30f,
                  neu = neu
                )
                .padding(18.dp)
            ) {
              Column {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Default.AutoAwesome,
                      contentDescription = null,
                      tint = GoldHolidayHighlight,
                      modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = "${currentYearMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${currentYearMonth.year} Holidays & RL Planner",
                      style = MaterialTheme.typography.titleMedium,
                      color = neu.textPrimary,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val combinedMonthHolidays = (monthGhList + monthRhList).sortedBy { it.dateIso }
                if (combinedMonthHolidays.isEmpty()) {
                  Text(
                    text = "No notified Public or Restricted Holidays in ${currentYearMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${currentYearMonth.year}. Switch to 2026 or 2027 to view synced TN Government GH & RL dates.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = neu.textSecondary
                  )
                } else {
                  combinedMonthHolidays.forEach { hItem ->
                    val alreadyLogged = recordsByDate[hItem.dateIso]?.firstOrNull()
                    val hAccent = if (hItem.isRestrictedHoliday) TealRhHighlight else GoldHolidayHighlight
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .neumorphicInset(
                          shape = IosSharpWellShape,
                          depth = 2.5.dp,
                          blurRadius = 5.dp,
                          accentTint = hAccent,
                          accentTintAlpha = 0.08f,
                          borderColor = hAccent.copy(alpha = 0.35f),
                          neu = neu
                        )
                        .clickable {
                          selectedDateIso = hItem.dateIso
                          calendarSubMode = 0
                        }
                        .padding(12.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Box(
                        modifier = Modifier.neumorphicExtruded(
                          shape = IosSharpMicroShape,
                          elevation = 1.5.dp,
                          blurRadius = 3.dp,
                          surfaceColor = hAccent,
                          neu = neu
                        )
                      ) {
                        Text(
                          text = if (hItem.isRestrictedHoliday) "RL" else "GH",
                          fontSize = 11.sp,
                          fontWeight = FontWeight.ExtraBold,
                          color = Color(0xFF041322),
                          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                      }
                      Spacer(modifier = Modifier.width(10.dp))
                      Column(modifier = Modifier.weight(1f)) {
                        Text(
                          text = "${ClinicalDateFormatter.formatFriendlyWithDay(hItem.dateIso)} • ${hItem.nameTamil}",
                          style = MaterialTheme.typography.labelLarge,
                          color = neu.textPrimary,
                          fontWeight = FontWeight.Bold,
                          maxLines = 1,
                          softWrap = false,
                          overflow = TextOverflow.Ellipsis
                        )
                        Text(
                          text = hItem.nameEnglish,
                          style = MaterialTheme.typography.bodyMedium,
                          fontSize = 12.sp,
                          color = if (hItem.isRestrictedHoliday) tealText else goldText
                        )
                      }
                      if (alreadyLogged != null) {
                        Box(
                          modifier = Modifier
                            .neumorphicInset(
                              shape = IosSharpMicroShape,
                              depth = 1.5.dp,
                              blurRadius = 3.dp,
                              accentTint = EmeraldLeaveHighlight,
                              accentTintAlpha = 0.12f,
                              borderColor = EmeraldLeaveHighlight.copy(alpha = 0.55f),
                              neu = neu
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                          Text(
                            text = alreadyLogged.leaveType,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = emeraldText
                          )
                        }
                      } else {
                        Box(
                          modifier = Modifier
                            .neumorphicExtruded(
                              shape = IosSharpBadgeShape,
                              elevation = 2.dp,
                              blurRadius = 4.dp,
                              accentTint = neu.primaryAccent,
                              accentTintAlpha = 0.08f,
                              borderAlpha = 0.35f,
                              neu = neu
                            )
                            .clickable {
                              if (hItem.isRestrictedHoliday) {
                                onQuickLogLeave("RH", hItem.dateIso, "${hItem.nameEnglish} (${hItem.nameTamil})")
                              } else {
                                onQuickLogLeave("GH_WORKED", hItem.dateIso, "GH Worked: ${hItem.nameEnglish}")
                              }
                            }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                          Text(
                            text = if (hItem.isRestrictedHoliday) "+ Log RH" else "+ GH Duty",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = neu.primaryAccent
                          )
                        }
                      }
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
      else -> {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .testTag("ios_calendar_lazy_list"),
          contentPadding = PaddingValues(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 104.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          // 1. Year Switcher + Luxury Month Scrubber Pill Row
          item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              IosSegmentedYearSelector(
                years = TnHolidays2027Catalog.supportedServiceYears,
                selectedYear = state.selectedYear,
                onSelectYear = onSelectYear
              )

              MonthScrubberBar(
                selectedYear = state.selectedYear,
                currentMonthValue = currentMonthValue,
                allRecords = state.allRecords,
                allPublicHolidays = allPublicHolidays,
                onSelectMonth = { mNum ->
                  currentMonthValue = mNum
                  selectedDateIso = String.format("%04d-%02d-01", state.selectedYear, mNum)
                }
              )

              CalendarLayerFilterStrip(
                selectedLayer = selectedLayer,
                dutyCountInMonth = monthDutyCount,
                onSelectLayer = { selectedLayerName = it.name }
              )
            }
          }

          // 2. Main Neumorphic Bento Squircle Calendar Card
          item {
            val neu = LocalNeumorphicColors.current
            val goldText = if (neu.isDark) Color(0xFFFDE68A) else Color(0xFFB45309)
            val tealText = if (neu.isDark) Color(0xFF99F6E4) else Color(0xFF0F766E)
            val cyanText = if (neu.isDark) Color(0xFF67E8F9) else Color(0xFF0369A1)
            val emeraldText = if (neu.isDark) Color(0xFF6EE7B7) else Color(0xFF047857)
            val sunColor = if (neu.isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
            val satColor = if (neu.isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)

            Box(
              modifier = Modifier
                .fillMaxWidth()
                .neumorphicExtruded(
                  shape = IosSharpHeroShape,
                  elevation = 6.dp,
                  blurRadius = 12.dp,
                  accentTint = neu.primaryAccent,
                  borderAlpha = 0.34f,
                  neu = neu
                )
            ) {
              Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp)) {
                // Calendar Month Header + Neumorphic Tactile Prev/Next Buttons
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
                      Box(
                        modifier = Modifier
                          .size(7.dp)
                          .clip(CircleShape)
                          .background(neu.primaryAccent)
                      )
                      Text(
                        text = "TN MEDLEAVE BENTO CALENDAR",
                        style = MaterialTheme.typography.labelSmall,
                        color = neu.primaryAccent,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                      )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = "${currentYearMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${currentYearMonth.year}",
                      style = MaterialTheme.typography.headlineMedium,
                      fontWeight = FontWeight.ExtraBold,
                      fontSize = 24.sp,
                      color = neu.textPrimary
                    )
                  }

                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    Box(
                      modifier = Modifier
                        .height(38.dp)
                        .neumorphicExtruded(
                          shape = IosSharpButtonShape,
                          elevation = 3.dp,
                          blurRadius = 6.dp,
                          accentTint = neu.primaryAccent,
                          accentTintAlpha = 0.08f,
                          borderAlpha = 0.35f,
                          neu = neu
                        )
                        .clickable {
                          onSelectYear(2026L)
                          currentMonthValue = 11
                          selectedDateIso = "2026-11-08"
                        }
                        .padding(horizontal = 10.dp)
                        .testTag("calendar_today_quick_jump"),
                      contentAlignment = Alignment.Center
                    ) {
                      Text(
                        text = "Today",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = neu.primaryAccent,
                        maxLines = 1,
                        softWrap = false
                      )
                    }

                    Box(
                      modifier = Modifier
                        .size(38.dp)
                        .neumorphicExtruded(
                          shape = IosSharpButtonShape,
                          elevation = 3.dp,
                          blurRadius = 6.dp,
                          borderAlpha = 0.28f,
                          neu = neu
                        ),
                      contentAlignment = Alignment.Center
                    ) {
                      IconButton(
                        onClick = {
                          if (currentMonthValue > 1) {
                            currentMonthValue -= 1
                          } else if (state.selectedYear > 2023L) {
                            onSelectYear(state.selectedYear - 1)
                            currentMonthValue = 12
                          }
                        },
                        modifier = Modifier.testTag("calendar_prev_month")
                      ) {
                        Icon(
                          imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                          contentDescription = "Previous Month",
                          tint = neu.textPrimary
                        )
                      }
                    }

                    Box(
                      modifier = Modifier
                        .size(38.dp)
                        .neumorphicExtruded(
                          shape = IosSharpButtonShape,
                          elevation = 3.dp,
                          blurRadius = 6.dp,
                          borderAlpha = 0.28f,
                          neu = neu
                        ),
                      contentAlignment = Alignment.Center
                    ) {
                      IconButton(
                        onClick = {
                          if (currentMonthValue < 12) {
                            currentMonthValue += 1
                          } else if (state.selectedYear < 2027L) {
                            onSelectYear(state.selectedYear + 1)
                            currentMonthValue = 1
                          }
                        },
                        modifier = Modifier.testTag("calendar_next_month")
                      ) {
                        Icon(
                          imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                          contentDescription = "Next Month",
                          tint = neu.textPrimary
                        )
                      }
                    }
                  }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3 Uniform Monthly Summary Telemetry Inset Wells
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  CalendarHeaderStatChip(
                    label = "${monthRecords.size} Leaves / Duties",
                    accentColor = cyanText,
                    borderColor = CyanColHighlight,
                    modifier = Modifier
                      .weight(1f)
                      .fillMaxHeight()
                  )
                  CalendarHeaderStatChip(
                    label = "${monthGhList.size} TN Public GH",
                    accentColor = goldText,
                    borderColor = GoldHolidayHighlight,
                    modifier = Modifier
                      .weight(1f)
                      .fillMaxHeight()
                  )
                  CalendarHeaderStatChip(
                    label = "${monthRhList.size} Restricted RL",
                    accentColor = tealText,
                    borderColor = TealRhHighlight,
                    modifier = Modifier
                      .weight(1f)
                      .fillMaxHeight()
                  )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Recessed Inset Capsule Weekday Header Bar (SUN .. SAT)
                val weekLabels = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .neumorphicInset(
                      shape = IosSharpButtonShape,
                      depth = 2.5.dp,
                      blurRadius = 5.dp,
                      neu = neu
                    )
                    .padding(vertical = 8.dp)
                ) {
                  weekLabels.forEachIndexed { idx, wLabel ->
                    val headerColor = when (idx) {
                      0 -> sunColor
                      6 -> satColor
                      else -> neu.textSecondary
                    }
                    Text(
                      text = wLabel,
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.ExtraBold,
                      fontSize = 11.sp,
                      letterSpacing = 0.5.sp,
                      color = headerColor,
                      textAlign = TextAlign.Center,
                      modifier = Modifier.weight(1f)
                    )
                  }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 7-Column Architectural Bento Month Grid
                val firstDayOfMonth = currentYearMonth.atDay(1)
                val leadingEmptyCells = firstDayOfMonth.dayOfWeek.value % 7
                val daysInMonth = currentYearMonth.lengthOfMonth()
                val totalCells = ((leadingEmptyCells + daysInMonth + 6) / 7) * 7
                val rows = totalCells / 7

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  for (rowIdx in 0 until rows) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                      for (colIdx in 0 until 7) {
                        val cellIndex = rowIdx * 7 + colIdx
                        val dayNumber = cellIndex - leadingEmptyCells + 1
                        if (dayNumber in 1..daysInMonth) {
                          val dateIso = String.format(
                            "%04d-%02d-%02d",
                            currentYearMonth.year,
                            currentYearMonth.monthValue,
                            dayNumber
                          )
                          val dayRecords = recordsByDate[dateIso].orEmpty()
                          val dayShifts = shiftsByDate[dateIso].orEmpty()
                          val ghItem = ghByDate[dateIso]
                          val rhItem = rhByDate[dateIso]
                          val isSunday = colIdx == 0
                          val isSaturday = colIdx == 6
                          val isSelected = selectedDateIso == dateIso

                          IosCalendarDayCell(
                            dayNumber = dayNumber,
                            dateIso = dateIso,
                            isSelected = isSelected,
                            isSunday = isSunday,
                            isSaturday = isSaturday,
                            dayRecords = dayRecords,
                            dayShifts = dayShifts,
                            layerFilter = selectedLayer,
                            ghItem = ghItem,
                            rhItem = rhItem,
                            tentativeHold = tentativeHolds[dateIso],
                            onClick = { selectedDateIso = dateIso },
                            modifier = Modifier.weight(1f)
                          )
                        } else {
                          // Recessed subtle ghost inset tile so the 7x5 matrix stays geometrically uniform
                          Box(
                            modifier = Modifier
                              .weight(1f)
                              .aspectRatio(0.84f)
                              .neumorphicInset(
                                shape = IosSharpButtonShape,
                                depth = 1.5.dp,
                                blurRadius = 3.dp,
                                neu = neu
                              )
                          )
                        }
                      }
                    }
                  }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2x2 Neumorphic Legend Grid
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    CalendarLegendPill(
                      label = "Leave Taken (CL/EL)",
                      badgeCode = "CL",
                      dotColor = EmeraldLeaveHighlight,
                      textColor = emeraldText,
                      modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                    )
                    CalendarLegendPill(
                      label = "COL / GH Duty",
                      badgeCode = "COL",
                      dotColor = CyanColHighlight,
                      textColor = cyanText,
                      modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                    )
                  }
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    CalendarLegendPill(
                      label = "TN Public Holiday",
                      badgeCode = "GH",
                      dotColor = GoldHolidayHighlight,
                      textColor = goldText,
                      modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                    )
                    CalendarLegendPill(
                      label = "Restricted Holiday",
                      badgeCode = "RL",
                      dotColor = TealRhHighlight,
                      textColor = tealText,
                      modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                    )
                  }
                }
              }
            }
          }

          // 3. Selected Date Interactive Inspector Sheet Card
          item {
            SelectedDayInspectorCard(
              selectedDateIso = selectedDateIso,
              dayRecords = recordsByDate[selectedDateIso].orEmpty(),
              ghItem = ghByDate[selectedDateIso],
              rhItem = rhByDate[selectedDateIso],
              tentativeHold = tentativeHolds[selectedDateIso],
              onPromoteTentativeHold = onPromoteTentativeHold,
              onClearTentativeHold = onClearTentativeHold,
              onQuickLogLeave = onQuickLogLeave,
              onOpenFullAddDialog = onOpenFullAddDialog
            )
          }

          // 4. Clinical Duty Roster & 1-Tap GH Worked -> +1 COL Capture Panel
          item {
            val ghCandidate = remember(selectedDateIso, dutyShifts, state.allRecords) {
              com.example.domain.DutyConflictEngine.findUnclaimedGhCaptureForDate(
                dateIso = selectedDateIso,
                dutyShifts = dutyShifts,
                existingRecords = state.allRecords
              )
            }
            CalendarDutyRosterPanel(
              selectedDateIso = selectedDateIso,
              shiftsForDate = shiftsByDate[selectedDateIso].orEmpty(),
              ghCaptureCandidateForDate = ghCandidate,
              onAddDutyShift = onAddDutyShift,
              onAddRecurringWeeklyShift = onAddRecurringWeeklyShift,
              onDeleteDutyShift = onDeleteDutyShift,
              onClaimGhCol = onClaimGhCol
            )
          }

          item {
            Spacer(modifier = Modifier.height(84.dp))
          }
        }
      }
    }
  }
}

@Composable
private fun MonthScrubberBar(
  selectedYear: Long,
  currentMonthValue: Int,
  allRecords: List<LeaveRecord>,
  allPublicHolidays: List<TnHolidayItem>,
  onSelectMonth: (Int) -> Unit
) {
  val neu = LocalNeumorphicColors.current
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState())
      .padding(vertical = 5.dp, horizontal = 2.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    val shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    shortMonths.forEachIndexed { idx, mLabel ->
      val mNum = idx + 1
      val isSelected = currentMonthValue == mNum
      val mPrefix = String.format("%04d-%02d", selectedYear, mNum)
      val hasLeaves = allRecords.any { it.dateIso.startsWith(mPrefix) }
      val hasGh = allPublicHolidays.any { it.dateIso.startsWith(mPrefix) }

      Box(
        modifier = Modifier
          .width(58.dp)
          .height(48.dp)
          .then(
            if (isSelected) {
              Modifier.neumorphicInset(
                shape = IosSharpButtonShape,
                depth = 3.dp,
                blurRadius = 6.dp,
                accentTint = neu.primaryAccent,
                accentTintAlpha = 0.14f,
                borderColor = neu.primaryAccent.copy(alpha = 0.55f),
                neu = neu
              )
            } else {
              Modifier.neumorphicExtruded(
                shape = IosSharpButtonShape,
                elevation = 2.5.dp,
                blurRadius = 5.dp,
                borderAlpha = 0.25f,
                neu = neu
              )
            }
          )
          .clickable { onSelectMonth(mNum) }
          .testTag("month_pill_$mNum")
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 6.dp, vertical = 7.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = mLabel,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 0.3.sp,
            maxLines = 1,
            softWrap = false,
            color = if (isSelected) neu.primaryAccent else neu.textPrimary
          )
          Row(
            modifier = Modifier.height(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            if (hasLeaves) {
              Box(
                modifier = Modifier
                  .size(5.dp)
                  .clip(CircleShape)
                  .background(neu.primaryAccent)
              )
            }
            if (hasGh) {
              Box(
                modifier = Modifier
                  .size(5.dp)
                  .clip(CircleShape)
                  .background(GoldHolidayHighlight)
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CalendarHeaderStatChip(
  label: String,
  accentColor: Color,
  borderColor: Color,
  modifier: Modifier = Modifier
) {
  val neu = LocalNeumorphicColors.current
  Box(
    modifier = modifier
      .neumorphicInset(
        shape = IosSharpBadgeShape,
        depth = 2.dp,
        blurRadius = 4.dp,
        accentTint = borderColor,
        accentTintAlpha = 0.09f,
        borderColor = borderColor.copy(alpha = 0.42f),
        neu = neu
      )
      .padding(horizontal = 8.dp, vertical = 7.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = label,
      fontFamily = JetBrainsMonoFontFamily,
      fontSize = 10.sp,
      fontWeight = FontWeight.Bold,
      color = accentColor,
      maxLines = 1,
      softWrap = false,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Composable
private fun PaddingBox(content: @Composable () -> Unit) {
  Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
    content()
  }
}

@Composable
private fun IosCalendarDayCell(
  dayNumber: Int,
  dateIso: String,
  isSelected: Boolean,
  isSunday: Boolean,
  isSaturday: Boolean,
  dayRecords: List<LeaveRecord>,
  dayShifts: List<com.example.data.DutyShift> = emptyList(),
  layerFilter: CalendarLayerFilter = CalendarLayerFilter.ALL,
  ghItem: TnHolidayItem?,
  rhItem: TnHolidayItem?,
  tentativeHold: TentativeHoldRecord? = null,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val neu = LocalNeumorphicColors.current
  val visibleRecords = if (layerFilter == CalendarLayerFilter.DUTY) emptyList() else dayRecords
  val visibleShifts = if (layerFilter == CalendarLayerFilter.LEAVES) emptyList() else dayShifts
  val primaryRecord = visibleRecords.firstOrNull()
  val primaryShift = visibleShifts.firstOrNull()
  val recordCode = primaryRecord?.category?.shortLabel?.uppercase()

  val isColOrDuty = recordCode == "COL" || recordCode == "GHW" || primaryRecord?.leaveType == "GH_WORKED"
  val isPlRecord = recordCode == "PL"
  val isLeaveRecord = primaryRecord != null && !isColOrDuty && !isPlRecord
  val hasHoldOnly = layerFilter != CalendarLayerFilter.DUTY && primaryRecord == null && tentativeHold != null
  val dutyHighlightColor = if (primaryShift?.shiftTypeEnum == com.example.data.DutyShiftType.CALL_24H ||
    primaryShift?.shiftTypeEnum == com.example.data.DutyShiftType.GH_EMERGENCY
  ) {
    com.example.ui.theme.StatusAlertRed
  } else {
    com.example.ui.theme.BadgeOnDutyBlue
  }

  val goldText = if (neu.isDark) Color(0xFFFDE68A) else Color(0xFFB45309)
  val tealText = if (neu.isDark) Color(0xFF99F6E4) else Color(0xFF0F766E)
  val cyanText = if (neu.isDark) Color(0xFF67E8F9) else Color(0xFF0369A1)
  val emeraldText = if (neu.isDark) Color(0xFF6EE7B7) else Color(0xFF047857)
  val holdText = if (neu.isDark) Color(0xFFA5B4FC) else Color(0xFF4338CA)
  val sunColor = if (neu.isDark) Color(0xFFFCD34D) else Color(0xFFD97706)
  val satColor = if (neu.isDark) Color(0xFF7DD3FC) else Color(0xFF0284C7)

  val tileAccent: Color? = when {
    layerFilter == CalendarLayerFilter.DUTY && primaryShift != null -> dutyHighlightColor
    isColOrDuty -> CyanColHighlight
    isPlRecord || ghItem != null -> GoldHolidayHighlight
    isLeaveRecord -> EmeraldLeaveHighlight
    primaryShift != null -> dutyHighlightColor
    hasHoldOnly -> TentativeHoldViolet
    rhItem != null -> TealRhHighlight
    else -> null
  }

  val dayNumberColor = when {
    isSelected -> neu.primaryAccent
    layerFilter == CalendarLayerFilter.DUTY && primaryShift != null -> dutyHighlightColor
    isColOrDuty -> cyanText
    isPlRecord || ghItem != null -> goldText
    isLeaveRecord -> emeraldText
    primaryShift != null -> dutyHighlightColor
    hasHoldOnly -> holdText
    rhItem != null -> tealText
    isSunday -> sunColor
    isSaturday -> satColor
    else -> neu.textPrimary
  }

  val badgeLabel: String? = when {
    layerFilter == CalendarLayerFilter.DUTY && primaryShift != null -> primaryShift.shiftTypeEnum.shortLabel.take(4)
    primaryRecord != null -> primaryRecord.category.shortLabel.take(4)
    primaryShift != null -> primaryShift.shiftTypeEnum.shortLabel.take(4)
    hasHoldOnly -> "HOLD"
    ghItem != null -> "GH"
    rhItem != null -> "RL"
    else -> null
  }

  val badgePillBg: Color = when {
    layerFilter == CalendarLayerFilter.DUTY && primaryShift != null -> dutyHighlightColor
    isColOrDuty -> CyanColHighlight
    isPlRecord || ghItem != null -> GoldHolidayHighlight
    isLeaveRecord -> EmeraldLeaveHighlight
    primaryShift != null -> dutyHighlightColor
    hasHoldOnly -> TentativeHoldViolet
    rhItem != null -> TealRhHighlight
    else -> EmeraldOptimal
  }

  val badgePillTextColor: Color = when {
    layerFilter == CalendarLayerFilter.DUTY && primaryShift != null -> Color.White
    isPlRecord || ghItem != null -> Color(0xFF1F1202)
    isColOrDuty || rhItem != null -> Color(0xFF031824)
    else -> Color.White
  }

  Box(
    modifier = modifier
      .aspectRatio(0.84f)
      .then(
        if (isSelected) {
          Modifier.neumorphicInset(
            shape = IosSharpButtonShape,
            depth = 3.dp,
            blurRadius = 6.dp,
            accentTint = tileAccent ?: neu.primaryAccent,
            accentTintAlpha = 0.16f,
            borderColor = neu.primaryAccent,
            neu = neu
          )
        } else {
          Modifier.neumorphicExtruded(
            shape = IosSharpButtonShape,
            elevation = 2.5.dp,
            blurRadius = 5.dp,
            accentTint = tileAccent,
            accentTintAlpha = if (tileAccent != null) 0.11f else 0.0f,
            borderAlpha = if (tileAccent != null) 0.45f else 0.20f,
            neu = neu
          )
        }
      )
      .clickable { onClick() }
      .padding(horizontal = 3.dp, vertical = 4.dp)
      .testTag("calendar_day_$dateIso")
  ) {
    // Multi-Record Count Badge or Duty Overlay Dot (top-left)
    if (visibleRecords.size > 1) {
      Box(
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(top = 1.dp, start = 1.dp)
          .clip(RoundedCornerShape(4.dp))
          .background(neu.primaryAccent.copy(alpha = 0.22f))
      ) {
        Text(
          text = "×${visibleRecords.size}",
          fontFamily = JetBrainsMonoFontFamily,
          fontSize = 7.sp,
          lineHeight = 8.sp,
          fontWeight = FontWeight.ExtraBold,
          color = neu.primaryAccent,
          modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
        )
      }
    } else if (visibleShifts.isNotEmpty() && primaryRecord != null) {
      Box(
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(top = 2.dp, start = 2.dp)
          .size(6.dp)
          .clip(CircleShape)
          .background(dutyHighlightColor)
      )
    }

    // Active Selection Indicator Dot in Top-Right Corner
    if (isSelected) {
      Box(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(top = 2.dp, end = 2.dp)
          .size(6.dp)
          .clip(CircleShape)
          .background(neu.primaryAccent)
      )
    }

    Column(
      modifier = Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Text(
        text = dayNumber.toString(),
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 14.sp,
        lineHeight = 16.sp,
        color = dayNumberColor
      )

      if (badgeLabel != null) {
        Spacer(modifier = Modifier.height(3.dp))
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(badgePillBg)
        ) {
          Text(
            text = badgeLabel,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 8.5.sp,
            lineHeight = 10.sp,
            letterSpacing = 0.2.sp,
            fontWeight = FontWeight.ExtraBold,
            color = badgePillTextColor,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
          )
        }
      } else {
        Spacer(modifier = Modifier.height(5.dp))
        Box(
          modifier = Modifier
            .size(3.dp)
            .clip(CircleShape)
            .background(
              when {
                isSunday -> sunColor.copy(alpha = 0.55f)
                isSaturday -> satColor.copy(alpha = 0.55f)
                else -> neu.textSecondary.copy(alpha = 0.28f)
              }
            )
        )
      }
    }
  }
}

@Composable
private fun CalendarLegendPill(
  label: String,
  badgeCode: String,
  dotColor: Color,
  textColor: Color,
  modifier: Modifier = Modifier
) {
  val neu = LocalNeumorphicColors.current
  Box(
    modifier = modifier.neumorphicInset(
      shape = IosSharpBadgeShape,
      depth = 2.dp,
      blurRadius = 4.dp,
      accentTint = dotColor,
      accentTintAlpha = 0.08f,
      borderColor = dotColor.copy(alpha = 0.40f),
      neu = neu
    )
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
      Box(
        modifier = Modifier.neumorphicExtruded(
          shape = RoundedCornerShape(5.dp),
          elevation = 1.dp,
          blurRadius = 2.dp,
          surfaceColor = dotColor,
          neu = neu
        )
      ) {
        Text(
          text = badgeCode,
          fontFamily = JetBrainsMonoFontFamily,
          fontSize = 8.sp,
          fontWeight = FontWeight.ExtraBold,
          color = Color(0xFF041322),
          modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
      }
      Text(
        text = label,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = textColor,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun SelectedDayInspectorCard(
  selectedDateIso: String,
  dayRecords: List<LeaveRecord>,
  ghItem: TnHolidayItem?,
  rhItem: TnHolidayItem?,
  tentativeHold: TentativeHoldRecord? = null,
  onPromoteTentativeHold: (TentativeHoldRecord) -> Unit = {},
  onClearTentativeHold: (String) -> Unit = {},
  onQuickLogLeave: (leaveType: String, dateIso: String, notes: String) -> Unit,
  onOpenFullAddDialog: () -> Unit
) {
  val neu = LocalNeumorphicColors.current
  val goldText = if (neu.isDark) Color(0xFFFDE68A) else Color(0xFFB45309)
  val tealText = if (neu.isDark) Color(0xFF99F6E4) else Color(0xFF0F766E)
  val cyanText = if (neu.isDark) Color(0xFF67E8F9) else Color(0xFF0369A1)
  val emeraldText = if (neu.isDark) Color(0xFF6EE7B7) else Color(0xFF047857)

  val parsedDayOfWeek = remember(selectedDateIso) {
    try {
      LocalDate.parse(selectedDateIso, DateTimeFormatter.ISO_LOCAL_DATE)
        .dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
    } catch (_: Exception) {
      ""
    }
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .neumorphicExtruded(
        shape = IosSharpCardShape,
        elevation = 5.dp,
        blurRadius = 10.dp,
        accentTint = neu.primaryAccent,
        borderAlpha = 0.34f,
        neu = neu
      )
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "SELECTED DATE INSPECTOR",
            style = MaterialTheme.typography.labelMedium,
            color = neu.primaryAccent,
            fontSize = 10.sp,
            lineHeight = 13.sp,
            letterSpacing = 0.4.sp,
            maxLines = 1,
            softWrap = false
          )
          Text(
            text = "${ClinicalDateFormatter.formatFriendlyDate(selectedDateIso)} • $parsedDayOfWeek",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = neu.textPrimary,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
          )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
          modifier = Modifier
            .neumorphicExtruded(
              shape = IosSharpButtonShape,
              elevation = 2.5.dp,
              blurRadius = 5.dp,
              accentTint = neu.primaryAccent,
              accentTintAlpha = 0.08f,
              borderAlpha = 0.38f,
              neu = neu
            )
            .clickable { onOpenFullAddDialog() }
            .padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = null,
              tint = neu.primaryAccent,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Custom Entry",
              fontSize = 11.sp,
              lineHeight = 14.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              softWrap = false,
              color = neu.primaryAccent
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      if (ghItem != null) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .neumorphicInset(
              shape = IosSharpWellShape,
              depth = 2.5.dp,
              blurRadius = 5.dp,
              accentTint = GoldHolidayHighlight,
              accentTintAlpha = 0.10f,
              borderColor = GoldHolidayHighlight.copy(alpha = 0.55f),
              neu = neu
            )
            .padding(12.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Star,
              contentDescription = null,
              tint = GoldHolidayHighlight,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "TN Govt Public Holiday: ${ghItem.nameTamil}",
                style = MaterialTheme.typography.labelLarge,
                color = goldText,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "${ghItem.nameEnglish} — Eligible for PL or GH Worked (+1 COL credit)",
                style = MaterialTheme.typography.bodyMedium,
                color = neu.textSecondary,
                fontSize = 12.sp
              )
            }
          }
        }
      }

      if (rhItem != null) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .neumorphicInset(
              shape = IosSharpWellShape,
              depth = 2.5.dp,
              blurRadius = 5.dp,
              accentTint = TealRhHighlight,
              accentTintAlpha = 0.10f,
              borderColor = TealRhHighlight.copy(alpha = 0.55f),
              neu = neu
            )
            .padding(12.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.EventAvailable,
              contentDescription = null,
              tint = TealRhHighlight,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "TN Restricted Holiday (RL): ${rhItem.nameTamil}",
                style = MaterialTheme.typography.labelLarge,
                color = tealText,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "${rhItem.nameEnglish} — Eligible for 1 of your 3 annual RH credits",
                style = MaterialTheme.typography.bodyMedium,
                color = neu.textSecondary,
                fontSize = 12.sp
              )
            }
          }
        }
      }

      if (tentativeHold != null) {
        val holdText = if (neu.isDark) Color(0xFFA5B4FC) else Color(0xFF4338CA)
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .neumorphicInset(
              shape = IosSharpWellShape,
              depth = 2.5.dp,
              blurRadius = 5.dp,
              accentTint = TentativeHoldViolet,
              accentTintAlpha = 0.12f,
              borderColor = TentativeHoldViolet.copy(alpha = 0.55f),
              neu = neu
            )
            .padding(12.dp)
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Bookmark,
                contentDescription = null,
                tint = TentativeHoldViolet,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "TENTATIVE AI HOLD (${tentativeHold.creditType})",
                  style = MaterialTheme.typography.labelLarge,
                  color = holdText,
                  fontWeight = FontWeight.ExtraBold
                )
                Text(
                  text = "${tentativeHold.bridgeTitle} — ${tentativeHold.note}",
                  style = MaterialTheme.typography.bodySmall,
                  color = neu.textSecondary
                )
              }
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Box(
                modifier = Modifier
                  .weight(1f)
                  .neumorphicExtruded(
                    shape = IosSharpButtonShape,
                    elevation = 2.dp,
                    blurRadius = 4.dp,
                    surfaceColor = EmeraldOptimal,
                    neu = neu
                  )
                  .clickable { onPromoteTentativeHold(tentativeHold) }
                  .padding(vertical = 8.dp, horizontal = 8.dp)
                  .testTag("promote_hold_btn_$selectedDateIso"),
                contentAlignment = Alignment.Center
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                  )
                  Text(
                    text = "Promote to Ledger",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false
                  )
                }
              }
              Box(
                modifier = Modifier
                  .neumorphicExtruded(
                    shape = IosSharpButtonShape,
                    elevation = 2.dp,
                    blurRadius = 4.dp,
                    accentTint = Color(0xFFEF4444),
                    accentTintAlpha = 0.08f,
                    borderAlpha = 0.35f,
                    neu = neu
                  )
                  .clickable { onClearTentativeHold(tentativeHold.dateIso) }
                  .padding(vertical = 8.dp, horizontal = 12.dp)
                  .testTag("clear_hold_btn_$selectedDateIso"),
                contentAlignment = Alignment.Center
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Remove Hold",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(14.dp)
                  )
                  Text(
                    text = "Clear",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF4444)
                  )
                }
              }
            }
          }
        }
      }

      if (dayRecords.isNotEmpty()) {
        dayRecords.forEach { rec ->
          val accent = rec.category.accentColor()
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp)
              .neumorphicInset(
                shape = IosSharpWellShape,
                depth = 2.5.dp,
                blurRadius = 5.dp,
                accentTint = accent,
                accentTintAlpha = 0.08f,
                borderColor = accent.copy(alpha = 0.45f),
                neu = neu
              )
              .padding(12.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier.neumorphicExtruded(
                  shape = IosSharpMicroShape,
                  elevation = 1.5.dp,
                  blurRadius = 3.dp,
                  surfaceColor = accent,
                  neu = neu
                )
              ) {
                Text(
                  text = rec.category.shortLabel,
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = rec.category.fullTitle,
                  style = MaterialTheme.typography.labelLarge,
                  color = neu.textPrimary,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = rec.notes.ifBlank { "Logged in HOD Leave Ledger" },
                  style = MaterialTheme.typography.bodyMedium,
                  fontSize = 12.sp,
                  color = neu.textSecondary
                )
                if (rec.pairedDateIso.isNotBlank()) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Default.Link,
                      contentDescription = null,
                      tint = neu.primaryAccent,
                      modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = "Paired with: ${ClinicalDateFormatter.formatFriendlyWithDay(rec.pairedDateIso)}",
                      style = MaterialTheme.typography.labelMedium,
                      color = neu.primaryAccent,
                      fontSize = 11.sp
                    )
                  }
                }
              }
            }
          }
        }
      } else {
        Text(
          text = "No leave or hospital duty logged on ${ClinicalDateFormatter.formatFriendlyDate(selectedDateIso)} yet. Quick-log for this date:",
          style = MaterialTheme.typography.bodyMedium,
          color = neu.textSecondary
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          QuickActionChip(
            label = "+ Log CL",
            color = EmeraldLeaveHighlight,
            textColor = emeraldText,
            onClick = { onQuickLogLeave("CL", selectedDateIso, "Casual Leave on $selectedDateIso") },
            modifier = Modifier.weight(1f)
          )
          QuickActionChip(
            label = "+ Log COL",
            color = CyanColHighlight,
            textColor = cyanText,
            onClick = { onQuickLogLeave("COL", selectedDateIso, "Compensatory Off on $selectedDateIso") },
            modifier = Modifier.weight(1f)
          )
          if (rhItem != null) {
            QuickActionChip(
              label = "+ Log RH",
              color = TealRhHighlight,
              textColor = tealText,
              onClick = { onQuickLogLeave("RH", selectedDateIso, "RH: ${rhItem.nameEnglish} (${rhItem.nameTamil})") },
              modifier = Modifier.weight(1f)
            )
          } else if (ghItem != null) {
            QuickActionChip(
              label = "+ GH Worked",
              color = GoldHolidayHighlight,
              textColor = goldText,
              onClick = { onQuickLogLeave("GH_WORKED", selectedDateIso, "GH Duty: ${ghItem.nameEnglish}") },
              modifier = Modifier.weight(1f)
            )
          } else {
            QuickActionChip(
              label = "+ Log PL",
              color = GoldHolidayHighlight,
              textColor = goldText,
              onClick = { onQuickLogLeave("PL", selectedDateIso, "Public Holiday Leave on $selectedDateIso") },
              modifier = Modifier.weight(1f)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun QuickActionChip(
  label: String,
  color: Color,
  textColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val neu = LocalNeumorphicColors.current
  Box(
    modifier = modifier
      .neumorphicExtruded(
        shape = IosSharpButtonShape,
        elevation = 2.5.dp,
        blurRadius = 5.dp,
        accentTint = color,
        accentTintAlpha = 0.08f,
        borderAlpha = 0.35f,
        neu = neu
      )
      .clickable { onClick() }
      .padding(vertical = 10.dp, horizontal = 8.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = label,
      color = textColor,
      fontWeight = FontWeight.ExtraBold,
      fontSize = 11.sp,
      letterSpacing = 0.25.sp
    )
  }
}
