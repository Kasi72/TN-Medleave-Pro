package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.Timeline
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LeaveCategory
import com.example.data.LeaveRecord
import com.example.ui.theme.BadgeClGreen
import com.example.ui.theme.BadgeColCyan
import com.example.ui.theme.BadgeElNeutral
import com.example.ui.theme.BadgeGhLime
import com.example.ui.theme.BadgeInLieuLavender
import com.example.ui.theme.BadgeMlOrange
import com.example.ui.theme.BadgeOnDutyBlue
import com.example.ui.theme.BadgePlGold
import com.example.ui.theme.BadgeRhSlate
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlin.math.roundToInt

data class MonthlyLeavePoint(
  val monthIndex: Int, // 1..12
  val monthLabel: String,
  val clDays: Double,
  val colDays: Double,
  val rhDays: Double,
  val plDays: Double,
  val elMlDays: Double,
  val ghWorkedCreditsEarned: Double,
  val totalLeavesTaken: Double,
  val availableCreditsRemaining: Double
)

@Composable
fun PsychiatryLeaveAnalyticsSection(
  state: LeaveUiState.Success,
  modifier: Modifier = Modifier
) {
  val selectedYear = state.selectedYear
  val yearRecords = remember(state.allRecords, selectedYear) {
    state.allRecords.filter { it.year == selectedYear }
  }

  val monthlySeries = remember(yearRecords, selectedYear) {
    computeMonthlySeriesForYear(yearRecords, selectedYear)
  }

  var selectedMonthIdx by rememberSaveable(selectedYear) {
    val activeMonth = monthlySeries.lastOrNull { it.totalLeavesTaken > 0 || it.ghWorkedCreditsEarned > 0 }?.monthIndex ?: 10
    mutableIntStateOf(activeMonth)
  }

  val animProgress by animateFloatAsState(
    targetValue = 1f,
    animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
    label = "chart_anim_progress"
  )

  val inspectedMonth = monthlySeries.getOrNull((selectedMonthIdx - 1).coerceIn(0, 11)) ?: monthlySeries.first()

  Column(
    modifier = modifier
      .fillMaxWidth()
      .testTag("psychiatry_analytics_dashboard"),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 1. Primary Monthly Leaves Taken vs. Available Credits Interactive Chart Card
    Surface(
      shape = RoundedCornerShape(26.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 2.dp,
      shadowElevation = 3.dp,
      modifier = Modifier
        .fillMaxWidth()
        .border(
          1.dp,
          MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
          RoundedCornerShape(26.dp)
        )
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        // Header Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(EmeraldOptimal.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Analytics,
                contentDescription = null,
                tint = EmeraldOptimal,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "DEPT. OF PSYCHIATRY • VISUAL ANALYTICS",
                style = MaterialTheme.typography.labelMedium,
                color = EmeraldOptimal,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = "Monthly Leaves vs. Available Credits ($selectedYear)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
              )
            }
          }

          Spacer(modifier = Modifier.width(8.dp))

          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primaryContainer
          ) {
            Text(
              text = "${yearRecords.size} Records",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              maxLines = 1,
              softWrap = false,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Chart Legend Strip
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          ChartLegendChip(label = "Leaves Taken (CL/COL/RH/EL)", color = BadgeClGreen)
          ChartLegendChip(label = "GH Worked (+COL Credit)", color = BadgeGhLime)
          ChartLegendChip(label = "Available Credits Curve", color = Color(0xFF0284C7), isLine = true)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Interactive Canvas Composed Bar + Smooth Bezier Area Line Chart
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(215.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f))
            .padding(horizontal = 12.dp, vertical = 14.dp)
            .testTag("monthly_leaves_vs_credits_canvas")
        ) {
          val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.16f)
          val selectedHighlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
          val creditLineColor = Color(0xFF0284C7)

          Canvas(
            modifier = Modifier
              .fillMaxSize()
              .pointerInput(monthlySeries) {
                detectTapGestures { tapOffset ->
                  val slotWidth = size.width / 12f
                  val tappedIndex = (tapOffset.x / slotWidth).toInt().coerceIn(0, 11)
                  selectedMonthIdx = tappedIndex + 1
                }
              }
          ) {
            val chartWidth = size.width
            val chartHeight = size.height - 24.dp.toPx() // Reserve bottom 24.dp for Jan..Dec labels
            val slotWidth = chartWidth / 12f

            val maxScaleValue = maxOf(
              16f,
              monthlySeries.maxOfOrNull {
                maxOf(
                  it.totalLeavesTaken.toFloat(),
                  it.availableCreditsRemaining.toFloat(),
                  it.ghWorkedCreditsEarned.toFloat()
                )
              } ?: 16f
            )

            // Draw 4 horizontal reference grid lines
            for (step in 0..3) {
              val y = chartHeight * (step / 3f)
              drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(chartWidth, y),
                strokeWidth = 1.dp.toPx()
              )
            }

            // Highlight selected month column band
            val selIdx = (selectedMonthIdx - 1).coerceIn(0, 11)
            drawRoundRect(
              color = selectedHighlightColor,
              topLeft = Offset(selIdx * slotWidth + 2.dp.toPx(), 0f),
              size = Size(slotWidth - 4.dp.toPx(), chartHeight),
              cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )

            // 1. Draw paired bars for each month: [Total Leaves Taken] and [GH Worked Credits]
            monthlySeries.forEachIndexed { idx, pt ->
              val centerX = idx * slotWidth + slotWidth / 2f
              val barWidth = (slotWidth * 0.30f).coerceAtLeast(5.dp.toPx())

              // Bar A: Leaves Taken (Stacked visual or emerald/green bar)
              val leaveRatio = (pt.totalLeavesTaken.toFloat() / maxScaleValue).coerceIn(0f, 1f) * animProgress
              val leaveBarH = (chartHeight * leaveRatio).coerceAtLeast(if (pt.totalLeavesTaken > 0) 4.dp.toPx() else 0f)
              if (leaveBarH > 0f) {
                drawRoundRect(
                  color = BadgeClGreen,
                  topLeft = Offset(centerX - barWidth - 1.5.dp.toPx(), chartHeight - leaveBarH),
                  size = Size(barWidth, leaveBarH),
                  cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
              }

              // Bar B: GH Worked / Duty Credits Earned
              val ghRatio = (pt.ghWorkedCreditsEarned.toFloat() / maxScaleValue).coerceIn(0f, 1f) * animProgress
              val ghBarH = (chartHeight * ghRatio).coerceAtLeast(if (pt.ghWorkedCreditsEarned > 0) 4.dp.toPx() else 0f)
              if (ghBarH > 0f) {
                drawRoundRect(
                  color = BadgeGhLime,
                  topLeft = Offset(centerX + 1.5.dp.toPx(), chartHeight - ghBarH),
                  size = Size(barWidth, ghBarH),
                  cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
              }
            }

            // 2. Draw Smooth Bezier Curve for Available Remaining Credits (CL + RH + Unused GH/COL)
            val linePoints = monthlySeries.mapIndexed { idx, pt ->
              val x = idx * slotWidth + slotWidth / 2f
              val ratio = (pt.availableCreditsRemaining.toFloat() / maxScaleValue).coerceIn(0f, 1f) * animProgress
              val y = chartHeight - (chartHeight * ratio)
              Offset(x, y)
            }

            if (linePoints.size >= 2) {
              val smoothPath = Path().apply {
                moveTo(linePoints.first().x, linePoints.first().y)
                for (i in 0 until linePoints.size - 1) {
                  val p0 = linePoints[i]
                  val p1 = linePoints[i + 1]
                  val controlX1 = (p0.x + p1.x) / 2f
                  cubicTo(controlX1, p0.y, controlX1, p1.y, p1.x, p1.y)
                }
              }

              val fillPath = Path().apply {
                addPath(smoothPath)
                lineTo(linePoints.last().x, chartHeight)
                lineTo(linePoints.first().x, chartHeight)
                close()
              }

              drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                  colors = listOf(
                    creditLineColor.copy(alpha = 0.24f),
                    creditLineColor.copy(alpha = 0.02f)
                  ),
                  startY = 0f,
                  endY = chartHeight
                )
              )

              drawPath(
                path = smoothPath,
                color = creditLineColor,
                style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
              )

              linePoints.forEachIndexed { idx, offset ->
                val isSelectedNode = idx == selIdx
                drawCircle(
                  color = Color.White,
                  radius = if (isSelectedNode) 6.dp.toPx() else 4.dp.toPx(),
                  center = offset
                )
                drawCircle(
                  color = if (isSelectedNode) EmeraldOptimal else creditLineColor,
                  radius = if (isSelectedNode) 4.2.dp.toPx() else 2.6.dp.toPx(),
                  center = offset
                )
              }
            }
          }

          // Bottom Month Labels Row (Jan .. Dec)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            monthlySeries.forEach { pt ->
              val isSel = pt.monthIndex == selectedMonthIdx
              Box(
                modifier = Modifier
                  .weight(1f)
                  .clickable { selectedMonthIdx = pt.monthIndex }
                  .testTag("chart_month_selector_${pt.monthIndex}"),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = pt.monthLabel,
                  fontSize = 9.5.sp,
                  lineHeight = 12.sp,
                  fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSel) EmeraldOptimal else MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  softWrap = false
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Interactive Month Tooltip Callout Card (for the selected month)
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "${inspectedMonth.monthLabel} $selectedYear • Monthly Breakdown",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Tap any month bar to inspect",
                fontSize = 10.sp,
                lineHeight = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false
              )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              MiniTooltipMetricBox(
                title = "Leaves Taken",
                value = "${formatDoubleDays(inspectedMonth.totalLeavesTaken)}d",
                sub = "CL:${formatDoubleDays(inspectedMonth.clDays)} COL:${inspectedMonth.colDays.roundToInt()} RH:${inspectedMonth.rhDays.roundToInt()}",
                accent = BadgeClGreen,
                modifier = Modifier
                  .weight(1f)
                  .fillMaxHeight()
              )
              MiniTooltipMetricBox(
                title = "GH Worked",
                value = "+${inspectedMonth.ghWorkedCreditsEarned.roundToInt()}d",
                sub = "PL Off: ${inspectedMonth.plDays.roundToInt()}d",
                accent = BadgeGhLime,
                modifier = Modifier
                  .weight(1f)
                  .fillMaxHeight()
              )
              MiniTooltipMetricBox(
                title = "Credits Left",
                value = "${formatDoubleDays(inspectedMonth.availableCreditsRemaining)}d",
                sub = "CL+RH+COL Pool",
                accent = Color(0xFF0284C7),
                modifier = Modifier
                  .weight(1f)
                  .fillMaxHeight()
              )
            }
          }
        }
      }
    }

    // 2. Department of Psychiatry Category Distribution Donut + Proportion Bars
    CategoryDistributionDonutCard(
      yearRecords = yearRecords,
      selectedYear = selectedYear
    )

    // 3. Multi-Year (2023–2026) Psychiatry HOD Leave vs. Holiday Duty Velocity Card
    MultiYearComparisonChartCard(
      allRecords = state.allRecords,
      selectedYear = selectedYear
    )
  }
}

@Composable
private fun MiniTooltipMetricBox(
  title: String,
  value: String,
  sub: String,
  accent: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surface,
    modifier = modifier.border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 9.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
      Text(
        text = title,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = value,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = JetBrainsMonoFontFamily,
        color = accent,
        maxLines = 1,
        softWrap = false
      )
      Text(
        text = sub,
        fontSize = 9.5.sp,
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
private fun CategoryDistributionDonutCard(
  yearRecords: List<LeaveRecord>,
  selectedYear: Long
) {
  val slices = remember(yearRecords) {
    val trackedCategories = listOf(
      LeaveCategory.CL to BadgeClGreen,
      LeaveCategory.COL to BadgeColCyan,
      LeaveCategory.PL to BadgePlGold,
      LeaveCategory.GH_WORKED to BadgeGhLime,
      LeaveCategory.ML to BadgeMlOrange,
      LeaveCategory.RH to BadgeRhSlate,
      LeaveCategory.EL to BadgeElNeutral,
      LeaveCategory.ON_DUTY to BadgeOnDutyBlue
    )
    trackedCategories.mapNotNull { (cat, color) ->
      val sum = yearRecords.filter { it.category == cat }.sumOf { it.dayValue }
      if (sum > 0.0) Triple(cat, sum, color) else null
    }
  }

  val totalDays = slices.sumOf { it.second }.coerceAtLeast(1.0)

  Surface(
    shape = RoundedCornerShape(26.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.DonutLarge,
          contentDescription = null,
          tint = BadgeColCyan,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Leave & Holiday Duty Mix ($selectedYear)",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.weight(1f),
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Radial Donut Ring Chart
        Box(
          modifier = Modifier.size(132.dp),
          contentAlignment = Alignment.Center
        ) {
          Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            var startAngle = -90f
            val strokeW = 18.dp.toPx()
            slices.forEach { (_, count, color) ->
              val sweep = ((count / totalDays) * 360.0).toFloat()
              drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = (sweep - 2.5f).coerceAtLeast(2f),
                useCenter = false,
                style = Stroke(width = strokeW, cap = StrokeCap.Butt)
              )
              startAngle += sweep
            }
          }
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = formatDoubleDays(slices.sumOf { it.second }),
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 20.sp,
              lineHeight = 22.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Total Days",
              fontSize = 10.sp,
              lineHeight = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Category Legend & Proportional Bars
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          slices.forEach { (cat, count, color) ->
            val pct = ((count / totalDays) * 100).roundToInt()
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = cat.shortLabel,
                  fontSize = 12.sp,
                  lineHeight = 15.sp,
                  fontWeight = FontWeight.SemiBold,
                  maxLines = 1,
                  softWrap = false,
                  overflow = TextOverflow.Ellipsis
                )
              }
              Text(
                text = "${formatDoubleDays(count)}d ($pct%)",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color,
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

@Composable
private fun MultiYearComparisonChartCard(
  allRecords: List<LeaveRecord>,
  selectedYear: Long
) {
  val years = listOf(2023L, 2024L, 2025L, 2026L)
  val yearStats = remember(allRecords) {
    years.map { yr ->
      val recs = allRecords.filter { it.year == yr }
      val clColRh = recs.filter {
        it.category in listOf(LeaveCategory.CL, LeaveCategory.COL, LeaveCategory.RH)
      }.sumOf { it.dayValue }
      val ghAndInLieu = recs.filter {
        it.category in listOf(LeaveCategory.GH_WORKED, LeaveCategory.IN_LIEU)
      }.sumOf { it.dayValue }
      val plDays = recs.filter { it.category == LeaveCategory.PL }.sumOf { it.dayValue }
      Triple(yr, clColRh + plDays, ghAndInLieu)
    }
  }

  val maxVal = (yearStats.maxOfOrNull { maxOf(it.second, it.third) } ?: 30.0).coerceAtLeast(10.0)

  Surface(
    shape = RoundedCornerShape(26.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.BarChart,
          contentDescription = null,
          tint = BadgeInLieuLavender,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "2023–2026 Multi-Year Leave & GH Duty Trend",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.weight(1f),
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      yearStats.forEach { (yr, leavesTotal, dutyTotal) ->
        val isSelectedYear = yr == selectedYear
        val leaveFraction = (leavesTotal / maxVal).toFloat().coerceIn(0.05f, 1f)
        val dutyFraction = (dutyTotal / maxVal).toFloat().coerceIn(0.05f, 1f)

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
              if (isSelectedYear) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
              } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
              }
            )
            .padding(10.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Year $yr",
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              lineHeight = 15.sp,
              color = if (isSelectedYear) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Leaves + PL: ${formatDoubleDays(leavesTotal)}d • GH/In-Lieu Duty: ${formatDoubleDays(dutyTotal)}d",
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 11.sp,
              lineHeight = 14.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              softWrap = false
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          Box(
            modifier = Modifier
              .fillMaxWidth(leaveFraction)
              .height(7.dp)
              .clip(RoundedCornerShape(4.dp))
              .background(EmeraldOptimal)
          )
          Spacer(modifier = Modifier.height(4.dp))
          Box(
            modifier = Modifier
              .fillMaxWidth(dutyFraction)
              .height(7.dp)
              .clip(RoundedCornerShape(4.dp))
              .background(BadgeGhLime)
          )
        }
      }
    }
  }
}

@Composable
private fun ChartLegendChip(
  label: String,
  color: Color,
  isLine: Boolean = false
) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    if (isLine) {
      Box(
        modifier = Modifier
          .width(14.dp)
          .height(3.dp)
          .clip(RoundedCornerShape(2.dp))
          .background(color)
      )
    } else {
      Box(
        modifier = Modifier
          .size(9.dp)
          .clip(RoundedCornerShape(3.dp))
          .background(color)
      )
    }
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = label,
      fontSize = 11.sp,
      lineHeight = 14.sp,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
      softWrap = false
    )
  }
}

private fun computeMonthlySeriesForYear(
  yearRecords: List<LeaveRecord>,
  year: Long
): List<MonthlyLeavePoint> {
  val shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
  // Base annual pool: 12 CL + 3 RH = 15.0 days, plus cumulative GH Worked credits minus COL taken
  var runningClRemaining = 12.0
  var runningRhRemaining = 3.0
  var runningColPool = 0.0

  return shortMonths.mapIndexed { idx, label ->
    val monthNum = idx + 1
    val prefix = String.format("%04d-%02d", year, monthNum)
    val monthRecs = yearRecords.filter { it.dateIso.startsWith(prefix) }

    val cl = monthRecs.filter { it.category == LeaveCategory.CL }.sumOf { it.dayValue }
    val col = monthRecs.filter { it.category == LeaveCategory.COL }.sumOf { it.dayValue }
    val rh = monthRecs.filter { it.category == LeaveCategory.RH }.sumOf { it.dayValue }
    val pl = monthRecs.filter { it.category == LeaveCategory.PL }.sumOf { it.dayValue }
    val elMl = monthRecs.filter { it.category == LeaveCategory.EL || it.category == LeaveCategory.ML }.sumOf { it.dayValue }
    val ghWorked = monthRecs.filter { it.category == LeaveCategory.GH_WORKED }.sumOf { it.dayValue }

    runningClRemaining = (runningClRemaining - cl).coerceAtLeast(0.0)
    runningRhRemaining = (runningRhRemaining - rh).coerceAtLeast(0.0)
    runningColPool = (runningColPool + ghWorked - col).coerceAtLeast(0.0)

    val totalLeavesInMonth = cl + col + rh + elMl
    val availableCreditsLeft = runningClRemaining + runningRhRemaining + runningColPool

    MonthlyLeavePoint(
      monthIndex = monthNum,
      monthLabel = label,
      clDays = cl,
      colDays = col,
      rhDays = rh,
      plDays = pl,
      elMlDays = elMl,
      ghWorkedCreditsEarned = ghWorked,
      totalLeavesTaken = totalLeavesInMonth,
      availableCreditsRemaining = availableCreditsLeft
    )
  }
}
