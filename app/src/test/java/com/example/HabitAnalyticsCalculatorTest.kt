package com.example

import com.example.data.model.Habit
import com.example.data.model.HabitCompletion
import com.example.data.model.ScheduleType
import com.example.domain.HabitAnalyticsCalculator
import com.example.domain.HabitDateUtils
import com.example.domain.HabitDurationFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitAnalyticsCalculatorTest {

    private fun dailyHabit(id: String, paused: Boolean = false, startDate: String = "2026-11-01") = Habit(
        id = id,
        userId = "user_1",
        name = "Habit $id",
        category = "Health",
        scheduleType = ScheduleType.DAILY.name,
        targetDays = listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L),
        targetFrequency = 1L,
        startDate = startDate,
        paused = paused
    )

    private fun completion(habitId: String, date: String, durationSeconds: Long = 0L) = HabitCompletion(
        id = "${habitId}_$date",
        userId = "user_1",
        habitId = habitId,
        habitName = "Habit $habitId",
        category = "Health",
        date = date,
        completedCount = 1L,
        targetFrequency = 1L,
        durationSeconds = durationSeconds
    )

    @Test
    fun dailyStats_calculatesCompletedPendingAndPercentage() {
        val habits = listOf(
            dailyHabit("h1"),
            dailyHabit("h2"),
            dailyHabit("h3", paused = true)
        )
        val completions = listOf(completion("h1", "2026-11-18"))
        val stats = HabitAnalyticsCalculator.calculateDailyStats(habits, completions, "2026-11-18")

        assertEquals(2, stats.totalScheduled)
        assertEquals(1, stats.completed)
        assertEquals(1, stats.pending)
        assertEquals(50, stats.percentage)
    }

    @Test
    fun streakCalculation_countsConsecutiveDaysAndLongestStreak() {
        val h1 = dailyHabit("h1", startDate = "2026-11-10")
        val completions = listOf(
            completion("h1", "2026-11-11"),
            completion("h1", "2026-11-12"),
            completion("h1", "2026-11-13"),
            completion("h1", "2026-11-14"),
            // missed 2026-11-15
            completion("h1", "2026-11-16"),
            completion("h1", "2026-11-17"),
            completion("h1", "2026-11-18")
        )
        val (current, longest) = HabitAnalyticsCalculator.calculateHabitStreak(h1, completions, "2026-11-18")
        assertEquals(3, current)
        assertEquals(4, longest)
    }

    @Test
    fun weeklySchedule_filtersByIsoWeekday() {
        // 2026-11-18 is a Wednesday (ISO weekday = 3)
        val weekday = HabitDateUtils.isoWeekday("2026-11-18")
        assertEquals(3, weekday)

        val wednesdayHabit = Habit(
            id = "h_wed",
            name = "Midweek Run",
            scheduleType = ScheduleType.WEEKLY.name,
            targetDays = listOf(3L),
            startDate = "2026-11-01"
        )
        assertTrue(HabitAnalyticsCalculator.isHabitScheduledOn(wednesdayHabit, "2026-11-18"))
        assertFalse(HabitAnalyticsCalculator.isHabitScheduledOn(wednesdayHabit, "2026-11-19"))
    }

    @Test
    fun durationFormatter_andSummaryTotals_computeAccurately() {
        assertEquals("45s", HabitDurationFormatter.formatCompact(45L))
        assertEquals("3m 15s", HabitDurationFormatter.formatCompact(195L))
        assertEquals("15m", HabitDurationFormatter.formatCompact(900L))
        assertEquals("1h 20m", HabitDurationFormatter.formatCompact(4800L))

        assertEquals("00:45", HabitDurationFormatter.formatStopwatch(45L))
        assertEquals("15:00", HabitDurationFormatter.formatStopwatch(900L))
        assertEquals("01:02:03", HabitDurationFormatter.formatStopwatch(3723L))

        val h1 = dailyHabit("h1")
        val h2 = dailyHabit("h2")
        val completions = listOf(
            completion("h1", "2026-11-17", durationSeconds = 300L),
            completion("h1", "2026-11-18", durationSeconds = 900L),
            completion("h2", "2026-11-18", durationSeconds = 120L)
        )
        val summaries = HabitAnalyticsCalculator.calculateHabitSummaries(listOf(h1, h2), completions, "2026-11-18")
        val s1 = summaries.first { it.habit.id == "h1" }
        val s2 = summaries.first { it.habit.id == "h2" }
        assertEquals(1200L, s1.totalDurationSeconds)
        assertEquals(120L, s2.totalDurationSeconds)
    }
}
