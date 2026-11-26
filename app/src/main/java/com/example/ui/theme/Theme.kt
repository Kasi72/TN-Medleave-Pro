package com.example.ui.theme

import android.content.Context
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.min

/**
 * Tactile Continuous Superellipse Squircle Shape tuned for Soft-UI Neumorphic relief.
 * Extends CornerBasedShape so it integrates natively with MaterialTheme.shapes as well as Canvas shadow paths.
 */
class IosSharpSquircleShape(
  topStart: CornerSize,
  topEnd: CornerSize,
  bottomEnd: CornerSize,
  bottomStart: CornerSize,
  private val sharpnessBias: Float = 0.74f
) : CornerBasedShape(
  topStart = topStart,
  topEnd = topEnd,
  bottomEnd = bottomEnd,
  bottomStart = bottomStart
) {

  constructor(
    topStart: Dp,
    topEnd: Dp,
    bottomEnd: Dp,
    bottomStart: Dp,
    sharpnessBias: Float = 0.74f
  ) : this(
    topStart = CornerSize(topStart),
    topEnd = CornerSize(topEnd),
    bottomEnd = CornerSize(bottomEnd),
    bottomStart = CornerSize(bottomStart),
    sharpnessBias = sharpnessBias
  )

  constructor(cornerRadius: Dp, sharpnessBias: Float = 0.74f) : this(
    topStart = CornerSize(cornerRadius),
    topEnd = CornerSize(cornerRadius),
    bottomEnd = CornerSize(cornerRadius),
    bottomStart = CornerSize(cornerRadius),
    sharpnessBias = sharpnessBias
  )

  override fun copy(
    topStart: CornerSize,
    topEnd: CornerSize,
    bottomEnd: CornerSize,
    bottomStart: CornerSize
  ): CornerBasedShape = IosSharpSquircleShape(
    topStart = topStart,
    topEnd = topEnd,
    bottomEnd = bottomEnd,
    bottomStart = bottomStart,
    sharpnessBias = sharpnessBias
  )

  override fun createOutline(
    size: Size,
    topStart: Float,
    topEnd: Float,
    bottomEnd: Float,
    bottomStart: Float,
    layoutDirection: LayoutDirection
  ): Outline {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) {
      return Outline.Rectangle(androidx.compose.ui.geometry.Rect(0f, 0f, w, h))
    }
    val maxR = min(w, h) / 2f
    val tl = (if (layoutDirection == LayoutDirection.Ltr) topStart else topEnd).coerceIn(0f, maxR)
    val tr = (if (layoutDirection == LayoutDirection.Ltr) topEnd else topStart).coerceIn(0f, maxR)
    val br = (if (layoutDirection == LayoutDirection.Ltr) bottomEnd else bottomStart).coerceIn(0f, maxR)
    val bl = (if (layoutDirection == LayoutDirection.Ltr) bottomStart else bottomEnd).coerceIn(0f, maxR)

    val k = sharpnessBias.coerceIn(0.60f, 0.88f)

    val path = Path().apply {
      moveTo(tl, 0f)
      lineTo(w - tr, 0f)
      if (tr > 0f) {
        cubicTo(
          w - tr * (1f - k), 0f,
          w, tr * (1f - k),
          w, tr
        )
      }
      lineTo(w, h - br)
      if (br > 0f) {
        cubicTo(
          w, h - br * (1f - k),
          w - br * (1f - k), h,
          w - br, h
        )
      }
      lineTo(bl, h)
      if (bl > 0f) {
        cubicTo(
          bl * (1f - k), h,
          0f, h - bl * (1f - k),
          0f, h - bl
        )
      }
      lineTo(0f, tl)
      if (tl > 0f) {
        cubicTo(
          0f, tl * (1f - k),
          tl * (1f - k), 0f,
          tl, 0f
        )
      }
      close()
    }
    return Outline.Generic(path)
  }
}

// Smooth Tactile Neumorphic Squircle Presets
val IosSharpMicroShape = IosSharpSquircleShape(7.dp, sharpnessBias = 0.72f)
val IosSharpBadgeShape = IosSharpSquircleShape(9.dp, sharpnessBias = 0.72f)
val IosSharpButtonShape = IosSharpSquircleShape(12.dp, sharpnessBias = 0.74f)
val IosSharpTabShape = IosSharpSquircleShape(13.dp, sharpnessBias = 0.74f)
val IosSharpWellShape = IosSharpSquircleShape(15.dp, sharpnessBias = 0.74f)
val IosSharpCardShape = IosSharpSquircleShape(18.dp, sharpnessBias = 0.74f)
val IosSharpHeroShape = IosSharpSquircleShape(22.dp, sharpnessBias = 0.74f)

@Immutable
data class NeumorphicColors(
  val isDark: Boolean,
  val baseBg: Color,
  val surfaceRaised: Color,
  val surfaceInset: Color,
  val lightShadow: Color,
  val darkShadow: Color,
  val bevelHighlight: Color,
  val primaryAccent: Color,
  val secondaryAccent: Color,
  val textPrimary: Color,
  val textSecondary: Color,
  val hairlineDivider: Color
)

private val MintNeumorphicColors = NeumorphicColors(
  isDark = false,
  baseBg = NeuMintBase,
  surfaceRaised = NeuMintRaised,
  surfaceInset = NeuMintInset,
  lightShadow = NeuMintShadowLight,
  darkShadow = NeuMintShadowDark,
  bevelHighlight = NeuMintBevel,
  primaryAccent = Color(0xFF047857),
  secondaryAccent = Color(0xFF0E7490),
  textPrimary = Color(0xFF062C22),
  textSecondary = Color(0xFF28584B),
  hairlineDivider = Color(0xFFA9C0B6).copy(alpha = 0.55f)
)

private val LightClayNeumorphicColors = NeumorphicColors(
  isDark = false,
  baseBg = NeuLightBase,
  surfaceRaised = NeuLightRaised,
  surfaceInset = NeuLightInset,
  lightShadow = NeuLightShadowLight,
  darkShadow = NeuLightShadowDark,
  bevelHighlight = NeuLightBevel,
  primaryAccent = Color(0xFF0284C7),
  secondaryAccent = Color(0xFF059669),
  textPrimary = Color(0xFF1E293B),
  textSecondary = Color(0xFF475569),
  hairlineDivider = Color(0xFFB8C4D8).copy(alpha = 0.60f)
)

private val DarkNeumorphicColors = NeumorphicColors(
  isDark = true,
  baseBg = NeuDarkBase,
  surfaceRaised = NeuDarkRaised,
  surfaceInset = NeuDarkInset,
  lightShadow = NeuDarkShadowLight,
  darkShadow = NeuDarkShadowDark,
  bevelHighlight = NeuDarkBevel,
  primaryAccent = Color(0xFF34D399),
  secondaryAccent = Color(0xFF38BDF8),
  textPrimary = Color(0xFFF1F5F9),
  textSecondary = Color(0xFF94A3B8),
  hairlineDivider = Color(0xFF383F49).copy(alpha = 0.70f)
)

val LocalNeumorphicColors = compositionLocalOf { MintNeumorphicColors }

enum class AppThemeMode(
  val id: String,
  val displayName: String,
  val shortBadge: String,
  val subtitle: String
) {
  GREEN_EMERALD(
    id = "green_emerald",
    displayName = "Mint Soft UI",
    shortBadge = "MINT NEU",
    subtitle = "Neumorphic Sage Mint & Emerald"
  ),
  WHITE_CLINICAL(
    id = "white_clinical",
    displayName = "Soft Clay Light",
    shortBadge = "CLAY LIGHT",
    subtitle = "Classic ui-neumorphism Daylight Clay"
  ),
  DARK_OBSIDIAN(
    id = "dark_obsidian",
    displayName = "Dark Neumorphic",
    shortBadge = "DARK #292E35",
    subtitle = "ui-neumorphism Slate Charcoal"
  );

  companion object {
    fun fromId(id: String?): AppThemeMode {
      return entries.firstOrNull { it.id == id } ?: GREEN_EMERALD
    }
  }
}

object AppThemeController {
  private const val PREFS_NAME = "hod_profile_settings_prefs"
  private const val KEY_APP_THEME_MODE = "app_theme_mode"

  private val _themeMode = MutableStateFlow(AppThemeMode.GREEN_EMERALD)
  val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()
  private var initialized = false

  fun initialize(context: Context) {
    if (!initialized) {
      val savedId = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_APP_THEME_MODE, AppThemeMode.GREEN_EMERALD.id)
      _themeMode.value = AppThemeMode.fromId(savedId)
      initialized = true
    }
  }

  fun getThemeMode(context: Context): AppThemeMode {
    initialize(context)
    return _themeMode.value
  }

  fun setThemeMode(context: Context, mode: AppThemeMode) {
    _themeMode.value = mode
    initialized = true
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putString(KEY_APP_THEME_MODE, mode.id)
      .apply()
  }
}

val LocalAppThemeMode = compositionLocalOf { AppThemeMode.GREEN_EMERALD }

// 1. MINT SOFT UI THEME (Single-Hue Botanical Sage-Mint Neumorphism)
private val GreenEmeraldColorScheme = lightColorScheme(
  primary = Color(0xFF047857),
  onPrimary = Color.White,
  primaryContainer = Color(0xFFC2E5D6),
  onPrimaryContainer = Color(0xFF062C22),
  secondary = EmeraldOptimal,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFD0E5DC),
  onSecondaryContainer = Color(0xFF064E3B),
  tertiary = Color(0xFF0E7490),
  background = NeuMintBase,
  onBackground = Color(0xFF062C22),
  surface = NeuMintRaised,
  onSurface = Color(0xFF062C22),
  surfaceVariant = NeuMintInset,
  onSurfaceVariant = Color(0xFF28584B),
  outline = NeuMintShadowDark,
  outlineVariant = Color(0xFFB5CDC2)
)

// 2. CLASSIC SOFT CLAY LIGHT THEME (Single-Hue #E4EBF5 ui-neumorphism Daylight)
private val WhiteClinicalColorScheme = lightColorScheme(
  primary = Color(0xFF0284C7),
  onPrimary = Color.White,
  primaryContainer = Color(0xFFD2E4F6),
  onPrimaryContainer = Color(0xFF0C4A6E),
  secondary = EmeraldOptimal,
  onSecondary = Color.White,
  secondaryContainer = NeuLightInset,
  onSecondaryContainer = Color(0xFF1E293B),
  tertiary = Color(0xFFD97706),
  background = NeuLightBase,
  onBackground = Color(0xFF1E293B),
  surface = NeuLightRaised,
  onSurface = Color(0xFF1E293B),
  surfaceVariant = NeuLightInset,
  onSurfaceVariant = Color(0xFF475569),
  outline = NeuLightShadowDark,
  outlineVariant = Color(0xFFC7D2E3)
)

// 3. DARK NEUMORPHIC THEME (Classic ui-neumorphism #292E35 Slate Charcoal)
private val DarkObsidianColorScheme = darkColorScheme(
  primary = Color(0xFF34D399),
  onPrimary = Color(0xFF0B1F1A),
  primaryContainer = Color(0xFF1E3A34),
  onPrimaryContainer = Color(0xFFD1FAE5),
  secondary = Color(0xFF38BDF8),
  onSecondary = Color(0xFF0C2334),
  secondaryContainer = Color(0xFF22272D),
  onSecondaryContainer = Color(0xFFE0F2FE),
  tertiary = Color(0xFFFBBF24),
  background = NeuDarkBase,
  onBackground = Color(0xFFF1F5F9),
  surface = NeuDarkRaised,
  onSurface = Color(0xFFF1F5F9),
  surfaceVariant = NeuDarkInset,
  onSurfaceVariant = Color(0xFF94A3B8),
  outline = NeuDarkShadowLight,
  outlineVariant = NeuDarkShadowDark
)

val IosSquircleShapes = Shapes(
  extraSmall = IosSharpMicroShape,
  small = IosSharpButtonShape,
  medium = IosSharpWellShape,
  large = IosSharpCardShape,
  extraLarge = IosSharpHeroShape
)

@Composable
fun MyApplicationTheme(
  overrideThemeMode: AppThemeMode? = null,
  content: @Composable () -> Unit
) {
  val context = LocalContext.current
  LaunchedEffect(context) {
    AppThemeController.initialize(context)
  }
  val observedMode by AppThemeController.themeMode.collectAsState()
  val activeMode = overrideThemeMode ?: observedMode

  val colorScheme = when (activeMode) {
    AppThemeMode.GREEN_EMERALD -> GreenEmeraldColorScheme
    AppThemeMode.WHITE_CLINICAL -> WhiteClinicalColorScheme
    AppThemeMode.DARK_OBSIDIAN -> DarkObsidianColorScheme
  }

  val neuColors = when (activeMode) {
    AppThemeMode.GREEN_EMERALD -> MintNeumorphicColors
    AppThemeMode.WHITE_CLINICAL -> LightClayNeumorphicColors
    AppThemeMode.DARK_OBSIDIAN -> DarkNeumorphicColors
  }

  CompositionLocalProvider(
    LocalAppThemeMode provides activeMode,
    LocalNeumorphicColors provides neuColors
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      shapes = IosSquircleShapes,
      content = content
    )
  }
}
