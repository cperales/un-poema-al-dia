package com.webpoema.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Alarms do not survive a reboot, so restore the schedule when the phone starts. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val frequency = PreferenceManager.getFrequency(context)
        if (frequency != "never") {
            NotificationScheduler.schedule(context, frequency)
        }
    }
}
