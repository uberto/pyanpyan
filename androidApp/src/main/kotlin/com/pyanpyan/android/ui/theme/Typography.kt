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

    // Get default typography and scale all styles
    val default = Typography()
    return Typography(
        displayLarge = default.displayLarge.scale(fontFamily, fontSizeScale),
        displayMedium = default.displayMedium.scale(fontFamily, fontSizeScale),
        displaySmall = default.displaySmall.scale(fontFamily, fontSizeScale),
        headlineLarge = default.headlineLarge.scale(fontFamily, fontSizeScale),
        headlineMedium = default.headlineMedium.scale(fontFamily, fontSizeScale),
        headlineSmall = default.headlineSmall.scale(fontFamily, fontSizeScale),
        titleLarge = default.titleLarge.scale(fontFamily, fontSizeScale),
        titleMedium = default.titleMedium.scale(fontFamily, fontSizeScale),
        titleSmall = default.titleSmall.scale(fontFamily, fontSizeScale),
        bodyLarge = default.bodyLarge.scale(fontFamily, fontSizeScale),
        bodyMedium = default.bodyMedium.scale(fontFamily, fontSizeScale),
        bodySmall = default.bodySmall.scale(fontFamily, fontSizeScale),
        labelLarge = default.labelLarge.scale(fontFamily, fontSizeScale),
        labelMedium = default.labelMedium.scale(fontFamily, fontSizeScale),
        labelSmall = default.labelSmall.scale(fontFamily, fontSizeScale)
    )
}

private fun TextStyle.scale(fontFamily: FontFamily?, scale: Float): TextStyle {
    return copy(
        fontFamily = fontFamily ?: this.fontFamily,
        fontSize = fontSize * scale
    )
}
