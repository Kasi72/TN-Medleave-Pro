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
import com.example.domain.TnHolidays2027Catalog
import com.example.ui.theme.BadgeClGreen
import com.example.ui.theme.BadgeColCyan
import com.example.ui.theme.BadgeGhLime
import com.example.ui.theme.BadgePlGold
import com.example.ui.theme.BadgeRhSlate
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.IosDarkDockSurface
import com.example.ui.theme.IosSquircleGlassFill
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonAquaAccent
import com.example.ui.theme.NeonCyanBright
import com.example.ui.theme.NeonCyanText
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// Luminous Jewel-Glass Calendar Palette (Tuned for Maximum Contrast & Apple iOS 18 Aesthetics)
private val GoldHolidayHighlight = Color(0xFFF59E0B)
private val GoldHolidayBrightText = Color(0xFFFDE68A)
private val GoldHolidayDeepFill = Color(0xFF3B2307)

private val TealRhHighlight = Color(0xFF14B8A6)
private val TealRhBrightText = Color(0xFF99F6E4)
private val TealRhDeepFill = Color(0xFF0B2E2F)

private val CyanColHighlight = Color(0xFF06B6D4)
private val CyanColBrightText = Color(0xFF67E8F9)
private val CyanColDeepFill = Color(0xFF082C42)

private val EmeraldLeaveHighlight = Color(0xFF10B981)
private val EmeraldLeaveBrightText = Color(0xFF6EE7B7)
private val EmeraldLeaveDeepFill = Color(0xFF073229)

private val CalendarCardDeepBg = Color(0xFF071627)
private val CalendarWeekdayTileBg = Color(0xFF0D223A)
private val CalendarWeekdayTileBorder = Color(0xFF1B385A)
private val CalendarWeekendTileBg = Color(0xFF102844)
private val CalendarWeekendTileBorder = Color(0xFF23456E)

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
  onApplyBridge: (com.example.data.SmartLeaveBridge) -> Unit = {}
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

  val recordsByDate: Map<String, List<LeaveRecord>> = remember(state.allRecords) {
    state.allRecords.groupBy { it.dateIso }
  }
  val ghByDate: Map<String, TnHolidayItem> = remember(allPublicHolidays) {
    allPublicHolidays.associateBy { it.dateIso }
  }
  val rhByDate: Map<String, TnHolidayItem> = remember(allRestrictedHolidays) {
    allRestrictedHolidays.associateBy { it.dateIso }
  }

  val monthPrefix = String.format("%04d-%02d", currentYearMonth.year, currentYearMonth.monthValue)
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
              onCommitDraftLeaves = onCommitDraftLeaves
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
            Surface(
              shape = RoundedCornerShape(24.dp),
              color = CalendarCardDeepBg,
              tonalElevation = 4.dp,
              border = BorderStroke(1.dp, NeonCyanText.copy(alpha = 0.32f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(18.dp)) {
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
                      color = Color.White,
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
                    color = NeonCyanBright.copy(alpha = 0.8f)
                  )
                } else {
                  combinedMonthHolidays.forEach { hItem ->
                    val alreadyLogged = recordsByDate[hItem.dateIso]?.firstOrNull()
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                          if (hItem.isRestrictedHoliday) TealRhDeepFill else GoldHolidayDeepFill
                        )
                        .border(
                          width = 1.dp,
                          color = if (hItem.isRestrictedHoliday) {
                            TealRhHighlight.copy(alpha = 0.45f)
                          } else {
                            GoldHolidayHighlight.copy(alpha = 0.50f)
                          },
                          shape = RoundedCornerShape(14.dp)
                        )
                        .clickable {
                          selectedDateIso = hItem.dateIso
                          calendarSubMode = 0
                        }
                        .padding(12.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (hItem.isRestrictedHoliday) TealRhHighlight else GoldHolidayHighlight
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
                          color = Color.White,
                          fontWeight = FontWeight.Bold,
                          maxLines = 1,
                          softWrap = false,
                          overflow = TextOverflow.Ellipsis
                        )
                        Text(
                          text = hItem.nameEnglish,
                          style = MaterialTheme.typography.bodyMedium,
                          fontSize = 12.sp,
                          color = if (hItem.isRestrictedHoliday) TealRhBrightText else GoldHolidayBrightText
                        )
                      }
                      if (alreadyLogged != null) {
                        Surface(
                          shape = RoundedCornerShape(8.dp),
                          color = EmeraldLeaveHighlight.copy(alpha = 0.22f),
                          border = BorderStroke(1.dp, EmeraldLeaveHighlight)
                        ) {
                          Text(
                            text = alreadyLogged.leaveType,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldLeaveBrightText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                          )
                        }
                      } else {
                        Surface(
                          shape = RoundedCornerShape(10.dp),
                          color = IosSquircleGlassFill,
                          border = BorderStroke(1.dp, NeonAquaAccent.copy(alpha = 0.55f)),
                          modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                              if (hItem.isRestrictedHoliday) {
                                onQuickLogLeave("RH", hItem.dateIso, "${hItem.nameEnglish} (${hItem.nameTamil})")
                              } else {
                                onQuickLogLeave("GH_WORKED", hItem.dateIso, "GH Worked: ${hItem.nameEnglish}")
                              }
                            }
                        ) {
                          Text(
                            text = if (hItem.isRestrictedHoliday) "+ Log RH" else "+ GH Duty",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonAquaAccent,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
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
            }
          }

          // 2. Main Luxury iOS 18 Bento Squircle Calendar Card
          item {
            Surface(
              shape = RoundedCornerShape(28.dp),
              color = CalendarCardDeepBg,
              tonalElevation = 6.dp,
              shadowElevation = 12.dp,
              modifier = Modifier
                .fillMaxWidth()
                .border(
                  width = 1.2.dp,
                  brush = Brush.linearGradient(
                    listOf(
                      NeonAquaAccent.copy(alpha = 0.50f),
                      NeonCyanText.copy(alpha = 0.28f),
                      GoldHolidayHighlight.copy(alpha = 0.32f)
                    )
                  ),
                  shape = RoundedCornerShape(28.dp)
                )
            ) {
              Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp)) {
                // Calendar Month Header + High-Contrast Neon Squircle Prev/Next Buttons
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
                          .background(NeonAquaAccent)
                      )
                      Text(
                        text = "TN MEDLEAVE BENTO CALENDAR",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonAquaAccent,
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
                      color = Color.White
                    )
                  }

                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Surface(
                      shape = RoundedCornerShape(12.dp),
                      color = NeonAquaAccent.copy(alpha = 0.16f),
                      border = BorderStroke(1.dp, NeonAquaAccent.copy(alpha = 0.55f)),
                      modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                          onSelectYear(2026L)
                          currentMonthValue = 11
                          selectedDateIso = "2026-11-08"
                        }
                        .testTag("calendar_today_quick_jump")
                    ) {
                      Box(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                      ) {
                        Text(
                          text = "Today",
                          fontFamily = JetBrainsMonoFontFamily,
                          fontSize = 11.sp,
                          fontWeight = FontWeight.ExtraBold,
                          color = NeonAquaAccent,
                          maxLines = 1,
                          softWrap = false
                        )
                      }
                    }

                    Surface(
                      shape = RoundedCornerShape(13.dp),
                      color = IosSquircleGlassFill,
                      border = BorderStroke(1.dp, NeonCyanText.copy(alpha = 0.45f)),
                      modifier = Modifier.size(38.dp)
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
                          tint = NeonCyanBright
                        )
                      }
                    }

                    Surface(
                      shape = RoundedCornerShape(13.dp),
                      color = IosSquircleGlassFill,
                      border = BorderStroke(1.dp, NeonCyanText.copy(alpha = 0.45f)),
                      modifier = Modifier.size(38.dp)
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
                          tint = NeonCyanBright
                        )
                      }
                    }
                  }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3 Uniform Monthly Summary Telemetry Chips
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  CalendarHeaderStatChip(
                    label = "${monthRecords.size} Leaves / Duties",
                    accentColor = CyanColBrightText,
                    borderColor = CyanColHighlight,
                    bgColor = CyanColDeepFill,
                    modifier = Modifier
                      .weight(1f)
                      .fillMaxHeight()
                  )
                  CalendarHeaderStatChip(
                    label = "${monthGhList.size} TN Public GH",
                    accentColor = GoldHolidayBrightText,
                    borderColor = GoldHolidayHighlight,
                    bgColor = GoldHolidayDeepFill,
                    modifier = Modifier
                      .weight(1f)
                      .fillMaxHeight()
                  )
                  CalendarHeaderStatChip(
                    label = "${monthRhList.size} Restricted RL",
                    accentColor = TealRhBrightText,
                    borderColor = TealRhHighlight,
                    bgColor = TealRhDeepFill,
                    modifier = Modifier
                      .weight(1f)
                      .fillMaxHeight()
                  )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Recessed iOS Capsule Weekday Header Bar (SUN .. SAT)
                val weekLabels = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = Color(0xFF0B1F36),
                  border = BorderStroke(1.dp, Color(0xFF1A3656)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(vertical = 8.dp)
                  ) {
                    weekLabels.forEachIndexed { idx, wLabel ->
                      val headerColor = when (idx) {
                        0 -> Color(0xFFFBBF24) // Warm Gold for Sunday
                        6 -> Color(0xFF38BDF8) // Sky Cyan for Saturday
                        else -> NeonCyanBright // Crisp Neon Cyan for Mon–Fri
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
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 7-Column Architectural Bento Month Grid
                val firstDayOfMonth = currentYearMonth.atDay(1)
                val leadingEmptyCells = firstDayOfMonth.dayOfWeek.value % 7
                val daysInMonth = currentYearMonth.lengthOfMonth()
                val totalCells = ((leadingEmptyCells + daysInMonth + 6) / 7) * 7
                val rows = totalCells / 7

                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                  for (rowIdx in 0 until rows) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                            ghItem = ghItem,
                            rhItem = rhItem,
                            onClick = { selectedDateIso = dateIso },
                            modifier = Modifier.weight(1f)
                          )
                        } else {
                          // Recessed subtle ghost tile so the 7x5 matrix stays geometrically uniform
                          Box(
                            modifier = Modifier
                              .weight(1f)
                              .aspectRatio(0.84f)
                              .clip(RoundedCornerShape(14.dp))
                              .background(Color(0xFF091829).copy(alpha = 0.42f))
                              .border(
                                width = 0.6.dp,
                                color = Color(0xFF132942).copy(alpha = 0.5f),
                                shape = RoundedCornerShape(14.dp)
                              )
                          )
                        }
                      }
                    }
                  }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2x2 High-Contrast Jewel Squircle Legend Grid
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    CalendarLegendPill(
                      label = "Leave Taken (CL/EL)",
                      badgeCode = "CL",
                      dotColor = EmeraldLeaveHighlight,
                      textColor = EmeraldLeaveBrightText,
                      bgColor = EmeraldLeaveDeepFill,
                      modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                    )
                    CalendarLegendPill(
                      label = "COL / GH Duty",
                      badgeCode = "COL",
                      dotColor = CyanColHighlight,
                      textColor = CyanColBrightText,
                      bgColor = CyanColDeepFill,
                      modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                    )
                  }
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    CalendarLegendPill(
                      label = "TN Public Holiday",
                      badgeCode = "GH",
                      dotColor = GoldHolidayHighlight,
                      textColor = GoldHolidayBrightText,
                      bgColor = GoldHolidayDeepFill,
                      modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                    )
                    CalendarLegendPill(
                      label = "Restricted Holiday",
                      badgeCode = "RL",
                      dotColor = TealRhHighlight,
                      textColor = TealRhBrightText,
                      bgColor = TealRhDeepFill,
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
              onQuickLogLeave = onQuickLogLeave,
              onOpenFullAddDialog = onOpenFullAddDialog
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
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    val shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    shortMonths.forEachIndexed { idx, mLabel ->
      val mNum = idx + 1
      val isSelected = currentMonthValue == mNum
      val mPrefix = String.format("%04d-%02d", selectedYear, mNum)
      val hasLeaves = allRecords.any { it.dateIso.startsWith(mPrefix) }
      val hasGh = allPublicHolidays.any { it.dateIso.startsWith(mPrefix) }

      Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) IosSquircleGlassFill else IosDarkDockSurface,
        tonalElevation = if (isSelected) 4.dp else 1.dp,
        border = BorderStroke(
          width = if (isSelected) 1.4.dp else 1.dp,
          color = if (isSelected) NeonAquaAccent else NeonCyanText.copy(alpha = 0.25f)
        ),
        modifier = Modifier
          .width(58.dp)
          .height(48.dp)
          .clip(RoundedCornerShape(14.dp))
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
            color = if (isSelected) NeonAquaAccent else NeonCyanBright
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
                  .background(NeonAquaAccent)
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
  bgColor: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(11.dp),
    color = bgColor,
    border = BorderStroke(1.dp, borderColor.copy(alpha = 0.55f))
  ) {
    Box(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
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
  ghItem: TnHolidayItem?,
  rhItem: TnHolidayItem?,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val primaryRecord = dayRecords.firstOrNull()
  val recordCode = primaryRecord?.category?.shortLabel?.uppercase()

  // Classify visual theme of the tile for razor-sharp dark-glass legibility
  val isColOrDuty = recordCode == "COL" || recordCode == "GHW" || primaryRecord?.leaveType == "GH_WORKED"
  val isPlRecord = recordCode == "PL"
  val isLeaveRecord = primaryRecord != null && !isColOrDuty && !isPlRecord

  val tileBgColor = when {
    isColOrDuty -> CyanColDeepFill
    isPlRecord || ghItem != null -> GoldHolidayDeepFill
    isLeaveRecord -> EmeraldLeaveDeepFill
    rhItem != null -> TealRhDeepFill
    isSunday || isSaturday -> CalendarWeekendTileBg
    else -> CalendarWeekdayTileBg
  }

  val tileBorderColor = when {
    isSelected -> NeonAquaAccent
    isColOrDuty -> CyanColHighlight.copy(alpha = 0.80f)
    isPlRecord || ghItem != null -> GoldHolidayHighlight.copy(alpha = 0.80f)
    isLeaveRecord -> EmeraldLeaveHighlight.copy(alpha = 0.80f)
    rhItem != null -> TealRhHighlight.copy(alpha = 0.70f)
    isSunday || isSaturday -> CalendarWeekendTileBorder
    else -> CalendarWeekdayTileBorder
  }

  // Always high-luminance text on dark glass so no number is ever dim or muddy
  val dayNumberColor = when {
    isSelected -> Color.White
    isColOrDuty -> CyanColBrightText
    isPlRecord || ghItem != null -> GoldHolidayBrightText
    isLeaveRecord -> EmeraldLeaveBrightText
    rhItem != null -> TealRhBrightText
    isSunday -> Color(0xFFFCD34D)
    isSaturday -> Color(0xFF7DD3FC)
    else -> Color(0xFFF8FAFC)
  }

  val badgeLabel: String? = when {
    primaryRecord != null -> primaryRecord.category.shortLabel.take(4)
    ghItem != null -> "GH"
    rhItem != null -> "RL"
    else -> null
  }

  val badgePillBg: Color = when {
    isColOrDuty -> CyanColHighlight
    isPlRecord || ghItem != null -> GoldHolidayHighlight
    isLeaveRecord -> EmeraldLeaveHighlight
    rhItem != null -> TealRhHighlight
    else -> EmeraldOptimal
  }

  val badgePillTextColor: Color = when {
    isPlRecord || ghItem != null -> Color(0xFF1F1202)
    isColOrDuty || rhItem != null -> Color(0xFF031824)
    else -> Color.White
  }

  Box(
    modifier = modifier
      .aspectRatio(0.84f)
      .clip(RoundedCornerShape(14.dp))
      .then(
        if (isSelected) {
          Modifier.background(
            Brush.verticalGradient(
              listOf(
                NeonAquaAccent.copy(alpha = 0.30f),
                tileBgColor
              )
            )
          )
        } else {
          Modifier.background(tileBgColor)
        }
      )
      .border(
        width = if (isSelected) 2.dp else 1.dp,
        color = tileBorderColor,
        shape = RoundedCornerShape(14.dp)
      )
      .clickable { onClick() }
      .padding(horizontal = 3.dp, vertical = 4.dp)
      .testTag("calendar_day_$dateIso")
  ) {
    // Multi-Record Count Badge (top-left) when 2+ entries exist on the same date (e.g., GH_WORKED + IN_LIEU or FN + AN)
    if (dayRecords.size > 1) {
      Surface(
        shape = RoundedCornerShape(4.dp),
        color = NeonAquaAccent.copy(alpha = 0.25f),
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(top = 1.dp, start = 1.dp)
      ) {
        Text(
          text = "×${dayRecords.size}",
          fontFamily = JetBrainsMonoFontFamily,
          fontSize = 7.sp,
          lineHeight = 8.sp,
          fontWeight = FontWeight.ExtraBold,
          color = NeonAquaAccent,
          modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
        )
      }
    }

    // Active Selection Indicator Dot in Top-Right Corner
    if (isSelected) {
      Box(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(top = 2.dp, end = 2.dp)
          .size(6.dp)
          .clip(CircleShape)
          .background(NeonAquaAccent)
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
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = badgePillBg
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
        // Subtle dot on weekends to preserve vertical balance across all tiles
        Spacer(modifier = Modifier.height(5.dp))
        Box(
          modifier = Modifier
            .size(3.dp)
            .clip(CircleShape)
            .background(
              when {
                isSunday -> Color(0xFFFBBF24).copy(alpha = 0.45f)
                isSaturday -> Color(0xFF38BDF8).copy(alpha = 0.45f)
                else -> Color.White.copy(alpha = 0.14f)
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
  bgColor: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(11.dp),
    color = bgColor,
    border = BorderStroke(1.dp, dotColor.copy(alpha = 0.50f))
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
      Surface(
        shape = RoundedCornerShape(5.dp),
        color = dotColor
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
  onQuickLogLeave: (leaveType: String, dateIso: String, notes: String) -> Unit,
  onOpenFullAddDialog: () -> Unit
) {
  val parsedDayOfWeek = remember(selectedDateIso) {
    try {
      LocalDate.parse(selectedDateIso, DateTimeFormatter.ISO_LOCAL_DATE)
        .dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
    } catch (_: Exception) {
      ""
    }
  }

  Surface(
    shape = RoundedCornerShape(24.dp),
    color = CalendarCardDeepBg,
    tonalElevation = 4.dp,
    modifier = Modifier
      .fillMaxWidth()
      .border(1.2.dp, NeonAquaAccent.copy(alpha = 0.42f), RoundedCornerShape(24.dp))
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
            color = NeonAquaAccent,
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
            color = Color.White,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
          )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = IosSquircleGlassFill,
          border = BorderStroke(1.dp, NeonAquaAccent.copy(alpha = 0.55f)),
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onOpenFullAddDialog() }
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = null,
              tint = NeonAquaAccent,
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
              color = NeonAquaAccent
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      if (ghItem != null) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = GoldHolidayDeepFill,
          border = BorderStroke(1.dp, GoldHolidayHighlight.copy(alpha = 0.6f)),
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
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
                color = GoldHolidayBrightText,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "${ghItem.nameEnglish} — Eligible for PL or GH Worked (+1 COL credit)",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp
              )
            }
          }
        }
      }

      if (rhItem != null) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = TealRhDeepFill,
          border = BorderStroke(1.dp, TealRhHighlight.copy(alpha = 0.6f)),
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
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
                color = TealRhBrightText,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "${rhItem.nameEnglish} — Eligible for 1 of your 3 annual RH credits",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp
              )
            }
          }
        }
      }

      if (dayRecords.isNotEmpty()) {
        dayRecords.forEach { rec ->
          val accent = rec.category.accentColor()
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF0E253E),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp)
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = accent
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
                  color = Color.White,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = rec.notes.ifBlank { "Logged in HOD Leave Ledger" },
                  style = MaterialTheme.typography.bodyMedium,
                  fontSize = 12.sp,
                  color = NeonCyanBright.copy(alpha = 0.85f)
                )
                if (rec.pairedDateIso.isNotBlank()) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Default.Link,
                      contentDescription = null,
                      tint = NeonAquaAccent,
                      modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = "Paired with: ${ClinicalDateFormatter.formatFriendlyWithDay(rec.pairedDateIso)}",
                      style = MaterialTheme.typography.labelMedium,
                      color = NeonAquaAccent,
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
          color = NeonCyanBright.copy(alpha = 0.85f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          QuickActionChip(
            label = "+ Log CL",
            color = EmeraldLeaveHighlight,
            textColor = EmeraldLeaveBrightText,
            onClick = { onQuickLogLeave("CL", selectedDateIso, "Casual Leave on $selectedDateIso") },
            modifier = Modifier.weight(1f)
          )
          QuickActionChip(
            label = "+ Log COL",
            color = CyanColHighlight,
            textColor = CyanColBrightText,
            onClick = { onQuickLogLeave("COL", selectedDateIso, "Compensatory Off on $selectedDateIso") },
            modifier = Modifier.weight(1f)
          )
          if (rhItem != null) {
            QuickActionChip(
              label = "+ Log RH",
              color = TealRhHighlight,
              textColor = TealRhBrightText,
              onClick = { onQuickLogLeave("RH", selectedDateIso, "RH: ${rhItem.nameEnglish} (${rhItem.nameTamil})") },
              modifier = Modifier.weight(1f)
            )
          } else if (ghItem != null) {
            QuickActionChip(
              label = "+ GH Worked",
              color = GoldHolidayHighlight,
              textColor = GoldHolidayBrightText,
              onClick = { onQuickLogLeave("GH_WORKED", selectedDateIso, "GH Duty: ${ghItem.nameEnglish}") },
              modifier = Modifier.weight(1f)
            )
          } else {
            QuickActionChip(
              label = "+ Log PL",
              color = GoldHolidayHighlight,
              textColor = GoldHolidayBrightText,
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
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = color.copy(alpha = 0.18f),
    border = BorderStroke(1.dp, color.copy(alpha = 0.55f)),
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .clickable { onClick() }
  ) {
    Box(
      modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
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
}
