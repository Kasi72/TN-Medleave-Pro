package com.example.ui

import android.graphics.BlurMaskFilter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.ui.theme.LocalNeumorphicColors
import com.example.ui.theme.NeumorphicColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.min

private fun outlineToComposePath(outline: Outline): Path = when (outline) {
  is Outline.Generic -> outline.path
  is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
  is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
}

private fun makeBlurPaint(color: Color, blurPx: Float): android.graphics.Paint {
  return android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
    this.color = color.toArgb()
    style = android.graphics.Paint.Style.FILL
    if (blurPx > 0.5f) {
      runCatching {
        maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
      }
    }
  }
}

/**
 * Core ui-neumorphism Extruded / Raised Soft-UI Modifier.
 * Renders dual opposite directional soft shadows (top-left light highlight + bottom-right dark shadow)
 * outside the shape bounds, then clips and paints a subtle convex surface fill with a crisp highlight bevel.
 */
@Composable
fun Modifier.neumorphicExtruded(
  shape: Shape = IosSharpCardShape,
  elevation: Dp = 5.dp,
  blurRadius: Dp = 10.dp,
  accentTint: Color? = null,
  accentTintAlpha: Float = 0.04f,
  surfaceColor: Color? = null,
  showBorder: Boolean = true,
  borderAlpha: Float = 0.35f,
  neu: NeumorphicColors = LocalNeumorphicColors.current
): Modifier {
  val baseFill = surfaceColor ?: if (accentTint != null) {
    lerp(neu.surfaceRaised, accentTint, accentTintAlpha.coerceIn(0f, 0.25f))
  } else {
    neu.surfaceRaised
  }
  val topLeftConvex = lerp(baseFill, neu.bevelHighlight, if (neu.isDark) 0.32f else 0.48f)
  val bottomRightConvex = lerp(baseFill, neu.darkShadow, if (neu.isDark) 0.14f else 0.09f)
  val rimStart = neu.lightShadow.copy(alpha = if (neu.isDark) 0.42f else 0.85f)
  val rimEnd = (accentTint ?: neu.darkShadow).copy(alpha = borderAlpha.coerceIn(0.10f, 0.55f))

  return this
    .drawWithCache {
      val offsetPx = elevation.toPx()
      val blurPx = blurRadius.toPx()
      val outline = shape.createOutline(size, layoutDirection, this)
      val composePath = outlineToComposePath(outline)
      val androidPath = composePath.asAndroidPath()
      val lightPaint = makeBlurPaint(neu.lightShadow.copy(alpha = if (neu.isDark) 0.82f else 0.92f), blurPx)
      val darkPaint = makeBlurPaint(neu.darkShadow.copy(alpha = if (neu.isDark) 0.90f else 0.72f), blurPx)

      onDrawBehind {
        drawIntoCanvas { canvas ->
          val nc = canvas.nativeCanvas
          nc.save()
          nc.translate(-offsetPx, -offsetPx)
          nc.drawPath(androidPath, lightPaint)
          nc.restore()

          nc.save()
          nc.translate(offsetPx, offsetPx)
          nc.drawPath(androidPath, darkPaint)
          nc.restore()
        }
      }
    }
    .clip(shape)
    .background(
      brush = Brush.linearGradient(
        colors = listOf(topLeftConvex, baseFill, bottomRightConvex),
        start = Offset(0f, 0f),
        end = Offset(720f, 720f)
      ),
      shape = shape
    )
    .then(
      if (showBorder) {
        Modifier.border(
          width = 0.85.dp,
          brush = Brush.linearGradient(
            colors = listOf(rimStart, rimEnd),
            start = Offset(0f, 0f),
            end = Offset(550f, 550f)
          ),
          shape = shape
        )
      } else {
        Modifier
      }
    )
}

/**
 * Core ui-neumorphism Inset / Carved Soft-UI Modifier.
 * Uses inverted shape PathOperation.Difference clipped to the container bounds to render authentic
 * inner top-left dark shadow and inner bottom-right bright highlight (identical to CSS box-shadow: inset).
 */
@Composable
fun Modifier.neumorphicInset(
  shape: Shape = IosSharpWellShape,
  depth: Dp = 3.5.dp,
  blurRadius: Dp = 7.dp,
  accentTint: Color? = null,
  accentTintAlpha: Float = 0.05f,
  surfaceColor: Color? = null,
  borderColor: Color? = null,
  neu: NeumorphicColors = LocalNeumorphicColors.current
): Modifier {
  val baseFill = surfaceColor ?: if (accentTint != null) {
    lerp(neu.surfaceInset, accentTint, accentTintAlpha.coerceIn(0f, 0.22f))
  } else {
    neu.surfaceInset
  }
  val concaveTopLeft = lerp(baseFill, neu.darkShadow, if (neu.isDark) 0.16f else 0.12f)
  val concaveBottomRight = lerp(baseFill, neu.lightShadow, if (neu.isDark) 0.10f else 0.18f)

  return this
    .clip(shape)
    .background(
      brush = Brush.linearGradient(
        colors = listOf(concaveTopLeft, baseFill, concaveBottomRight),
        start = Offset(0f, 0f),
        end = Offset(600f, 600f)
      ),
      shape = shape
    )
    .drawWithCache {
      val offsetPx = depth.toPx()
      val blurPx = blurRadius.toPx()
      val outline = shape.createOutline(size, layoutDirection, this)
      val holePath = outlineToComposePath(outline)
      val pad = (blurPx * 2f) + offsetPx + 24f
      val outerRect = Path().apply {
        addRect(Rect(-pad, -pad, size.width + pad, size.height + pad))
      }
      val ringPath = Path.combine(PathOperation.Difference, outerRect, holePath)
      val androidRing = ringPath.asAndroidPath()
      val darkPaint = makeBlurPaint(neu.darkShadow.copy(alpha = if (neu.isDark) 0.92f else 0.72f), blurPx)
      val lightPaint = makeBlurPaint(neu.lightShadow.copy(alpha = if (neu.isDark) 0.78f else 0.92f), blurPx)

      onDrawBehind {
        drawIntoCanvas { canvas ->
          canvas.save()
          canvas.clipPath(holePath)
          val nc = canvas.nativeCanvas
          nc.save()
          nc.translate(offsetPx, offsetPx)
          nc.drawPath(androidRing, darkPaint)
          nc.restore()

          nc.save()
          nc.translate(-offsetPx, -offsetPx)
          nc.drawPath(androidRing, lightPaint)
          nc.restore()
          canvas.restore()
        }
        if (borderColor != null) {
          drawPath(
            path = holePath,
            color = borderColor,
            style = Stroke(width = 1.dp.toPx())
          )
        }
      }
    }
}

/**
 * Reusable Neumorphic Soft-UI Pill/Track Linear Progress Bar (Inset trough + Glowing Raised Fill).
 */
@Composable
fun NeumorphicLinearProgress(
  progress: Float,
  accentColor: Color,
  modifier: Modifier = Modifier,
  height: Dp = 8.dp
) {
  val clamped = progress.coerceIn(0f, 1f)
  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(height)
      .neumorphicInset(
        shape = IosSharpMicroShape,
        depth = 2.dp,
        blurRadius = 4.dp
      )
      .padding(1.5.dp)
  ) {
    if (clamped > 0.01f) {
      Box(
        modifier = Modifier
          .fillMaxWidth(clamped.coerceAtLeast(0.04f))
          .fillMaxHeight()
          .clip(IosSharpMicroShape)
          .background(
            Brush.horizontalGradient(
              colors = listOf(
                accentColor.copy(alpha = 0.82f),
                accentColor
              )
            )
          )
      )
    }
  }
}

/**
 * Reusable ui-neumorphism Tactile Soft-UI Switch (Inset Track + Extruded Circular/Squircle Thumb).
 */
@Composable
fun NeumorphicSwitch(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  accentColor: Color = EmeraldOptimal
) {
  val neu = LocalNeumorphicColors.current
  val thumbOffset by animateDpAsState(
    targetValue = if (checked) 22.dp else 2.dp,
    animationSpec = spring(),
    label = "neu_switch_offset"
  )
  val trackBg by animateColorAsState(
    targetValue = if (checked) lerp(neu.surfaceInset, accentColor, 0.26f) else neu.surfaceInset,
    animationSpec = spring(),
    label = "neu_switch_bg"
  )

  Box(
    modifier = modifier
      .width(52.dp)
      .height(30.dp)
      .neumorphicInset(
        shape = IosSharpButtonShape,
        depth = 2.5.dp,
        blurRadius = 5.dp,
        surfaceColor = trackBg,
        borderColor = if (checked) accentColor.copy(alpha = 0.55f) else neu.hairlineDivider
      )
      .clickable { onCheckedChange(!checked) },
    contentAlignment = Alignment.CenterStart
  ) {
    Box(
      modifier = Modifier
        .offset(x = thumbOffset)
        .size(24.dp)
        .neumorphicExtruded(
          shape = CircleShape,
          elevation = 2.dp,
          blurRadius = 4.dp,
          surfaceColor = if (checked) accentColor else neu.surfaceRaised,
          showBorder = true,
          borderAlpha = 0.45f
        )
    )
  }
}

/**
 * Full-bleed Single-Hue Neumorphic Soft-UI Backdrop.
 * Provides a calm, matte tactile canvas so extruded & inset shadow geometry across cards pops vividly.
 */
@Composable
fun IosAmbientGlassBackdrop(
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit
) {
  val neu = LocalNeumorphicColors.current
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            lerp(neu.baseBg, neu.bevelHighlight, 0.14f),
            neu.baseBg,
            lerp(neu.baseBg, neu.darkShadow, 0.06f)
          )
        )
      ),
    content = content
  )
}

/**
 * Neumorphic Raised Card Modifier (drop-in replacement for all iosFrostedGlassCard surfaces).
 */
@Composable
fun Modifier.iosFrostedGlassCard(
  accentColor: Color = EmeraldOptimal,
  shape: Shape = IosSharpCardShape,
  borderWidth: Dp = 1.dp,
  borderAlpha: Float = 0.30f
): Modifier {
  return this.neumorphicExtruded(
    shape = shape,
    elevation = 4.5.dp,
    blurRadius = 9.dp,
    accentTint = accentColor,
    accentTintAlpha = 0.035f,
    showBorder = borderWidth > 0.dp,
    borderAlpha = borderAlpha
  )
}

/**
 * Neumorphic Executive Hero Container — adapts seamlessly to Mint Soft UI, Soft Clay Light, and Dark #292E35.
 * Uses raised extruded outer relief with subtle directional bevel highlight & soft accent glow.
 */
@Composable
fun EnterpriseMeshHeroContainer(
  modifier: Modifier = Modifier,
  cornerRadius: Dp = 18.dp,
  primaryGlowColor: Color = EmeraldOptimal,
  secondaryGlowColor: Color = BadgeColCyan,
  contentPadding: PaddingValues = PaddingValues(18.dp),
  content: @Composable BoxScope.() -> Unit
) {
  val neu = LocalNeumorphicColors.current
  val clampedRadius = min(cornerRadius.value, 22f).coerceAtLeast(14f).dp
  val shape = IosSharpSquircleShape(clampedRadius, sharpnessBias = 0.74f)

  Box(
    modifier = modifier
      .fillMaxWidth()
      .neumorphicExtruded(
        shape = shape,
        elevation = 6.dp,
        blurRadius = 12.dp,
        accentTint = primaryGlowColor,
        accentTintAlpha = 0.045f,
        showBorder = true,
        borderAlpha = 0.34f,
        neu = neu
      )
      .padding(contentPadding),
    content = content
  )
}

/**
 * Tactile Neumorphic Control Center Quick-Action Tile Strip.
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
    horizontalArrangement = Arrangement.spacedBy(9.dp)
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
  val neu = LocalNeumorphicColors.current
  Box(
    modifier = modifier
      .neumorphicExtruded(
        shape = IosSharpButtonShape,
        elevation = 4.dp,
        blurRadius = 8.dp,
        accentTint = accent,
        accentTintAlpha = 0.04f,
        borderAlpha = 0.28f
      )
      .clickable { onClick() }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 9.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(26.dp)
            .neumorphicInset(
              shape = IosSharpMicroShape,
              depth = 2.dp,
              blurRadius = 4.dp,
              accentTint = accent,
              accentTintAlpha = 0.12f
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = icon,
            contentDescription = title,
            tint = accent,
            modifier = Modifier.size(14.dp)
          )
        }
        Icon(
          imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
          contentDescription = null,
          tint = neu.textSecondary.copy(alpha = 0.72f),
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
          color = neu.textPrimary,
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
 * Neumorphic Section Header with inset indicator jewel & extruded pill badge.
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
  val neu = LocalNeumorphicColors.current
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
              .size(width = 8.dp, height = 8.dp)
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
          color = neu.textPrimary,
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis
        )
      }

      if (badgeText != null) {
        Spacer(modifier = Modifier.width(10.dp))
        Box(
          modifier = Modifier
            .neumorphicExtruded(
              shape = IosSharpBadgeShape,
              elevation = 3.dp,
              blurRadius = 6.dp,
              accentTint = badgeColor,
              accentTintAlpha = 0.08f,
              borderAlpha = 0.35f
            )
            .then(if (onBadgeClick != null) Modifier.clickable { onBadgeClick() } else Modifier)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
          ) {
            Text(
              text = badgeText,
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 9.5.sp,
              lineHeight = 12.sp,
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
        color = neu.textSecondary,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

/**
 * Neumorphic 3-Pill Live Governance & Telemetry Strip.
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
    horizontalArrangement = Arrangement.spacedBy(9.dp)
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
  val neu = LocalNeumorphicColors.current
  Box(
    modifier = modifier
      .neumorphicInset(
        shape = IosSharpButtonShape,
        depth = 2.5.dp,
        blurRadius = 5.dp,
        accentTint = accent,
        accentTintAlpha = 0.04f,
        borderColor = accent.copy(alpha = 0.25f)
      )
      .clickable { onClick() }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 9.dp, vertical = 8.dp),
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
          color = neu.textSecondary,
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
 * Neumorphic Card Header Row with inset icon well and extruded badge pill.
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
  val neu = LocalNeumorphicColors.current
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
      Box(
        modifier = Modifier
          .size(38.dp)
          .neumorphicInset(
            shape = IosSharpButtonShape,
            depth = 2.5.dp,
            blurRadius = 5.dp,
            accentTint = iconAccent,
            accentTintAlpha = 0.12f
          ),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = iconAccent,
          modifier = Modifier.size(19.dp)
        )
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = (-0.2).sp,
          color = neu.textPrimary,
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
            color = neu.textSecondary,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }

    if (!badgeText.isNullOrBlank()) {
      Spacer(modifier = Modifier.width(8.dp))
      Box(
        modifier = Modifier
          .neumorphicExtruded(
            shape = IosSharpBadgeShape,
            elevation = 2.5.dp,
            blurRadius = 5.dp,
            accentTint = badgeAccent,
            accentTintAlpha = 0.08f,
            borderAlpha = 0.35f
          )
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
          modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
      }
    }
  }
}

/**
 * 1-Tap Native Material 3 Calendar DatePicker + Inset Neumorphic ISO Text Field.
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
  val neu = LocalNeumorphicColors.current
  val friendlyHint = remember(value) {
    val clean = value.trim()
    if (clean.length == 10) ClinicalDateFormatter.formatFriendlyWithDay(clean) else null
  }

  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .neumorphicInset(
          shape = IosSharpWellShape,
          depth = 3.dp,
          blurRadius = 6.dp
        )
    ) {
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
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = neu.primaryAccent.copy(alpha = 0.55f),
          unfocusedBorderColor = Color.Transparent,
          focusedContainerColor = Color.Transparent,
          unfocusedContainerColor = Color.Transparent
        ),
        modifier = Modifier.fillMaxWidth()
      )
    }
    if (showFriendlySupportingText && friendlyHint != null) {
      Text(
        text = friendlyHint,
        fontFamily = JetBrainsMonoFontFamily,
        fontSize = 10.sp,
        color = EmeraldOptimal,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(start = 8.dp)
      )
    }
  }

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
 * Tier-3 Progressive Disclosure Accordion Card with Extruded Neumorphic Relief.
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
  val neu = LocalNeumorphicColors.current
  Box(
    modifier = modifier
      .fillMaxWidth()
      .neumorphicExtruded(
        shape = IosSharpCardShape,
        elevation = if (expanded) 5.5.dp else 4.dp,
        blurRadius = if (expanded) 11.dp else 8.dp,
        accentTint = accent,
        accentTintAlpha = 0.04f,
        borderAlpha = if (expanded) 0.40f else 0.24f
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
          Box(
            modifier = Modifier
              .size(36.dp)
              .neumorphicInset(
                shape = IosSharpButtonShape,
                depth = 2.dp,
                blurRadius = 4.dp,
                accentTint = accent,
                accentTintAlpha = 0.12f
              ),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = icon,
              contentDescription = title,
              tint = accent,
              modifier = Modifier.size(18.dp)
            )
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = title,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.ExtraBold,
              color = neu.textPrimary,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = subtitle,
              style = MaterialTheme.typography.bodySmall,
              fontSize = 11.sp,
              color = neu.textSecondary,
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
          Box(
            modifier = Modifier
              .neumorphicInset(
                shape = IosSharpBadgeShape,
                depth = 2.dp,
                blurRadius = 4.dp,
                accentTint = accent,
                accentTintAlpha = 0.10f
              )
          ) {
            Text(
              text = badgeText,
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = accent,
              maxLines = 1,
              softWrap = false,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
          }
          Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (expanded) "Collapse" else "Expand",
            tint = neu.textSecondary,
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
 * Executive Neumorphic Dialog Header with an inset icon badge, primary title, and monospace subtitle.
 */
@Composable
fun ExecutiveDialogHeader(
  icon: ImageVector,
  accent: Color,
  title: String,
  subtitle: String,
  modifier: Modifier = Modifier
) {
  val neu = LocalNeumorphicColors.current
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Box(
      modifier = Modifier
        .size(38.dp)
        .neumorphicInset(
          shape = IosSharpButtonShape,
          depth = 2.5.dp,
          blurRadius = 5.dp,
          accentTint = accent,
          accentTintAlpha = 0.14f
        ),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = accent,
        modifier = Modifier.size(19.dp)
      )
    }

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.ExtraBold,
        color = neu.textPrimary,
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
