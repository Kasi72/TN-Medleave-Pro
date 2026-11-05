package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ColGhAuditSummary
import com.example.domain.YearCreditSummary
import com.example.ui.theme.BadgeClGreen
import com.example.ui.theme.BadgeColCyan
import com.example.ui.theme.BadgePlGold
import com.example.ui.theme.BadgeRhSlate
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StatusAlertRed
import com.example.ui.theme.StatusPendingOrange
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

enum class LeaveGaugeFocusMode(
  val code: String,
  val label: String,
  val ruleNote: String
) {
  TOTAL_POOL(
    code = "ALL",
    label = "Total Credit Pool",
    ruleNote = "Combined Annual Statutory CL (12) + RH (3) + Earned Duty Credits (In-Lieu & GH Worked)"
  ),
  CASUAL_CL(
    code = "CL",
    label = "Casual Leave (CL)",
    ruleNote = "12.0 days annual entitlement under TN Leave Rules; max 10 days continuous absence"
  ),
  RESTRICTED_RH(
    code = "RH",
    label = "Restricted (RH)",
    ruleNote = "3.0 optional festival holidays per calendar year from 31 notified TN Govt options"
  ),
  COMP_OFF_COL(
    code = "COL",
    label = "Comp-Off & GH Pool",
    ruleNote = "Earned 1-to-1 from special/turn duty & Holiday Duty (GH Worked) at GEMC&H"
  )
}

private data class GaugeMetrics(
  val title: String,
  val shortCode: String,
  val totalEntitlement: Double,
  val usedCredits: Double,
  val remainingCredits: Double,
  val remainingFraction: Float,
  val usedFraction: Float,
  val primaryColor: Color,
  val secondaryColor: Color,
  val usedColor: Color,
  val statusLabel: String,
  val footnote: String
)

/**
 * Compact reusable circular progress ring for hero pills, bento cards, and sub-pool summaries.
 */
@Composable
fun MiniCircularQuotaRing(
  remainingFraction: Float,
  accentColor: Color,
  modifier: Modifier = Modifier,
  usedColor: Color = Color(0xFFF59E0B).copy(alpha = 0.42f),
  trackColor: Color = Color.White.copy(alpha = 0.12f),
  size: Dp = 42.dp,
  strokeWidth: Dp = 4.5.dp,
  centerText: String? = null,
  centerTextColor: Color = accentColor
) {
  val clampedRemaining = remainingFraction.coerceIn(0f, 1f)
  val animatedRemaining by animateFloatAsState(
    targetValue = clampedRemaining,
    animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
    label = "mini_ring_remaining"
  )

  Box(
    modifier = modifier.size(size),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val strokePx = strokeWidth.toPx()
      val diameter = this.size.minDimension - strokePx
      val topLeft = Offset(strokePx / 2f, strokePx / 2f)
      val arcSize = Size(diameter, diameter)

      // 1. Base Track
      drawArc(
        color = trackColor,
        startAngle = -90f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = strokePx, cap = StrokeCap.Round)
      )

      // 2. Used Portion Arc (clockwise from end of remaining arc)
      val usedFraction = (1f - animatedRemaining).coerceIn(0f, 1f)
      if (usedFraction > 0.02f) {
        val startUsed = -90f + (animatedRemaining * 360f)
        drawArc(
          color = usedColor,
          startAngle = startUsed,
          sweepAngle = (usedFraction * 360f).coerceAtLeast(4f),
          useCenter = false,
          topLeft = topLeft,
          size = arcSize,
          style = Stroke(width = strokePx * 0.82f, cap = StrokeCap.Butt)
        )
      }

      // 3. Remaining Balance Arc
      if (animatedRemaining > 0.01f) {
        drawArc(
          brush = Brush.sweepGradient(
            colors = listOf(
              accentColor.copy(alpha = 0.85f),
              accentColor,
              accentColor.copy(alpha = 0.95f)
            )
          ),
          startAngle = -90f,
          sweepAngle = animatedRemaining * 360f,
          useCenter = false,
          topLeft = topLeft,
          size = arcSize,
          style = Stroke(width = strokePx, cap = StrokeCap.Round)
        )
      }
    }

    if (centerText != null) {
      Text(
        text = centerText,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        color = centerTextColor,
        maxLines = 1,
        softWrap = false
      )
    }
  }
}

/**
 * Enterprise Circular Progress Indicator Card visualizing Remaining Leave Balance vs. Used Leave Credits
 * with multi-ring concentric telemetry, interactive category lens, and tap-to-flip center readout.
 */
@Composable
fun EnterpriseLeaveBalanceCircularCard(
  summary: YearCreditSummary,
  colGhAudit: ColGhAuditSummary,
  onNavigateToLedger: () -> Unit,
  onNavigateToAudit: () -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedFocus by remember { mutableStateOf(LeaveGaugeFocusMode.TOTAL_POOL) }
  var showUsedInCenter by remember { mutableStateOf(false) }

  // Compute pool metrics for each category
  val clQuota = summary.clQuota.toDouble()
  val clUsed = summary.clTaken.coerceAtLeast(0.0)
  val clRem = summary.clRemaining.coerceAtLeast(0.0)
  val clRemFraction = if (clQuota > 0) (clRem / clQuota).toFloat().coerceIn(0f, 1f) else 0f

  val rhQuota = summary.rhQuota.toDouble()
  val rhUsed = summary.rhTaken.toDouble().coerceAtLeast(0.0)
  val rhRem = summary.rhRemaining.toDouble().coerceAtLeast(0.0)
  val rhRemFraction = if (rhQuota > 0) (rhRem / rhQuota).toFloat().coerceIn(0f, 1f) else 0f

  // Earned Comp-Off / Holiday Duty Pool:
  // Total earned credits = max(inLieuWorked, colTaken) + availableUnusedGhCount
  val earnedDutyPool =
    (maxOf(summary.inLieuWorked, summary.colTaken) + colGhAudit.availableUnusedGhCount).toDouble()
      .coerceAtLeast(1.0)
  val colUsed = summary.colTaken.toDouble().coerceAtLeast(0.0)
  val colRem = (earnedDutyPool - colUsed).coerceAtLeast(0.0)
  val colRemFraction = (colRem / earnedDutyPool).toFloat().coerceIn(0f, 1f)

  val totalPoolQuota = clQuota + rhQuota + earnedDutyPool
  val totalPoolUsed = clUsed + rhUsed + colUsed
  val totalPoolRem = (totalPoolQuota - totalPoolUsed).coerceAtLeast(0.0)
  val totalPoolRemFraction =
    if (totalPoolQuota > 0) (totalPoolRem / totalPoolQuota).toFloat().coerceIn(0f, 1f) else 0f

  val activeMetrics: GaugeMetrics = remember(
    selectedFocus,
    summary,
    colGhAudit
  ) {
    when (selectedFocus) {
      LeaveGaugeFocusMode.TOTAL_POOL -> {
        val remPct = (totalPoolRemFraction * 100).roundToInt()
        GaugeMetrics(
          title = "Total Statutory & Duty Pool",
          shortCode = "ALL CREDITS",
          totalEntitlement = totalPoolQuota,
          usedCredits = totalPoolUsed,
          remainingCredits = totalPoolRem,
          remainingFraction = totalPoolRemFraction,
          usedFraction = (1f - totalPoolRemFraction).coerceIn(0f, 1f),
          primaryColor = EmeraldOptimal,
          secondaryColor = Color(0xFF38BDF8),
          usedColor = Color(0xFFF59E0B),
          statusLabel = when {
            remPct >= 55 -> "OPTIMAL RESERVE ($remPct% LEFT)"
            remPct >= 25 -> "BALANCED UTILIZATION ($remPct% LEFT)"
            else -> "HIGH UTILIZATION ($remPct% LEFT)"
          },
          footnote = "Includes 12 CL + 3 RH + ${earnedDutyPool.toInt()} Earned Duty/GH Credits in ${summary.year}"
        )
      }
      LeaveGaugeFocusMode.CASUAL_CL -> {
        val remPct = (clRemFraction * 100).roundToInt()
        GaugeMetrics(
          title = "Casual Leave (CL) Quota",
          shortCode = "CL QUOTA",
          totalEntitlement = clQuota,
          usedCredits = clUsed,
          remainingCredits = clRem,
          remainingFraction = clRemFraction,
          usedFraction = (1f - clRemFraction).coerceIn(0f, 1f),
          primaryColor = BadgeClGreen,
          secondaryColor = EmeraldOptimal,
          usedColor = Color(0xFFF59E0B),
          statusLabel = when {
            clRem >= 6.0 -> "HEALTHY CL BUFFER ($remPct% LEFT)"
            clRem >= 2.5 -> "MODERATE CL RESERVE ($remPct% LEFT)"
            else -> "LOW CL RESERVE ($remPct% LEFT)"
          },
          footnote = "Half-day (0.5d) & full-day CL tracked under TN Leave Rules"
        )
      }
      LeaveGaugeFocusMode.RESTRICTED_RH -> {
        val remPct = (rhRemFraction * 100).roundToInt()
        GaugeMetrics(
          title = "Restricted Holiday (RH) Quota",
          shortCode = "RH QUOTA",
          totalEntitlement = rhQuota,
          usedCredits = rhUsed,
          remainingCredits = rhRem,
          remainingFraction = rhRemFraction,
          usedFraction = (1f - rhRemFraction).coerceIn(0f, 1f),
          primaryColor = Color(0xFF38BDF8),
          secondaryColor = BadgeRhSlate,
          usedColor = Color(0xFFF59E0B),
          statusLabel = if (rhRem >= 2.0) {
            "FULL FESTIVAL FLEXIBILITY ($remPct% LEFT)"
          } else if (rhRem >= 1.0) {
            "1 RH BRIDGE CREDIT READY"
          } else {
            "ANNUAL RH QUOTA FULLY AVAILED"
          },
          footnote = "Up to 3 Restricted Holidays selectable from 31 notified TN festivals"
        )
      }
      LeaveGaugeFocusMode.COMP_OFF_COL -> {
        val remPct = (colRemFraction * 100).roundToInt()
        GaugeMetrics(
          title = "Compensatory & GH Duty Pool",
          shortCode = "COL ↔ GH",
          totalEntitlement = earnedDutyPool,
          usedCredits = colUsed,
          remainingCredits = colRem,
          remainingFraction = colRemFraction,
          usedFraction = (1f - colRemFraction).coerceIn(0f, 1f),
          primaryColor = BadgeColCyan,
          secondaryColor = BadgePlGold,
          usedColor = Color(0xFFF59E0B),
          statusLabel = if (colGhAudit.unaccountedColCount == 0) {
            "100% VERIFIED • ${colRem.toInt()} FREE GH CREDITS"
          } else {
            "${colGhAudit.unaccountedColCount} COL NEEDS GH PAIRING ($remPct% FREE)"
          },
          footnote = "${colGhAudit.fullyAccountedColCount}/${colGhAudit.totalColTaken} COL paired • ${colGhAudit.availableUnusedGhCount} unused GH worked credits"
        )
      }
    }
  }

  val animatedRemFraction by animateFloatAsState(
    targetValue = activeMetrics.remainingFraction,
    animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
    label = "main_gauge_rem"
  )
  val animatedUsedFraction by animateFloatAsState(
    targetValue = activeMetrics.usedFraction,
    animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
    label = "main_gauge_used"
  )
  val animatedClFraction by animateFloatAsState(
    targetValue = clRemFraction,
    animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
    label = "inner_cl_ring"
  )
  val animatedRhFraction by animateFloatAsState(
    targetValue = rhRemFraction,
    animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
    label = "inner_rh_ring"
  )
  val animatedColFraction by animateFloatAsState(
    targetValue = colRemFraction,
    animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
    label = "inner_col_ring"
  )

  val animatedPrimaryColor by animateColorAsState(
    targetValue = activeMetrics.primaryColor,
    animationSpec = tween(durationMillis = 450),
    label = "gauge_primary_color"
  )

  Surface(
    shape = RoundedCornerShape(28.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 3.dp,
    shadowElevation = 6.dp,
    modifier = modifier
      .fillMaxWidth()
      .testTag("leave_balance_circular_gauge_card")
      .border(
        width = 1.2.dp,
        brush = Brush.linearGradient(
          colors = listOf(
            animatedPrimaryColor.copy(alpha = 0.45f),
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
            activeMetrics.secondaryColor.copy(alpha = 0.35f)
          )
        ),
        shape = RoundedCornerShape(28.dp)
      )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          Brush.radialGradient(
            colors = listOf(
              animatedPrimaryColor.copy(alpha = 0.08f),
              Color.Transparent
            ),
            radius = 650f
          )
        )
        .padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // 1. Top Header & Status Pill
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
            shape = RoundedCornerShape(14.dp),
            color = animatedPrimaryColor.copy(alpha = 0.14f),
            border = BorderStroke(1.dp, animatedPrimaryColor.copy(alpha = 0.35f)),
            modifier = Modifier.size(42.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.DonutLarge,
                contentDescription = "Circular Leave Balance Gauge",
                tint = animatedPrimaryColor,
                modifier = Modifier.size(22.dp)
              )
            }
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Remaining vs. Used Leave Credits",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "${summary.year} Radial Credit Utilization • Tap ring to flip view",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontSize = 11.sp,
              lineHeight = 14.sp,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Flip Mode Pill (Remaining vs Used)
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = if (showUsedInCenter) {
            activeMetrics.usedColor.copy(alpha = 0.15f)
          } else {
            animatedPrimaryColor.copy(alpha = 0.14f)
          },
          border = BorderStroke(
            1.dp,
            if (showUsedInCenter) {
              activeMetrics.usedColor.copy(alpha = 0.40f)
            } else {
              animatedPrimaryColor.copy(alpha = 0.40f)
            }
          ),
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { showUsedInCenter = !showUsedInCenter }
            .testTag("toggle_gauge_center_mode")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Autorenew,
              contentDescription = "Switch Center Readout",
              tint = if (showUsedInCenter) activeMetrics.usedColor else animatedPrimaryColor,
              modifier = Modifier.size(13.dp)
            )
            Text(
              text = if (showUsedInCenter) "SHOWING USED" else "SHOWING LEFT",
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 10.sp,
              lineHeight = 12.sp,
              fontWeight = FontWeight.Bold,
              color = if (showUsedInCenter) activeMetrics.usedColor else animatedPrimaryColor,
              maxLines = 1,
              softWrap = false
            )
          }
        }
      }

      // 2. Category Lens Filter Pills (ALL | CL | RH | COL)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        LeaveGaugeFocusMode.entries.forEach { mode ->
          val isSelected = selectedFocus == mode
          val modeAccent = when (mode) {
            LeaveGaugeFocusMode.TOTAL_POOL -> EmeraldOptimal
            LeaveGaugeFocusMode.CASUAL_CL -> BadgeClGreen
            LeaveGaugeFocusMode.RESTRICTED_RH -> Color(0xFF38BDF8)
            LeaveGaugeFocusMode.COMP_OFF_COL -> BadgeColCyan
          }
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isSelected) {
              modeAccent.copy(alpha = 0.18f)
            } else {
              MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            },
            border = BorderStroke(
              width = if (isSelected) 1.2.dp else 1.dp,
              color = if (isSelected) {
                modeAccent.copy(alpha = 0.55f)
              } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)
              }
            ),
            modifier = Modifier
              .clip(RoundedCornerShape(14.dp))
              .clickable { selectedFocus = mode }
              .testTag("gauge_focus_${mode.code}")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(7.dp)
                  .clip(CircleShape)
                  .background(
                    if (isSelected) modeAccent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                  )
              )
              Text(
                text = mode.label,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) modeAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false
              )
            }
          }
        }
      }

      // 3. Main Interactive Circular Progress Indicator + Side-by-Side Telemetry Breakdown
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Left: High-Precision Multi-Ring Circular Progress Gauge
        Box(
          modifier = Modifier
            .size(182.dp)
            .clip(CircleShape)
            .clickable { showUsedInCenter = !showUsedInCenter }
            .testTag("circular_leave_gauge_canvas"),
          contentAlignment = Alignment.Center
        ) {
          val trackBaseColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
          val tickActiveColor = animatedPrimaryColor.copy(alpha = 0.65f)
          val tickUsedColor = activeMetrics.usedColor.copy(alpha = 0.45f)
          val tickIdleColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

          Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = size.minDimension
            val centerOffset = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = canvasSize / 2f - 4.dp.toPx()

            // A. Precision 36-Tick Perimeter Compass Ring
            val totalTicks = 36
            val tickOuterR = outerRadius
            val tickInnerR = outerRadius - 5.dp.toPx()
            val remTickThreshold = (animatedRemFraction * totalTicks).roundToInt()

            for (i in 0 until totalTicks) {
              val angleDeg = -90f + (i * (360f / totalTicks))
              val angleRad = Math.toRadians(angleDeg.toDouble())
              val startPt = Offset(
                x = centerOffset.x + (tickInnerR * cos(angleRad)).toFloat(),
                y = centerOffset.y + (tickInnerR * sin(angleRad)).toFloat()
              )
              val endPt = Offset(
                x = centerOffset.x + (tickOuterR * cos(angleRad)).toFloat(),
                y = centerOffset.y + (tickOuterR * sin(angleRad)).toFloat()
              )
              val tickColor = when {
                i < remTickThreshold -> tickActiveColor
                i < totalTicks -> tickUsedColor
                else -> tickIdleColor
              }
              drawLine(
                color = tickColor,
                start = startPt,
                end = endPt,
                strokeWidth = if (i % 3 == 0) 2.2.dp.toPx() else 1.2.dp.toPx(),
                cap = StrokeCap.Round
              )
            }

            // B. Primary Outer Ring (Remaining vs. Used Leave Credits)
            val mainStrokePx = 14.dp.toPx()
            val mainRadius = outerRadius - 14.dp.toPx()
            val mainTopLeft = Offset(centerOffset.x - mainRadius, centerOffset.y - mainRadius)
            val mainArcSize = Size(mainRadius * 2f, mainRadius * 2f)

            // Base Track
            drawArc(
              color = trackBaseColor,
              startAngle = -90f,
              sweepAngle = 360f,
              useCenter = false,
              topLeft = mainTopLeft,
              size = mainArcSize,
              style = Stroke(width = mainStrokePx, cap = StrokeCap.Round)
            )

            // Used Leave Credits Segment (Amber/Coral arc for consumed credits)
            if (animatedUsedFraction > 0.01f) {
              val gapDeg = if (animatedRemFraction > 0.03f && animatedUsedFraction > 0.03f) 6f else 0f
              val usedStartAngle = -90f + (animatedRemFraction * 360f) + (gapDeg / 2f)
              val usedSweepAngle = ((animatedUsedFraction * 360f) - gapDeg).coerceAtLeast(3f)

              drawArc(
                brush = Brush.linearGradient(
                  colors = listOf(
                    activeMetrics.usedColor.copy(alpha = 0.85f),
                    Color(0xFFFB7185).copy(alpha = 0.85f)
                  )
                ),
                startAngle = usedStartAngle,
                sweepAngle = usedSweepAngle,
                useCenter = false,
                topLeft = mainTopLeft,
                size = mainArcSize,
                style = Stroke(width = mainStrokePx * 0.78f, cap = StrokeCap.Round)
              )
            }

            // Remaining Leave Balance Segment (Vibrant Primary Gradient Arc + Glowing Tip)
            if (animatedRemFraction > 0.01f) {
              val gapDeg = if (animatedRemFraction > 0.03f && animatedUsedFraction > 0.03f) 6f else 0f
              val remSweepAngle = ((animatedRemFraction * 360f) - (gapDeg / 2f)).coerceAtLeast(4f)

              // Soft ambient glow behind remaining arc
              drawArc(
                color = animatedPrimaryColor.copy(alpha = 0.22f),
                startAngle = -90f,
                sweepAngle = remSweepAngle,
                useCenter = false,
                topLeft = mainTopLeft,
                size = mainArcSize,
                style = Stroke(width = mainStrokePx * 1.45f, cap = StrokeCap.Round)
              )

              // Main crisp remaining arc
              drawArc(
                brush = Brush.linearGradient(
                  colors = listOf(
                    animatedPrimaryColor,
                    activeMetrics.secondaryColor,
                    animatedPrimaryColor
                  )
                ),
                startAngle = -90f,
                sweepAngle = remSweepAngle,
                useCenter = false,
                topLeft = mainTopLeft,
                size = mainArcSize,
                style = Stroke(width = mainStrokePx, cap = StrokeCap.Round)
              )

              // Glowing End-Cap Orb on the Remaining Arc Tip
              val tipAngleRad = Math.toRadians((-90f + remSweepAngle).toDouble())
              val tipOffset = Offset(
                x = centerOffset.x + (mainRadius * cos(tipAngleRad)).toFloat(),
                y = centerOffset.y + (mainRadius * sin(tipAngleRad)).toFloat()
              )
              drawCircle(
                color = Color.White,
                radius = 3.5.dp.toPx(),
                center = tipOffset
              )
            }

            // C. Inner Concentric Multi-Pool Activity Rings (CL / RH / COL simultaneous rings)
            val innerStrokePx = 4.dp.toPx()
            val ringGap = 6.5.dp.toPx()

            // Inner Ring 1: CL Remaining
            val clRadius = mainRadius - (mainStrokePx / 2f) - 8.dp.toPx()
            val clTopLeft = Offset(centerOffset.x - clRadius, centerOffset.y - clRadius)
            val clSize = Size(clRadius * 2f, clRadius * 2f)
            drawArc(
              color = BadgeClGreen.copy(alpha = 0.14f),
              startAngle = -90f,
              sweepAngle = 360f,
              useCenter = false,
              topLeft = clTopLeft,
              size = clSize,
              style = Stroke(width = innerStrokePx, cap = StrokeCap.Round)
            )
            drawArc(
              color = BadgeClGreen.copy(alpha = 0.85f),
              startAngle = -90f,
              sweepAngle = (animatedClFraction * 360f).coerceAtLeast(4f),
              useCenter = false,
              topLeft = clTopLeft,
              size = clSize,
              style = Stroke(width = innerStrokePx, cap = StrokeCap.Round)
            )

            // Inner Ring 2: RH Remaining
            val rhRadius = clRadius - ringGap
            val rhTopLeft = Offset(centerOffset.x - rhRadius, centerOffset.y - rhRadius)
            val rhSize = Size(rhRadius * 2f, rhRadius * 2f)
            drawArc(
              color = Color(0xFF38BDF8).copy(alpha = 0.14f),
              startAngle = -90f,
              sweepAngle = 360f,
              useCenter = false,
              topLeft = rhTopLeft,
              size = rhSize,
              style = Stroke(width = innerStrokePx, cap = StrokeCap.Round)
            )
            drawArc(
              color = Color(0xFF38BDF8).copy(alpha = 0.85f),
              startAngle = -90f,
              sweepAngle = (animatedRhFraction * 360f).coerceAtLeast(4f),
              useCenter = false,
              topLeft = rhTopLeft,
              size = rhSize,
              style = Stroke(width = innerStrokePx, cap = StrokeCap.Round)
            )

            // Inner Ring 3: COL / GH Duty Remaining
            val colRadius = rhRadius - ringGap
            val colTopLeft = Offset(centerOffset.x - colRadius, centerOffset.y - colRadius)
            val colSize = Size(colRadius * 2f, colRadius * 2f)
            drawArc(
              color = BadgePlGold.copy(alpha = 0.14f),
              startAngle = -90f,
              sweepAngle = 360f,
              useCenter = false,
              topLeft = colTopLeft,
              size = colSize,
              style = Stroke(width = innerStrokePx, cap = StrokeCap.Round)
            )
            drawArc(
              color = BadgePlGold.copy(alpha = 0.90f),
              startAngle = -90f,
              sweepAngle = (animatedColFraction * 360f).coerceAtLeast(4f),
              useCenter = false,
              topLeft = colTopLeft,
              size = colSize,
              style = Stroke(width = innerStrokePx, cap = StrokeCap.Round)
            )
          }

          // Center Hub Digital Readout
          val centerPercent = if (showUsedInCenter) {
            (activeMetrics.usedFraction * 100).roundToInt()
          } else {
            (activeMetrics.remainingFraction * 100).roundToInt()
          }
          val centerDaysText = if (showUsedInCenter) {
            "${formatGaugeDays(activeMetrics.usedCredits)}d"
          } else {
            "${formatGaugeDays(activeMetrics.remainingCredits)}d"
          }
          val centerAccent = if (showUsedInCenter) activeMetrics.usedColor else animatedPrimaryColor

          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = centerAccent.copy(alpha = 0.14f)
            ) {
              Text(
                text = if (showUsedInCenter) "USED" else "REMAINING",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 8.sp,
                lineHeight = 10.sp,
                fontWeight = FontWeight.Bold,
                color = centerAccent,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = centerDaysText,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 21.sp,
              lineHeight = 24.sp,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              softWrap = false
            )
            Text(
              text = "$centerPercent% of ${formatGaugeDays(activeMetrics.totalEntitlement)}d",
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 9.sp,
              lineHeight = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              softWrap = false
            )
          }
        }

        // Right: Remaining Balance vs. Used Credits Breakdown Cards
        Column(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Remaining Leave Balance Card
          RadialLegendMetricTile(
            label = "REMAINING BALANCE",
            daysValue = "${formatGaugeDays(activeMetrics.remainingCredits)} Days",
            percentageBadge = "${(activeMetrics.remainingFraction * 100).roundToInt()}%",
            subText = "Available to schedule",
            accentColor = animatedPrimaryColor,
            isHighlighted = !showUsedInCenter,
            onClick = { showUsedInCenter = false }
          )

          // Used Leave Credits Card
          RadialLegendMetricTile(
            label = "USED LEAVE CREDITS",
            daysValue = "${formatGaugeDays(activeMetrics.usedCredits)} Days",
            percentageBadge = "${(activeMetrics.usedFraction * 100).roundToInt()}%",
            subText = "Availed in ${summary.year} ledger",
            accentColor = activeMetrics.usedColor,
            isHighlighted = showUsedInCenter,
            onClick = { showUsedInCenter = true }
          )

          // Total Entitlement & Rule Compliance Chip
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            border = BorderStroke(
              1.dp,
              MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            ),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "TOTAL POOL",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 9.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false
              )
              Text(
                text = "${formatGaugeDays(activeMetrics.totalEntitlement)} Days",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false
              )
            }
          }
        }
      }

      // 4. Bottom 3-Column Sub-Pool Mini Circular Progress Strip (CL | RH | COL)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        SubPoolCircularMiniCard(
          code = "CL",
          title = "Casual Leave",
          remainingText = "${formatGaugeDays(clRem)}d left",
          usedText = "${formatGaugeDays(clUsed)}/${summary.clQuota}d used",
          remainingFraction = clRemFraction,
          accentColor = BadgeClGreen,
          isSelected = selectedFocus == LeaveGaugeFocusMode.CASUAL_CL,
          onClick = {
            selectedFocus = if (selectedFocus == LeaveGaugeFocusMode.CASUAL_CL) {
              LeaveGaugeFocusMode.TOTAL_POOL
            } else {
              LeaveGaugeFocusMode.CASUAL_CL
            }
          },
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
        )

        SubPoolCircularMiniCard(
          code = "RH",
          title = "Restricted",
          remainingText = "${summary.rhRemaining}d left",
          usedText = "${summary.rhTaken}/${summary.rhQuota}d used",
          remainingFraction = rhRemFraction,
          accentColor = Color(0xFF38BDF8),
          isSelected = selectedFocus == LeaveGaugeFocusMode.RESTRICTED_RH,
          onClick = {
            selectedFocus = if (selectedFocus == LeaveGaugeFocusMode.RESTRICTED_RH) {
              LeaveGaugeFocusMode.TOTAL_POOL
            } else {
              LeaveGaugeFocusMode.RESTRICTED_RH
            }
          },
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
        )

        SubPoolCircularMiniCard(
          code = "COL",
          title = "Comp-Off Pool",
          remainingText = "${formatGaugeDays(colRem)}d free",
          usedText = "${summary.colTaken}/${earnedDutyPool.toInt()}d used",
          remainingFraction = colRemFraction,
          accentColor = BadgeColCyan,
          isSelected = selectedFocus == LeaveGaugeFocusMode.COMP_OFF_COL,
          onClick = {
            selectedFocus = if (selectedFocus == LeaveGaugeFocusMode.COMP_OFF_COL) {
              LeaveGaugeFocusMode.TOTAL_POOL
            } else {
              LeaveGaugeFocusMode.COMP_OFF_COL
            }
          },
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
        )
      }

      // 5. Status & Statutory Compliance Footer Bar
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = animatedPrimaryColor.copy(alpha = 0.09f),
        border = BorderStroke(1.dp, animatedPrimaryColor.copy(alpha = 0.25f)),
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .clickable {
            if (selectedFocus == LeaveGaugeFocusMode.COMP_OFF_COL) {
              onNavigateToAudit()
            } else {
              onNavigateToLedger()
            }
          }
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 9.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = if (activeMetrics.remainingFraction >= 0.25f) {
                Icons.Default.CheckCircle
              } else {
                Icons.Default.WarningAmber
              },
              contentDescription = null,
              tint = animatedPrimaryColor,
              modifier = Modifier.size(16.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = activeMetrics.statusLabel,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Bold,
                color = animatedPrimaryColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = activeMetrics.footnote,
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

          Icon(
            imageVector = Icons.Default.TrendingUp,
            contentDescription = null,
            tint = animatedPrimaryColor,
            modifier = Modifier.size(16.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun RadialLegendMetricTile(
  label: String,
  daysValue: String,
  percentageBadge: String,
  subText: String,
  accentColor: Color,
  isHighlighted: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = if (isHighlighted) {
      accentColor.copy(alpha = 0.13f)
    } else {
      MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    },
    border = BorderStroke(
      width = if (isHighlighted) 1.3.dp else 1.dp,
      color = if (isHighlighted) {
        accentColor.copy(alpha = 0.55f)
      } else {
        accentColor.copy(alpha = 0.22f)
      }
    ),
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .clickable { onClick() }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 9.dp),
      verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp),
          modifier = Modifier.weight(1f)
        ) {
          Box(
            modifier = Modifier
              .size(7.dp)
              .clip(CircleShape)
              .background(accentColor)
          )
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
        }

        Surface(
          shape = RoundedCornerShape(6.dp),
          color = accentColor.copy(alpha = 0.18f)
        ) {
          Text(
            text = percentageBadge,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 9.sp,
            lineHeight = 11.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
          )
        }
      }

      Text(
        text = daysValue,
        fontFamily = JetBrainsMonoFontFamily,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )

      Text(
        text = subText,
        style = MaterialTheme.typography.bodySmall,
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
private fun SubPoolCircularMiniCard(
  code: String,
  title: String,
  remainingText: String,
  usedText: String,
  remainingFraction: Float,
  accentColor: Color,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val pctText = "${(remainingFraction * 100).roundToInt()}%"
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = if (isSelected) {
      accentColor.copy(alpha = 0.14f)
    } else {
      MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
    },
    border = BorderStroke(
      width = if (isSelected) 1.3.dp else 1.dp,
      color = if (isSelected) {
        accentColor.copy(alpha = 0.55f)
      } else {
        accentColor.copy(alpha = 0.22f)
      }
    ),
    modifier = modifier
      .clip(RoundedCornerShape(18.dp))
      .clickable { onClick() }
      .testTag("subpool_ring_card_$code")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 10.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        MiniCircularQuotaRing(
          remainingFraction = remainingFraction,
          accentColor = accentColor,
          trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
          size = 38.dp,
          strokeWidth = 4.dp,
          centerText = pctText,
          centerTextColor = accentColor
        )

        Surface(
          shape = RoundedCornerShape(7.dp),
          color = accentColor.copy(alpha = 0.15f)
        ) {
          Text(
            text = code,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 9.sp,
            lineHeight = 11.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = title,
          style = MaterialTheme.typography.labelMedium,
          fontSize = 11.sp,
          lineHeight = 13.sp,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = remainingText,
          fontFamily = JetBrainsMonoFontFamily,
          fontSize = 12.sp,
          lineHeight = 15.sp,
          fontWeight = FontWeight.Bold,
          color = accentColor,
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = usedText,
          fontFamily = JetBrainsMonoFontFamily,
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
}

private fun formatGaugeDays(value: Double): String {
  return if (value % 1.0 == 0.0) {
    value.toInt().toString()
  } else {
    String.format("%.1f", value)
  }
}
