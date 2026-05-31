package com.example.dinesplit.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Extended color tokens from the Stitch design system that are not part of
 * Material3's standard ColorScheme (e.g., fixed colors, dim variants).
 *
 * Access via `LocalExtendedColors.current` inside a DineSplitTheme.
 */
@Immutable
data class ExtendedColors(
    // Primary extended
    val primaryDim: Color,
    val primaryFixed: Color,
    val primaryFixedDim: Color,
    val onPrimaryFixed: Color,
    val onPrimaryFixedVariant: Color,
    // Secondary extended
    val secondaryDim: Color,
    val secondaryFixed: Color,
    val secondaryFixedDim: Color,
    val onSecondaryFixed: Color,
    val onSecondaryFixedVariant: Color,
    // Tertiary extended
    val tertiaryDim: Color,
    val tertiaryFixed: Color,
    val tertiaryFixedDim: Color,
    val onTertiaryFixed: Color,
    val onTertiaryFixedVariant: Color,
    // Error extended
    val errorDim: Color,
    // Surface extended
    val surfaceTint: Color,
)

val LightExtendedColors =
    ExtendedColors(
        primaryDim = BrandPrimaryDim,
        primaryFixed = BrandPrimaryFixed,
        primaryFixedDim = BrandPrimaryFixedDim,
        onPrimaryFixed = BrandOnPrimaryFixed,
        onPrimaryFixedVariant = BrandOnPrimaryFixedVariant,
        secondaryDim = BrandSecondaryDim,
        secondaryFixed = BrandSecondaryFixed,
        secondaryFixedDim = BrandSecondaryFixedDim,
        onSecondaryFixed = BrandOnSecondaryFixed,
        onSecondaryFixedVariant = BrandOnSecondaryFixedVariant,
        tertiaryDim = BrandTertiaryDim,
        tertiaryFixed = BrandTertiaryFixed,
        tertiaryFixedDim = BrandTertiaryFixedDim,
        onTertiaryFixed = BrandOnTertiaryFixed,
        onTertiaryFixedVariant = BrandOnTertiaryFixedVariant,
        errorDim = BrandErrorDim,
        surfaceTint = BrandSurfaceTint,
    )

val DarkExtendedColors =
    ExtendedColors(
        primaryDim = BrandDarkPrimary, // in dark mode, primary itself is the "dim" variant
        primaryFixed = BrandPrimaryFixed, // fixed colors stay the same across themes
        primaryFixedDim = BrandPrimaryFixedDim,
        onPrimaryFixed = BrandOnPrimaryFixed,
        onPrimaryFixedVariant = BrandOnPrimaryFixedVariant,
        secondaryDim = BrandDarkSecondary,
        secondaryFixed = BrandSecondaryFixed,
        secondaryFixedDim = BrandSecondaryFixedDim,
        onSecondaryFixed = BrandOnSecondaryFixed,
        onSecondaryFixedVariant = BrandOnSecondaryFixedVariant,
        tertiaryDim = BrandTertiaryDim,
        tertiaryFixed = BrandTertiaryFixed,
        tertiaryFixedDim = BrandTertiaryFixedDim,
        onTertiaryFixed = BrandOnTertiaryFixed,
        onTertiaryFixedVariant = BrandOnTertiaryFixedVariant,
        errorDim = BrandErrorDim,
        surfaceTint = BrandDarkPrimary,
    )

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }
