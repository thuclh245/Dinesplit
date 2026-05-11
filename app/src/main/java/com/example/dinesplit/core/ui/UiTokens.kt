@file:Suppress("unused")

package com.example.dinesplit.core.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

@Suppress("unused")
object AppDimens {
    val spaceXs = 4.dp
    val spaceSm = 8.dp
    val spaceMd = 12.dp
    val spaceLg = 16.dp
    val spaceXl = 24.dp
    val space2Xl = 32.dp
    val space3Xl = 40.dp
    val space4Xl = 48.dp

    val screenHorizontal = 16.dp
    val screenVertical = 16.dp

    val radiusSm = 4.dp
    val radiusMd = 12.dp
    val radiusLg = 16.dp
    val radiusXl = 24.dp
    val radiusFull = 9999.dp
    val cornerMedium = 12.dp  // Backward compat alias

    val buttonHeight = 52.dp
    val textFieldMinHeight = 56.dp
    val cardElevation = 2.dp
    val elevEditorial = 4.dp  // Backward compat: editorial elevation

    val iconLg = 32.dp  // Backward compat alias for large icon size
}

@Suppress("unused")
object AppShapes {
    val medium = RoundedCornerShape(AppDimens.radiusMd)
    val large = RoundedCornerShape(AppDimens.radiusLg)
    val xLarge = RoundedCornerShape(AppDimens.radiusXl)
    val full = RoundedCornerShape(AppDimens.radiusFull)
}
