package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.model.Habit
import com.example.data.model.HabitCategory
import com.example.data.model.HabitCompletion
import com.example.data.model.HabitPriority
import com.example.data.model.ScheduleType
import com.example.domain.HabitAnalyticsCalculator
import com.example.domain.HabitDateUtils
import com.example.ui.components.PriorityBadge
import com.example.ui.components.StreakPill
import com.example.ui.components.categoryIcon
import com.example.ui.components.parseHexColor
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.IndigoContainer
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.MintContainer
import com.example.ui.theme.MintSuccess
import com.example.ui.viewmodel.HabitFlowDashboardData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    data: HabitFlowDashboardData,
    openCreateInitially: Boolean = false,
    onConsumeOpenCreate: () -> Unit = {},
    onSaveHabit: (
        habitId: String?,
        name: String,
        description: String,
        category: String,
        scheduleType: ScheduleType,
        targetDays: List<Long>,
        targetFrequency: Long,
        priority: HabitPriority,
        startDate: String,
        reminderTime: String,
        remindersEnabled: Boolean,
        paused: Boolean,
        colorHex: String
    ) -> Unit,
    onTogglePaused: (Habit) -> Unit,
    onDeleteHabit: (Habit) -> Unit,
    onOpenTracker: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("All") } // All, Active, Paused
    var selectedCategory by remember { mutableStateOf("All") }
    var editingHabit by remember { mutableStateOf<Habit?>(null) }
    var showSheet by remember { mutableStateOf(openCreateInitially) }
    var habitToDelete by remember { mutableStateOf<Habit?>(null) }

    if (openCreateInitially && !showSheet) {
        showSheet = true
        onConsumeOpenCreate()
    }

    val filteredHabits = data.habits.filter { habit ->
        val matchesQuery = searchQuery.isBlank() ||
            habit.name.contains(searchQuery, ignoreCase = true) ||
            habit.description.contains(searchQuery, ignoreCase = true)
        val matchesStatus = when (statusFilter) {
            "Active" -> !habit.paused
            "Paused" -> habit.paused
            else -> true
        }
        val matchesCat = selectedCategory == "All" || habit.category.equals(selectedCategory, ignoreCase = true)
        matchesQuery && matchesStatus && matchesCat
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingHabit = null
                    showSheet = true
                },
                containerColor = IndigoPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_new_habit")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Create new habit")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("My Habits", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = "${data.habits.count { !it.paused }} active · ${data.habits.count { it.paused }} paused",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(onClick = onOpenTracker, shape = RoundedCornerShape(12.dp)) {
                        Text("Daily Tracker")
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search habits...") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = "Search") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "Active", "Paused").forEach { status ->
                        FilterChip(
                            selected = statusFilter == status,
                            onClick = { statusFilter = status },
                            label = { Text(status) }
                        )
                    }
                    data.categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat.name,
                            onClick = {
                                selectedCategory = if (selectedCategory == cat.name) "All" else cat.name
                            },
                            label = { Text(cat.name) }
                        )
                    }
                }
            }

            if (filteredHabits.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No habits match this view", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap + to create a habit with custom schedules, priorities, and reminders.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredHabits, key = { it.id }) { habit ->
                    val streak = HabitAnalyticsCalculator.calculateHabitStreak(
                        habit = habit,
                        completions = data.completions,
                        todayStr = data.todayStr
                    ).first
                    val accent = parseHexColor(habit.colorHex)
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (habit.paused) {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(accent.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = categoryIcon(habit.category),
                                        contentDescription = habit.category,
                                        tint = accent
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = habit.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (habit.paused) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = AmberContainer
                                            ) {
                                                Text(
                                                    text = "Paused",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF92400E),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${habit.category} · ${ScheduleType.fromRaw(habit.scheduleType).label} · ${habit.targetFrequency}x/day",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                StreakPill(streakDays = streak)
                            }

                            if (habit.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = habit.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    PriorityBadge(habit.priority)
                                    if (habit.remindersEnabled && habit.reminderTime.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = IndigoContainer
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    Icons.Outlined.Notifications,
                                                    contentDescription = "Reminder",
                                                    tint = IndigoPrimary,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = habit.reminderTime,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = IndigoPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                                Row {
                                    IconButton(onClick = { onTogglePaused(habit) }) {
                                        Icon(
                                            imageVector = if (habit.paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                            contentDescription = if (habit.paused) "Reactivate habit" else "Pause habit"
                                        )
                                    }
                                    IconButton(onClick = {
                                        editingHabit = habit
                                        showSheet = true
                                    }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit habit")
                                    }
                                    IconButton(onClick = { habitToDelete = habit }) {
                                        Icon(
                                            Icons.Filled.DeleteOutline,
                                            contentDescription = "Delete habit",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showSheet = false
                onConsumeOpenCreate()
            }
        ) {
            CreateEditHabitForm(
                initialHabit = editingHabit,
                categories = data.categories,
                onCancel = {
                    showSheet = false
                    onConsumeOpenCreate()
                },
                onSave = { name, desc, cat, sched, days, freq, prio, start, remTime, remOn, paused, color ->
                    onSaveHabit(
                        editingHabit?.id,
                        name, desc, cat, sched, days, freq, prio, start, remTime, remOn, paused, color
                    )
                    showSheet = false
                    onConsumeOpenCreate()
                }
            )
        }
    }

    habitToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { habitToDelete = null },
            title = { Text("Delete \"${target.name}\"?") },
            text = { Text("This removes the habit from your active list. Past completion records remain in your history.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteHabit(target)
                    habitToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { habitToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CreateEditHabitForm(
    initialHabit: Habit?,
    categories: List<HabitCategory>,
    onCancel: () -> Unit,
    onSave: (
        name: String,
        description: String,
        category: String,
        scheduleType: ScheduleType,
        targetDays: List<Long>,
        targetFrequency: Long,
        priority: HabitPriority,
        startDate: String,
        reminderTime: String,
        remindersEnabled: Boolean,
        paused: Boolean,
        colorHex: String
    ) -> Unit
) {
    var name by remember { mutableStateOf(initialHabit?.name ?: "") }
    var description by remember { mutableStateOf(initialHabit?.description ?: "") }
    var category by remember { mutableStateOf(initialHabit?.category ?: categories.firstOrNull()?.name ?: "Health") }
    var scheduleType by remember { mutableStateOf(ScheduleType.fromRaw(initialHabit?.scheduleType)) }
    val selectedDays = remember {
        mutableStateListOf<Long>().apply {
            addAll(initialHabit?.targetDays?.ifEmpty { listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L) }
                ?: listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L))
        }
    }
    var targetFrequency by remember { mutableIntStateOf((initialHabit?.targetFrequency ?: 1L).toInt()) }
    var priority by remember { mutableStateOf(HabitPriority.fromRaw(initialHabit?.priority)) }
    var startDate by remember { mutableStateOf(initialHabit?.startDate?.ifBlank { HabitDateUtils.todayIso() } ?: HabitDateUtils.todayIso()) }
    var remindersEnabled by remember { mutableStateOf(initialHabit?.remindersEnabled ?: false) }
    var reminderTime by remember { mutableStateOf(initialHabit?.reminderTime?.ifBlank { "08:00" } ?: "08:00") }
    var colorHex by remember { mutableStateOf(initialHabit?.colorHex ?: "#6C63FF") }

    val colorChoices = listOf("#6C63FF", "#A78BFA", "#22C55E", "#0EA5E9", "#F59E0B", "#EC4899")
    val dayLabels = listOf(1L to "M", 2L to "T", 3L to "W", 4L to "T", 5L to "F", 6L to "S", 7L to "S")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = if (initialHabit == null) "Create Habit" else "Edit Habit",
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Habit Name") },
            placeholder = { Text("e.g., Morning Hydration") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("habit_name_input")
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description (optional)") },
            maxLines = 2,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Text("Category", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                FilterChip(
                    selected = category.equals(cat.name, ignoreCase = true),
                    onClick = {
                        category = cat.name
                        colorHex = cat.colorHex
                    },
                    label = { Text(cat.name) }
                )
            }
        }

        Text("Schedule", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ScheduleType.entries.forEach { type ->
                FilterChip(
                    selected = scheduleType == type,
                    onClick = { scheduleType = type },
                    label = { Text(type.label) }
                )
            }
        }

        if (scheduleType != ScheduleType.DAILY) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dayLabels.forEach { (dayNum, label) ->
                    val isSelected = selectedDays.contains(dayNum)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) IndigoPrimary else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                if (isSelected && selectedDays.size > 1) selectedDays.remove(dayNum)
                                else if (!isSelected) selectedDays.add(dayNum)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Daily Target Frequency", style = MaterialTheme.typography.labelLarge)
                Text("Completions needed per day", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { if (targetFrequency > 1) targetFrequency-- },
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(38.dp)
                ) { Text("-") }
                Text("$targetFrequency", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(
                    onClick = { if (targetFrequency < 10) targetFrequency++ },
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(38.dp)
                ) { Text("+") }
            }
        }

        Text("Priority", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HabitPriority.entries.forEach { p ->
                FilterChip(
                    selected = priority == p,
                    onClick = { priority = p },
                    label = { Text(p.label) }
                )
            }
        }

        OutlinedTextField(
            value = startDate,
            onValueChange = { if (it.length <= 10) startDate = it },
            label = { Text("Start Date (YYYY-MM-DD)") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Reminder Notification", style = MaterialTheme.typography.labelLarge)
            Switch(checked = remindersEnabled, onCheckedChange = { remindersEnabled = it })
        }

        if (remindersEnabled) {
            OutlinedTextField(
                value = reminderTime,
                onValueChange = { if (it.length <= 5) reminderTime = it },
                label = { Text("Reminder Time (HH:mm)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Text("Accent Color", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            colorChoices.forEach { hex ->
                val c = parseHexColor(hex)
                val selected = colorHex.equals(hex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(c)
                        .then(
                            if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            else Modifier
                        )
                        .clickable { colorHex = hex }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    val daysToSave = if (scheduleType == ScheduleType.DAILY) {
                        listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L)
                    } else {
                        selectedDays.toList().sorted()
                    }
                    onSave(
                        name,
                        description,
                        category,
                        scheduleType,
                        daysToSave,
                        targetFrequency.toLong(),
                        priority,
                        startDate,
                        if (remindersEnabled) reminderTime else "",
                        remindersEnabled,
                        initialHabit?.paused ?: false,
                        colorHex
                    )
                },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary, contentColor = Color.White),
                modifier = Modifier
                    .weight(1f)
                    .testTag("save_habit_button")
            ) {
                Text(if (initialHabit == null) "Create Habit" else "Save Changes")
            }
        }
    }
}

@Composable
fun TrackerScreen(
    data: HabitFlowDashboardData,
    selectedDate: String,
    onSelectDate: (String) -> Unit,
    onToggleHabit: (Habit, String) -> Unit,
    onUpdateNotes: (HabitCompletion, String) -> Unit
) {
    val recentDays = HabitDateUtils.recentDates(data.todayStr, 7)
    val dateStats = HabitAnalyticsCalculator.calculateDailyStats(data.habits, data.completions, selectedDate)
    val scheduled = data.habits.filter { HabitAnalyticsCalculator.isHabitScheduledOn(it, selectedDate) }
    val completionMap = data.completions.filter { it.date == selectedDate }.associateBy { it.habitId }

    val completedList = scheduled.filter {
        val c = completionMap[it.id]
        c != null && c.completedCount >= it.targetFrequency
    }
    val pendingList = scheduled.filter {
        val c = completionMap[it.id]
        c == null || c.completedCount < it.targetFrequency
    }

    var notesTarget by remember { mutableStateOf<HabitCompletion?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text("Daily Tracker", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = HabitDateUtils.displayDateLabel(selectedDate),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 7-Day Date Strip
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                recentDays.forEach { d ->
                    val isSelected = d == selectedDate
                    val stats = HabitAnalyticsCalculator.calculateDailyStats(data.habits, data.completions, d)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) IndigoPrimary else MaterialTheme.colorScheme.surface
                            )
                            .clickable { onSelectDate(d) }
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = HabitDateUtils.shortDayLabel(d).take(3),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = d.takeLast(2),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        stats.totalScheduled > 0 && stats.completed == stats.totalScheduled -> MintSuccess
                                        stats.completed > 0 -> if (isSelected) Color.White else IndigoPrimary
                                        else -> Color.Transparent
                                    }
                                )
                        )
                    }
                }
            }
        }

        // Daily Progress Summary
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${dateStats.completed} of ${dateStats.totalScheduled} completed",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${dateStats.percentage}%",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = IndigoPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { dateStats.percentage / 100f },
                        color = if (dateStats.percentage == 100) MintSuccess else IndigoPrimary,
                        trackColor = IndigoContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(50))
                    )
                    if (selectedDate < data.todayStr && pendingList.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${pendingList.size} missed on this day — no worries, every day is a fresh opportunity!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (pendingList.isNotEmpty()) {
            item {
                Text(
                    text = if (selectedDate < data.todayStr) "Missed / Incomplete" else "Pending Today",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            items(pendingList, key = { "pending_${it.id}" }) { habit ->
                val comp = completionMap[habit.id]
                val count = comp?.completedCount ?: 0L
                val accent = parseHexColor(habit.colorHex)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onToggleHabit(habit, selectedDate) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .border(2.dp, accent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$count",
                                style = MaterialTheme.typography.labelSmall,
                                color = accent
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(habit.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${habit.category} · $count/${habit.targetFrequency}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        PriorityBadge(habit.priority)
                    }
                }
            }
        }

        if (completedList.isNotEmpty()) {
            item {
                Text(
                    text = "Completed",
                    style = MaterialTheme.typography.titleMedium,
                    color = MintSuccess
                )
            }
            items(completedList, key = { "done_${it.id}" }) { habit ->
                val comp = completionMap[habit.id]!!
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MintContainer.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MintSuccess),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Check, contentDescription = "Done", tint = Color.White)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = habit.name,
                                    style = MaterialTheme.typography.titleMedium.copy(textDecoration = TextDecoration.LineThrough)
                                )
                                Text(
                                    text = if (comp.notes.isNotBlank()) "Note: ${comp.notes}" else "Tap note icon to add reflection",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { notesTarget = comp }) {
                                Icon(Icons.Outlined.EditNote, contentDescription = "Add or edit completion note")
                            }
                            IconButton(onClick = { onToggleHabit(habit, selectedDate) }) {
                                Icon(Icons.Outlined.Undo, contentDescription = "Undo completion")
                            }
                        }
                    }
                }
            }
        }
    }

    notesTarget?.let { comp ->
        var noteText by remember(comp.id) { mutableStateOf(comp.notes) }
        AlertDialog(
            onDismissRequest = { notesTarget = null },
            title = { Text("Completion Note · ${comp.habitName}") },
            text = {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { if (it.length <= 300) noteText = it },
                    label = { Text("How did it go?") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateNotes(comp, noteText)
                    notesTarget = null
                }) {
                    Text("Save Note")
                }
            },
            dismissButton = {
                TextButton(onClick = { notesTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
