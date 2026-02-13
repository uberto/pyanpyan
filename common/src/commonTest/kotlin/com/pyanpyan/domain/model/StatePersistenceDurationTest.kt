package com.pyanpyan.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StatePersistenceDurationTest {
    @Test
    fun has_6_duration_options() {
        assertEquals(6, StatePersistenceDuration.entries.size)
    }

    @Test
    fun zero_resets_immediately() {
        assertEquals(0L, StatePersistenceDuration.ZERO.milliseconds)
    }

    @Test
    fun fifteen_minutes_is_default() {
        assertEquals(900_000L, StatePersistenceDuration.FIFTEEN_MINUTES.milliseconds)
    }

    @Test
    fun never_expires_has_null_duration() {
        assertNull(StatePersistenceDuration.NEVER.milliseconds)
    }

    @Test
    fun all_durations_have_display_names() {
        assertEquals("Reset immediately", StatePersistenceDuration.ZERO.displayName)
        assertEquals("1 minute", StatePersistenceDuration.ONE_MINUTE.displayName)
        assertEquals("15 minutes", StatePersistenceDuration.FIFTEEN_MINUTES.displayName)
        assertEquals("1 hour", StatePersistenceDuration.ONE_HOUR.displayName)
        assertEquals("1 day", StatePersistenceDuration.ONE_DAY.displayName)
        assertEquals("Never", StatePersistenceDuration.NEVER.displayName)
    }
}
