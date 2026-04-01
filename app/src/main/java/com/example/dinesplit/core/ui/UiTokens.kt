package com.example.dinesplit.core.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object AppDimens {
    val spaceXs = 4.dp
    val spaceSm = 8.dp
    val spaceMd = 12.dp
    val spaceLg = 16.dp
    val spaceXl = 24.dp
    val space2Xl = 32.dp

    val screenHorizontal = 16.dp
    val screenVertical = 16.dp

    val radiusSm = 8.dp
    val radiusMd = 12.dp
    val radiusLg = 16.dp

    val buttonHeight = 52.dp
    val textFieldMinHeight = 56.dp
    val cardElevation = 2.dp
}

object AppShapes {
    val small = RoundedCornerShape(AppDimens.radiusSm)
    val medium = RoundedCornerShape(AppDimens.radiusMd)
    val large = RoundedCornerShape(AppDimens.radiusLg)
}
