package com.pyanpyan.domain.model

@JvmInline
value class ChecklistItemId(val value: String)

@JvmInline
value class ItemIconId(val value: String)

data class ChecklistItem(
    val id: ChecklistItemId,
    val title: String,
    val iconId: ItemIconId? = null,
    val state: ChecklistItemState
) {
    fun markDone(): ChecklistItem = copy(state = ChecklistItemState.Done)

    fun ignoreToday(): ChecklistItem = copy(state = ChecklistItemState.IgnoredToday)

    fun reset(): ChecklistItem = copy(state = ChecklistItemState.Pending)
}
