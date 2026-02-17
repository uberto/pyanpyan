package com.pyanpyan.android.ui.createedit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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

                // Time Range
                Text(
                    text = "Time Range",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )

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

@Composable
fun TimeRangePicker(
    timeRange: TimeRange,
    onChange: (TimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    val isAllDay = timeRange is TimeRange.AllDay
    val startTime = if (timeRange is TimeRange.Specific) timeRange.startTime else LocalTime(9, 0)
    val endTime = if (timeRange is TimeRange.Specific) timeRange.endTime else LocalTime(17, 0)

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
                onClick = { onChange(TimeRange.AllDay) }
            )
            Text(
                text = "All Day",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Specific Time Radio
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            RadioButton(
                selected = !isAllDay,
                onClick = {
                    onChange(TimeRange.Specific(startTime, endTime))
                }
            )
            Text(
                text = "Specific Time",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Time Pickers (only visible when Specific selected)
        if (!isAllDay) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = formatTime(startTime),
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Start") },
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showStartPicker = true },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    )
                )

                OutlinedTextField(
                    value = formatTime(endTime),
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("End") },
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showEndPicker = true },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    )
                )
            }
        }
    }

    // Time picker dialogs would be implemented with Material3 TimePicker
    // For now, using simple dialogs (full implementation in actual code)
    if (showStartPicker) {
        // TODO: Implement Material3 TimePicker dialog
        showStartPicker = false
    }

    if (showEndPicker) {
        // TODO: Implement Material3 TimePicker dialog
        showEndPicker = false
    }
}

private fun formatTime(time: LocalTime): String {
    val hour = if (time.hour == 0) 12 else if (time.hour > 12) time.hour - 12 else time.hour
    val amPm = if (time.hour < 12) "AM" else "PM"
    return String.format("%d:%02d %s", hour, time.minute, amPm)
}
