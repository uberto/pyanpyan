package com.pyanpyan.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val swipeSound: SwipeSound = SwipeSound.SOFT_CLICK,
    val completionSound: CompletionSound = CompletionSound.NOTIFICATION,
    val enableHapticFeedback: Boolean = true
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
