package com.example.dinesplit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

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
    surfaceTint = BrandSurfaceTint,
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
    tertiaryContainer = BrandDarkTertiaryContainer,
    onTertiaryContainer = BrandDarkOnTertiaryContainer,
    background = BrandDarkBackground,
    onBackground = BrandDarkOnBackground,
    surface = BrandDarkSurface,
    onSurface = BrandDarkOnSurface,
    surfaceDim = BrandDarkSurfaceDim,
    surfaceBright = BrandDarkSurfaceBright,
    surfaceContainerLowest = BrandDarkSurfaceContainerLowest,
    surfaceContainerLow = BrandDarkSurfaceContainerLow,
    surfaceContainer = BrandDarkSurfaceContainer,
    surfaceContainerHigh = BrandDarkSurfaceContainerHigh,
    surfaceContainerHighest = BrandDarkSurfaceContainerHighest,
    surfaceVariant = BrandDarkSurfaceVariant,
    onSurfaceVariant = BrandDarkOnSurfaceVariant,
    outline = BrandDarkOutline,
    outlineVariant = BrandDarkOutlineVariant,
    inverseSurface = BrandDarkInverseSurface,
    inverseOnSurface = BrandDarkInverseOnSurface,
    inversePrimary = BrandDarkInversePrimary,
    error = BrandDarkError,
    onError = BrandDarkOnError,
    errorContainer = BrandDarkErrorContainer,
    onErrorContainer = BrandDarkOnErrorContainer
)

@Composable
fun DineSplitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
