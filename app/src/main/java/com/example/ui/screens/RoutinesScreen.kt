package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.Routine
import com.example.data.model.RoutineTimeOfDay
import com.example.domain.HabitDurationFormatter
import com.example.ui.theme.IndigoContainer
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.MintContainer
import com.example.ui.theme.MintSuccess
import com.example.ui.viewmodel.HabitFlowDashboardData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinesScreen(
    data: HabitFlowDashboardData,
    onToggleHabitToday: (Habit) -> Unit,
    onStartTimer: (Habit) -> Unit,
    onReorderHabit: (Routine, Int, Int) -> Unit,
    onSaveRoutine: (
        routineId: String?,
        name: String,
        timeOfDay: RoutineTimeOfDay,
        startTime: String,
        habitIds: List<String>,
        remindersEnabled: Boolean
    ) -> Unit,
    onDeleteRoutine: (Routine) -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }
    var editingRoutine by remember { mutableStateOf<Routine?>(null) }

    val habitById = data.habits.associateBy { it.id }
    val todayCompletions = data.completions.filter { it.date == data.todayStr }.associateBy { it.habitId }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingRoutine = null
                    showSheet = true
                },
                containerColor = IndigoPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_new_routine")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Create Routine")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column {
                    Text("Routine Builder", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        text = "Chain morning, afternoon, and evening habits in order",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (data.routines.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = "No routines yet",
                                tint = IndigoPrimary,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No routines yet", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Group your habits into Morning, Afternoon, or Evening flows and reorder steps anytime.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    editingRoutine = null
                                    showSheet = true
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary, contentColor = Color.White)
                            ) {
                                Text("Build First Routine")
                            }
                        }
                    }
                }
            } else {
                items(data.routines, key = { it.id }) { routine ->
                    val timeOfDay = RoutineTimeOfDay.fromRaw(routine.timeOfDay)
                    val resolvedHabits = routine.habitIds.mapNotNull { habitById[it] }
                    val completedInRoutine = resolvedHabits.count { h ->
                        val c = todayCompletions[h.id]
                        c != null && c.completedCount >= h.targetFrequency
                    }
                    val progress = if (resolvedHabits.isEmpty()) 0f else completedInRoutine.toFloat() / resolvedHabits.size.toFloat()

                    val slotIcon = when (timeOfDay) {
                        RoutineTimeOfDay.MORNING -> Icons.Outlined.WbSunny
                        RoutineTimeOfDay.AFTERNOON -> Icons.Outlined.WbTwilight
                        RoutineTimeOfDay.EVENING -> Icons.Outlined.NightsStay
                        RoutineTimeOfDay.CUSTOM -> Icons.Outlined.Schedule
                    }

                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                        .background(IndigoContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(slotIcon, contentDescription = timeOfDay.label, tint = IndigoPrimary)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(routine.name, style = MaterialTheme.typography.titleLarge)
                                    Text(
                                        text = "${timeOfDay.label} · Starts ${routine.startTime} · $completedInRoutine/${resolvedHabits.size} done",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = {
                                    editingRoutine = routine
                                    showSheet = true
                                }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit routine")
                                }
                                IconButton(onClick = { onDeleteRoutine(routine) }) {
                                    Icon(
                                        Icons.Filled.DeleteOutline,
                                        contentDescription = "Delete routine",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                color = if (progress >= 0.99f && resolvedHabits.isNotEmpty()) MintSuccess else IndigoPrimary,
                                trackColor = IndigoContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50))
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            if (resolvedHabits.isEmpty()) {
                                Text(
                                    text = "No habits linked yet. Tap Edit to add habits to this routine.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    resolvedHabits.forEachIndexed { index, habit ->
                                        val comp = todayCompletions[habit.id]
                                        val isDone = (comp?.completedCount ?: 0L) >= habit.targetFrequency
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isDone) MintContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .clickable { onToggleHabitToday(habit) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isDone) MintSuccess else IndigoPrimary.copy(alpha = 0.14f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (isDone) {
                                                        Icon(Icons.Filled.Check, contentDescription = "Done", tint = Color.White, modifier = Modifier.size(16.dp))
                                                    } else {
                                                        Text(
                                                            text = "${index + 1}",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                            color = IndigoPrimary
                                                        )
                                                    }
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = habit.name,
                                                        style = MaterialTheme.typography.titleSmall.copy(
                                                            textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                                                        )
                                                    )
                                                    val secs = comp?.durationSeconds ?: 0L
                                                    Text(
                                                        text = if (secs > 0L) {
                                                            "${habit.category} · ${HabitDurationFormatter.formatCompact(secs)} tracked"
                                                        } else {
                                                            habit.category
                                                        },
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { onStartTimer(habit) },
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .testTag("routine_timer_${routine.id}_${habit.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Timer,
                                                        contentDescription = "Start stopwatch timer for ${habit.name}",
                                                        tint = IndigoPrimary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                if (index > 0) {
                                                    IconButton(
                                                        onClick = { onReorderHabit(routine, index, index - 1) },
                                                        modifier = Modifier.size(34.dp)
                                                    ) {
                                                        Icon(Icons.Filled.ArrowUpward, contentDescription = "Move up", modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                                if (index < resolvedHabits.lastIndex) {
                                                    IconButton(
                                                        onClick = { onReorderHabit(routine, index, index + 1) },
                                                        modifier = Modifier.size(34.dp)
                                                    ) {
                                                        Icon(Icons.Filled.ArrowDownward, contentDescription = "Move down", modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
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
        ModalBottomSheet(onDismissRequest = { showSheet = false }) {
            CreateEditRoutineForm(
                initialRoutine = editingRoutine,
                allHabits = data.habits,
                onCancel = { showSheet = false },
                onSave = { name, timeOfDay, startTime, habitIds, remOn ->
                    onSaveRoutine(editingRoutine?.id, name, timeOfDay, startTime, habitIds, remOn)
                    showSheet = false
                }
            )
        }
    }
}

@Composable
private fun CreateEditRoutineForm(
    initialRoutine: Routine?,
    allHabits: List<Habit>,
    onCancel: () -> Unit,
    onSave: (String, RoutineTimeOfDay, String, List<String>, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initialRoutine?.name ?: "") }
    var timeOfDay by remember { mutableStateOf(RoutineTimeOfDay.fromRaw(initialRoutine?.timeOfDay)) }
    var startTime by remember { mutableStateOf(initialRoutine?.startTime ?: timeOfDay.defaultTime) }
    var remindersEnabled by remember { mutableStateOf(initialRoutine?.remindersEnabled ?: false) }
    val selectedIds = remember {
        mutableStateListOf<String>().apply {
            initialRoutine?.habitIds?.let { addAll(it) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (initialRoutine == null) "Create Routine" else "Edit Routine",
            style = MaterialTheme.typography.headlineSmall
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Routine Name") },
            placeholder = { Text("e.g., Energized Morning") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Text("Time of Day", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RoutineTimeOfDay.entries.forEach { slot ->
                FilterChip(
                    selected = timeOfDay == slot,
                    onClick = {
                        timeOfDay = slot
                        if (initialRoutine == null) startTime = slot.defaultTime
                    },
                    label = { Text(slot.label) }
                )
            }
        }
        OutlinedTextField(
            value = startTime,
            onValueChange = { if (it.length <= 5) startTime = it },
            label = { Text("Start Time (HH:mm)") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Routine Reminder", style = MaterialTheme.typography.labelLarge)
            Switch(checked = remindersEnabled, onCheckedChange = { remindersEnabled = it })
        }
        Text("Select Habits (${selectedIds.size} chosen)", style = MaterialTheme.typography.labelLarge)
        if (allHabits.isEmpty()) {
            Text(
                text = "Create at least one habit first in the Habits tab to link it here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            allHabits.forEach { habit ->
                val checked = selectedIds.contains(habit.id)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (checked) selectedIds.remove(habit.id)
                            else selectedIds.add(habit.id)
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = {
                            if (it) selectedIds.add(habit.id)
                            else selectedIds.remove(habit.id)
                        }
                    )
                    Column {
                        Text(habit.name, style = MaterialTheme.typography.bodyLarge)
                        Text(habit.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = onCancel, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
            Button(
                onClick = { onSave(name, timeOfDay, startTime, selectedIds.toList(), remindersEnabled) },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary, contentColor = Color.White),
                modifier = Modifier.weight(1f)
            ) {
                Text("Save Routine")
            }
        }
    }
}
