package com.example.dinesplit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BrandPrimary,
    onPrimary = BrandOnPrimary,
    primaryContainer = Color(0xFF1D356B),
    onPrimaryContainer = Color(0xFFDCE6FF),
    secondary = Color(0xFF78D9B7),
    onSecondary = BrandOnSecondary,
    secondaryContainer = Color(0xFF0E4F3B),
    tertiary = Color(0xFFFFB4A2),
    onTertiary = BrandOnTertiary,
    background = Color(0xFF111722),
    onBackground = Color(0xFFE3E8F5),
    surface = Color(0xFF1A2331),
    onSurface = Color(0xFFE3E8F5),
    surfaceVariant = Color(0xFF2E3A4B),
    onSurfaceVariant = Color(0xFFBFC8D9),
    outline = Color(0xFF97A0B3),
    error = BrandError,
    onError = BrandOnError,
    errorContainer = BrandErrorContainer
)

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = BrandOnPrimary,
    primaryContainer = BrandPrimaryContainer,
    onPrimaryContainer = BrandOnPrimaryContainer,
    secondary = BrandSecondary,
    onSecondary = BrandOnSecondary,
    secondaryContainer = BrandSecondaryContainer,
    tertiary = BrandTertiary,
    onTertiary = BrandOnTertiary,
    background = BrandBackground,
    onBackground = BrandOnSurface,
    surface = BrandSurface,
    onSurface = BrandOnSurface,
    surfaceVariant = BrandSurfaceVariant,
    onSurfaceVariant = BrandOnSurfaceVariant,
    outline = BrandOutline,
    error = BrandError,
    onError = BrandOnError,
    errorContainer = BrandErrorContainer
)

@Composable
fun DineSplitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}