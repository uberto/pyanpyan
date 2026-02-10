package com.pyanpyan.domain.service

import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class FakeClockTest {

    @Test
    fun `fake clock returns configured instant`() {
        val fixedTime = Instant.parse("2026-02-10T10:00:00Z")
        val clock = FakeClock(fixedTime)

        assertEquals(fixedTime, clock.now())
    }

    @Test
    fun `fake clock can advance time`() {
        val startTime = Instant.parse("2026-02-10T10:00:00Z")
        val clock = FakeClock(startTime)

        clock.advanceBy(kotlinx.datetime.DateTimeUnit.MINUTE, 5)

        val expected = Instant.parse("2026-02-10T10:05:00Z")
        assertEquals(expected, clock.now())
    }
}
