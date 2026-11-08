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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.PictureAsPdf
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.BadgeColCyan
import com.example.ui.theme.BadgeOnDutyBlue
import com.example.ui.theme.BadgePlGold
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.IosSharpBadgeShape
import com.example.ui.theme.IosSharpButtonShape
import com.example.ui.theme.IosSharpCardShape
import com.example.ui.theme.IosSharpMicroShape
import com.example.ui.theme.IosSharpSquircleShape
import com.example.ui.theme.IosSharpWellShape
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppThemeMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.min

/**
 * Full-bleed Apple iOS 18 Ambient Mesh Backdrop.
 * Renders subtle, theme-adaptive atmospheric light blooms behind scrollable screens so
 * translucent glassmorphic cards refract soft color and specular depth.
 */
@Composable
fun IosAmbientGlassBackdrop(
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit
) {
  val themeMode = LocalAppThemeMode.current
  val baseBg = MaterialTheme.colorScheme.background

  val orbTopLeft = when (themeMode) {
    AppThemeMode.GREEN_EMERALD -> Color(0xFF10B981).copy(alpha = 0.14f)
    AppThemeMode.WHITE_CLINICAL -> Color(0xFF0EA5E9).copy(alpha = 0.09f)
    AppThemeMode.DARK_OBSIDIAN -> Color(0xFF059669).copy(alpha = 0.16f)
  }
  val orbMidRight = when (themeMode) {
    AppThemeMode.GREEN_EMERALD -> Color(0xFF06B6D4).copy(alpha = 0.11f)
    AppThemeMode.WHITE_CLINICAL -> Color(0xFF10B981).copy(alpha = 0.08f)
    AppThemeMode.DARK_OBSIDIAN -> Color(0xFF0284C7).copy(alpha = 0.15f)
  }
  val orbBottomLeft = when (themeMode) {
    AppThemeMode.GREEN_EMERALD -> Color(0xFF34D399).copy(alpha = 0.12f)
    AppThemeMode.WHITE_CLINICAL -> Color(0xFF6366F1).copy(alpha = 0.06f)
    AppThemeMode.DARK_OBSIDIAN -> Color(0xFF14B8A6).copy(alpha = 0.12f)
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(baseBg)
      .drawBehind {
        // 1. Top-Left Ambient Light Bloom
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(orbTopLeft, Color.Transparent),
            center = Offset(size.width * 0.12f, size.height * 0.10f),
            radius = size.width * 0.72f
          ),
          center = Offset(size.width * 0.12f, size.height * 0.10f),
          radius = size.width * 0.72f
        )

        // 2. Mid-Right Ambient Light Bloom
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(orbMidRight, Color.Transparent),
            center = Offset(size.width * 0.90f, size.height * 0.44f),
            radius = size.width * 0.68f
          ),
          center = Offset(size.width * 0.90f, size.height * 0.44f),
          radius = size.width * 0.68f
        )

        // 3. Bottom-Left Ambient Light Bloom
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(orbBottomLeft, Color.Transparent),
            center = Offset(size.width * 0.20f, size.height * 0.86f),
            radius = size.width * 0.75f
          ),
          center = Offset(size.width * 0.20f, size.height * 0.86f),
          radius = size.width * 0.75f
        )
      },
    content = content
  )
}

/**
 * Reusable Apple iOS 18 / visionOS Frosted Glassmorphism Modifier for cards and containers.
 * Combines:
 * 1. Translucent multi-stop frosted surface fill
 * 2. Top-left radial light refraction + diagonal 45° glass sheen
 * 3. Crisp 1.5px top inner specular highlight bevel
 * 4. Dual-stop refractive gradient border (bright top-left specular edge to subtle bottom-right rim)
 */
@Composable
fun Modifier.iosFrostedGlassCard(
  accentColor: Color = EmeraldOptimal,
  shape: Shape = IosSharpCardShape,
  borderWidth: Dp = 1.15.dp,
  borderAlpha: Float = 0.46f
): Modifier {
  val themeMode = LocalAppThemeMode.current
  val isDark = themeMode == AppThemeMode.DARK_OBSIDIAN
  val isWhite = themeMode == AppThemeMode.WHITE_CLINICAL

  val glassTopStart = when {
    isDark -> Color(0xFF13253E).copy(alpha = 0.82f)
    isWhite -> Color(0xFFFFFFFF).copy(alpha = 0.86f)
    else -> Color(0xFFFAFFFC).copy(alpha = 0.84f)
  }
  val glassBottomEnd = when {
    isDark -> Color(0xFF0A1424).copy(alpha = 0.90f)
    isWhite -> Color(0xFFF8FAFC).copy(alpha = 0.93f)
    else -> Color(0xFFECFDF5).copy(alpha = 0.91f)
  }
  val topInnerBevelColor = when {
    isDark -> Color.White.copy(alpha = 0.24f)
    isWhite -> Color.White.copy(alpha = 0.92f)
    else -> Color.White.copy(alpha = 0.82f)
  }
  val diagonalSheenColor = when {
    isDark -> Color.White.copy(alpha = 0.045f)
    isWhite -> Color.White.copy(alpha = 0.55f)
    else -> Color.White.copy(alpha = 0.42f)
  }
  val topRimColor = if (isDark) {
    Color.White.copy(alpha = 0.30f)
  } else {
    Color.White.copy(alpha = 0.85f)
  }

  return this
    .clip(shape)
    .background(
      brush = Brush.linearGradient(
        colors = listOf(glassTopStart, glassBottomEnd),
        start = Offset(0f, 0f),
        end = Offset(700f, 700f)
      ),
      shape = shape
    )
    .drawBehind {
      // 1. Subtle top-left accent refraction bloom inside the frosted glass
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            accentColor.copy(alpha = if (isDark) 0.13f else 0.09f),
            Color.Transparent
          ),
          center = Offset(size.width * 0.14f, size.height * 0.12f),
          radius = size.width * 0.62f
        ),
        center = Offset(size.width * 0.14f, size.height * 0.12f),
        radius = size.width * 0.62f
      )

      // 2. Diagonal iOS glass reflection sheen
      drawRect(
        brush = Brush.linearGradient(
          colors = listOf(
            diagonalSheenColor,
            Color.Transparent,
            accentColor.copy(alpha = if (isDark) 0.05f else 0.035f)
          ),
          start = Offset(0f, 0f),
          end = Offset(size.width, size.height)
        )
      )

      // 3. Crisp 1.5px top inner specular highlight bevel
      drawLine(
        brush = Brush.horizontalGradient(
          colors = listOf(
            Color.Transparent,
            topInnerBevelColor,
            accentColor.copy(alpha = 0.38f),
            Color.Transparent
          )
        ),
        start = Offset(size.width * 0.05f, 1.2f),
        end = Offset(size.width * 0.95f, 1.2f),
        strokeWidth = 1.6f
      )
    }
    .border(
      width = borderWidth,
      brush = Brush.linearGradient(
        colors = listOf(
          topRimColor,
          accentColor.copy(alpha = borderAlpha),
          accentColor.copy(alpha = borderAlpha * 0.35f)
        ),
        start = Offset(0f, 0f),
        end = Offset(600f, 600f)
      ),
      shape = shape
    )
}

/**
 * Apple iOS 18 Pro Sharp-Edged Squircle Hero Container with:
 * - Crisp superellipse squircle geometry (clamped to 12.dp max for razor-sharp architectural corners)
 * - Multi-stop deep clinical sapphire & emerald frosted glass gradient background
 * - Custom Canvas architectural micro-grid, radial light orbs, diagonal glass sheen, and top specular bevel
 */
@Composable
fun EnterpriseMeshHeroContainer(
  modifier: Modifier = Modifier,
  cornerRadius: Dp = 12.dp,
  primaryGlowColor: Color = Color(0xFF10B981),
  secondaryGlowColor: Color = Color(0xFF0EA5E9),
  contentPadding: PaddingValues = PaddingValues(18.dp),
  content: @Composable BoxScope.() -> Unit
) {
  val sharpRadius = min(cornerRadius.value, 12f).dp
  val shape = IosSharpSquircleShape(sharpRadius, sharpnessBias = 0.84f)
  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(shape)
      .background(
        Brush.linearGradient(
          colors = listOf(
            Color(0xFF051122).copy(alpha = 0.95f),
            Color(0xFF0B2442).copy(alpha = 0.93f),
            Color(0xFF083340).copy(alpha = 0.93f),
            Color(0xFF042A27).copy(alpha = 0.95f)
          ),
          start = Offset(0f, 0f),
          end = Offset(1000f, 900f)
        )
      )
      .drawBehind {
        // 1. Subtle architectural grid lines
        val step = 32.dp.toPx()
        val gridColor = Color.White.copy(alpha = 0.04f)
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
              primaryGlowColor.copy(alpha = 0.30f),
              Color.Transparent
            ),
            center = Offset(size.width * 0.88f, size.height * 0.18f),
            radius = size.width * 0.56f
          ),
          center = Offset(size.width * 0.88f, size.height * 0.18f),
          radius = size.width * 0.56f
        )

        // 3. Bottom-left ambient sapphire/cyan radial orb
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(
              secondaryGlowColor.copy(alpha = 0.24f),
              Color.Transparent
            ),
            center = Offset(size.width * 0.12f, size.height * 0.88f),
            radius = size.width * 0.52f
          ),
          center = Offset(size.width * 0.12f, size.height * 0.88f),
          radius = size.width * 0.52f
        )

        // 4. Diagonal frosted glass reflection band
        drawRect(
          brush = Brush.linearGradient(
            colors = listOf(
              Color.White.copy(alpha = 0.09f),
              Color.Transparent,
              Color.White.copy(alpha = 0.03f)
            ),
            start = Offset(0f, 0f),
            end = Offset(size.width * 0.85f, size.height)
          )
        )

        // 5. iOS 18 Pro Top Specular Chamfer Line
        drawLine(
          brush = Brush.horizontalGradient(
            colors = listOf(
              Color.Transparent,
              Color.White.copy(alpha = 0.65f),
              primaryGlowColor.copy(alpha = 0.75f),
              Color.Transparent
            )
          ),
          start = Offset(size.width * 0.05f, 1.5f),
          end = Offset(size.width * 0.95f, 1.5f),
          strokeWidth = 2.4f
        )
      }
      .border(
        width = 1.3.dp,
        brush = Brush.linearGradient(
          colors = listOf(
            Color.White.copy(alpha = 0.48f),
            primaryGlowColor.copy(alpha = 0.58f),
            secondaryGlowColor.copy(alpha = 0.38f)
          )
        ),
        shape = shape
      )
      .padding(contentPadding),
    content = content
  )
}

/**
 * Apple iOS 18 Control Center Quick-Action Bento Strip with Frosted Glassmorphism.
 */
@Composable
fun IosControlCenterBentoStrip(
  onLogLeave: () -> Unit,
  onOpenColAudit: () -> Unit,
  onOpenOptimizer: () -> Unit,
  onExportPdf: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .height(IntrinsicSize.Min),
    horizontalArrangement = Arrangement.spacedBy(7.dp)
  ) {
    IosControlCenterTile(
      icon = Icons.Default.Add,
      title = "Log Leave",
      subtitle = "CL • RH • EL",
      accent = EmeraldOptimal,
      onClick = onLogLeave,
      modifier = Modifier
        .weight(1f)
        .fillMaxHeight()
    )
    IosControlCenterTile(
      icon = Icons.Default.FactCheck,
      title = "COL ↔ GH",
      subtitle = "Pair Duty",
      accent = BadgeColCyan,
      onClick = onOpenColAudit,
      modifier = Modifier
        .weight(1f)
        .fillMaxHeight()
    )
    IosControlCenterTile(
      icon = Icons.Default.AutoAwesome,
      title = "AI Bridge",
      subtitle = "10d Cap",
      accent = BadgePlGold,
      onClick = onOpenOptimizer,
      modifier = Modifier
        .weight(1f)
        .fillMaxHeight()
    )
    IosControlCenterTile(
      icon = Icons.Default.PictureAsPdf,
      title = "A4 Report",
      subtitle = "Dean PDF",
      accent = BadgeOnDutyBlue,
      onClick = onExportPdf,
      modifier = Modifier
        .weight(1f)
        .fillMaxHeight()
    )
  }
}

@Composable
private fun IosControlCenterTile(
  icon: ImageVector,
  title: String,
  subtitle: String,
  accent: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .iosFrostedGlassCard(
        accentColor = accent,
        shape = IosSharpButtonShape,
        borderWidth = 1.1.dp,
        borderAlpha = 0.44f
      )
      .clickable { onClick() }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = IosSharpMicroShape,
          color = accent.copy(alpha = 0.16f),
          border = BorderStroke(0.8.dp, accent.copy(alpha = 0.45f)),
          modifier = Modifier.size(24.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = icon,
              contentDescription = title,
              tint = accent,
              modifier = Modifier.size(14.dp)
            )
          }
        }
        Icon(
          imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
          modifier = Modifier.size(14.dp)
        )
      }
      Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
          text = title,
          style = MaterialTheme.typography.labelLarge,
          fontSize = 11.sp,
          lineHeight = 13.sp,
          fontWeight = FontWeight.ExtraBold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = subtitle,
          fontFamily = JetBrainsMonoFontFamily,
          fontSize = 8.5.sp,
          lineHeight = 10.sp,
          fontWeight = FontWeight.Bold,
          color = accent,
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}

/**
 * iOS Inset Grouped Section Header with sharp squircle indicator, tight SF-style title, and frosted glass badge.
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
              .size(width = 7.dp, height = 7.dp)
              .clip(IosSharpMicroShape)
              .background(badgeColor)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = indexTag.uppercase(),
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 9.5.sp,
            lineHeight = 12.sp,
            letterSpacing = 0.4.sp,
            fontWeight = FontWeight.ExtraBold,
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
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = (-0.3).sp,
          color = MaterialTheme.colorScheme.onBackground,
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis
        )
      }

      if (badgeText != null) {
        Spacer(modifier = Modifier.width(10.dp))
        Surface(
          shape = IosSharpBadgeShape,
          color = badgeColor.copy(alpha = 0.14f),
          border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.42f)),
          modifier = Modifier
            .clip(IosSharpBadgeShape)
            .then(if (onBadgeClick != null) Modifier.clickable { onBadgeClick() } else Modifier)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
          ) {
            Text(
              text = badgeText,
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 10.sp,
              lineHeight = 13.sp,
              fontWeight = FontWeight.ExtraBold,
              color = badgeColor,
              maxLines = 1,
              softWrap = false
            )
            if (onBadgeClick != null) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = badgeColor,
                modifier = Modifier.size(13.dp)
              )
            }
          }
        }
      }
    }

    if (!subtitle.isNullOrBlank()) {
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        fontSize = 11.5.sp,
        lineHeight = 15.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

/**
 * Enterprise 3-Pill Live Governance & Telemetry Strip with Frosted Glassmorphism & Sharp iOS Squircles.
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
    horizontalArrangement = Arrangement.spacedBy(7.dp)
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
  Box(
    modifier = modifier
      .iosFrostedGlassCard(
        accentColor = accent,
        shape = IosSharpButtonShape,
        borderWidth = 1.1.dp,
        borderAlpha = 0.42f
      )
      .clickable { onClick() }
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // Sharp top accent laser strip
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(2.dp)
          .background(accent)
      )
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 9.dp, vertical = 7.dp),
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
            modifier = Modifier.size(11.dp)
          )
          Text(
            text = label,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 8.5.sp,
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
          fontSize = 10.5.sp,
          lineHeight = 13.sp,
          fontWeight = FontWeight.ExtraBold,
          color = accent,
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis
        )
      }
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
 * Guaranteed Zero-Vertical-Wrap Card Header Row with sharp iOS squircle icon badge.
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
        shape = IosSharpButtonShape,
        color = iconAccent.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, iconAccent.copy(alpha = 0.45f)),
        modifier = Modifier.size(38.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconAccent,
            modifier = Modifier.size(19.dp)
          )
        }
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = (-0.2).sp,
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
        shape = IosSharpBadgeShape,
        color = badgeAccent.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, badgeAccent.copy(alpha = 0.45f)),
        modifier = Modifier
          .clip(IosSharpBadgeShape)
          .then(if (onBadgeClick != null) Modifier.clickable { onBadgeClick() } else Modifier)
      ) {
        Text(
          text = badgeText,
          fontFamily = JetBrainsMonoFontFamily,
          fontSize = 9.5.sp,
          lineHeight = 12.sp,
          fontWeight = FontWeight.ExtraBold,
          color = badgeAccent,
          maxLines = 1,
          softWrap = false,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
      }
    }
  }
}

/**
 * 1-Tap Native Material 3 Calendar DatePicker + Direct ISO Text Field with sharp iOS squircle border.
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
    shape = IosSharpWellShape,
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
 * Tier-3 Progressive Disclosure Accordion Card with Frosted Glassmorphism & sharp iOS squircle geometry.
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
  Box(
    modifier = modifier
      .fillMaxWidth()
      .iosFrostedGlassCard(
        accentColor = accent,
        shape = IosSharpCardShape,
        borderWidth = if (expanded) 1.3.dp else 1.dp,
        borderAlpha = if (expanded) 0.55f else 0.35f
      )
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { expanded = !expanded }
          .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Surface(
            shape = IosSharpButtonShape,
            color = accent.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.40f)),
            modifier = Modifier.size(36.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accent,
                modifier = Modifier.size(18.dp)
              )
            }
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = title,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.ExtraBold,
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
            shape = IosSharpBadgeShape,
            color = accent.copy(alpha = 0.14f),
            border = BorderStroke(0.8.dp, accent.copy(alpha = 0.35f))
          ) {
            Text(
              text = badgeText,
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = accent,
              maxLines = 1,
              softWrap = false,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
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
            .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
          content = content
        )
      }
    }
  }
}

/**
 * Executive Glass Dialog Header with a sharp 6dp squircle icon badge, primary title, and monospace subtitle.
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
      shape = IosSharpButtonShape,
      color = accent.copy(alpha = 0.16f),
      border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
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
