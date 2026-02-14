package com.pyanpyan.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface ChecklistItemState {
    @Serializable
    @SerialName("Pending")
    data object Pending : ChecklistItemState

    @Serializable
    @SerialName("Done")
    data object Done : ChecklistItemState

    @Serializable
    @SerialName("IgnoredToday")
    data object IgnoredToday : ChecklistItemState
}
