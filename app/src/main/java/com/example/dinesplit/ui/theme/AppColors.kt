@file:Suppress("unused")

package com.example.dinesplit.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Semantic color aliases for DineSplit screens.
 *
 * These map directly to the Stitch design tokens so that screens don't need
 * to hardcode hex values. Use `AppColors.xxx` for quick access or
 * `MaterialTheme.colorScheme.xxx` for standard Material tokens.
 *
 * ─── Mapping from old hardcoded values ───────────────────────────────────────
 * Old Color(0xFFF9F9F9) → MaterialTheme.colorScheme.surface (#F6F6F6)
 * Old Color(0xFFFFFFFF) → MaterialTheme.colorScheme.surfaceContainerLowest
 * Old Color(0xFFF3F3F3) → MaterialTheme.colorScheme.surfaceContainerLow (#F0F1F1)
 * Old Color(0xFFE8E8E8) → MaterialTheme.colorScheme.surfaceContainer (#E7E8E8)
 * Old Color(0xFFE2E2E2) → MaterialTheme.colorScheme.surfaceContainerHigh (#E1E3E3)
 * Old Color(0xFFEEEEEE) → MaterialTheme.colorScheme.surfaceContainerHigh
 * Old Color(0xFF1A1C1C) → MaterialTheme.colorScheme.onSurface (#2D2F2F)
 * Old Color(0xFF56423E) → MaterialTheme.colorScheme.onSurfaceVariant (#5A5C5C)
 * Old Color(0xFFE2725B) → BrandPrimaryContainer (#FF7851) — gradient start
 * Old Color(0xFF9F402D) → BrandPrimary (#AB2D00) — gradient end
 * Old Color(0xFFBA1A1A) → MaterialTheme.colorScheme.error (#B31B25)
 * Old Color(0xFF006B5B) → MaterialTheme.colorScheme.secondary (#00675F)
 * Old Color(0xFFE0F2F1) → MaterialTheme.colorScheme.secondaryContainer (#81F3E5)
 * Old Color(0xFFD0F4EB) → MaterialTheme.colorScheme.secondaryContainer
 * Old Color(0xFFFFDAD3/6) → MaterialTheme.colorScheme.errorContainer (#FB5151) or lighter
 */
object AppColors {
    // ─── Gradient for Primary CTA (Design System "Signature Texture") ────────
    /** 135° gradient from primary → primaryContainer for main CTAs */
    val primaryGradient: Brush
        get() =
            Brush.linearGradient(
                colors = listOf(BrandPrimary, BrandPrimaryContainer),
            )

    /** Reverse gradient for dark-on-light contexts */
    val primaryGradientReverse: Brush
        get() =
            Brush.linearGradient(
                colors = listOf(BrandPrimaryContainer, BrandPrimary),
            )

    // ─── Semantic Surface Colors (use these instead of hardcoded hex) ────────
    val background: Color get() = BrandSurface // #F6F6F6
    val surfaceWhite: Color get() = BrandSurfaceContainerLowest // #FFFFFF
    val surfaceLow: Color get() = BrandSurfaceContainerLow // #F0F1F1
    val surfaceMid: Color get() = BrandSurfaceContainer // #E7E8E8
    val surfaceHigh: Color get() = BrandSurfaceContainerHigh // #E1E3E3
    val surfaceHighest: Color get() = BrandSurfaceContainerHighest // #DBDDDD

    // ─── Text Colors ─────────────────────────────────────────────────────────
    val textMain: Color get() = BrandOnSurface // #2D2F2F
    val textSub: Color get() = BrandOnSurfaceVariant // #5A5C5C
    val textHint: Color get() = BrandOutline // #767777

    // ─── Accent Colors ───────────────────────────────────────────────────────
    val orangePrimary: Color get() = BrandPrimary // #AB2D00
    val orangeContainer: Color get() = BrandPrimaryContainer // #FF7851
    val tealPrimary: Color get() = BrandSecondary // #00675F
    val tealContainer: Color get() = BrandSecondaryContainer // #81F3E5
    val errorRed: Color get() = BrandError // #B31B25
    val errorContainer: Color get() = BrandErrorContainer // #FB5151

    // ─── Border / Outline (Ghost Border at 15% opacity per design system) ────
    val borderLight: Color get() = BrandOutlineVariant // #ACADAD
}
