package com.pyanpyan.domain.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RepositoryResultTest {

    @Test
    fun `success creates Success result`() {
        val result = RepositoryResult.success("test")
        assertTrue(result.isSuccess())
        assertFalse(result.isFailure())
        assertEquals("test", result.getOrNull())
        assertNull(result.errorOrNull())
    }

    @Test
    fun `failure creates Failure result`() {
        val error = RepositoryError.FileReadError("test error")
        val result = RepositoryResult.failure<String>(error)
        assertFalse(result.isSuccess())
        assertTrue(result.isFailure())
        assertNull(result.getOrNull())
        assertEquals(error, result.errorOrNull())
    }

    @Test
    fun `map transforms Success value`() {
        val result = RepositoryResult.success(5)
        val mapped = result.map { it * 2 }
        assertTrue(mapped.isSuccess())
        assertEquals(10, mapped.getOrNull())
    }

    @Test
    fun `map preserves Failure`() {
        val error = RepositoryError.InvalidDataError("test")
        val result = RepositoryResult.failure<Int>(error)
        val mapped = result.map { it * 2 }
        assertTrue(mapped.isFailure())
        assertEquals(error, mapped.errorOrNull())
    }

    @Test
    fun `flatMap chains Success results`() {
        val result = RepositoryResult.success(5)
        val chained = result.flatMap { RepositoryResult.success(it * 2) }
        assertTrue(chained.isSuccess())
        assertEquals(10, chained.getOrNull())
    }

    @Test
    fun `flatMap propagates Failure`() {
        val error = RepositoryError.InvalidDataError("test")
        val result = RepositoryResult.failure<Int>(error)
        val chained = result.flatMap { RepositoryResult.success(it * 2) }
        assertTrue(chained.isFailure())
        assertEquals(error, chained.errorOrNull())
    }

    @Test
    fun `onSuccess executes action for Success`() {
        var executed = false
        RepositoryResult.success(5).onSuccess { executed = true }
        assertTrue(executed)
    }

    @Test
    fun `onSuccess does not execute action for Failure`() {
        var executed = false
        val error = RepositoryError.InvalidDataError("test")
        RepositoryResult.failure<Int>(error).onSuccess { executed = true }
        assertFalse(executed)
    }

    @Test
    fun `onFailure executes action for Failure`() {
        var executed = false
        val error = RepositoryError.InvalidDataError("test")
        RepositoryResult.failure<Int>(error).onFailure { executed = true }
        assertTrue(executed)
    }

    @Test
    fun `onFailure does not execute action for Success`() {
        var executed = false
        RepositoryResult.success(5).onFailure { executed = true }
        assertFalse(executed)
    }
}
