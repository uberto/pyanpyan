package com.pyanpyan.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ChecklistColor(val hex: String, val displayName: String) {
    SOFT_BLUE("#A8D5E2", "Soft Blue"),
    CALM_GREEN("#C8E6C9", "Calm Green"),
    GENTLE_PURPLE("#D1C4E9", "Gentle Purple"),
    WARM_PEACH("#FFE0B2", "Warm Peach"),
    COOL_MINT("#B2DFDB", "Cool Mint"),
    LIGHT_LAVENDER("#E1BEE7", "Light Lavender"),
    PALE_YELLOW("#FFF9C4", "Pale Yellow"),
    SOFT_ROSE("#F8BBD0", "Soft Rose"),
    LIGHT_CORAL("#FFAB91", "Light Coral"),
    POWDER_BLUE("#B3E5FC", "Powder Blue"),
    MINT_CREAM("#E0F2F1", "Mint Cream"),
    BLUSH_PINK("#F48FB1", "Blush Pink"),
    LAVENDER("#E6E6FA", "Lavender"),
    SKY_BLUE("#87CEEB", "Sky Blue"),
    SAGE_GREEN("#B2D8B2", "Sage Green"),
    PEACH_CREAM("#FFDAB9", "Peach Cream")
}
