package com.timshubet.hubitatdashboard.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Visual design tokens for dashboard tiles. Mirrors the Tailwind palette used
 * by the web dashboard so the Android tiles look and behave identically.
 */
object TileTokens {
    // --- On / off / status colors (match web) ---
    val GreenOn       = Color(0xFF22C55E)   // green-500  — pill bg when on
    val GreenBorder   = Color(0xFF4ADE80)   // green-400  — card border when on
    val GreenComfort  = Color(0xFF22C55E)   // temp 65-80°F
    val BlueCold      = Color(0xFF3B82F6)   // blue-500   — cold temp / selected mode chip
    val BlueBorder    = Color(0xFF93C5FD)   // blue-300
    val OrangeHot     = Color(0xFFF97316)   // orange-500 — hot temp
    val OrangeBorder  = Color(0xFFFED7AA)   // orange-300
    val AmberActive   = Color(0xFFFFC107)   // amber/yellow — dimmer, motion, power active
    val RedAlert      = Color(0xFFF44336)   // red-500    — unlocked, armed-away, critical battery
    val PurpleRgb     = Color(0xFF9C27B0)   // purple-500 — RGBW on

    val OffBorder        = Color(0xFF6B7280)   // gray-500  — visible border when switch is OFF
    val MultiPanelBorder = Color(0xFF6366F1)   // indigo-500 — multi-device panel card border

    // --- Neutrals (pill off / muted title / skeleton) ---
    val PillOffBgLight   = Color(0xFFE5E7EB) // gray-200
    val PillOffBgDark    = Color(0xFF374151) // gray-700
    val PillOffTextLight = Color(0xFF374151) // gray-700
    val PillOffTextDark  = Color(0xFFD1D5DB) // gray-300
    val TitleMuted       = Color(0xFF9CA3AF) // gray-400 — tile title
    val SkeletonLight    = Color(0xFFE5E7EB)
    val SkeletonDark     = Color(0xFF374151)

    // --- Shape & spacing ---
    val TileCornerRadius = 12.dp
    val TilePadding      = 16.dp
    val TileMinHeight    = 104.dp
    val PillRadius       = 8.dp
    val PillHeight       = 36.dp
    val IconSize         = 18.dp
    val GridGap          = 12.dp
    val GridMinTile      = 160.dp
}
