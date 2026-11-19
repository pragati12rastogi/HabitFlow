package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.DefaultCatalogs
import com.example.domain.HabitAnalyticsCalculator
import com.example.ui.components.CalendarHeatmap
import com.example.ui.components.SponsoredBannerSlot
import com.example.ui.components.StreakPill
import com.example.ui.components.TrendAreaChart
import com.example.ui.components.WeeklyBarChart
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberStreak
import com.example.ui.theme.IndigoContainer
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.LavenderSecondary
import com.example.ui.theme.MintContainer
import com.example.ui.theme.MintSuccess
import com.example.ui.viewmodel.HabitFlowDashboardData

@Composable
fun AnalyticsScreen(
    data: HabitFlowDashboardData,
    rangeDays: Int,
    categoryFilter: String,
    onSelectRange: (Int) -> Unit,
    onSelectCategory: (String) -> Unit
) {
    val trendPoints = HabitAnalyticsCalculator.calculateRangeTrend(
        habits = data.habits,
        completions = data.completions,
        days = rangeDays,
        categoryFilter = categoryFilter,
        endDate = data.todayStr
    )
    val monthHeatmap = HabitAnalyticsCalculator.calculateRangeTrend(
        habits = data.habits,
        completions = data.completions,
        days = 28,
        categoryFilter = categoryFilter,
        endDate = data.todayStr
    )
    val filteredSummaries = if (categoryFilter == "All") {
        data.habitSummaries
    } else {
        data.habitSummaries.filter { it.habit.category.equals(categoryFilter, ignoreCase = true) }
    }
    val avgRate = if (trendPoints.isEmpty()) 0 else {
        val scheduled = trendPoints.sumOf { it.scheduled }
        val completed = trendPoints.sumOf { it.completed }
        if (scheduled == 0) 0 else ((completed * 100f) / scheduled).toInt().coerceIn(0, 100)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text("Progress & Analytics", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "Track trends, streaks, and your strongest habits",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Date Range & Category Filters
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(7, 14, 30).forEach { days ->
                        FilterChip(
                            selected = rangeDays == days,
                            onClick = { onSelectRange(days) },
                            label = { Text("${days}D") }
                        )
                    }
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = categoryFilter == "All",
                        onClick = { onSelectCategory("All") },
                        label = { Text("All Categories") }
                    )
                    data.categories.forEach { cat ->
                        FilterChip(
                            selected = categoryFilter.equals(cat.name, ignoreCase = true),
                            onClick = { onSelectCategory(cat.name) },
                            label = { Text(cat.name) }
                        )
                    }
                }
            }
        }

        // Top Streak & Completion KPI Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnalyticsKpiCard(
                    title = "Completion",
                    value = "$avgRate%",
                    subtitle = "Last $rangeDays days",
                    tint = IndigoPrimary,
                    modifier = Modifier.weight(1f)
                )
                AnalyticsKpiCard(
                    title = "Current Streak",
                    value = "${data.overallStreak}d",
                    subtitle = "Active run",
                    tint = AmberStreak,
                    modifier = Modifier.weight(1f)
                )
                AnalyticsKpiCard(
                    title = "Best Streak",
                    value = "${data.longestOverallStreak}d",
                    subtitle = "All-time high",
                    tint = MintSuccess,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Daily Trend Area Chart
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Daily Completion Trend", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "$rangeDays-day consistency curve ($categoryFilter)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    TrendAreaChart(points = trendPoints)
                }
            }
        }

        // Weekly Progress Bar Chart
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Last 7 Days Breakdown", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(14.dp))
                    WeeklyBarChart(points = trendPoints.takeLast(7))
                }
            }
        }

        // 28-Day Calendar Heatmap
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Calendar Habit History", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Last 4 weeks · darker tiles indicate higher completion",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    CalendarHeatmap(points = monthHeatmap)
                }
            }
        }

        // Best-Performing Habits Leaderboard
        item {
            Text("Habit Performance", style = MaterialTheme.typography.titleLarge)
        }
        if (filteredSummaries.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        text = "No habits in this category yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
        } else {
            items(filteredSummaries, key = { it.habit.id }) { summary ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(summary.habit.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "${summary.habit.category} · ${summary.totalCompletions} total · Best ${summary.longestStreak}d",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StreakPill(streakDays = summary.currentStreak)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { summary.completionRate / 100f },
                                color = if (summary.completionRate >= 80) MintSuccess else IndigoPrimary,
                                trackColor = IndigoContainer,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50))
                            )
                            Text(
                                text = "${summary.completionRate}%",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = IndigoPrimary
                            )
                        }
                    }
                }
            }
        }

        // Optional Ad Placement below Analytics charts
        if (data.appSettings.adsEnabled && data.appSettings.showAdsOnAnalytics) {
            item {
                SponsoredBannerSlot(
                    settings = data.appSettings,
                    slotLabel = "Analytics Insights",
                    slotId = data.appSettings.analyticsAdSlotId
                )
            }
        }
    }
}

@Composable
private fun AnalyticsKpiCard(
    title: String,
    value: String,
    subtitle: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = tint
            )
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AchievementsScreen(data: HabitFlowDashboardData) {
    val unlockedMap = data.achievements.associateBy { it.badgeKey }
    val xpInLevel = (data.profile.xp % 100L).toInt()
    val progressMap = data.challengeProgress.associateBy { it.challengeId }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Level & XP Banner
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(IndigoPrimary, LavenderSecondary)))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Level ${data.profile.level}",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = Color.White
                                )
                                Text(
                                    text = "${data.profile.xp} Total XP · ${100 - xpInLevel} XP to Level ${data.profile.level + 1}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.EmojiEvents,
                                    contentDescription = "Level trophy",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        LinearProgressIndicator(
                            progress = { xpInLevel / 100f },
                            color = MintSuccess,
                            trackColor = Color.White.copy(alpha = 0.25f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(50))
                        )
                    }
                }
            }
        }

        // Daily & Weekly Challenges
        item {
            Text("Active Challenges", style = MaterialTheme.typography.titleLarge)
        }
        items(data.challenges, key = { it.id }) { challenge ->
            val progressObj = progressMap[challenge.id]
            val current = (progressObj?.progress ?: 0L).coerceAtMost(challenge.targetCount)
            val isDone = progressObj?.completed == true || current >= challenge.targetCount
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDone) MintContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(challenge.title, style = MaterialTheme.typography.titleMedium)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = IndigoContainer
                                ) {
                                    Text(
                                        text = challenge.type,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = IndigoPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = challenge.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = AmberContainer
                        ) {
                            Text(
                                text = "+${challenge.xpReward} XP",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF92400E),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { (current.toFloat() / challenge.targetCount.toFloat()).coerceIn(0f, 1f) },
                            color = if (isDone) MintSuccess else IndigoPrimary,
                            trackColor = IndigoContainer,
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(50))
                        )
                        Text(
                            text = if (isDone) "Completed!" else "$current/${challenge.targetCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDone) MintSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Achievement Badges
        item {
            Text(
                text = "Achievement Badges (${data.achievements.size}/${DefaultCatalogs.achievementSpecs.size})",
                style = MaterialTheme.typography.titleLarge
            )
        }
        items(DefaultCatalogs.achievementSpecs, key = { it.key }) { spec ->
            val unlocked = unlockedMap.containsKey(spec.key)
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (unlocked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (unlocked) MintContainer else MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (unlocked) Icons.Filled.Verified else Icons.Filled.Lock,
                            contentDescription = spec.title,
                            tint = if (unlocked) MintSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(spec.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = spec.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (unlocked) MintContainer else IndigoContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = if (unlocked) "Unlocked" else "+${spec.xpReward} XP",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (unlocked) Color(0xFF15803D) else IndigoPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
