package com.pyanpyan.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val swipeSound: SwipeSound = SwipeSound.SOFT_CLICK,
    val completionSound: CompletionSound = CompletionSound.NOTIFICATION,
    val enableHapticFeedback: Boolean = true
)

@Serializable
enum class SwipeSound {
    @SerialName("none") NONE,
    @SerialName("soft_click") SOFT_CLICK,
    @SerialName("beep") BEEP,
    @SerialName("pop") POP
}

@Serializable
enum class CompletionSound {
    @SerialName("none") NONE,
    @SerialName("notification") NOTIFICATION,
    @SerialName("success_chime") SUCCESS_CHIME,
    @SerialName("tada") TADA
}
