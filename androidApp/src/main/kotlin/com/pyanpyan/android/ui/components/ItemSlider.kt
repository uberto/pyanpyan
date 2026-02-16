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

    // Initial position based on state
    LaunchedEffect(state) {
        val target = when (state) {
            SliderState.Center -> 0f
            SliderState.Left -> -trackWidth / 2
            SliderState.Right -> trackWidth / 2
        }
        offsetX.snapTo(target)
    }

    // Drag threshold (70%)
    val threshold = trackWidth * 0.35f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput

                detectHorizontalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            val currentOffset = offsetX.value

                            when {
                                currentOffset < -threshold -> {
                                    // Snap to left
                                    offsetX.animateTo(
                                        -trackWidth / 2,
                                        animationSpec = tween(300)
                                    )
                                    onSkip()
                                }
                                currentOffset > threshold -> {
                                    // Snap to right
                                    offsetX.animateTo(
                                        trackWidth / 2,
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
                                .coerceIn(-trackWidth / 2, trackWidth / 2)
                            offsetX.snapTo(newValue)
                        }
                    }
                )
            }
    ) {
        // Measure track width
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            trackWidth = with(density) { constraints.maxWidth.toFloat() }

            // Left label (Skip)
            Text(
                text = "Skip",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (offsetX.value < 0) 0.8f else 0.3f
                ),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
            )

            // Right label (Done)
            Text(
                text = "Done",
                color = MaterialTheme.colorScheme.primary.copy(
                    alpha = if (offsetX.value > 0) 0.8f else 0.3f
                ),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
            )

            // Thumb
            val thumbX = with(density) {
                (offsetX.value + trackWidth / 2).toDp()
            }

            Box(
                modifier = Modifier
                    .offset(x = thumbX - 24.dp)
                    .align(Alignment.CenterStart)
                    .size(48.dp)
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
