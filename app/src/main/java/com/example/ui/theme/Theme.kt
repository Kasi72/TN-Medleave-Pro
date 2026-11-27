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

/**
 * Standardized 7-role functional button taxonomy so buttons doing the same job
 * always share the exact same signature jewel-tone color across every screen.
 */
enum class ActionButtonRole {
  /** Create / Log Leave / Add Duty / Save / Commit / Apply Plan — Royal Sky Blue */
  CREATE_PRIMARY,
  /** Verify / Approve / Advance Stage / Claim +1 COL / Pair COL ↔ GH — Clinical Emerald */
  VERIFY_APPROVE,
  /** AI Intelligence / Gemini Optimizer / Smart Bridges / Ask Advisor — Royal Indigo-Violet */
  AI_OPTIMIZE,
  /** Export PDF / Official Joining Report / Google Calendar Sync / .ics — Warm Amber-Gold */
  EXPORT_DOCS,
  /** Share / WhatsApp Handover / Copy Memo / Attach Slip Photo — Deep Clinical Teal */
  SHARE_HANDOVER,
  /** Edit / Filter / Sync TN Holidays / Configure / Utility — Crisp Cobalt Blue */
  EDIT_UTILITY,
  /** Delete / Clear / Cancel / Remove / Urgent Expiry Warning — Crisp Crimson Red */
  DESTRUCTIVE_ALERT
}

@Immutable
data class FunctionalActionColors(
  val createPrimary: Color,
  val verifyApprove: Color,
  val aiOptimize: Color,
  val exportDocs: Color,
  val shareHandover: Color,
  val editUtility: Color,
  val destructiveAlert: Color
) {
  fun colorFor(role: ActionButtonRole): Color = when (role) {
    ActionButtonRole.CREATE_PRIMARY -> createPrimary
    ActionButtonRole.VERIFY_APPROVE -> verifyApprove
    ActionButtonRole.AI_OPTIMIZE -> aiOptimize
    ActionButtonRole.EXPORT_DOCS -> exportDocs
    ActionButtonRole.SHARE_HANDOVER -> shareHandover
    ActionButtonRole.EDIT_UTILITY -> editUtility
    ActionButtonRole.DESTRUCTIVE_ALERT -> destructiveAlert
  }
}

private val LightFunctionalActionColors = FunctionalActionColors(
  createPrimary = ActionCreateLight,
  verifyApprove = ActionVerifyLight,
  aiOptimize = ActionAiLight,
  exportDocs = ActionExportLight,
  shareHandover = ActionShareLight,
  editUtility = ActionEditLight,
  destructiveAlert = ActionDestructiveLight
)

private val DarkFunctionalActionColors = FunctionalActionColors(
  createPrimary = ActionCreateDark,
  verifyApprove = ActionVerifyDark,
  aiOptimize = ActionAiDark,
  exportDocs = ActionExportDark,
  shareHandover = ActionShareDark,
  editUtility = ActionEditDark,
  destructiveAlert = ActionDestructiveDark
)

val LocalFunctionalActionColors = compositionLocalOf { LightFunctionalActionColors }

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

private val SkyWhiteNeumorphicColors = NeumorphicColors(
  isDark = false,
  baseBg = NeuSkyBase,
  surfaceRaised = NeuSkyRaised,
  surfaceInset = NeuSkyInset,
  lightShadow = NeuSkyShadowLight,
  darkShadow = NeuSkyShadowDark,
  bevelHighlight = NeuSkyBevel,
  primaryAccent = SkyPrimary,
  secondaryAccent = ActionShareLight,
  textPrimary = Color(0xFF0F2942),
  textSecondary = Color(0xFF476582),
  hairlineDivider = Color(0xFFB5D2EE).copy(alpha = 0.65f)
)

private val CloudWhiteNeumorphicColors = NeumorphicColors(
  isDark = false,
  baseBg = NeuLightBase,
  surfaceRaised = NeuLightRaised,
  surfaceInset = NeuLightInset,
  lightShadow = NeuLightShadowLight,
  darkShadow = NeuLightShadowDark,
  bevelHighlight = NeuLightBevel,
  primaryAccent = SkyPrimary,
  secondaryAccent = ActionShareLight,
  textPrimary = Color(0xFF0F2942),
  textSecondary = Color(0xFF526A84),
  hairlineDivider = Color(0xFFC2D2E6).copy(alpha = 0.65f)
)

private val DarkNeumorphicColors = NeumorphicColors(
  isDark = true,
  baseBg = NeuDarkBase,
  surfaceRaised = NeuDarkRaised,
  surfaceInset = NeuDarkInset,
  lightShadow = NeuDarkShadowLight,
  darkShadow = NeuDarkShadowDark,
  bevelHighlight = NeuDarkBevel,
  primaryAccent = SkyPrimaryBright,
  secondaryAccent = ActionShareDark,
  textPrimary = Color(0xFFF1F5F9),
  textSecondary = Color(0xFF94A3B8),
  hairlineDivider = Color(0xFF333F52).copy(alpha = 0.72f)
)

val LocalNeumorphicColors = compositionLocalOf { SkyWhiteNeumorphicColors }

enum class AppThemeMode(
  val id: String,
  val displayName: String,
  val shortBadge: String,
  val subtitle: String
) {
  SKY_WHITE(
    id = "sky_white",
    displayName = "Sky Blue & White",
    shortBadge = "SKY WHITE",
    subtitle = "Airy Light Sky-Blue Canvas & Crisp White Neumorphism"
  ),
  WHITE_CLINICAL(
    id = "white_clinical",
    displayName = "Pure White Cloud",
    shortBadge = "CLOUD WHITE",
    subtitle = "Ultra-Crisp Porcelain White Daylight Soft UI"
  ),
  DARK_OBSIDIAN(
    id = "dark_obsidian",
    displayName = "Dark Sky Obsidian",
    shortBadge = "DARK SKY",
    subtitle = "Midnight Slate Neumorphism with Luminous Sky Accent"
  );

  companion object {
    fun fromId(id: String?): AppThemeMode {
      // Automatically migrate legacy saved "green_emerald" preference to the new Sky Blue & White default
      if (id == null || id == "green_emerald") return SKY_WHITE
      return entries.firstOrNull { it.id == id } ?: SKY_WHITE
    }
  }
}

object AppThemeController {
  private const val PREFS_NAME = "hod_profile_settings_prefs"
  private const val KEY_APP_THEME_MODE = "app_theme_mode"

  private val _themeMode = MutableStateFlow(AppThemeMode.SKY_WHITE)
  val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()
  private var initialized = false

  fun initialize(context: Context) {
    if (!initialized) {
      val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      val savedId = prefs.getString(KEY_APP_THEME_MODE, AppThemeMode.SKY_WHITE.id)
      val resolved = AppThemeMode.fromId(savedId)
      _themeMode.value = resolved
      if (savedId == "green_emerald") {
        prefs.edit().putString(KEY_APP_THEME_MODE, AppThemeMode.SKY_WHITE.id).apply()
      }
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

val LocalAppThemeMode = compositionLocalOf { AppThemeMode.SKY_WHITE }

// 1. AIRY SKY BLUE & CRISP WHITE THEME (New Signature Default)
private val SkyWhiteColorScheme = lightColorScheme(
  primary = SkyPrimary,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFD2EAFE),
  onPrimaryContainer = Color(0xFF0C4A6E),
  secondary = ActionShareLight,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFCCFBF1),
  onSecondaryContainer = Color(0xFF115E59),
  tertiary = ActionAiLight,
  onTertiary = Color.White,
  background = NeuSkyBase,
  onBackground = Color(0xFF0F2942),
  surface = NeuSkyRaised,
  onSurface = Color(0xFF0F2942),
  surfaceVariant = NeuSkyInset,
  onSurfaceVariant = Color(0xFF476582),
  outline = NeuSkyShadowDark,
  outlineVariant = Color(0xFFC5DDF4),
  error = ActionDestructiveLight,
  onError = Color.White
)

// 2. PURE WHITE CLOUD LIGHT THEME
private val WhiteClinicalColorScheme = lightColorScheme(
  primary = SkyPrimary,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFD8ECFD),
  onPrimaryContainer = Color(0xFF0C4A6E),
  secondary = ActionShareLight,
  onSecondary = Color.White,
  secondaryContainer = NeuLightInset,
  onSecondaryContainer = Color(0xFF0F2942),
  tertiary = ActionAiLight,
  onTertiary = Color.White,
  background = NeuLightBase,
  onBackground = Color(0xFF0F2942),
  surface = NeuLightRaised,
  onSurface = Color(0xFF0F2942),
  surfaceVariant = NeuLightInset,
  onSurfaceVariant = Color(0xFF526A84),
  outline = NeuLightShadowDark,
  outlineVariant = Color(0xFFD2DFEE),
  error = ActionDestructiveLight,
  onError = Color.White
)

// 3. DARK SKY OBSIDIAN NEUMORPHIC THEME
private val DarkObsidianColorScheme = darkColorScheme(
  primary = SkyPrimaryBright,
  onPrimary = Color(0xFF082032),
  primaryContainer = Color(0xFF133652),
  onPrimaryContainer = Color(0xFFE0F2FE),
  secondary = ActionShareDark,
  onSecondary = Color(0xFF042F2E),
  secondaryContainer = Color(0xFF1A2533),
  onSecondaryContainer = Color(0xFFCCFBF1),
  tertiary = ActionAiDark,
  onTertiary = Color.White,
  background = NeuDarkBase,
  onBackground = Color(0xFFF1F5F9),
  surface = NeuDarkRaised,
  onSurface = Color(0xFFF1F5F9),
  surfaceVariant = NeuDarkInset,
  onSurfaceVariant = Color(0xFF94A3B8),
  outline = NeuDarkShadowLight,
  outlineVariant = NeuDarkShadowDark,
  error = ActionDestructiveDark,
  onError = Color.White
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
    AppThemeMode.SKY_WHITE -> SkyWhiteColorScheme
    AppThemeMode.WHITE_CLINICAL -> WhiteClinicalColorScheme
    AppThemeMode.DARK_OBSIDIAN -> DarkObsidianColorScheme
  }

  val neuColors = when (activeMode) {
    AppThemeMode.SKY_WHITE -> SkyWhiteNeumorphicColors
    AppThemeMode.WHITE_CLINICAL -> CloudWhiteNeumorphicColors
    AppThemeMode.DARK_OBSIDIAN -> DarkNeumorphicColors
  }

  val actionColors = if (neuColors.isDark) DarkFunctionalActionColors else LightFunctionalActionColors

  CompositionLocalProvider(
    LocalAppThemeMode provides activeMode,
    LocalNeumorphicColors provides neuColors,
    LocalFunctionalActionColors provides actionColors
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      shapes = IosSquircleShapes,
      content = content
    )
  }
}
