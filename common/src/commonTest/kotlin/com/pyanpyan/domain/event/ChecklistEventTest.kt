// common/src/commonTest/kotlin/com/pyanpyan/domain/event/ChecklistEventTest.kt
package com.pyanpyan.domain.event

import com.pyanpyan.domain.model.ChecklistId
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals

class ChecklistEventTest {
    @Test
    fun created_event_captures_initial_state() {
        val id = ChecklistId("morning")
        val now = Clock.System.now()

        val event = ChecklistEvent.Created(
            checklistId = id,
            timestamp = now,
            name = "Morning Routine",
            itemCount = 3
        )

        assertEquals(id, event.checklistId)
        assertEquals(now, event.timestamp)
        assertEquals("Morning Routine", event.name)
        assertEquals(3, event.itemCount)
    }

    @Test
    fun updated_event_tracks_changes() {
        val event = ChecklistEvent.Updated(
            checklistId = ChecklistId("test"),
            timestamp = Clock.System.now(),
            changes = setOf(
                ChecklistEvent.ChangeType.NAME,
                ChecklistEvent.ChangeType.COLOR
            )
        )

        assertEquals(2, event.changes.size)
        assert(event.changes.contains(ChecklistEvent.ChangeType.NAME))
    }

    @Test
    fun accessed_event_tracks_when_user_opens_checklist() {
        val event = ChecklistEvent.Accessed(
            checklistId = ChecklistId("test"),
            timestamp = Clock.System.now()
        )

        assertEquals(ChecklistId("test"), event.checklistId)
    }

    @Test
    fun deleted_event_tracks_removal() {
        val event = ChecklistEvent.Deleted(
            checklistId = ChecklistId("test"),
            timestamp = Clock.System.now()
        )

        assertEquals(ChecklistId("test"), event.checklistId)
    }
}
