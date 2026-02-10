package com.pyanpyan.domain.model

import kotlinx.datetime.Instant
import kotlin.time.Duration

@JvmInline
value class TimerId(val value: String)

enum class TimerType {
    Short,  // seconds
    Long    // minutes
}

sealed interface TimerState {
    data object NotStarted : TimerState
    data class Running(val startedAt: Instant) : TimerState
    data object Completed : TimerState
}

data class Timer(
    val id: TimerId,
    val duration: Duration,
    val type: TimerType,
    val state: TimerState
) {
    fun start(at: Instant): Timer = copy(state = TimerState.Running(startedAt = at))

    fun complete(): Timer = copy(state = TimerState.Completed)

    fun remainingTime(now: Instant): Duration? {
        return when (val s = state) {
            is TimerState.Running -> {
                val elapsed = now - s.startedAt
                (duration - elapsed).coerceAtLeast(Duration.ZERO)
            }
            else -> null
        }
    }
}
