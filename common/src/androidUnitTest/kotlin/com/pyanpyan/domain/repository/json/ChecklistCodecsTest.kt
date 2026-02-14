package com.pyanpyan.domain.repository.json

import com.pyanpyan.domain.model.*
import com.ubertob.kondor.outcome.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ChecklistCodecsTest {

    @Test
    fun checklist_id_roundtrip() {
        val original = ChecklistId("test-123")
        val json = JChecklistId.toJson(original)
        val decoded = JChecklistId.fromJson(json).orThrow()

        assertEquals(original, decoded)
    }

    @Test
    fun checklist_item_id_roundtrip() {
        val original = ChecklistItemId("item-456")
        val json = JChecklistItemId.toJson(original)
        val decoded = JChecklistItemId.fromJson(json).orThrow()

        assertEquals(original, decoded)
    }

    @Test
    fun item_icon_id_roundtrip() {
        val original = ItemIconId("tooth")
        val json = JItemIconId.toJson(original)
        val decoded = JItemIconId.fromJson(json).orThrow()

        assertEquals(original, decoded)
    }
}
