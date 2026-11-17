package com.webpoema.app

import android.content.Context
import android.content.SharedPreferences

object PreferenceManager {
    private const val PREFS = "webpoema_prefs"
    private const val KEY_FREQ = "frequency"
    private const val KEY_THEME = "theme"
    private const val KEY_CACHED_TODAY = "cached_today_json"

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    fun getFrequency(context: Context): String {
        return prefs(context).getString(KEY_FREQ, "never") ?: "never"
    }

    fun setFrequency(context: Context, freq: String) {
        prefs(context).edit().putString(KEY_FREQ, freq).apply()
    }

    /** "light", "dark", or null if the user never chose (follow the system). */
    fun getTheme(context: Context): String? {
        return prefs(context).getString(KEY_THEME, null)
    }

    fun setTheme(context: Context, theme: String) {
        prefs(context).edit().putString(KEY_THEME, theme).apply()
    }

    /** Last successful today.json response, kept for offline notifications. */
    fun getCachedTodayJson(context: Context): String? {
        return prefs(context).getString(KEY_CACHED_TODAY, null)
    }

    fun setCachedTodayJson(context: Context, json: String) {
        prefs(context).edit().putString(KEY_CACHED_TODAY, json).apply()
    }
}
