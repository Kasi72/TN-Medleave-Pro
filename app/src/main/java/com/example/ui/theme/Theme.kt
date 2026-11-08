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
 * Mathematical Apple iOS Continuous Superellipse Squircle Shape with crisp, sharper edge geometry.
 * Extends CornerBasedShape so it integrates natively with MaterialTheme.shapes as well as Modifier.clip/border.
 */
class IosSharpSquircleShape(
  topStart: CornerSize,
  topEnd: CornerSize,
  bottomEnd: CornerSize,
  bottomStart: CornerSize,
  private val sharpnessBias: Float = 0.82f
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
    sharpnessBias: Float = 0.82f
  ) : this(
    topStart = CornerSize(topStart),
    topEnd = CornerSize(topEnd),
    bottomEnd = CornerSize(bottomEnd),
    bottomStart = CornerSize(bottomStart),
    sharpnessBias = sharpnessBias
  )

  constructor(cornerRadius: Dp, sharpnessBias: Float = 0.82f) : this(
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

    val k = sharpnessBias.coerceIn(0.65f, 0.90f)

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

// Global Sharp-Edged iOS Squircle Presets
val IosSharpMicroShape = IosSharpSquircleShape(4.dp, sharpnessBias = 0.84f)
val IosSharpBadgeShape = IosSharpSquircleShape(5.dp, sharpnessBias = 0.84f)
val IosSharpButtonShape = IosSharpSquircleShape(6.dp, sharpnessBias = 0.84f)
val IosSharpTabShape = IosSharpSquircleShape(7.dp, sharpnessBias = 0.84f)
val IosSharpWellShape = IosSharpSquircleShape(8.dp, sharpnessBias = 0.82f)
val IosSharpCardShape = IosSharpSquircleShape(10.dp, sharpnessBias = 0.82f)
val IosSharpHeroShape = IosSharpSquircleShape(12.dp, sharpnessBias = 0.80f)

enum class AppThemeMode(
  val id: String,
  val displayName: String,
  val shortBadge: String,
  val subtitle: String
) {
  GREEN_EMERALD(
    id = "green_emerald",
    displayName = "Emerald Green",
    shortBadge = "GREEN THEME",
    subtitle = "Botanical Mint & Clinical Emerald"
  ),
  WHITE_CLINICAL(
    id = "white_clinical",
    displayName = "Clinical White",
    shortBadge = "WHITE THEME",
    subtitle = "Pure Daylight Paper & Sapphire"
  ),
  DARK_OBSIDIAN(
    id = "dark_obsidian",
    displayName = "Obsidian Dark",
    shortBadge = "DARK THEME",
    subtitle = "OLED Midnight & Neon Aqua"
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

// 1. EMERALD GREEN THEME (Signature Clinical Mint & Jade Palette)
private val GreenEmeraldColorScheme = lightColorScheme(
  primary = Color(0xFF047857),
  onPrimary = Color.White,
  primaryContainer = Color(0xFFD1FAE5),
  onPrimaryContainer = Color(0xFF064E3B),
  secondary = EmeraldOptimal,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFA7F3D0),
  onSecondaryContainer = Color(0xFF064E3B),
  tertiary = Color(0xFF0891B2),
  background = Color(0xFFECFDF5),
  onBackground = Color(0xFF052E16),
  surface = Color(0xFFF6FEF9),
  onSurface = Color(0xFF064E3B),
  surfaceVariant = Color(0xFFD1FAE5),
  onSurfaceVariant = Color(0xFF047857),
  outline = Color(0xFF6EE7B7),
  outlineVariant = Color(0xFFA7F3D0)
)

// 2. CLINICAL WHITE THEME (Pure Daylight Alabaster & iOS Grouped Paper Palette)
private val WhiteClinicalColorScheme = lightColorScheme(
  primary = SapphirePrimary,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFE0F2FE),
  onPrimaryContainer = SapphirePrimary,
  secondary = EmeraldOptimal,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFF1F5F9),
  onSecondaryContainer = Color(0xFF0F172A),
  tertiary = Color(0xFFD97706),
  background = Color(0xFFF2F2F7), // Authentic iOS systemGroupedBackground
  onBackground = Color(0xFF0F172A),
  surface = Color(0xFFFFFFFF),
  onSurface = Color(0xFF0F172A),
  surfaceVariant = Color(0xFFE5E5EA), // Authentic iOS systemFill
  onSurfaceVariant = Color(0xFF3C3C43),
  outline = Color(0xFFC6C6C8),
  outlineVariant = Color(0xFFD1D1D6)
)

// 3. OBSIDIAN DARK THEME (OLED Midnight Sapphire & Neon Aqua Palette)
private val DarkObsidianColorScheme = darkColorScheme(
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
  onSurfaceVariant = Color(0xFF94A3B8),
  outline = Color(0xFF334155),
  outlineVariant = Color(0xFF1E293B)
)

// Sharp-Edged iOS Continuous Squircle Shapes applied globally to MaterialTheme
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

  CompositionLocalProvider(LocalAppThemeMode provides activeMode) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      shapes = IosSquircleShapes,
      content = content
    )
  }
}
