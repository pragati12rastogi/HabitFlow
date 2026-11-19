package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.data.repository.HabitRepository
import com.example.notifications.HabitReminderScheduler
import com.example.ui.components.ActiveStopwatchBar
import com.example.ui.components.StopwatchFocusSheet
import com.example.ui.screens.AchievementsScreen
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HabitsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RoutinesScreen
import com.example.ui.screens.TrackerScreen
import com.example.ui.theme.HabitFlowTheme
import com.example.ui.theme.IndigoPrimary
import com.example.ui.viewmodel.HabitFlowViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

object HabitRoutes {
    const val DASHBOARD = "dashboard"
    const val HABITS = "habits"
    const val ROUTINES = "routines"
    const val ANALYTICS = "analytics"
    const val PROFILE = "profile"
    const val TRACKER = "tracker"
    const val ACHIEVEMENTS = "achievements"
    const val ADMIN = "admin"
}

private data class NavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val primaryTabs = listOf(
    NavTab(HabitRoutes.DASHBOARD, "Today", Icons.Filled.Home, Icons.Outlined.Home),
    NavTab(HabitRoutes.HABITS, "Habits", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircle),
    NavTab(HabitRoutes.ROUTINES, "Routines", Icons.Filled.Schedule, Icons.Outlined.Schedule),
    NavTab(HabitRoutes.ANALYTICS, "Analytics", Icons.Filled.Analytics, Icons.Outlined.Analytics),
    NavTab(HabitRoutes.PROFILE, "Profile", Icons.Filled.Person, Icons.Outlined.Person)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        HabitReminderScheduler.ensureChannel(this)
        setContent {
            HabitFlowTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            MainHabitFlowShell(user = user)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainHabitFlowShell(user: FirebaseUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: HabitFlowViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                HabitFlowViewModel(
                    repository = HabitRepository(db),
                    currentUserId = user.uid,
                    userDisplayName = user.displayName ?: "HabitFlow Member",
                    userEmail = user.email ?: "",
                    userPhotoUrl = user.photoUrl?.toString() ?: ""
                )
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val transient by viewModel.transient.collectAsStateWithLifecycle()
    val adminUsers by viewModel.adminUsers.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: HabitRoutes.DASHBOARD
    val snackbarHostState = remember { SnackbarHostState() }
    var openCreateHabitSheet by remember { mutableStateOf(false) }

    LaunchedEffect(transient.snackbarMessage) {
        val msg = transient.snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    // Keep local Android reminders synced with user preferences & habits
    val successData = (uiState as? UiState.Success)?.data
    LaunchedEffect(
        successData?.profile?.remindersEnabled,
        successData?.habits,
        successData?.routines
    ) {
        if (successData != null) {
            HabitReminderScheduler.syncAllReminders(
                context = context,
                globalEnabled = successData.profile.remindersEnabled,
                habits = successData.habits,
                routines = successData.routines
            )
        }
    }

    val isSecondaryRoute = currentRoute in setOf(
        HabitRoutes.TRACKER,
        HabitRoutes.ACHIEVEMENTS,
        HabitRoutes.ADMIN
    )

    fun navigateTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= 600.dp

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (isSecondaryRoute) {
                    val title = when (currentRoute) {
                        HabitRoutes.TRACKER -> "Daily Tracker"
                        HabitRoutes.ACHIEVEMENTS -> "Achievements & Challenges"
                        HabitRoutes.ADMIN -> "Admin Panel"
                        else -> "HabitFlow"
                    }
                    TopAppBar(
                        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                }
            },
            bottomBar = {
                if (!isExpanded && !isSecondaryRoute) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        primaryTabs.forEach { tab ->
                            val selected = navBackStackEntry?.destination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpanded && !isSecondaryRoute) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Spacer(modifier = Modifier.size(12.dp))
                        primaryTabs.forEach { tab ->
                            val selected = navBackStackEntry?.destination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) }
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(modifier = Modifier.widthIn(max = 820.dp).fillMaxSize()) {
                        when (val state = uiState) {
                            is UiState.Loading -> {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(color = IndigoPrimary)
                                    Spacer(modifier = Modifier.size(12.dp))
                                    Text(
                                        text = "Syncing your habits...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            is UiState.Error -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = MaterialTheme.colorScheme.errorContainer
                                    ) {
                                        Text(
                                            text = state.message,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.padding(18.dp)
                                        )
                                    }
                                }
                            }

                            is UiState.Success -> {
                                val data = state.data
                                val activeStopwatch = transient.activeStopwatch
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                                        NavHost(
                                            navController = navController,
                                            startDestination = HabitRoutes.DASHBOARD
                                        ) {
                                            composable(HabitRoutes.DASHBOARD) {
                                                DashboardScreen(
                                                    data = data,
                                                    onToggleHabit = { habit ->
                                                        viewModel.toggleHabitCompletion(habit, data.todayStr)
                                                    },
                                                    onStartTimer = { habit ->
                                                        viewModel.startHabitTimer(habit, data.todayStr)
                                                    },
                                                    onCreateHabit = {
                                                        openCreateHabitSheet = true
                                                        navigateTab(HabitRoutes.HABITS)
                                                    },
                                                    onLoadDemoData = { viewModel.loadDemoData() },
                                                    onOpenTracker = { navController.navigate(HabitRoutes.TRACKER) },
                                                    onOpenRoutines = { navigateTab(HabitRoutes.ROUTINES) },
                                                    onOpenAnalytics = { navigateTab(HabitRoutes.ANALYTICS) },
                                                    onOpenAchievements = { navController.navigate(HabitRoutes.ACHIEVEMENTS) }
                                                )
                                            }
                                            composable(HabitRoutes.HABITS) {
                                                HabitsScreen(
                                                    data = data,
                                                    openCreateInitially = openCreateHabitSheet,
                                                    onConsumeOpenCreate = { openCreateHabitSheet = false },
                                                    onSaveHabit = viewModel::saveHabit,
                                                    onTogglePaused = viewModel::toggleHabitPaused,
                                                    onDeleteHabit = viewModel::deleteHabit,
                                                    onOpenTracker = { navController.navigate(HabitRoutes.TRACKER) }
                                                )
                                            }
                                            composable(HabitRoutes.ROUTINES) {
                                                RoutinesScreen(
                                                    data = data,
                                                    onToggleHabitToday = { habit ->
                                                        viewModel.toggleHabitCompletion(habit, data.todayStr)
                                                    },
                                                    onStartTimer = { habit ->
                                                        viewModel.startHabitTimer(habit, data.todayStr)
                                                    },
                                                    onReorderHabit = viewModel::reorderRoutineHabit,
                                                    onSaveRoutine = viewModel::saveRoutine,
                                                    onDeleteRoutine = viewModel::deleteRoutine
                                                )
                                            }
                                            composable(HabitRoutes.ANALYTICS) {
                                                AnalyticsScreen(
                                                    data = data,
                                                    rangeDays = transient.analyticsRangeDays,
                                                    categoryFilter = transient.analyticsCategoryFilter,
                                                    onSelectRange = viewModel::setAnalyticsRange,
                                                    onSelectCategory = viewModel::setAnalyticsCategory
                                                )
                                            }
                                            composable(HabitRoutes.PROFILE) {
                                                ProfileScreen(
                                                    data = data,
                                                    onUpdateProfile = viewModel::updateProfile,
                                                    onLoadDemoData = viewModel::loadDemoData,
                                                    onOpenAchievements = { navController.navigate(HabitRoutes.ACHIEVEMENTS) },
                                                    onOpenAdmin = { navController.navigate(HabitRoutes.ADMIN) },
                                                    onSignOut = {
                                                        signOutUser(credentialManager = credentialManager, scope = scope)
                                                    },
                                                    onNotifyMessage = { msg ->
                                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                                    }
                                                )
                                            }
                                            composable(HabitRoutes.TRACKER) {
                                                TrackerScreen(
                                                    data = data,
                                                    selectedDate = transient.selectedTrackerDate,
                                                    onSelectDate = viewModel::selectTrackerDate,
                                                    onToggleHabit = { habit, date ->
                                                        viewModel.toggleHabitCompletion(habit, date)
                                                    },
                                                    onStartTimer = { habit, date ->
                                                        viewModel.startHabitTimer(habit, date)
                                                    },
                                                    onUpdateNotes = { comp, notes ->
                                                        viewModel.updateCompletionNotes(comp.id, notes)
                                                    }
                                                )
                                            }
                                            composable(HabitRoutes.ACHIEVEMENTS) {
                                                AchievementsScreen(data = data)
                                            }
                                            composable(HabitRoutes.ADMIN) {
                                                AdminScreen(
                                                    data = data,
                                                    adminUsers = adminUsers,
                                                    onSaveCategory = viewModel::adminSaveCategory,
                                                    onDeleteCategory = viewModel::adminDeleteCategory,
                                                    onSaveQuote = viewModel::adminSaveQuote,
                                                    onDeleteQuote = viewModel::adminDeleteQuote,
                                                    onSaveChallenge = viewModel::adminSaveChallenge,
                                                    onSaveAppSettings = viewModel::adminSaveAppSettings
                                                )
                                            }
                                        }
                                    }

                                    if (activeStopwatch != null) {
                                        ActiveStopwatchBar(
                                            session = activeStopwatch,
                                            onExpand = { viewModel.setStopwatchSheetExpanded(true) },
                                            onPause = viewModel::pauseHabitTimer,
                                            onResume = viewModel::resumeHabitTimer,
                                            onQuickFinish = { viewModel.finishHabitTimer(markCompleted = true) }
                                        )
                                    }
                                }

                                if (activeStopwatch != null && activeStopwatch.isSheetExpanded) {
                                    val previouslyLoggedToday = data.completions
                                        .firstOrNull {
                                            it.habitId == activeStopwatch.habitId && it.date == activeStopwatch.date
                                        }
                                        ?.durationSeconds ?: 0L
                                    StopwatchFocusSheet(
                                        session = activeStopwatch,
                                        previouslyLoggedSecondsToday = previouslyLoggedToday,
                                        onMinimize = { viewModel.setStopwatchSheetExpanded(false) },
                                        onPause = viewModel::pauseHabitTimer,
                                        onResume = viewModel::resumeHabitTimer,
                                        onReset = viewModel::resetHabitTimer,
                                        onDiscard = viewModel::discardHabitTimer,
                                        onFinishAndComplete = { notes ->
                                            viewModel.finishHabitTimer(markCompleted = true, notes = notes)
                                        },
                                        onLogTimeOnly = { notes ->
                                            viewModel.finishHabitTimer(markCompleted = false, notes = notes)
                                        }
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
