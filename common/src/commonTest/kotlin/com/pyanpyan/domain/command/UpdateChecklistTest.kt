package com.pyanpyan.domain.command

import com.pyanpyan.domain.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.datetime.LocalTime

class UpdateChecklistTest {
    @Test
    fun updates_checklist_name() {
        val original = createTestChecklist(name = "Old Name")

        val command = UpdateChecklist(
            checklist = original,
            name = "New Name"
        )

        val updated = command.execute()

        assertEquals("New Name", updated.name)
        assertEquals(original.id, updated.id)
        assertEquals(original.color, updated.color)
    }

    @Test
    fun updates_checklist_color() {
        val original = createTestChecklist(color = ChecklistColor.SOFT_BLUE)

        val command = UpdateChecklist(
            checklist = original,
            color = ChecklistColor.WARM_PEACH
        )

        val updated = command.execute()

        assertEquals(ChecklistColor.WARM_PEACH, updated.color)
    }

    @Test
    fun updates_state_persistence() {
        val original = createTestChecklist(
            statePersistence = StatePersistenceDuration.FIFTEEN_MINUTES
        )

        val command = UpdateChecklist(
            checklist = original,
            statePersistence = StatePersistenceDuration.ONE_HOUR
        )

        val updated = command.execute()

        assertEquals(StatePersistenceDuration.ONE_HOUR, updated.statePersistence)
    }

    @Test
    fun updates_multiple_properties_at_once() {
        val original = createTestChecklist(
            name = "Old",
            color = ChecklistColor.SOFT_BLUE
        )

        val command = UpdateChecklist(
            checklist = original,
            name = "New",
            color = ChecklistColor.WARM_PEACH
        )

        val updated = command.execute()

        assertEquals("New", updated.name)
        assertEquals(ChecklistColor.WARM_PEACH, updated.color)
    }

    @Test
    fun rejects_blank_name_update() {
        val original = createTestChecklist()

        val exception = assertFailsWith<IllegalArgumentException> {
            UpdateChecklist(
                checklist = original,
                name = "   " // blank
            )
        }
        assertEquals("Checklist name cannot be blank", exception.message)
    }

    @Test
    fun rejects_invalid_time_range_update() {
        val original = createTestChecklist()

        val exception = assertFailsWith<IllegalArgumentException> {
            UpdateChecklist(
                checklist = original,
                schedule = ChecklistSchedule(
                    emptySet(),
                    TimeRange.Specific(
                        startTime = LocalTime(9, 0),
                        endTime = LocalTime(6, 0) // end before start!
                    )
                )
            )
        }
        assertEquals("Start time must be before end time", exception.message)
    }

    private fun createTestChecklist(
        name: String = "Test",
        color: ChecklistColor = ChecklistColor.SOFT_BLUE,
        statePersistence: StatePersistenceDuration = StatePersistenceDuration.FIFTEEN_MINUTES
    ) = Checklist(
        id = ChecklistId("test"),
        name = name,
        schedule = ChecklistSchedule(emptySet(), TimeRange.AllDay),
        items = emptyList(),
        color = color,
        statePersistence = statePersistence
    )
}
