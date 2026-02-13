package com.pyanpyan.android.ui.components

sealed class SliderState {
    abstract val isCommitted: Boolean

    object Center : SliderState() {
        override val isCommitted = false
    }

    object Left : SliderState() {
        override val isCommitted = true
    }

    object Right : SliderState() {
        override val isCommitted = true
    }
}
