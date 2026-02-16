package com.pyanpyan.android.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun ItemSlider(
    state: SliderState,
    onSkip: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onReset: () -> Unit = {}
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    // Track dimensions
    var trackWidth by remember { mutableStateOf(0f) }

    // Offset state
    val offsetX = remember { Animatable(0f) }

    // Maximum offset to keep thumb inside track (thumb size is 32dp)
    val thumbSize = with(density) { 32.dp.toPx() }
    val maxOffset = (trackWidth - thumbSize) / 2

    // Initial position based on state
    LaunchedEffect(state, trackWidth) {
        if (trackWidth > 0) {
            val target = when (state) {
                SliderState.Center -> 0f
                SliderState.Left -> -maxOffset
                SliderState.Right -> maxOffset
            }
            offsetX.snapTo(target)
        }
    }

    // Drag threshold (35%)
    val threshold = trackWidth * 0.35f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(enabled, maxOffset) {
                if (!enabled || maxOffset <= 0) return@pointerInput

                detectHorizontalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            val currentOffset = offsetX.value

                            when {
                                currentOffset < -threshold -> {
                                    // Snap to left
                                    offsetX.animateTo(
                                        -maxOffset,
                                        animationSpec = tween(300)
                                    )
                                    onSkip()
                                }
                                currentOffset > threshold -> {
                                    // Snap to right
                                    offsetX.animateTo(
                                        maxOffset,
                                        animationSpec = tween(300)
                                    )
                                    onDone()
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

            // Single label based on position
            when {
                offsetX.value < -threshold -> {
                    // Show "Skipped" on the left
                    Text(
                        text = "Skipped",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 12.dp)
                    )
                }
                offsetX.value > threshold -> {
                    // Show "Done" on the right
                    Text(
                        text = "Done",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 12.dp)
                    )
                }
            }

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
                    .background(
                        when {
                            offsetX.value < -threshold -> MaterialTheme.colorScheme.onSurfaceVariant
                            offsetX.value > threshold -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
            )
        }
    }
}
