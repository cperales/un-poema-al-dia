package com.webpoema.app

import android.os.Bundle
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class SettingsActivity : AppCompatActivity() {

    private lateinit var frequencyGroup: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        frequencyGroup = findViewById(R.id.frequency_group)

        // Load saved frequency
        val saved = PreferenceManager.getFrequency(this)
        when (saved) {
            "daily" -> frequencyGroup.check(R.id.freq_daily)
            "weekly" -> frequencyGroup.check(R.id.freq_weekly)
            else -> frequencyGroup.check(R.id.freq_never)
        }

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
            finish()
        }

        // Back button
        findViewById<MaterialButton>(R.id.btn_back).setOnClickListener {
            finish()
        }
    }
}
