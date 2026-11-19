package com.example.domain

import com.example.data.model.Challenge
import com.example.data.model.ChallengeType
import com.example.data.model.HabitCategory
import com.example.data.model.MotivationalMessage

data class AchievementBadgeSpec(
    val key: String,
    val title: String,
    val description: String,
    val xpReward: Long,
    val iconName: String
)

object DefaultCatalogs {
    const val ADMIN_EMAIL = "rastogi.pragati.it@gmail.com"

    val defaultCategories: List<HabitCategory> = listOf(
        HabitCategory(id = "cat_health", name = "Health", iconName = "favorite", colorHex = "#EF4444"),
        HabitCategory(id = "cat_fitness", name = "Fitness", iconName = "fitness", colorHex = "#F59E0B"),
        HabitCategory(id = "cat_learning", name = "Learning", iconName = "school", colorHex = "#0EA5E9"),
        HabitCategory(id = "cat_productivity", name = "Productivity", iconName = "bolt", colorHex = "#6C63FF"),
        HabitCategory(id = "cat_personal_dev", name = "Personal Development", iconName = "psychology", colorHex = "#A78BFA"),
        HabitCategory(id = "cat_finance", name = "Finance", iconName = "savings", colorHex = "#22C55E"),
        HabitCategory(id = "cat_mindfulness", name = "Mindfulness", iconName = "spa", colorHex = "#14B8A6"),
        HabitCategory(id = "cat_lifestyle", name = "Lifestyle", iconName = "wb_sunny", colorHex = "#EC4899")
    )

    val defaultMotivationalMessages: List<MotivationalMessage> = listOf(
        MotivationalMessage(
            id = "quote_1",
            quote = "Small daily habits compound into remarkable transformations over time.",
            author = "HabitFlow"
        ),
        MotivationalMessage(
            id = "quote_2",
            quote = "Consistency beats intensity. Show up gently for yourself today.",
            author = "HabitFlow"
        ),
        MotivationalMessage(
            id = "quote_3",
            quote = "Every completed check-in is a vote for the person you are becoming.",
            author = "James Clear"
        ),
        MotivationalMessage(
            id = "quote_4",
            quote = "Missing a day doesn't erase your progress. Today is a fresh canvas.",
            author = "HabitFlow"
        ),
        MotivationalMessage(
            id = "quote_5",
            quote = "Design your morning routine and the rest of your day falls into rhythm.",
            author = "HabitFlow"
        )
    )

    val defaultChallenges: List<Challenge> = listOf(
        Challenge(
            id = "ch_daily_focus",
            title = "Daily Momentum",
            description = "Complete at least 3 habits today to build unstoppable momentum.",
            type = ChallengeType.DAILY.name,
            targetCount = 3L,
            xpReward = 50L
        ),
        Challenge(
            id = "ch_weekly_consistency",
            title = "Weekly Flow Champion",
            description = "Log 15 habit completions across the week.",
            type = ChallengeType.WEEKLY.name,
            targetCount = 15L,
            xpReward = 150L
        ),
        Challenge(
            id = "ch_routine_starter",
            title = "Routine Architect",
            description = "Complete every habit inside a routine in a single day.",
            type = ChallengeType.DAILY.name,
            targetCount = 1L,
            xpReward = 75L
        )
    )

    val achievementSpecs: List<AchievementBadgeSpec> = listOf(
        AchievementBadgeSpec(
            key = "first_step",
            title = "First Step",
            description = "Log your very first habit completion.",
            xpReward = 30L,
            iconName = "flag"
        ),
        AchievementBadgeSpec(
            key = "streak_3",
            title = "3-Day Spark",
            description = "Maintain a 3-day consecutive completion streak.",
            xpReward = 60L,
            iconName = "local_fire_department"
        ),
        AchievementBadgeSpec(
            key = "streak_7",
            title = "Week Warrior",
            description = "Reach a 7-day unbroken habit streak.",
            xpReward = 120L,
            iconName = "workspace_premium"
        ),
        AchievementBadgeSpec(
            key = "routine_builder",
            title = "Routine Architect",
            description = "Create a custom daily routine with habits.",
            xpReward = 50L,
            iconName = "view_timeline"
        ),
        AchievementBadgeSpec(
            key = "habit_master_25",
            title = "Habit Master",
            description = "Complete 25 total habit check-ins.",
            xpReward = 150L,
            iconName = "emoji_events"
        ),
        AchievementBadgeSpec(
            key = "balanced_life",
            title = "Balanced Flow",
            description = "Build active habits across 3 or more categories.",
            xpReward = 80L,
            iconName = "auto_awesome"
        )
    )
}
