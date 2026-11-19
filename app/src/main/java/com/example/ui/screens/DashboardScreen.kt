package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Habit
import com.example.domain.HabitAnalyticsCalculator
import com.example.domain.HabitDateUtils
import com.example.domain.HabitDurationFormatter
import com.example.ui.components.AnimatedCompletionRing
import com.example.ui.components.EmptyHabitsCard
import com.example.ui.components.PriorityBadge
import com.example.ui.components.SponsoredBannerSlot
import com.example.ui.components.StreakPill
import com.example.ui.components.WeeklyBarChart
import com.example.ui.components.categoryIcon
import com.example.ui.components.parseHexColor
import com.example.ui.theme.IndigoContainer
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.LavenderContainer
import com.example.ui.theme.LavenderSecondary
import com.example.ui.theme.MintContainer
import com.example.ui.theme.MintSuccess
import com.example.ui.viewmodel.HabitFlowDashboardData

@Composable
fun DashboardScreen(
    data: HabitFlowDashboardData,
    onToggleHabit: (Habit) -> Unit,
    onStartTimer: (Habit) -> Unit,
    onCreateHabit: () -> Unit,
    onLoadDemoData: () -> Unit,
    onOpenTracker: () -> Unit,
    onOpenRoutines: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenAchievements: () -> Unit
) {
    val activeHabits = data.habits.filter { !it.paused }
    val scheduledToday = activeHabits.filter { HabitAnalyticsCalculator.isHabitScheduledOn(it, data.todayStr) }
    val todayCompletions = data.completions.filter { it.date == data.todayStr }.associateBy { it.habitId }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Announcement Banner if configured by admin
        if (data.appSettings.announcement.isNotBlank()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = IndigoContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = "Announcement",
                            tint = IndigoPrimary
                        )
                        Text(
                            text = data.appSettings.announcement,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Greeting + Daily Completion Hero Card
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(104.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_banner),
                            contentDescription = "HabitFlow morning banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Black.copy(alpha = 0.25f), Color.Black.copy(alpha = 0.55f))
                                    )
                                )
                                .padding(horizontal = 18.dp, vertical = 14.dp)
                        ) {
                            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                                Text(
                                    text = HabitDateUtils.displayDateLabel(data.todayStr),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Text(
                                    text = "Hi, ${data.profile.displayName.substringBefore(" ")}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White
                                )
                            }
                            StreakPill(
                                streakDays = data.overallStreak,
                                modifier = Modifier.align(Alignment.TopEnd)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        AnimatedCompletionRing(percentage = data.todayStats.percentage)
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                StatMiniTile(
                                    label = "Completed",
                                    value = "${data.todayStats.completed}",
                                    tint = MintSuccess,
                                    bg = MintContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                StatMiniTile(
                                    label = "Pending",
                                    value = "${data.todayStats.pending}",
                                    tint = IndigoPrimary,
                                    bg = IndigoContainer,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                StatMiniTile(
                                    label = "Active",
                                    value = "${activeHabits.size}",
                                    tint = LavenderSecondary,
                                    bg = LavenderContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                StatMiniTile(
                                    label = "Monthly",
                                    value = "${data.monthlyCompletionRate}%",
                                    tint = IndigoPrimary,
                                    bg = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Motivational Message of the Day
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = IndigoContainer.copy(alpha = 0.45f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FormatQuote,
                        contentDescription = "Quote of the day",
                        tint = IndigoPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "\"${data.quoteOfTheDay.quote}\"",
                            style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "— ${data.quoteOfTheDay.author}",
                            style = MaterialTheme.typography.labelMedium,
                            color = IndigoPrimary
                        )
                    }
                }
            }
        }

        // Empty State or Today's Checklist
        if (data.habits.isEmpty()) {
            item {
                EmptyHabitsCard(
                    onCreateHabit = onCreateHabit,
                    onLoadDemoData = onLoadDemoData,
                    heroImage = {
                        Image(
                            painter = painterResource(id = R.drawable.img_empty_habits),
                            contentDescription = "Start building habits",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(18.dp))
                        )
                    }
                )
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's Checklist",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onOpenTracker) {
                            Text("Full Tracker")
                        }
                        IconButton(
                            onClick = onCreateHabit,
                            modifier = Modifier.testTag("dashboard_add_habit_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add habit")
                        }
                    }
                }
            }

            if (scheduledToday.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Text(
                            text = "No habits scheduled for today. Check your Weekly/Custom schedules or create a daily habit.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(18.dp)
                        )
                    }
                }
            } else {
                items(scheduledToday, key = { it.id }) { habit ->
                    val comp = todayCompletions[habit.id]
                    val doneCount = comp?.completedCount ?: 0L
                    val isDone = doneCount >= habit.targetFrequency
                    val accent = parseHexColor(habit.colorHex)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onToggleHabit(habit) }
                            .testTag("dashboard_habit_${habit.id}"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDone) MintContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isDone) MintSuccess else accent.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isDone) Icons.Filled.Check else categoryIcon(habit.category),
                                    contentDescription = habit.name,
                                    tint = if (isDone) Color.White else accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = habit.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = buildString {
                                        append("${habit.category} · $doneCount/${habit.targetFrequency} today")
                                        val secs = comp?.durationSeconds ?: 0L
                                        if (secs > 0L) append(" · ${HabitDurationFormatter.formatCompact(secs)} tracked")
                                        if (habit.reminderTime.isNotBlank()) append(" · ${habit.reminderTime}")
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            PriorityBadge(habit.priority)
                            IconButton(
                                onClick = { onStartTimer(habit) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("dashboard_timer_${habit.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Timer,
                                    contentDescription = "Start stopwatch timer for ${habit.name}",
                                    tint = IndigoPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Optional Ad Placement between sections (Dashboard)
        if (data.appSettings.adsEnabled && data.appSettings.showAdsOnDashboard) {
            item {
                SponsoredBannerSlot(
                    settings = data.appSettings,
                    slotLabel = "Dashboard Feed",
                    slotId = data.appSettings.dashboardAdSlotId
                )
            }
        }

        // Weekly Progress Chart Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Weekly Progress",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Last 7 days completion rate",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = onOpenAnalytics) {
                            Icon(Icons.AutoMirrored.Outlined.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Analytics")
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    WeeklyBarChart(points = data.weeklyTrend)
                }
            }
        }

        // Quick Action Row for Routines & Achievements
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onOpenRoutines() },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Routines", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${data.routines.size} active flows",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onOpenAchievements() },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Level ${data.profile.level}", style = MaterialTheme.typography.titleMedium, color = IndigoPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${data.profile.xp} XP · ${data.achievements.size} badges",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatMiniTile(
    label: String,
    value: String,
    tint: Color,
    bg: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = bg.copy(alpha = 0.65f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = tint
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
