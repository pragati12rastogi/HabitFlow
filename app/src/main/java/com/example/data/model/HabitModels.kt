package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class ScheduleType(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    CUSTOM("Custom");

    companion object {
        fun fromRaw(raw: String?): ScheduleType =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: DAILY
    }
}

enum class HabitPriority(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High");

    companion object {
        fun fromRaw(raw: String?): HabitPriority =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: MEDIUM
    }
}

enum class RoutineTimeOfDay(val label: String, val defaultTime: String) {
    MORNING("Morning", "07:00"),
    AFTERNOON("Afternoon", "13:00"),
    EVENING("Evening", "20:00"),
    CUSTOM("Custom", "09:00");

    companion object {
        fun fromRaw(raw: String?): RoutineTimeOfDay =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: MORNING
    }
}

enum class ChallengeType(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly");

    companion object {
        fun fromRaw(raw: String?): ChallengeType =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: DAILY
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val bio: String = "",
    val xp: Long = 0L,
    val level: Long = 1L,
    val remindersEnabled: Boolean = true,
    val dailySummaryTime: String = "08:00",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Habit(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "Health",
    val scheduleType: String = ScheduleType.DAILY.name,
    val targetDays: List<Long> = listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L),
    val targetFrequency: Long = 1L,
    val priority: String = HabitPriority.MEDIUM.name,
    val startDate: String = "",
    val reminderTime: String = "",
    val remindersEnabled: Boolean = false,
    val paused: Boolean = false,
    val colorHex: String = "#6C63FF",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class HabitCompletion(
    @DocumentId val id: String = "",
    val userId: String = "",
    val habitId: String = "",
    val habitName: String = "",
    val category: String = "",
    val date: String = "",
    val completedCount: Long = 1L,
    val targetFrequency: Long = 1L,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Routine(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val timeOfDay: String = RoutineTimeOfDay.MORNING.name,
    val startTime: String = "07:00",
    val habitIds: List<String> = emptyList(),
    val remindersEnabled: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserAchievement(
    @DocumentId val id: String = "",
    val userId: String = "",
    val badgeKey: String = "",
    val title: String = "",
    val description: String = "",
    val xpReward: Long = 0L,
    @ServerTimestamp val unlockedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChallengeProgress(
    @DocumentId val id: String = "",
    val userId: String = "",
    val challengeId: String = "",
    val progress: Long = 0L,
    val target: Long = 1L,
    val completed: Boolean = false,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class HabitCategory(
    @DocumentId val id: String = "",
    val name: String = "",
    val iconName: String = "star",
    val colorHex: String = "#6C63FF",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MotivationalMessage(
    @DocumentId val id: String = "",
    val quote: String = "",
    val author: String = "",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Challenge(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val type: String = ChallengeType.DAILY.name,
    val targetCount: Long = 3L,
    val xpReward: Long = 50L,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppSettings(
    @DocumentId val id: String = "settings",
    val adsEnabled: Boolean = false,
    val adPublisherId: String = "",
    val dashboardAdSlotId: String = "",
    val analyticsAdSlotId: String = "",
    val showAdsOnDashboard: Boolean = true,
    val showAdsOnAnalytics: Boolean = true,
    val maintenanceMode: Boolean = false,
    val announcement: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)
