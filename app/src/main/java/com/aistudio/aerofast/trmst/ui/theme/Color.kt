package com.aistudio.aerofast.trmst.ui.theme

import androidx.compose.ui.graphics.Color

enum class AppThemeMode(val displayName: String, val icon: String) {
    INDUSTRIAL_LIGHT("Bright Industrial", "☀️"),
    BLUEPRINT_CAD("Blueprint CAD", "📐"),
    MACHINIST_GOLD("Machinist Gold", "⚙️"),
    CYBER_EMERALD("Cyber Emerald", "🟩"),
    AERO_TITANIUM("Aero Titanium", "✈️")
}

// Industrial Light
val LightBgMain = Color(0xFFF1F5F9)
val LightBgCard = Color(0xFFFFFFFF)
val LightBgElevated = Color(0xFFF8FAFC)
val LightTextMain = Color(0xFF0F172A)
val LightTextMuted = Color(0xFF475569)
val LightBorder = Color(0xFFCBD5E1)
val LightAccentPrimary = Color(0xFF0284C7)
val LightAccentSecondary = Color(0xFFD97706)

// Blueprint CAD
val CadBgMain = Color(0xFF0A192F)
val CadBgCard = Color(0xFF112240)
val CadBgElevated = Color(0xFF1D3557)
val CadTextMain = Color(0xFFE6F1FF)
val CadTextMuted = Color(0xFF8892B0)
val CadBorder = Color(0xFF233554)
val CadAccentPrimary = Color(0xFF00B4D8)
val CadAccentSecondary = Color(0xFFFFB703)

// Machinist Gold
val GoldBgMain = Color(0xFF18181B)
val GoldBgCard = Color(0xFF27272A)
val GoldBgElevated = Color(0xFF3F3F46)
val GoldTextMain = Color(0xFFFAFAFA)
val GoldTextMuted = Color(0xFFA1A1AA)
val GoldBorder = Color(0xFF52525B)
val GoldAccentPrimary = Color(0xFFF59E0B)
val GoldAccentSecondary = Color(0xFF38BDF8)

// Cyber Emerald
val EmeraldBgMain = Color(0xFF05100A)
val EmeraldBgCard = Color(0xFF0B1F15)
val EmeraldBgElevated = Color(0xFF123122)
val EmeraldTextMain = Color(0xFFECFDF5)
val EmeraldTextMuted = Color(0xFF6EE7B7)
val EmeraldBorder = Color(0xFF065F46)
val EmeraldAccentPrimary = Color(0xFF10B981)
val EmeraldAccentSecondary = Color(0xFFFBBF24)

// Aero Titanium
val TitaniumBgMain = Color(0xFF0B0F19)
val TitaniumBgCard = Color(0xFF111827)
val TitaniumBgElevated = Color(0xFF1E293B)
val TitaniumTextMain = Color(0xFFF8FAFC)
val TitaniumTextMuted = Color(0xFF94A3B8)
val TitaniumBorder = Color(0xFF374151)
val TitaniumAccentPrimary = Color(0xFF06B6D4)
val TitaniumAccentSecondary = Color(0xFFF59E0B)

// Shared Alert colors
val AlertRed = Color(0xFFDC2626)
val AlertRedBg = Color(0xFFE11D48)
val SuccessGreen = Color(0xFF059669)
val WarningAmber = Color(0xFFD97706)
