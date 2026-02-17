package com.pyanpyan.domain.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class AppSettingsTest {

    @Test
    fun `serializes AppSettings to JSON`() {
        val settings = AppSettings(
            swipeSound = SwipeSound.SOFT_CLICK,
            completionSound = CompletionSound.NOTIFICATION,
            enableHapticFeedback = true
        )

        val json = Json.encodeToString(AppSettings.serializer(), settings)
        val decoded = Json.decodeFromString(AppSettings.serializer(), json)

        assertEquals(settings, decoded)
    }

    @Test
    fun `defaults to SOFT_CLICK and NOTIFICATION`() {
        val settings = AppSettings()

        assertEquals(SwipeSound.SOFT_CLICK, settings.swipeSound)
        assertEquals(CompletionSound.NOTIFICATION, settings.completionSound)
        assertEquals(true, settings.enableHapticFeedback)
    }
}
