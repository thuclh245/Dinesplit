package com.example.dinesplit.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.example.dinesplit.R

// Use a safe initialization for the Google Font provider to avoid crashing the Compose Preview
// if the library is not found in the renderer's classpath (NoClassDefFoundError).
private val provider: GoogleFont.Provider? = try {
    GoogleFont.Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage = "com.google.android.gms",
        certificates = R.array.com_google_android_gms_fonts_certs
    )
} catch (e: Throwable) {
    null
}

/**
 * Plus Jakarta Sans — Display & Headlines (editorial, bold, high-contrast)
 */
val PlusJakartaSans = provider?.let { p ->
    val fontName = GoogleFont("Plus Jakarta Sans")
    FontFamily(
        Font(googleFont = fontName, fontProvider = p, weight = FontWeight.Normal),
        Font(googleFont = fontName, fontProvider = p, weight = FontWeight.Medium),
        Font(googleFont = fontName, fontProvider = p, weight = FontWeight.SemiBold),
        Font(googleFont = fontName, fontProvider = p, weight = FontWeight.Bold),
        Font(googleFont = fontName, fontProvider = p, weight = FontWeight.ExtraBold),
    )
} ?: FontFamily.Default

/**
 * Inter — Body & UI labels (high x-height, excellent readability)
 */
val Inter = provider?.let { p ->
    val fontName = GoogleFont("Inter")
    FontFamily(
        Font(googleFont = fontName, fontProvider = p, weight = FontWeight.Normal),
        Font(googleFont = fontName, fontProvider = p, weight = FontWeight.Medium),
        Font(googleFont = fontName, fontProvider = p, weight = FontWeight.SemiBold),
        Font(googleFont = fontName, fontProvider = p, weight = FontWeight.Bold),
    )
} ?: FontFamily.Default

val Typography = Typography(
    // ─── Display: Plus Jakarta Sans (editorial "Total Due" amounts) ───
    displayLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 56.sp,
        lineHeight = 60.sp,
        letterSpacing = (-1.2).sp
    ),
    displayMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 48.sp,
        lineHeight = 54.sp,
        letterSpacing = (-0.8).sp
    ),
    displaySmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.4).sp
    ),

    // ─── Headlines: Plus Jakarta Sans (restaurant names, bill titles) ───
    headlineLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp
    ),

    // ─── Titles: Plus Jakarta Sans (section headers) ───
    titleLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleSmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),

    // ─── Body: Inter (descriptions, expense data) ───
    bodyLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.2.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp
    ),

    // ─── Labels: Inter (UI labels, uppercase descriptors) ───
    labelLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.8.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.sp
    )
)
