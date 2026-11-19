package com.example.domain

import com.example.data.model.Habit
import com.example.data.model.HabitCompletion
import com.example.data.model.ScheduleType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class DailyStats(
    val date: String,
    val totalScheduled: Int,
    val completed: Int,
    val pending: Int,
    val percentage: Int
)

data class DayProgressPoint(
    val date: String,
    val label: String,
    val completed: Int,
    val scheduled: Int,
    val rate: Float
)

data class HabitStatSummary(
    val habit: Habit,
    val totalCompletions: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val completionRate: Int
)

object HabitDateUtils {
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val shortDayFormat = SimpleDateFormat("EEE", Locale.US)
    private val displayFormat = SimpleDateFormat("EEE, MMM d", Locale.US)

    fun todayIso(nowMillis: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        return formatIso(cal)
    }

    fun formatIso(cal: Calendar): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)

    fun parseIso(dateStr: String): Calendar? {
        return try {
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                isLenient = false
            }.parse(dateStr) ?: return null
            Calendar.getInstance().apply { time = parsed }
        } catch (_: Exception) {
            null
        }
    }

    fun addDays(dateStr: String, deltaDays: Int): String {
        val cal = parseIso(dateStr) ?: Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, deltaDays)
        return formatIso(cal)
    }

    fun shortDayLabel(dateStr: String): String {
        val cal = parseIso(dateStr) ?: return dateStr.takeLast(2)
        return shortDayFormat.format(cal.time)
    }

    fun displayDateLabel(dateStr: String): String {
        val cal = parseIso(dateStr) ?: return dateStr
        return displayFormat.format(cal.time)
    }

    /** Returns 1 for Monday ... 7 for Sunday */
    fun isoWeekday(dateStr: String): Int {
        val cal = parseIso(dateStr) ?: Calendar.getInstance()
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
    }

    fun recentDates(endDate: String, count: Int): List<String> {
        return (count - 1 downTo 0).map { offset ->
            addDays(endDate, -offset)
        }
    }
}

object HabitAnalyticsCalculator {

    fun isHabitScheduledOn(habit: Habit, dateStr: String): Boolean {
        if (habit.paused) return false
        if (habit.startDate.isNotBlank() && dateStr < habit.startDate) return false
        val schedule = ScheduleType.fromRaw(habit.scheduleType)
        return when (schedule) {
            ScheduleType.DAILY -> true
            ScheduleType.WEEKLY, ScheduleType.CUSTOM -> {
                val weekday = HabitDateUtils.isoWeekday(dateStr).toLong()
                habit.targetDays.isEmpty() || habit.targetDays.contains(weekday)
            }
        }
    }

    fun calculateDailyStats(
        habits: List<Habit>,
        completions: List<HabitCompletion>,
        dateStr: String
    ): DailyStats {
        val scheduledHabits = habits.filter { isHabitScheduledOn(it, dateStr) }
        val completionMap = completions
            .filter { it.date == dateStr }
            .associateBy { it.habitId }

        val completedCount = scheduledHabits.count { habit ->
            val comp = completionMap[habit.id]
            comp != null && comp.completedCount >= habit.targetFrequency
        }
        val total = scheduledHabits.size
        val pending = (total - completedCount).coerceAtLeast(0)
        val percentage = if (total == 0) 0 else ((completedCount * 100f) / total).toInt().coerceIn(0, 100)
        return DailyStats(
            date = dateStr,
            totalScheduled = total,
            completed = completedCount,
            pending = pending,
            percentage = percentage
        )
    }

    fun calculateHabitStreak(
        habit: Habit,
        completions: List<HabitCompletion>,
        todayStr: String
    ): Pair<Int, Int> {
        val completedDates = completions
            .filter { it.habitId == habit.id && it.completedCount >= habit.targetFrequency }
            .map { it.date }
            .toSet()

        if (completedDates.isEmpty()) return 0 to 0

        // Current streak: walk backwards from today (or yesterday if today isn't completed yet)
        var currentStreak = 0
        var cursor = todayStr
        if (!completedDates.contains(cursor)) {
            cursor = HabitDateUtils.addDays(cursor, -1)
        }
        var guard = 0
        while (guard < 365) {
            if (isHabitScheduledOn(habit, cursor)) {
                if (completedDates.contains(cursor)) {
                    currentStreak++
                } else {
                    break
                }
            } else if (cursor < habit.startDate && habit.startDate.isNotBlank()) {
                break
            }
            cursor = HabitDateUtils.addDays(cursor, -1)
            guard++
        }

        // Longest streak across sorted completed dates (consecutive scheduled days)
        val startCheck = habit.startDate.takeIf { it.isNotBlank() }
            ?: completedDates.minOrNull()
            ?: todayStr
        var longest = 0
        var running = 0
        var scan = startCheck
        var scanGuard = 0
        while (scan <= todayStr && scanGuard < 730) {
            if (isHabitScheduledOn(habit, scan)) {
                if (completedDates.contains(scan)) {
                    running++
                    if (running > longest) longest = running
                } else {
                    running = 0
                }
            }
            scan = HabitDateUtils.addDays(scan, 1)
            scanGuard++
        }
        if (currentStreak > longest) longest = currentStreak
        return currentStreak to longest
    }

    fun calculateOverallStreak(
        habits: List<Habit>,
        completions: List<HabitCompletion>,
        todayStr: String
    ): Int {
        val activeHabits = habits.filter { !it.paused }
        if (activeHabits.isEmpty()) return 0

        val datesWithAnyCompletion = completions
            .filter { it.completedCount >= 1L }
            .map { it.date }
            .toSet()

        var streak = 0
        var cursor = todayStr
        if (!datesWithAnyCompletion.contains(cursor)) {
            cursor = HabitDateUtils.addDays(cursor, -1)
        }
        var guard = 0
        while (guard < 365) {
            if (datesWithAnyCompletion.contains(cursor)) {
                streak++
                cursor = HabitDateUtils.addDays(cursor, -1)
            } else {
                break
            }
            guard++
        }
        return streak
    }

    fun calculateRangeTrend(
        habits: List<Habit>,
        completions: List<HabitCompletion>,
        days: Int,
        categoryFilter: String?,
        endDate: String
    ): List<DayProgressPoint> {
        val filteredHabits = if (categoryFilter.isNullOrBlank() || categoryFilter == "All") {
            habits
        } else {
            habits.filter { it.category.equals(categoryFilter, ignoreCase = true) }
        }
        val filteredIds = filteredHabits.map { it.id }.toSet()
        val filteredCompletions = completions.filter { filteredIds.contains(it.habitId) }

        return HabitDateUtils.recentDates(endDate, days).map { date ->
            val stats = calculateDailyStats(filteredHabits, filteredCompletions, date)
            DayProgressPoint(
                date = date,
                label = HabitDateUtils.shortDayLabel(date),
                completed = stats.completed,
                scheduled = stats.totalScheduled,
                rate = if (stats.totalScheduled == 0) 0f else stats.completed.toFloat() / stats.totalScheduled.toFloat()
            )
        }
    }

    fun calculateHabitSummaries(
        habits: List<Habit>,
        completions: List<HabitCompletion>,
        todayStr: String,
        windowDays: Int = 30
    ): List<HabitStatSummary> {
        val windowStart = HabitDateUtils.addDays(todayStr, -(windowDays - 1))
        val datesInWindow = HabitDateUtils.recentDates(todayStr, windowDays)
        return habits.map { habit ->
            val (current, longest) = calculateHabitStreak(habit, completions, todayStr)
            val habitCompletions = completions.filter {
                it.habitId == habit.id && it.date >= windowStart && it.date <= todayStr && it.completedCount >= habit.targetFrequency
            }
            val scheduledInWindow = datesInWindow.count { isHabitScheduledOn(habit, it) }.coerceAtLeast(1)
            val rate = ((habitCompletions.size * 100f) / scheduledInWindow).toInt().coerceIn(0, 100)
            HabitStatSummary(
                habit = habit,
                totalCompletions = completions.count { it.habitId == habit.id && it.completedCount >= habit.targetFrequency },
                currentStreak = current,
                longestStreak = longest,
                completionRate = rate
            )
        }.sortedByDescending { it.completionRate }
    }
}
