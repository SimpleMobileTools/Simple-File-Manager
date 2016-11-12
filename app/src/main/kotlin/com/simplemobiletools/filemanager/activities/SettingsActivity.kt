package com.simplemobiletools.filemanager.activities

import android.os.Bundle
import android.support.v4.app.TaskStackBuilder
import com.simplemobiletools.filemanager.R
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : SimpleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        setupDarkTheme()
        setupShowHidden()
    }

    private fun setupDarkTheme() {
        settings_dark_theme.isChecked = mConfig.isDarkTheme
        settings_dark_theme_holder.setOnClickListener {
            settings_dark_theme.toggle()
            mConfig.isDarkTheme = settings_dark_theme.isChecked
            restartActivity()
        }
    }

    private fun setupShowHidden() {
        settings_show_hidden.isChecked = mConfig.showHidden
        settings_show_hidden_holder.setOnClickListener {
            settings_show_hidden.toggle()
            mConfig.showHidden = settings_show_hidden.isChecked
        }
    }

    private fun restartActivity() {
        TaskStackBuilder.create(applicationContext).addNextIntentWithParentStack(intent).startActivities()
    }
}
