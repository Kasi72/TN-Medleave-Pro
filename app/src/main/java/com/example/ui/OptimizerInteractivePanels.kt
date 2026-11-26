package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.AiBridgeDayPlan
import com.example.domain.AiLeaveRecommendation
import com.example.domain.HodSanctionLetterGenerator
import com.example.domain.OptimizerConstraints
import com.example.domain.OptimizerHorizon
import com.example.domain.PortfolioImpactSummary
import com.example.ui.theme.BadgeClGreen
import com.example.ui.theme.BadgeColCyan
import com.example.ui.theme.BadgeElNeutral
import com.example.ui.theme.BadgeGhLime
import com.example.ui.theme.BadgePlGold
import com.example.ui.theme.BadgeRhSlate
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.IosSharpBadgeShape
import com.example.ui.theme.IosSharpButtonShape
import com.example.ui.theme.IosSharpCardShape
import com.example.ui.theme.IosSharpMicroShape
import com.example.ui.theme.IosSharpWellShape
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalNeumorphicColors
import com.example.ui.theme.StatusAlertRed
import com.example.ui.theme.StatusPendingOrange
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

private val ElVioletAccent = Color(0xFF7C3AED)
private val HoldIndigoAccent = Color(0xFF6366F1)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OptimizerConstraintControlPanel(
  constraints: OptimizerConstraints,
  onUpdateConstraints: (OptimizerConstraints) -> Unit,
  onRunWithPrompt: () -> Unit
) {
  val neu = LocalNeumorphicColors.current
  var showTuningDrawer by remember { mutableStateOf(false) }

  val activeFilterCount = remember(constraints) {
    var count = 0
    if (constraints.reserveClFloor > 0.0) count++
    if (constraints.allowElBridges) count++
    if (constraints.strictZeroCl) count++
    if (constraints.horizonMode != OptimizerHorizon.FULL_HORIZON) count++
    if (constraints.protectedDaysOfWeek.isNotEmpty()) count++
    if (constraints.customNaturalPrompt.isNotBlank()) count++
    count
  }

  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    // 1. Natural-Language Optimizer Ask Bar
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .neumorphicInset(
          shape = IosSharpWellShape,
          depth = 2.5.dp,
          blurRadius = 5.dp,
          accentTint = neu.primaryAccent,
          accentTintAlpha = if (constraints.customNaturalPrompt.isNotBlank()) 0.08f else 0.03f,
          borderColor = neu.primaryAccent.copy(alpha = if (constraints.customNaturalPrompt.isNotBlank()) 0.42f else 0.22f),
          neu = neu
        )
        .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(
          imageVector = Icons.Default.AutoAwesome,
          contentDescription = null,
          tint = neu.primaryAccent,
          modifier = Modifier.size(16.dp)
        )
        Box(modifier = Modifier.weight(1f)) {
          if (constraints.customNaturalPrompt.isEmpty()) {
            Text(
              text = "Ask AI (e.g. Keep 3 CL, protect Tue/Thu OPD, 7d EL break)",
              style = MaterialTheme.typography.bodySmall,
              color = neu.textSecondary.copy(alpha = 0.72f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
          BasicTextField(
            value = constraints.customNaturalPrompt,
            onValueChange = { onUpdateConstraints(constraints.copy(customNaturalPrompt = it)) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall.copy(
              color = neu.textPrimary,
              fontWeight = FontWeight.SemiBold
            ),
            cursorBrush = SolidColor(neu.primaryAccent),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("optimizer_natural_prompt_input")
          )
        }
        if (constraints.customNaturalPrompt.isNotBlank()) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Clear prompt",
            tint = neu.textSecondary,
            modifier = Modifier
              .size(18.dp)
              .clickable { onUpdateConstraints(constraints.copy(customNaturalPrompt = "")) }
          )
        }
      }
    }

    // Quick-Fill Natural Prompt Chips + Constraint Tuning Drawer Toggle
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(vertical = 2.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Tuning Drawer Pill
      Box(
        modifier = Modifier
          .then(
            if (showTuningDrawer || activeFilterCount > 0) {
              Modifier.neumorphicInset(
                shape = IosSharpBadgeShape,
                depth = 2.dp,
                blurRadius = 4.dp,
                accentTint = neu.primaryAccent,
                accentTintAlpha = 0.14f,
                borderColor = neu.primaryAccent.copy(alpha = 0.45f),
                neu = neu
              )
            } else {
              Modifier.neumorphicExtruded(
                shape = IosSharpBadgeShape,
                elevation = 2.dp,
                blurRadius = 4.dp,
                borderAlpha = 0.25f,
                neu = neu
              )
            }
          )
          .clickable { showTuningDrawer = !showTuningDrawer }
          .padding(horizontal = 10.dp, vertical = 6.dp)
          .testTag("optimizer_constraints_toggle_btn")
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Tune,
            contentDescription = "Tune Constraints",
            tint = neu.primaryAccent,
            modifier = Modifier.size(13.dp)
          )
          Text(
            text = if (activeFilterCount > 0) "Constraints ($activeFilterCount)" else "Tune Rules",
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = neu.primaryAccent,
            maxLines = 1,
            softWrap = false
          )
          Icon(
            imageVector = if (showTuningDrawer) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = neu.primaryAccent,
            modifier = Modifier.size(14.dp)
          )
        }
      }

      val quickChips = remember {
        listOf(
          "Keep 3 CL" to { c: OptimizerConstraints ->
            c.copy(reserveClFloor = if (c.reserveClFloor == 3.0) 0.0 else 3.0)
          },
          "Zero-CL" to { c: OptimizerConstraints ->
            c.copy(strictZeroCl = !c.strictZeroCl)
          },
          "Rule-8 EL (7–9d)" to { c: OptimizerConstraints ->
            c.copy(allowElBridges = !c.allowElBridges)
          },
          "0.5 CL Half-Day" to { c: OptimizerConstraints ->
            c.copy(allowHalfDayCl = !c.allowHalfDayCl)
          },
          "Expiring COL" to { c: OptimizerConstraints ->
            c.copy(prioritizeExpiringCol = !c.prioritizeExpiringCol)
          }
        )
      }

      quickChips.forEachIndexed { idx, (label, toggleFn) ->
        val isActive = when (idx) {
          0 -> constraints.reserveClFloor >= 3.0
          1 -> constraints.strictZeroCl
          2 -> constraints.allowElBridges
          3 -> constraints.allowHalfDayCl
          else -> constraints.prioritizeExpiringCol
        }
        val chipAccent = when (idx) {
          0 -> BadgeClGreen
          1 -> BadgeRhSlate
          2 -> ElVioletAccent
          3 -> EmeraldOptimal
          else -> BadgeColCyan
        }
        val displayAccent = if (chipAccent == BadgeRhSlate && neu.isDark) Color(0xFF94A3B8) else chipAccent

        Box(
          modifier = Modifier
            .then(
              if (isActive) {
                Modifier.neumorphicInset(
                  shape = IosSharpBadgeShape,
                  depth = 1.8.dp,
                  blurRadius = 3.5.dp,
                  accentTint = chipAccent,
                  accentTintAlpha = 0.14f,
                  borderColor = chipAccent.copy(alpha = 0.45f),
                  neu = neu
                )
              } else {
                Modifier.neumorphicExtruded(
                  shape = IosSharpBadgeShape,
                  elevation = 1.8.dp,
                  blurRadius = 3.5.dp,
                  borderAlpha = 0.22f,
                  neu = neu
                )
              }
            )
            .clickable { onUpdateConstraints(toggleFn(constraints)) }
            .padding(horizontal = 9.dp, vertical = 6.dp)
            .testTag("optimizer_quick_chip_$idx")
        ) {
          Text(
            text = if (isActive) "✓ $label" else label,
            fontSize = 10.5.sp,
            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (isActive) displayAccent else neu.textSecondary,
            maxLines = 1,
            softWrap = false
          )
        }
      }
    }

    // Collapsible Deep Constraint Tuning Panel
    AnimatedVisibility(
      visible = showTuningDrawer,
      enter = expandVertically() + fadeIn(),
      exit = shrinkVertically() + fadeOut()
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .neumorphicInset(
            shape = IosSharpWellShape,
            depth = 2.5.dp,
            blurRadius = 5.dp,
            accentTint = neu.primaryAccent,
            accentTintAlpha = 0.04f,
            borderColor = neu.primaryAccent.copy(alpha = 0.22f),
            neu = neu
          )
          .padding(12.dp)
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          // Horizon Selector
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "PLANNING HORIZON",
              fontSize = 10.sp,
              fontWeight = FontWeight.ExtraBold,
              color = neu.primaryAccent,
              letterSpacing = 0.5.sp
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              OptimizerHorizon.entries.forEach { horizon ->
                val selected = constraints.horizonMode == horizon
                Box(
                  modifier = Modifier
                    .weight(1f)
                    .then(
                      if (selected) {
                        Modifier.neumorphicExtruded(
                          shape = IosSharpMicroShape,
                          elevation = 2.dp,
                          blurRadius = 4.dp,
                          surfaceColor = neu.primaryAccent,
                          neu = neu
                        )
                      } else {
                        Modifier.neumorphicExtruded(
                          shape = IosSharpMicroShape,
                          elevation = 1.5.dp,
                          blurRadius = 3.dp,
                          borderAlpha = 0.22f,
                          neu = neu
                        )
                      }
                    )
                    .clickable { onUpdateConstraints(constraints.copy(horizonMode = horizon)) }
                    .padding(vertical = 7.dp, horizontal = 4.dp)
                    .testTag("horizon_chip_${horizon.name}"),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = horizon.shortChip,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (selected) Color.White else neu.textPrimary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
            }
          }

          // CL Safety Reserve Floor
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "EMERGENCY CL SAFETY RESERVE FLOOR",
              fontSize = 10.sp,
              fontWeight = FontWeight.ExtraBold,
              color = BadgeClGreen,
              letterSpacing = 0.5.sp
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              listOf(0.0, 2.0, 3.0, 4.0, 5.0).forEach { floorVal ->
                val selected = constraints.reserveClFloor == floorVal
                Box(
                  modifier = Modifier
                    .weight(1f)
                    .then(
                      if (selected) {
                        Modifier.neumorphicExtruded(
                          shape = IosSharpMicroShape,
                          elevation = 2.dp,
                          blurRadius = 4.dp,
                          surfaceColor = BadgeClGreen,
                          neu = neu
                        )
                      } else {
                        Modifier.neumorphicExtruded(
                          shape = IosSharpMicroShape,
                          elevation = 1.5.dp,
                          blurRadius = 3.dp,
                          borderAlpha = 0.22f,
                          neu = neu
                        )
                      }
                    )
                    .clickable { onUpdateConstraints(constraints.copy(reserveClFloor = floorVal)) }
                    .padding(vertical = 6.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = if (floorVal == 0.0) "None" else "≥${floorVal.toInt()} CL",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) Color.White else neu.textPrimary,
                    maxLines = 1,
                    softWrap = false
                  )
                }
              }
            }
          }

          // Protected Clinical OPD / Duty Weekdays
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "PROTECTED OPD / CLINICAL DUTY DAYS (AVOID LEAVE)",
              fontSize = 10.sp,
              fontWeight = FontWeight.ExtraBold,
              color = StatusPendingOrange,
              letterSpacing = 0.5.sp
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
              val weekdays = listOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY
              )
              weekdays.forEach { dow ->
                val isProtected = dow in constraints.protectedDaysOfWeek
                Box(
                  modifier = Modifier
                    .weight(1f)
                    .then(
                      if (isProtected) {
                        Modifier.neumorphicInset(
                          shape = IosSharpMicroShape,
                          depth = 2.dp,
                          blurRadius = 4.dp,
                          accentTint = StatusPendingOrange,
                          accentTintAlpha = 0.16f,
                          borderColor = StatusPendingOrange.copy(alpha = 0.55f),
                          neu = neu
                        )
                      } else {
                        Modifier.neumorphicExtruded(
                          shape = IosSharpMicroShape,
                          elevation = 1.5.dp,
                          blurRadius = 3.dp,
                          borderAlpha = 0.20f,
                          neu = neu
                        )
                      }
                    )
                    .clickable {
                      val next = constraints.protectedDaysOfWeek.toMutableSet()
                      if (isProtected) next.remove(dow) else next.add(dow)
                      onUpdateConstraints(constraints.copy(protectedDaysOfWeek = next))
                    }
                    .padding(vertical = 6.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = dow.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isProtected) StatusPendingOrange else neu.textSecondary,
                    maxLines = 1,
                    softWrap = false
                  )
                }
              }
            }
          }

          // Apply Prompt & Constraints directly to Gemini AI
          if (constraints.customNaturalPrompt.isNotBlank() || activeFilterCount > 0) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End
            ) {
              Box(
                modifier = Modifier
                  .neumorphicExtruded(
                    shape = IosSharpBadgeShape,
                    elevation = 2.dp,
                    blurRadius = 4.dp,
                    surfaceColor = neu.primaryAccent,
                    neu = neu
                  )
                  .clickable { onRunWithPrompt() }
                  .padding(horizontal = 12.dp, vertical = 6.dp)
              ) {
                Text(
                  text = "Synthesize with Gemini AI →",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = Color.White
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
fun BridgeVisualTimelineStrip(
  dailyPlan: List<AiBridgeDayPlan>,
  modifier: Modifier = Modifier
) {
  val neu = LocalNeumorphicColors.current
  if (dailyPlan.isEmpty()) return

  Row(
    modifier = modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState())
      .padding(vertical = 2.dp),
    horizontalArrangement = Arrangement.spacedBy(5.dp)
  ) {
    dailyPlan.forEach { day ->
      val upperType = day.creditType.uppercase(Locale.US)
      val (blockColor, shortCode, isDebit) = when (upperType) {
        "GH" -> Triple(BadgePlGold, "GH", false)
        "WEEKEND" -> Triple(Color(0xFF64748B), "WK", false)
        "COL" -> Triple(BadgeColCyan, "COL", true)
        "RH" -> Triple(Color(0xFF0D9488), "RH", true)
        "HALF_CL" -> Triple(EmeraldOptimal, "½CL", true)
        "EL" -> Triple(ElVioletAccent, "EL", true)
        "CL" -> Triple(BadgeClGreen, "CL", true)
        else -> Triple(BadgeElNeutral, upperType.take(3), true)
      }

      val dayShort = day.dayLabel.substringBefore(",").trim().take(3)
      val dayNum = day.dateIso.takeLast(2).trimStart('0').ifEmpty { day.dayLabel.takeLast(2) }
      val textAccent = if (upperType == "GH" && !neu.isDark) Color(0xFFB45309) else blockColor

      Box(
        modifier = Modifier
          .width(46.dp)
          .then(
            if (isDebit) {
              Modifier.neumorphicInset(
                shape = IosSharpMicroShape,
                depth = 1.8.dp,
                blurRadius = 3.5.dp,
                accentTint = blockColor,
                accentTintAlpha = 0.14f,
                borderColor = blockColor.copy(alpha = 0.50f),
                neu = neu
              )
            } else {
              Modifier.neumorphicExtruded(
                shape = IosSharpMicroShape,
                elevation = 1.5.dp,
                blurRadius = 3.dp,
                accentTint = blockColor,
                accentTintAlpha = 0.06f,
                borderAlpha = 0.25f,
                neu = neu
              )
            }
          )
          .padding(horizontal = 3.dp, vertical = 5.dp)
      ) {
        if (day.isProtectedOpdDay) {
          Box(
            modifier = Modifier
              .align(Alignment.TopEnd)
              .size(5.dp)
              .clip(CircleShape)
              .background(StatusPendingOrange)
          )
        }
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
          Text(
            text = dayShort,
            fontSize = 8.5.sp,
            lineHeight = 10.sp,
            fontWeight = FontWeight.Bold,
            color = neu.textSecondary,
            maxLines = 1,
            softWrap = false
          )
          Text(
            text = dayNum,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = neu.textPrimary,
            maxLines = 1,
            softWrap = false
          )
          Box(
            modifier = Modifier
              .clip(IosSharpMicroShape)
              .background(if (isDebit) blockColor else blockColor.copy(alpha = 0.18f))
              .padding(horizontal = 4.dp, vertical = 1.dp)
          ) {
            Text(
              text = shortCode,
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 8.sp,
              lineHeight = 9.5.sp,
              fontWeight = FontWeight.ExtraBold,
              color = if (isDebit) Color.White else textAccent,
              maxLines = 1,
              softWrap = false
            )
          }
        }
      }
    }
  }
}

@Composable
fun PortfolioImpactSummaryDock(
  impact: PortfolioImpactSummary,
  onApplyPortfolio: () -> Unit,
  onClearSelection: () -> Unit
) {
  val neu = LocalNeumorphicColors.current
  val hasWarning = impact.hasOverlappingDates || impact.violatesClReserve || impact.exceedsQuotaMessage != null
  val accent = if (hasWarning) StatusAlertRed else EmeraldOptimal

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .neumorphicExtruded(
        shape = IosSharpCardShape,
        elevation = 5.dp,
        blurRadius = 10.dp,
        accentTint = accent,
        accentTintAlpha = 0.06f,
        borderAlpha = 0.45f,
        neu = neu
      )
      .padding(14.dp)
      .testTag("portfolio_impact_dock")
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
            imageVector = Icons.Default.Layers,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(18.dp)
          )
          Column {
            Text(
              text = "PORTFOLIO BUILDER (${impact.selectedPlanCount} SELECTED)",
              fontSize = 11.sp,
              fontWeight = FontWeight.ExtraBold,
              color = accent,
              letterSpacing = 0.4.sp
            )
            Text(
              text = "${impact.totalDaysOffUnlocked} Total Days Off • ${formatCleanPortfolioDouble(impact.totalCreditsDebited)} Credits Debited",
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = neu.textPrimary
            )
          }
        }
        Text(
          text = "Clear",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = neu.textSecondary,
          modifier = Modifier
            .clickable { onClearSelection() }
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag("portfolio_clear_btn")
        )
      }

      // Live Remaining Balances Strip
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        PortfolioMetricChip(
          label = "2026 CL Left",
          value = formatCleanPortfolioDouble(impact.remainingCl2026),
          accent = if (impact.violatesClReserve) StatusAlertRed else BadgeClGreen,
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
        )
        PortfolioMetricChip(
          label = "2026 RH Left",
          value = "${impact.remainingRh2026}",
          accent = BadgeRhSlate,
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
        )
        PortfolioMetricChip(
          label = "COL Pool Left",
          value = "${impact.remainingColPool}",
          accent = BadgeColCyan,
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
        )
        PortfolioMetricChip(
          label = "Dec Lapse Saved",
          value = "+${formatCleanPortfolioDouble(impact.dec2026LapseSavedDays)}d",
          accent = EmeraldOptimal,
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
        )
      }

      if (hasWarning) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .neumorphicInset(
              shape = IosSharpWellShape,
              depth = 2.dp,
              blurRadius = 4.dp,
              accentTint = StatusAlertRed,
              accentTintAlpha = 0.12f,
              borderColor = StatusAlertRed.copy(alpha = 0.45f),
              neu = neu
            )
            .padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = null,
              tint = StatusAlertRed,
              modifier = Modifier.size(15.dp)
            )
            val warningText = buildList {
              if (impact.hasOverlappingDates) add("Overlapping dates between selected plans")
              if (impact.violatesClReserve) add("Drops below CL Safety Reserve floor")
              impact.exceedsQuotaMessage?.let { add(it) }
            }.joinToString(" • ")
            Text(
              text = warningText,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = StatusAlertRed
            )
          }
        }
      }

      Button(
        onClick = onApplyPortfolio,
        enabled = !impact.hasOverlappingDates && impact.exceedsQuotaMessage == null,
        shape = IosSharpButtonShape,
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldOptimal),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("apply_portfolio_btn")
      ) {
        Icon(
          imageVector = Icons.Default.CheckCircle,
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Commit Portfolio (${impact.selectedPlanCount} Plans) to Ledger",
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

@Composable
private fun PortfolioMetricChip(
  label: String,
  value: String,
  accent: Color,
  modifier: Modifier = Modifier
) {
  val neu = LocalNeumorphicColors.current
  val textAccent = if (accent == BadgeRhSlate && neu.isDark) Color(0xFF94A3B8) else accent
  Box(
    modifier = modifier
      .neumorphicInset(
        shape = IosSharpMicroShape,
        depth = 1.8.dp,
        blurRadius = 3.5.dp,
        accentTint = accent,
        accentTintAlpha = 0.08f,
        borderColor = accent.copy(alpha = 0.28f),
        neu = neu
      )
      .padding(horizontal = 6.dp, vertical = 6.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = label,
        fontSize = 9.sp,
        lineHeight = 11.sp,
        color = neu.textSecondary,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = value,
        fontFamily = JetBrainsMonoFontFamily,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        color = textAccent,
        maxLines = 1,
        softWrap = false
      )
    }
  }
}

@Composable
fun HodSanctionLetterDialog(
  rec: AiLeaveRecommendation,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val neu = LocalNeumorphicColors.current

  val letterText = remember(rec.id) {
    HodSanctionLetterGenerator.generateOfficialApplicationAndHandover(
      rec = rec,
      officerName = HodProfileSettingsStore.getOfficerName(context),
      designation = HodProfileSettingsStore.getDesignation(context),
      department = HodProfileSettingsStore.getDepartment(context),
      college = HodProfileSettingsStore.getCollege(context),
      ifhrmsId = HodProfileSettingsStore.getIfhrmsId(context)
    )
  }

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
          accentTintAlpha = 0.04f,
          borderAlpha = 0.35f,
          neu = neu
        )
        .padding(16.dp)
        .testTag("hod_sanction_letter_dialog")
    ) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
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
              imageVector = Icons.Default.Description,
              contentDescription = null,
              tint = neu.primaryAccent,
              modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "TN Govt Sanction Application & Handover",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = neu.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = "${rec.title} • ${rec.totalDaysOff}d Off",
                style = MaterialTheme.typography.labelSmall,
                color = neu.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
          IconButton(onClick = onDismiss) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close sanction letter dialog",
              tint = neu.textSecondary
            )
          }
        }

        HorizontalDivider(color = neu.hairlineDivider)

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp, max = 420.dp)
            .neumorphicInset(
              shape = IosSharpWellShape,
              depth = 2.5.dp,
              blurRadius = 5.dp,
              neu = neu
            )
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
        ) {
          Text(
            text = letterText,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            color = neu.textPrimary
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = {
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              clipboard.setPrimaryClip(ClipData.newPlainText("TN Leave Sanction Letter", letterText))
              Toast.makeText(context, "Copied official TN Govt leave application!", Toast.LENGTH_SHORT).show()
            },
            shape = IosSharpButtonShape,
            modifier = Modifier
              .weight(1f)
              .testTag("copy_sanction_letter_btn")
          ) {
            Icon(
              imageVector = Icons.Default.ContentCopy,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Copy Letter", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
          }

          Button(
            onClick = {
              val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Leave Application: ${rec.title}")
                putExtra(Intent.EXTRA_TEXT, letterText)
              }
              context.startActivity(Intent.createChooser(sendIntent, "Share Leave Application"))
            },
            shape = IosSharpButtonShape,
            colors = ButtonDefaults.buttonColors(containerColor = neu.primaryAccent),
            modifier = Modifier
              .weight(1f)
              .testTag("share_sanction_letter_btn")
          ) {
            Icon(
              imageVector = Icons.Default.Share,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Share Memo", fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, softWrap = false)
          }
        }
      }
    }
  }
}

private fun formatCleanPortfolioDouble(value: Double): String {
  return if (value % 1.0 == 0.0) {
    value.toInt().toString()
  } else {
    String.format(Locale.US, "%.1f", value)
  }
}
