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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.domain.OptimizerConstraints
import com.example.domain.TentativeHoldRecord
import com.example.domain.TnHolidays2027Catalog
import com.example.ui.theme.BadgeClGreen
import com.example.ui.theme.BadgeColCyan
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
import java.util.Locale

private val ElVioletBadge = Color(0xFF7C3AED)
private val HoldIndigoBadge = Color(0xFF6366F1)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiLeaveRecommendationSection(
  state: LeaveUiState.Success,
  aiState: AiRecommendationUiState,
  constraints: OptimizerConstraints = OptimizerConstraints(),
  onUpdateConstraints: (OptimizerConstraints) -> Unit = {},
  tentativeHolds: Map<String, TentativeHoldRecord> = emptyMap(),
  deepLinkGoalHint: String? = null,
  onConsumeDeepLink: () -> Unit = {},
  onGenerateRecommendations: (String) -> Unit,
  onApplyBridge: (SmartLeaveBridge) -> Unit,
  onApplyPortfolio: (List<AiLeaveRecommendation>) -> Unit = {},
  onToggleTentativeHold: (AiLeaveRecommendation) -> Unit = {},
  onSimulateInBuilder: (AiLeaveRecommendation) -> Unit = {},
  loggedDutyCount: Int = 0,
  protectLoggedDutyShifts: Boolean = true,
  onToggleProtectLoggedDutyShifts: (Boolean) -> Unit = {},
  onToggleBlockedDate: (String) -> Unit = {},
  onSetPinnedDate: (String, String?) -> Unit = { _, _ -> },
  onTogglePlannedWorkHoliday: (String) -> Unit = {},
  onClearSteeringOverrides: () -> Unit = {},
  onRecalibrateStrategy: (isGeminiActive: Boolean, focusGoal: String) -> Unit = { _, _ -> }
) {
  val context = LocalContext.current
  val summary2026 = state.summary2026
  val summary2027 = remember(state.allRecords) {
    LeaveOptimizerEngine.computeYearSummary(state.allRecords, 2027L)
  }
  val colGhAudit = state.colGhAuditAllYears

  val allGh = TnHolidays2027Catalog.allSyncedPublicHolidays()
  val allRh = TnHolidays2027Catalog.allSyncedRestrictedHolidays()
  val kbRevision = TnHolidays2027Catalog.knowledgeBaseRevision
  val totalSyncedHolidays = remember(allGh.size, allRh.size, kbRevision) {
    allGh.size + allRh.size
  }

  val focusStrategies = remember {
    listOf(
      "Maximize Long Weekends (Highest ROI)" to "Prioritize 3 to 9 day upcoming long weekends using minimal CL/RH/COL credits",
      "Consume Expiring GH Worked (FIFO COL)" to "Prioritize using uncompensated GH worked credits before 6-month expiry",
      "Zero-CL Combos (RH + COL Only)" to "Preserve Casual Leave (CL) for emergencies; use only RH & COL credits",
      "Festival Megabridges (Pongal / Puja / Christmas)" to "Target major Tamil Nadu festival clusters for extended family breaks",
      "Earned Leave (EL) + LTC 7–9d Break" to "Optimize Rule-8 EL strictly prefixed/suffixed by weekends & GH so 0 EL is wasted on holidays"
    )
  }
  var selectedStrategyIndex by remember { mutableIntStateOf(0) }
  var selectedPortfolioIds by remember { mutableStateOf(setOf<String>()) }
  var sanctionLetterRec by remember { mutableStateOf<AiLeaveRecommendation?>(null) }
  var showSteeringCalendar by remember { mutableStateOf(false) }
  var steeringDialogDateIso by remember { mutableStateOf<String?>(null) }

  // Handle Notification Center -> AI Optimizer deep links seamlessly
  LaunchedEffect(deepLinkGoalHint) {
    if (!deepLinkGoalHint.isNullOrBlank()) {
      val hint = deepLinkGoalHint.lowercase(Locale.US)
      val matchedIdx = when {
        hint.contains("col") || hint.contains("expir") || hint.contains("gh") -> 1
        hint.contains("zero") || hint.contains("preserve cl") -> 2
        hint.contains("festival") || hint.contains("pongal") || hint.contains("christmas") -> 3
        hint.contains("el") || hint.contains("ltc") || hint.contains("earned") -> 4
        else -> 0
      }
      selectedStrategyIndex = matchedIdx
      val updated = when (matchedIdx) {
        1 -> constraints.copy(prioritizeExpiringCol = true)
        2 -> constraints.copy(strictZeroCl = true)
        4 -> constraints.copy(allowElBridges = true)
        else -> constraints
      }
      if (updated != constraints) onUpdateConstraints(updated)
      onConsumeDeepLink()
    }
  }

  // Merge selected strategy chip with explicit constraint toggles
  val effectiveUiConstraints = remember(constraints, selectedStrategyIndex) {
    when (selectedStrategyIndex) {
      1 -> constraints.copy(prioritizeExpiringCol = true)
      2 -> constraints.copy(strictZeroCl = true)
      4 -> constraints.copy(allowElBridges = true)
      else -> constraints
    }
  }

  val activeStrategyString = remember(selectedStrategyIndex, effectiveUiConstraints) {
    val base = focusStrategies[selectedStrategyIndex].first + " — " + focusStrategies[selectedStrategyIndex].second
    effectiveUiConstraints.buildStrategySummary(base)
  }

  // Live Constraint-Aware Holiday DB Scanner (automatically refreshes on WorkManager KB updates)
  val instantHolidayDbScan = remember(
    summary2026,
    summary2027,
    colGhAudit,
    state.allRecords,
    activeStrategyString,
    effectiveUiConstraints,
    totalSyncedHolidays,
    kbRevision
  ) {
    AiLeaveRecommendationEngine.scanHolidayDatabaseForLongWeekends(
      summary2026 = summary2026,
      colGhAuditAllYears = colGhAudit,
      records = state.allRecords,
      focusGoal = activeStrategyString,
      summary2027 = summary2027,
      constraints = effectiveUiConstraints
    )
  }

  val displayedRecommendations = remember(aiState, instantHolidayDbScan) {
    when (aiState) {
      is AiRecommendationUiState.Success -> aiState.recommendations
      else -> instantHolidayDbScan
    }
  }

  val selectedPortfolioRecs = remember(selectedPortfolioIds, displayedRecommendations) {
    displayedRecommendations.filter { it.id in selectedPortfolioIds }
  }

  val portfolioImpact = remember(
    selectedPortfolioRecs,
    summary2026,
    summary2027,
    colGhAudit,
    effectiveUiConstraints
  ) {
    if (selectedPortfolioRecs.isEmpty()) {
      null
    } else {
      AiLeaveRecommendationEngine.computePortfolioImpact(
        selectedRecs = selectedPortfolioRecs,
        summary2026 = summary2026,
        summary2027 = summary2027,
        colGhAuditAllYears = colGhAudit,
        constraints = effectiveUiConstraints
      )
    }
  }

  val neu = LocalNeumorphicColors.current

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .neumorphicExtruded(
        shape = IosSharpCardShape,
        elevation = 6.dp,
        blurRadius = 12.dp,
        accentTint = neu.primaryAccent,
        accentTintAlpha = 0.04f,
        borderAlpha = 0.36f,
        neu = neu
      )
      .testTag("ai_leave_optimizer_card")
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
              .neumorphicExtruded(
                shape = IosSharpButtonShape,
                elevation = 3.dp,
                blurRadius = 6.dp,
                surfaceColor = neu.primaryAccent,
                neu = neu
              )
              .background(
                Brush.linearGradient(
                  colors = listOf(
                    neu.primaryAccent,
                    MaterialTheme.colorScheme.tertiary
                  )
                ),
                shape = IosSharpButtonShape
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
                color = neu.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
              )
              Box(
                modifier = Modifier.neumorphicInset(
                  shape = IosSharpBadgeShape,
                  depth = 1.5.dp,
                  blurRadius = 3.dp,
                  accentTint = neu.primaryAccent,
                  accentTintAlpha = 0.12f,
                  borderColor = neu.primaryAccent.copy(alpha = 0.28f),
                  neu = neu
                )
              ) {
                Text(
                  text = "GEMINI + SOLVER",
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.ExtraBold,
                  color = neu.primaryAccent,
                  maxLines = 1,
                  softWrap = false
                )
              }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "Joint non-overlapping solver • ${effectiveUiConstraints.effectiveWeekendPolicy().shortChip} • $totalSyncedHolidays TN dates • FIFO COL & OPD safe",
              style = MaterialTheme.typography.labelSmall,
              color = neu.textSecondary
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
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          focusStrategies.forEachIndexed { index, (title, _) ->
            val isSelected = selectedStrategyIndex == index
            Box(
              modifier = Modifier
                .then(
                  if (isSelected) {
                    Modifier.neumorphicExtruded(
                      shape = IosSharpButtonShape,
                      elevation = 3.dp,
                      blurRadius = 6.dp,
                      surfaceColor = if (index == 4) ElVioletBadge else neu.primaryAccent,
                      neu = neu
                    )
                  } else {
                    Modifier.neumorphicExtruded(
                      shape = IosSharpButtonShape,
                      elevation = 2.dp,
                      blurRadius = 4.dp,
                      borderAlpha = 0.24f,
                      neu = neu
                    )
                  }
                )
                .clickable {
                  selectedStrategyIndex = index
                  if (index == 4 && !constraints.allowElBridges) {
                    onUpdateConstraints(constraints.copy(allowElBridges = true))
                  }
                }
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("ai_strategy_chip_$index")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                if (isSelected) {
                  Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                  )
                }
                Text(
                  text = title,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSelected) Color.White else neu.textPrimary,
                  maxLines = 1,
                  softWrap = false
                )
              }
            }
          }
        }
      }

      // Interactive Natural-Language Ask + Constraint Tuning Panel
      OptimizerConstraintControlPanel(
        constraints = effectiveUiConstraints,
        onUpdateConstraints = { updated -> onUpdateConstraints(updated) },
        onRunWithPrompt = {
          onUpdateConstraints(effectiveUiConstraints)
          onGenerateRecommendations(activeStrategyString)
        },
        loggedDutyCount = loggedDutyCount,
        protectLoggedDutyShifts = protectLoggedDutyShifts,
        onToggleProtectLoggedDutyShifts = onToggleProtectLoggedDutyShifts
      )

      // Expandable Interactive Date Steering Calendar + Work-Duty (+1 Virtual COL) Picker
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .weight(1f)
            .then(
              if (showSteeringCalendar) {
                Modifier.neumorphicInset(
                  shape = IosSharpButtonShape,
                  depth = 2.dp,
                  blurRadius = 4.dp,
                  accentTint = neu.primaryAccent,
                  accentTintAlpha = 0.12f,
                  borderColor = neu.primaryAccent.copy(alpha = 0.45f),
                  neu = neu
                )
              } else {
                Modifier.neumorphicExtruded(
                  shape = IosSharpButtonShape,
                  elevation = 2.dp,
                  blurRadius = 4.dp,
                  borderAlpha = 0.24f,
                  neu = neu
                )
              }
            )
            .clickable { showSteeringCalendar = !showSteeringCalendar }
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("toggle_steering_calendar_btn")
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
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = "Steer calendar dates",
                tint = neu.primaryAccent,
                modifier = Modifier.size(16.dp)
              )
              Text(
                text = "Steer Dates: Pin, Block or Mark Holiday Work Duty (+1 COL)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = neu.primaryAccent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            Icon(
              imageVector = if (showSteeringCalendar) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
              contentDescription = null,
              tint = neu.primaryAccent,
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }

      AnimatedVisibility(
        visible = showSteeringCalendar,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
      ) {
        SteeringMonthCalendarCard(
          constraints = effectiveUiConstraints,
          records = state.allRecords,
          recommendations = displayedRecommendations,
          selectedYearHint = state.selectedYear,
          onSelectDateForSteering = { iso -> steeringDialogDateIso = iso }
        )
      }

      // Active Steering Overrides + Recalibrate Strategy Banner
      SteeringOverridesAndRecalibrateBanner(
        constraints = effectiveUiConstraints,
        isGeminiActive = aiState is AiRecommendationUiState.Success,
        onClearAllOverrides = onClearSteeringOverrides,
        onRecalibrate = {
          onRecalibrateStrategy(aiState is AiRecommendationUiState.Success, activeStrategyString)
        }
      )

      // Multi-Plan Portfolio Impact Dock (appears when ≥1 card is selected)
      if (portfolioImpact != null) {
        PortfolioImpactSummaryDock(
          impact = portfolioImpact,
          onApplyPortfolio = {
            onApplyPortfolio(selectedPortfolioRecs)
            selectedPortfolioIds = emptySet()
            Toast.makeText(
              context,
              "Committing ${selectedPortfolioRecs.size} portfolio plan(s) to Firestore Ledger...",
              Toast.LENGTH_SHORT
            ).show()
          },
          onClearSelection = { selectedPortfolioIds = emptySet() }
        )
      }

      // Primary Gemini AI CTA
      val actions = LocalFunctionalActionColors.current
      if (aiState !is AiRecommendationUiState.Success) {
        Button(
          onClick = {
            onUpdateConstraints(effectiveUiConstraints)
            onGenerateRecommendations(activeStrategyString)
          },
          enabled = aiState !is AiRecommendationUiState.Loading,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("generate_ai_recommendations_btn"),
          shape = IosSharpButtonShape,
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = actions.aiOptimize,
            contentColor = Color.White
          )
        ) {
          if (aiState is AiRecommendationUiState.Loading) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              strokeWidth = 2.2.dp,
              color = Color.White
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
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .neumorphicInset(
                  shape = IosSharpWellShape,
                  depth = 2.5.dp,
                  blurRadius = 5.dp,
                  accentTint = neu.primaryAccent,
                  accentTintAlpha = 0.08f,
                  borderColor = neu.primaryAccent.copy(alpha = 0.30f),
                  neu = neu
                )
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
                  color = neu.primaryAccent
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Synthesizing Constraint-Verified Bridges...",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = neu.textPrimary
                  )
                  Text(
                    text = "Running adjacency check, FIFO COL decay, OPD safety, Rule-8 EL & Gemini AI synthesis...",
                    style = MaterialTheme.typography.labelSmall,
                    color = neu.textSecondary
                  )
                }
              }
            }
          }

          if (aiState is AiRecommendationUiState.Error) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .neumorphicInset(
                  shape = IosSharpWellShape,
                  depth = 2.5.dp,
                  blurRadius = 5.dp,
                  accentTint = MaterialTheme.colorScheme.error,
                  accentTintAlpha = 0.12f,
                  borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.45f),
                  neu = neu
                )
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
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                  )
                }
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "Local Constraint Solver recommendations remain active below.",
                    style = MaterialTheme.typography.labelSmall,
                    color = neu.textSecondary,
                    modifier = Modifier.weight(1f)
                  )
                  OutlinedButton(
                    onClick = { onGenerateRecommendations(activeStrategyString) },
                    shape = IosSharpButtonShape,
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, actions.aiOptimize)
                  ) {
                    Icon(
                      imageVector = Icons.Default.Refresh,
                      contentDescription = null,
                      tint = actions.aiOptimize,
                      modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retry Gemini AI", color = actions.aiOptimize, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }

          // Instant Algorithmic Constraint Solver Header
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .neumorphicInset(
                shape = IosSharpWellShape,
                depth = 2.5.dp,
                blurRadius = 5.dp,
                accentTint = neu.primaryAccent,
                accentTintAlpha = 0.08f,
                borderColor = neu.primaryAccent.copy(alpha = 0.28f),
                neu = neu
              )
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
                    tint = neu.primaryAccent,
                    modifier = Modifier.size(16.dp)
                  )
                  Text(
                    text = "LIVE CONSTRAINT SOLVER (${effectiveUiConstraints.horizonMode.shortChip.uppercase()})",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = neu.primaryAccent,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                  )
                }
                Row(
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  if (instantHolidayDbScan.isNotEmpty()) {
                    val allSelected = instantHolidayDbScan.all { it.id in selectedPortfolioIds }
                    Box(
                      modifier = Modifier
                        .neumorphicExtruded(
                          shape = IosSharpBadgeShape,
                          elevation = 1.5.dp,
                          blurRadius = 3.dp,
                          accentTint = neu.primaryAccent,
                          accentTintAlpha = 0.08f,
                          borderAlpha = 0.28f,
                          neu = neu
                        )
                        .clickable {
                          selectedPortfolioIds = if (allSelected) {
                            emptySet()
                          } else {
                            instantHolidayDbScan.map { it.id }.toSet()
                          }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("select_all_portfolio_btn")
                    ) {
                      Text(
                        text = if (allSelected) "Clear All" else "Select All",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = neu.primaryAccent,
                        maxLines = 1,
                        softWrap = false
                      )
                    }
                  }
                  Box(
                    modifier = Modifier.neumorphicInset(
                      shape = IosSharpBadgeShape,
                      depth = 1.5.dp,
                      blurRadius = 3.dp,
                      accentTint = EmeraldOptimal,
                      accentTintAlpha = 0.14f,
                      borderColor = EmeraldOptimal.copy(alpha = 0.45f),
                      neu = neu
                    )
                  ) {
                    Text(
                      text = "${instantHolidayDbScan.size} Verified",
                      modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.ExtraBold,
                      color = EmeraldOptimal,
                      maxLines = 1,
                      softWrap = false
                    )
                  }
                }
              }
              Text(
                text = "Tap any day pill inside a card to Pin/Block, '+ Portfolio' to model cumulative balances, or 'Hold' to reserve tentatively.",
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = neu.textSecondary
              )
            }
          }

          // Render Dynamic Upcoming Constraint-Verified Cards
          if (instantHolidayDbScan.isEmpty()) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .neumorphicInset(
                  shape = IosSharpWellShape,
                  depth = 2.dp,
                  blurRadius = 4.dp,
                  neu = neu
                )
                .padding(14.dp)
            ) {
              Text(
                text = "No upcoming windows match your current strict filters (e.g., Horizon + CL reserve + Zero-CL). Relax a constraint chip above or widen the horizon to All '26–27.",
                style = MaterialTheme.typography.bodySmall,
                color = neu.textSecondary
              )
            }
          } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              instantHolidayDbScan.forEachIndexed { index, rec ->
                val isHeld = rec.dailyPlan.any {
                  it.creditType.uppercase(Locale.US) in setOf("CL", "HALF_CL", "RH", "COL", "EL") &&
                    tentativeHolds[it.dateIso]?.bridgeId == rec.id
                }
                AiRecommendationItemCard(
                  rank = index + 1,
                  rec = rec,
                  isSelectedForPortfolio = rec.id in selectedPortfolioIds,
                  isHeldOnCalendar = isHeld,
                  onTogglePortfolioSelection = {
                    selectedPortfolioIds = if (rec.id in selectedPortfolioIds) {
                      selectedPortfolioIds - rec.id
                    } else {
                      selectedPortfolioIds + rec.id
                    }
                  },
                  onApplyRecommendation = {
                    onApplyBridge(rec.toSmartLeaveBridge())
                    Toast.makeText(
                      context,
                      "Applying ${rec.title} to your Leave Ledger...",
                      Toast.LENGTH_SHORT
                    ).show()
                  },
                  onToggleTentativeHold = { onToggleTentativeHold(rec) },
                  onSimulateInBuilder = { onSimulateInBuilder(rec) },
                  onOpenSanctionLetter = { sanctionLetterRec = rec },
                  onCopyMemo = { copyRecommendationToClipboard(context, rec) },
                  onSteerDate = { dateIso -> steeringDialogDateIso = dateIso }
                )
              }
            }
          }
        }

        is AiRecommendationUiState.Success -> {
          // Executive Strategy Summary Banner
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .neumorphicInset(
                shape = IosSharpWellShape,
                depth = 2.5.dp,
                blurRadius = 5.dp,
                accentTint = neu.primaryAccent,
                accentTintAlpha = 0.09f,
                borderColor = neu.primaryAccent.copy(alpha = 0.34f),
                neu = neu
              )
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
                    tint = neu.primaryAccent,
                    modifier = Modifier.size(16.dp)
                  )
                  Text(
                    text = "GEMINI AI BRIEFING (${aiState.analyzedHolidayCount} DATES)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = neu.primaryAccent,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                  )
                }
                Box(
                  modifier = Modifier.neumorphicExtruded(
                    shape = IosSharpBadgeShape,
                    elevation = 1.5.dp,
                    blurRadius = 3.dp,
                    borderAlpha = 0.22f,
                    neu = neu
                  )
                ) {
                  Text(
                    text = aiState.focusGoal.substringBefore(" —").take(22),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = neu.textSecondary,
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
                color = neu.textPrimary
              )
              if (aiState.appliedConstraintsSummary.isNotBlank()) {
                Text(
                  text = "Active Rules: ${aiState.appliedConstraintsSummary}",
                  style = MaterialTheme.typography.labelSmall,
                  color = neu.primaryAccent,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }
          }

          // Top Re-run & Select-All Toolbar
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
              Text(
                text = "${aiState.recommendations.size} Verified Gemini AI Plans",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              if (aiState.recommendations.isNotEmpty()) {
                val allSelected = aiState.recommendations.all { it.id in selectedPortfolioIds }
                Box(
                  modifier = Modifier
                    .neumorphicExtruded(
                      shape = IosSharpBadgeShape,
                      elevation = 1.5.dp,
                      blurRadius = 3.dp,
                      accentTint = neu.primaryAccent,
                      accentTintAlpha = 0.08f,
                      borderAlpha = 0.28f,
                      neu = neu
                    )
                    .clickable {
                      selectedPortfolioIds = if (allSelected) {
                        emptySet()
                      } else {
                        aiState.recommendations.map { it.id }.toSet()
                      }
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("select_all_gemini_portfolio_btn")
                ) {
                  Text(
                    text = if (allSelected) "Clear" else "Select All",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = neu.primaryAccent,
                    maxLines = 1,
                    softWrap = false
                  )
                }
              }
            }
            OutlinedButton(
              onClick = {
                onUpdateConstraints(effectiveUiConstraints)
                onGenerateRecommendations(activeStrategyString)
              },
              modifier = Modifier.testTag("regenerate_ai_recommendations_btn"),
              shape = IosSharpButtonShape,
              border = androidx.compose.foundation.BorderStroke(1.2.dp, actions.aiOptimize),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Regenerate AI Plan",
                tint = actions.aiOptimize,
                modifier = Modifier.size(15.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Re-run Strategy",
                style = MaterialTheme.typography.labelSmall,
                color = actions.aiOptimize,
                fontWeight = FontWeight.Bold
              )
            }
          }

          // Recommendations List
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            aiState.recommendations.forEachIndexed { index, rec ->
              val isHeld = rec.dailyPlan.any {
                it.creditType.uppercase(Locale.US) in setOf("CL", "HALF_CL", "RH", "COL", "EL") &&
                  tentativeHolds[it.dateIso]?.bridgeId == rec.id
              }
              AiRecommendationItemCard(
                rank = index + 1,
                rec = rec,
                isSelectedForPortfolio = rec.id in selectedPortfolioIds,
                isHeldOnCalendar = isHeld,
                onTogglePortfolioSelection = {
                  selectedPortfolioIds = if (rec.id in selectedPortfolioIds) {
                    selectedPortfolioIds - rec.id
                  } else {
                    selectedPortfolioIds + rec.id
                  }
                },
                onApplyRecommendation = {
                  onApplyBridge(rec.toSmartLeaveBridge())
                  Toast.makeText(
                    context,
                    "Applying ${rec.title} to your Leave Ledger...",
                    Toast.LENGTH_SHORT
                  ).show()
                },
                onToggleTentativeHold = { onToggleTentativeHold(rec) },
                onSimulateInBuilder = { onSimulateInBuilder(rec) },
                onOpenSanctionLetter = { sanctionLetterRec = rec },
                onCopyMemo = { copyRecommendationToClipboard(context, rec) },
                onSteerDate = { dateIso -> steeringDialogDateIso = dateIso }
              )
            }
          }
        }
      }
    }
  }

  sanctionLetterRec?.let { rec ->
    HodSanctionLetterDialog(
      rec = rec,
      onDismiss = { sanctionLetterRec = null }
    )
  }

  steeringDialogDateIso?.let { dateIso ->
    OptimizerDaySteeringDialog(
      dateIso = dateIso,
      constraints = effectiveUiConstraints,
      recordsOnDate = state.allRecords.filter { it.dateIso == dateIso },
      onSetPinnedDate = onSetPinnedDate,
      onToggleBlockedDate = onToggleBlockedDate,
      onTogglePlannedWorkHoliday = onTogglePlannedWorkHoliday,
      onDismiss = { steeringDialogDateIso = null }
    )
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
    if (rec.opdImpactNote.isNotBlank()) {
      appendLine("Clinical OPD Impact: ${rec.opdImpactNote}")
    }
    if (rec.projectedBalanceAfter.isNotBlank()) {
      appendLine("Projected Balance After: ${rec.projectedBalanceAfter}")
    }
    if (rec.portfolioBalanceAfter.isNotBlank()) {
      appendLine("Cumulative Sequence Balance: ${rec.portfolioBalanceAfter}")
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
  isSelectedForPortfolio: Boolean,
  isHeldOnCalendar: Boolean,
  onTogglePortfolioSelection: () -> Unit,
  onApplyRecommendation: () -> Unit,
  onToggleTentativeHold: () -> Unit,
  onSimulateInBuilder: () -> Unit,
  onOpenSanctionLetter: () -> Unit,
  onCopyMemo: () -> Unit,
  onSteerDate: (String) -> Unit = {}
) {
  val neu = LocalNeumorphicColors.current
  var expanded by remember(rec.id) { mutableStateOf(rank <= 1) }

  val primaryTag = when {
    rec.isElBridge || rec.creditMixSummary.contains("EL", ignoreCase = true) -> "EL"
    rec.creditMixSummary.contains("COL", ignoreCase = true) -> "COL"
    rec.creditMixSummary.contains("RH", ignoreCase = true) -> "RH"
    else -> "CL"
  }

  val badgeColor = when (primaryTag) {
    "EL" -> ElVioletBadge
    "COL" -> BadgeColCyan
    "RH" -> BadgeRhSlate
    else -> BadgeClGreen
  }
  val pillTextAccent = if (primaryTag == "RH" && neu.isDark) Color(0xFF94A3B8) else badgeColor

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .neumorphicExtruded(
        shape = IosSharpCardShape,
        elevation = if (isSelectedForPortfolio) 5.5.dp else 4.dp,
        blurRadius = 8.dp,
        accentTint = if (isSelectedForPortfolio) EmeraldOptimal else badgeColor,
        accentTintAlpha = if (isSelectedForPortfolio) 0.06f else 0.03f,
        borderAlpha = if (isSelectedForPortfolio) 0.55f else 0.32f,
        neu = neu
      )
      .clickable { expanded = !expanded }
      .testTag("ai_rec_card_$rank")
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
              .neumorphicExtruded(
                shape = CircleShape,
                elevation = 2.5.dp,
                blurRadius = 4.dp,
                surfaceColor = badgeColor,
                neu = neu
              ),
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
              color = neu.textPrimary
            )
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = pillTextAccent,
                modifier = Modifier.size(13.dp)
              )
              Text(
                text = ClinicalDateFormatter.formatRangeFriendly(rec.startDateIso, rec.endDateIso),
                style = MaterialTheme.typography.labelSmall.copy(
                  fontFamily = JetBrainsMonoFontFamily
                ),
                fontWeight = FontWeight.SemiBold,
                color = neu.textSecondary,
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
          Box(
            modifier = Modifier.neumorphicInset(
              shape = IosSharpBadgeShape,
              depth = 1.5.dp,
              blurRadius = 3.dp,
              accentTint = EmeraldOptimal,
              accentTintAlpha = 0.14f,
              borderColor = EmeraldOptimal.copy(alpha = 0.45f),
              neu = neu
            )
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
            Box(
              modifier = Modifier.neumorphicInset(
                shape = IosSharpBadgeShape,
                depth = 1.5.dp,
                blurRadius = 3.dp,
                accentTint = badgeColor,
                accentTintAlpha = 0.12f,
                borderColor = badgeColor.copy(alpha = 0.30f),
                neu = neu
              )
            ) {
              Text(
                text = "Use ${formatCreditCount(rec.leaveCreditsUsed)} Cr",
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = pillTextAccent,
                maxLines = 1,
                softWrap = false
              )
            }
            if (rec.freeDaysUnlocked > 0) {
              Box(
                modifier = Modifier.neumorphicInset(
                  shape = IosSharpBadgeShape,
                  depth = 1.5.dp,
                  blurRadius = 3.dp,
                  accentTint = MaterialTheme.colorScheme.tertiary,
                  accentTintAlpha = 0.12f,
                  borderColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.28f),
                  neu = neu
                )
              ) {
                Text(
                  text = "+${rec.freeDaysUnlocked} Free",
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.tertiary,
                  maxLines = 1,
                  softWrap = false
                )
              }
            }
          }
        }
      }

      // Intelligence Badges Row (Portfolio toggle + Expiry Urgency + Rule-8 EL + 0.5 CL + OPD + Hold)
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        // Portfolio Select Toggle Chip
        Box(
          modifier = Modifier
            .then(
              if (isSelectedForPortfolio) {
                Modifier.neumorphicExtruded(
                  shape = IosSharpBadgeShape,
                  elevation = 2.dp,
                  blurRadius = 4.dp,
                  surfaceColor = LocalFunctionalActionColors.current.verifyApprove,
                  neu = neu
                )
              } else {
                Modifier.neumorphicInset(
                  shape = IosSharpBadgeShape,
                  depth = 1.5.dp,
                  blurRadius = 3.dp,
                  accentTint = neu.primaryAccent,
                  accentTintAlpha = 0.08f,
                  borderColor = neu.primaryAccent.copy(alpha = 0.30f),
                  neu = neu
                )
              }
            )
            .clickable { onTogglePortfolioSelection() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("portfolio_select_chip_$rank")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Layers,
              contentDescription = null,
              tint = if (isSelectedForPortfolio) Color.White else neu.primaryAccent,
              modifier = Modifier.size(12.dp)
            )
            Text(
              text = if (isSelectedForPortfolio) "✓ In Portfolio" else "+ Portfolio",
              fontSize = 10.sp,
              fontWeight = FontWeight.ExtraBold,
              color = if (isSelectedForPortfolio) Color.White else neu.primaryAccent,
              maxLines = 1,
              softWrap = false
            )
          }
        }

        if (isHeldOnCalendar) {
          IntelligenceStatusTag(label = "HELD ON CALENDAR", accent = HoldIndigoBadge)
        }
        if (rec.expiryUrgencyTag.isNotBlank()) {
          IntelligenceStatusTag(label = rec.expiryUrgencyTag, accent = StatusAlertRed)
        }
        if (rec.isElBridge) {
          IntelligenceStatusTag(label = "RULE-8 EL PREFIX/SUFFIX", accent = ElVioletBadge)
        }
        if (rec.isHalfDayBridge) {
          IntelligenceStatusTag(label = "0.5 CL HALF-DAY", accent = EmeraldOptimal)
        }
        if (rec.usesVirtualCol) {
          IntelligenceStatusTag(label = "VIRTUAL +1 COL", accent = BadgeColCyan)
        }
        if (rec.pinnedDatesUsed.isNotEmpty()) {
          IntelligenceStatusTag(label = "${rec.pinnedDatesUsed.size} PINNED", accent = HoldIndigoBadge)
        }
        if (rec.hasOpdConflict) {
          IntelligenceStatusTag(label = "OPD OVERLAP", accent = StatusPendingOrange)
        } else if (rec.opdImpactNote.isNotBlank()) {
          IntelligenceStatusTag(label = "OPD-SAFE", accent = EmeraldOptimal)
        }
      }

      // Visual Color-Coded Timeline Strip (tap any day pill to Pin/Block/Mark Work Duty)
      BridgeVisualTimelineStrip(
        dailyPlan = rec.dailyPlan,
        onDayClick = { dayPlan -> onSteerDate(dayPlan.dateIso) }
      )

      // Always-Visible Callout: EXACT DATES TO APPLY FOR LEAVE
      if (rec.exactDatesToApply.isNotEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .neumorphicInset(
              shape = IosSharpWellShape,
              depth = 2.dp,
              blurRadius = 4.dp,
              accentTint = badgeColor,
              accentTintAlpha = 0.08f,
              borderColor = badgeColor.copy(alpha = 0.30f),
              neu = neu
            )
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
                tint = pillTextAccent,
                modifier = Modifier.size(14.dp)
              )
              Text(
                text = "EXACT DATES TO APPLY (${rec.exactDatesToApply.size} ${if (rec.exactDatesToApply.size == 1) "DATE" else "DATES"}):",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = pillTextAccent
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
                Box(
                  modifier = Modifier.neumorphicExtruded(
                    shape = IosSharpBadgeShape,
                    elevation = 1.5.dp,
                    blurRadius = 3.dp,
                    accentTint = badgeColor,
                    accentTintAlpha = 0.04f,
                    borderAlpha = 0.35f,
                    neu = neu
                  )
                ) {
                  Text(
                    text = displayChip,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontFamily = JetBrainsMonoFontFamily
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    color = neu.textPrimary
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
        color = neu.textSecondary,
        maxLines = if (expanded) 6 else 2,
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
          HorizontalDivider(color = neu.hairlineDivider)

          // Day-by-Day Chronological Schedule Pills
          Text(
            text = "FULL SCHEDULE BREAKDOWN (${rec.totalDaysOff} CONSECUTIVE DAYS OFF)",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = neu.primaryAccent
          )

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            rec.dailyPlan.forEach { dayStep ->
              val isLeaveAction = dayStep.creditType.uppercase(Locale.US) in setOf("CL", "HALF_CL", "RH", "COL", "EL")
              Box(
                modifier = if (isLeaveAction) {
                  Modifier.neumorphicInset(
                    shape = IosSharpBadgeShape,
                    depth = 1.5.dp,
                    blurRadius = 3.dp,
                    accentTint = badgeColor,
                    accentTintAlpha = 0.12f,
                    borderColor = badgeColor.copy(alpha = 0.42f),
                    neu = neu
                  )
                } else {
                  Modifier.neumorphicExtruded(
                    shape = IosSharpBadgeShape,
                    elevation = 1.5.dp,
                    blurRadius = 3.dp,
                    borderAlpha = 0.20f,
                    neu = neu
                  )
                }
              ) {
                Text(
                  text = "${dayStep.dayLabel}: ${dayStep.description}",
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = JetBrainsMonoFontFamily
                  ),
                  fontWeight = if (isLeaveAction) FontWeight.Bold else FontWeight.Medium,
                  color = if (isLeaveAction) pillTextAccent else neu.textPrimary
                )
              }
            }
          }

          // Compliance, OPD, LTC Synergy & Sequential Balance Footer
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .neumorphicInset(
                shape = IosSharpWellShape,
                depth = 2.dp,
                blurRadius = 4.dp,
                neu = neu
              )
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
                  color = neu.textSecondary
                )
              }
              if (rec.opdImpactNote.isNotBlank()) {
                Text(
                  text = "Clinical OPD Check: ${rec.opdImpactNote}",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.SemiBold,
                  color = if (rec.hasOpdConflict) StatusPendingOrange else EmeraldOptimal
                )
              }
              if (rec.ltcOdSynergyNote.isNotBlank()) {
                Text(
                  text = "LTC / Conference Synergy: ${rec.ltcOdSynergyNote}",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.SemiBold,
                  color = ElVioletBadge
                )
              }
              if (rec.projectedBalanceAfter.isNotBlank()) {
                Text(
                  text = "After This Plan: ${rec.projectedBalanceAfter}",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = JetBrainsMonoFontFamily
                  ),
                  fontWeight = FontWeight.Bold,
                  color = neu.primaryAccent
                )
              }
              if (rec.portfolioBalanceAfter.isNotBlank()) {
                Text(
                  text = "Cumulative Sequence: ${rec.portfolioBalanceAfter}",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = JetBrainsMonoFontFamily
                  ),
                  fontWeight = FontWeight.Medium,
                  color = neu.textSecondary
                )
              }
            }
          }

          // Secondary Tactical Actions Row: Hold on Calendar | Simulate in Builder | Sanction Letter
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            TacticalCardActionChip(
              icon = if (isHeldOnCalendar) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
              label = if (isHeldOnCalendar) "Held ✓" else "Hold",
              accent = HoldIndigoBadge,
              isActive = isHeldOnCalendar,
              onClick = onToggleTentativeHold,
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .testTag("hold_ai_rec_btn_$rank")
            )
            val actionsCard = LocalFunctionalActionColors.current
            TacticalCardActionChip(
              icon = Icons.Default.Tune,
              label = "Simulate",
              accent = actionsCard.aiOptimize,
              isActive = false,
              onClick = onSimulateInBuilder,
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .testTag("simulate_ai_rec_btn_$rank")
            )
            TacticalCardActionChip(
              icon = Icons.Default.Description,
              label = "HOD Letter",
              accent = actionsCard.exportDocs,
              isActive = false,
              onClick = onOpenSanctionLetter,
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .testTag("letter_ai_rec_btn_$rank")
            )
          }

          // Primary Apply Action Row + Quick Copy
          val actionsRow = LocalFunctionalActionColors.current
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
                containerColor = actionsRow.createPrimary,
                contentColor = Color.White
              ),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Apply Exact Leave Dates to Ledger",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
              )
            }

            Box(
              modifier = Modifier
                .size(40.dp)
                .neumorphicExtruded(
                  shape = IosSharpMicroShape,
                  elevation = 2.dp,
                  blurRadius = 4.dp,
                  borderAlpha = 0.22f,
                  neu = neu
                ),
              contentAlignment = Alignment.Center
            ) {
              IconButton(
                onClick = onCopyMemo,
                modifier = Modifier.testTag("copy_ai_rec_btn_$rank")
              ) {
                Icon(
                  imageVector = Icons.Default.ContentCopy,
                  contentDescription = "Copy Optimal Leave Plan",
                  tint = actionsRow.shareHandover,
                  modifier = Modifier.size(18.dp)
                )
              }
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
          text = if (expanded) "Hide breakdown & actions" else "Inspect schedule, Hold, Simulate or HOD Letter",
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
private fun IntelligenceStatusTag(
  label: String,
  accent: Color
) {
  val neu = LocalNeumorphicColors.current
  Box(
    modifier = Modifier.neumorphicInset(
      shape = IosSharpBadgeShape,
      depth = 1.5.dp,
      blurRadius = 3.dp,
      accentTint = accent,
      accentTintAlpha = 0.14f,
      borderColor = accent.copy(alpha = 0.42f),
      neu = neu
    )
  ) {
    Text(
      text = label,
      modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.5.dp),
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
private fun TacticalCardActionChip(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  accent: Color,
  isActive: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val neu = LocalNeumorphicColors.current
  Box(
    modifier = modifier
      .then(
        if (isActive) {
          Modifier.neumorphicInset(
            shape = IosSharpButtonShape,
            depth = 2.dp,
            blurRadius = 4.dp,
            accentTint = accent,
            accentTintAlpha = 0.16f,
            borderColor = accent.copy(alpha = 0.55f),
            neu = neu
          )
        } else {
          Modifier.neumorphicExtruded(
            shape = IosSharpButtonShape,
            elevation = 2.dp,
            blurRadius = 4.dp,
            accentTint = accent,
            accentTintAlpha = 0.05f,
            borderAlpha = 0.30f,
            neu = neu
          )
        }
      )
      .clickable { onClick() }
      .padding(horizontal = 8.dp, vertical = 9.dp),
    contentAlignment = Alignment.Center
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = accent,
        modifier = Modifier.size(14.dp)
      )
      Text(
        text = label,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        color = accent,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
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
  val neu = LocalNeumorphicColors.current
  val safeAccent = if (accent == BadgeRhSlate && neu.isDark) Color(0xFF94A3B8) else accent
  Box(
    modifier = modifier.neumorphicInset(
      shape = IosSharpButtonShape,
      depth = 2.dp,
      blurRadius = 4.dp,
      accentTint = accent,
      accentTintAlpha = 0.08f,
      borderColor = accent.copy(alpha = 0.28f),
      neu = neu
    )
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
        color = neu.textSecondary,
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
        color = safeAccent,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}
