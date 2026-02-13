// common/src/commonMain/kotlin/com/pyanpyan/domain/model/TimeRange.kt
package com.pyanpyan.domain.model

import kotlinx.datetime.LocalTime

sealed class TimeRange {
    abstract val isAllDay: Boolean
    abstract fun contains(time: LocalTime): Boolean

    object AllDay : TimeRange() {
        override val isAllDay: Boolean = true
        override fun contains(time: LocalTime): Boolean = true
    }

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
