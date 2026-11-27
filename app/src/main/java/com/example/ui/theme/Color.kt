package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// SKY BLUE & CRISP WHITE NEUMORPHIC SOFT-UI PALETTE + 7-ROLE FUNCTIONAL BUTTONS
// ============================================================================

// 1. Airy Light Sky Blue & Crisp Porcelain White (Default Signature Theme)
val NeuSkyBase = Color(0xFFE6F2FE)          // Glacial Sky-Blue backdrop so pure-white highlights pop
val NeuSkyRaised = Color(0xFFFAFDFE)        // Crisp Porcelain White extruded Neumorphic cards
val NeuSkyInset = Color(0xFFDCECFD)         // Soft Ice-Sky carved inset wells & progress troughs
val NeuSkyShadowLight = Color(0xFFFFFFFF)   // Pure specular white top-left Neumorphic lift
val NeuSkyShadowDark = Color(0xFFB5D2EE)    // Cool sky-mist bottom-right Neumorphic depth cast
val NeuSkyBevel = Color(0xFFFFFFFF)         // Crisp white rim highlight

// 2. Pure White Cloud Light (Ultra-Crisp Secondary Light Theme)
val NeuLightBase = Color(0xFFF1F6FC)
val NeuLightRaised = Color(0xFFFFFFFF)
val NeuLightInset = Color(0xFFE6EFF9)
val NeuLightShadowLight = Color(0xFFFFFFFF)
val NeuLightShadowDark = Color(0xFFC2D2E6)
val NeuLightBevel = Color(0xFFFFFFFF)

// Legacy aliases routed to Sky Blue & White so any indirect reference stays harmonized
val NeuMintBase = NeuSkyBase
val NeuMintRaised = NeuSkyRaised
val NeuMintInset = NeuSkyInset
val NeuMintShadowLight = NeuSkyShadowLight
val NeuMintShadowDark = NeuSkyShadowDark
val NeuMintBevel = NeuSkyBevel

// 3. Dark Sky Obsidian Neumorphism (Refined Midnight Slate with Sky-Blue Accent)
val NeuDarkBase = Color(0xFF1D2430)
val NeuDarkRaised = Color(0xFF242D3B)
val NeuDarkInset = Color(0xFF161C26)
val NeuDarkShadowLight = Color(0xFF333F52)
val NeuDarkShadowDark = Color(0xFF0F141C)
val NeuDarkBevel = Color(0xFF313D50)

// Backward-compatible semantic aliases
val IosGroupedBgLight = NeuSkyBase
val IosSurfaceCardLight = NeuSkyRaised
val IosElevatedGlassLight = NeuSkyInset

val IosGroupedBgDark = NeuDarkBase
val IosSurfaceCardDark = NeuDarkRaised
val IosElevatedGlassDark = NeuDarkInset

// Core Brand & Clinical Sky Tokens
val SapphirePrimary = Color(0xFF0F2942)
val SapphireAccent = Color(0xFF0284C7)
val SkyPrimary = Color(0xFF0284C7)
val SkyPrimaryDeep = Color(0xFF0369A1)
val SkyPrimaryBright = Color(0xFF38BDF8)
val SkySoftBg = Color(0xFFE0F2FE)

// Semantic Verification / Healthy Status Emerald (reserved for Verify/Approve/Claim/+1 COL)
val EmeraldOptimal = Color(0xFF059669)
val EmeraldSoftBg = Color(0xFFD1FAE5)

// ============================================================================
// 7-ROLE FUNCTIONAL MULTI-COLOR BUTTON TOKENS (IDENTICAL FUNCTION = SAME COLOR)
// ============================================================================

// 1. CREATE / LOG / ADD / SAVE / APPLY — Royal Sky Blue
val ActionCreateLight = Color(0xFF0284C7)
val ActionCreateDark = Color(0xFF0EA5E9)

// 2. VERIFY / APPROVE / PAIR / CLAIM +1 COL — Clinical Emerald
val ActionVerifyLight = Color(0xFF059669)
val ActionVerifyDark = Color(0xFF10B981)

// 3. AI INTELLIGENCE / OPTIMIZE / SMART BRIDGES / ASK AI — Royal Indigo-Violet
val ActionAiLight = Color(0xFF4F46E5)
val ActionAiDark = Color(0xFF6366F1)

// 4. EXPORT / OFFICIAL PDF / JOINING REPORT / CALENDAR SYNC — Warm Amber-Gold
val ActionExportLight = Color(0xFFD97706)
val ActionExportDark = Color(0xFFF59E0B)

// 5. SHARE / WHATSAPP HANDOVER / COPY MEMO / SLIP PHOTO — Deep Clinical Teal
val ActionShareLight = Color(0xFF0D9488)
val ActionShareDark = Color(0xFF14B8A6)

// 6. EDIT / FILTER / SYNC HOLIDAYS / UTILITY — Crisp Cobalt Blue
val ActionEditLight = Color(0xFF2563EB)
val ActionEditDark = Color(0xFF3B82F6)

// 7. DELETE / CLEAR / CANCEL / URGENT ALERT — Crisp Crimson Red
val ActionDestructiveLight = Color(0xFFDC2626)
val ActionDestructiveDark = Color(0xFFEF4444)

// Spreadsheet Category Authentic Colors (refined for high contrast on White & Sky-Blue surfaces)
val BadgeClGreen = Color(0xFF0284C7)      // Harmonized Clinical Sky-Blue for CL primary leave
val BadgeColCyan = Color(0xFF0E7490)
val BadgeInLieuLavender = Color(0xFF6D28D9)
val BadgePlGold = Color(0xFFB45309)
val BadgeGhLime = Color(0xFF059669)       // Clinical Emerald for GH Worked / Earned Credit
val BadgeMlOrange = Color(0xFFC2410C)
val BadgeRhSlate = Color(0xFF334155)
val BadgeElNeutral = Color(0xFF475569)
val BadgeOnDutyBlue = Color(0xFF1D4ED8)
val BadgeWeekOffTeal = Color(0xFF0F766E)

val StatusAlertRed = Color(0xFFDC2626)
val StatusVerifiedGreen = Color(0xFF059669)
val StatusPendingOrange = Color(0xFFD97706)

// High-Contrast Accent Tokens
val NeonCyanText = Color(0xFF0284C7)
val NeonCyanBright = Color(0xFF38BDF8)
val NeonAquaAccent = Color(0xFF0EA5E9)
val IosDarkDockSurface = NeuDarkRaised
val IosSquircleGlassFill = NeuDarkInset
