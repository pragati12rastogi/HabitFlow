package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppSettings
import com.example.data.model.Challenge
import com.example.data.model.ChallengeProgress
import com.example.data.model.ChallengeType
import com.example.data.model.Habit
import com.example.data.model.HabitCategory
import com.example.data.model.HabitCompletion
import com.example.data.model.HabitPriority
import com.example.data.model.MotivationalMessage
import com.example.data.model.Routine
import com.example.data.model.RoutineTimeOfDay
import com.example.data.model.ScheduleType
import com.example.data.model.UserAchievement
import com.example.data.model.UserProfile
import com.example.data.repository.HabitRepository
import com.example.domain.DailyStats
import com.example.domain.DayProgressPoint
import com.example.domain.DefaultCatalogs
import com.example.domain.HabitAnalyticsCalculator
import com.example.domain.HabitDateUtils
import com.example.domain.HabitStatSummary
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class HabitFlowDashboardData(
    val profile: UserProfile,
    val habits: List<Habit>,
    val completions: List<HabitCompletion>,
    val routines: List<Routine>,
    val achievements: List<UserAchievement>,
    val challengeProgress: List<ChallengeProgress>,
    val categories: List<HabitCategory>,
    val motivationalMessages: List<MotivationalMessage>,
    val challenges: List<Challenge>,
    val appSettings: AppSettings,
    val todayStr: String,
    val todayStats: DailyStats,
    val overallStreak: Int,
    val longestOverallStreak: Int,
    val weeklyTrend: List<DayProgressPoint>,
    val monthlyCompletionRate: Int,
    val habitSummaries: List<HabitStatSummary>,
    val quoteOfTheDay: MotivationalMessage,
    val isAdmin: Boolean
)

data class HabitFlowTransientState(
    val selectedTrackerDate: String = HabitDateUtils.todayIso(),
    val analyticsRangeDays: Int = 14,
    val analyticsCategoryFilter: String = "All",
    val snackbarMessage: String? = null,
    val isBusy: Boolean = false
)

class HabitFlowViewModel(
    private val repository: HabitRepository,
    private val currentUserId: String,
    private val userDisplayName: String,
    private val userEmail: String,
    private val userPhotoUrl: String
) : ViewModel() {

    private val isAdminUser = userEmail.equals(DefaultCatalogs.ADMIN_EMAIL, ignoreCase = true)

    private val _transient = MutableStateFlow(HabitFlowTransientState())
    val transient: StateFlow<HabitFlowTransientState> = _transient.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(
                userId = currentUserId,
                displayName = userDisplayName,
                email = userEmail,
                photoUrl = userPhotoUrl
            ).onFailure { err ->
                Log.w(TAG, "Profile init warning: ${err.message}")
            }
        }
    }

    private val coreUserFlow = combine(
        repository.observeUserProfile(currentUserId),
        repository.observeHabits(currentUserId),
        repository.observeCompletions(currentUserId),
        repository.observeRoutines(currentUserId),
        repository.observeAchievements(currentUserId)
    ) { profile, habits, completions, routines, achievements ->
        CoreUserBundle(profile, habits, completions, routines, achievements)
    }

    private val catalogFlow = combine(
        repository.observeCategories(),
        repository.observeMotivationalMessages(),
        repository.observeChallenges(),
        repository.observeChallengeProgress(currentUserId),
        repository.observeAppSettings()
    ) { categories, quotes, challenges, progress, settings ->
        CatalogBundle(categories, quotes, challenges, progress, settings)
    }

    val uiState: StateFlow<UiState<HabitFlowDashboardData>> = combine(
        coreUserFlow,
        catalogFlow
    ) { core, catalog ->
        val today = HabitDateUtils.todayIso()
        val resolvedProfile = core.profile ?: UserProfile(
            id = currentUserId,
            userId = currentUserId,
            displayName = userDisplayName.ifBlank { "HabitFlow Member" },
            email = userEmail,
            photoUrl = userPhotoUrl,
            xp = 0L,
            level = 1L,
            remindersEnabled = true
        )
        val mergedCategories = if (catalog.categories.isEmpty()) {
            DefaultCatalogs.defaultCategories
        } else {
            catalog.categories.filter { it.active }
        }
        val mergedQuotes = if (catalog.quotes.isEmpty()) {
            DefaultCatalogs.defaultMotivationalMessages
        } else {
            catalog.quotes.filter { it.active }.ifEmpty { DefaultCatalogs.defaultMotivationalMessages }
        }
        val mergedChallenges = if (catalog.challenges.isEmpty()) {
            DefaultCatalogs.defaultChallenges
        } else {
            catalog.challenges.filter { it.active }
        }
        val todayStats = HabitAnalyticsCalculator.calculateDailyStats(core.habits, core.completions, today)
        val overallStreak = HabitAnalyticsCalculator.calculateOverallStreak(core.habits, core.completions, today)
        val weeklyTrend = HabitAnalyticsCalculator.calculateRangeTrend(core.habits, core.completions, 7, null, today)
        val monthlyTrend = HabitAnalyticsCalculator.calculateRangeTrend(core.habits, core.completions, 30, null, today)
        val scheduledMonthly = monthlyTrend.sumOf { it.scheduled }
        val completedMonthly = monthlyTrend.sumOf { it.completed }
        val monthlyRate = if (scheduledMonthly == 0) 0 else ((completedMonthly * 100f) / scheduledMonthly).toInt().coerceIn(0, 100)
        val summaries = HabitAnalyticsCalculator.calculateHabitSummaries(core.habits, core.completions, today)
        val longestOverall = (summaries.maxOfOrNull { it.longestStreak } ?: 0).coerceAtLeast(overallStreak)
        val dayIndex = (HabitDateUtils.parseIso(today)?.get(java.util.Calendar.DAY_OF_YEAR) ?: 0) % mergedQuotes.size
        val quoteOfTheDay = mergedQuotes[dayIndex]

        HabitFlowDashboardData(
            profile = resolvedProfile,
            habits = core.habits.sortedBy { it.name.lowercase() },
            completions = core.completions,
            routines = core.routines.sortedBy { it.startTime },
            achievements = core.achievements,
            challengeProgress = catalog.progress,
            categories = mergedCategories,
            motivationalMessages = catalog.quotes.ifEmpty { DefaultCatalogs.defaultMotivationalMessages },
            challenges = mergedChallenges,
            appSettings = catalog.settings ?: AppSettings(),
            todayStr = today,
            todayStats = todayStats,
            overallStreak = overallStreak,
            longestOverallStreak = longestOverall,
            weeklyTrend = weeklyTrend,
            monthlyCompletionRate = monthlyRate,
            habitSummaries = summaries,
            quoteOfTheDay = quoteOfTheDay,
            isAdmin = isAdminUser
        )
    }
        .map<HabitFlowDashboardData, UiState<HabitFlowDashboardData>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing dashboard state", rootError)
            emit(UiState.Error(rootError.message ?: "Unable to sync your HabitFlow data"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val adminUsers: StateFlow<List<UserProfile>> = flow {
        if (!isAdminUser) {
            emit(emptyList())
        } else {
            emitAll(repository.observeAllUsersForAdmin())
        }
    }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Admin users list warning", firestoreError ?: error)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun selectTrackerDate(dateStr: String) {
        _transient.update { it.copy(selectedTrackerDate = dateStr) }
    }

    fun setAnalyticsRange(days: Int) {
        _transient.update { it.copy(analyticsRangeDays = days) }
    }

    fun setAnalyticsCategory(category: String) {
        _transient.update { it.copy(analyticsCategoryFilter = category) }
    }

    fun clearSnackbar() {
        _transient.update { it.copy(snackbarMessage = null) }
    }

    private fun postMessage(msg: String) {
        _transient.update { it.copy(snackbarMessage = msg) }
    }

    fun toggleHabitCompletion(habit: Habit, date: String, notes: String = "") {
        val currentData = (uiState.value as? UiState.Success)?.data ?: return
        val existing = currentData.completions.firstOrNull { it.habitId == habit.id && it.date == date }
        viewModelScope.launch {
            val result = repository.toggleHabitCompletion(currentUserId, habit, date, existing, notes)
            result.onSuccess { nowCompleted ->
                if (nowCompleted) {
                    val newXp = currentData.profile.xp + 20L
                    repository.awardXp(currentUserId, newXp)
                    evaluateGamification(currentData, newlyCompletedHabit = true, newBaseXp = newXp)
                    postMessage("Completed \"${habit.name}\" (+20 XP)")
                } else {
                    postMessage("Updated \"${habit.name}\"")
                }
            }.onFailure { err ->
                postMessage(err.localizedMessage ?: "Could not update completion")
            }
        }
    }

    fun updateCompletionNotes(completionId: String, notes: String) {
        viewModelScope.launch {
            repository.updateCompletionNotes(currentUserId, completionId, notes)
                .onSuccess { postMessage("Note saved") }
                .onFailure { postMessage(it.localizedMessage ?: "Failed to save note") }
        }
    }

    fun saveHabit(
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
    ) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) {
            postMessage("Please enter a habit name")
            return
        }
        viewModelScope.launch {
            _transient.update { it.copy(isBusy = true) }
            val result = repository.saveHabit(
                userId = currentUserId,
                habitId = habitId,
                name = cleanName,
                description = description,
                category = category,
                scheduleType = scheduleType,
                targetDays = targetDays,
                targetFrequency = targetFrequency,
                priority = priority,
                startDate = startDate,
                reminderTime = reminderTime,
                remindersEnabled = remindersEnabled,
                paused = paused,
                colorHex = colorHex
            )
            _transient.update { it.copy(isBusy = false) }
            result.onSuccess {
                postMessage(if (habitId == null) "Habit created!" else "Habit updated!")
                (uiState.value as? UiState.Success)?.data?.let { data ->
                    evaluateBalancedCategoryBadge(data, extraCategory = category)
                }
            }.onFailure { err ->
                postMessage(err.localizedMessage ?: "Failed to save habit")
            }
        }
    }

    fun toggleHabitPaused(habit: Habit) {
        viewModelScope.launch {
            repository.toggleHabitPaused(currentUserId, habit.id, !habit.paused)
                .onSuccess {
                    postMessage(if (!habit.paused) "\"${habit.name}\" paused" else "\"${habit.name}\" reactivated")
                }
                .onFailure { postMessage(it.localizedMessage ?: "Failed to update habit") }
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            repository.deleteHabit(currentUserId, habit.id)
                .onSuccess { postMessage("Deleted \"${habit.name}\"") }
                .onFailure { postMessage(it.localizedMessage ?: "Failed to delete habit") }
        }
    }

    fun saveRoutine(
        routineId: String?,
        name: String,
        timeOfDay: RoutineTimeOfDay,
        startTime: String,
        habitIds: List<String>,
        remindersEnabled: Boolean
    ) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) {
            postMessage("Please enter a routine name")
            return
        }
        viewModelScope.launch {
            _transient.update { it.copy(isBusy = true) }
            repository.saveRoutine(
                userId = currentUserId,
                routineId = routineId,
                name = cleanName,
                timeOfDay = timeOfDay,
                startTime = startTime,
                habitIds = habitIds,
                remindersEnabled = remindersEnabled
            ).onSuccess {
                postMessage(if (routineId == null) "Routine created!" else "Routine updated!")
                unlockBadge("routine_builder")
            }.onFailure {
                postMessage(it.localizedMessage ?: "Failed to save routine")
            }
            _transient.update { it.copy(isBusy = false) }
        }
    }

    fun reorderRoutineHabit(routine: Routine, fromIndex: Int, toIndex: Int) {
        if (fromIndex !in routine.habitIds.indices || toIndex !in routine.habitIds.indices) return
        val mutable = routine.habitIds.toMutableList()
        val moved = mutable.removeAt(fromIndex)
        mutable.add(toIndex, moved)
        viewModelScope.launch {
            repository.saveRoutine(
                userId = currentUserId,
                routineId = routine.id,
                name = routine.name,
                timeOfDay = RoutineTimeOfDay.fromRaw(routine.timeOfDay),
                startTime = routine.startTime,
                habitIds = mutable,
                remindersEnabled = routine.remindersEnabled
            ).onFailure {
                postMessage(it.localizedMessage ?: "Could not reorder routine")
            }
        }
    }

    fun deleteRoutine(routine: Routine) {
        viewModelScope.launch {
            repository.deleteRoutine(currentUserId, routine.id)
                .onSuccess { postMessage("Deleted routine \"${routine.name}\"") }
                .onFailure { postMessage(it.localizedMessage ?: "Failed to delete routine") }
        }
    }

    fun updateProfile(
        displayName: String,
        bio: String,
        remindersEnabled: Boolean,
        dailySummaryTime: String
    ) {
        viewModelScope.launch {
            repository.updateUserProfile(
                userId = currentUserId,
                displayName = displayName,
                bio = bio,
                remindersEnabled = remindersEnabled,
                dailySummaryTime = dailySummaryTime
            ).onSuccess {
                postMessage("Profile & preferences saved")
            }.onFailure {
                postMessage(it.localizedMessage ?: "Failed to update profile")
            }
        }
    }

    fun loadDemoData() {
        viewModelScope.launch {
            _transient.update { it.copy(isBusy = true) }
            repository.seedDemoData(currentUserId)
                .onSuccess { postMessage("Sample habits, routines, and history loaded!") }
                .onFailure { postMessage(it.localizedMessage ?: "Failed to load demo data") }
            _transient.update { it.copy(isBusy = false) }
        }
    }

    // --- Admin Actions ---

    fun adminSaveCategory(id: String?, name: String, iconName: String, colorHex: String, active: Boolean) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.saveCategory(id, name, iconName, colorHex, active)
                .onSuccess { postMessage("Category saved") }
                .onFailure { postMessage(it.localizedMessage ?: "Failed to save category") }
        }
    }

    fun adminDeleteCategory(id: String) {
        viewModelScope.launch {
            repository.deleteCategory(id)
                .onSuccess { postMessage("Category removed") }
                .onFailure { postMessage(it.localizedMessage ?: "Failed to remove category") }
        }
    }

    fun adminSaveQuote(id: String?, quote: String, author: String, active: Boolean) {
        if (quote.isBlank()) return
        viewModelScope.launch {
            repository.saveMotivationalMessage(id, quote, author, active)
                .onSuccess { postMessage("Motivational message saved") }
                .onFailure { postMessage(it.localizedMessage ?: "Failed to save quote") }
        }
    }

    fun adminDeleteQuote(id: String) {
        viewModelScope.launch {
            repository.deleteMotivationalMessage(id)
                .onSuccess { postMessage("Quote removed") }
                .onFailure { postMessage(it.localizedMessage ?: "Failed to remove quote") }
        }
    }

    fun adminSaveChallenge(
        id: String?,
        title: String,
        description: String,
        type: ChallengeType,
        targetCount: Long,
        xpReward: Long,
        active: Boolean
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.saveChallenge(id, title, description, type.name, targetCount, xpReward, active)
                .onSuccess { postMessage("Challenge saved") }
                .onFailure { postMessage(it.localizedMessage ?: "Failed to save challenge") }
        }
    }

    fun adminSaveAppSettings(settings: AppSettings) {
        viewModelScope.launch {
            repository.saveAppSettings(settings)
                .onSuccess { postMessage("App & Ad settings updated") }
                .onFailure { postMessage(it.localizedMessage ?: "Failed to save settings") }
        }
    }

    // --- Gamification Evaluation ---

    private suspend fun unlockBadge(key: String) {
        val spec = DefaultCatalogs.achievementSpecs.firstOrNull { it.key == key } ?: return
        val unlocked = repository.unlockAchievementIfMissing(
            userId = currentUserId,
            badgeKey = spec.key,
            title = spec.title,
            description = spec.description,
            xpReward = spec.xpReward
        ).getOrDefault(false)
        if (unlocked) {
            val currentXp = (uiState.value as? UiState.Success)?.data?.profile?.xp ?: 0L
            repository.awardXp(currentUserId, currentXp + spec.xpReward)
            postMessage("Badge Unlocked: ${spec.title} (+${spec.xpReward} XP)!")
        }
    }

    private suspend fun evaluateBalancedCategoryBadge(data: HabitFlowDashboardData, extraCategory: String) {
        val categories = (data.habits.map { it.category } + extraCategory).toSet()
        if (categories.size >= 3) {
            unlockBadge("balanced_life")
        }
    }

    private suspend fun evaluateGamification(
        data: HabitFlowDashboardData,
        newlyCompletedHabit: Boolean,
        newBaseXp: Long
    ) {
        val totalCompletions = data.completions.size + if (newlyCompletedHabit) 1 else 0
        if (totalCompletions >= 1) unlockBadge("first_step")
        if (totalCompletions >= 25) unlockBadge("habit_master_25")
        val projectedStreak = data.overallStreak.coerceAtLeast(1)
        if (projectedStreak >= 3) unlockBadge("streak_3")
        if (projectedStreak >= 7) unlockBadge("streak_7")

        val todayCount = data.completions.count { it.date == data.todayStr } + if (newlyCompletedHabit) 1 else 0
        val weekStart = HabitDateUtils.addDays(data.todayStr, -6)
        val weekCount = data.completions.count { it.date >= weekStart } + if (newlyCompletedHabit) 1 else 0

        data.challenges.forEach { ch ->
            val progressVal = when (ChallengeType.fromRaw(ch.type)) {
                ChallengeType.DAILY -> if (ch.id == "ch_routine_starter") {
                    val routineDone = data.routines.any { r ->
                        r.habitIds.isNotEmpty() && r.habitIds.all { hid ->
                            data.completions.any { it.habitId == hid && it.date == data.todayStr }
                        }
                    }
                    if (routineDone) 1L else 0L
                } else todayCount.toLong()
                ChallengeType.WEEKLY -> weekCount.toLong()
            }
            val prev = data.challengeProgress.firstOrNull { it.challengeId == ch.id }
            val completedNow = progressVal >= ch.targetCount
            repository.saveChallengeProgress(
                userId = currentUserId,
                challengeId = ch.id,
                progress = progressVal,
                target = ch.targetCount,
                completed = completedNow
            )
            if (completedNow && prev?.completed != true) {
                repository.awardXp(currentUserId, newBaseXp + ch.xpReward)
            }
        }
    }

    private data class CoreUserBundle(
        val profile: UserProfile?,
        val habits: List<Habit>,
        val completions: List<HabitCompletion>,
        val routines: List<Routine>,
        val achievements: List<UserAchievement>
    )

    private data class CatalogBundle(
        val categories: List<HabitCategory>,
        val quotes: List<MotivationalMessage>,
        val challenges: List<Challenge>,
        val progress: List<ChallengeProgress>,
        val settings: AppSettings?
    )

    private companion object {
        const val TAG = "HabitFlowVM"
    }
}
