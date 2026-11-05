package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
  primary = Color(0xFF38BDF8),
  onPrimary = Color(0xFF061527),
  primaryContainer = Color(0xFF143252),
  onPrimaryContainer = Color(0xFFE0F2FE),
  secondary = Color(0xFF34D399),
  onSecondary = Color(0xFF042F2E),
  secondaryContainer = Color(0xFF064E3B),
  onSecondaryContainer = Color(0xFFD1FAE5),
  tertiary = Color(0xFFFBBF24),
  background = IosGroupedBgDark,
  onBackground = Color(0xFFF1F5F9),
  surface = IosSurfaceCardDark,
  onSurface = Color(0xFFF8FAFC),
  surfaceVariant = IosElevatedGlassDark,
  onSurfaceVariant = Color(0xFF94A3B8)
)

private val LightColorScheme = lightColorScheme(
  primary = SapphirePrimary,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFE0F2FE),
  onPrimaryContainer = SapphirePrimary,
  secondary = EmeraldOptimal,
  onSecondary = Color.White,
  secondaryContainer = EmeraldSoftBg,
  onSecondaryContainer = Color(0xFF064E3B),
  tertiary = Color(0xFFD97706),
  background = IosGroupedBgLight,
  onBackground = Color(0xFF0F172A),
  surface = IosSurfaceCardLight,
  onSurface = Color(0xFF0F172A),
  surfaceVariant = IosElevatedGlassLight,
  onSurfaceVariant = Color(0xFF475569)
)

// iOS Continuous Squircle Shapes
val IosSquircleShapes = Shapes(
  extraSmall = RoundedCornerShape(10.dp),
  small = RoundedCornerShape(14.dp),
  medium = RoundedCornerShape(20.dp),
  large = RoundedCornerShape(26.dp),
  extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = IosSquircleShapes,
    content = content
  )
}
