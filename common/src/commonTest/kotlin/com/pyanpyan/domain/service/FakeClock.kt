package com.pyanpyan.domain.service

import kotlinx.datetime.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus

class FakeClock(private var currentTime: Instant) : Clock {
    override fun now(): Instant = currentTime

    fun advanceBy(unit: DateTimeUnit, value: Int) {
        currentTime = when (unit) {
            is DateTimeUnit.TimeBased -> currentTime.plus(value, unit)
            else -> throw IllegalArgumentException("Only TimeBased units are supported")
        }
    }
}
