package com.example.dinesplit.core.ui

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Editorial Design Tokens for DineSplit
 * Centralizing these values avoids hardcoding across the project.
 */
object AppDimens {
    // Spacing
    val spaceXs = 4.dp
    val spaceSm = 8.dp
    val spaceMd = 12.dp
    val spaceLg = 16.dp
    val spaceXl = 24.dp
    val space2Xl = 32.dp
    val space3Xl = 48.dp
    val space4Xl = 64.dp
    
    // Corners
    val cornerSmall = 8.dp
    val cornerMedium = 12.dp
    val cornerLarge = 24.dp
    val cornerExtraLarge = 32.dp
    
    // Elevation
    val elevLow = 2.dp
    val elevMedium = 8.dp
    val elevHigh = 16.dp
    val elevEditorial = 24.dp
    
    // Icon Sizes
    val iconSm = 16.dp
    val iconMd = 24.dp
    val iconLg = 32.dp
    val iconXl = 48.dp
    
    // Specific components
    val topBarHeight = 64.dp
    val buttonHeight = 56.dp
    val avatarSm = 40.dp
    val avatarMd = 80.dp
    val avatarLg = 128.dp
}

object AppAnimation {
    val durationShort = 300
    val durationMedium = 500
    val durationLong = 1500
}

object AppFontSizes {
    val displayLarge = 60.sp
    val headlineLarge = 40.sp
    val labelSmall = 10.sp
    val caption = 11.sp
}
