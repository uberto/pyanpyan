package com.pyanpyan.android.ui.theme

import android.graphics.Typeface
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

fun buildCustomTypography(
    fontFamilyName: String?,
    fontSizeScale: Float
): Typography {
    // Get font family, fall back to default on error
    val fontFamily = fontFamilyName?.let {
        try {
            FontFamily(Typeface.create(it, Typeface.NORMAL))
        } catch (e: Exception) {
            null  // Invalid font name, use default
        }
    }

    // Clamp scale to valid range
    val clampedScale = fontSizeScale.coerceIn(0.7f, 1.5f)

    // Get default typography and scale all styles
    val default = Typography()
    return Typography(
        displayLarge = default.displayLarge.scale(fontFamily, clampedScale),
        displayMedium = default.displayMedium.scale(fontFamily, clampedScale),
        displaySmall = default.displaySmall.scale(fontFamily, clampedScale),
        headlineLarge = default.headlineLarge.scale(fontFamily, clampedScale),
        headlineMedium = default.headlineMedium.scale(fontFamily, clampedScale),
        headlineSmall = default.headlineSmall.scale(fontFamily, clampedScale),
        titleLarge = default.titleLarge.scale(fontFamily, clampedScale),
        titleMedium = default.titleMedium.scale(fontFamily, clampedScale),
        titleSmall = default.titleSmall.scale(fontFamily, clampedScale),
        bodyLarge = default.bodyLarge.scale(fontFamily, clampedScale),
        bodyMedium = default.bodyMedium.scale(fontFamily, clampedScale),
        bodySmall = default.bodySmall.scale(fontFamily, clampedScale),
        labelLarge = default.labelLarge.scale(fontFamily, clampedScale),
        labelMedium = default.labelMedium.scale(fontFamily, clampedScale),
        labelSmall = default.labelSmall.scale(fontFamily, clampedScale)
    )
}

private fun TextStyle.scale(fontFamily: FontFamily?, scale: Float): TextStyle {
    return copy(
        fontFamily = fontFamily ?: this.fontFamily,
        fontSize = fontSize * scale
    )
}
