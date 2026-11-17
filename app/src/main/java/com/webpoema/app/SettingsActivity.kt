package com.webpoema.app

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.widget.RadioGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

class SettingsActivity : AppCompatActivity() {

    private lateinit var frequencyGroup: RadioGroup
    private lateinit var themeGroup: RadioGroup

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            NotificationScheduler.schedule(this, PreferenceManager.getFrequency(this))
            finish()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        frequencyGroup = findViewById(R.id.frequency_group)
        themeGroup = findViewById(R.id.theme_group)

        // Load saved frequency
        val saved = PreferenceManager.getFrequency(this)
        when (saved) {
            "daily" -> frequencyGroup.check(R.id.freq_daily)
            "weekly" -> frequencyGroup.check(R.id.freq_weekly)
            else -> frequencyGroup.check(R.id.freq_never)
        }

        // Load saved theme (if never chosen, show the one currently in use)
        val theme = PreferenceManager.getTheme(this) ?: run {
            val night = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            if (night == Configuration.UI_MODE_NIGHT_YES) "dark" else "light"
        }
        themeGroup.check(if (theme == "dark") R.id.theme_dark else R.id.theme_light)

        // Save button
        findViewById<MaterialButton>(R.id.btn_save).setOnClickListener {
            val selected = frequencyGroup.checkedRadioButtonId
            val freq = when (selected) {
                R.id.freq_daily -> "daily"
                R.id.freq_weekly -> "weekly"
                else -> "never"
            }
            PreferenceManager.setFrequency(this, freq)
            NotificationScheduler.schedule(this, freq)

            val newTheme = if (themeGroup.checkedRadioButtonId == R.id.theme_dark) "dark" else "light"
            PreferenceManager.setTheme(this, newTheme)
            WebPoemaApp.applyTheme(newTheme)

            if (freq != "never" && needsNotificationPermission()) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                finish()
            }
        }

        // Back button
        findViewById<MaterialButton>(R.id.btn_back).setOnClickListener {
            finish()
        }
    }

    private fun needsNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
}
