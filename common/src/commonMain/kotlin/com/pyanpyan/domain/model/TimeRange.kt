package com.pyanpyan.domain.model

import kotlinx.datetime.LocalTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class TimeRange {
    abstract val isAllDay: Boolean
    abstract fun contains(time: LocalTime): Boolean

    @Serializable
    @SerialName("AllDay")
    data object AllDay : TimeRange() {
        override val isAllDay: Boolean = true
        override fun contains(time: LocalTime): Boolean = true
    }

    @Serializable
    @SerialName("Specific")
    data class Specific(
        val startTime: LocalTime,
        val endTime: LocalTime
    ) : TimeRange() {
        override val isAllDay: Boolean = false

        override fun contains(time: LocalTime): Boolean {
            return time >= startTime && time <= endTime
        }
    }
}
