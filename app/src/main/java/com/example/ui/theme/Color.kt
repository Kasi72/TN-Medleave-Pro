package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// UI-NEUMORPHISM INSPIRED SOFT-UI PALETTE TOKENS (LIGHT CLAY, MINT, DARK #292E35)
// ============================================================================

// 1. Classic Soft Clay Light (--light-bg style)
val NeuLightBase = Color(0xFFE4EBF5)
val NeuLightRaised = Color(0xFFE4EBF5)
val NeuLightInset = Color(0xFFD9E2EF)
val NeuLightShadowLight = Color(0xFFFFFFFF)
val NeuLightShadowDark = Color(0xFFB8C4D8)
val NeuLightBevel = Color(0xFFF9FBFF)

// 2. Botanical Mint Soft UI (Default Signature Theme)
val NeuMintBase = Color(0xFFDCEEE6)
val NeuMintRaised = Color(0xFFDCEEE6)
val NeuMintInset = Color(0xFFD0E5DC)
val NeuMintShadowLight = Color(0xFFF5FFFA)
val NeuMintShadowDark = Color(0xFFA9C0B6)
val NeuMintBevel = Color(0xFFECFFF6)

// 3. Classic Dark Neumorphism (--dark-bg: #292E35 from ui-neumorphism)
val NeuDarkBase = Color(0xFF292E35)
val NeuDarkRaised = Color(0xFF292E35)
val NeuDarkInset = Color(0xFF22272D)
val NeuDarkShadowLight = Color(0xFF383F49)
val NeuDarkShadowDark = Color(0xFF1A1D22)
val NeuDarkBevel = Color(0xFF343B44)

// Backward-compatible semantic aliases
val IosGroupedBgLight = NeuLightBase
val IosSurfaceCardLight = NeuLightRaised
val IosElevatedGlassLight = NeuLightInset

val IosGroupedBgDark = NeuDarkBase
val IosSurfaceCardDark = NeuDarkRaised
val IosElevatedGlassDark = NeuDarkInset

val SapphirePrimary = Color(0xFF0A2540)
val SapphireAccent = Color(0xFF0284C7)
val EmeraldOptimal = Color(0xFF059669)
val EmeraldSoftBg = Color(0xFFD1FAE5)

// Spreadsheet Category Authentic Colors (refined for high contrast on Soft-UI surfaces)
val BadgeClGreen = Color(0xFF15803D)
val BadgeColCyan = Color(0xFF0E7490)
val BadgeInLieuLavender = Color(0xFF7C3AED)
val BadgePlGold = Color(0xFFB45309)
val BadgeGhLime = Color(0xFF4D7C0F)
val BadgeMlOrange = Color(0xFFC2410C)
val BadgeRhSlate = Color(0xFF334155)
val BadgeElNeutral = Color(0xFF475569)
val BadgeOnDutyBlue = Color(0xFF1D4ED8)
val BadgeWeekOffTeal = Color(0xFF0F766E)

val StatusAlertRed = Color(0xFFDC2626)
val StatusVerifiedGreen = Color(0xFF15803D)
val StatusPendingOrange = Color(0xFFD97706)

// High-Contrast Accent Tokens
val NeonCyanText = Color(0xFF0EA5E9)
val NeonCyanBright = Color(0xFF38BDF8)
val NeonAquaAccent = Color(0xFF10B981)
val IosDarkDockSurface = NeuDarkRaised
val IosSquircleGlassFill = NeuDarkInset
