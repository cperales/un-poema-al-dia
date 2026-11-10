package com.webpoema.app

import android.content.Context
import android.content.SharedPreferences

object PreferenceManager {
    private const val PREFS = "webpoema_prefs"
    private const val KEY_FREQ = "frequency"

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    fun getFrequency(context: Context): String {
        return prefs(context).getString(KEY_FREQ, "never") ?: "never"
    }

    fun setFrequency(context: Context, freq: String) {
        prefs(context).edit().putString(KEY_FREQ, freq).apply()
    }
}
