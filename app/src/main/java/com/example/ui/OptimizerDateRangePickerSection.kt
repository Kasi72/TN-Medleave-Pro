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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import com.example.data.LeaveCategory
import com.example.data.SmartLeaveBridge
import com.example.domain.TnHolidays2027Catalog
import com.example.ui.theme.BadgeClGreen
import com.example.ui.theme.BadgeColCyan
import com.example.ui.theme.BadgePlGold
import com.example.ui.theme.BadgeRhSlate
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.IosDarkDockSurface
import com.example.ui.theme.IosSquircleGlassFill
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonAquaAccent
import com.example.ui.theme.NeonCyanBright
import com.example.ui.theme.NeonCyanText
import com.example.ui.theme.StatusAlertRed
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

data class LongWeekendCalendarPreset(
  val id: String,
  val label: String,
  val startIso: String,
  val endIso: String,
  val holidayHighlight: String,
  val yieldBadge: String,
  val requiredLeaveDates: List<Pair<String, String>>
)

data class RangeDayInspection(
  val dateIso: String,
  val dayShort: String,
  val dayLabel: String,
  val badgeText: String,
  val badgeColor: Color,
  val isFreeDay: Boolean,
  val suggestedCategory: String
)

private fun LocalDate.toUtcEpochMillis(): Long =
  this.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toUtcLocalDate(): LocalDate =
  Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptimizerMaterial3CalendarSection(
  state: LeaveUiState.Success,
  onRunCustomRange: (String, String) -> Unit,
  onApplyBridge: (SmartLeaveBridge) -> Unit,
  onCommitWorkingDaysBatch: (category: LeaveCategory, workingDates: List<String>, notes: String) -> Unit,
  onShowStatusBanner: (String) -> Unit
) {
  val publicHolidaysMap = remember(state.allRecords.size) {
    TnHolidays2027Catalog.allSyncedPublicHolidays().associateBy { it.dateIso }
  }
  val restrictedHolidaysMap = remember(state.allRecords.size) {
    TnHolidays2027Catalog.allSyncedRestrictedHolidays().associateBy { it.dateIso }
  }

  val longWeekendPresets = remember {
    listOf(
      LongWeekendCalendarPreset(
        id = "preset_deepavali_nov_2026",
        label = "Deepavali 2026 Bridge",
        startIso = "2026-11-07",
        endIso = "2026-11-10",
        holidayHighlight = "Sun Nov 8 Deepavali + Mon Nov 9 RL/CL Bridge",
        yieldBadge = "4d Off • 1–2 Credits",
        requiredLeaveDates = listOf("2026-11-09" to "CL", "2026-11-10" to "RH")
      ),
      LongWeekendCalendarPreset(
        id = "preset_christmas_2026",
        label = "Christmas 4-Day Bridge",
        startIso = "2026-12-24",
        endIso = "2026-12-27",
        holidayHighlight = "Thu Dec 24 Christmas Eve RL + Fri Dec 25 Christmas GH",
        yieldBadge = "4.0x ROI • 4d Off / 1 RL",
        requiredLeaveDates = listOf("2026-12-24" to "RH")
      ),
      LongWeekendCalendarPreset(
        id = "preset_new_year_2027",
        label = "New Year 2027 4-Day Bridge",
        startIso = "2026-12-31",
        endIso = "2027-01-03",
        holidayHighlight = "Thu Dec 31 RL/CL + Fri Jan 1, 2027 New Year GH",
        yieldBadge = "4.0x ROI • 4d Off / 1 CL",
        requiredLeaveDates = listOf("2026-12-31" to "CL")
      ),
      LongWeekendCalendarPreset(
        id = "preset_pongal_2027_mega",
        label = "Pongal 2027 6-Day Megabridge",
        startIso = "2027-01-14",
        endIso = "2027-01-19",
        holidayHighlight = "Bhogi RL (Jan 14) + Pongal/Thiruvalluvar/Uzhavar GH (Jan 15–17)",
        yieldBadge = "3.0x ROI • 6d Off / 2 Credits",
        requiredLeaveDates = listOf("2027-01-14" to "RH", "2027-01-18" to "CL")
      ),
      LongWeekendCalendarPreset(
        id = "preset_republic_day_2027",
        label = "Republic Day 2027 4-Day Bridge",
        startIso = "2027-01-23",
        endIso = "2027-01-26",
        holidayHighlight = "Sat/Sun + Mon Jan 25 CL Bridge + Tue Jan 26 Republic Day GH",
        yieldBadge = "4.0x ROI • 4d Off / 1 CL",
        requiredLeaveDates = listOf("2027-01-25" to "CL")
      ),
      LongWeekendCalendarPreset(
        id = "preset_tamil_new_year_2027",
        label = "Tamil New Year 2027 5-Day Bridge",
        startIso = "2027-04-14",
        endIso = "2027-04-18",
        holidayHighlight = "Wed Apr 14 Tamil New Year GH + Thu/Fri Bridge + Weekend",
        yieldBadge = "2.5x ROI • 5d Off / 2 CL",
        requiredLeaveDates = listOf("2027-04-15" to "CL", "2027-04-16" to "CL")
      )
    )
  }

  val initialStartMillis = remember {
    LocalDate.parse("2026-12-24").toUtcEpochMillis()
  }
  val initialEndMillis = remember {
    LocalDate.parse("2026-12-27").toUtcEpochMillis()
  }

  val dateRangePickerState = rememberDateRangePickerState(
    initialSelectedStartDateMillis = initialStartMillis,
    initialSelectedEndDateMillis = initialEndMillis
  )

  val selectedStartDate = dateRangePickerState.selectedStartDateMillis?.toUtcLocalDate()
  val selectedEndDate = dateRangePickerState.selectedEndDateMillis?.toUtcLocalDate()

  // Sync range selection with ViewModel custom range analyzer
  LaunchedEffect(selectedStartDate, selectedEndDate) {
    if (selectedStartDate != null && selectedEndDate != null && !selectedEndDate.isBefore(selectedStartDate)) {
      onRunCustomRange(selectedStartDate.toString(), selectedEndDate.toString())
    }
  }

  val dayInspections = remember(selectedStartDate, selectedEndDate, publicHolidaysMap, restrictedHolidaysMap) {
    val start = selectedStartDate
    val end = selectedEndDate
    if (start == null || end == null || end.isBefore(start)) {
      emptyList()
    } else {
      val list = mutableListOf<RangeDayInspection>()
      var cursor: LocalDate = start
      var safety = 0
      while (!cursor.isAfter(end) && safety < 35) {
        val iso = cursor.toString()
        val dowShort = cursor.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
        val gh = publicHolidaysMap[iso]
        val rh = restrictedHolidaysMap[iso]
        val isWeekend = cursor.dayOfWeek == DayOfWeek.SATURDAY || cursor.dayOfWeek == DayOfWeek.SUNDAY

        val inspection = when {
          gh != null -> RangeDayInspection(
            dateIso = iso,
            dayShort = dowShort,
            dayLabel = "${gh.nameEnglish} (Public GH)",
            badgeText = "FREE GH (0 Cr)",
            badgeColor = BadgePlGold,
            isFreeDay = true,
            suggestedCategory = "GH"
          )
          isWeekend -> RangeDayInspection(
            dateIso = iso,
            dayShort = dowShort,
            dayLabel = if (rh != null) "Weekend + ${rh.nameEnglish}" else "Weekend Non-Debit Day",
            badgeText = "WEEKEND (0 Cr)",
            badgeColor = EmeraldOptimal,
            isFreeDay = true,
            suggestedCategory = "WEEKEND"
          )
          rh != null -> RangeDayInspection(
            dateIso = iso,
            dayShort = dowShort,
            dayLabel = "${rh.nameEnglish} (Restricted Holiday)",
            badgeText = "1.0 RH/RL CREDIT",
            badgeColor = BadgeColCyan,
            isFreeDay = false,
            suggestedCategory = "RH"
          )
          else -> RangeDayInspection(
            dateIso = iso,
            dayShort = dowShort,
            dayLabel = "Working Bridge Day (Apply CL / COL)",
            badgeText = "1.0 CL BRIDGE",
            badgeColor = NeonCyanText,
            isFreeDay = false,
            suggestedCategory = "CL"
          )
        }
        list.add(inspection)
        cursor = cursor.plusDays(1)
        safety++
      }
      list
    }
  }

  val totalDaysSelected = dayInspections.size
  val freeHolidayOrWeekendDays = dayInspections.count { it.isFreeDay }
  val leaveCreditsRequired = dayInspections.count { !it.isFreeDay }
  val ghCountInRange = dayInspections.count { it.suggestedCategory == "GH" }
  val rhCountInRange = dayInspections.count { it.suggestedCategory == "RH" }
  val exceedsTenDayCap = totalDaysSelected > 10
  val efficiencyRatio = if (leaveCreditsRequired > 0) {
    String.format(Locale.US, "%.1fx", totalDaysSelected.toDouble() / leaveCreditsRequired.toDouble())
  } else if (totalDaysSelected > 0) {
    "FREE"
  } else {
    "—"
  }

  Column(
    verticalArrangement = Arrangement.spacedBy(14.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("optimizer_m3_date_range_picker_section")
  ) {
    // 1. Header & Quick-Jump Long Weekend Opportunity Strip
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = IosDarkDockSurface,
      tonalElevation = 4.dp,
      shadowElevation = 6.dp,
      border = BorderStroke(1.2.dp, NeonCyanText.copy(alpha = 0.40f)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(
                  Brush.linearGradient(listOf(NeonAquaAccent, NeonCyanText))
                ),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = "Material 3 DateRangePicker",
                tint = Color(0xFF041322),
                modifier = Modifier.size(22.dp)
              )
            }
            Column {
              Text(
                text = "MATERIAL 3 HOLIDAY & BRIDGE RANGE PICKER",
                style = MaterialTheme.typography.labelSmall,
                color = NeonAquaAccent,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.4.sp
              )
              Text(
                text = "Tap a Long-Weekend Opportunity or Select Dates",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
              )
            }
          }
        }

        // Horizontal Long-Weekend Opportunity Presets that drive DateRangePickerState
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          longWeekendPresets.forEach { preset ->
            val presetStart = LocalDate.parse(preset.startIso)
            val presetEnd = LocalDate.parse(preset.endIso)
            val isSelected = selectedStartDate == presetStart && selectedEndDate == presetEnd

            Surface(
              shape = RoundedCornerShape(14.dp),
              color = if (isSelected) IosSquircleGlassFill else Color(0xFF0D223A),
              border = BorderStroke(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) NeonAquaAccent else NeonCyanText.copy(alpha = 0.28f)
              ),
              modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .clickable {
                  dateRangePickerState.setSelection(
                    startDateMillis = presetStart.toUtcEpochMillis(),
                    endDateMillis = presetEnd.toUtcEpochMillis()
                  )
                }
                .testTag("m3_preset_chip_${preset.id}")
            ) {
              Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = if (isSelected) NeonAquaAccent else NeonCyanBright,
                    modifier = Modifier.size(13.dp)
                  )
                  Text(
                    text = preset.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) NeonAquaAccent else NeonCyanBright,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 0.25.sp
                  )
                }
                Text(
                  text = "${ClinicalDateFormatter.formatRangeFriendly(preset.startIso, preset.endIso)} • ${preset.yieldBadge}",
                  fontFamily = JetBrainsMonoFontFamily,
                  fontSize = 10.sp,
                  color = Color.White.copy(alpha = 0.85f)
                )
              }
            }
          }
        }
      }
    }

    // 2. Interactive Material 3 DateRangePicker Visual Calendar Card
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 3.dp,
      shadowElevation = 4.dp,
      border = BorderStroke(1.dp, NeonCyanText.copy(alpha = 0.32f)),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("m3_date_range_picker_card")
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        // Legend Bar for Holiday & Bridge Highlights
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(IosDarkDockSurface)
            .padding(horizontal = 14.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          LegendDotItem(color = BadgePlGold, label = "TN Public GH")
          LegendDotItem(color = BadgeColCyan, label = "Restricted RL")
          LegendDotItem(color = EmeraldOptimal, label = "Sat/Sun Free")
          LegendDotItem(color = NeonAquaAccent, label = "Bridge Range")
        }

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(440.dp)
            .testTag("m3_date_range_picker_widget")
        ) {
          DateRangePicker(
            state = dateRangePickerState,
            modifier = Modifier.fillMaxWidth(),
            title = {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(start = 16.dp, end = 16.dp, top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "SELECT LEAVE BRIDGE WINDOW (2026–2027)",
                  style = MaterialTheme.typography.labelSmall,
                  color = EmeraldOptimal,
                  fontWeight = FontWeight.ExtraBold,
                  letterSpacing = 0.35.sp
                )
                Text(
                  text = "ROI: $efficiencyRatio",
                  fontFamily = JetBrainsMonoFontFamily,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = NeonCyanText
                )
              }
            },
            headline = {
              val fmt = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
              val startLabel = selectedStartDate?.format(fmt) ?: "Start Date"
              val endLabel = selectedEndDate?.format(fmt) ?: "End Date"
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 6.dp)
              ) {
                Text(
                  text = "$startLabel  →  $endLabel",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.ExtraBold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "$totalDaysSelected Days Total • $freeHolidayOrWeekendDays Free (GH/Wknd) • $leaveCreditsRequired Leave Credits ($ghCountInRange GH, $rhCountInRange RL)",
                  style = MaterialTheme.typography.bodySmall,
                  color = EmeraldOptimal,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 11.sp
                )
              }
            },
            showModeToggle = true,
            colors = DatePickerDefaults.colors(
              selectedDayContainerColor = EmeraldOptimal,
              selectedDayContentColor = Color.White,
              dayInSelectionRangeContainerColor = NeonCyanText.copy(alpha = 0.22f),
              dayInSelectionRangeContentColor = MaterialTheme.colorScheme.onSurface,
              todayDateBorderColor = NeonAquaAccent,
              todayContentColor = EmeraldOptimal
            )
          )
        }
      }
    }

    // 3. Live Selected Range Holiday & Long-Weekend Breakdown + 1-Tap Apply
    if (dayInspections.isNotEmpty()) {
      Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(
          1.2.dp,
          if (exceedsTenDayCap) StatusAlertRed.copy(alpha = 0.5f) else EmeraldOptimal.copy(alpha = 0.4f)
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("m3_selected_range_breakdown_card")
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = if (exceedsTenDayCap) Icons.Default.WarningAmber else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (exceedsTenDayCap) StatusAlertRed else EmeraldOptimal,
                modifier = Modifier.size(20.dp)
              )
              Text(
                text = if (exceedsTenDayCap) {
                  "EXCEEDS TN 10-DAY CONTINUOUS ABSENCE CAP ($totalDaysSelected DAYS)"
                } else {
                  "TN LEAVE RULES 1933 COMPLIANT • $efficiencyRatio YIELD"
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (exceedsTenDayCap) StatusAlertRed else EmeraldOptimal,
                fontWeight = FontWeight.ExtraBold
              )
            }
          }

          // 4 Uniform Summary Pills
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            RangeSummaryMetricPill(
              label = "Total Off",
              value = "$totalDaysSelected Days",
              accent = EmeraldOptimal,
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
            )
            RangeSummaryMetricPill(
              label = "Free GH/Wknd",
              value = "$freeHolidayOrWeekendDays Days",
              accent = BadgePlGold,
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
            )
            RangeSummaryMetricPill(
              label = "Credits Used",
              value = "$leaveCreditsRequired Cr",
              accent = BadgeColCyan,
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
            )
            RangeSummaryMetricPill(
              label = "Efficiency",
              value = efficiencyRatio,
              accent = BadgeClGreen,
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
            )
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

          Text(
            text = "Day-by-Day Holiday & Bridge Splicing Breakdown",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
          )

          dayInspections.forEach { day ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(day.badgeColor.copy(alpha = 0.08f))
                .padding(horizontal = 10.dp, vertical = 7.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = ClinicalDateFormatter.formatFriendlyWithDay(day.dateIso),
                  fontFamily = JetBrainsMonoFontFamily,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = day.dayLabel,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  softWrap = false,
                  overflow = TextOverflow.Ellipsis
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = day.badgeColor.copy(alpha = 0.18f),
                border = BorderStroke(1.dp, day.badgeColor.copy(alpha = 0.4f))
              ) {
                Text(
                  text = day.badgeText,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = day.badgeColor,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            }
          }

          val workingLeaveDays = dayInspections.filter { !it.isFreeDay }
          if (workingLeaveDays.isNotEmpty() && !exceedsTenDayCap) {
            Button(
              onClick = {
                val clDates = workingLeaveDays
                  .filter { it.suggestedCategory != "RH" }
                  .map { it.dateIso }
                val rhDates = workingLeaveDays
                  .filter { it.suggestedCategory == "RH" }
                  .map { it.dateIso }

                val friendlyRange = if (selectedStartDate != null && selectedEndDate != null) {
                  ClinicalDateFormatter.formatRangeFriendly(selectedStartDate.toString(), selectedEndDate.toString())
                } else {
                  "$selectedStartDate → $selectedEndDate"
                }
                if (clDates.isNotEmpty()) {
                  onCommitWorkingDaysBatch(
                    LeaveCategory.CL,
                    clDates,
                    "M3 DateRangePicker Long-Weekend Bridge ($friendlyRange)"
                  )
                }
                if (rhDates.isNotEmpty()) {
                  onCommitWorkingDaysBatch(
                    LeaveCategory.RH,
                    rhDates,
                    "M3 DateRangePicker Restricted Holiday Bridge ($friendlyRange)"
                  )
                }
                onShowStatusBanner(
                  "Applied ${workingLeaveDays.size} bridge leave day(s) for $totalDaysSelected-day long weekend ($friendlyRange)!"
                )
              },
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(containerColor = EmeraldOptimal),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("m3_apply_selected_range_btn")
            ) {
              Icon(
                imageVector = Icons.Default.EventAvailable,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Apply ${workingLeaveDays.size} Bridge Leave(s) for $totalDaysSelected Days Off",
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }

    // 4. Upcoming Tamil Nadu Government Holidays & Long-Weekend Opportunities Radar
    Surface(
      shape = RoundedCornerShape(22.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 2.dp,
      border = BorderStroke(1.dp, BadgePlGold.copy(alpha = 0.35f)),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("m3_upcoming_holidays_radar_card")
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.CalendarMonth,
            contentDescription = null,
            tint = BadgePlGold,
            modifier = Modifier.size(20.dp)
          )
          Column {
            Text(
              text = "UPCOMING TN GOVT HOLIDAYS & LONG-WEEKEND RADAR",
              style = MaterialTheme.typography.labelMedium,
              color = BadgePlGold,
              fontWeight = FontWeight.ExtraBold
            )
            Text(
              text = "Tap any holiday opportunity below to highlight its optimal window on the Material 3 DateRangePicker",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        longWeekendPresets.forEach { preset ->
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .clickable {
                val s = LocalDate.parse(preset.startIso)
                val e = LocalDate.parse(preset.endIso)
                dateRangePickerState.setSelection(
                  startDateMillis = s.toUtcEpochMillis(),
                  endDateMillis = e.toUtcEpochMillis()
                )
                onShowStatusBanner(
                  "Highlighted ${preset.label} (${ClinicalDateFormatter.formatRangeFriendly(preset.startIso, preset.endIso)}) on M3 Calendar"
                )
              }
              .testTag("radar_holiday_row_${preset.id}")
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
                  text = preset.label,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = preset.holidayHighlight,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                  text = "Window: ${ClinicalDateFormatter.formatRangeFriendly(preset.startIso, preset.endIso)}",
                  fontFamily = JetBrainsMonoFontFamily,
                  fontSize = 11.sp,
                  color = EmeraldOptimal,
                  fontWeight = FontWeight.SemiBold
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = IosDarkDockSurface,
                border = BorderStroke(1.dp, NeonAquaAccent.copy(alpha = 0.5f))
              ) {
                Text(
                  text = preset.yieldBadge,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = NeonAquaAccent,
                  modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
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
private fun LegendDotItem(
  color: Color,
  label: String
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(5.dp)
  ) {
    Box(
      modifier = Modifier
        .size(8.dp)
        .clip(CircleShape)
        .background(color)
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = NeonCyanBright,
      fontSize = 10.sp,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      softWrap = false
    )
  }
}

@Composable
private fun RangeSummaryMetricPill(
  label: String,
  value: String,
  accent: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(12.dp),
    color = accent.copy(alpha = 0.12f),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
  ) {
    Column(
      modifier = Modifier
        .fillMaxHeight()
        .padding(horizontal = 6.dp, vertical = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = value,
        fontFamily = JetBrainsMonoFontFamily,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.ExtraBold,
        color = accent,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}
