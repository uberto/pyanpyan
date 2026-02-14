package com.pyanpyan.domain.model

import com.pyanpyan.domain.repository.json.ChecklistItemIdSerializer
import com.pyanpyan.domain.repository.json.ItemIconIdSerializer
import kotlinx.serialization.Serializable

@JvmInline
@Serializable(with = ChecklistItemIdSerializer::class)
value class ChecklistItemId(val value: String)

@JvmInline
@Serializable(with = ItemIconIdSerializer::class)
value class ItemIconId(val value: String)

@Serializable
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
