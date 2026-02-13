package com.pyanpyan.android.ui.components

import org.junit.Test
import org.junit.Assert.*

class SliderStateTest {
    @Test
    fun center_is_pending_state() {
        val state = SliderState.Center
        assertFalse(state.isCommitted)
    }

    @Test
    fun left_is_skip_committed_state() {
        val state = SliderState.Left
        assertTrue(state.isCommitted)
    }

    @Test
    fun right_is_done_committed_state() {
        val state = SliderState.Right
        assertTrue(state.isCommitted)
    }
}
