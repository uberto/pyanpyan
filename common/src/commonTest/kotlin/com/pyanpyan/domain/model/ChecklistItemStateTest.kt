package com.pyanpyan.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChecklistItemStateTest {

    @Test
    fun `state can be Pending`() {
        val state = ChecklistItemState.Pending
        assertTrue(state is ChecklistItemState.Pending)
    }

    @Test
    fun `state can be Done`() {
        val state = ChecklistItemState.Done
        assertTrue(state is ChecklistItemState.Done)
    }

    @Test
    fun `state can be IgnoredToday`() {
        val state = ChecklistItemState.IgnoredToday
        assertTrue(state is ChecklistItemState.IgnoredToday)
    }
}
