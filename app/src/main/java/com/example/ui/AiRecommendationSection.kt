package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.SmartLeaveBridge
import com.example.domain.AiLeaveRecommendation
import com.example.domain.AiLeaveRecommendationEngine
import com.example.domain.AiRecommendationUiState
import com.example.domain.LeaveOptimizerEngine
import com.example.domain.TnHolidays2027Catalog
import com.example.ui.theme.BadgeClGreen
import com.example.ui.theme.BadgeColCyan
import com.example.ui.theme.BadgeRhSlate
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.IosSharpBadgeShape
import com.example.ui.theme.IosSharpButtonShape
import com.example.ui.theme.IosSharpCardShape
import com.example.ui.theme.IosSharpWellShape
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiLeaveRecommendationSection(
  state: LeaveUiState.Success,
  aiState: AiRecommendationUiState,
  onGenerateRecommendations: (String) -> Unit,
  onApplyBridge: (SmartLeaveBridge) -> Unit
) {
  val context = LocalContext.current
  val summary2026 = state.summary2026
  val summary2027 = remember(state.allRecords) {
    LeaveOptimizerEngine.computeYearSummary(state.allRecords, 2027L)
  }
  val colGhAudit = state.colGhAuditAllYears

  val allGh = TnHolidays2027Catalog.allSyncedPublicHolidays()
  val allRh = TnHolidays2027Catalog.allSyncedRestrictedHolidays()
  val totalSyncedHolidays = remember(allGh.size, allRh.size) {
    allGh.size + allRh.size
  }

  val focusStrategies = remember {
    listOf(
      "Maximize Long Weekends (Highest ROI)" to "Prioritize 3 to 9 day upcoming long weekends using minimal CL/RH/COL credits",
      "Consume Expiring GH Worked (FIFO COL)" to "Prioritize using uncompensated GH worked credits before 6-month expiry",
      "Zero-CL Combos (RH + COL Only)" to "Preserve Casual Leave (CL) for emergencies; use only RH & COL credits",
      "Festival Megabridges (Pongal / Puja / Christmas)" to "Target major Tamil Nadu festival clusters for extended family breaks"
    )
  }
  var selectedStrategyIndex by remember { mutableIntStateOf(0) }
  val activeStrategyString = remember(selectedStrategyIndex) {
    focusStrategies[selectedStrategyIndex].first + " — " + focusStrategies[selectedStrategyIndex].second
  }

  // Strategy-aware dynamic Holiday DB scanner across upcoming 2026 & 2027 TN GH/RH calendars
  val instantHolidayDbScan = remember(summary2026, summary2027, colGhAudit, state.allRecords, activeStrategyString, totalSyncedHolidays) {
    AiLeaveRecommendationEngine.scanHolidayDatabaseForLongWeekends(
      summary2026 = summary2026,
      colGhAuditAllYears = colGhAudit,
      records = state.allRecords,
      focusGoal = activeStrategyString,
      summary2027 = summary2027
    )
  }

  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .iosFrostedGlassCard(
        accentColor = MaterialTheme.colorScheme.primary,
        shape = IosSharpCardShape,
        borderWidth = 1.35.dp,
        borderAlpha = 0.55f
      )
      .testTag("ai_leave_optimizer_card"),
    shape = IosSharpCardShape,
    color = Color.Transparent,
    tonalElevation = 0.dp,
    shadowElevation = 4.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // Header Banner
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
              .size(42.dp)
              .clip(IosSharpButtonShape)
              .background(
                Brush.linearGradient(
                  colors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.tertiary
                  )
                )
              ),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.AutoAwesome,
              contentDescription = "AI Leave Optimizer Engine",
              tint = Color.White,
              modifier = Modifier.size(22.dp)
            )
          }
          Column(modifier = Modifier.weight(1f)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = "AI Leave Optimizer Engine",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
              )
              Surface(
                shape = IosSharpBadgeShape,
                color = MaterialTheme.colorScheme.primaryContainer
              ) {
                Text(
                  text = "GEMINI AI + DB",
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.ExtraBold,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  maxLines = 1,
                  softWrap = false
                )
              }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "Scans upcoming TN Holiday DB ($totalSyncedHolidays dates) & live CL/RH/COL balances",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      // Live Holiday DB & Remaining Credit Input Strip (4 uniform pills)
      val activeYearSummary = if (state.selectedYear == 2027L) summary2027 else summary2026
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        LiveCreditInputPill(
          label = "Holiday DB",
          value = "$totalSyncedHolidays dates",
          accent = MaterialTheme.colorScheme.tertiary,
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
        )
        LiveCreditInputPill(
          label = "${activeYearSummary.year} CL Left",
          value = "${formatCreditCount(activeYearSummary.clRemaining)}/12",
          accent = MaterialTheme.colorScheme.primary,
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
        )
        LiveCreditInputPill(
          label = "${activeYearSummary.year} RH Left",
          value = "${activeYearSummary.rhRemaining}/3",
          accent = BadgeRhSlate,
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
        )
        LiveCreditInputPill(
          label = "COL Ready",
          value = "${colGhAudit.availableUnusedGhCount} GH",
          accent = EmeraldOptimal,
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
        )
      }

      // Strategy Selector Chips
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = "OPTIMIZATION STRATEGY FOCUS",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.ExtraBold,
          color = MaterialTheme.colorScheme.primary
        )
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          focusStrategies.forEachIndexed { index, (title, _) ->
            FilterChip(
              selected = selectedStrategyIndex == index,
              onClick = { selectedStrategyIndex = index },
              shape = IosSharpButtonShape,
              label = {
                Text(
                  text = title,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = if (selectedStrategyIndex == index) FontWeight.Bold else FontWeight.Medium,
                  maxLines = 1,
                  softWrap = false
                )
              },
              leadingIcon = if (selectedStrategyIndex == index) {
                {
                  Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                  )
                }
              } else null,
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
              ),
              modifier = Modifier.testTag("ai_strategy_chip_$index")
            )
          }
        }
      }

      // Primary Gemini AI CTA (Always available when not actively loading, and switches tag cleanly)
      if (aiState !is AiRecommendationUiState.Success) {
        Button(
          onClick = { onGenerateRecommendations(activeStrategyString) },
          enabled = aiState !is AiRecommendationUiState.Loading,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("generate_ai_recommendations_btn"),
          shape = IosSharpButtonShape,
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
          )
        ) {
          if (aiState is AiRecommendationUiState.Loading) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              strokeWidth = 2.2.dp,
              color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Synthesizing with Gemini AI...",
              fontWeight = FontWeight.ExtraBold,
              maxLines = 1,
              softWrap = false
            )
          } else {
            Icon(
              imageVector = Icons.Default.AutoAwesome,
              contentDescription = null,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Run Gemini AI Deep Leave Optimizer",
              fontWeight = FontWeight.ExtraBold,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }

      // State-driven Content
      when (aiState) {
        is AiRecommendationUiState.Idle,
        is AiRecommendationUiState.Loading,
        is AiRecommendationUiState.Error -> {
          if (aiState is AiRecommendationUiState.Loading) {
            Surface(
              shape = IosSharpWellShape,
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.30f))
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                CircularProgressIndicator(
                  modifier = Modifier.size(24.dp),
                  strokeWidth = 2.8.dp,
                  color = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Synthesizing Optimal Upcoming Bridges...",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = "Evaluating $totalSyncedHolidays TN Govt Holidays (2026–2027), FIFO GH-Worked pool & FR Annexure VII rules via Gemini AI...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }

          if (aiState is AiRecommendationUiState.Error) {
            Surface(
              shape = IosSharpWellShape,
              color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f))
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                  )
                  Text(
                    text = aiState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
                  )
                }
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "Local Holiday DB recommendations remain active below.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                    modifier = Modifier.weight(1f)
                  )
                  OutlinedButton(
                    onClick = { onGenerateRecommendations(activeStrategyString) },
                    shape = IosSharpButtonShape
                  ) {
                    Icon(
                      imageVector = Icons.Default.Refresh,
                      contentDescription = null,
                      modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retry Gemini AI")
                  }
                }
              }
            }
          }

          // Instant Algorithmic Holiday DB Long-Weekend Scanner Header
          Surface(
            shape = IosSharpWellShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                  )
                  Text(
                    text = "LIVE HOLIDAY DB LONG-WEEKEND SCANNER",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                  )
                }
                Surface(
                  shape = IosSharpBadgeShape,
                  color = EmeraldOptimal.copy(alpha = 0.16f),
                  border = BorderStroke(1.dp, EmeraldOptimal.copy(alpha = 0.45f))
                ) {
                  Text(
                    text = "${instantHolidayDbScan.size} Upcoming",
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = EmeraldOptimal,
                    maxLines = 1,
                    softWrap = false
                  )
                }
              }
              Text(
                text = "Strategy: ${focusStrategies[selectedStrategyIndex].first}. Tap any card to inspect day-by-day schedule or book directly to your Ledger.",
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          // Render Dynamic Upcoming Holiday DB Long-Weekend Cards
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            instantHolidayDbScan.forEachIndexed { index, rec ->
              AiRecommendationItemCard(
                rank = index + 1,
                rec = rec,
                onApplyRecommendation = {
                  onApplyBridge(rec.toSmartLeaveBridge())
                  Toast.makeText(
                    context,
                    "Applying ${rec.title} to your Leave Ledger...",
                    Toast.LENGTH_SHORT
                  ).show()
                },
                onCopyMemo = {
                  copyRecommendationToClipboard(context, rec)
                }
              )
            }
          }
        }

        is AiRecommendationUiState.Success -> {
          // Executive Strategy Summary Banner
          Surface(
            shape = IosSharpWellShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.36f),
            border = BorderStroke(
              1.dp,
              MaterialTheme.colorScheme.primary.copy(alpha = 0.34f)
            ),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                  )
                  Text(
                    text = "GEMINI AI BRIEFING (${aiState.analyzedHolidayCount} DATES)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                  )
                }
                Surface(
                  shape = IosSharpBadgeShape,
                  color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                ) {
                  Text(
                    text = aiState.focusGoal.substringBefore(" —").take(22),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
              Text(
                text = aiState.executiveSummary,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }

          // Top Re-run Toolbar so switching strategies doesn't require scrolling past all cards
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "${aiState.recommendations.size} Gemini AI Plans",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.onSurface
            )
            OutlinedButton(
              onClick = { onGenerateRecommendations(activeStrategyString) },
              modifier = Modifier.testTag("regenerate_ai_recommendations_btn"),
              shape = IosSharpButtonShape,
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Regenerate AI Plan",
                modifier = Modifier.size(15.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Re-run Selected Strategy",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
              )
            }
          }

          // Recommendations List
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            aiState.recommendations.forEachIndexed { index, rec ->
              AiRecommendationItemCard(
                rank = index + 1,
                rec = rec,
                onApplyRecommendation = {
                  onApplyBridge(rec.toSmartLeaveBridge())
                  Toast.makeText(
                    context,
                    "Applying ${rec.title} to your Leave Ledger...",
                    Toast.LENGTH_SHORT
                  ).show()
                },
                onCopyMemo = {
                  copyRecommendationToClipboard(context, rec)
                }
              )
            }
          }
        }
      }
    }
  }
}

private fun formatCreditCount(value: Double): String {
  return if (value % 1.0 == 0.0) {
    value.toInt().toString()
  } else {
    String.format(Locale.US, "%.1f", value)
  }
}

private fun copyRecommendationToClipboard(
  context: Context,
  rec: AiLeaveRecommendation
) {
  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
  val memoText = buildString {
    appendLine("OPTIMAL LEAVE APPLICATION PLAN — ${rec.title}")
    appendLine("Window: ${ClinicalDateFormatter.formatRangeFriendly(rec.startDateIso, rec.endDateIso)} (${rec.totalDaysOff} Consecutive Days Off)")
    appendLine("Leave Credits Used: ${formatCreditCount(rec.leaveCreditsUsed)} (${rec.creditMixSummary}) | ${rec.efficiencyMultiplier}x ROI | +${rec.freeDaysUnlocked} Free GH/Weekend Days")
    if (rec.exactDatesToApply.isNotEmpty()) {
      val formattedExact = rec.exactDatesToApply.joinToString(", ") { raw ->
        val cleanIso = raw.trim().take(10)
        if (cleanIso.length == 10 && cleanIso[4] == '-' && cleanIso[7] == '-') {
          val suffix = raw.trim().drop(10).trim()
          val friendly = ClinicalDateFormatter.formatFriendlyWithDay(cleanIso)
          if (suffix.isNotEmpty()) "$friendly $suffix" else friendly
        } else {
          raw
        }
      }
      appendLine("Exact Dates to Apply for Leave: $formattedExact")
    }
    appendLine("Schedule Breakdown:")
    rec.dailyPlan.forEach { day ->
      appendLine("  • ${ClinicalDateFormatter.formatFriendlyWithDay(day.dateIso)} [${day.creditType}]: ${day.description}")
    }
    appendLine("Strategic Rationale: ${rec.whyOptimalForHod}")
    appendLine("FIFO / Rule Check: ${rec.tnRuleComplianceCheck}")
    if (rec.projectedBalanceAfter.isNotBlank()) {
      appendLine("Projected Balance After: ${rec.projectedBalanceAfter}")
    }
  }
  clipboard.setPrimaryClip(ClipData.newPlainText("Optimal Leave Plan", memoText))
  Toast.makeText(context, "Copied optimal leave plan to clipboard", Toast.LENGTH_SHORT).show()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiRecommendationItemCard(
  rank: Int,
  rec: AiLeaveRecommendation,
  onApplyRecommendation: () -> Unit,
  onCopyMemo: () -> Unit
) {
  var expanded by remember(rec.id) { mutableStateOf(rank <= 2) }

  val primaryTag = when {
    rec.creditMixSummary.contains("COL", ignoreCase = true) -> "COL"
    rec.creditMixSummary.contains("RH", ignoreCase = true) -> "RH"
    else -> "CL"
  }

  val badgeColor = when (primaryTag) {
    "COL" -> BadgeColCyan
    "RH" -> BadgeRhSlate
    else -> BadgeClGreen
  }

  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .iosFrostedGlassCard(
        accentColor = badgeColor,
        shape = IosSharpCardShape,
        borderWidth = 1.1.dp,
        borderAlpha = 0.42f
      )
      .clickable { expanded = !expanded }
      .testTag("ai_rec_card_$rank"),
    shape = IosSharpCardShape,
    color = Color.Transparent,
    tonalElevation = 0.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // Top Row: Rank + Title + ROI Multiplier Badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.weight(1f)
        ) {
          Box(
            modifier = Modifier
              .size(28.dp)
              .clip(CircleShape)
              .background(badgeColor),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "#$rank",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.ExtraBold,
              color = Color.White
            )
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = rec.title,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = badgeColor,
                modifier = Modifier.size(13.dp)
              )
              Text(
                text = ClinicalDateFormatter.formatRangeFriendly(rec.startDateIso, rec.endDateIso),
                style = MaterialTheme.typography.labelSmall.copy(
                  fontFamily = JetBrainsMonoFontFamily
                ),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }

        // Efficiency & Days Off Badges
        Column(
          horizontalAlignment = Alignment.End,
          verticalArrangement = Arrangement.spacedBy(4.dp),
          modifier = Modifier.padding(start = 8.dp)
        ) {
          Surface(
            shape = IosSharpBadgeShape,
            color = EmeraldOptimal.copy(alpha = 0.16f),
            border = BorderStroke(1.dp, EmeraldOptimal.copy(alpha = 0.45f))
          ) {
            Text(
              text = "${rec.totalDaysOff}d Off • ${rec.efficiencyMultiplier}x",
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.ExtraBold,
              color = EmeraldOptimal,
              maxLines = 1,
              softWrap = false
            )
          }
          Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = IosSharpBadgeShape,
              color = badgeColor.copy(alpha = 0.16f)
            ) {
              Text(
                text = "Use ${formatCreditCount(rec.leaveCreditsUsed)} Cr",
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = badgeColor,
                maxLines = 1,
                softWrap = false
              )
            }
            if (rec.freeDaysUnlocked > 0) {
              Surface(
                shape = IosSharpBadgeShape,
                color = MaterialTheme.colorScheme.tertiaryContainer
              ) {
                Text(
                  text = "+${rec.freeDaysUnlocked} Free",
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onTertiaryContainer,
                  maxLines = 1,
                  softWrap = false
                )
              }
            }
          }
        }
      }

      // Always-Visible Callout: EXACT DATES TO APPLY FOR LEAVE
      if (rec.exactDatesToApply.isNotEmpty()) {
        Surface(
          shape = IosSharpWellShape,
          color = badgeColor.copy(alpha = 0.10f),
          border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.30f))
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = badgeColor,
                modifier = Modifier.size(14.dp)
              )
              Text(
                text = "EXACT DATES TO APPLY FOR LEAVE (${rec.exactDatesToApply.size} ${if (rec.exactDatesToApply.size == 1) "DATE" else "DATES"}):",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = badgeColor
              )
            }
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              rec.exactDatesToApply.forEach { applyDateChip ->
                val cleanIso = applyDateChip.trim().take(10)
                val displayChip = if (cleanIso.length == 10 && cleanIso[4] == '-' && cleanIso[7] == '-') {
                  val suffix = applyDateChip.trim().drop(10).trim()
                  val friendly = ClinicalDateFormatter.formatFriendlyWithDay(cleanIso)
                  if (suffix.isNotEmpty()) "$friendly $suffix" else friendly
                } else {
                  applyDateChip
                }
                Surface(
                  shape = IosSharpBadgeShape,
                  color = MaterialTheme.colorScheme.surface,
                  border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.45f))
                ) {
                  Text(
                    text = displayChip,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontFamily = JetBrainsMonoFontFamily
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }
              }
            }
          }
        }
      }

      // Strategic rationale snippet
      Text(
        text = rec.whyOptimalForHod,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = if (expanded) 5 else 2,
        overflow = TextOverflow.Ellipsis
      )

      AnimatedVisibility(
        visible = expanded,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
      ) {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
          )

          // Day-by-Day Chronological Schedule Pills
          Text(
            text = "FULL LONG-WEEKEND SCHEDULE (${rec.totalDaysOff} CONSECUTIVE DAYS OFF)",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
          )

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            rec.dailyPlan.forEach { dayStep ->
              val isLeaveAction = dayStep.creditType.uppercase(Locale.US) in setOf("CL", "RH", "COL")
              Surface(
                shape = IosSharpBadgeShape,
                color = if (isLeaveAction) {
                  badgeColor.copy(alpha = 0.15f)
                } else {
                  MaterialTheme.colorScheme.surface
                },
                border = BorderStroke(
                  width = 1.dp,
                  color = if (isLeaveAction) {
                    badgeColor.copy(alpha = 0.45f)
                  } else {
                    MaterialTheme.colorScheme.outlineVariant
                  }
                )
              ) {
                Text(
                  text = "${dayStep.dayLabel}: ${dayStep.description}",
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = JetBrainsMonoFontFamily
                  ),
                  fontWeight = if (isLeaveAction) FontWeight.Bold else FontWeight.Medium,
                  color = if (isLeaveAction) badgeColor else MaterialTheme.colorScheme.onSurface
                )
              }
            }
          }

          // Compliance & Projected Balance Footer
          Surface(
            shape = IosSharpWellShape,
            color = MaterialTheme.colorScheme.surface
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = null,
                  tint = EmeraldOptimal,
                  modifier = Modifier.size(15.dp)
                )
                Text(
                  text = rec.tnRuleComplianceCheck,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              if (rec.projectedBalanceAfter.isNotBlank()) {
                Text(
                  text = "Projected Balance After: ${rec.projectedBalanceAfter}",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = JetBrainsMonoFontFamily
                  ),
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary
                )
              }
            }
          }

          // Action Buttons Row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Button(
              onClick = onApplyRecommendation,
              modifier = Modifier
                .weight(1f)
                .testTag("apply_ai_rec_btn_$rank"),
              shape = IosSharpButtonShape,
              colors = ButtonDefaults.buttonColors(
                containerColor = badgeColor
              ),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF042F2E),
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Apply Exact Leave Dates to Ledger",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF042F2E),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
              )
            }

            IconButton(
              onClick = onCopyMemo,
              modifier = Modifier.testTag("copy_ai_rec_btn_$rank")
            ) {
              Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy Optimal Leave Plan",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }
      }

      // Expand/Collapse indicator row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (expanded) "Hide day-by-day breakdown" else "View day-by-day breakdown & 1-tap apply",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.SemiBold
        )
        Icon(
          imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(16.dp)
        )
      }
    }
  }
}

@Composable
private fun LiveCreditInputPill(
  label: String,
  value: String,
  accent: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier,
    shape = IosSharpButtonShape,
    color = accent.copy(alpha = 0.11f),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.28f))
  ) {
    Column(
      modifier = Modifier
        .fillMaxHeight()
        .padding(horizontal = 8.dp, vertical = 7.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(
          fontSize = 10.sp,
          lineHeight = 12.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.labelMedium.copy(
          fontFamily = JetBrainsMonoFontFamily,
          fontSize = 11.sp,
          lineHeight = 14.sp
        ),
        fontWeight = FontWeight.ExtraBold,
        color = accent,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}
