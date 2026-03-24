package com.pyanpyan.android.ui.checklist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pyanpyan.android.ui.components.ItemSlider
import com.pyanpyan.android.ui.components.SliderState
import com.pyanpyan.domain.model.ChecklistId
import com.pyanpyan.domain.model.ChecklistItem
import com.pyanpyan.domain.model.ChecklistItemState
import com.pyanpyan.domain.repository.ChecklistRepository
import com.pyanpyan.domain.repository.SettingsRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistScreen(
    checklistId: ChecklistId,
    onBackClick: () -> Unit,
    repository: ChecklistRepository,
    settingsRepository: SettingsRepository
) {
    val context = LocalContext.current

    val viewModel: ChecklistViewModel = viewModel(
        key = checklistId.value,
        factory = viewModelFactory {
            initializer {
                ChecklistViewModel(checklistId, repository, context, settingsRepository)
            }
        }
    )

    val uiState by viewModel.uiState.collectAsState()
    val settings by settingsRepository.settings.collectAsState(initial = com.pyanpyan.domain.model.AppSettings())

    // Timer lifecycle management
    LaunchedEffect(uiState.checklist?.id) {
        val checklist = uiState.checklist
        val timerDuration = checklist?.timerDurationMinutes

        if (timerDuration != null) {
            val currentState = uiState.timerState
            if (currentState is TimerState.Paused) {
                viewModel.resumeTimer()
            } else if (currentState is TimerState.NotConfigured) {
                viewModel.startTimer(timerDuration)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.pauseTimer()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = uiState.checklist?.let { Color(android.graphics.Color.parseColor(it.color.hex)) }
            ?: MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TopAppBar with back button
            TopAppBar(
                title = { Text("Checklist") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )

            // Existing content wrapped in Column
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                uiState.checklist?.let { checklist ->
                    Text(
                        text = checklist.name,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Timer display
                    if (uiState.timerState !is TimerState.NotConfigured &&
                        uiState.timerState !is TimerState.Paused) {
                        TimerCard(
                            timerState = uiState.timerState,
                            onDismiss = { viewModel.dismissTimer() },
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(
                            items = checklist.items,
                            key = { it.id.value }
                        ) { item ->
                            ChecklistItemRow(
                                item = item,
                                onMarkDone = { viewModel.markItemDone(item.id) },
                                onIgnoreToday = { viewModel.ignoreItemToday(item.id) },
                                onReset = { viewModel.resetItem(item.id) },
                                enableHaptic = settings.enableHapticFeedback
                            )
                        }
                    }

                    // Last accessed timestamp
                    val lastAccessedText = formatRelativeTime(checklist.lastAccessedAt)
                    if (lastAccessedText.isNotEmpty()) {
                        Text(
                            text = lastAccessedText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChecklistItemRow(
    item: ChecklistItem,
    onMarkDone: () -> Unit,
    onIgnoreToday: () -> Unit,
    onReset: () -> Unit,
    enableHaptic: Boolean = true
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (item.state) {
                ChecklistItemState.Done -> MaterialTheme.colorScheme.primaryContainer
                ChecklistItemState.IgnoredToday -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ChecklistItemState.Pending -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Icon and title (70% width)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(0.7f)
            ) {
                // Icon
                item.iconId?.let { iconId ->
                    Icon(
                        imageVector = getIconForItemId(iconId.value),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = when (item.state) {
                            ChecklistItemState.Done -> Color(0xFF1B5E20)
                            ChecklistItemState.IgnoredToday -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            ChecklistItemState.Pending -> Color(0xFF1B5E20)
                        }
                    )
                }

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = when (item.state) {
                        ChecklistItemState.Done -> TextDecoration.LineThrough
                        else -> null
                    },
                    color = when (item.state) {
                        ChecklistItemState.IgnoredToday -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }

            // Right side: Slider (30% width)
            Box(
                modifier = Modifier.weight(0.3f)
            ) {
                ItemSlider(
                    state = when (item.state) {
                        ChecklistItemState.Pending -> SliderState.Center
                        ChecklistItemState.Done -> SliderState.Right
                        ChecklistItemState.IgnoredToday -> SliderState.Left
                    },
                    onSkip = onIgnoreToday,
                    onDone = onMarkDone,
                    enabled = true,
                    enableHaptic = enableHaptic,
                    onReset = onReset
                )
            }
        }
    }
}

fun getIconForItemId(iconId: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (iconId) {
        // Food & Drink
        "restaurant" -> Icons.Filled.Restaurant
        "local_cafe" -> Icons.Filled.LocalCafe
        "local_bar" -> Icons.Filled.LocalBar
        "lunch_dining" -> Icons.Filled.LunchDining
        "breakfast_dining" -> Icons.Filled.BreakfastDining
        "dinner_dining" -> Icons.Filled.DinnerDining
        "local_pizza" -> Icons.Filled.LocalPizza
        "cake" -> Icons.Filled.Cake
        "water_drop" -> Icons.Filled.WaterDrop

        // Health & Fitness
        "fitness_center" -> Icons.Filled.FitnessCenter
        "directions_run" -> Icons.Filled.DirectionsRun
        "self_improvement" -> Icons.Filled.SelfImprovement
        "spa" -> Icons.Filled.Spa
        "favorite" -> Icons.Filled.Favorite
        "medical_services" -> Icons.Filled.MedicalServices
        "pool" -> Icons.Filled.Pool
        "sports_soccer" -> Icons.Filled.SportsSoccer

        // Home & Living
        "home" -> Icons.Filled.Home
        "bed" -> Icons.Filled.Bed
        "weekend" -> Icons.Filled.Weekend
        "shower" -> Icons.Filled.Shower
        "clean_hands" -> Icons.Filled.CleanHands
        "cleaning_services" -> Icons.Filled.CleaningServices
        "light" -> Icons.Filled.Light
        "kitchen" -> Icons.Filled.Kitchen

        // Work & Study
        "work" -> Icons.Filled.Work
        "school" -> Icons.Filled.School
        "menu_book" -> Icons.Filled.MenuBook
        "computer" -> Icons.Filled.Computer
        "edit" -> Icons.Filled.Edit
        "folder" -> Icons.Filled.Folder
        "assignment" -> Icons.Filled.Assignment
        "laptop" -> Icons.Filled.Laptop

        // Travel & Transport
        "directions_car" -> Icons.Filled.DirectionsCar
        "directions_bus" -> Icons.Filled.DirectionsBus
        "directions_bike" -> Icons.Filled.DirectionsBike
        "flight" -> Icons.Filled.Flight
        "train" -> Icons.Filled.Train
        "local_shipping" -> Icons.Filled.LocalShipping

        // Entertainment
        "movie" -> Icons.Filled.Movie
        "music_note" -> Icons.Filled.MusicNote
        "headphones" -> Icons.Filled.Headphones
        "videogame_asset" -> Icons.Filled.VideogameAsset
        "theaters" -> Icons.Filled.Theaters
        "sports_esports" -> Icons.Filled.SportsEsports

        // Shopping
        "shopping_cart" -> Icons.Filled.ShoppingCart
        "shopping_bag" -> Icons.Filled.ShoppingBag
        "local_grocery_store" -> Icons.Filled.LocalGroceryStore
        "store" -> Icons.Filled.Store
        "receipt" -> Icons.Filled.Receipt

        // Nature & Animals
        "pets" -> Icons.Filled.Pets
        "park" -> Icons.Filled.Park
        "eco" -> Icons.Filled.Eco
        "forest" -> Icons.Filled.Forest
        "yard" -> Icons.Filled.Yard

        // People & Communication
        "person" -> Icons.Filled.Person
        "people" -> Icons.Filled.People
        "family_restroom" -> Icons.Filled.FamilyRestroom
        "child_care" -> Icons.Filled.ChildCare
        "phone" -> Icons.Filled.Phone
        "email" -> Icons.Filled.Email
        "chat" -> Icons.Filled.Chat

        // Time & Planning
        "calendar_today" -> Icons.Filled.CalendarToday
        "schedule" -> Icons.Filled.Schedule
        "alarm" -> Icons.Filled.Alarm
        "timer" -> Icons.Filled.Timer
        "access_time" -> Icons.Filled.AccessTime

        // Tasks & Actions
        "check_circle" -> Icons.Filled.CheckCircle
        "done" -> Icons.Filled.Done
        "task_alt" -> Icons.Filled.TaskAlt
        "star" -> Icons.Filled.Star
        "bookmark" -> Icons.Filled.Bookmark
        "flag" -> Icons.Filled.Flag

        // Other Useful
        "notifications" -> Icons.Filled.Notifications
        "location_on" -> Icons.Filled.LocationOn
        "attach_money" -> Icons.Filled.AttachMoney
        "volunteer_activism" -> Icons.Filled.VolunteerActivism
        "celebration" -> Icons.Filled.Celebration
        "card_giftcard" -> Icons.Filled.CardGiftcard

        // Legacy icons for backwards compatibility
        "settings" -> Icons.Filled.Settings
        "account" -> Icons.Filled.AccountCircle
        "calendar" -> Icons.Filled.DateRange
        "location" -> Icons.Filled.LocationOn
        "search" -> Icons.Filled.Search
        "info" -> Icons.Filled.Info
        "warning" -> Icons.Filled.Warning
        "lock" -> Icons.Filled.Lock
        "arrow_forward" -> Icons.Filled.ArrowForward
        "arrow_back" -> Icons.Filled.ArrowBack
        "refresh" -> Icons.Filled.Refresh

        else -> Icons.Filled.Circle
    }
}

@Composable
fun TimerCard(
    timerState: TimerState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (timerState) {
        is TimerState.Running -> {
            Card(
                modifier = modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⏱️",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Timer: ${formatTime(timerState.remainingSeconds)}",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }
        is TimerState.Expired -> {
            Card(
                modifier = modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "⚠️",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Timer expired!",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Dismiss")
                    }
                }
            }
        }
        is TimerState.Completed -> {
            Card(
                modifier = modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✅",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Completed with ${formatTime(timerState.remainingSeconds)} remaining",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        else -> {
            // NotConfigured or Paused - don't show card
        }
    }
}

private fun formatTime(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

fun formatRelativeTime(instant: Instant?): String {
    if (instant == null) return ""

    val now = Clock.System.now()
    val duration = now - instant

    return when {
        duration < 1.minutes -> "Last accessed just now"
        duration < 1.hours -> {
            val mins = duration.inWholeMinutes
            "Last accessed $mins minute${if (mins == 1L) "" else "s"} ago"
        }
        duration < 24.hours -> {
            val hrs = duration.inWholeHours
            "Last accessed $hrs hour${if (hrs == 1L) "" else "s"} ago"
        }
        duration < 7.days -> {
            val days = duration.inWholeDays
            "Last accessed $days day${if (days == 1L) "" else "s"} ago"
        }
        else -> {
            val date = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
            "Last accessed on ${date.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${date.dayOfMonth}"
        }
    }
}
