package com.pyanpyan.domain.model

import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class TimerTest {

    @Test
    fun `timer can be created with duration and type`() {
        val timer = Timer(
            id = TimerId("timer-1"),
            duration = 60.seconds,
            type = TimerType.Short,
            state = TimerState.NotStarted
        )

        assertEquals(TimerId("timer-1"), timer.id)
        assertEquals(60.seconds, timer.duration)
        assertEquals(TimerType.Short, timer.type)
    }

    @Test
    fun `timer can be started`() {
        val timer = Timer(
            id = TimerId("timer-1"),
            duration = 60.seconds,
            type = TimerType.Short,
            state = TimerState.NotStarted
        )
        val startTime = Instant.parse("2026-02-10T10:00:00Z")

        val started = timer.start(startTime)

        assertTrue(started.state is TimerState.Running)
        assertEquals(startTime, (started.state as TimerState.Running).startedAt)
    }

    @Test
    fun `timer can be completed`() {
        val startTime = Instant.parse("2026-02-10T10:00:00Z")
        val timer = Timer(
            id = TimerId("timer-1"),
            duration = 60.seconds,
            type = TimerType.Short,
            state = TimerState.Running(startedAt = startTime)
        )

        val completed = timer.complete()

        assertTrue(completed.state is TimerState.Completed)
    }
}
