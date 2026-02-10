package com.pyanpyan.domain.service

import kotlinx.datetime.Instant

interface Clock {
    fun now(): Instant
}
