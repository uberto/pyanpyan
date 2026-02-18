package com.pyanpyan.android.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private val SkippedGray = Color(0xFFD3D3D3)
private val DoneGreen = Color(0xFF4CAF50)

@Composable
fun ItemSlider(
    state: SliderState,
    onSkip: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    enableHaptic: Boolean = true,
    onReset: () -> Unit = {}
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // Track dimensions
    var trackWidth by remember { mutableStateOf(0f) }

    // Offset state
    val offsetX = remember { Animatable(0f) }

    // Maximum offset to keep thumb inside track (thumb size is 32dp)
    val thumbSize = with(density) { 32.dp.toPx() }
    val maxOffset = remember(trackWidth) {
        (trackWidth - thumbSize) / 2
    }

    // Track if we're currently dragging
    var isDragging by remember { mutableStateOf(false) }

    // Initial position based on state (only when not dragging)
    LaunchedEffect(state, trackWidth) {
        if (trackWidth > 0 && !isDragging) {
            val target = when (state) {
                SliderState.Center -> 0f
                SliderState.Left -> -maxOffset
                SliderState.Right -> maxOffset
            }
            // Only snap if position is significantly different
            if (kotlin.math.abs(offsetX.value - target) > 10f) {
                offsetX.snapTo(target)
            }
        }
    }

    // Drag threshold (20% - easier to trigger)
    val threshold = trackWidth * 0.20f

    // Background color based on position (swapped: left is Done, right is Skipped)
    val backgroundColor = when {
        offsetX.value < -threshold -> DoneGreen
        offsetX.value > threshold -> SkippedGray
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    // Text opacity based on position - reveal text when moving toward it
    // Done text (on left) shows when moving right (positive offsetX)
    val doneOpacity = if (offsetX.value > 0) {
        (offsetX.value / maxOffset).coerceIn(0f, 1f)
    } else {
        0f
    }

    // Skipped text (on right) shows when moving left (negative offsetX)
    val skippedOpacity = if (offsetX.value < 0) {
        (kotlin.math.abs(offsetX.value) / maxOffset).coerceIn(0f, 1f)
    } else {
        0f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(1.dp, Color.DarkGray, RoundedCornerShape(20.dp))
            .pointerInput(enabled, maxOffset) {
                if (!enabled || maxOffset <= 0) return@pointerInput

                detectHorizontalDragGestures(
                    onDragStart = {
                        isDragging = true
                    },
                    onDragEnd = {
                        isDragging = false
                        scope.launch {
                            val currentOffset = offsetX.value

                            when {
                                currentOffset < -threshold -> {
                                    // Snap to left (Done)
                                    offsetX.animateTo(
                                        -maxOffset,
                                        animationSpec = tween(300)
                                    )
                                    if (enableHaptic) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    onDone()
                                }
                                currentOffset > threshold -> {
                                    // Snap to right (Skipped)
                                    offsetX.animateTo(
                                        maxOffset,
                                        animationSpec = tween(300)
                                    )
                                    if (enableHaptic) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    onSkip()
                                }
                                else -> {
                                    // Spring back to center
                                    offsetX.animateTo(
                                        0f,
                                        animationSpec = tween(300)
                                    )
                                    // Only call onReset if we were previously committed
                                    if (state.isCommitted) {
                                        onReset()
                                    }
                                }
                            }
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        scope.launch {
                            val newValue = (offsetX.value + dragAmount)
                                .coerceIn(-maxOffset, maxOffset)
                            offsetX.snapTo(newValue)
                        }
                    }
                )
            }
    ) {
        // Measure track width
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            trackWidth = with(density) { constraints.maxWidth.toFloat() }

            // Thumb (smaller, stays within bounds)
            val thumbX = with(density) {
                // Center the track, then add offset
                val centerOffset = trackWidth / 2
                val thumbRadius = 16.dp.toPx()
                (centerOffset + offsetX.value - thumbRadius).toDp()
            }

            Box(
                modifier = Modifier
                    .offset(x = thumbX)
                    .align(Alignment.TopStart)
                    .padding(vertical = 4.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color.Black, CircleShape)  // Thick black border
                    .background(MaterialTheme.colorScheme.primary)  // Sky blue fill
            )

            // "Done" text on left side
            Text(
                text = "Done",
                color = Color.Black.copy(alpha = doneOpacity),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
            )

            // "Skipped" text on right side
            Text(
                text = "Skipped",
                color = Color.Black.copy(alpha = skippedOpacity),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
            )
        }
    }
}
