package com.webpoema.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

object NotificationScheduler {

    private const val DAILY_INTENT = "com.webpoema.app.DAILY"
    private const val WEEKLY_INTENT = "com.webpoema.app.WEEKLY"

    fun schedule(context: Context, frequency: String) {
        cancel(context)

        if (frequency == "never") return

        val poem = PoemLoader.getRandomPoem(context)

        val intent = Intent(context, PoemNotificationReceiver::class.java).apply {
            action = "com.webpoema.app.ACTION_SHOW_POEM"
            putExtra("title", poem.title)
            putExtra("author", poem.author)
            putExtra("text", poem.text)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Check if exact alarm permission is granted
        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        if (!canScheduleExact) {
            // Request permission
            val intentExact = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            context.startActivity(intentExact)
        }

        val triggerAtMillis = System.currentTimeMillis() + getMillis(frequency)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun cancel(context: Context) {
        val intent = Intent(context, PoemNotificationReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pending)
    }

    private fun getMillis(freq: String): Long {
        return when (freq) {
            "daily" -> AlarmManager.INTERVAL_DAY
            "weekly" -> AlarmManager.INTERVAL_DAY * 7
            else -> 0
        }
    }
}
