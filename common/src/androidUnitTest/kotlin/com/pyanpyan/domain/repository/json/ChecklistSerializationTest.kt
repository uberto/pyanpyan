package com.pyanpyan.domain.repository.json

import com.pyanpyan.domain.model.*
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChecklistSerializationTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    @Test
    fun checklist_id_roundtrip() {
        val original = ChecklistId("test-123")
        val jsonString = json.encodeToString(original)
        val decoded = json.decodeFromString<ChecklistId>(jsonString)

        assertEquals(original, decoded)
        assertEquals("\"test-123\"", jsonString)
    }

    @Test
    fun checklist_item_id_roundtrip() {
        val original = ChecklistItemId("item-456")
        val jsonString = json.encodeToString(original)
        val decoded = json.decodeFromString<ChecklistItemId>(jsonString)

        assertEquals(original, decoded)
        assertEquals("\"item-456\"", jsonString)
    }

    @Test
    fun item_icon_id_roundtrip() {
        val original = ItemIconId("tooth")
        val jsonString = json.encodeToString(original)
        val decoded = json.decodeFromString<ItemIconId>(jsonString)

        assertEquals(original, decoded)
        assertEquals("\"tooth\"", jsonString)
    }
}
