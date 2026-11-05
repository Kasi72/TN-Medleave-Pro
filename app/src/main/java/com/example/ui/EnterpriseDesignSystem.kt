package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
