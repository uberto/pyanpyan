package com.pyanpyan.domain.model

sealed interface ChecklistItemState {
    data object Pending : ChecklistItemState
    data object Done : ChecklistItemState
    data object IgnoredToday : ChecklistItemState
}
