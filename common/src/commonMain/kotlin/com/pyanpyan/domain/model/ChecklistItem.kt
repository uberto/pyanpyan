package com.pyanpyan.domain.model

@JvmInline
value class ChecklistItemId(val value: String)

data class ChecklistItem(
    val id: ChecklistItemId,
    val title: String,
    val state: ChecklistItemState
) {
    fun markDone(): ChecklistItem = copy(state = ChecklistItemState.Done)

    fun ignoreToday(): ChecklistItem = copy(state = ChecklistItemState.IgnoredToday)

    fun reset(): ChecklistItem = copy(state = ChecklistItemState.Pending)
}
