package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PanToolAlt
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.InitialLeaveSeedData
import com.example.data.LeaveCategory
import com.example.domain.LeaveBuilderAnalyzer
import com.example.ui.theme.BadgeClGreen
import com.example.ui.theme.BadgeColCyan
import com.example.ui.theme.BadgeElNeutral
import com.example.ui.theme.BadgeMlOrange
import com.example.ui.theme.BadgePlGold
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.StatusAlertRed
import com.example.ui.theme.StatusVerifiedGreen
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

enum class BuilderBlockBrush(
  val code: String,
  val shortTitle: String,
  val ruleSubtitle: String,
  val category: LeaveCategory?,
  val color: Color
) {
  EL(
    code = "EL",
    shortTitle = "EL Block",
    ruleSubtitle = "Earned Leave (Rule 8)",
    category = LeaveCategory.EL,
    color = Color(0xFF6366F1)
  ),
  CL(
    code = "CL",
    shortTitle = "CL Block",
    ruleSubtitle = "Casual Leave (≤12/yr)",
    category = LeaveCategory.CL,
    color = BadgeClGreen
  ),
  RH(
    code = "RH",
    shortTitle = "RH Block",
    ruleSubtitle = "Restricted RL (≤3/yr)",
    category = LeaveCategory.RH,
    color = BadgePlGold
  ),
  COL(
    code = "COL",
    shortTitle = "COL Block",
    ruleSubtitle = "Against GH Worked",
    category = LeaveCategory.COL,
    color = BadgeColCyan
  ),
  ML(
    code = "ML",
    shortTitle = "ML Block",
    ruleSubtitle = "Medical UEL (Rule 15)",
    category = LeaveCategory.ML,
    color = BadgeMlOrange
  ),
  ERASE(
    code = "ERASE",
    shortTitle = "Eraser",
    ruleSubtitle = "Clear Cell",
    category = null,
    color = Color(0xFF64748B)
  )
}

@Composable
fun InteractiveLeaveBuilderSection(
  state: LeaveUiState.Success,
  onCommitDraftLeaves: (Map<String, LeaveCategory>) -> Unit,
  modifier: Modifier = Modifier
) {
  var builderMonth by rememberSaveable { mutableIntStateOf(11) } // Default Nov 2026
  val currentYearMonth = remember(builderMonth) {
    YearMonth.of(2026, builderMonth.coerceIn(1, 12))
  }

  // User's interactive draft blocks (dateIso -> LeaveCategory)
  val draftBlocks = remember {
    mutableStateMapOf<String, LeaveCategory>().apply {
      // Pre-populate with an optimal Mon–Fri Earned Leave (EL) demonstration in Nov 2026
      // so the user immediately sees real-time EL balance & free weekend prefix/suffix feedback!
      put("2026-11-16", LeaveCategory.EL)
      put("2026-11-17", LeaveCategory.EL)
      put("2026-11-18", LeaveCategory.EL)
      put("2026-11-19", LeaveCategory.EL)
      put("2026-11-20", LeaveCategory.EL)
    }
  }

  var activeBrush by remember { mutableStateOf(BuilderBlockBrush.EL) }

  // Drag-and-drop coordinate tracking
  var containerOriginInRoot by remember { mutableStateOf(Offset.Zero) }
  var isDraggingBlock by remember { mutableStateOf(false) }
  var draggedBrush by remember { mutableStateOf<BuilderBlockBrush?>(null) }
  var dragPointerInRoot by remember { mutableStateOf(Offset.Zero) }
  var hoveredDateIso by remember { mutableStateOf<String?>(null) }
  val cellBoundsInRoot = remember { mutableStateMapOf<String, Rect>() }

  val ghByDate = remember {
    InitialLeaveSeedData.tnGovtPublicHolidays2026.associateBy { it.dateIso }
  }
  val rhByDate = remember {
    InitialLeaveSeedData.tnRestrictedHolidays2026.associateBy { it.dateIso }
  }
  val existingByDate = remember(state.allRecords) {
    state.allRecords.filter { it.year == 2026L }.associateBy { it.dateIso }
  }

  val analysis = remember(
    draftBlocks.toMap(),
    state.summary2026,
    state.colGhAuditAllYears.availableUnusedGhCount,
    state.allRecords
  ) {
    LeaveBuilderAnalyzer.analyzeDraft(
      draftBlocks = draftBlocks.toMap(),
      summary2026 = state.summary2026,
      unusedGhCreditsCount = state.colGhAuditAllYears.availableUnusedGhCount,
      allRecords = state.allRecords
    )
  }

  fun applyBrushToDate(dateIso: String, brush: BuilderBlockBrush) {
    if (brush.category == null) {
      draftBlocks.remove(dateIso)
    } else {
      draftBlocks[dateIso] = brush.category
    }
  }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .onGloballyPositioned { coords ->
        containerOriginInRoot = coords.positionInRoot()
      }
      .testTag("interactive_leave_builder_section")
  ) {
    Surface(
      shape = RoundedCornerShape(26.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 2.dp,
      border = BorderStroke(
        1.2.dp,
        if (analysis.isFullyCompliant) EmeraldOptimal.copy(alpha = 0.35f) else StatusAlertRed.copy(alpha = 0.5f)
      ),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // 1. Header Banner
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF6366F1).copy(alpha = 0.16f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.PanToolAlt,
                contentDescription = null,
                tint = Color(0xFF6366F1),
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "DRAG & DROP • TN GOVT RULE SIMULATOR",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF6366F1),
                fontSize = 10.sp,
                lineHeight = 13.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = "Interactive Leave Builder (EL, CL, RH, COL)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
              )
            }
          }

          Spacer(modifier = Modifier.width(8.dp))

          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (analysis.isFullyCompliant) {
              EmeraldOptimal.copy(alpha = 0.14f)
            } else {
              StatusAlertRed.copy(alpha = 0.14f)
            }
          ) {
            Text(
              text = if (analysis.isFullyCompliant) "TN RULE OK" else "RULE ALERT",
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              fontSize = 11.sp,
              lineHeight = 14.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              color = if (analysis.isFullyCompliant) EmeraldOptimal else StatusAlertRed,
              maxLines = 1,
              softWrap = false
            )
          }
        }

        // 2. Real-Time Earned Leave (EL) & TN Govt Compliance Live Telemetry Strip (4 Equal-Height Pill Boxes)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          BuilderTelemetryPill(
            label = "EL Balance",
            value = "${analysis.elProjectedRemainingBalance}d Left",
            sub = if (analysis.elTrappedInterveningHolidayDays > 0) {
              "-${analysis.elTotalDebitedDays}d (${analysis.elTrappedInterveningHolidayDays}d Trap!)"
            } else {
              "-${analysis.elTotalDebitedDays}d Debited"
            },
            accent = if (analysis.elTrappedInterveningHolidayDays > 0) BadgePlGold else Color(0xFF6366F1),
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
          )
          BuilderTelemetryPill(
            label = "Total Off",
            value = "${analysis.totalDaysOffIncludingAttachedHolidays} Days",
            sub = "+${analysis.elFreePrefixSuffixDays}d Free Wknd",
            accent = EmeraldOptimal,
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
          )
          BuilderTelemetryPill(
            label = "CL / RH Left",
            value = "${formatDoubleDays(analysis.clProjectedRemaining)}CL • ${analysis.rhProjectedRemaining}RH",
            sub = "2026 Quota",
            accent = if (analysis.clProjectedRemaining < 0 || analysis.rhProjectedRemaining < 0) {
              StatusAlertRed
            } else {
              BadgeClGreen
            },
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
          )
          BuilderTelemetryPill(
            label = "Max Stretch",
            value = "${analysis.maxContiguousStretchDays}d Spell",
            sub = if (analysis.isFullyCompliant) "≤10d / EL OK" else "Rule Violation",
            accent = if (analysis.isFullyCompliant) StatusVerifiedGreen else StatusAlertRed,
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
          )
        }

        // 3. Draggable Leave Block Palette
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "DRAG A LEAVE BLOCK ONTO CALENDAR (OR TAP TO SELECT BRUSH):",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontSize = 10.sp,
              lineHeight = 13.sp,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f)
            )
            if (draftBlocks.isNotEmpty()) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .clickable { draftBlocks.clear() }
                  .testTag("clear_leave_builder_button")
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(13.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "Clear (${draftBlocks.size})",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState())
              .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            BuilderBlockBrush.entries.forEach { brush ->
              val isSelected = activeBrush == brush
              var chipCenterInRoot by remember { mutableStateOf(Offset.Zero) }

              Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) brush.color else brush.color.copy(alpha = 0.12f),
                border = BorderStroke(
                  width = if (isSelected) 2.dp else 1.dp,
                  color = brush.color
                ),
                modifier = Modifier
                  .width(108.dp)
                  .fillMaxHeight()
                  .onGloballyPositioned { coords ->
                    val pos = coords.positionInRoot()
                    chipCenterInRoot = Offset(
                      pos.x + coords.size.width / 2f,
                      pos.y + coords.size.height / 2f
                    )
                  }
                  .clip(RoundedCornerShape(14.dp))
                  .clickable { activeBrush = brush }
                  .pointerInput(brush) {
                    detectDragGestures(
                      onDragStart = { offsetInChip ->
                        activeBrush = brush
                        isDraggingBlock = true
                        draggedBrush = brush
                        dragPointerInRoot = chipCenterInRoot + offsetInChip - Offset(50f, 25f)
                        hoveredDateIso = cellBoundsInRoot.entries
                          .firstOrNull { it.value.contains(dragPointerInRoot) }
                          ?.key
                      },
                      onDrag = { change, dragAmount ->
                        change.consume()
                        dragPointerInRoot += dragAmount
                        hoveredDateIso = cellBoundsInRoot.entries
                          .firstOrNull { it.value.contains(dragPointerInRoot) }
                          ?.key
                      },
                      onDragEnd = {
                        val targetIso = hoveredDateIso
                        val currentDragged = draggedBrush
                        if (targetIso != null && currentDragged != null) {
                          applyBrushToDate(targetIso, currentDragged)
                        }
                        isDraggingBlock = false
                        draggedBrush = null
                        hoveredDateIso = null
                      },
                      onDragCancel = {
                        isDraggingBlock = false
                        draggedBrush = null
                        hoveredDateIso = null
                      }
                    )
                  }
                  .testTag("builder_brush_${brush.code}")
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                  verticalArrangement = Arrangement.SpaceBetween
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = brush.shortTitle,
                      fontWeight = FontWeight.Bold,
                      fontSize = 12.sp,
                      lineHeight = 15.sp,
                      color = if (isSelected) Color.White else brush.color,
                      maxLines = 1,
                      softWrap = false,
                      overflow = TextOverflow.Ellipsis,
                      modifier = Modifier.weight(1f)
                    )
                    Icon(
                      imageVector = if (brush == BuilderBlockBrush.ERASE) {
                        Icons.Default.Backspace
                      } else {
                        Icons.Default.DragIndicator
                      },
                      contentDescription = null,
                      tint = if (isSelected) Color.White else brush.color,
                      modifier = Modifier.size(14.dp)
                    )
                  }
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = brush.ruleSubtitle,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    color = if (isSelected) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
            }
          }
        }

        // 4. Quick Simulation Scenario Presets (1-Tap Load)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          PresetScenarioChip(
            label = "Preset 1: Optimal 5d EL (+4d Free Wknd)",
            onClick = {
              builderMonth = 11
              draftBlocks.clear()
              listOf("2026-11-16", "2026-11-17", "2026-11-18", "2026-11-19", "2026-11-20").forEach {
                draftBlocks[it] = LeaveCategory.EL
              }
            }
          )
          PresetScenarioChip(
            label = "Preset 2: EL Weekend Trap Demo (Rule 8)",
            onClick = {
              builderMonth = 11
              draftBlocks.clear()
              // Fri Nov 13 + Mon Nov 16 brackets Sat Nov 14 & Sun Nov 15 -> triggers Rule 8 Auto-Debit Warning!
              draftBlocks["2026-11-13"] = LeaveCategory.EL
              draftBlocks["2026-11-16"] = LeaveCategory.EL
            }
          )
          PresetScenarioChip(
            label = "Preset 3: Dec 9d RH+COL+CL Bridge",
            onClick = {
              builderMonth = 12
              draftBlocks.clear()
              draftBlocks["2026-12-24"] = LeaveCategory.RH
              draftBlocks["2026-12-28"] = LeaveCategory.COL
              draftBlocks["2026-12-29"] = LeaveCategory.CL
              draftBlocks["2026-12-30"] = LeaveCategory.CL
              draftBlocks["2026-12-31"] = LeaveCategory.RH
            }
          )
        }

        // 5. Month Navigator & Drop-Target Calendar Grid
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              IconButton(
                onClick = { if (builderMonth > 1) builderMonth-- else builderMonth = 12 },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                  contentDescription = "Previous Month"
                )
              }

              Text(
                text = "${
                  currentYearMonth.month.getDisplayName(
                    TextStyle.FULL,
                    Locale.ENGLISH
                  )
                } 2026 • Drop Leave Blocks Below",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
              )

              IconButton(
                onClick = { if (builderMonth < 12) builderMonth++ else builderMonth = 1 },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                  contentDescription = "Next Month"
                )
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Weekday Header Row
            Row(modifier = Modifier.fillMaxWidth()) {
              listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEachIndexed { idx, dName ->
                Text(
                  text = dName,
                  modifier = Modifier.weight(1f),
                  textAlign = TextAlign.Center,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (idx == 0 || idx == 6) BadgePlGold else MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Calendar Grid Cells (with drop-zone bounds registration & drag-paint gesture)
            val firstDayOfMonth = currentYearMonth.atDay(1)
            val startOffset = firstDayOfMonth.dayOfWeek.value % 7
            val daysInMonth = currentYearMonth.lengthOfMonth()
            val totalCells = ((startOffset + daysInMonth + 6) / 7) * 7
            val rows = totalCells / 7

            Column(
              verticalArrangement = Arrangement.spacedBy(5.dp),
              modifier = Modifier
                .fillMaxWidth()
                .pointerInput(activeBrush, currentYearMonth) {
                  detectDragGestures(
                    onDragStart = { localOffset ->
                      val rootPoint = containerOriginInRoot + localOffset
                      // Also check directly against cellBoundsInRoot using root coordinates
                    },
                    onDrag = { change, _ ->
                      change.consume()
                    }
                  )
                }
            ) {
              for (r in 0 until rows) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                  for (c in 0 until 7) {
                    val cellIdx = r * 7 + c
                    val dayNum = cellIdx - startOffset + 1
                    if (dayNum in 1..daysInMonth) {
                      val dateObj = currentYearMonth.atDay(dayNum)
                      val dateIso = dateObj.toString()
                      val isWeekend =
                        dateObj.dayOfWeek == DayOfWeek.SATURDAY || dateObj.dayOfWeek == DayOfWeek.SUNDAY
                      val ghItem = ghByDate[dateIso]
                      val rhItem = rhByDate[dateIso]
                      val draftedCat = draftBlocks[dateIso]
                      val existingRec = existingByDate[dateIso]
                      val isTrappedEl = dateIso in analysis.trappedElHolidayDates
                      val isFreeAttached = dateIso in analysis.freeAttachedHolidayDates
                      val isHovered = hoveredDateIso == dateIso

                      BuilderCalendarDayCell(
                        dayNum = dayNum,
                        dateIso = dateIso,
                        isWeekend = isWeekend,
                        ghName = ghItem?.nameEnglish,
                        rhName = rhItem?.nameEnglish,
                        draftedCat = draftedCat,
                        existingCat = existingRec?.category,
                        isTrappedEl = isTrappedEl,
                        isFreeAttached = isFreeAttached,
                        isHoveredDropTarget = isHovered,
                        onRegisterBounds = { rect ->
                          cellBoundsInRoot[dateIso] = rect
                        },
                        onClickCell = {
                          if (draftBlocks[dateIso] == activeBrush.category) {
                            draftBlocks.remove(dateIso)
                          } else {
                            applyBrushToDate(dateIso, activeBrush)
                          }
                        },
                        modifier = Modifier.weight(1f)
                      )
                    } else {
                      Box(
                        modifier = Modifier
                          .weight(1f)
                          .aspectRatio(0.95f)
                      )
                    }
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Legend for Calendar Annotations
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              LegendDotItem(color = Color(0xFF6366F1), label = "Draft EL")
              LegendDotItem(color = StatusAlertRed, label = "EL Rule 8 Trapped Wknd (Debited!)")
              LegendDotItem(color = EmeraldOptimal, label = "Free Prefix/Suffix Wknd/GH (0 Debit)")
              LegendDotItem(color = BadgePlGold, label = "TN GH / RH")
            }
          }
        }

        // 6. Real-Time TN Govt Compliance & Earned Leave Optimization Feedback Box
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = when {
            !analysis.isFullyCompliant -> StatusAlertRed.copy(alpha = 0.1f)
            analysis.elTrappedInterveningHolidayDays > 0 -> BadgePlGold.copy(alpha = 0.12f)
            else -> EmeraldOptimal.copy(alpha = 0.1f)
          },
          border = BorderStroke(
            1.dp,
            when {
              !analysis.isFullyCompliant -> StatusAlertRed.copy(alpha = 0.4f)
              analysis.elTrappedInterveningHolidayDays > 0 -> BadgePlGold.copy(alpha = 0.4f)
              else -> EmeraldOptimal.copy(alpha = 0.35f)
            }
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = when {
                  !analysis.isFullyCompliant -> Icons.Default.ErrorOutline
                  analysis.elTrappedInterveningHolidayDays > 0 -> Icons.Default.WarningAmber
                  else -> Icons.Default.Verified
                },
                contentDescription = null,
                tint = when {
                  !analysis.isFullyCompliant -> StatusAlertRed
                  analysis.elTrappedInterveningHolidayDays > 0 -> BadgePlGold
                  else -> EmeraldOptimal
                },
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = analysis.statutorySummaryVerdict,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                  !analysis.isFullyCompliant -> StatusAlertRed
                  analysis.elTrappedInterveningHolidayDays > 0 -> BadgePlGold
                  else -> EmeraldOptimal
                }
              )
            }

            analysis.complianceViolations.forEach { violation ->
              Text(
                text = "• $violation",
                style = MaterialTheme.typography.bodyMedium,
                color = StatusAlertRed,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium
              )
            }

            analysis.smartOptimizationWarnings.forEach { warning ->
              Text(
                text = "• $warning",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                lineHeight = 16.sp
              )
            }
          }
        }

        // 7. Commit Draft Leave Blocks to 2026 Firestore Ledger Button
        Button(
          onClick = {
            if (draftBlocks.isNotEmpty()) {
              onCommitDraftLeaves(draftBlocks.toMap())
            }
          },
          enabled = draftBlocks.isNotEmpty() && analysis.isFullyCompliant,
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = EmeraldOptimal,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("commit_leave_builder_button")
        ) {
          Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (!analysis.isFullyCompliant) {
              "Resolve TN Rule Violations Before Saving"
            } else {
              "Commit ${draftBlocks.size} Drafted Leave Block(s) to 2026 Ledger"
            },
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }

    // Floating Ghost Pill while dragging a Leave Block across the screen
    if (isDraggingBlock && draggedBrush != null) {
      val brush = draggedBrush!!
      val localOffset = dragPointerInRoot - containerOriginInRoot
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = brush.color,
        shadowElevation = 10.dp,
        border = BorderStroke(2.dp, Color.White),
        modifier = Modifier
          .offset {
            IntOffset(
              x = localOffset.x.roundToInt(),
              y = localOffset.y.roundToInt()
            )
          }
          .zIndex(20f)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = if (hoveredDateIso != null) {
              "Drop ${brush.code} on ${hoveredDateIso!!.takeLast(5)}"
            } else {
              "Dragging ${brush.shortTitle}"
            },
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
          )
        }
      }
    }
  }
}

@Composable
private fun BuilderCalendarDayCell(
  dayNum: Int,
  dateIso: String,
  isWeekend: Boolean,
  ghName: String?,
  rhName: String?,
  draftedCat: LeaveCategory?,
  existingCat: LeaveCategory?,
  isTrappedEl: Boolean,
  isFreeAttached: Boolean,
  isHoveredDropTarget: Boolean,
  onRegisterBounds: (Rect) -> Unit,
  onClickCell: () -> Unit,
  modifier: Modifier = Modifier
) {
  val bgColor = when {
    isHoveredDropTarget -> EmeraldOptimal.copy(alpha = 0.35f)
    draftedCat != null -> categoryBuilderColor(draftedCat)
    isTrappedEl -> StatusAlertRed.copy(alpha = 0.22f)
    isFreeAttached -> EmeraldOptimal.copy(alpha = 0.18f)
    ghName != null -> BadgePlGold.copy(alpha = 0.18f)
    rhName != null -> Color(0xFF0D9488).copy(alpha = 0.15f)
    isWeekend -> MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
    else -> MaterialTheme.colorScheme.surface
  }

  val borderColor = when {
    isHoveredDropTarget -> EmeraldOptimal
    draftedCat != null -> categoryBuilderColor(draftedCat)
    isTrappedEl -> StatusAlertRed
    isFreeAttached -> EmeraldOptimal
    ghName != null -> BadgePlGold
    rhName != null -> Color(0xFF0D9488)
    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
  }

  val badgeLabel = when {
    draftedCat != null -> draftedCat.shortLabel
    isTrappedEl -> "EL TRAP"
    isFreeAttached -> "FREE"
    ghName != null -> "GH"
    rhName != null -> "RH"
    existingCat != null -> existingCat.shortLabel
    isWeekend -> "WKND"
    else -> ""
  }

  Surface(
    shape = RoundedCornerShape(10.dp),
    color = bgColor,
    border = BorderStroke(if (isHoveredDropTarget || draftedCat != null || isTrappedEl) 1.5.dp else 0.8.dp, borderColor),
    modifier = modifier
      .aspectRatio(0.95f)
      .onGloballyPositioned { coords ->
        onRegisterBounds(coords.boundsInRoot())
      }
      .clip(RoundedCornerShape(10.dp))
      .clickable { onClickCell() }
      .testTag("builder_day_$dateIso")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(3.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = dayNum.toString(),
        fontSize = 11.sp,
        lineHeight = 13.sp,
        fontWeight = FontWeight.Bold,
        color = if (draftedCat != null) {
          Color.White
        } else if (isTrappedEl) {
          StatusAlertRed
        } else {
          MaterialTheme.colorScheme.onSurface
        }
      )

      if (badgeLabel.isNotEmpty()) {
        Text(
          text = badgeLabel,
          fontSize = 8.sp,
          lineHeight = 10.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          color = when {
            draftedCat != null -> Color.White
            isTrappedEl -> StatusAlertRed
            isFreeAttached -> EmeraldOptimal
            ghName != null -> BadgePlGold
            rhName != null -> Color(0xFF0D9488)
            else -> MaterialTheme.colorScheme.onSurfaceVariant
          },
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}

@Composable
private fun BuilderTelemetryPill(
  label: String,
  value: String,
  sub: String,
  accent: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = accent.copy(alpha = 0.1f),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.3f)),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 8.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = label,
        fontSize = 9.sp,
        lineHeight = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = value,
        fontSize = 12.sp,
        lineHeight = 15.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        color = accent,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = sub,
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

@Composable
private fun PresetScenarioChip(
  label: String,
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = MaterialTheme.colorScheme.surfaceVariant,
    modifier = Modifier
      .height(30.dp)
      .clip(RoundedCornerShape(10.dp))
      .clickable { onClick() }
  ) {
    Box(
      modifier = Modifier.padding(horizontal = 10.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = label,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        softWrap = false
      )
    }
  }
}

@Composable
private fun LegendDotItem(
  color: Color,
  label: String
) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
      modifier = Modifier
        .size(8.dp)
        .clip(CircleShape)
        .background(color)
    )
    Spacer(modifier = Modifier.width(4.dp))
    Text(
      text = label,
      fontSize = 10.sp,
      lineHeight = 12.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
      softWrap = false
    )
  }
}

private fun categoryBuilderColor(cat: LeaveCategory): Color = when (cat) {
  LeaveCategory.EL -> Color(0xFF6366F1)
  LeaveCategory.CL -> BadgeClGreen
  LeaveCategory.RH -> BadgePlGold
  LeaveCategory.COL -> BadgeColCyan
  LeaveCategory.ML -> BadgeMlOrange
  else -> EmeraldOptimal
}
