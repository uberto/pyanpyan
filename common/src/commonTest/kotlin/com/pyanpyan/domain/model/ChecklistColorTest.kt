package com.pyanpyan.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ChecklistColorTest {
    @Test
    fun has_8_color_options() {
        assertEquals(8, ChecklistColor.entries.size)
    }

    @Test
    fun each_color_has_hex_and_name() {
        val blue = ChecklistColor.SOFT_BLUE

        assertEquals("#A8D5E2", blue.hex)
        assertEquals("Soft Blue", blue.displayName)
    }

    @Test
    fun can_get_all_colors() {
        val colors = ChecklistColor.entries

        assertEquals(ChecklistColor.SOFT_BLUE, colors[0])
        assertEquals(ChecklistColor.SOFT_ROSE, colors[7])
    }
}
