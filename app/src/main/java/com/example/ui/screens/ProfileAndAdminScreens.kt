package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.AppSettings
import com.example.data.model.ChallengeType
import com.example.data.model.UserProfile
import com.example.notifications.HabitReminderScheduler
import com.example.ui.theme.IndigoContainer
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.MintContainer
import com.example.ui.theme.MintSuccess
import com.example.ui.viewmodel.HabitFlowDashboardData

@Composable
fun ProfileScreen(
    data: HabitFlowDashboardData,
    onUpdateProfile: (displayName: String, bio: String, remindersEnabled: Boolean, dailySummaryTime: String) -> Unit,
    onLoadDemoData: () -> Unit,
    onOpenAchievements: () -> Unit,
    onOpenAdmin: () -> Unit,
    onSignOut: () -> Unit,
    onNotifyMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var displayName by remember(data.profile.displayName) { mutableStateOf(data.profile.displayName) }
    var bio by remember(data.profile.bio) { mutableStateOf(data.profile.bio) }
    var remindersEnabled by remember(data.profile.remindersEnabled) { mutableStateOf(data.profile.remindersEnabled) }
    var summaryTime by remember(data.profile.dailySummaryTime) { mutableStateOf(data.profile.dailySummaryTime.ifBlank { "08:00" }) }
    var hasNotifPermission by remember { mutableStateOf(HabitReminderScheduler.hasNotificationPermission(context)) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
        if (granted) {
            HabitReminderScheduler.sendTestNotification(context)
            onNotifyMessage("Notifications enabled!")
        } else {
            onNotifyMessage("Notification permission declined. You can enable it in system settings.")
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User Header Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (data.profile.photoUrl.isNotBlank()) {
                        AsyncImage(
                            model = data.profile.photoUrl,
                            contentDescription = "Profile photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(IndigoContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Person, contentDescription = "User avatar", tint = IndigoPrimary, modifier = Modifier.size(32.dp))
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(data.profile.displayName, style = MaterialTheme.typography.titleLarge)
                        if (data.profile.email.isNotBlank()) {
                            Text(
                                text = data.profile.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(shape = RoundedCornerShape(50), color = IndigoContainer) {
                                Text(
                                    text = "Level ${data.profile.level} · ${data.profile.xp} XP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = IndigoPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            if (data.isAdmin) {
                                Surface(shape = RoundedCornerShape(50), color = MintContainer) {
                                    Text(
                                        text = "Admin",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF15803D),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Achievements & Gamification Shortcut
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = IndigoContainer.copy(alpha = 0.55f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Outlined.EmojiEvents, contentDescription = "Achievements", tint = IndigoPrimary)
                        Column {
                            Text("Achievements & Challenges", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${data.achievements.size} badges unlocked",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Button(
                        onClick = onOpenAchievements,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary, contentColor = Color.White)
                    ) {
                        Text("View")
                    }
                }
            }
        }

        // Profile & Reminder Preferences Form
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Account & Notification Preferences", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { if (it.length <= 200) bio = it },
                        label = { Text("Personal Goal / Motto") },
                        maxLines = 2,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Habit & Routine Reminders", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = if (hasNotifPermission) "Permission granted" else "Tap Test Notification to grant permission",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = remindersEnabled, onCheckedChange = { remindersEnabled = it })
                    }
                    OutlinedTextField(
                        value = summaryTime,
                        onValueChange = { if (it.length <= 5) summaryTime = it },
                        label = { Text("Daily Reminder Time (HH:mm)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !HabitReminderScheduler.hasNotificationPermission(context)) {
                                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    HabitReminderScheduler.sendTestNotification(context)
                                    onNotifyMessage("Test notification sent!")
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Test Alert")
                        }
                        Button(
                            onClick = { onUpdateProfile(displayName, bio, remindersEnabled, summaryTime) },
                            enabled = displayName.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary, contentColor = Color.White),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }

        // Demo Data Seeder
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = "Demo data", tint = IndigoPrimary)
                        Column {
                            Text("Sample Habits & History", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "Seed realistic habits, routines, and 10-day analytics",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onLoadDemoData,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("load_demo_button")
                    ) {
                        Text("Load Demo")
                    }
                }
            }
        }

        // Admin Panel Entry (Only visible for verified admin)
        if (data.isAdmin) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MintContainer.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Filled.AdminPanelSettings, contentDescription = "Admin Dashboard", tint = Color(0xFF15803D))
                            Column {
                                Text("Admin Dashboard & Settings", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Manage categories, quotes, challenges & AdSense config",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Button(
                            onClick = onOpenAdmin,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MintSuccess, contentColor = Color.White)
                        ) {
                            Text("Open")
                        }
                    }
                }
            }
        }

        // Sign Out
        item {
            OutlinedButton(
                onClick = onSignOut,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("Sign Out")
            }
        }
    }
}

@Composable
fun AdminScreen(
    data: HabitFlowDashboardData,
    adminUsers: List<UserProfile>,
    onSaveCategory: (id: String?, name: String, iconName: String, colorHex: String, active: Boolean) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onSaveQuote: (id: String?, quote: String, author: String, active: Boolean) -> Unit,
    onDeleteQuote: (String) -> Unit,
    onSaveChallenge: (id: String?, title: String, desc: String, type: ChallengeType, target: Long, xp: Long, active: Boolean) -> Unit,
    onSaveAppSettings: (AppSettings) -> Unit
) {
    var adsEnabled by remember(data.appSettings) { mutableStateOf(data.appSettings.adsEnabled) }
    var pubId by remember(data.appSettings) { mutableStateOf(data.appSettings.adPublisherId) }
    var dashSlot by remember(data.appSettings) { mutableStateOf(data.appSettings.dashboardAdSlotId) }
    var analyticsSlot by remember(data.appSettings) { mutableStateOf(data.appSettings.analyticsAdSlotId) }
    var showOnDash by remember(data.appSettings) { mutableStateOf(data.appSettings.showAdsOnDashboard) }
    var showOnAnalytics by remember(data.appSettings) { mutableStateOf(data.appSettings.showAdsOnAnalytics) }
    var announcement by remember(data.appSettings) { mutableStateOf(data.appSettings.announcement) }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddQuoteDialog by remember { mutableStateOf(false) }
    var showAddChallengeDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text("Admin Dashboard", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "Platform overview, content management & AdSense config",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Overview Metrics
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AdminStatBox("Users", "${adminUsers.size.coerceAtLeast(1)}", Modifier.weight(1f))
                AdminStatBox("Categories", "${data.categories.size}", Modifier.weight(1f))
                AdminStatBox("Challenges", "${data.challenges.size}", Modifier.weight(1f))
            }
        }

        // AdSense & App Settings Configuration
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("AdSense & App Settings", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Configure your Publisher ID and responsive slot IDs. Ads never appear on login or account pages.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Display Ads", style = MaterialTheme.typography.titleSmall)
                        Switch(checked = adsEnabled, onCheckedChange = { adsEnabled = it })
                    }
                    OutlinedTextField(
                        value = pubId,
                        onValueChange = { pubId = it },
                        label = { Text("AdSense Publisher ID (e.g. ca-pub-xxxxxxxx)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dashSlot,
                        onValueChange = { dashSlot = it },
                        label = { Text("Dashboard Ad Slot ID") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = analyticsSlot,
                        onValueChange = { analyticsSlot = it },
                        label = { Text("Analytics Ad Slot ID") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show on Dashboard", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = showOnDash, onCheckedChange = { showOnDash = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show on Analytics", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = showOnAnalytics, onCheckedChange = { showOnAnalytics = it })
                    }
                    OutlinedTextField(
                        value = announcement,
                        onValueChange = { if (it.length <= 240) announcement = it },
                        label = { Text("Global Announcement Banner (optional)") },
                        maxLines = 2,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            onSaveAppSettings(
                                AppSettings(
                                    adsEnabled = adsEnabled,
                                    adPublisherId = pubId,
                                    dashboardAdSlotId = dashSlot,
                                    analyticsAdSlotId = analyticsSlot,
                                    showAdsOnDashboard = showOnDash,
                                    showAdsOnAnalytics = showOnAnalytics,
                                    maintenanceMode = false,
                                    announcement = announcement
                                )
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Admin Settings")
                    }
                }
            }
        }

        // Manage Categories
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Habit Categories", style = MaterialTheme.typography.titleLarge)
                        TextButton(onClick = { showAddCategoryDialog = true }) { Text("+ Add") }
                    }
                    data.categories.forEach { cat ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${cat.name} (${cat.colorHex})", style = MaterialTheme.typography.bodyMedium)
                            if (!cat.id.startsWith("cat_")) {
                                IconButton(onClick = { onDeleteCategory(cat.id) }) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete category")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Manage Motivational Messages
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Motivational Messages", style = MaterialTheme.typography.titleLarge)
                        TextButton(onClick = { showAddQuoteDialog = true }) { Text("+ Add") }
                    }
                    data.motivationalMessages.forEach { msg ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("\"${msg.quote}\"", style = MaterialTheme.typography.bodySmall)
                                Text("— ${msg.author}", style = MaterialTheme.typography.labelSmall, color = IndigoPrimary)
                            }
                            if (!msg.id.startsWith("quote_")) {
                                IconButton(onClick = { onDeleteQuote(msg.id) }) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete quote")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Manage Challenges
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Challenges", style = MaterialTheme.typography.titleLarge)
                        TextButton(onClick = { showAddChallengeDialog = true }) { Text("+ Add") }
                    }
                    data.challenges.forEach { ch ->
                        Column {
                            Text("${ch.title} (${ch.type})", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "Target: ${ch.targetCount} · +${ch.xpReward} XP",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddCategoryDialog) {
        var name by remember { mutableStateOf("") }
        var color by remember { mutableStateOf("#6C63FF") }
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("New Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Category Name") })
                    OutlinedTextField(value = color, onValueChange = { color = it }, label = { Text("Hex Color") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onSaveCategory(null, name, "star", color, true)
                    showAddCategoryDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddQuoteDialog) {
        var quote by remember { mutableStateOf("") }
        var author by remember { mutableStateOf("HabitFlow") }
        AlertDialog(
            onDismissRequest = { showAddQuoteDialog = false },
            title = { Text("New Motivational Message") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = quote, onValueChange = { quote = it }, label = { Text("Quote") }, maxLines = 3)
                    OutlinedTextField(value = author, onValueChange = { author = it }, label = { Text("Author") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onSaveQuote(null, quote, author, true)
                    showAddQuoteDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showAddQuoteDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddChallengeDialog) {
        var title by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var type by remember { mutableStateOf(ChallengeType.DAILY) }
        var target by remember { mutableStateOf("5") }
        var xp by remember { mutableStateOf("100") }
        AlertDialog(
            onDismissRequest = { showAddChallengeDialog = false },
            title = { Text("New Challenge") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChallengeType.entries.forEach { t ->
                            FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t.label) })
                        }
                    }
                    OutlinedTextField(value = target, onValueChange = { target = it }, label = { Text("Target Count") })
                    OutlinedTextField(value = xp, onValueChange = { xp = it }, label = { Text("XP Reward") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onSaveChallenge(
                        null,
                        title,
                        desc.ifBlank { "Complete $target habits" },
                        type,
                        target.toLongOrNull() ?: 3L,
                        xp.toLongOrNull() ?: 50L,
                        true
                    )
                    showAddChallengeDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showAddChallengeDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun AdminStatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = IndigoPrimary
            )
        }
    }
}
