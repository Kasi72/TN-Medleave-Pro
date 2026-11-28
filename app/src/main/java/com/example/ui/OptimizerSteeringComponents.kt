package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.LeaveCategory
import com.example.data.LeaveRecord
import com.example.data.TnHolidayItem
import com.example.domain.AiLeaveRecommendation
import com.example.domain.OptimizerConstraints
import com.example.domain.TnHolidays2027Catalog
import com.example.ui.theme.BadgeClGreen
import com.example.ui.theme.BadgeColCyan
import com.example.ui.theme.BadgePlGold
import com.example.ui.theme.BadgeRhSlate
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.IosSharpBadgeShape
import com.example.ui.theme.IosSharpButtonShape
import com.example.ui.theme.IosSharpCardShape
import com.example.ui.theme.IosSharpMicroShape
import com.example.ui.theme.IosSharpWellShape
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalFunctionalActionColors
import com.example.ui.theme.LocalNeumorphicColors
import com.example.ui.theme.StatusAlertRed
import com.example.ui.theme.StatusPendingOrange
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val PinIndigoAccent = Color(0xFF4F46E5)
private val ElVioletSteering = Color(0xFF7C3AED)
private val RhTealSteering = Color(0xFF0D9488)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SteeringOverridesAndRecalibrateBanner(
  constraints: OptimizerConstraints,
  isGeminiActive: Boolean,
  onClearAllOverrides: () -> Unit,
  onRecalibrate: () -> Unit
) {
  if (!constraints.hasSteeringOverrides && !constraints.hasPendingRecalc) return

  val neu = LocalNeumorphicColors.current
  val actions = LocalFunctionalActionColors.current
  val accent = if (constraints.hasPendingRecalc) actions.createPrimary else PinIndigoAccent

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .neumorphicInset(
        shape = IosSharpWellShape,
        depth = 2.5.dp,
        blurRadius = 5.dp,
        accentTint = accent,
        accentTintAlpha = if (constraints.hasPendingRecalc) 0.10f else 0.05f,
        borderColor = accent.copy(alpha = if (constraints.hasPendingRecalc) 0.48f else 0.28f),
        neu = neu
      )
      .padding(12.dp)
      .testTag("steering_overrides_banner")
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (constraints.hasPendingRecalc) {
            "STEERING UPDATED • RECALIBRATION READY"
          } else {
            "ACTIVE DATE STEERING OVERRIDES"
          },
          fontSize = 10.5.sp,
          fontWeight = FontWeight.ExtraBold,
          color = accent,
          letterSpacing = 0.4.sp,
          modifier = Modifier.weight(1f)
        )
        if (constraints.hasSteeringOverrides) {
          Box(
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .clip(IosSharpBadgeShape)
              .clickable { onClearAllOverrides() }
              .padding(horizontal = 8.dp, vertical = 6.dp)
              .testTag("steering_clear_all_btn"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "Clear All",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = StatusAlertRed
            )
          }
        }
      }

      if (constraints.hasSteeringOverrides) {
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          if (constraints.pinnedDates.isNotEmpty()) {
            SteeringCountPill(
              label = "PINNED: ${constraints.pinnedDates.size}",
              accent = PinIndigoAccent
            )
          }
          if (constraints.blockedDatesIso.isNotEmpty()) {
            SteeringCountPill(
              label = "BLOCKED: ${constraints.blockedDatesIso.size}",
              accent = StatusAlertRed
            )
          }
          if (constraints.plannedToWorkHolidays.isNotEmpty()) {
            SteeringCountPill(
              label = "WORK DUTY: +${constraints.plannedToWorkHolidays.size} VIRTUAL COL",
              accent = BadgeColCyan
            )
          }
        }
      }

      if (constraints.hasPendingRecalc) {
        Button(
          onClick = onRecalibrate,
          shape = IosSharpButtonShape,
          colors = ButtonDefaults.buttonColors(containerColor = actions.createPrimary),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .testTag("steering_recalibrate_btn")
        ) {
          Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (isGeminiActive) {
              "Recalibrate Strategy (Sync Gemini + Solver)"
            } else {
              "Recalibrate Strategy"
            },
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
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
private fun SteeringCountPill(
  label: String,
  accent: Color
) {
  val neu = LocalNeumorphicColors.current
  Box(
    modifier = Modifier
      .neumorphicExtruded(
        shape = IosSharpBadgeShape,
        elevation = 1.5.dp,
        blurRadius = 3.dp,
        accentTint = accent,
        accentTintAlpha = 0.08f,
        borderAlpha = 0.35f,
        neu = neu
      )
      .padding(horizontal = 8.dp, vertical = 4.dp)
  ) {
    Text(
      text = label,
      fontFamily = JetBrainsMonoFontFamily,
      fontSize = 9.5.sp,
      fontWeight = FontWeight.ExtraBold,
      color = accent,
      maxLines = 1,
      softWrap = false
    )
  }
}

@Composable
fun SteeringMonthCalendarCard(
  constraints: OptimizerConstraints,
  records: List<LeaveRecord>,
  recommendations: List<AiLeaveRecommendation>,
  selectedYearHint: Long,
  onSelectDateForSteering: (String) -> Unit
) {
  val neu = LocalNeumorphicColors.current
  val isoFmt = remember { DateTimeFormatter.ISO_LOCAL_DATE }
  var currentMonth by remember(selectedYearHint) {
    val today = LocalDate.now()
    val initYearMonth = when {
      selectedYearHint == 2027L -> YearMonth.of(2027, 1)
      today.year in 2026..2027 -> YearMonth.from(today)
      else -> YearMonth.of(2026, 11)
    }
    mutableStateOf(initYearMonth)
  }

  val ghMap = remember { TnHolidays2027Catalog.allSyncedPublicHolidays().associateBy { it.dateIso } }
  val rhMap = remember { TnHolidays2027Catalog.allSyncedRestrictedHolidays().associateBy { it.dateIso } }
  val recordsByDate = remember(records) { records.groupBy { it.dateIso } }
  val plannedMap = remember(recommendations) {
    val map = linkedMapOf<String, String>()
    recommendations.forEach { rec ->
      rec.dailyPlan.forEach { step ->
        if (!step.isAlreadyLogged && step.creditType.uppercase(Locale.US) in setOf("CL", "HALF_CL", "RH", "COL", "EL")) {
          map[step.dateIso] = step.creditType.uppercase(Locale.US)
        }
      }
    }
    map
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .neumorphicInset(
        shape = IosSharpWellShape,
        depth = 2.5.dp,
        blurRadius = 5.dp,
        accentTint = neu.primaryAccent,
        accentTintAlpha = 0.04f,
        borderColor = neu.primaryAccent.copy(alpha = 0.24f),
        neu = neu
      )
      .padding(12.dp)
      .testTag("steering_month_calendar_card")
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      // Month Navigator Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = {
            val prev = currentMonth.minusMonths(1)
            if (prev.year >= 2026) currentMonth = prev
          },
          enabled = currentMonth.isAfter(YearMonth.of(2026, 1)),
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("steering_cal_prev_month")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = "Previous month",
            tint = neu.primaryAccent
          )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${currentMonth.year}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = neu.textPrimary
          )
          Text(
            text = "Tap any date to Pin, Block, or Plan Work Duty (+1 COL)",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = neu.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        IconButton(
          onClick = {
            val next = currentMonth.plusMonths(1)
            if (next.year <= 2027) currentMonth = next
          },
          enabled = currentMonth.isBefore(YearMonth.of(2027, 12)),
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("steering_cal_next_month")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Next month",
            tint = neu.primaryAccent
          )
        }
      }

      // Weekday Headers (Mon..Sun)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEachIndexed { idx, label ->
          Text(
            text = label,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (idx == 6) StatusPendingOrange else neu.textSecondary
          )
        }
      }

      // 7-Column Month Grid
      val firstOfMonth = currentMonth.atDay(1)
      val leadingBlanks = (firstOfMonth.dayOfWeek.value - 1) // Mon=0 .. Sun=6
      val daysInMonth = currentMonth.lengthOfMonth()
      val totalCells = ((leadingBlanks + daysInMonth + 6) / 7) * 7

      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (rowStart in 0 until totalCells step 7) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            for (col in 0..6) {
              val cellIdx = rowStart + col
              val dayNum = cellIdx - leadingBlanks + 1
              if (dayNum < 1 || dayNum > daysInMonth) {
                Spacer(modifier = Modifier.weight(1f).height(48.dp))
              } else {
                val date = currentMonth.atDay(dayNum)
                val iso = date.format(isoFmt)
                val isGh = ghMap.containsKey(iso)
                val isRh = rhMap.containsKey(iso)
                val isWknd = constraints.isWeekendDay(date)
                val isPinned = constraints.pinnedDates[iso]
                val isBlocked = iso in constraints.blockedDatesIso
                val isWorkDuty = iso in constraints.plannedToWorkHolidays
                val isOpd = date.dayOfWeek in constraints.protectedDaysOfWeek || iso in constraints.protectedDatesIso
                val loggedCats = recordsByDate[iso].orEmpty()
                val plannedCat = plannedMap[iso]

                val cellAccent = when {
                  isBlocked -> StatusAlertRed
                  isWorkDuty -> BadgeColCyan
                  isPinned != null -> PinIndigoAccent
                  plannedCat != null -> EmeraldOptimal
                  loggedCats.isNotEmpty() -> BadgeClGreen
                  isGh -> BadgePlGold
                  isRh -> RhTealSteering
                  isWknd -> Color(0xFF64748B)
                  else -> neu.primaryAccent
                }

                val isHighlighted = isBlocked || isWorkDuty || isPinned != null || plannedCat != null || loggedCats.isNotEmpty() || isGh || isRh
                val tagBadge = when {
                  isBlocked -> "BLK"
                  isWorkDuty -> "+COL"
                  isPinned != null -> "PIN"
                  plannedCat != null -> if (plannedCat == "HALF_CL") "½CL" else plannedCat
                  loggedCats.isNotEmpty() -> loggedCats.first().category.shortLabel.take(3)
                  isGh -> "GH"
                  isRh -> "RH"
                  isWknd -> "OFF"
                  else -> ""
                }

                Box(
                  modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .then(
                      if (isBlocked || isPinned != null || isWorkDuty) {
                        Modifier.neumorphicInset(
                          shape = IosSharpMicroShape,
                          depth = 1.8.dp,
                          blurRadius = 3.dp,
                          accentTint = cellAccent,
                          accentTintAlpha = 0.16f,
                          borderColor = cellAccent.copy(alpha = 0.55f),
                          neu = neu
                        )
                      } else {
                        Modifier.neumorphicExtruded(
                          shape = IosSharpMicroShape,
                          elevation = if (isHighlighted) 1.8.dp else 1.dp,
                          blurRadius = 2.5.dp,
                          accentTint = cellAccent,
                          accentTintAlpha = if (isHighlighted) 0.08f else 0.01f,
                          borderAlpha = if (plannedCat != null) 0.45f else 0.18f,
                          neu = neu
                        )
                      }
                    )
                    .clickable { onSelectDateForSteering(iso) }
                    .padding(horizontal = 2.dp, vertical = 4.dp)
                    .testTag("steering_day_$iso"),
                  contentAlignment = Alignment.Center
                ) {
                  if (isOpd) {
                    Box(
                      modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                        .size(4.5.dp)
                        .clip(CircleShape)
                        .background(StatusPendingOrange)
                    )
                  }
                  Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                  ) {
                    Text(
                      text = dayNum.toString(),
                      fontFamily = JetBrainsMonoFontFamily,
                      fontSize = 11.sp,
                      fontWeight = if (isHighlighted) FontWeight.ExtraBold else FontWeight.SemiBold,
                      color = if (isBlocked) StatusAlertRed else neu.textPrimary,
                      textDecoration = if (isBlocked) TextDecoration.LineThrough else null,
                      maxLines = 1,
                      softWrap = false
                    )
                    if (tagBadge.isNotEmpty()) {
                      Text(
                        text = tagBadge,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isGh && !neu.isDark) Color(0xFFB45309) else cellAccent,
                        maxLines = 1,
                        softWrap = false
                      )
                    } else {
                      Spacer(modifier = Modifier.height(9.dp))
                    }
                  }
                }
              }
            }
          }
        }
      }

      // Legend Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
      ) {
        CalendarLegendDot("GH", BadgePlGold)
        CalendarLegendDot("RH", RhTealSteering)
        CalendarLegendDot("PLAN", EmeraldOptimal)
        CalendarLegendDot("PIN", PinIndigoAccent)
        CalendarLegendDot("BLK", StatusAlertRed)
        CalendarLegendDot("+COL", BadgeColCyan)
      }
    }
  }
}

@Composable
private fun CalendarLegendDot(label: String, color: Color) {
  val neu = LocalNeumorphicColors.current
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(3.dp)
  ) {
    Box(
      modifier = Modifier
        .size(6.dp)
        .clip(CircleShape)
        .background(color)
    )
    Text(
      text = label,
      fontFamily = JetBrainsMonoFontFamily,
      fontSize = 8.5.sp,
      fontWeight = FontWeight.Bold,
      color = neu.textSecondary
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OptimizerDaySteeringDialog(
  dateIso: String,
  constraints: OptimizerConstraints,
  recordsOnDate: List<LeaveRecord>,
  onSetPinnedDate: (dateIso: String, pinType: String?) -> Unit,
  onToggleBlockedDate: (String) -> Unit,
  onTogglePlannedWorkHoliday: (String) -> Unit,
  onDismiss: () -> Unit
) {
  val neu = LocalNeumorphicColors.current
  val localDate = remember(dateIso) { runCatching { LocalDate.parse(dateIso) }.getOrNull() }
  val ghItem: TnHolidayItem? = remember(dateIso) {
    TnHolidays2027Catalog.allSyncedPublicHolidays().firstOrNull { it.dateIso == dateIso }
  }
  val rhItem: TnHolidayItem? = remember(dateIso) {
    TnHolidays2027Catalog.allSyncedRestrictedHolidays().firstOrNull { it.dateIso == dateIso }
  }

  val isWeekend = localDate?.let { constraints.isWeekendDay(it) } == true
  val isOffBaseline = ghItem != null || isWeekend
  val isBlocked = dateIso in constraints.blockedDatesIso
  val currentPin = constraints.pinnedDates[dateIso]
  val isPlannedWorkHoliday = dateIso in constraints.plannedToWorkHolidays
  val isOpdProtected = localDate != null &&
    (localDate.dayOfWeek in constraints.protectedDaysOfWeek || dateIso in constraints.protectedDatesIso)
  val hasExistingLeave = recordsOnDate.any {
    it.category in setOf(LeaveCategory.CL, LeaveCategory.RH, LeaveCategory.COL, LeaveCategory.PL, LeaveCategory.EL, LeaveCategory.ML)
  }
  val hasExistingGhWorked = recordsOnDate.any { it.category == LeaveCategory.GH_WORKED }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 24.dp)
        .neumorphicExtruded(
          shape = IosSharpCardShape,
          elevation = 8.dp,
          blurRadius = 16.dp,
          accentTint = neu.primaryAccent,
          accentTintAlpha = 0.05f,
          borderAlpha = 0.35f,
          neu = neu
        )
        .padding(16.dp)
        .testTag("optimizer_day_steering_dialog")
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(
              imageVector = Icons.Default.CalendarMonth,
              contentDescription = null,
              tint = neu.primaryAccent,
              modifier = Modifier.size(20.dp)
            )
            Column {
              Text(
                text = ClinicalDateFormatter.formatFriendlyWithDay(dateIso),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = neu.textPrimary
              )
              Text(
                text = dateIso,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                color = neu.textSecondary
              )
            }
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.minimumInteractiveComponentSize()
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close day steering dialog",
              tint = neu.textSecondary
            )
          }
        }

        // Day Context Chips
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          if (ghItem != null) {
            SteeringCountPill("TN GH: ${ghItem.nameEnglish.substringBefore(" (")}", BadgePlGold)
          }
          if (rhItem != null) {
            SteeringCountPill("RH: ${rhItem.nameEnglish.substringBefore(" (")}", RhTealSteering)
          }
          if (isWeekend) {
            val wkLabel = if (localDate?.dayOfWeek == DayOfWeek.SATURDAY) "2nd/Wknd Sat Off" else "Sunday Off"
            SteeringCountPill(wkLabel, Color(0xFF64748B))
          }
          if (isOpdProtected) {
            SteeringCountPill("PROTECTED OPD / ROSTER", StatusPendingOrange)
          }
          recordsOnDate.forEach { rec ->
            SteeringCountPill("LOGGED: ${rec.category.shortLabel}", BadgeClGreen)
          }
          if (currentPin != null) {
            SteeringCountPill("PINNED: $currentPin", PinIndigoAccent)
          }
          if (isBlocked) {
            SteeringCountPill("BLOCKED WORKDAY", StatusAlertRed)
          }
          if (isPlannedWorkHoliday) {
            SteeringCountPill("WORK DUTY (+1 VIRTUAL COL)", BadgeColCyan)
          }
        }

        HorizontalDivider(color = neu.hairlineDivider)

        // Section 1: Pin Working Day as Specific Leave Type
        if (!isOffBaseline && !hasExistingLeave) {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                imageVector = Icons.Default.PushPin,
                contentDescription = null,
                tint = PinIndigoAccent,
                modifier = Modifier.size(15.dp)
              )
              Text(
                text = "PIN DATE IN OPTIMIZER PORTFOLIO",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PinIndigoAccent
              )
            }
            val pinOptions = buildList {
              add("AUTO" to "Auto")
              add("CL" to "CL")
              add("HALF_CL" to "½ CL")
              if (rhItem != null) add("RH" to "RH")
              add("COL" to "COL")
              add("EL" to "EL")
            }
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              pinOptions.forEach { (code, label) ->
                val active = currentPin == code
                val chipColor = when (code) {
                  "CL" -> BadgeClGreen
                  "HALF_CL" -> EmeraldOptimal
                  "RH" -> RhTealSteering
                  "COL" -> BadgeColCyan
                  "EL" -> ElVioletSteering
                  else -> PinIndigoAccent
                }
                Box(
                  modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .then(
                      if (active) {
                        Modifier.neumorphicExtruded(
                          shape = IosSharpMicroShape,
                          elevation = 2.dp,
                          blurRadius = 4.dp,
                          surfaceColor = chipColor,
                          neu = neu
                        )
                      } else {
                        Modifier.neumorphicExtruded(
                          shape = IosSharpMicroShape,
                          elevation = 1.5.dp,
                          blurRadius = 3.dp,
                          borderAlpha = 0.28f,
                          neu = neu
                        )
                      }
                    )
                    .clickable {
                      onSetPinnedDate(dateIso, if (active) null else code)
                      onDismiss()
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("steer_pin_${code}_$dateIso"),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = if (active) "✓ $label" else "Pin $label",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (active) Color.White else chipColor,
                    maxLines = 1,
                    softWrap = false
                  )
                }
              }
              if (currentPin != null) {
                Box(
                  modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .neumorphicInset(
                      shape = IosSharpMicroShape,
                      depth = 1.8.dp,
                      blurRadius = 3.dp,
                      accentTint = StatusAlertRed,
                      accentTintAlpha = 0.12f,
                      borderColor = StatusAlertRed.copy(alpha = 0.45f),
                      neu = neu
                    )
                    .clickable {
                      onSetPinnedDate(dateIso, null)
                      onDismiss()
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("steer_unpin_$dateIso"),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = "Unpin",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = StatusAlertRed
                  )
                }
              }
            }
          }
        }

        // Section 2: Block Workday from Leave Planning
        if (!isOffBaseline && !hasExistingLeave) {
          OutlinedButton(
            onClick = {
              onToggleBlockedDate(dateIso)
              onDismiss()
            },
            shape = IosSharpButtonShape,
            border = androidx.compose.foundation.BorderStroke(1.2.dp, StatusAlertRed),
            modifier = Modifier
              .fillMaxWidth()
              .minimumInteractiveComponentSize()
              .testTag("steer_block_toggle_$dateIso")
          ) {
            Icon(
              imageVector = Icons.Default.Block,
              contentDescription = null,
              tint = StatusAlertRed,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (isBlocked) "Unblock This Date (Allow Leave)" else "Block Date (Clinical Duty / Never Plan Leave)",
              fontWeight = FontWeight.ExtraBold,
              color = StatusAlertRed,
              fontSize = 12.sp
            )
          }
        }

        // Section 3: Plan Work Duty on Holiday / Weekend -> +1 Virtual COL
        if ((isOffBaseline || rhItem != null) && !hasExistingGhWorked) {
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Button(
              onClick = {
                onTogglePlannedWorkHoliday(dateIso)
                onDismiss()
              },
              shape = IosSharpButtonShape,
              colors = ButtonDefaults.buttonColors(
                containerColor = if (isPlannedWorkHoliday) StatusAlertRed else BadgeColCyan
              ),
              modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("steer_virtual_col_toggle_$dateIso")
            ) {
              Icon(
                imageVector = Icons.Default.MedicalServices,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (isPlannedWorkHoliday) {
                  "Cancel Planned Work Duty (-1 Virtual COL)"
                } else {
                  "Mark as Work Duty (+1 Virtual COL)"
                },
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                fontSize = 12.sp
              )
            }
            Text(
              text = "Virtual COL unlocks a 180-day compensatory off credit in the solver and logs GH Worked + COL + In Lieu atomically when you apply the bridge.",
              style = MaterialTheme.typography.labelSmall,
              color = neu.textSecondary
            )
          }
        }

        if (hasExistingLeave || hasExistingGhWorked) {
          Text(
            text = "This date already has a saved Firestore record. Edit or remove it in the Leave Ledger tab.",
            style = MaterialTheme.typography.labelSmall,
            color = neu.textSecondary
          )
        }
      }
    }
  }
}
