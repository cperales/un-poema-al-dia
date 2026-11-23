package com.webpoema.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class PoemNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Fetching today.json needs the network, and onReceive runs on the main
        // thread; goAsync() lets us finish in a worker without blocking the UI.
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        TodayRepository.executor.execute {
            try {
                val poem = TodayRepository.fetchBlocking(appContext)
                if (poem != null) {
                    showNotification(appContext, poem.title, poem.author, poem.text)
                } else {
                    Log.w(TAG, "No poem of the day (offline, nothing cached); skipping")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not show the poem notification", e)
            } finally {
                // Alarms are one-shot: always schedule the next one (daily/weekly),
                // even if this one had no poem to show.
                try {
                    val frequency = PreferenceManager.getFrequency(appContext)
                    if (frequency != "never") {
                        NotificationScheduler.schedule(appContext, frequency)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not re-arm the poem alarm", e)
                }
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, title: String, author: String, text: String) {
        val channelId = "poem_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Poemas diarios",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("show_poem", true)
            putExtra("title", title)
            putExtra("author", author)
            putExtra("text", text)
        }
        val pendingOpen = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_notification_overlay)
            .setContentTitle(title)
            .setContentText("$author\n${text.take(100)}...")
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingOpen)
            .setAutoCancel(true)
            .build()

        val manager = ContextCompat.getSystemService(context, NotificationManager::class.java)
        if (manager != null && NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            manager.notify(NOTIFICATION_ID, notification)
        } else {
            Log.w(TAG, "Notifications are disabled; skipping the poem notification")
        }
    }

    private companion object {
        const val TAG = "PoemNotification"
        const val NOTIFICATION_ID = 0
    }
}
