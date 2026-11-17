package com.webpoema.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.DayOfWeek
import java.time.ZonedDateTime

object NotificationScheduler {

    const val ACTION_SHOW_POEM = "com.webpoema.app.ACTION_SHOW_POEM"
    const val NOTIFICATION_HOUR = 9

    private const val REQUEST_CODE = 0

    /**
     * (Re)schedules the next poem notification at 09:00 local time.
     *
     * The poem is resolved when the alarm fires, not here, so the notification
     * always shows that day's poem instead of one frozen at save time.
     */
    fun schedule(context: Context, frequency: String) {
        cancel(context)
        if (frequency == "never") return

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            alarmIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAtMillis = nextTrigger(frequency)

        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (canScheduleExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                // Best effort when the exact-alarm permission is not granted.
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun cancel(context: Context) {
        val pending = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            alarmIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pending)
    }

    /** Next 09:00 local time; "weekly" waits for the next Sunday at 09:00. */
    fun nextTrigger(frequency: String, now: ZonedDateTime = ZonedDateTime.now()): Long {
        var next = now
            .withHour(NOTIFICATION_HOUR)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)

        if (frequency == "weekly") {
            while (next.dayOfWeek != DayOfWeek.SUNDAY) next = next.plusDays(1)
        }
        return next.toInstant().toEpochMilli()
    }

    private fun alarmIntent(context: Context): Intent =
        Intent(context, PoemNotificationReceiver::class.java).apply {
            action = ACTION_SHOW_POEM
        }
}
