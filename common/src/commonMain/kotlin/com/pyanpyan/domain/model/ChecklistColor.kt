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
    SOFT_ROSE("#F8BBD0", "Soft Rose")
}
