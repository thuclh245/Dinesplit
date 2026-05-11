package com.example.dinesplit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BrandDarkPrimary,
    onPrimary = BrandDarkOnPrimary,
    primaryContainer = BrandDarkPrimaryContainer,
    onPrimaryContainer = BrandDarkOnPrimaryContainer,
    secondary = BrandDarkSecondary,
    onSecondary = BrandDarkOnSecondary,
    secondaryContainer = BrandDarkSecondaryContainer,
    onSecondaryContainer = BrandDarkOnSecondaryContainer,
    tertiary = BrandDarkTertiary,
    onTertiary = BrandDarkOnTertiary,
    background = BrandDarkBackground,
    onBackground = BrandDarkOnBackground,
    surface = BrandDarkSurface,
    onSurface = BrandDarkOnSurface,
    surfaceVariant = BrandDarkSurfaceVariant,
    onSurfaceVariant = BrandDarkOnSurfaceVariant,
    surfaceContainerLowest = Color(0xFF121927),
    surfaceContainerLow = Color(0xFF182131),
    surfaceContainer = Color(0xFF1D2838),
    surfaceContainerHigh = Color(0xFF263141),
    surfaceContainerHighest = Color(0xFF2E3A4B),
    outline = BrandDarkOutline,
    error = BrandDarkError,
    onError = BrandDarkOnError,
    errorContainer = BrandDarkErrorContainer,
    onErrorContainer = BrandOnErrorContainer,
    inverseSurface = BrandInverseSurface,
    inverseOnSurface = BrandInverseOnSurface,
    inversePrimary = BrandInversePrimary
)

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = BrandOnPrimary,
    primaryContainer = BrandPrimaryContainer,
    onPrimaryContainer = BrandOnPrimaryContainer,
    secondary = BrandSecondary,
    onSecondary = BrandOnSecondary,
    secondaryContainer = BrandSecondaryContainer,
    onSecondaryContainer = BrandOnSecondaryContainer,
    tertiary = BrandTertiary,
    onTertiary = BrandOnTertiary,
    tertiaryContainer = BrandTertiaryContainer,
    onTertiaryContainer = BrandOnTertiaryContainer,
    background = BrandBackground,
    onBackground = BrandOnSurface,
    surface = BrandSurface,
    onSurface = BrandOnSurface,
    surfaceDim = BrandSurfaceDim,
    surfaceBright = BrandSurfaceBright,
    surfaceContainerLowest = BrandSurfaceContainerLowest,
    surfaceContainerLow = BrandSurfaceContainerLow,
    surfaceContainer = BrandSurfaceContainer,
    surfaceContainerHigh = BrandSurfaceContainerHigh,
    surfaceContainerHighest = BrandSurfaceContainerHighest,
    surfaceVariant = BrandSurfaceVariant,
    onSurfaceVariant = BrandOnSurfaceVariant,
    outline = BrandOutline,
    outlineVariant = BrandOutlineVariant,
    inverseSurface = BrandInverseSurface,
    inverseOnSurface = BrandInverseOnSurface,
    inversePrimary = BrandInversePrimary,
    error = BrandError,
    onError = BrandOnError,
    errorContainer = BrandErrorContainer,
    onErrorContainer = BrandOnErrorContainer
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