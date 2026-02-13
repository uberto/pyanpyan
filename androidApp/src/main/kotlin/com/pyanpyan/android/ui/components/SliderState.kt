package com.pyanpyan.android.ui.components

/**
 * Represents the position state of the drag slider UI component.
 *
 * Used to model user interaction with the slider control that replaces
 * buttons for marking checklist items as done or skipped.
 *
 * Maps to ChecklistItemState as follows:
 * - [Center] → Pending (not yet acted upon)
 * - [Left] → IgnoredToday (skipped)
 * - [Right] → Done (completed)
 */
sealed class SliderState {
    /**
     * Whether the slider position represents a committed action.
     * - `false` for [Center] (user can still drag)
     * - `true` for [Left] and [Right] (action completed, slider locked)
     */
    abstract val isCommitted: Boolean

    /** Slider at center position - item is Pending */
    object Center : SliderState() {
        override val isCommitted = false
    }

    /** Slider at left position - item will be marked IgnoredToday */
    object Left : SliderState() {
        override val isCommitted = true
    }

    /** Slider at right position - item will be marked Done */
    object Right : SliderState() {
        override val isCommitted = true
    }
}
