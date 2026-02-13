// common/src/commonMain/kotlin/com/pyanpyan/domain/event/ChecklistEvent.kt
package com.pyanpyan.domain.event

import com.pyanpyan.domain.model.ChecklistId
import kotlinx.datetime.Instant

sealed class ChecklistEvent {
    abstract val checklistId: ChecklistId
    abstract val timestamp: Instant

    data class Created(
        override val checklistId: ChecklistId,
        override val timestamp: Instant,
        val name: String,
        val itemCount: Int
    ) : ChecklistEvent()

    data class Updated(
        override val checklistId: ChecklistId,
        override val timestamp: Instant,
        val changes: Set<ChangeType>
    ) : ChecklistEvent()

    data class Accessed(
        override val checklistId: ChecklistId,
        override val timestamp: Instant
    ) : ChecklistEvent()

    data class Deleted(
        override val checklistId: ChecklistId,
        override val timestamp: Instant
    ) : ChecklistEvent()

    enum class ChangeType {
        NAME,
        SCHEDULE,
        COLOR,
        STATE_PERSISTENCE,
        ITEMS_ADDED,
        ITEMS_REMOVED,
        ITEMS_REORDERED
    }
}
