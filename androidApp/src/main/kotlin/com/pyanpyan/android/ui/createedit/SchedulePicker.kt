package com.pyanpyan.android.ui.createedit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.pyanpyan.domain.model.TimeRange
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

@Composable
fun SchedulePicker(
    daysOfWeek: Set<DayOfWeek>,
    timeRange: TimeRange,
    onDaysChange: (Set<DayOfWeek>) -> Unit,
    onTimeRangeChange: (TimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    // Initialize with all days and all day when expanded for the first time
    LaunchedEffect(expanded) {
        if (expanded && daysOfWeek.isEmpty()) {
            onDaysChange(DayOfWeek.entries.toSet())
            onTimeRangeChange(TimeRange.AllDay)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (expanded) "Schedule ▼" else "Schedule ▶",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.clickable { expanded = !expanded }
            )

            if (expanded) {
                // Days of Week
                Text(
                    text = "Days of Week",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                DayOfWeekChipRow(
                    selectedDays = daysOfWeek,
                    onSelectionChange = onDaysChange
                )

                Spacer(modifier = Modifier.padding(4.dp))

                TimeRangePicker(
                    timeRange = timeRange,
                    onChange = onTimeRangeChange
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayOfWeekChipRow(
    selectedDays: Set<DayOfWeek>,
    onSelectionChange: (Set<DayOfWeek>) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val days = listOf(
            DayOfWeek.MONDAY to "M",
            DayOfWeek.TUESDAY to "T",
            DayOfWeek.WEDNESDAY to "W",
            DayOfWeek.THURSDAY to "T",
            DayOfWeek.FRIDAY to "F",
            DayOfWeek.SATURDAY to "S",
            DayOfWeek.SUNDAY to "S"
        )

        days.forEach { (day, label) ->
            FilterChip(
                selected = day in selectedDays,
                onClick = {
                    val newSelection = if (day in selectedDays) {
                        selectedDays - day
                    } else {
                        selectedDays + day
                    }
                    onSelectionChange(newSelection)
                },
                label = { Text(label) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeRangePicker(
    timeRange: TimeRange,
    onChange: (TimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAllDay = timeRange is TimeRange.AllDay
    val startTime = if (timeRange is TimeRange.Specific) timeRange.startTime else LocalTime(9, 0)
    val endTime = if (timeRange is TimeRange.Specific) timeRange.endTime else LocalTime(17, 0)

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // All Day Radio
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            RadioButton(
                selected = isAllDay,
                onClick = { onChange(TimeRange.AllDay) },
                colors = RadioButtonDefaults.colors(
                    selectedColor = Color(0xFF1B5E20)
                )
            )
            Text(
                text = "All Day",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Specific Time Radio with inline time pickers
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            RadioButton(
                selected = !isAllDay,
                onClick = {
                    onChange(TimeRange.Specific(startTime, endTime))
                },
                colors = RadioButtonDefaults.colors(
                    selectedColor = Color(0xFF1B5E20)
                )
            )
            Text(
                text = "From",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp)
            )
            OutlinedButton(
                onClick = { showStartPicker = true },
                enabled = !isAllDay,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(36.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.Black
                )
            ) {
                Text(
                    text = formatTime(startTime),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = "To",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            OutlinedButton(
                onClick = { showEndPicker = true },
                enabled = !isAllDay,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(36.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.Black
                )
            ) {
                Text(
                    text = formatTime(endTime),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    // Start Time Picker Dialog
    if (showStartPicker) {
        TimePickerDialog(
            initialTime = startTime,
            onDismiss = { showStartPicker = false },
            onConfirm = { newTime ->
                onChange(TimeRange.Specific(newTime, endTime))
                showStartPicker = false
            }
        )
    }

    // End Time Picker Dialog
    if (showEndPicker) {
        TimePickerDialog(
            initialTime = endTime,
            onDismiss = { showEndPicker = false },
            onConfirm = { newTime ->
                onChange(TimeRange.Specific(startTime, newTime))
                showEndPicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialTime: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = false
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Select Time",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                TimePicker(state = timePickerState)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    TextButton(
                        onClick = {
                            val newTime = LocalTime(
                                timePickerState.hour,
                                timePickerState.minute
                            )
                            onConfirm(newTime)
                        }
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

private fun formatTime(time: LocalTime): String {
    val hour = if (time.hour == 0) 12 else if (time.hour > 12) time.hour - 12 else time.hour
    val amPm = if (time.hour < 12) "AM" else "PM"
    return String.format("%d:%02d %s", hour, time.minute, amPm)
}
