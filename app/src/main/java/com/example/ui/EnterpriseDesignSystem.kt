package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BadgeColCyan
import com.example.ui.theme.BadgePlGold
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Enterprise-grade dark obsidian/sapphire hero container with:
 * - Multi-stop deep clinical sapphire & emerald gradient background
 * - Custom Canvas architectural micro-grid & glowing radial light orbs
 * - Specular 1.dp gradient border along the perimeter
 */
@Composable
fun EnterpriseMeshHeroContainer(
  modifier: Modifier = Modifier,
  cornerRadius: Dp = 28.dp,
  primaryGlowColor: Color = Color(0xFF10B981),
  secondaryGlowColor: Color = Color(0xFF0EA5E9),
  contentPadding: PaddingValues = PaddingValues(22.dp),
  content: @Composable BoxScope.() -> Unit
) {
  val shape = RoundedCornerShape(cornerRadius)
  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(shape)
      .background(
        Brush.linearGradient(
          colors = listOf(
            Color(0xFF061326),
            Color(0xFF0B2545),
            Color(0xFF093542),
            Color(0xFF052E2B)
          ),
          start = Offset(0f, 0f),
          end = Offset(1000f, 900f)
        )
      )
      .drawBehind {
        // 1. Subtle architectural grid lines
        val step = 36.dp.toPx()
        val gridColor = Color.White.copy(alpha = 0.035f)
        var x = step
        while (x < size.width) {
          drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1f
          )
          x += step
        }
        var y = step
        while (y < size.height) {
          drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
          )
          y += step
        }

        // 2. Top-right ambient emerald radial orb
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(
              primaryGlowColor.copy(alpha = 0.26f),
              Color.Transparent
            ),
            center = Offset(size.width * 0.88f, size.height * 0.18f),
            radius = size.width * 0.55f
          ),
          center = Offset(size.width * 0.88f, size.height * 0.18f),
          radius = size.width * 0.55f
        )

        // 3. Bottom-left ambient sapphire/cyan radial orb
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(
              secondaryGlowColor.copy(alpha = 0.20f),
              Color.Transparent
            ),
            center = Offset(size.width * 0.12f, size.height * 0.88f),
            radius = size.width * 0.50f
          ),
          center = Offset(size.width * 0.12f, size.height * 0.88f),
          radius = size.width * 0.50f
        )
      }
      .border(
        width = 1.2.dp,
        brush = Brush.linearGradient(
          colors = listOf(
            Color.White.copy(alpha = 0.30f),
            primaryGlowColor.copy(alpha = 0.45f),
            secondaryGlowColor.copy(alpha = 0.25f)
          )
        ),
        shape = shape
      )
      .padding(contentPadding),
    content = content
  )
}

/**
 * Enterprise Section Header with numbered overline tag, main title, and optional right badge/action.
 */
@Composable
fun EnterpriseSectionHeader(
  indexTag: String,
  title: String,
  subtitle: String? = null,
  badgeText: String? = null,
  badgeColor: Color = EmeraldOptimal,
  onBadgeClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(6.dp)
              .clip(CircleShape)
              .background(badgeColor)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = indexTag.uppercase(),
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 10.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.Bold,
            color = badgeColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
          )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = title,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground,
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis
        )
      }

      if (badgeText != null) {
        Spacer(modifier = Modifier.width(10.dp))
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = badgeColor.copy(alpha = 0.12f),
          border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.30f)),
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .then(if (onBadgeClick != null) Modifier.clickable { onBadgeClick() } else Modifier)
        ) {
          Text(
            text = badgeText,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
            color = badgeColor,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
          )
        }
      }
    }

    if (!subtitle.isNullOrBlank()) {
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

/**
 * Enterprise 3-Pill Live Governance & Telemetry Strip for the HOD Executive Desk.
 */
@Composable
fun EnterpriseExecutiveStatusStrip(
  pairedColCount: Int,
  totalColCount: Int,
  unusedGhCount: Int,
  onClickAudit: () -> Unit,
  onClickOptimizer: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isFullyPaired = pairedColCount >= totalColCount
  Row(
    modifier = modifier
      .fillMaxWidth()
      .height(IntrinsicSize.Min),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    EnterpriseTelemetryMicroPill(
      icon = Icons.Default.VerifiedUser,
      label = "FR / TN RULES",
      value = "100% COMPLIANT",
      accent = EmeraldOptimal,
      onClick = onClickOptimizer,
      modifier = Modifier
        .weight(1f)
        .fillMaxHeight()
    )
    EnterpriseTelemetryMicroPill(
      icon = Icons.Default.CheckCircle,
      label = "COL ↔ GH AUDIT",
      value = "$pairedColCount/$totalColCount VERIFIED",
      accent = if (isFullyPaired) BadgeColCyan else BadgePlGold,
      onClick = onClickAudit,
      modifier = Modifier
        .weight(1f)
        .fillMaxHeight()
    )
    EnterpriseTelemetryMicroPill(
      icon = Icons.Default.CloudDone,
      label = "UNUSED GH CREDIT",
      value = "$unusedGhCount AVAILABLE",
      accent = BadgePlGold,
      onClick = onClickAudit,
      modifier = Modifier
        .weight(1f)
        .fillMaxHeight()
    )
  }
}

@Composable
private fun EnterpriseTelemetryMicroPill(
  icon: ImageVector,
  label: String,
  value: String,
  accent: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    shadowElevation = 2.dp,
    border = BorderStroke(1.dp, accent.copy(alpha = 0.25f)),
    modifier = modifier
      .clip(RoundedCornerShape(16.dp))
      .clickable { onClick() }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 9.dp),
      verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = accent,
          modifier = Modifier.size(12.dp)
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
      Text(
        text = value,
        fontFamily = JetBrainsMonoFontFamily,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Bold,
        color = accent,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

/**
 * Unified clinical & executive date formatter that turns raw database ISO strings ("2026-11-23")
 * into scannable human-readable dates ("23 Nov 2026", "MON", "14 Jan → 18 Jan 2026 (Wed–Sun)").
 */
object ClinicalDateFormatter {
  private val friendlyFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
  private val shortMonthDayFormatter = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH)

  fun formatFriendlyDate(dateIso: String): String {
    val clean = dateIso.trim()
    if (clean.length != 10) return dateIso
    return runCatching {
      LocalDate.parse(clean).format(friendlyFormatter)
    }.getOrDefault(dateIso)
  }

  fun formatFriendlyWithDay(dateIso: String): String {
    val clean = dateIso.trim()
    if (clean.length != 10) return dateIso
    return runCatching {
      val ld = LocalDate.parse(clean)
      val dow = ld.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
      "$dow, ${ld.format(friendlyFormatter)}"
    }.getOrDefault(dateIso)
  }

  fun dayOfWeekShort(dateIso: String): String? {
    val clean = dateIso.trim()
    if (clean.length != 10) return null
    return runCatching {
      LocalDate.parse(clean).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase(Locale.ENGLISH)
    }.getOrNull()
  }

  fun formatRangeFriendly(startIso: String, endIso: String): String {
    return runCatching {
      val s = LocalDate.parse(startIso.trim())
      val e = LocalDate.parse(endIso.trim())
      val sDow = s.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
      val eDow = e.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
      if (s == e) {
        "${s.format(friendlyFormatter)} ($sDow)"
      } else if (s.year == e.year) {
        "${s.format(shortMonthDayFormatter)} → ${e.format(friendlyFormatter)} ($sDow–$eDow)"
      } else {
        "${s.format(friendlyFormatter)} → ${e.format(friendlyFormatter)} ($sDow–$eDow)"
      }
    }.getOrElse { "$startIso → $endIso" }
  }
}

/**
 * Guaranteed Zero-Vertical-Wrap Card Header Row.
 * Ensures the left title/subtitle column always claims `Modifier.weight(1f)` and the right badge
 * always uses `maxLines = 1, softWrap = false` so no text can ever wrap vertically 1 character per line.
 */
@Composable
fun ExecutiveCardHeaderRow(
  icon: ImageVector,
  iconAccent: Color,
  title: String,
  subtitle: String? = null,
  badgeText: String? = null,
  badgeAccent: Color = iconAccent,
  onBadgeClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(
      modifier = Modifier.weight(1f),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Surface(
        shape = RoundedCornerShape(13.dp),
        color = iconAccent.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, iconAccent.copy(alpha = 0.40f)),
        modifier = Modifier.size(40.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconAccent,
            modifier = Modifier.size(20.dp)
          )
        }
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis
        )
        if (!subtitle.isNullOrBlank()) {
          Text(
            text = subtitle,
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
    }

    if (!badgeText.isNullOrBlank()) {
      Spacer(modifier = Modifier.width(8.dp))
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = badgeAccent.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, badgeAccent.copy(alpha = 0.42f)),
        modifier = Modifier
          .clip(RoundedCornerShape(10.dp))
          .then(if (onBadgeClick != null) Modifier.clickable { onBadgeClick() } else Modifier)
      ) {
        Text(
          text = badgeText,
          fontFamily = JetBrainsMonoFontFamily,
          fontSize = 10.sp,
          lineHeight = 13.sp,
          fontWeight = FontWeight.ExtraBold,
          color = badgeAccent,
          maxLines = 1,
          softWrap = false,
          modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
        )
      }
    }
  }
}

/**
 * 1-Tap Native Material 3 Calendar DatePicker + Direct ISO Text Field.
 * Lets the user either tap the trailing Calendar icon to pick a date from a visual month calendar
 * OR type/edit YYYY-MM-DD directly, while displaying a live human-readable subtitle ("Mon, 23 Nov 2026").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerOutlinedField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  modifier: Modifier = Modifier,
  showFriendlySupportingText: Boolean = true
) {
  var showCalendarDialog by remember { mutableStateOf(false) }
  val friendlyHint = remember(value) {
    val clean = value.trim()
    if (clean.length == 10) ClinicalDateFormatter.formatFriendlyWithDay(clean) else null
  }

  OutlinedTextField(
    value = value,
    onValueChange = { onValueChange(it.trim()) },
    label = {
      Text(
        text = label,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
    },
    supportingText = if (showFriendlySupportingText && friendlyHint != null) {
      {
        Text(
          text = friendlyHint,
          fontFamily = JetBrainsMonoFontFamily,
          fontSize = 10.sp,
          color = EmeraldOptimal,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis
        )
      }
    } else null,
    trailingIcon = {
      IconButton(
        onClick = { showCalendarDialog = true }
      ) {
        Icon(
          imageVector = Icons.Default.CalendarMonth,
          contentDescription = "Open Calendar Date Picker for $label",
          tint = BadgeColCyan,
          modifier = Modifier.size(20.dp)
        )
      }
    },
    singleLine = true,
    shape = RoundedCornerShape(14.dp),
    modifier = modifier
  )

  if (showCalendarDialog) {
    val initialMillis = remember(value) {
      runCatching {
        LocalDate.parse(value.trim())
          .atStartOfDay(ZoneOffset.UTC)
          .toInstant()
          .toEpochMilli()
      }.getOrElse {
        LocalDate.of(2026, 11, 23)
          .atStartOfDay(ZoneOffset.UTC)
          .toInstant()
          .toEpochMilli()
      }
    }
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
      onDismissRequest = { showCalendarDialog = false },
      confirmButton = {
        TextButton(
          onClick = {
            pickerState.selectedDateMillis?.let { millis ->
              val pickedDate = Instant.ofEpochMilli(millis)
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
              onValueChange(pickedDate.toString())
            }
            showCalendarDialog = false
          }
        ) {
          Text("Select Date", fontWeight = FontWeight.Bold, color = EmeraldOptimal)
        }
      },
      dismissButton = {
        TextButton(onClick = { showCalendarDialog = false }) {
          Text("Cancel")
        }
      }
    ) {
      DatePicker(state = pickerState)
    }
  }
}

/**
 * Tier-3 Progressive Disclosure Accordion Card for secondary calculators, statutory rules, and tools.
 */
@Composable
fun ExecutiveCollapsibleAccordionCard(
  icon: ImageVector,
  accent: Color,
  title: String,
  subtitle: String,
  badgeText: String,
  initiallyExpanded: Boolean = false,
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit
) {
  var expanded by remember { mutableStateOf(initiallyExpanded) }
  Surface(
    shape = RoundedCornerShape(24.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = if (expanded) 2.dp else 1.dp,
    border = BorderStroke(
      width = 1.dp,
      color = if (expanded) accent.copy(alpha = 0.42f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    ),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { expanded = !expanded }
          .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = accent.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
            modifier = Modifier.size(38.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accent,
                modifier = Modifier.size(19.dp)
              )
            }
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = title,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = subtitle,
              style = MaterialTheme.typography.bodySmall,
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = accent.copy(alpha = 0.14f)
          ) {
            Text(
              text = badgeText,
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = accent,
              maxLines = 1,
              softWrap = false,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
          Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (expanded) "Collapse" else "Expand",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
          )
        }
      }

      AnimatedVisibility(
        visible = expanded,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
          content = content
        )
      }
    }
  }
}

/**
 * Executive Glass Dialog Header with a 38dp squircle icon badge, primary title, and monospace subtitle.
 */
@Composable
fun ExecutiveDialogHeader(
  icon: ImageVector,
  accent: Color,
  title: String,
  subtitle: String,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Surface(
      shape = RoundedCornerShape(13.dp),
      color = accent.copy(alpha = 0.16f),
      border = BorderStroke(1.dp, accent.copy(alpha = 0.42f)),
      modifier = Modifier.size(40.dp)
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = accent,
          modifier = Modifier.size(20.dp)
        )
      }
    }

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        fontFamily = JetBrainsMonoFontFamily,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = accent,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}


