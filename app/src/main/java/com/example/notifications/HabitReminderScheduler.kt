package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.Habit
import com.example.data.model.Routine
import java.util.Calendar

object HabitReminderScheduler {
    const val CHANNEL_ID = "habitflow_reminders"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Habit & Routine Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Gentle reminders for your scheduled habits and daily routines"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun syncAllReminders(
        context: Context,
        globalEnabled: Boolean,
        habits: List<Habit>,
        routines: List<Routine>
    ) {
        ensureChannel(context)
        if (!globalEnabled) {
            habits.forEach { cancelReminder(context, it.id.hashCode()) }
            routines.forEach { cancelReminder(context, ("routine_" + it.id).hashCode()) }
            return
        }
        habits.forEach { habit ->
            val requestCode = habit.id.hashCode()
            if (habit.remindersEnabled && !habit.paused && habit.reminderTime.isNotBlank()) {
                scheduleDailyReminder(
                    context = context,
                    requestCode = requestCode,
                    timeHHmm = habit.reminderTime,
                    title = "Time for ${habit.name}",
                    message = habit.description.ifBlank { "Keep your ${habit.category} streak going!" }
                )
            } else {
                cancelReminder(context, requestCode)
            }
        }
        routines.forEach { routine ->
            val requestCode = ("routine_" + routine.id).hashCode()
            if (routine.remindersEnabled && routine.startTime.isNotBlank()) {
                scheduleDailyReminder(
                    context = context,
                    requestCode = requestCode,
                    timeHHmm = routine.startTime,
                    title = "${routine.name} starts now",
                    message = "${routine.habitIds.size} habits waiting in your routine."
                )
            } else {
                cancelReminder(context, requestCode)
            }
        }
    }

    private fun scheduleDailyReminder(
        context: Context,
        requestCode: Int,
        timeHHmm: String,
        title: String,
        message: String
    ) {
        val parts = timeHHmm.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: return
        val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0

        val now = Calendar.getInstance()
        val trigger = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_MESSAGE, message)
            putExtra(ReminderReceiver.EXTRA_NOTIFICATION_ID, requestCode)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            trigger.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    private fun cancelReminder(context: Context, requestCode: Int) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun sendTestNotification(context: Context): Boolean {
        ensureChannel(context)
        if (!hasNotificationPermission(context)) return false
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            9999,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("HabitFlow Reminders Active")
            .setContentText("You're all set! We'll gently nudge you at your scheduled times.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        NotificationManagerCompat.from(context).notify(9999, notification)
        return true
    }
}
