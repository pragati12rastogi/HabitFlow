package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AppSettings
import com.example.data.model.Challenge
import com.example.data.model.ChallengeProgress
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
import com.example.domain.HabitDateUtils
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class HabitRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun <T> Flow<T>.handleSnapshotErrors(
        expectedUid: String?,
        opType: OperationType,
        path: String
    ): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && (expectedUid == null || auth.currentUser?.uid == expectedUid)) {
            handleFirestoreError(exceptionToLog, opType, path)
        }
        throw error
    }

    // --- Real-Time Observation Flows ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { it.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .handleSnapshotErrors(userId, OperationType.GET, path)
        )
    }

    fun observeHabits(userId: String): Flow<List<Habit>> = flow {
        val path = "users/$userId/habits"
        emitAll(
            db.collection("users").document(userId).collection("habits")
                .snapshots()
                .map { snap -> snap.toObjects(Habit::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .handleSnapshotErrors(userId, OperationType.LIST, path)
        )
    }

    fun observeCompletions(userId: String): Flow<List<HabitCompletion>> = flow {
        val path = "users/$userId/completions"
        emitAll(
            db.collection("users").document(userId).collection("completions")
                .snapshots()
                .map { snap -> snap.toObjects(HabitCompletion::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .handleSnapshotErrors(userId, OperationType.LIST, path)
        )
    }

    fun observeRoutines(userId: String): Flow<List<Routine>> = flow {
        val path = "users/$userId/routines"
        emitAll(
            db.collection("users").document(userId).collection("routines")
                .snapshots()
                .map { snap -> snap.toObjects(Routine::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .handleSnapshotErrors(userId, OperationType.LIST, path)
        )
    }

    fun observeAchievements(userId: String): Flow<List<UserAchievement>> = flow {
        val path = "users/$userId/achievements"
        emitAll(
            db.collection("users").document(userId).collection("achievements")
                .snapshots()
                .map { snap -> snap.toObjects(UserAchievement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .handleSnapshotErrors(userId, OperationType.LIST, path)
        )
    }

    fun observeChallengeProgress(userId: String): Flow<List<ChallengeProgress>> = flow {
        val path = "users/$userId/challengeProgress"
        emitAll(
            db.collection("users").document(userId).collection("challengeProgress")
                .snapshots()
                .map { snap -> snap.toObjects(ChallengeProgress::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .handleSnapshotErrors(userId, OperationType.LIST, path)
        )
    }

    fun observeCategories(): Flow<List<HabitCategory>> = flow {
        val path = "categories"
        emitAll(
            db.collection("categories")
                .snapshots()
                .map { snap -> snap.toObjects(HabitCategory::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .handleSnapshotErrors(null, OperationType.LIST, path)
        )
    }

    fun observeMotivationalMessages(): Flow<List<MotivationalMessage>> = flow {
        val path = "motivational_messages"
        emitAll(
            db.collection("motivational_messages")
                .snapshots()
                .map { snap -> snap.toObjects(MotivationalMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .handleSnapshotErrors(null, OperationType.LIST, path)
        )
    }

    fun observeChallenges(): Flow<List<Challenge>> = flow {
        val path = "challenges"
        emitAll(
            db.collection("challenges")
                .snapshots()
                .map { snap -> snap.toObjects(Challenge::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .handleSnapshotErrors(null, OperationType.LIST, path)
        )
    }

    fun observeAppSettings(): Flow<AppSettings?> = flow {
        val path = "app_config/settings"
        emitAll(
            db.collection("app_config").document("settings")
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(AppSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else {
                        null
                    }
                }
                .handleSnapshotErrors(null, OperationType.GET, path)
        )
    }

    fun observeAllUsersForAdmin(): Flow<List<UserProfile>> = flow {
        val path = "users"
        emitAll(
            db.collection("users")
                .snapshots()
                .map { snap -> snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .handleSnapshotErrors(null, OperationType.LIST, path)
        )
    }

    // --- User Profile Mutations ---

    suspend fun ensureUserProfile(
        userId: String,
        displayName: String,
        email: String,
        photoUrl: String
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val docRef = db.collection("users").document(userId)
            val snap = docRef.get().await()
            val safeName = displayName.trim().ifBlank { "HabitFlow Member" }.take(80)
            val safeEmail = email.trim().take(120)
            val safePhoto = photoUrl.trim().take(500)
            if (!snap.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to safeEmail,
                    "photoUrl" to safePhoto,
                    "bio" to "Building daily consistency one step at a time.",
                    "xp" to 0L,
                    "level" to 1L,
                    "remindersEnabled" to true,
                    "dailySummaryTime" to "08:00",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                docRef.update(
                    mapOf(
                        "displayName" to safeName,
                        "email" to safeEmail,
                        "photoUrl" to safePhoto,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(
        userId: String,
        displayName: String,
        bio: String,
        remindersEnabled: Boolean,
        dailySummaryTime: String
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val cleanName = displayName.trim().take(80)
            require(cleanName.isNotEmpty()) { "Display name is required" }
            db.collection("users").document(userId).update(
                mapOf(
                    "displayName" to cleanName,
                    "bio" to bio.trim().take(200),
                    "remindersEnabled" to remindersEnabled,
                    "dailySummaryTime" to dailySummaryTime.trim().take(10),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun awardXp(userId: String, newXp: Long): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val boundedXp = newXp.coerceIn(0L, 10_000_000L)
            val newLevel = ((boundedXp / 100L) + 1L).coerceIn(1L, 1000L)
            db.collection("users").document(userId).update(
                mapOf(
                    "xp" to boundedXp,
                    "level" to newLevel,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    // --- Habit Mutations ---

    suspend fun saveHabit(
        userId: String,
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
    ): Result<String> {
        var docPath = "users/$userId/habits"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val cleanName = name.trim().take(100)
            require(cleanName.isNotEmpty()) { "Habit name cannot be empty" }
            val cleanCategory = category.trim().ifBlank { "Health" }.take(50)
            val cleanDays = targetDays.distinct().filter { it in 1L..7L }.take(7)
            val cleanFreq = targetFrequency.coerceIn(1L, 20L)
            val cleanStart = startDate.trim().take(10).ifBlank { HabitDateUtils.todayIso() }
            require(cleanStart.length == 10) { "Start date must be YYYY-MM-DD" }

            val colRef = db.collection("users").document(uid).collection("habits")
            if (habitId.isNullOrBlank()) {
                val docRef = colRef.document()
                docPath = docRef.path
                val payload = mapOf(
                    "userId" to uid,
                    "name" to cleanName,
                    "description" to description.trim().take(400),
                    "category" to cleanCategory,
                    "scheduleType" to scheduleType.name,
                    "targetDays" to cleanDays,
                    "targetFrequency" to cleanFreq,
                    "priority" to priority.name,
                    "startDate" to cleanStart,
                    "reminderTime" to reminderTime.trim().take(10),
                    "remindersEnabled" to remindersEnabled,
                    "paused" to paused,
                    "colorHex" to colorHex.trim().take(12).ifBlank { "#6C63FF" },
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                Result.success(docRef.id)
            } else {
                val docRef = colRef.document(habitId)
                docPath = docRef.path
                docRef.update(
                    mapOf(
                        "name" to cleanName,
                        "description" to description.trim().take(400),
                        "category" to cleanCategory,
                        "scheduleType" to scheduleType.name,
                        "targetDays" to cleanDays,
                        "targetFrequency" to cleanFreq,
                        "priority" to priority.name,
                        "startDate" to cleanStart,
                        "reminderTime" to reminderTime.trim().take(10),
                        "remindersEnabled" to remindersEnabled,
                        "paused" to paused,
                        "colorHex" to colorHex.trim().take(12).ifBlank { "#6C63FF" },
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success(habitId)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (habitId == null) OperationType.CREATE else OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleHabitPaused(userId: String, habitId: String, paused: Boolean): Result<Unit> {
        val docPath = "users/$userId/habits/$habitId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(habitId.isNotBlank())
            db.collection("users").document(uid).collection("habits").document(habitId)
                .update(mapOf("paused" to paused, "updatedAt" to FieldValue.serverTimestamp()))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteHabit(userId: String, habitId: String): Result<Unit> {
        val docPath = "users/$userId/habits/$habitId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(habitId.isNotBlank())
            db.collection("users").document(uid).collection("habits").document(habitId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- Daily Completion Mutations (Deterministic ID prevents duplicates) ---

    suspend fun toggleHabitCompletion(
        userId: String,
        habit: Habit,
        date: String,
        existingCompletion: HabitCompletion?,
        notes: String = ""
    ): Result<Boolean> {
        val completionId = "${habit.id}_$date"
        val docPath = "users/$userId/completions/$completionId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(habit.id.isNotBlank() && date.length == 10)
            val docRef = db.collection("users").document(uid)
                .collection("completions").document(completionId)

            val isAlreadyFullyCompleted = existingCompletion != null &&
                existingCompletion.completedCount >= habit.targetFrequency

            if (isAlreadyFullyCompleted) {
                // Undo completion
                docRef.delete().await()
                Result.success(false)
            } else {
                val nextCount = ((existingCompletion?.completedCount ?: 0L) + 1L)
                    .coerceIn(1L, habit.targetFrequency.coerceIn(1L, 20L))
                val snap = docRef.get().await()
                if (!snap.exists()) {
                    docRef.set(
                        mapOf(
                            "userId" to uid,
                            "habitId" to habit.id,
                            "habitName" to habit.name.take(100),
                            "category" to habit.category.take(50),
                            "date" to date,
                            "completedCount" to nextCount,
                            "targetFrequency" to habit.targetFrequency.coerceIn(1L, 20L),
                            "notes" to notes.trim().take(300),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } else {
                    docRef.update(
                        mapOf(
                            "habitName" to habit.name.take(100),
                            "category" to habit.category.take(50),
                            "completedCount" to nextCount,
                            "targetFrequency" to habit.targetFrequency.coerceIn(1L, 20L),
                            "notes" to (notes.ifBlank { existingCompletion?.notes ?: "" }).trim().take(300),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
                Result.success(nextCount >= habit.targetFrequency)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateCompletionNotes(
        userId: String,
        completionId: String,
        notes: String
    ): Result<Unit> {
        val docPath = "users/$userId/completions/$completionId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(completionId.isNotBlank())
            db.collection("users").document(uid)
                .collection("completions").document(completionId)
                .update(
                    mapOf(
                        "notes" to notes.trim().take(300),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    // --- Routine Mutations ---

    suspend fun saveRoutine(
        userId: String,
        routineId: String?,
        name: String,
        timeOfDay: RoutineTimeOfDay,
        startTime: String,
        habitIds: List<String>,
        remindersEnabled: Boolean
    ): Result<String> {
        var docPath = "users/$userId/routines"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val cleanName = name.trim().take(100)
            require(cleanName.isNotEmpty()) { "Routine name is required" }
            val cleanTime = startTime.trim().ifBlank { timeOfDay.defaultTime }.take(10)
            val cleanHabits = habitIds.distinct().take(30)
            val colRef = db.collection("users").document(uid).collection("routines")
            if (routineId.isNullOrBlank()) {
                val docRef = colRef.document()
                docPath = docRef.path
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "name" to cleanName,
                        "timeOfDay" to timeOfDay.name,
                        "startTime" to cleanTime,
                        "habitIds" to cleanHabits,
                        "remindersEnabled" to remindersEnabled,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success(docRef.id)
            } else {
                val docRef = colRef.document(routineId)
                docPath = docRef.path
                docRef.update(
                    mapOf(
                        "name" to cleanName,
                        "timeOfDay" to timeOfDay.name,
                        "startTime" to cleanTime,
                        "habitIds" to cleanHabits,
                        "remindersEnabled" to remindersEnabled,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success(routineId)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteRoutine(userId: String, routineId: String): Result<Unit> {
        val docPath = "users/$userId/routines/$routineId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(routineId.isNotBlank())
            db.collection("users").document(uid).collection("routines").document(routineId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- Gamification Mutations ---

    suspend fun unlockAchievementIfMissing(
        userId: String,
        badgeKey: String,
        title: String,
        description: String,
        xpReward: Long
    ): Result<Boolean> {
        val docPath = "users/$userId/achievements/$badgeKey"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val docRef = db.collection("users").document(uid)
                .collection("achievements").document(badgeKey)
            val snap = docRef.get().await()
            if (snap.exists()) {
                Result.success(false)
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "badgeKey" to badgeKey.take(60),
                        "title" to title.take(80),
                        "description" to description.take(200),
                        "xpReward" to xpReward.coerceIn(0L, 5000L),
                        "unlockedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success(true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveChallengeProgress(
        userId: String,
        challengeId: String,
        progress: Long,
        target: Long,
        completed: Boolean
    ): Result<Unit> {
        val docPath = "users/$userId/challengeProgress/$challengeId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val docRef = db.collection("users").document(uid)
                .collection("challengeProgress").document(challengeId)
            val snap = docRef.get().await()
            val boundedTarget = target.coerceIn(1L, 1000L)
            val boundedProgress = progress.coerceIn(0L, 1000L)
            val payload = mapOf(
                "userId" to uid,
                "challengeId" to challengeId.take(128),
                "progress" to boundedProgress,
                "target" to boundedTarget,
                "completed" to completed,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!snap.exists()) {
                docRef.set(payload).await()
            } else {
                val alreadyDone = snap.getBoolean("completed") == true
                docRef.update(
                    mapOf(
                        "progress" to boundedProgress,
                        "target" to boundedTarget,
                        "completed" to (alreadyDone || completed),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    // --- Realistic Demo Data Seeder (Explicitly triggered by user) ---

    suspend fun seedDemoData(userId: String): Result<Unit> {
        val basePath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val existingDemo = db.collection("users").document(uid)
                .collection("habits").document("demo_hydrate").get().await()
            if (existingDemo.exists()) {
                return Result.success(Unit)
            }
            val today = HabitDateUtils.todayIso()
            val start = HabitDateUtils.addDays(today, -14)
            val batch = db.batch()

            val h1 = db.collection("users").document(uid).collection("habits").document("demo_hydrate")
            val h2 = db.collection("users").document(uid).collection("habits").document("demo_meditate")
            val h3 = db.collection("users").document(uid).collection("habits").document("demo_read")
            val h4 = db.collection("users").document(uid).collection("habits").document("demo_workout")

            batch.set(h1, mapOf(
                "userId" to uid, "name" to "Morning Hydration", "description" to "Drink 500ml water right after waking up",
                "category" to "Health", "scheduleType" to "DAILY", "targetDays" to listOf(1L,2L,3L,4L,5L,6L,7L),
                "targetFrequency" to 1L, "priority" to "HIGH", "startDate" to start,
                "reminderTime" to "07:15", "remindersEnabled" to true, "paused" to false, "colorHex" to "#0EA5E9",
                "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
            ))
            batch.set(h2, mapOf(
                "userId" to uid, "name" to "Mindful Breathing", "description" to "10 minutes of calm guided focus",
                "category" to "Mindfulness", "scheduleType" to "DAILY", "targetDays" to listOf(1L,2L,3L,4L,5L,6L,7L),
                "targetFrequency" to 1L, "priority" to "HIGH", "startDate" to start,
                "reminderTime" to "07:30", "remindersEnabled" to true, "paused" to false, "colorHex" to "#14B8A6",
                "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
            ))
            batch.set(h3, mapOf(
                "userId" to uid, "name" to "Read 20 Pages", "description" to "Non-fiction or personal growth reading",
                "category" to "Learning", "scheduleType" to "DAILY", "targetDays" to listOf(1L,2L,3L,4L,5L,6L,7L),
                "targetFrequency" to 1L, "priority" to "MEDIUM", "startDate" to start,
                "reminderTime" to "21:00", "remindersEnabled" to false, "paused" to false, "colorHex" to "#6C63FF",
                "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
            ))
            batch.set(h4, mapOf(
                "userId" to uid, "name" to "Strength & Mobility", "description" to "30 min functional workout",
                "category" to "Fitness", "scheduleType" to "WEEKLY", "targetDays" to listOf(1L,3L,5L,6L),
                "targetFrequency" to 1L, "priority" to "MEDIUM", "startDate" to start,
                "reminderTime" to "17:30", "remindersEnabled" to false, "paused" to false, "colorHex" to "#F59E0B",
                "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
            ))

            val r1 = db.collection("users").document(uid).collection("routines").document("demo_morning_flow")
            batch.set(r1, mapOf(
                "userId" to uid, "name" to "Energized Morning", "timeOfDay" to "MORNING",
                "startTime" to "07:00", "habitIds" to listOf("demo_hydrate", "demo_meditate"),
                "remindersEnabled" to true,
                "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
            ))

            val r2 = db.collection("users").document(uid).collection("routines").document("demo_evening_winddown")
            batch.set(r2, mapOf(
                "userId" to uid, "name" to "Evening Wind-Down", "timeOfDay" to "EVENING",
                "startTime" to "20:30", "habitIds" to listOf("demo_read"),
                "remindersEnabled" to false,
                "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
            ))

            // Seed past 10 days of realistic completion history (leaving today ready for interactive toggling!)
            for (offset in 1..10) {
                val date = HabitDateUtils.addDays(today, -offset)
                val comp1 = db.collection("users").document(uid).collection("completions").document("demo_hydrate_$date")
                batch.set(comp1, mapOf(
                    "userId" to uid, "habitId" to "demo_hydrate", "habitName" to "Morning Hydration",
                    "category" to "Health", "date" to date, "completedCount" to 1L, "targetFrequency" to 1L,
                    "notes" to "Refreshed start!", "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
                ))
                if (offset <= 6 || offset == 8) {
                    val comp2 = db.collection("users").document(uid).collection("completions").document("demo_meditate_$date")
                    batch.set(comp2, mapOf(
                        "userId" to uid, "habitId" to "demo_meditate", "habitName" to "Mindful Breathing",
                        "category" to "Mindfulness", "date" to date, "completedCount" to 1L, "targetFrequency" to 1L,
                        "notes" to "", "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
                    ))
                }
                if (offset % 2 == 1) {
                    val comp3 = db.collection("users").document(uid).collection("completions").document("demo_read_$date")
                    batch.set(comp3, mapOf(
                        "userId" to uid, "habitId" to "demo_read", "habitName" to "Read 20 Pages",
                        "category" to "Learning", "date" to date, "completedCount" to 1L, "targetFrequency" to 1L,
                        "notes" to "Great chapter", "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
                    ))
                }
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, basePath)
            Result.failure(e)
        }
    }

    // --- Admin Catalog & Settings Mutations ---

    suspend fun saveCategory(
        categoryId: String?,
        name: String,
        iconName: String,
        colorHex: String,
        active: Boolean
    ): Result<Unit> {
        val path = "categories/${categoryId ?: "new"}"
        return try {
            requireUserId()
            val colRef = db.collection("categories")
            if (categoryId.isNullOrBlank()) {
                colRef.document().set(
                    mapOf(
                        "name" to name.trim().take(50),
                        "iconName" to iconName.trim().take(40).ifBlank { "star" },
                        "colorHex" to colorHex.trim().take(12).ifBlank { "#6C63FF" },
                        "active" to active,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                colRef.document(categoryId).update(
                    mapOf(
                        "name" to name.trim().take(50),
                        "iconName" to iconName.trim().take(40).ifBlank { "star" },
                        "colorHex" to colorHex.trim().take(12).ifBlank { "#6C63FF" },
                        "active" to active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCategory(categoryId: String): Result<Unit> {
        val path = "categories/$categoryId"
        return try {
            requireUserId()
            db.collection("categories").document(categoryId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveMotivationalMessage(
        messageId: String?,
        quote: String,
        author: String,
        active: Boolean
    ): Result<Unit> {
        val path = "motivational_messages/${messageId ?: "new"}"
        return try {
            requireUserId()
            val colRef = db.collection("motivational_messages")
            if (messageId.isNullOrBlank()) {
                colRef.document().set(
                    mapOf(
                        "quote" to quote.trim().take(280),
                        "author" to author.trim().take(80).ifBlank { "HabitFlow" },
                        "active" to active,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                colRef.document(messageId).update(
                    mapOf(
                        "quote" to quote.trim().take(280),
                        "author" to author.trim().take(80).ifBlank { "HabitFlow" },
                        "active" to active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteMotivationalMessage(messageId: String): Result<Unit> {
        val path = "motivational_messages/$messageId"
        return try {
            requireUserId()
            db.collection("motivational_messages").document(messageId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveChallenge(
        challengeId: String?,
        title: String,
        description: String,
        type: String,
        targetCount: Long,
        xpReward: Long,
        active: Boolean
    ): Result<Unit> {
        val path = "challenges/${challengeId ?: "new"}"
        return try {
            requireUserId()
            val colRef = db.collection("challenges")
            if (challengeId.isNullOrBlank()) {
                colRef.document().set(
                    mapOf(
                        "title" to title.trim().take(100),
                        "description" to description.trim().take(300),
                        "type" to type,
                        "targetCount" to targetCount.coerceIn(1L, 100L),
                        "xpReward" to xpReward.coerceIn(1L, 5000L),
                        "active" to active,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                colRef.document(challengeId).update(
                    mapOf(
                        "title" to title.trim().take(100),
                        "description" to description.trim().take(300),
                        "type" to type,
                        "targetCount" to targetCount.coerceIn(1L, 100L),
                        "xpReward" to xpReward.coerceIn(1L, 5000L),
                        "active" to active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun saveAppSettings(settings: AppSettings): Result<Unit> {
        val path = "app_config/settings"
        return try {
            requireUserId()
            val docRef = db.collection("app_config").document("settings")
            val snap = docRef.get().await()
            val payload = mapOf(
                "adsEnabled" to settings.adsEnabled,
                "adPublisherId" to settings.adPublisherId.trim().take(100),
                "dashboardAdSlotId" to settings.dashboardAdSlotId.trim().take(100),
                "analyticsAdSlotId" to settings.analyticsAdSlotId.trim().take(100),
                "showAdsOnDashboard" to settings.showAdsOnDashboard,
                "showAdsOnAnalytics" to settings.showAdsOnAnalytics,
                "maintenanceMode" to settings.maintenanceMode,
                "announcement" to settings.announcement.trim().take(240),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!snap.exists()) {
                docRef.set(payload).await()
            } else {
                docRef.update(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}
