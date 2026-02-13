package com.pyanpyan.domain.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RepositoryErrorTest {
    @Test
    fun file_read_error_captures_message_and_cause() {
        val cause = RuntimeException("IO failed")
        val error = RepositoryError.FileReadError("Failed to read", cause)

        assertEquals("Failed to read", error.message)
        assertEquals(cause, error.cause)
    }

    @Test
    fun file_write_error_captures_message() {
        val error = RepositoryError.FileWriteError("Write failed", null)

        assertEquals("Write failed", error.message)
    }

    @Test
    fun json_parse_error_captures_message() {
        val error = RepositoryError.JsonParseError("Invalid JSON", null)

        assertEquals("Invalid JSON", error.message)
    }

    @Test
    fun invalid_data_error_captures_message() {
        val error = RepositoryError.InvalidDataError("Bad data")

        assertEquals("Bad data", error.message)
    }
}
