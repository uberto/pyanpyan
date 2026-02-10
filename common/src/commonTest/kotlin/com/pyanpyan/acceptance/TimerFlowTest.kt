package com.pyanpyan.acceptance

import com.pyanpyan.domain.model.*
import com.pyanpyan.domain.service.FakeClock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Acceptance Test: User starts and completes a timer
 *
 * Given: A timer with specified duration
 * When: User starts the timer
 * Then: Timer begins counting down
 * When: Time elapses equal to duration
 * Then: Timer can be marked as completed
 */
class TimerFlowTest {

    @Test
    fun `user can start and complete a timer`() {
        // Given: A 60-second timer
        val timer = Timer(
            id = TimerId("brush-teeth-timer"),
            duration = 60.seconds,
            type = TimerType.Short,
            state = TimerState.NotStarted
        )
        val clock = FakeClock(Instant.parse("2026-02-10T10:00:00Z"))

        // When: User starts the timer
        val runningTimer = timer.start(clock.now())

        // Then: Timer is running
        assertTrue(runningTimer.state is TimerState.Running)

        // When: 30 seconds pass
        clock.advanceBy(kotlinx.datetime.DateTimeUnit.SECOND, 30)

        // Then: 30 seconds remain
        val remaining = runningTimer.remainingTime(clock.now())
        assertEquals(30.seconds, remaining)

        // When: Full duration passes
        clock.advanceBy(kotlinx.datetime.DateTimeUnit.SECOND, 30)

        // Then: Timer can be completed
        val completedTimer = runningTimer.complete()
        assertTrue(completedTimer.state is TimerState.Completed)
    }
}
