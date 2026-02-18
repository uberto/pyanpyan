package com.pyanpyan.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val swipeSound: SwipeSound = SwipeSound.SOFT_CLICK,
    val completionSound: CompletionSound = CompletionSound.NOTIFICATION,
    val enableHapticFeedback: Boolean = true,
    val fontFamilyName: String? = null,      // null = system default
    val fontSizeScale: Float = 1.0f          // 0.7 to 1.5
)

@Serializable
enum class SwipeSound(val displayName: String) {
    NONE("None"),
    SOFT_CLICK("Soft Click"),
    BEEP("Beep"),
    POP("Pop")
}

@Serializable
enum class CompletionSound(val displayName: String) {
    NONE("None"),
    NOTIFICATION("Notification"),
    SUCCESS_CHIME("Success Chime"),
    TADA("Tada")
}
