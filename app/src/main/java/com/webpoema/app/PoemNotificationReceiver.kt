package com.webpoema.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class PoemNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "WebPoema"
        val author = intent.getStringExtra("author") ?: ""
        val text = intent.getStringExtra("text") ?: ""

        showNotification(context, title, author, text)
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
            .setContentText("$author\n$text.take(100)}...")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingOpen)
            .setAutoCancel(true)
            .build()

        val manager = ContextCompat.getSystemService(context, NotificationManager::class.java)
        manager?.notify(0, notification)
    }
}
